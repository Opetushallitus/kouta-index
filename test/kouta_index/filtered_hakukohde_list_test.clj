(ns kouta-index.filtered-hakukohde-list-test
  (:require [clojure.test :refer :all]
            [kouta-index.api :refer :all]
            [ring.mock.request :as mock]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-index.test-tools :as tools]
            [kouta-indeksoija-service.fixture.external-services :as mocks])
  (:import (java.net URLEncoder)))

(intern 'clj-log.access-log 'service "kouta-index")

(use-fixtures :each fixture/mock-indexing-fixture tools/mock-organisaatio)

(defn enc
  [str]
  (URLEncoder/encode str))

(defn get-200
  [url]
  (let [response (app (mock/request :get url))]
    (is (= (:status response) 200))
    (fixture/->keywordized-json (slurp (:body response)))))

(defn get-200-oids
  [url]
  (let [result (:result (get-200 url))]
    ;(println (cheshire.core/generate-string result {:pretty true}))
    (map #(:oid %) result)))

(deftest hakukohde-list-empty-index-test
  (testing "search in empty index"
    (get-200-oids "/kouta-index/hakukohde/filtered-list?oids=1.2.246.562.20.000001"))
  (testing "search in empty index sort by nimi"
    (get-200-oids "/kouta-index/hakukohde/filtered-list?oids=1.2.246.562.20.000001&order-by=nimi"))
  (testing "search in empty index sort by tila"
    (get-200-oids "/kouta-index/hakukohde/filtered-list?oids=1.2.246.562.20.000001&order-by=tila"))
  (testing "search in empty index sort by muokkaaja"
    (get-200-oids "/kouta-index/hakukohde/filtered-list?oids=1.2.246.562.20.000001&order-by=muokkaaja"))
  (testing "search in empty index sort by modified"
    (get-200-oids "/kouta-index/hakukohde/filtered-list?oids=1.2.246.562.20.000001&order-by=modified")))

(deftest filtered-hakukohde-list-test

  (let [toteutusOid1 "1.2.246.562.17.0000001"
        toteutusOid2 "1.2.246.562.17.0000002"
        toteutusOid3 "1.2.246.562.17.0000003"
        toteutusOid4 "1.2.246.562.17.0000004"
        toteutusOid5 "1.2.246.562.17.0000005"
        hakukohdeOid1 "1.2.246.562.20.0000001"
        hakukohdeOid2 "1.2.246.562.20.0000002"
        hakukohdeOid3 "1.2.246.562.20.0000003"
        hakukohdeOid4 "1.2.246.562.20.0000004"
        hakukohdeOid5 "1.2.246.562.20.0000005"
        valintaperusteId1 "31972648-ebb7-4185-ac64-31fa6b841e34"
        sorakuvausId      "31972648-ebb7-4185-ac64-31fa6b841e39"]

    (defn hakukohde-url
      ([oids] (str "/kouta-index/hakukohde/filtered-list?oids=" oids))
      ([] (hakukohde-url (str hakukohdeOid2 "," hakukohdeOid3 "," hakukohdeOid4 "," hakukohdeOid5))))

    (fixture/add-toteutus-mock toteutusOid1 "1.2.246.562.13.0000001" :tila "julkaistu"   :nimi "Automaatioalan perusopinnot" :organisaatio mocks/Oppilaitos2 :tarjoajat mocks/Oppilaitos2)
    (fixture/add-toteutus-mock toteutusOid2 "1.2.246.562.13.0000001" :tila "julkaistu"   :nimi "Automatiikan perusopinnot" :tarjoajat mocks/Oppilaitos2)
    (fixture/add-toteutus-mock toteutusOid3 "1.2.246.562.13.0000001" :tila "julkaistu"   :nimi "Autoalan perusopinnot" :modified "2018-05-05T12:02:23" :muokkaaja "1.2.246.562.24.55555555555")
    (fixture/add-toteutus-mock toteutusOid4 "1.2.246.562.13.0000001" :tila "arkistoitu"  :nimi "Autoalan perusopinnot" :modified "2018-06-05T12:02:23")
    (fixture/add-toteutus-mock toteutusOid5 "1.2.246.562.13.0000001" :tila "tallennettu" :nimi "Autoalan perusopinnot" :modified "2018-06-05T12:02:23")

    (fixture/add-hakukohde-mock hakukohdeOid1 toteutusOid1 "1.2.246.562.29.0000001" :tila "julkaistu" :nimi "Hakukohde" :valintaperuste valintaperusteId1 :organisaatio mocks/Oppilaitos2)
    (fixture/add-hakukohde-mock hakukohdeOid2 toteutusOid4 "1.2.246.562.29.0000001" :tila "julkaistu" :nimi "Hakukohde" :valintaperuste valintaperusteId1)
    (fixture/add-hakukohde-mock hakukohdeOid3 toteutusOid2 "1.2.246.562.29.0000001" :tila "julkaistu" :nimi "autoalan hakukohde" :valintaperuste valintaperusteId1 :modified "2018-05-05T12:02:23" :muokkaaja "1.2.246.562.24.55555555555")
    (fixture/add-hakukohde-mock hakukohdeOid4 toteutusOid5 "1.2.246.562.29.0000001" :tila "arkistoitu" :nimi "Autoalan hakukohde" :valintaperuste valintaperusteId1 :modified "2018-06-05T12:02:23")
    (fixture/add-hakukohde-mock hakukohdeOid5 toteutusOid5 "1.2.246.562.29.0000001" :tila "tallennettu" :nimi "Autoalan hakukohde" :valintaperuste valintaperusteId1 :modified "2018-06-05T12:02:23")

    (fixture/add-koulutus-mock "1.2.246.562.13.0000001" :tila "julkaistu" :nimi "Hauska koulutus" :sorakuvausId sorakuvausId)
    (fixture/add-haku-mock "1.2.246.562.29.0000001" :tila "julkaistu"   :nimi "Yhteishaku")

    (fixture/add-sorakuvaus-mock sorakuvausId :tila "julkaistu" :nimi "Kiva SORA-kuvaus")
    (fixture/add-valintaperuste-mock valintaperusteId1 :tila "julkaistu" :nimi "Valintaperustekuvaus")

    (fixture/index-oids-without-related-indices {:hakukohteet [hakukohdeOid1 hakukohdeOid2 hakukohdeOid3 hakukohdeOid4 hakukohdeOid5]})

    (testing "Filter hakukohde"
      (testing "by organisaatio"
        (let [oids (get-200-oids (hakukohde-url hakukohdeOid1))]
          (is (= [hakukohdeOid1] oids))))
      (testing "by oid"
        (let [oids (get-200-oids (str (hakukohde-url) "&nimi=" hakukohdeOid2))]
          (is (= [hakukohdeOid2] oids))))
      (testing "by muokkaajan oid"
        (let [oids (get-200-oids (str (hakukohde-url) "&muokkaaja=1.2.246.562.24.55555555555"))]
          (is (= [hakukohdeOid3] oids))))
      (testing "by tila"
        (let [oids (get-200-oids (str (hakukohde-url) "&tila=tallennettu"))]
          (is (= [hakukohdeOid5] oids))))
      (testing "ei arkistoidut"
        (let [oids (get-200-oids (str (hakukohde-url) "&arkistoidut=false"))]
          (is (= [hakukohdeOid3 hakukohdeOid5 hakukohdeOid2] oids))))
      (testing "monella arvolla"
        (let [oids (get-200-oids (str (hakukohde-url) "&tila=julkaistu&muokkaaja=1.2.246.562.24.55555555555"))]
          (is (= [hakukohdeOid3] oids)))))

    (testing "Sort hakukohde result"
      (testing "by tila asc"
        (let [oids (get-200-oids (str (hakukohde-url) "&order-by=tila&order=asc"))]
          (is (= [hakukohdeOid4 hakukohdeOid3 hakukohdeOid2 hakukohdeOid5] oids))))
      (testing "by tila desc"
        (let [oids (get-200-oids (str (hakukohde-url) "&order-by=tila&order=desc"))]
          (is (= [hakukohdeOid5 hakukohdeOid3 hakukohdeOid2 hakukohdeOid4] oids))))
      (testing "by modified asc"
        (let [oids (get-200-oids (str (hakukohde-url) "&tila=julkaistu&order-by=modified&order=asc"))]
          (is (= [hakukohdeOid3 hakukohdeOid2] oids))))
      (testing "by modified desc"
        (let [oids (get-200-oids (str (hakukohde-url) "&tila=julkaistu&order-by=modified&order=desc"))]
          (is (= [hakukohdeOid2 hakukohdeOid3] oids))))
      (testing "by nimi asc"
        (let [oids (get-200-oids (str (hakukohde-url) "&tila=julkaistu&order-by=nimi&order=asc"))]
          (is (= [hakukohdeOid3 hakukohdeOid2] oids))))
      (testing "by nimi desc"
        (let [oids (get-200-oids (str (hakukohde-url) "&tila=julkaistu&order-by=nimi&order=desc"))]
          (is (= [hakukohdeOid2 hakukohdeOid3] oids))))
      (comment testing "by muokkaaja asc"                 ;TODO: muokkaajan nimi onr:stä / nimen mockaus
               (let [oids (get-200-oids (str (hakukohde-url) "&tila=julkaistu&order-by=muokkaaja&order=asc"))]
                 (is (= [koulutusOid2 koulutusOid3] oids))))
      (comment testing "by muokkaaja desc"                ;TODO: muokkaajan nimi onr:stä / nimen mockaus
               (let [oids (get-200-oids (str (hakukohde-url) "&tila=julkaistu&order-by=muokkaaja&order=desc"))]
                 (is (= [koulutusOid3 koulutusOid2] oids)))))

    (testing "Page hakukohde result"
      (testing "return first page"
        (let [oids (get-200-oids (str (hakukohde-url) "&page=1&size=2&order-by=tila"))]
          (is (= [hakukohdeOid4 hakukohdeOid3] oids))))
      (testing "return first page when negative page"
        (let [oids (get-200-oids (str (hakukohde-url) "&page=-1&size=2&order-by=tila"))]
          (is (= [hakukohdeOid4 hakukohdeOid3] oids))))
      (testing "return second page"
        (let [oids (get-200-oids (str (hakukohde-url) "&page=2&size=2&order-by=tila"))]
          (is (= [hakukohdeOid2 hakukohdeOid5] oids))))
      (testing "return empty page when none left"
        (let [oids (get-200-oids (str (hakukohde-url) "&page=3&size=2&order-by=tila"))]
          (is (= [] oids)))))

    (testing "Hakukohde result contain proper fields"
      (let [res (get-200 (str (hakukohde-url) "&nimi=" hakukohdeOid3))]
        (is (= 1 (:value (:totalCount res))))
        (let [hakukohde (first (:result res))
              muokkaaja (:nimi (:muokkaaja hakukohde))]    ;TODO: muokkaajan nimi onr:stä / nimen mockaus
          (is (= {:oid hakukohdeOid3
                  :tila "julkaistu"
                  :nimi { :fi "autoalan hakukohde fi"
                         :sv "autoalan hakukohde sv" }
                  :organisaatio { :oid mocks/Oppilaitos1
                                 :nimi { :fi "Kiva ammattikorkeakoulu"
                                        :sv "Kiva ammattikorkeakoulu sv" }
                                 :paikkakunta { :koodiUri "kunta_091"
                                               :nimi { :fi "kunta_091 nimi fi"
                                                      :sv "kunta_091 nimi sv" }}}
                  :muokkaaja { :oid "1.2.246.562.24.55555555555"
                              :nimi muokkaaja }
                  :modified "2018-05-05T12:02:23"} hakukohde)))))))
