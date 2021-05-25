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
    (or (defined? :nimi) (defined? :muokkaaja) (defined? :tila) (not (:arkistoidut filters)))))

(defn- create-nimi-query
  [search-term]
  {:should (->> ["fi" "sv" "en"]
                (map #(->match-query (str "nimi." %) search-term)))})

(defn- ->nimi-filter
  [lng filters]
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
  (when-let [tila (:tila filters)]
    (->term-query :tila.keyword (->trimmed-lowercase tila))))

(defn- ->filters
  [lng filters]
  (let [nimi      (->nimi-filter lng filters)
        muokkaaja (->muokkaaja-filter filters)
        tila      (->tila-filter filters)]
    (vec (remove nil? [nimi muokkaaja tila]))))

(defn ->basic-oid-query
  [oids]
  (->terms-query :oid.keyword (vec oids)))

(defn ->basic-id-query
  [ids]
  (->terms-query :id.keyword (vec ids)))

(defn- ->query-with-filters
  [lng base-query filters]
  (let [filter-queries (->filters lng filters)]
    {:bool (-> { :must base-query }
               (cond-> (false? (:arkistoidut filters)) (assoc :must_not (->term-query :tila.keyword "arkistoitu")))
               (cond-> (not-empty filter-queries) (assoc :filter filter-queries)))}))

(defn- ->query
  [lng base-query filters]
  (if (filters? filters)
    (->query-with-filters lng base-query filters)
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
        (assoc :totalCount total)
        (assoc :result result))))

(defn search
  [index source-fields base-query {:keys [lng page size order-by order] :or {lng "fi" page 1 size 10 order-by "nimi" order "asc"} :as filters}]
  (let [source (vec source-fields)
        from (->from page size)
        sort (->sort-array lng order-by order)
        query (->query (->lng lng) base-query filters)]
    (debug-pretty { :_source source :from from :size size :sort sort :query query})
    (let [response (e/search index :_source source :from from :size size :sort sort :query query)]
      (->result response))))