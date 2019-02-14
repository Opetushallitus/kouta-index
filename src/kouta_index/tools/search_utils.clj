(ns kouta-index.tools.search-utils
  (:require
    [kouta-index.tools.generic-utils :refer [->trimmed-lowercase]]))

(defn ->order
  [order]
  (if (and (not (nil? order)) (= (->trimmed-lowercase order) "desc")) "desc" "asc"))

(defn ->lng
  [lng]
  (let [l (->trimmed-lowercase lng)]
    (if (or (= "fi" l) (= "sv" l) (= "en" l)) l "fi")))

(defn ->from
  [page size]
  (if (pos? page) (* (- page 1) size) 0))

(defn ->size
  ([size default]
   (if (pos? size) (if (< size default) size default) 0))
  ([size]
   (->size size 20)))

(defn ->sort
  [field order]
  { (keyword field) { :order (->order order)} })

(defn ->terms-query
  [field values]
  { :terms { (keyword field) (vec values) } })

(defn ->term-query
  [field value]
  { :term { (keyword field) value } })

(defn ->match-query
  [field value]
  { :match { (keyword field) value } })