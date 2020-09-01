(ns kouta-index.filtered-haku-list-test
  (:require [clojure.test :refer :all]
            [kouta-index.api :refer :all]
            [ring.mock.request :as mock]
            [clj-test-utils.elasticsearch-mock-utils :as utils]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-index.test-tools :as tools]
            [kouta-indeksoija-service.fixture.external-services :as mocks]))

(intern 'clj-log.access-log 'service "kouta-index")

(use-fixtures :each utils/mock-embedded-elasticsearch-fixture fixture/mock-indexing-fixture tools/mock-organisaatio)

(defn get-200
  [url]
  (let [response (app (mock/request :get url))]
    (is (= (:status response) 200))
    (fixture/->keywordized-json (slurp (:body response)))))

(defn get-200-oids
  [url]
  (map #(:oid %) (:result (get-200 url))))

(deftest haku-list-empty-index-test
  (testing "search in empty index"
    (get-200-oids "/kouta-index/haku/filtered-list?oids=1.2.246.562.29.000001"))
  (testing "search in empty index sort by nimi"
    (get-200-oids "/kouta-index/haku/filtered-list?oids=1.2.246.562.29.000001&order-by=nimi"))
  (testing "search in empty index sort by tila"
    (get-200-oids "/kouta-index/haku/filtered-list?oids=1.2.246.562.29.000001&order-by=tila"))
  (testing "search in empty index sort by muokkaaja"
    (get-200-oids "/kouta-index/haku/filtered-list?oids=1.2.246.562.29.000001&order-by=muokkaaja"))
  (testing "search in empty index sort by modified"
    (get-200-oids "/kouta-index/haku/filtered-list?oids=1.2.246.562.29.000001&order-by=modified")))

(deftest filtered-haku-list-test

  (let [hakuOid1 "1.2.246.562.29.0000001"
        hakuOid2 "1.2.246.562.29.0000002"
        hakuOid3 "1.2.246.562.29.0000003"
        hakuOid4 "1.2.246.562.29.0000004"
        hakuOid5 "1.2.246.562.29.0000005"]

    (defn haku-url
      ([oids] (str "/kouta-index/haku/filtered-list?oids=" oids))
      ([] (haku-url (str hakuOid2 "," hakuOid3 "," hakuOid4 "," hakuOid5))))

    (fixture/add-haku-mock hakuOid1 :tila "julkaistu"   :nimi "Yhteishaku" :organisaatio mocks/Oppilaitos2)
    (fixture/add-haku-mock hakuOid2 :tila "julkaistu"   :nimi "Yhteishaku")
    (fixture/add-haku-mock hakuOid3 :tila "julkaistu"   :nimi "Jatkuva haku" :modified "2018-05-05T12:02:23" :muokkaaja "1.2.246.562.24.55555555555")
    (fixture/add-haku-mock hakuOid4 :tila "arkistoitu"  :nimi "Jatkuva haku" :modified "2018-06-05T12:02:23")
    (fixture/add-haku-mock hakuOid5 :tila "tallennettu" :nimi "Jatkuva haku" :modified "2018-06-05T12:02:23")

    ;(fixture/add-hakukohde-mock "1.2.246.562.20.000001" "1.2.246.562.17.000001" hakuOid2 :valintaperuste "31972648-ebb7-4185-ac64-31fa6b841e34")
    ;(fixture/add-hakukohde-mock "1.2.246.562.20.000002" "1.2.246.562.17.000002" hakuOid3 :valintaperuste "31972648-ebb7-4185-ac64-31fa6b841e34")
    ;(fixture/add-hakukohde-mock "1.2.246.562.20.000003" "1.2.246.562.17.000002" hakuOid3 :valintaperuste "31972648-ebb7-4185-ac64-31fa6b841e34")
    ;fixture/add-hakukohde-mock "1.2.246.562.20.000004" "1.2.246.562.17.000002" hakuOid3 :valintaperuste "31972648-ebb7-4185-ac64-31fa6b841e34")

    ;(fixture/add-toteutus-mock "1.2.246.562.17.000001" "1.2.246.562.13.000001" :tila "julkaistu")
    ;(fixture/add-toteutus-mock "1.2.246.562.17.000002" "1.2.246.562.13.000002" :tila "julkaistu")

    ;(fixture/add-valintaperuste-mock "31972648-ebb7-4185-ac64-31fa6b841e34")

    (fixture/index-oids-without-related-indices {:haut [hakuOid1 hakuOid2 hakuOid3 hakuOid4 hakuOid5]})

    (testing "Filter haku"
      (testing "by organisaatio"
        (let [oids (get-200-oids (haku-url hakuOid1))]
          (is (= [hakuOid1] oids))))
      (testing "by oid"
        (let [oids (get-200-oids (str (haku-url) "&nimi=" hakuOid2))]
          (is (= [hakuOid2] oids))))
      (testing "by muokkaajan oid"
        (let [oids (get-200-oids (str (haku-url) "&muokkaaja=1.2.246.562.24.55555555555"))]
          (is (= [hakuOid3] oids))))
      (testing "by tila"
        (let [oids (get-200-oids (str (haku-url) "&tila=tallennettu"))]
          (is (= [hakuOid5] oids))))
      (testing "ei arkistoidut"
        (let [oids (get-200-oids (str (haku-url) "&arkistoidut=false"))]
          (is (= [hakuOid3 hakuOid5 hakuOid2] oids))))
      (testing "monella arvolla"
        (let [oids (get-200-oids (str (haku-url) "&tila=julkaistu&muokkaaja=1.2.246.562.24.55555555555"))]
          (is (= [hakuOid3] oids)))))

    (testing "Sort haku result"
      (testing "by tila asc"
        (let [oids (get-200-oids (str (haku-url) "&order-by=tila&order=asc"))]
          (is (= [hakuOid4 hakuOid3 hakuOid2 hakuOid5] oids))))
      (testing "by tila desc"
        (let [oids (get-200-oids (str (haku-url) "&order-by=tila&order=desc"))]
          (is (= [hakuOid5 hakuOid3 hakuOid2 hakuOid4] oids))))
      (testing "by modified asc"
        (let [oids (get-200-oids (str (haku-url) "&tila=julkaistu&order-by=modified&order=asc"))]
          (is (= [hakuOid3 hakuOid2] oids))))
      (testing "by modified desc"
        (let [oids (get-200-oids (str (haku-url) "&tila=julkaistu&order-by=modified&order=desc"))]
          (is (= [hakuOid2 hakuOid3] oids))))
      (testing "by nimi asc"
        (let [oids (get-200-oids (str (haku-url) "&tila=julkaistu&order-by=nimi&order=asc"))]
          (is (= [hakuOid3 hakuOid2] oids))))
      (testing "by nimi desc"
        (let [oids (get-200-oids (str (haku-url) "&tila=julkaistu&order-by=nimi&order=desc"))]
          (is (= [hakuOid2 hakuOid3] oids))))
      (comment testing "by hakukohde count asc"
               (let [oids (get-200-oids (str (haku-url) "&order-by=hakukohteet&order=asc"))]
                 (is (= [hakuOid4 hakuOid5 hakuOid2 hakuOid3] oids))))
      (comment testing "by hakukohde count desc"
               (let [oids (get-200-oids (str (haku-url) "&order-by=hakukohteet&order=desc"))]
                 (is (= [hakuOid3 hakuOid2 hakuOid4 hakuOid5] oids))))
      (comment testing "by muokkaaja asc"                 ;TODO: muokkaajan nimi onr:stä / nimen mockaus
               (let [oids (get-200-oids (str (haku-url) "&tila=julkaistu&order-by=muokkaaja&order=asc"))]
                 (is (= [koulutusOid2 koulutusOid3] oids))))
      (comment testing "by muokkaaja desc"                ;TODO: muokkaajan nimi onr:stä / nimen mockaus
               (let [oids (get-200-oids (str (haku-url) "&tila=julkaistu&order-by=muokkaaja&order=desc"))]
                 (is (= [koulutusOid3 koulutusOid2] oids)))))

    (testing "Page haku result"
      (testing "return first page"
        (let [oids (get-200-oids (str (haku-url) "&page=1&size=2&order-by=tila"))]
          (is (= [hakuOid4 hakuOid3] oids))))
      (testing "return first page when negative page"
        (let [oids (get-200-oids (str (haku-url) "&page=-1&size=2&order-by=tila"))]
          (is (= [hakuOid4 hakuOid3] oids))))
      (testing "return second page"
        (let [oids (get-200-oids (str (haku-url) "&page=2&size=2&order-by=tila"))]
          (is (= [hakuOid2 hakuOid5] oids))))
      (testing "return empty page when none left"
        (let [oids (get-200-oids (str (haku-url) "&page=3&size=2&order-by=tila"))]
          (is (= [] oids)))))

    (testing "haku result contain proper fields"
      (let [res (get-200 (str (haku-url) "&nimi=" hakuOid3))]
        (is (= 1 (:totalCount res)))
        (let [haku (first (:result res))
              muokkaaja (:nimi (:muokkaaja haku))]    ;TODO: muokkaajan nimi onr:stä / nimen mockaus
          (is (= {:oid hakuOid3
                  :tila "julkaistu"
                  :nimi { :fi "Jatkuva haku fi"
                         :sv "Jatkuva haku sv" }
                  :organisaatio { :oid mocks/Oppilaitos1
                                 :nimi { :fi "Kiva ammattikorkeakoulu"
                                        :sv "Kiva ammattikorkeakoulu sv" }
                                 :paikkakunta { :koodiUri "kunta_091"
                                               :nimi { :fi "kunta_091 nimi fi"
                                                      :sv "kunta_091 nimi sv" }}}
                  :muokkaaja { :oid "1.2.246.562.24.55555555555"
                              :nimi muokkaaja }
                  :modified "2018-05-05T12:02:23"} haku)))))))
