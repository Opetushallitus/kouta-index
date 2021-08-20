(ns kouta-index.filtered-koulutus-list-test-post
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

(defn post-200
  [oids params]
  (let [url      (str "/kouta-index/koulutus/filtered-list" params)
        response (app (-> (mock/request :post url)
                          (mock/json-body oids)))]
    (is (= (:status response) 200))
    (fixture/->keywordized-json (slurp (:body response)))))

(defn post-200-oids
  ([oids params] (map #(:oid %) (:result (post-200 oids params))))
  ([oids] (post-200-oids oids "")))

(deftest koulutus-list-empty-index-test
  (testing "search in empty index"
    (post-200-oids ["1.2.246.562.13.000001"] ""))
  (testing "search in empty index sort by nimi"
    (post-200-oids ["1.2.246.562.13.000001"] "?order-by=nimi"))
  (testing "search in empty index sort by tila"
    (post-200-oids ["1.2.246.562.13.000001"] "?order-by=tila"))
  (testing "search in empty index sort by muokkaaja"
    (post-200-oids ["1.2.246.562.13.000001"] "?order-by=muokkaaja"))
  (testing "search in empty index sort by modified"
    (post-200-oids ["1.2.246.562.13.000001"] "?order-by=modified")))

(deftest filtered-koulutus-list-test

  (let [koulutusOid1 "1.2.246.562.13.000001"
        koulutusOid2 "1.2.246.562.13.000002"
        koulutusOid3 "1.2.246.562.13.000003"
        koulutusOid4 "1.2.246.562.13.000004"
        koulutusOid5 "1.2.246.562.13.000005"
        defaultOids  [koulutusOid2 koulutusOid3 koulutusOid4 koulutusOid5]
        sorakuvausId "31972648-ebb7-4185-ac64-31fa6b841e39"]

    (fixture/add-sorakuvaus-mock sorakuvausId :tila "julkaistu" :nimi "Kiva SORA-kuvaus")

    (fixture/add-koulutus-mock koulutusOid1 :tila "julkaistu" :nimi "Hauska koulutus" :organisaatio mocks/Oppilaitos2 :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock koulutusOid2 :tila "julkaistu" :nimi "Tietojenkäsittelytieteen perusopinnot" :modified "2018-05-05T12:02:23" :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock koulutusOid3 :tila "julkaistu" :nimi "Tietotekniikan perusopinnot" :muokkaaja "1.2.246.562.24.55555555555" :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock koulutusOid4 :tila "arkistoitu" :nimi "Tietojenkäsittelytieteen perusopinnot" :sorakuvausId sorakuvausId)
    (fixture/add-koulutus-mock koulutusOid5 :tila "tallennettu" :nimi "Tietojenkäsittelytieteen perusopinnot" :sorakuvausId sorakuvausId)

    ;(fixture/add-toteutus-mock "1.2.246.562.17.000001" koulutusOid2 :tila "julkaistu")
    ;(fixture/add-toteutus-mock "1.2.246.562.17.000002" koulutusOid2 :tila "julkaistu")
    ;(fixture/add-toteutus-mock "1.2.246.562.17.000003" koulutusOid2 :tila "julkaistu")
    ;(fixture/add-toteutus-mock "1.2.246.562.17.000004" koulutusOid4 :tila "julkaistu")

    (fixture/index-oids-without-related-indices {:koulutukset [koulutusOid1 koulutusOid2 koulutusOid3 koulutusOid4 koulutusOid5]})

    (testing "Filter koulutus"
      (testing "by organisaatio"
        (let [oids (post-200-oids [koulutusOid1])]
          (is (= [koulutusOid1] oids))))
      (testing "by oid"
        (let [oids (post-200-oids defaultOids (str "?nimi=" koulutusOid2))]
          (is (= [koulutusOid2] oids))))
      (testing "by muokkaajan oid"
        (let [oids (post-200-oids defaultOids "?muokkaaja=1.2.246.562.24.55555555555")]
          (is (= [koulutusOid3] oids))))
      (testing "by tila"
        (let [oids (post-200-oids defaultOids "?tila=tallennettu")]
          (is (= [koulutusOid5] oids))))
      (comment testing "julkinen"
        (fixture/update-koulutus-mock koulutusOid2 :julkinen "true")
        (fixture/index-oids-without-related-indices {:koulutukset [koulutusOid2]})
        (let [oids (post-200-oids (koulutus-url koulutusOid1))]
          (is (= [koulutusOid1 koulutusOid2] oids)))
        (fixture/update-koulutus-mock koulutusOid2 :julkinen "true")
        (fixture/index-oids-without-related-indices {:koulutukset [koulutusOid2]}))
      (testing "ei arkistoidut"
        (let [oids (post-200-oids defaultOids "?arkistoidut=false&order-by=tila")]
          (is (= [koulutusOid2 koulutusOid3 koulutusOid5] oids))))
      (testing "monella arvolla"
        (let [oids (post-200-oids defaultOids "?tila=julkaistu&muokkaaja=1.2.246.562.24.55555555555")]
          (is (= [koulutusOid3] oids)))))

    (testing "Sort koulutus result"
      (testing "by tila asc"
        (let [oids (post-200-oids defaultOids "?order-by=tila&order=asc")]
          (is (= [koulutusOid4 koulutusOid2 koulutusOid3 koulutusOid5] oids))))
      (testing "by tila desc"
        (let [oids (post-200-oids defaultOids "?order-by=tila&order=desc")]
          (is (= [koulutusOid5 koulutusOid2 koulutusOid3 koulutusOid4] oids))))
      (testing "by modified asc"
        (let [oids (post-200-oids defaultOids "?tila=julkaistu&order-by=modified&order=asc")]
          (is (= [koulutusOid2 koulutusOid3] oids))))
      (testing "by modified desc"
        (let [oids (post-200-oids defaultOids "?tila=julkaistu&order-by=modified&order=desc")]
          (is (= [koulutusOid3 koulutusOid2] oids))))
      (testing "by nimi asc"
        (let [oids (post-200-oids defaultOids "?tila=julkaistu&order-by=nimi&order=asc")]
          (is (= [koulutusOid2 koulutusOid3] oids))))
      (testing "by desc desc"
        (let [oids (post-200-oids defaultOids "?tila=julkaistu&order-by=nimi&order=desc")]
          (is (= [koulutusOid3 koulutusOid2] oids))))
      (comment testing "by toteutus count asc"
        (let [oids (post-200-oids defaultOids "?order-by=toteutukset&order=asc")]
          (is (= [koulutusOid5 koulutusOid3 koulutusOid4 koulutusOid2] oids))))
      (comment testing "by toteutus count desc"
        (let [oids (post-200-oids defaultOids "?order-by=toteutukset&order=desc")]
          (is (= [koulutusOid2 koulutusOid4 koulutusOid5 koulutusOid3] oids))))
      (comment testing "by muokkaaja asc"                 ;TODO: muokkaajan nimi onr:stä / nimen mockaus
        (let [oids (post-200-oids defaultOids "?tila=julkaistu&order-by=muokkaaja&order=asc")]
          (is (= [koulutusOid2 koulutusOid3] oids))))
      (comment testing "by muokkaaja desc"                ;TODO: muokkaajan nimi onr:stä / nimen mockaus
        (let [oids (post-200-oids defaultOids "?tila=julkaistu&order-by=muokkaaja&order=desc")]
          (is (= [koulutusOid3 koulutusOid2] oids)))))

    (testing "Page koulutus result"
      (testing "return first page"
        (let [oids (post-200-oids defaultOids "?page=1&size=2&order-by=tila")]
          (is (= [koulutusOid4 koulutusOid2] oids))))
      (testing "return first page when negative page"
        (let [oids (post-200-oids defaultOids "?page=-1&size=2&order-by=tila")]
          (is (= [koulutusOid4 koulutusOid2] oids))))
      (testing "return second page"
        (let [oids (post-200-oids defaultOids "?page=2&size=2&order-by=tila")]
          (is (= [koulutusOid3 koulutusOid5] oids))))
      (testing "return empty page when none left"
        (let [oids (post-200-oids defaultOids "?page=3&size=2&order-by=tila")]
          (is (= [] oids)))))

    (testing "Koulutus result contain proper fields"
      (let [res (post-200 defaultOids (str "?nimi=" koulutusOid2))]
        (is (= 1 (:value (:totalCount res))))
        (let [koulutus (first (:result res))
              muokkaaja (:nimi (:muokkaaja koulutus))]    ;TODO: muokkaajan nimi onr:stä / nimen mockaus
          (is (= { :oid koulutusOid2
                   :tila "julkaistu"
                   :nimi { :fi "Tietojenkäsittelytieteen perusopinnot fi"
                           :sv "Tietojenkäsittelytieteen perusopinnot sv" }
                   :organisaatio { :oid mocks/Oppilaitos1
                                   :nimi { :fi "Kiva ammattikorkeakoulu"
                                           :sv "Kiva ammattikorkeakoulu sv" }
                                   :paikkakunta { :koodiUri "kunta_091"
                                                  :nimi { :fi "kunta_091 nimi fi"
                                                          :sv "kunta_091 nimi sv" }}}
                   :muokkaaja { :oid "1.2.246.562.24.10000000000"
                                :nimi muokkaaja }
                   :modified "2018-05-05T12:02:23"
                   :eperuste {:voimassaoloLoppuu "2018-01-01T00:00:00", :diaarinumero "1111-OPH-2021", :id 1234}} koulutus)))))


    (comment testing "Filter koulutus by nimi"              ;TODO Tämä eivät saisi feilata!
             (testing "by 'tieto'"
               (let [oids (post-200-oids defaultOids "?nimi=tieto")]
                 (is (= [koulutusOid2 koulutusOid3 koulutusOid4 koulutusOid5] oids))))
             (testing "by 'tietojenkäsittely'"
               (let [oids (post-200-oids defaultOids "?nimi=tietojenkäsittely")]
                 (is (= [koulutusOid2 koulutusOid4 koulutusOid5] oids))))
             (testing "by 'tietojenkäsittelytiede'"
               (let [oids (post-200-oids defaultOids "?nimi=tietojenkäsittelytiede")]
                 (is (= [koulutusOid2 koulutusOid4 koulutusOid5] oids))))
             (testing "by 'tietojenkäsittelytieteen perusopinnot'"
               (let [oids (post-200-oids defaultOids (str "?nimi=" (enc "tietojenkäsittelytieteen perusopinnot")))]
                 (is (= [koulutusOid2 koulutusOid4 koulutusOid5] oids)))))))
