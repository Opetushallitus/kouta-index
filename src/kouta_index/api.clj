(ns kouta-index.api
  (:require
    [kouta-index.config :refer [config]]
    [kouta-index.util.tools :refer [comma-separated-string->vec]]
    [clj-log.access-log :refer [with-access-logging]]
    [compojure.api.sweet :refer :all]
    [ring.middleware.cors :refer [wrap-cors]]
    [clj-elasticsearch.elastic-utils]
    [clj-log.error-log]
    [ring.util.http-response :refer :all]
    [environ.core :refer [env]]
    [clojure.tools.logging :as log]
    [kouta-index.filtered-list.service :refer :all]
    [schema.core :as schema]))

(defn init
  []
  (if-let [elastic-url (:elastic-url config)]
    (intern 'clj-elasticsearch.elastic-utils 'elastic-host elastic-url)
    (throw (IllegalStateException. "Could not read elastic-url from configuration!")))
  (intern 'clj-log.access-log 'service "kouta-index")
  (intern 'clj-log.error-log 'test false))

(schema/defschema Oids [schema/Str])

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
          :summary "Listaa koulutusten rikastetut perustiedot indeksistä"
          :query-params [oids :- (describe String "Pilkulla eroteltu lista koulutusten oideja")
                         {nimi :- (describe String "Suodata annetulla koulutuksen nimellä tai oidilla") nil}
                         {muokkaaja :- (describe String "Suodata annetulla muokkaajan nimellä tai oidilla") nil}
                         {tila :- (describe String "Suodata annetulla koulutuksen tilalla (julkaistu/tallennettu/arkistoitu)") nil}
                         {arkistoidut :- (describe Boolean "Näytetäänkö arkistoidut koulutukset (false)") true}
                         {page :- (describe Long "Sivunumero (1)") 1}
                         {size :- (describe Long "Sivun koko (10)") 10}
                         {lng :- (describe String "fi/sv/en (fi)") "fi"}
                         {order-by :- (describe String "nimi/tila/muokkaaja/modified (nimi)") "nimi"}
                         {order :- (describe String "asc/desc (asc)") "asc"} :as params]
          (with-access-logging request (ok (search-koulutukset (comma-separated-string->vec oids) params))))
        (POST "/filtered-list" [:as request]
          :summary "Listaa koulutusten rikastetut perustiedot indeksistä"
          :query-params [{nimi :- (describe String "Suodata annetulla koulutuksen nimellä tai oidilla") nil}
                         {muokkaaja :- (describe String "Suodata annetulla muokkaajan nimellä tai oidilla") nil}
                         {tila :- (describe String "Suodata annetulla koulutuksen tilalla (julkaistu/tallennettu/arkistoitu)") nil}
                         {arkistoidut :- (describe Boolean "Näytetäänkö arkistoidut koulutukset (false)") true}
                         {page :- (describe Long "Sivunumero (1)") 1}
                         {size :- (describe Long "Sivun koko (10)") 10}
                         {lng :- (describe String "fi/sv/en (fi)") "fi"}
                         {order-by :- (describe String "nimi/tila/muokkaaja/modified (nimi)") "nimi"}
                         {order :- (describe String "asc/desc (asc)") "asc"} :as params]
          :body [oids Oids]
          (with-access-logging request (ok (search-koulutukset oids params)))))

      (context "/toteutus"
               []
        :tags ["toteutus"]
        (GET "/filtered-list" [:as request]
          :summary "Listaa toteutusten rikastetut perustiedot indeksistä"
          :query-params [oids :- (describe String "Pilkulla eroteltu lista toteutusten oideja")
                         {nimi :- (describe String "Suodata annetulla toteutuksen nimellä tai oidilla") nil}
                         {muokkaaja :- (describe String "Suodata annetulla muokkaajan nimellä tai oidilla") nil}
                         {tila :- (describe String "Suodata annetulla toteutuksen tilalla (julkaistu/tallennettu/arkistoitu)") nil}
                         {arkistoidut :- (describe Boolean "Näytetäänkö arkistoidut toteutukset (false)") true}
                         {page :- (describe Long "Sivunumero (1)") 1}
                         {size :- (describe Long "Sivun koko (10)") 10}
                         {lng :- (describe String "fi/sv/en (fi)") "fi"}
                         {order-by :- (describe String "nimi/tila/muokkaaja/modified (nimi)") "nimi"}
                         {order :- (describe String "asc/desc (asc)") "asc"} :as params]
          (with-access-logging request (ok (search-toteutukset (comma-separated-string->vec oids) params))))
        (POST "/filtered-list" [:as request]
          :summary "Listaa toteutusten rikastetut perustiedot indeksistä"
          :query-params [{nimi :- (describe String "Suodata annetulla toteutuksen nimellä tai oidilla") nil}
                         {muokkaaja :- (describe String "Suodata annetulla muokkaajan nimellä tai oidilla") nil}
                         {tila :- (describe String "Suodata annetulla toteutuksen tilalla (julkaistu/tallennettu/arkistoitu)") nil}
                         {arkistoidut :- (describe Boolean "Näytetäänkö arkistoidut toteutukset (false)") true}
                         {page :- (describe Long "Sivunumero (1)") 1}
                         {size :- (describe Long "Sivun koko (10)") 10}
                         {lng :- (describe String "fi/sv/en (fi)") "fi"}
                         {order-by :- (describe String "nimi/tila/muokkaaja/modified (nimi)") "nimi"}
                         {order :- (describe String "asc/desc (asc)") "asc"} :as params]
          :body [oids Oids]
          (with-access-logging request (ok (search-toteutukset oids params)))))

      (context "/haku"
               []
        :tags ["haku"]
        (GET "/filtered-list" [:as request]
          :summary "Listaa hakujen rikastetut perustiedot indeksistä"
          :query-params [oids :- (describe String "Pilkulla eroteltu lista hakujen oideja")
                         {nimi :- (describe String "Suodata annetulla haun nimellä tai oidilla") nil}
                         {muokkaaja :- (describe String "Suodata annetulla muokkaajan nimellä tai oidilla") nil}
                         {tila :- (describe String "Suodata annetulla haun tilalla (julkaistu/tallennettu/arkistoitu)") nil}
                         {arkistoidut :- (describe Boolean "Näytetäänkö arkistoidut haut (false)") true}
                         {page :- (describe Long "Sivunumero (1)") 1}
                         {size :- (describe Long "Sivun koko (10)") 10}
                         {lng :- (describe String "fi/sv/en (fi)") "fi"}
                         {order-by :- (describe String "nimi/tila/muokkaaja/modified (nimi)") "nimi"}
                         {order :- (describe String "asc/desc (asc)") "asc"} :as params]
          (with-access-logging request (ok (search-haut (comma-separated-string->vec oids) params))))
        (POST "/filtered-list" [:as request]
          :summary "Listaa hakujen rikastetut perustiedot indeksistä"
          :query-params [{nimi :- (describe String "Suodata annetulla haun nimellä tai oidilla") nil}
                         {muokkaaja :- (describe String "Suodata annetulla muokkaajan nimellä tai oidilla") nil}
                         {tila :- (describe String "Suodata annetulla haun tilalla (julkaistu/tallennettu/arkistoitu)") nil}
                         {arkistoidut :- (describe Boolean "Näytetäänkö arkistoidut haut (false)") true}
                         {page :- (describe Long "Sivunumero (1)") 1}
                         {size :- (describe Long "Sivun koko (10)") 10}
                         {lng :- (describe String "fi/sv/en (fi)") "fi"}
                         {order-by :- (describe String "nimi/tila/muokkaaja/modified (nimi)") "nimi"}
                         {order :- (describe String "asc/desc (asc)") "asc"} :as params]
          :body [oids Oids]
          (with-access-logging request (ok (search-haut oids params)))))

      (context "/valintaperuste"
               []
        :tags ["valintaperuste"]
        (GET "/filtered-list" [:as request]
          :summary "Listaa valintaperusteiden rikastetut perustiedot indeksistä"
          :query-params [ids :- (describe String "Pilkulla eroteltu lista valintaperusteiden id:itä")
                         {nimi :- (describe String "Suodata annetulla valintaperusteen nimellä tai oidilla") nil}
                         {muokkaaja :- (describe String "Suodata annetulla muokkaajan nimellä tai oidilla") nil}
                         {tila :- (describe String "Suodata annetulla valintaperusteen tilalla (julkaistu/tallennettu/arkistoitu)") nil}
                         {arkistoidut :- (describe Boolean "Näytetäänkö arkistoidut valintaperusteet (false)") true}
                         {page :- (describe Long "Sivunumero (1)") 1}
                         {size :- (describe Long "Sivun koko (10)") 10}
                         {lng :- (describe String "fi/sv/en (fi)") "fi"}
                         {order-by :- (describe String "nimi/tila/muokkaaja/modified (nimi)") "nimi"}
                         {order :- (describe String "asc/desc (asc)") "asc"} :as params]
          (with-access-logging request (ok (search-valintaperusteet (comma-separated-string->vec ids) params))))
        (POST "/filtered-list" [:as request]
          :summary "Listaa valintaperusteiden rikastetut perustiedot indeksistä"
          :query-params [{nimi :- (describe String "Suodata annetulla valintaperusteen nimellä tai oidilla") nil}
                         {muokkaaja :- (describe String "Suodata annetulla muokkaajan nimellä tai oidilla") nil}
                         {tila :- (describe String "Suodata annetulla valintaperusteen tilalla (julkaistu/tallennettu/arkistoitu)") nil}
                         {arkistoidut :- (describe Boolean "Näytetäänkö arkistoidut valintaperusteet (false)") true}
                         {page :- (describe Long "Sivunumero (1)") 1}
                         {size :- (describe Long "Sivun koko (10)") 10}
                         {lng :- (describe String "fi/sv/en (fi)") "fi"}
                         {order-by :- (describe String "nimi/tila/muokkaaja/modified (nimi)") "nimi"}
                         {order :- (describe String "asc/desc (asc)") "asc"} :as params]
          :body [ids Oids]
          (with-access-logging request (ok (search-valintaperusteet ids params)))))

      (context "/hakukohde"
               []
        :tags ["hakukohde"]
        (GET "/filtered-list" [:as request]
          :summary "Listaa hakukohteiden rikastetut perustiedot indeksistä"
          :query-params [oids :- (describe String "Pilkulla eroteltu lista hakukohteiden oideja")
                         {nimi :- (describe String "Suodata annetulla hakukohteen nimellä tai oidilla") nil}
                         {muokkaaja :- (describe String "Suodata annetulla muokkaajan nimellä tai oidilla") nil}
                         {tila :- (describe String "Suodata annetulla haun tilalla (julkaistu/tallennettu/arkistoitu)") nil}
                         {arkistoidut :- (describe Boolean "Näytetäänkö arkistoidut hakukohteet (false)") true}
                         {page :- (describe Long "Sivunumero (1)") 1}
                         {size :- (describe Long "Sivun koko (10)") 10}
                         {lng :- (describe String "fi/sv/en (fi)") "fi"}
                         {order-by :- (describe String "nimi/tila/muokkaaja/modified (nimi)") "nimi"}
                         {order :- (describe String "asc/desc (asc)") "asc"} :as params]
          (with-access-logging request (ok (search-hakukohteet (comma-separated-string->vec oids) params))))
        (POST "/filtered-list" [:as request]
          :summary "Listaa hakukohteiden rikastetut perustiedot indeksistä"
          :query-params [{nimi :- (describe String "Suodata annetulla hakukohteen nimellä tai oidilla") nil}
                         {muokkaaja :- (describe String "Suodata annetulla muokkaajan nimellä tai oidilla") nil}
                         {tila :- (describe String "Suodata annetulla haun tilalla (julkaistu/tallennettu/arkistoitu)") nil}
                         {arkistoidut :- (describe Boolean "Näytetäänkö arkistoidut hakukohteet (false)") true}
                         {page :- (describe Long "Sivunumero (1)") 1}
                         {size :- (describe Long "Sivun koko (10)") 10}
                         {lng :- (describe String "fi/sv/en (fi)") "fi"}
                         {order-by :- (describe String "nimi/tila/muokkaaja/modified (nimi)") "nimi"}
                         {order :- (describe String "asc/desc (asc)") "asc"} :as params]
          :body [oids Oids]
          (with-access-logging request (ok (search-hakukohteet oids params))))))))

(def app
  (wrap-cors kouta-index-api :access-control-allow-origin [#".*"] :access-control-allow-methods [:get :post]))
