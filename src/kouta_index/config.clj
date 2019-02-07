(ns kouta-index.config
  (:require
    [cprop.core :refer [load-config]]
    [cprop.source :as source]))

(defonce config (load-config :merge [(source/from-system-props) (source/from-env)]))