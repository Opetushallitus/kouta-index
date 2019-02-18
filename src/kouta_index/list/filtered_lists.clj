(ns kouta-index.list.filtered-lists
  (:require
    [kouta-index.tools.search-utils :refer :all]
    [kouta-index.tools.generic-utils :refer :all]
    [clojure.tools.logging :as log]
    [cheshire.core :as cheshire]
    [clj-elasticsearch.elastic-connect :as e]))

(defonce default-source-fields ["oid", "nimi", "tila", "muokkaaja", "modified", "organisaatio", "count"])

(defn- ->field-keyword
  [lng field]
  (keyword (case (->trimmed-lowercase field)
             "nimi"        (str "nimi." (->lng lng) ".keyword")
             "tila"        "tila.keyword"
             "muokkaaja"   "muokkaaja.nimi.keyword"
             "modified"    "modified"
                           (str "nimi." (->lng lng) ".keyword"))))

(defn- ->counter-script
  [orgs nested-field]
  (let [org-list  (clojure.string/join "," (map #(str "'" % "'") (comma-separated-string->vec orgs)))
        script    (str "int count = 0;"
                       "List oids = Arrays.asList(new String[] {" org-list "});"
                         "for(int i = 0; i < params['_source']['" nested-field "'].getLength(); i++) {"
                           "if(oids.contains(params['_source']['" nested-field "'].get(i).get('organisaatio').get('oid'))) {"
                             "count++;"
                           "}"
                         "}"
                       "return count;")]
    { :script { :lang "painless" :inline script}}))

(defn- ->counter-script-field
  [orgs nested-field]
  { :count (->counter-script orgs nested-field)})

(defn- ->counter-script-sort
  [orgs nested-field order]
  { :_script (merge { :type "number" :order (->order order) } (->counter-script orgs nested-field))})

(defn- ->second-sort
  [lng field]
  (if (and field (= "nimi" (->trimmed-lowercase field)))
    (->sort (->field-keyword lng "modified") "asc")
    (->sort (->field-keyword lng "nimi") "asc")))

(defn- ->sort-array
  [lng orgs count-field field order]
  (let [first-sort (if (and count-field (= count-field field))
                     (->counter-script-sort orgs field order)
                     (->sort (->field-keyword lng field) order))]
    [first-sort (->second-sort lng field) ]))

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

(defn- debug-pretty
  [json]
  (log/info (cheshire/generate-string json {:pretty true})))

(defn- ->result
  [response count-field]
  (debug-pretty response)
  (let [hits (:hits response)
        total (:total hits)
        result (vec (map (fn [x] (-> x
                                     :_source
                                     (cond-> (not (nil? count-field)) (assoc (keyword count-field) (first (:count (:fields x))))))) (:hits hits)))]
    (-> {}
        (assoc :totalCount total)
        (assoc :result result))))

(defn- search
  [index source-fields count-field orgs lng page size order-by order-direction & {:as filters}]
  (let [source (vec source-fields)
        from (->from page size)
        size (->size size)
        sort (->sort-array lng orgs count-field order-by order-direction)
        query (->query (->lng lng) orgs filters)
        script-fields (when count-field (->counter-script-field orgs count-field))]
    (debug-pretty { :_source source :from from :size size :sort sort :query query :script_fields script-fields })
    (let [response (if count-field
                     (e/search index index :_source source :from from :size size :sort sort :query query :script_fields script-fields)
                     (e/search index index :_source source :from from :size size :sort sort :query query))]
      (->result response count-field))))

(def filtered-koulutukset-list
  (partial search "koulutus-kouta" default-source-fields "toteutukset"))

(def filtered-toteutukset-list
  (partial search "toteutus-kouta" default-source-fields "haut"))

(def filtered-haut-list
  (partial search "haku-kouta" default-source-fields "hakukohteet"))

(def filtered-valintaperusteet-list
  (partial search "valintaperuste-kouta" default-source-fields nil))