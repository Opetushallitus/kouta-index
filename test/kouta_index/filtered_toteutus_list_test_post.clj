(ns kouta-index.filtered-toteutus-list-test-post
  (:require [clojure.test :refer :all]
            [kouta-index.api :refer :all]
            [ring.mock.request :as mock]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [kouta-index.test-tools :as tools]
            [kouta-index.util.tools :refer [contains-many?]]
            [kouta-indeksoija-service.fixture.external-services :as mocks]))

(intern 'clj-log.access-log 'service "kouta-index")

(use-fixtures :each fixture/mock-indexing-fixture tools/mock-organisaatio)

(defn post-200
  [oids params]
  (let [url      (str "/kouta-index/toteutus/filtered-list" params)
        response (app (-> (mock/request :post url)
                          (mock/json-body oids)))]
    (is (= (:status response) 200))
    (fixture/->keywordized-json (slurp (:body response)))))

(defn post-200-oids
  ([oids params] (map #(:oid %) (:result (post-200 oids params))))
  ([oids] (post-200-oids oids "")))

(deftest toteutus-list-empty-index-test
  (testing "search in empty index"
    (post-200-oids ["1.2.246.562.17.000001"]))
  (testing "search in empty index sort by nimi"
    (post-200-oids ["1.2.246.562.17.000001"] "?order-by=nimi"))
  (testing "search in empty index sort by tila"
    (post-200-oids ["1.2.246.562.17.000001"] "?order-by=tila"))
  (testing "search in empty index sort by muokkaaja"
    (post-200-oids ["1.2.246.562.17.000001"] "?order-by=muokkaaja"))
  (testing "search in empty index sort by modified"
    (post-200-oids ["1.2.246.562.17.000001"] "?order-by=modified")))

(deftest filtered-toteutus-list-test

  (let [hakuOid1      "1.2.246.562.29.000001"
        koulutusOid1  "1.2.246.562.13.000001"
        toteutusOid1  "1.2.246.562.17.000001"
        toteutusOid2  "1.2.246.562.17.000002"
        toteutusOid3  "1.2.246.562.17.000003"
        toteutusOid4  "1.2.246.562.17.000004"
        toteutusOid5  "1.2.246.562.17.000005"
        defaultOids   [toteutusOid2 toteutusOid3 toteutusOid4 toteutusOid5]
        hakukohdeOid1 "1.2.246.562.20.000001"
        hakukohdeOid2 "1.2.246.562.20.000002"
        valintaperusteId1 "2d0651b7-cdd3-463b-80d9-303a60d9616c"]

    (defn toteutus-url
      ([oids] (str "/kouta-index/toteutus/filtered-list?oids=" oids))
      ([] (toteutus-url (str toteutusOid2 "," toteutusOid3 "," toteutusOid4 "," toteutusOid5))))

    (fixture/add-toteutus-mock toteutusOid1 koulutusOid1 :tila "julkaistu"   :nimi "Automaatioalan perusopinnot" :organisaatio mocks/Oppilaitos1)
    (fixture/add-toteutus-mock toteutusOid2 koulutusOid1 :tila "julkaistu"   :nimi "Automatiikan perusopinnot")
    (fixture/add-toteutus-mock toteutusOid3 koulutusOid1 :tila "julkaistu"   :nimi "Autoalan perusopinnot" :modified "2018-05-05T12:02:23" :muokkaaja "1.2.246.562.24.55555555555")
    (fixture/add-toteutus-mock toteutusOid4 koulutusOid1 :tila "arkistoitu"  :nimi "Autoalan perusopinnot" :modified "2018-06-05T12:02:23")
    (fixture/add-toteutus-mock toteutusOid5 koulutusOid1 :tila "tallennettu" :nimi "Autoalan perusopinnot" :modified "2018-06-05T12:02:23")

    (fixture/add-haku-mock hakuOid1 :tila "julkaistu" :organisaatio mocks/Oppilaitos1)

    (fixture/add-hakukohde-mock hakukohdeOid1 toteutusOid1 hakuOid1 :tila "julkaistu" :nimi "Hakukohde" :organisaatio mocks/Oppilaitos1 :valintaperuste valintaperusteId1)
    (fixture/add-hakukohde-mock hakukohdeOid2 toteutusOid1 hakuOid1 :tila "tallennettu" :nimi "Hakukohde" :organisaatio mocks/Oppilaitos1 :valintaperuste valintaperusteId1)

    (fixture/index-oids-without-related-indices {:toteutukset [toteutusOid1 toteutusOid2 toteutusOid3 toteutusOid4 toteutusOid5]})

    (testing "Filter toteutus"
      (testing "by organisaatio"
        (let [oids (post-200-oids [toteutusOid1])]
          (is (= [toteutusOid1] oids))))
      (testing "by oid"
        (let [oids (post-200-oids defaultOids (str "?nimi=" toteutusOid2))]
          (is (= [toteutusOid2] oids))))
      (testing "by muokkaajan oid"
        (let [oids (post-200-oids defaultOids "?muokkaaja=1.2.246.562.24.55555555555")]
          (is (= [toteutusOid3] oids))))
      (testing "by tila"
        (let [oids (post-200-oids defaultOids "?tila=tallennettu")]
          (is (= [toteutusOid5] oids))))
      (testing "ei arkistoidut"
        (let [oids (post-200-oids defaultOids "?arkistoidut=false")]
          (is (= [toteutusOid3 toteutusOid5 toteutusOid2] oids))))
      (testing "monella arvolla"
        (let [oids (post-200-oids defaultOids "?tila=julkaistu&muokkaaja=1.2.246.562.24.55555555555")]
          (is (= [toteutusOid3] oids)))))

    (testing "Sort toteutus result"
      (testing "by tila asc"
        (let [oids (post-200-oids defaultOids "?order-by=tila&order=asc")]
          (is (= [toteutusOid4 toteutusOid3 toteutusOid2 toteutusOid5] oids))))
      (testing "by tila desc"
        (let [oids (post-200-oids defaultOids "?order-by=tila&order=desc")]
          (is (= [toteutusOid5 toteutusOid3 toteutusOid2 toteutusOid4] oids))))
      (testing "by modified asc"
        (let [oids (post-200-oids defaultOids "?tila=julkaistu&order-by=modified&order=asc")]
          (is (= [toteutusOid3 toteutusOid2] oids))))
      (testing "by modified desc"
        (let [oids (post-200-oids defaultOids "?tila=julkaistu&order-by=modified&order=desc")]
          (is (= [toteutusOid2 toteutusOid3] oids))))
      (testing "by nimi asc"
        (let [oids (post-200-oids defaultOids "?tila=julkaistu&order-by=nimi&order=asc")]
          (is (= [toteutusOid3 toteutusOid2] oids))))
      (testing "by nimi desc"
        (let [oids (post-200-oids defaultOids "?tila=julkaistu&order-by=nimi&order=desc")]
          (is (= [toteutusOid2 toteutusOid3] oids))))
      (comment testing "by hakukohde count asc"
        (let [oids (post-200-oids defaultOids "?order-by=hakukohteet&order=asc")]
          (is (= [toteutusOid3 toteutusOid2 toteutusOid4 toteutusOid5] oids))))
      (comment testing "by hakukohde count desc"
        (let [oids (post-200-oids defaultOids "?order-by=hakukohteet&order=desc")]
          (is (= [toteutusOid5 toteutusOid4 toteutusOid3 toteutusOid2] oids))))
      (comment testing "by muokkaaja asc"                 ;TODO: muokkaajan nimi onr:stä / nimen mockaus
               (let [oids (post-200-oids defaultOids "?tila=julkaistu&order-by=muokkaaja&order=asc")]
                 (is (= [koulutusOid2 koulutusOid3] oids))))
      (comment testing "by muokkaaja desc"                ;TODO: muokkaajan nimi onr:stä / nimen mockaus
               (let [oids (post-200-oids defaultOids "?tila=julkaistu&order-by=muokkaaja&order=desc")]
                 (is (= [koulutusOid3 koulutusOid2] oids)))))

    (testing "Page toteutus result"
      (testing "return first page"
        (let [oids (post-200-oids defaultOids "?page=1&size=2&order-by=tila")]
          (is (= [toteutusOid4 toteutusOid3] oids))))
      (testing "return first page when negative page"
        (let [oids (post-200-oids defaultOids "?page=-1&size=2&order-by=tila")]
          (is (= [toteutusOid4 toteutusOid3] oids))))
      (testing "return second page"
        (let [oids (post-200-oids defaultOids "?page=2&size=2&order-by=tila")]
          (is (= [toteutusOid2 toteutusOid5] oids))))
      (testing "return empty page when none left"
        (let [oids (post-200-oids defaultOids "?page=3&size=2&order-by=tila")]
          (is (= [] oids)))))

      (testing "Toteutus result contains hakutieto hakukohde organisaatio and tila"
        (let [res (post-200 [toteutusOid1] "")]
          (is (= 1 (:totalCount res)))
          (is (= 1 (-> res :result (first) :hakutiedot (count))))
          (is (= 2 (-> res :result (first) :hakutiedot (first) :hakukohteet (count))))
          (is (->>     res :result (first) :hakutiedot (first) :hakukohteet (every? #(contains-many? % :organisaatioOid :tila))))))

      (testing "Toteutus result contain proper fields"
        (let [res (post-200 defaultOids (str "?nimi=" toteutusOid3))]
          (is (= 1 (:totalCount res)))
          (let [toteutus (first (:result res))
                muokkaaja (:nimi (:muokkaaja toteutus))]    ;TODO: muokkaajan nimi onr:stä / nimen mockaus
            (is (= {:oid toteutusOid3
                    :tila "julkaistu"
                    :nimi { :fi "Autoalan perusopinnot fi"
                           :sv "Autoalan perusopinnot sv" }
                    :organisaatio {:oid mocks/Oppilaitos1
                                   :nimi { :fi "Kiva ammattikorkeakoulu"
                                          :sv "Kiva ammattikorkeakoulu sv" }
                                   :paikkakunta { :koodiUri "kunta_091"
                                                 :nimi { :fi "kunta_091 nimi fi"
                                                        :sv "kunta_091 nimi sv" }}}
                    :organisaatiot ["1.2.246.562.10.54545454545" "1.2.246.562.10.67476956288" "1.2.246.562.10.594252633210"]
                    :muokkaaja {:oid "1.2.246.562.24.55555555555"
                                :nimi muokkaaja }
                    :modified "2018-05-05T12:02:23"} toteutus)))))))

