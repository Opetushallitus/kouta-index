(ns kouta-index.api
  (:require
    [kouta-index.config :refer [config]]
    [clj-log.access-log :refer [with-access-logging]]
    [compojure.api.sweet :refer :all]
    [ring.middleware.cors :refer [wrap-cors]]
    [clj-elasticsearch.elastic-utils]
    [clj-log.error-log]
    [ring.util.http-response :refer :all]
    [environ.core :refer [env]]
    [clojure.tools.logging :as log]))

(defn init
  []
  (if-let [elastic-url (:elastic-url config)]
    (intern 'clj-elasticsearch.elastic-utils 'elastic-host elastic-url)
    (throw (IllegalStateException. "Could not read elastic-url from configuration!")))
  (intern 'clj-log.access-log 'service "kouta-index")
  (intern 'clj-log.error-log 'test false))

(def kouta-index-api
  (api
    {:swagger {:ui   "/kouta-index"
               :spec "/kouta-index/swagger.json"
               :data {:info {:title       "Kouta-index"
                             :description "Backend for serving indexed kouta data for virkailija services."}}}}
    (context "/kouta-index" []
      (GET "/healthcheck" [:as request] :summary "Healthcheck API"
        (with-access-logging request (ok "OK"))))))

(def app
  (wrap-cors kouta-index-api :access-control-allow-origin [#".*"] :access-control-allow-methods [:get :post]))