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
    [clojure.tools.logging :as log]
    [kouta-index.list.filtered-lists :as l]))

(defn init
  []
  (if-let [elastic-url (:elastic-url config)]
    (intern 'clj-elasticsearch.elastic-utils 'elastic-host elastic-url)
    (throw (IllegalStateException. "Could not read elastic-url from configuration!")))
  (intern 'clj-log.access-log 'service "kouta-index")
  (intern 'clj-log.error-log 'test false))

(def kouta-index-api
  (api
    {:swagger {:ui   "/kouta-index/swagger"
               :spec "/kouta-index/swagger.json"
               :data {:info {:title       "Kouta-index"
                             :description "Backend for serving indexed kouta data for virkailija services."}}}}
    (context "/kouta-index"
      []
      (GET "/healthcheck" [:as request] :summary "Healthcheck API"
        (with-access-logging request (ok "OK")))
      (context "/koulutus"
         []
         :tags ["koulutus"]
         (GET "/filtered-list" [:as request]
              :summary "Listaa koulutusten perustiedot"
              :query-params [organisaatio :- (describe String "Pilkulla eroteltu lista organisaatioiden oideja")
                             {nimi :- (describe String "Suodata annetulla koulutuksen nimellä tai oidilla") nil}
                             {muokkaaja :- (describe String "Suodata annetulla muokkaajan nimellä tai oidilla") nil}
                             {tila :- (describe String "Suodata annetulla koulutuksen tilalla (julkaistu/tallennettu/arkistoitu)") nil}
                             {arkistoidut :- (describe Boolean "Näytetäänkö arkistoidut koulutukset (false)") true}
                             {page :- (describe Long "Sivunumero (1)") 1}
                             {size :- (describe Long "Sivun koko (10)") 10}
                             {lng :- (describe String "fi/sv/en (fi)") "fi"}
                             {order-by :- (describe String "nimi/tila/muokkaaja/modified/toteutukset (nimi)") "nimi"}
                             {order :- (describe String "asc/desc (asc)") "asc"}]
              (with-access-logging request (ok (l/filtered-koulutukset-list organisaatio lng page size order-by order :nimi nimi :muokkaaja muokkaaja :tila tila :arkistoidut arkistoidut)))))

      (context "/toteutus"
               []
        :tags ["toteutus"]
        (GET "/filtered-list" [:as request]
          :summary "Listaa toteutusten perustiedot"
          :query-params [organisaatio :- (describe String "Pilkulla eroteltu lista organisaatioiden oideja")
                         {nimi :- (describe String "Suodata annetulla toteutuksen nimellä tai oidilla") nil}
                         {muokkaaja :- (describe String "Suodata annetulla muokkaajan nimellä tai oidilla") nil}
                         {tila :- (describe String "Suodata annetulla toteutuksen tilalla (julkaistu/tallennettu/arkistoitu)") nil}
                         {arkistoidut :- (describe Boolean "Näytetäänkö arkistoidut toteutukset (false)") true}
                         {page :- (describe Long "Sivunumero (1)") 1}
                         {size :- (describe Long "Sivun koko (10)") 10}
                         {lng :- (describe String "fi/sv/en (fi)") "fi"}
                         {order-by :- (describe String "nimi/tila/muokkaaja/modified/haut (nimi)") "nimi"}
                         {order :- (describe String "asc/desc (asc)") "asc"}]
          (with-access-logging request (ok (l/filtered-toteutukset-list organisaatio lng page size order-by order :nimi nimi :muokkaaja muokkaaja :tila tila :arkistoidut arkistoidut)))))

      (context "/haku"
               []
        :tags ["haku"]
        (GET "/filtered-list" [:as request]
          :summary "Listaa hakujen perustiedot"
          :query-params [organisaatio :- (describe String "Pilkulla eroteltu lista organisaatioiden oideja")
                         {nimi :- (describe String "Suodata annetulla haun nimellä tai oidilla") nil}
                         {muokkaaja :- (describe String "Suodata annetulla muokkaajan nimellä tai oidilla") nil}
                         {tila :- (describe String "Suodata annetulla haun tilalla (julkaistu/tallennettu/arkistoitu)") nil}
                         {arkistoidut :- (describe Boolean "Näytetäänkö arkistoidut haut (false)") true}
                         {page :- (describe Long "Sivunumero (1)") 1}
                         {size :- (describe Long "Sivun koko (10)") 10}
                         {lng :- (describe String "fi/sv/en (fi)") "fi"}
                         {order-by :- (describe String "nimi/tila/muokkaaja/modified/hakukohteet (nimi)") "nimi"}
                         {order :- (describe String "asc/desc (asc)") "asc"}]
          (with-access-logging request (ok (l/filtered-haut-list organisaatio lng page size order-by order :nimi nimi :muokkaaja muokkaaja :tila tila :arkistoidut arkistoidut)))))

      (context "/valintaperuste"
               []
        :tags ["valintaperuste"]
        (GET "/filtered-list" [:as request]
          :summary "Listaa valintaperusteiden perustiedot"
          :query-params [organisaatio :- (describe String "Pilkulla eroteltu lista organisaatioiden oideja")
                         {nimi :- (describe String "Suodata annetulla valintaperusteen nimellä tai oidilla") nil}
                         {muokkaaja :- (describe String "Suodata annetulla muokkaajan nimellä tai oidilla") nil}
                         {tila :- (describe String "Suodata annetulla valintaperusteen tilalla (julkaistu/tallennettu/arkistoitu)") nil}
                         {arkistoidut :- (describe Boolean "Näytetäänkö arkistoidut valintaperusteet (false)") true}
                         {page :- (describe Long "Sivunumero (1)") 1}
                         {size :- (describe Long "Sivun koko (10)") 10}
                         {lng :- (describe String "fi/sv/en (fi)") "fi"}
                         {order-by :- (describe String "nimi/tila/muokkaaja/modified (nimi)") "nimi"}
                         {order :- (describe String "asc/desc (asc)") "asc"}]
          (with-access-logging request (ok (l/filtered-valintaperusteet-list organisaatio lng page size order-by order :nimi nimi :muokkaaja muokkaaja :tila tila :arkistoidut arkistoidut))))))))

(def app
  (wrap-cors kouta-index-api :access-control-allow-origin [#".*"] :access-control-allow-methods [:get :post]))