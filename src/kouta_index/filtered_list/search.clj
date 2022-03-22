(ns kouta-index.filtered-list.search
  (:require
    [kouta-index.util.search :refer :all]
    [kouta-index.util.tools :refer :all]
    [kouta-index.rest.organisaatio :refer [with-children with-parents-and-children]]
    [clojure.tools.logging :as log]
    [cheshire.core :as cheshire]
    [clj-elasticsearch.elastic-connect :as e]
    [kouta-index.util.logging :refer [debug-pretty]]))

(defonce default-source-fields ["oid", "nimi", "tila", "muokkaaja", "modified", "organisaatio"])

(defn- ->field-keyword
  [lng field]
  (keyword (case (->trimmed-lowercase field)
             "nimi"        (str "nimi." (->lng lng) ".keyword")
             "tila"        "tila.keyword"
             "muokkaaja"   "muokkaaja.nimi.keyword"
             "modified"    "modified"
             "koulutustyyppi" "koulutustyyppi.keyword",
             "julkinen"    "julkinen"
                           (str "nimi." (->lng lng) ".keyword"))))

(defn- ->second-sort
  [lng order-by]
  (if (and order-by (= "nimi" (->trimmed-lowercase order-by)))
    (->sort (->field-keyword lng "modified") "asc")
    (->sort (->field-keyword lng "nimi") "asc")))

(defn- ->sort-array
  [lng order-by order]
    [(->sort (->field-keyword lng order-by) order) (->second-sort lng order-by) ])

(defn- filters?
  [filters]
  (let [defined? (fn [k] (not (nil? (k filters))))]
    (or (defined? :nimi)
        (defined? :muokkaaja)
        (defined? :tila)
        (defined? :koulutustyyppi)
        (defined? :julkinen)
        (defined? :hakutapa)
        (defined? :koulutuksenAlkamisvuosi)
        (defined? :koulutuksenAlkamiskausi))))

(defn- create-nimi-query
  [search-term]
  [{:bool {:should (->> ["fi" "sv" "en"]
                        (map #(->match-query (str "nimi." %) search-term)))}}
   {:bool {:should (->> ["fi" "sv" "en"]
                        (map (fn [lng] {:wildcard {(keyword (str "nimi." lng ".keyword")) (str "*" search-term "*")}})))}}])

(defn- ->nimi-filter
  [filters]
  (when-let [nimi (:nimi filters)]
    (if (oid? nimi)
      (->term-query :oid.keyword nimi)
      (if (uuid? nimi)
        (->term-query :id.keyword nimi)
        (create-nimi-query nimi)))))

(defn- ->muokkaaja-filter
  [filters]
  (when-let [muokkaaja (:muokkaaja filters)]
    (if (oid? muokkaaja)
      (->term-query :muokkaaja.oid muokkaaja)
      (->match-query :muokkaaja.nimi muokkaaja))))

(defn- ->tila-filter
  [filters]
  (when-let [tila-str (:tila filters)]
    (->terms-query :tila.keyword (comma-separated-string->vec tila-str))))

(defn- ->hakutapa-filter
  [filters]
  (when-let [hakutapa-str (:hakutapa filters)]
    (->terms-query :hakutapa.koodiUri.keyword
                   (comma-separated-string->vec
                     (clojure.string/replace hakutapa-str #"#\d+" "")))))

(defn- ->koulutuksen-alkamisvuosi-filter
  [filters]
  (when-let [koulutuksen-alkamisvuosi-str (:koulutuksenAlkamisvuosi filters)]
    (->terms-query :metadata.koulutuksenAlkamiskausi.koulutuksenAlkamisvuosi.keyword
                   (comma-separated-string->vec koulutuksen-alkamisvuosi-str))))

(defn- ->koulutuksen-alkamiskausi-filter
  [filters]
  (when-let [koulutuksen-alkamiskausi-str (:koulutuksenAlkamiskausi filters)]
    (->terms-query :metadata.koulutuksenAlkamiskausi.koulutuksenAlkamiskausi.koodiUri.keyword
                   (comma-separated-string->vec
                     (clojure.string/replace koulutuksen-alkamiskausi-str #"#\w+" "")))))

(defn- ->koulutustyyppi-filter
  [filters]
  (when-let [koulutustyyppi-str (:koulutustyyppi filters)]
    (->terms-query :koulutustyyppi.keyword (comma-separated-string->vec koulutustyyppi-str))))

(defn ->julkinen-filter
  [filters]
  (let [julkinen (filters :julkinen)]
    (when (some? julkinen)
      {:term {"julkinen" julkinen}})))

(defn- ->filters
  [filters]
  (let [nimi      (->nimi-filter filters)
        muokkaaja (->muokkaaja-filter filters)
        tila      (->tila-filter filters)
        koulutustyyppi (->koulutustyyppi-filter filters)
        julkinen  (->julkinen-filter filters)
        hakutapa  (->hakutapa-filter filters)
        koulutuksenAlkamisvuosi (->koulutuksen-alkamisvuosi-filter filters)
        koulutuksenAlkamiskausi (->koulutuksen-alkamiskausi-filter filters)]
    (vec (remove nil? (flatten
                        [nimi
                         muokkaaja
                         tila koulutustyyppi
                         julkinen
                         hakutapa
                         koulutuksenAlkamisvuosi
                         koulutuksenAlkamiskausi])))))

(defn ->basic-oid-query
  [oids]
  (->terms-query :oid.keyword (vec oids)))

(defn ->basic-id-query
  [ids]
  (->terms-query :id.keyword (vec ids)))

(defn- ->query-with-filters
  [base-query filters]
  (let [filter-queries (->filters filters)]
    {:bool (-> { :must base-query }
               (cond-> (not-empty filter-queries) (assoc :filter filter-queries)))}))

(defn- ->query
  [base-query filters]
  (if (filters? filters)
    (->query-with-filters base-query filters)
    base-query))

(defn- ->result
  [response]
  (debug-pretty response)
  (when (< 0 (-> response :_shards :failed))
    (log/error (cheshire/generate-string (-> response :_shards :failures) {:pretty true})))

  (let [hits (:hits response)
        total (:total hits)
        result (vec (map #(-> % :_source) (:hits hits)))]
    (-> {}
        (assoc :totalCount (:value total))
        (assoc :result result))))

(defn search
  [index source-fields base-query {:keys [lng page size order-by order] :or {lng "fi" page 1 size 10 order-by "nimi" order "asc"} :as filters}]
  (let [source (vec source-fields)
        from (->from page size)
        sort (->sort-array lng order-by order)
        query (->query base-query filters)]
    (debug-pretty { :_source source :from from :size size :sort sort :query query})
    (let [response (e/search index :_source source :from from :size size :sort sort :query query)]
      (->result response))))
