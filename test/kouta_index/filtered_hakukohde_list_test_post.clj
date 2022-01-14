(ns kouta-index.filtered-hakukohde-list-test-post
  (:require [clojure.test :refer :all]
            [kouta-index.api :refer :all]
            [ring.mock.request :as mock]
            [kouta-index.test-tools :refer :all]))

(intern 'clj-log.access-log 'service "kouta-index")

(use-fixtures :once mock-organisaatio)

(defn post-200
  [oids params]
  (let [url      (str "/kouta-index/hakukohde/filtered-list" params)
        response (app (-> (mock/request :post url)
                          (mock/json-body oids)))]
    (is (= (:status response) 200))
    (->keywordized-json (slurp (:body response)))))

(defn post-200-oids
  ([oids params] (map #(:oid %) (:result (post-200 oids params))))
  ([oids] (post-200-oids oids "")))

(deftest hakukohde-list-empty-index-test
  (testing "search in empty index"
    (post-200-oids ["1.2.246.562.20.000001"] ""))
  (testing "search in empty index sort by nimi"
    (post-200-oids ["1.2.246.562.20.000001"] "?order-by=nimi"))
  (testing "search in empty index sort by tila"
    (post-200-oids ["1.2.246.562.20.000001"] "?order-by=tila"))
  (testing "search in empty index sort by muokkaaja"
    (post-200-oids ["1.2.246.562.20.000001"] "?order-by=muokkaaja"))
  (testing "search in empty index sort by modified"
    (post-200-oids ["1.2.246.562.20.000001"] "?order-by=modified")))

(deftest filtered-hakukohde-list-test
  (prepare-elastic-test-data)
  (testing "Filter hakukohde"
    (testing "by organisaatio"
      (let [oids (post-200-oids [hakukohdeOid1])]
        (is (= [hakukohdeOid1] oids))))
    (testing "by oid"
      (let [oids (post-200-oids defaultHakukohdeOids (str "?nimi=" hakukohdeOid2))]
        (is (= [hakukohdeOid2] oids))))
    (testing "by muokkaajan oid"
      (let [oids (post-200-oids defaultHakukohdeOids "?muokkaaja=1.2.246.562.24.55555555555")]
        (is (= [hakukohdeOid3] oids))))
    (testing "by tila"
      (let [oids (post-200-oids defaultHakukohdeOids "?tila=tallennettu")]
        (is (= [hakukohdeOid5] oids))))
    (testing "monella tilalla"
      (let [oids (post-200-oids defaultHakukohdeOids "?tila=tallennettu,julkaistu")]
        (is (= [hakukohdeOid3 hakukohdeOid5 hakukohdeOid2] oids))))
    (testing "monella parametrilla"
      (let [oids (post-200-oids defaultHakukohdeOids "?tila=julkaistu&muokkaaja=1.2.246.562.24.55555555555")]
        (is (= [hakukohdeOid3] oids)))))

  (testing "Sort hakukohde result"
    (testing "by tila asc"
      (let [oids (post-200-oids defaultHakukohdeOids "?order-by=tila&order=asc")]
        (is (= [hakukohdeOid4 hakukohdeOid3 hakukohdeOid2 hakukohdeOid5] oids))))
    (testing "by tila desc"
      (let [oids (post-200-oids defaultHakukohdeOids "?order-by=tila&order=desc")]
        (is (= [hakukohdeOid5 hakukohdeOid3 hakukohdeOid2 hakukohdeOid4] oids))))
    (testing "by modified asc"
      (let [oids (post-200-oids defaultHakukohdeOids "?tila=julkaistu&order-by=modified&order=asc")]
        (is (= [hakukohdeOid3 hakukohdeOid2] oids))))
    (testing "by modified desc"
      (let [oids (post-200-oids defaultHakukohdeOids "?tila=julkaistu&order-by=modified&order=desc")]
        (is (= [hakukohdeOid2 hakukohdeOid3] oids))))
    (testing "by nimi asc"
      (let [oids (post-200-oids defaultHakukohdeOids "?tila=julkaistu&order-by=nimi&order=asc")]
        (is (= [hakukohdeOid3 hakukohdeOid2] oids))))
    (testing "by nimi desc"
      (let [oids (post-200-oids defaultHakukohdeOids "?tila=julkaistu&order-by=nimi&order=desc")]
        (is (= [hakukohdeOid2 hakukohdeOid3] oids))))
    (comment testing "by muokkaaja asc"                 ;TODO: muokkaajan nimi onr:stä / nimen mockaus
             (let [oids (post-200-oids defaultHakukohdeOids "?tila=julkaistu&order-by=muokkaaja&order=asc")]
               (is (= [koulutusOid2 koulutusOid3] oids))))
    (comment testing "by muokkaaja desc"                ;TODO: muokkaajan nimi onr:stä / nimen mockaus
             (let [oids (post-200-oids defaultHakukohdeOids "?tila=julkaistu&order-by=muokkaaja&order=desc")]
               (is (= [koulutusOid3 koulutusOid2] oids)))))

  (testing "Page hakukohde result"
    (testing "return first page"
      (let [oids (post-200-oids defaultHakukohdeOids "?page=1&size=2&order-by=tila")]
        (is (= [hakukohdeOid4 hakukohdeOid3] oids))))
    (testing "return first page when negative page"
      (let [oids (post-200-oids defaultHakukohdeOids "?page=-1&size=2&order-by=tila")]
        (is (= [hakukohdeOid4 hakukohdeOid3] oids))))
    (testing "return second page"
      (let [oids (post-200-oids defaultHakukohdeOids "?page=2&size=2&order-by=tila")]
        (is (= [hakukohdeOid2 hakukohdeOid5] oids))))
    (testing "return empty page when none left"
      (let [oids (post-200-oids defaultHakukohdeOids "?page=3&size=2&order-by=tila")]
        (is (= [] oids)))))

  (testing "Hakukohde result contain proper fields"
    (let [res (post-200 defaultHakukohdeOids (str "?nimi=" hakukohdeOid3))]
      (is (= 1 (:totalCount res)))
      (let [hakukohde (first (:result res))
            muokkaaja (:nimi (:muokkaaja hakukohde))]    ;TODO: muokkaajan nimi onr:stä / nimen mockaus
        (is (= {:oid hakukohdeOid3
                :tila "julkaistu"
                :koulutustyyppi "amm"
                :nimi {:fi "autoalan hakukohde fi"
                       :sv "autoalan hakukohde sv"}
                :organisaatio {:oid Oppilaitos1
                               :nimi {:fi "Kiva ammattikorkeakoulu"
                                      :sv "Kiva ammattikorkeakoulu sv"}
                               :paikkakunta {:koodiUri "kunta_091"
                                             :nimi {:fi "kunta_091 nimi fi"
                                                    :sv "kunta_091 nimi sv"}}}
                :muokkaaja {:oid "1.2.246.562.24.55555555555"
                            :nimi muokkaaja}
                :modified "2018-05-05T12:02:23"} hakukohde))))))
