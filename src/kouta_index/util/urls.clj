(ns kouta-index.util.urls
  (:require [kouta-index.config :refer [config]])
  (:import (fi.vm.sade.properties OphProperties)))

(def ^fi.vm.sade.properties.OphProperties url-properties (atom nil))

(defn- load-config
  []
  (let [{:keys [virkailija-internal]
         :or {virkailija-internal ""}} (:hosts config)]
    (reset! url-properties
            (doto (OphProperties. (into-array String ["/kouta-index-oph.properties"]))
              (.addDefault "host-virkailija-internal" virkailija-internal)))))

(defn resolve-url
  [key & params]
  (when (nil? @url-properties)
    (load-config))
  (.url @url-properties (name key) (to-array (or params []))))