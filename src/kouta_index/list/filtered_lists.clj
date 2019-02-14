(ns kouta-index.list.filtered-lists
  (:require
    [kouta-index.tools.search-utils :refer :all]
    [kouta-index.tools.generic-utils :refer :all]
    [clojure.tools.logging :as log]
    [clj-elasticsearch.elastic-connect :as e]))

(defonce default-source-fields ["oid", "nimi", "tila", "muokkaaja", "modified", "organisaatio"])

(defn- ->field-keyword
  [lng field]
  (keyword (case (->trimmed-lowercase field)
             "nimi"        (str "nimi." (->lng lng) ".keyword")
             "tila"        "tila.keyword"
             "muokkaaja"   "muokkaaja.nimi.keyword"
             "modified"    "modified"
             "toteutukset" "toteutusCount"
             "haut"        "hakuCount"
             "hakukohteet" "hakukohdeCount"
                           (str "nimi." (->lng lng) ".keyword"))))

(defn- ->second-sort
  [lng field]
  (if (and field (= "nimi" (->trimmed-lowercase field)))
    (->sort (->field-keyword lng "modified") "asc")
    (->sort (->field-keyword lng "nimi") "asc")))

(defn- ->sort-array
  [lng field order]
  [ (->sort (->field-keyword lng field) order)
    (->second-sort lng field) ])

(defn- filters?
  [filters]
  (let [defined? (fn [k] (not (nil? (k filters))))]
    (or (defined? :nimi) (defined? :muokkaaja) (defined? :tila) (not (:arkistoidut filters)))))

(defn- ->nimi-filter
  [lng filters]
  (when-let [nimi (:nimi filters)]
    (if (oid? nimi)
      (->term-query :oid.keyword nimi)
      (->match-query (str "nimi." (->lng lng)) nimi))))

(defn- ->muokkaaja-filter
  [lng filters]
  (when-let [muokkaaja (:muokkaaja filters)]
    (if (oid? muokkaaja)
      (->term-query :muokkaaja.oid muokkaaja)
      (->match-query :muokkaaja.nimi muokkaaja))))

(defn- ->tila-filter
  [lng filters]
  (when-let [tila (:tila filters)]
    (->term-query :tila (->trimmed-lowercase tila))))

(defn- ->filters
  [lng filters]
  (let [nimi      (->nimi-filter lng filters)
        muokkaaja (->muokkaaja-filter lng filters)
        tila      (->tila-filter lng filters)]
    (vec (remove nil? [nimi muokkaaja tila]))))

(defn- ->basic-query
  [orgs]
  (->terms-query :organisaatio.oid (comma-separated-string->vec orgs)))

(defn- ->query-with-filters
  [lng orgs filters]
  (let [filter-queries (->filters lng filters)]
    {:bool (-> { :must (->basic-query orgs) }
               (cond-> (false? (:arkistoidut filters)) (assoc :must_not (->term-query :tila "arkistoitu")))
               (cond-> (not-empty filter-queries) (assoc :filter filter-queries)))}))

(defn- ->query
  [lng orgs filters]
  (if (filters? filters)
    (->query-with-filters lng orgs filters)
    (->basic-query orgs)))

(defn- ->result
  [response]
  (let [hits (:hits response)
        total (:total hits)
        result (vec (map #(:_source %) (:hits hits)))]
    (-> {}
        (assoc :totalCount total)
        (assoc :result result))))

(defn- search
  [index source-fields orgs lng page size order-by order-direction & {:as filters}]
  (println (->query (->lng lng) orgs filters))
  (->result (e/search index
                      index
                      :_source (vec source-fields)
                      :from (->from page size)
                      :size (->size size)
                      :sort (->sort-array lng order-by order-direction)
                      :query (->query (->lng lng) orgs filters))))

(def filtered-koulutukset-list
  (partial search "koulutus-kouta" (conj default-source-fields "toteutusCount")))

(def filtered-toteutukset-list
  (partial search "toteutus-kouta" (conj default-source-fields "hakuCount")))

(def filtered-haut-list
  (partial search "haku-kouta" (conj default-source-fields "hakukohdeCount")))

(def filtered-valintaperusteet-list
  (partial search "valintaperuste-kouta" default-source-fields))