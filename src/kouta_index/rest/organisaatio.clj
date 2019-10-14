(ns kouta-index.rest.organisaatio
  (:require [kouta-index.rest.utils :refer [get->json-body]]
            [kouta-index.util.urls :refer [resolve-url]]
            [clojure.string :refer [split]]))

(defrecord Oids [parents oid children])

(defonce Oph "1.2.246.562.10.00000000001")

(defn- get-organisaatio-with-children
  [oid]
  (-> (get->json-body (resolve-url :organisaatio-service.v4.hierarkia.hae)
                      {:aktiiviset true :suunnitellut true :lakkautetut true :oid oid :skipParents true})
      :organisaatiot
      (first)))

(defn- parents
  [organisaatio]
  (-> organisaatio
      :parentOidPath
      (split #"/")
      (rest)))

(defn- children-recursive
  [children]
  (when (seq children)
    (concat (map :oid children) (children-recursive (mapcat :children children)))))

(defn- ->Oids
  ([oid children parents]
   (map->Oids { :parents (vec parents)
               :oid oid
               :children (vec children)}))
  ([oid]
   (->Oids oid [] [])))

(defn get-oids
  [oid]
  (if (= oid Oph)
    (->Oids Oph)
    (let [organisaatio (get-organisaatio-with-children oid)]
      (->Oids oid (children-recursive (:children organisaatio)) (parents organisaatio)))))

(defn with-parents-and-children
  [oids]
  (-> (vector (:oid oids))
      (concat (:parents oids) (:children oids))
      (vec)))

(defn with-children
  [oids]
  (-> (vector (:oid oids))
      (concat (:children oids))
      (vec)))