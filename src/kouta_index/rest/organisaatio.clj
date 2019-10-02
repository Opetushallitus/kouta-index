(ns kouta-index.rest.organisaatio
  (:require [kouta-index.rest.utils :refer [get->json-body]]
            [kouta-index.util.urls :refer [resolve-url]]
            [clojure.string :refer [split]]))

(defrecord Oids [parents oid children])

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

(defn get-oids
  [oid]
  (let [organisaatio (get-organisaatio-with-children oid)]
    (map->Oids { :parents (parents organisaatio)
                 :oid oid
                 :children (children-recursive (:children organisaatio))})))

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