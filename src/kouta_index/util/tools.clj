(ns kouta-index.util.tools
  (:import (java.util.regex Pattern)))

(defonce oid-pattern  (Pattern/compile "^[\\d][\\d\\.]+[\\d]$"))

(defn oid?
  [s]
  (and (not (nil? s)) (.matches (.matcher oid-pattern s))))

(defn uuid?
  [s]
  (try
    (java.util.UUID/fromString s)
    true
    (catch Exception e false)))

(defn ->trimmed-lowercase
  [s]
  (when s (clojure.string/lower-case (clojure.string/trim s))))

(defn comma-separated-string->vec
  [string]
  (vec (map #(clojure.string/trim %) (clojure.string/split string #","))))