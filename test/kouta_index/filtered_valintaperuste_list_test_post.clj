(ns kouta-index.filtered-valintaperuste-list-test-post
  (:require [clojure.test :refer :all]
            [kouta-index.api :refer :all]
            [ring.mock.request :as mock]
            [clj-test-utils.elasticsearch-mock-utils :as utils]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-index.test-tools :as tools]
            [kouta-indeksoija-service.fixture.external-services :as mocks]))

(intern 'clj-log.access-log 'service "kouta-index")

(use-fixtures :each utils/mock-embedded-elasticsearch-fixture fixture/mock-indexing-fixture tools/mock-organisaatio)

(defn post-200
  [ids params]
  (let [url      (str "/kouta-index/valintaperuste/filtered-list" params)
        response (app (-> (mock/request :post url)
                          (mock/json-body ids)))]
    (is (= (:status response) 200))
    (fixture/->keywordized-json (slurp (:body response)))))

(defn post-200-ids
  ([ids params] (map #(:id %) (:result (post-200 ids params))))
  ([ids] (post-200-ids ids "")))

(deftest valintaperuste-list-empty-index-test
  (testing "search in empty index"
    (post-200-ids ["31972648-ebb7-4185-ac64-31fa6b841e34"]))
  (testing "search in empty index sort by nimi"
    (post-200-ids ["31972648-ebb7-4185-ac64-31fa6b841e34"] "?order-by=nimi"))
  (testing "search in empty index sort by tila"
    (post-200-ids ["31972648-ebb7-4185-ac64-31fa6b841e34"] "?order-by=tila"))
  (testing "search in empty index sort by muokkaaja"
    (post-200-ids ["31972648-ebb7-4185-ac64-31fa6b841e34"] "?order-by=muokkaaja"))
  (testing "search in empty index sort by modified"
    (post-200-ids ["31972648-ebb7-4185-ac64-31fa6b841e34"] "?order-by=modified")))

(deftest filtered-valintaperuste-list-test

  (let [valintaperusteId1 "31972648-ebb7-4185-ac64-31fa6b841e34"
        valintaperusteId2 "31972648-ebb7-4185-ac64-31fa6b841e35"
        valintaperusteId3 "31972648-ebb7-4185-ac64-31fa6b841e36"
        valintaperusteId4 "31972648-ebb7-4185-ac64-31fa6b841e37"
        valintaperusteId5 "31972648-ebb7-4185-ac64-31fa6b841e38"
        sorakuvausId      "31972648-ebb7-4185-ac64-31fa6b841e39"
        defaultIds        [valintaperusteId2 valintaperusteId3 valintaperusteId4 valintaperusteId5]]

    (fixture/add-valintaperuste-mock valintaperusteId1 :tila "julkaistu" :nimi "Valintaperustekuvaus" :sorakuvaus sorakuvausId :organisaatio mocks/Oppilaitos2)
    (fixture/add-valintaperuste-mock valintaperusteId2 :tila "julkaistu" :nimi "Valintaperustekuvaus" :sorakuvaus sorakuvausId)
    (fixture/add-valintaperuste-mock valintaperusteId3 :tila "julkaistu" :nimi "Kiva valintaperustekuvaus" :sorakuvaus sorakuvausId :modified "2018-05-05T12:02" :muokkaaja "1.2.246.562.24.55555555555")
    (fixture/add-valintaperuste-mock valintaperusteId4 :tila "arkistoitu" :nimi "Kiva valintaperustekuvaus" :sorakuvaus sorakuvausId :modified "2018-06-05T12:02")
    (fixture/add-valintaperuste-mock valintaperusteId5 :tila "tallennettu" :nimi "Kiva valintaperustekuvaus" :sorakuvaus sorakuvausId :modified "2018-06-05T12:02")

    (fixture/add-sorakuvaus-mock sorakuvausId :tila "julkaistu" :nimi "Kiva SORA-kuvaus")

    (fixture/index-oids-without-related-indices {:valintaperusteet [valintaperusteId1 valintaperusteId2 valintaperusteId3 valintaperusteId4 valintaperusteId5]})

    (testing "Filter valintaperuste"
      (testing "by organisaatio"
        (let [ids (post-200-ids [valintaperusteId1])]
          (is (= [valintaperusteId1] ids))))
      (testing "by oid"
        (let [ids (post-200-ids defaultIds (str "?nimi=" valintaperusteId2))]
          (is (= [valintaperusteId2] ids))))
      (testing "by muokkaajan oid"
        (let [ids (post-200-ids defaultIds "?muokkaaja=1.2.246.562.24.55555555555")]
          (is (= [valintaperusteId3] ids))))
      (testing "by tila"
        (let [ids (post-200-ids defaultIds "?tila=tallennettu")]
          (is (= [valintaperusteId5] ids))))
      (comment testing "julkinen"
        (fixture/update-valintaperuste-mock valintaperusteId2 :julkinen "true")
        (fixture/index-oids-without-related-indices {:valintaperusteet [valintaperusteId2]})
        (let [oids (post-200-ids (valintaperuste-url mocks/Oppilaitos2))]
          (is (= [valintaperusteId1 valintaperusteId2] oids)))
        (fixture/update-valintaperuste-mock valintaperusteId2 :julkinen "true")
        (fixture/index-oids-without-related-indices {:valintaperusteet [valintaperusteId2]}))
      (testing "ei arkistoidut"
        (let [ids (post-200-ids defaultIds "?arkistoidut=false")]
          (is (= [valintaperusteId3 valintaperusteId5 valintaperusteId2] ids))))
      (testing "monella arvolla"
        (let [ids (post-200-ids defaultIds "?tila=julkaistu&muokkaaja=1.2.246.562.24.55555555555")]
          (is (= [valintaperusteId3] ids)))))

    (testing "Sort haku result"
      (testing "by tila asc"
        (let [ids (post-200-ids defaultIds "?order-by=tila&order=asc")]
          (is (= [valintaperusteId4 valintaperusteId3 valintaperusteId2 valintaperusteId5] ids))))
      (testing "by tila desc"
        (let [ids (post-200-ids defaultIds "?order-by=tila&order=desc")]
          (is (= [valintaperusteId5 valintaperusteId3 valintaperusteId2 valintaperusteId4] ids))))
      (testing "by modified asc"
        (let [ids (post-200-ids defaultIds "?tila=julkaistu&order-by=modified&order=asc")]
          (is (= [valintaperusteId3 valintaperusteId2] ids))))
      (testing "by modified desc"
        (let [ids (post-200-ids defaultIds "?tila=julkaistu&order-by=modified&order=desc")]
          (is (= [valintaperusteId2 valintaperusteId3] ids))))
      (testing "by nimi asc"
        (let [ids (post-200-ids defaultIds "?tila=julkaistu&order-by=nimi&order=asc")]
          (is (= [valintaperusteId3 valintaperusteId2] ids))))
      (testing "by nimi desc"
        (let [ids (post-200-ids defaultIds "?tila=julkaistu&order-by=nimi&order=desc")]
          (is (= [valintaperusteId2 valintaperusteId3] ids))))
      (comment testing "by hakukohde count asc"             ;TODO
        (let [ids (post-200-ids defaultIds "?order-by=hakukohteet&order=asc")]
          (is (= [valintaperusteId4 valintaperusteId5 valintaperusteId2 valintaperusteId3] ids))))
      (comment testing "by hakukohde count desc"            ;TODO
        (let [ids (post-200-ids defaultIds "?order-by=hakukohteet&order=desc")]
          (is (= [valintaperusteId3 valintaperusteId2 valintaperusteId4 valintaperusteId5] ids))))
      (comment testing "by muokkaaja asc"                 ;TODO: muokkaajan nimi onr:stä / nimen mockaus
               (let [ids (post-200-ids defaultIds "?tila=julkaistu&order-by=muokkaaja&order=asc")]
                 (is (= [koulutusOid2 koulutusOid3] ids))))
      (comment testing "by muokkaaja desc"                ;TODO: muokkaajan nimi onr:stä / nimen mockaus
               (let [ids (post-200-ids defaultIds "?tila=julkaistu&order-by=muokkaaja&order=desc")]
                 (is (= [koulutusOid3 koulutusOid2] ids)))))

    (testing "Page valintaperuste result"
      (testing "return first page"
        (let [ids (post-200-ids defaultIds "?page=1&size=2&order-by=tila")]
          (is (= [valintaperusteId4 valintaperusteId3] ids))))
      (testing "return first page when negative page"
        (let [ids (post-200-ids defaultIds "?page=-1&size=2&order-by=tila")]
          (is (= [valintaperusteId4 valintaperusteId3] ids))))
      (testing "return second page"
        (let [ids (post-200-ids defaultIds "?page=2&size=2&order-by=tila")]
          (is (= [valintaperusteId2 valintaperusteId5] ids))))
      (testing "return empty page when none left"
        (let [ids (post-200-ids defaultIds "?page=3&size=2&order-by=tila")]
          (is (= [] ids)))))

    (testing "haku valintaperuste contain proper fields"
      (let [res (post-200 defaultIds (str "?nimi=" valintaperusteId3))]
        (is (= 1 (:totalCount res)))
        (let [valintaperuste (first (:result res))
              muokkaaja (:nimi (:muokkaaja valintaperuste))]    ;TODO: muokkaajan nimi onr:stä / nimen mockaus
          (is (= {:id valintaperusteId3
                  :tila "julkaistu"
                  :nimi { :fi "Kiva valintaperustekuvaus fi"
                          :sv "Kiva valintaperustekuvaus sv" }
                  :organisaatio { :oid mocks/Oppilaitos1
                                  :nimi { :fi "Kiva ammattikorkeakoulu"
                                          :sv "Kiva ammattikorkeakoulu sv" }
                                  :paikkakunta { :koodiUri "kunta_091"
                                                 :nimi { :fi "kunta_091 nimi fi"
                                                         :sv "kunta_091 nimi sv" }}}
                  :muokkaaja { :oid "1.2.246.562.24.55555555555"
                               :nimi muokkaaja }
                  :modified "2018-05-05T12:02"} valintaperuste)))))))
