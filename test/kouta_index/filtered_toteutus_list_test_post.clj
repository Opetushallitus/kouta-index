(ns kouta-index.filtered-toteutus-list-test-post
  (:require [clojure.test :refer :all]
            [kouta-index.api :refer :all]
            [ring.mock.request :as mock]
            [kouta-index.test-tools :refer :all]
            [kouta-index.util.tools :refer [contains-many?]]))

(intern 'clj-log.access-log 'service "kouta-index")

(use-fixtures :once mock-organisaatio)

(defn post-200
  [oids params]
  (let [url      (str "/kouta-index/toteutus/filtered-list" params)
        response (app (-> (mock/request :post url)
                          (mock/json-body oids)))]
    (is (= (:status response) 200))
    (->keywordized-json (slurp (:body response)))))

(defn post-200-oids
  ([oids params] (map #(:oid %) (:result (post-200 oids params))))
  ([oids] (post-200-oids oids "")))

(deftest toteutus-list-empty-index-test
  (prepare-empty-elastic-indices)
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
  (prepare-elastic-test-data)
  (testing "Filter toteutus"
    (testing "by organisaatio"
      (let [oids (post-200-oids [toteutusOid1])]
        (is (= [toteutusOid1] oids))))
    (testing "by oid"
      (let [oids (post-200-oids defaultToteutusOids (str "?nimi=" toteutusOid2))]
        (is (= [toteutusOid2] oids))))
    (testing "by muokkaajan oid"
      (let [oids (post-200-oids defaultToteutusOids "?muokkaaja=1.2.246.562.24.55555555555")]
        (is (= [toteutusOid3] oids))))
    (testing "by tila"
      (let [oids (post-200-oids defaultToteutusOids "?tila=tallennettu")]
        (is (= [toteutusOid5] oids))))
    (testing "monella tilalla"
      (let [oids (post-200-oids defaultToteutusOids "?tila=tallennettu,julkaistu")]
        (is (= [toteutusOid3 toteutusOid5 toteutusOid2] oids))))
    (testing "monella parametrilla"
      (let [oids (post-200-oids defaultToteutusOids "?tila=julkaistu&muokkaaja=1.2.246.562.24.55555555555")]
        (is (= [toteutusOid3] oids)))))

  (testing "Sort toteutus result"
    (testing "by tila asc"
      (let [oids (post-200-oids defaultToteutusOids "?order-by=tila&order=asc")]
        (is (= [toteutusOid4 toteutusOid3 toteutusOid2 toteutusOid5] oids))))
    (testing "by tila desc"
      (let [oids (post-200-oids defaultToteutusOids "?order-by=tila&order=desc")]
        (is (= [toteutusOid5 toteutusOid3 toteutusOid2 toteutusOid4] oids))))
    (testing "by modified asc"
      (let [oids (post-200-oids defaultToteutusOids "?tila=julkaistu&order-by=modified&order=asc")]
        (is (= [toteutusOid3 toteutusOid2] oids))))
    (testing "by modified desc"
      (let [oids (post-200-oids defaultToteutusOids "?tila=julkaistu&order-by=modified&order=desc")]
        (is (= [toteutusOid2 toteutusOid3] oids))))
    (testing "by nimi asc"
      (let [oids (post-200-oids defaultToteutusOids "?tila=julkaistu&order-by=nimi&order=asc")]
        (is (= [toteutusOid3 toteutusOid2] oids))))
    (testing "by nimi desc"
      (let [oids (post-200-oids defaultToteutusOids "?tila=julkaistu&order-by=nimi&order=desc")]
        (is (= [toteutusOid2 toteutusOid3] oids))))
    (comment testing "by hakukohde count asc"
             (let [oids (post-200-oids defaultToteutusOids "?order-by=hakukohteet&order=asc")]
               (is (= [toteutusOid3 toteutusOid2 toteutusOid4 toteutusOid5] oids))))
    (comment testing "by hakukohde count desc"
             (let [oids (post-200-oids defaultToteutusOids "?order-by=hakukohteet&order=desc")]
               (is (= [toteutusOid5 toteutusOid4 toteutusOid3 toteutusOid2] oids))))
    (comment testing "by muokkaaja asc"                 ;TODO: muokkaajan nimi onr:stä / nimen mockaus
             (let [oids (post-200-oids defaultToteutusOids "?tila=julkaistu&order-by=muokkaaja&order=asc")]
               (is (= [koulutusOid2 koulutusOid3] oids))))
    (comment testing "by muokkaaja desc"                ;TODO: muokkaajan nimi onr:stä / nimen mockaus
             (let [oids (post-200-oids defaultToteutusOids "?tila=julkaistu&order-by=muokkaaja&order=desc")]
               (is (= [koulutusOid3 koulutusOid2] oids)))))

  (testing "Page toteutus result"
    (testing "return first page"
      (let [oids (post-200-oids defaultToteutusOids "?page=1&size=2&order-by=tila")]
        (is (= [toteutusOid4 toteutusOid3] oids))))
    (testing "return first page when negative page"
      (let [oids (post-200-oids defaultToteutusOids "?page=-1&size=2&order-by=tila")]
        (is (= [toteutusOid4 toteutusOid3] oids))))
    (testing "return second page"
      (let [oids (post-200-oids defaultToteutusOids "?page=2&size=2&order-by=tila")]
        (is (= [toteutusOid2 toteutusOid5] oids))))
    (testing "return empty page when none left"
      (let [oids (post-200-oids defaultToteutusOids "?page=3&size=2&order-by=tila")]
        (is (= [] oids)))))

  (testing "Toteutus result contains hakutieto hakukohde organisaatio and tila"
    (let [res (post-200 [toteutusOid1] "")
          hakukohteet-count (-> res :result (first) :hakukohteet (count))
          has-organisaatio-and-tila (->> res :result (first) :hakukohteet (every? #(contains-many? % :organisaatio :tila)))]
      (is (= 1 (:totalCount res)))
      (is (= 2 hakukohteet-count))
      (is (true? has-organisaatio-and-tila))))

  (testing "Toteutus result contain proper fields"
    (let [res (post-200 defaultToteutusOids (str "?nimi=" toteutusOid3))]
      (is (= 1 (:totalCount res)))
      (let [toteutus (first (:result res))
            muokkaaja (:nimi (:muokkaaja toteutus))]    ;TODO: muokkaajan nimi onr:stä / nimen mockaus
        (is (= {:oid toteutusOid3
                :tila "julkaistu"
                :koulutustyyppi "amm"
                :nimi {:fi "Autoalan perusopinnot fi"
                       :sv "Autoalan perusopinnot sv"}
                :organisaatio {:oid Oppilaitos1
                               :nimi {:fi "Kiva ammattikorkeakoulu"
                                      :sv "Kiva ammattikorkeakoulu sv"}
                               :paikkakunta {:koodiUri "kunta_091"
                                             :nimi {:fi "kunta_091 nimi fi"
                                                    :sv "kunta_091 nimi sv"}}}
                :organisaatiot ["1.2.246.562.10.54545454545" "1.2.246.562.10.67476956288" "1.2.246.562.10.594252633210"]
                :hakukohteet []
                :muokkaaja {:oid "1.2.246.562.24.55555555555"
                            :nimi muokkaaja}
                :modified "2018-05-05T12:02:23"} toteutus))))))

