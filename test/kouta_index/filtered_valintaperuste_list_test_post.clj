(ns kouta-index.filtered-valintaperuste-list-test-post
  (:require [clojure.test :refer :all]
            [kouta-index.api :refer :all]
            [ring.mock.request :as mock]
            [kouta-index.test-tools :refer :all]))

(intern 'clj-log.access-log 'service "kouta-index")

(use-fixtures :once mock-organisaatio)

(defn post-200
  [ids params]
  (let [url      (str "/kouta-index/valintaperuste/filtered-list" params)
        response (app (-> (mock/request :post url)
                          (mock/json-body ids)))]
    (is (= (:status response) 200))
    (->keywordized-json (slurp (:body response)))))

(defn post-200-ids
  ([ids params] (map #(:id %) (:result (post-200 ids params))))
  ([ids] (post-200-ids ids "")))

(deftest valintaperuste-list-empty-index-test
  (prepare-empty-elastic-indices)
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
  (prepare-elastic-test-data)
  (testing "Filter valintaperuste"
    (testing "by organisaatio"
      (let [ids (post-200-ids [valintaperusteId1])]
        (is (= [valintaperusteId1] ids))))
    (testing "by oid"
      (let [ids (post-200-ids defaultValintaperusteIds (str "?nimi=" valintaperusteId2))]
        (is (= [valintaperusteId2] ids))))
    (testing "by muokkaajan oid"
      (let [ids (post-200-ids defaultValintaperusteIds "?muokkaaja=1.2.246.562.24.55555555555")]
        (is (= [valintaperusteId3] ids))))
    (testing "by tila"
      (let [ids (post-200-ids defaultValintaperusteIds "?tila=tallennettu")]
        (is (= [valintaperusteId5] ids))))
    (comment testing "julkinen"
             (let [oids (post-200-ids (valintaperuste-url Oppilaitos2))]
               (is (= [valintaperusteId1 valintaperusteId2] oids))))
    (testing "monella tilalla"
      (let [ids (post-200-ids defaultValintaperusteIds "?tila=tallennettu,julkaistu")]
        (is (= [valintaperusteId3 valintaperusteId5 valintaperusteId2] ids))))
    (testing "monella parametrilla"
      (let [ids (post-200-ids defaultValintaperusteIds "?tila=julkaistu&muokkaaja=1.2.246.562.24.55555555555")]
        (is (= [valintaperusteId3] ids)))))

  (testing "Sort haku result"
    (testing "by tila asc"
      (let [ids (post-200-ids defaultValintaperusteIds "?order-by=tila&order=asc")]
        (is (= [valintaperusteId4 valintaperusteId3 valintaperusteId2 valintaperusteId5] ids))))
    (testing "by tila desc"
      (let [ids (post-200-ids defaultValintaperusteIds "?order-by=tila&order=desc")]
        (is (= [valintaperusteId5 valintaperusteId3 valintaperusteId2 valintaperusteId4] ids))))
    (testing "by modified asc"
      (let [ids (post-200-ids defaultValintaperusteIds "?tila=julkaistu&order-by=modified&order=asc")]
        (is (= [valintaperusteId3 valintaperusteId2] ids))))
    (testing "by modified desc"
      (let [ids (post-200-ids defaultValintaperusteIds "?tila=julkaistu&order-by=modified&order=desc")]
        (is (= [valintaperusteId2 valintaperusteId3] ids))))
    (testing "by nimi asc"
      (let [ids (post-200-ids defaultValintaperusteIds "?tila=julkaistu&order-by=nimi&order=asc")]
        (is (= [valintaperusteId3 valintaperusteId2] ids))))
    (testing "by nimi desc"
      (let [ids (post-200-ids defaultValintaperusteIds "?tila=julkaistu&order-by=nimi&order=desc")]
        (is (= [valintaperusteId2 valintaperusteId3] ids))))
    (comment testing "by hakukohde count asc"             ;TODO
             (let [ids (post-200-ids defaultValintaperusteIds "?order-by=hakukohteet&order=asc")]
               (is (= [valintaperusteId4 valintaperusteId5 valintaperusteId2 valintaperusteId3] ids))))
    (comment testing "by hakukohde count desc"            ;TODO
             (let [ids (post-200-ids defaultValintaperusteIds "?order-by=hakukohteet&order=desc")]
               (is (= [valintaperusteId3 valintaperusteId2 valintaperusteId4 valintaperusteId5] ids))))
    (comment testing "by muokkaaja asc"                 ;TODO: muokkaajan nimi onr:stä / nimen mockaus
             (let [ids (post-200-ids defaultValintaperusteIds "?tila=julkaistu&order-by=muokkaaja&order=asc")]
               (is (= [koulutusOid2 koulutusOid3] ids))))
    (comment testing "by muokkaaja desc"                ;TODO: muokkaajan nimi onr:stä / nimen mockaus
             (let [ids (post-200-ids defaultValintaperusteIds "?tila=julkaistu&order-by=muokkaaja&order=desc")]
               (is (= [koulutusOid3 koulutusOid2] ids)))))

  (testing "Page valintaperuste result"
    (testing "return first page"
      (let [ids (post-200-ids defaultValintaperusteIds "?page=1&size=2&order-by=tila")]
        (is (= [valintaperusteId4 valintaperusteId3] ids))))
    (testing "return first page when negative page"
      (let [ids (post-200-ids defaultValintaperusteIds "?page=-1&size=2&order-by=tila")]
        (is (= [valintaperusteId4 valintaperusteId3] ids))))
    (testing "return second page"
      (let [ids (post-200-ids defaultValintaperusteIds "?page=2&size=2&order-by=tila")]
        (is (= [valintaperusteId2 valintaperusteId5] ids))))
    (testing "return empty page when none left"
      (let [ids (post-200-ids defaultValintaperusteIds "?page=3&size=2&order-by=tila")]
        (is (= [] ids)))))

  (testing "haku valintaperuste contain proper fields"
    (let [res (post-200 defaultValintaperusteIds (str "?nimi=" valintaperusteId3))]
      (is (= 1 (:totalCount res)))
      (let [valintaperuste (first (:result res))
            muokkaaja (:nimi (:muokkaaja valintaperuste))]    ;TODO: muokkaajan nimi onr:stä / nimen mockaus
        (is (= {:id valintaperusteId3
                :tila "julkaistu"
                :koulutustyyppi "amm"
                :nimi {:fi "Kiva valintaperustekuvaus fi"
                       :sv "Kiva valintaperustekuvaus sv"}
                :organisaatio {:oid Oppilaitos1
                               :nimi {:fi "Kiva ammattikorkeakoulu"
                                      :sv "Kiva ammattikorkeakoulu sv"}
                               :paikkakunta {:koodiUri "kunta_091"
                                             :nimi {:fi "kunta_091 nimi fi"
                                                    :sv "kunta_091 nimi sv"}}}
                :muokkaaja {:oid "1.2.246.562.24.55555555555"
                            :nimi muokkaaja}
                :modified "2018-05-05T12:02:23"} valintaperuste))))))
