(ns kouta-index.filtered-list.search
  (:require
    [kouta-index.util.search :refer :all]
    [kouta-index.util.tools :refer :all]
    [kouta-index.rest.organisaatio :refer [with-children with-parents-and-children]]
    [clojure.tools.logging :as log]
    [cheshire.core :as cheshire]
    [clj-elasticsearch.elastic-connect :as e]
    [kouta-index.util.logging :refer [debug-pretty]]))

(defonce default-source-fields ["oid", "nimi", "tila", "muokkaaja", "modified", "organisaatio", "count"])

(defn- ->field-keyword
  [lng field]
  (keyword (case (->trimmed-lowercase field)
             "nimi"        (str "nimi." (->lng lng) ".keyword")
             "tila"        "tila.keyword"
             "muokkaaja"   "muokkaaja.nimi.keyword"
             "modified"    "modified"
                           (str "nimi." (->lng lng) ".keyword"))))

(defn- ->count-script-field
  [script]
  { :count { :script { :lang "painless" :inline script}}})

(defn- ->count-script-sort
  [script order]
  { :_script { :type "number" :order (->order order) :script { :lang "painless" :inline script}}})

(defn- ->second-sort
  [lng order-by]
  (if (and order-by (= "nimi" (->trimmed-lowercase order-by)))
    (->sort (->field-keyword lng "modified") "asc")
    (->sort (->field-keyword lng "nimi") "asc")))

(defn- ->sort-array
  [lng script script-field order-by order]
  (let [first-sort (if (and script-field (= script-field order-by))
                     (->count-script-sort script order)
                     (->sort (->field-keyword lng order-by) order))]
    [first-sort (->second-sort lng order-by) ]))

(defn- filters?
  [filters]
  (let [defined? (fn [k] (not (nil? (k filters))))]
    (or (defined? :nimi) (defined? :muokkaaja) (defined? :tila) (not (:arkistoidut filters)))))

(defn- ->nimi-filter
  [lng filters]
  (when-let [nimi (:nimi filters)]
    (if (oid? nimi)
      (->term-query :oid.keyword nimi)
      (if (uuid? nimi)
        (->term-query :id.keyword nimi)
        (->match-query (str "nimi." (->lng lng)) nimi)))))

(defn- ->muokkaaja-filter
  [lng filters]
  (when-let [muokkaaja (:muokkaaja filters)]
    (if (oid? muokkaaja)
      (->term-query :muokkaaja.oid muokkaaja)
      (->match-query :muokkaaja.nimi muokkaaja))))

(defn- ->tila-filter
  [lng filters]
  (when-let [tila (:tila filters)]
    (->term-query :tila.keyword (->trimmed-lowercase tila))))

(defn- ->filters
  [lng filters]
  (let [nimi      (->nimi-filter lng filters)
        muokkaaja (->muokkaaja-filter lng filters)
        tila      (->tila-filter lng filters)]
    (vec (remove nil? [nimi muokkaaja tila]))))

(defn ->basic-org-query
  [orgs]
  (->terms-query :organisaatio.oid (vec orgs)))

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
  [response script-field]
  (debug-pretty response)
  (when (< 0 (-> response :_shards :failed))
    (log/error (cheshire/generate-string (-> response :_shards :failures) {:pretty true})))

  (let [hits (:hits response)
        total (:total hits)
        result (vec (map (fn [x] (-> x
                                     :_source
                                     (cond-> (not (nil? script-field)) (assoc (keyword script-field) (first (:count (:fields x))))))) (:hits hits)))]
    (-> {}
        (assoc :totalCount total)
        (assoc :result result))))

(defn search
  [index source-fields base-query script script-field {:keys [lng page size order-by order] :or {lng "fi" page 1 size 10 order-by "nimi" order "asc"} :as filters}]
  (let [source (vec source-fields)
        from (->from page size)
        size (->size size)
        sort (->sort-array lng script script-field order-by order)
        query (->query (->lng lng) base-query filters)
        script-fields (when script-field (->count-script-field script))]
    (debug-pretty { :_source source :from from :size size :sort sort :query query :script_fields script-fields })
    (let [response (if script-field
                     (e/search index index :_source source :from from :size size :sort sort :query query :script_fields script-fields)
                     (e/search index index :_source source :from from :size size :sort sort :query query))]
      (->result response script-field))))