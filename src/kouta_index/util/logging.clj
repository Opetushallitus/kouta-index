(ns kouta-index.util.logging
  (:require
    [kouta-index.config :refer [config]]
    [clojure.tools.logging :as log]
    [cheshire.core :as cheshire]))

(defonce log-queries? (if-let [conf (:log-elastic-queries config)] conf false))

(defn debug-pretty
  [json]
  (when true
    (println (cheshire/generate-string json {:pretty true}))))
