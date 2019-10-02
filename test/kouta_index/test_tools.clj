(ns kouta-index.test-tools)

(defn mock-organisaatio
  [tests]
  (defrecord Oids [parents oid children])
  (with-redefs [kouta-index.rest.organisaatio/get-oids (fn [oid] (map->Oids { :parents () :oid oid :children ()}))]
    (tests)))