(ns kouta-index.tools.generic-utils
  (:import (java.util.regex Pattern)))

(defonce oid-pattern (Pattern/compile "^[\\d][\\d\\.]+[\\d]$"))

(defn oid?
  [s]
  (and (not (nil? s)) (.matches (.matcher oid-pattern s))))

(defn ->trimmed-lowercase
  [s]
  (when s (clojure.string/lower-case (clojure.string/trim s))))

(defn comma-separated-string->vec
  [string]
  (vec (map #(clojure.string/trim %) (clojure.string/split string #","))))