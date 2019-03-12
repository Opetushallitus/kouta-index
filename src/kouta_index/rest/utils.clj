(ns kouta-index.rest.utils
  (:require [clj-http.client :as client]))

(defn add-callerinfo
  [options]
  (update-in options [:headers] assoc
             "Caller-Id" "fi.opintopolku.kouta-index"
             "clientSubSystemCode" "fi.opintopolku.kouta-index"))

(defn get [url options]
  (client/get url (add-callerinfo options)))
