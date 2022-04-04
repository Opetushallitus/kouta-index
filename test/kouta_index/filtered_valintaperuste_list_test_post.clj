(ns kouta-index.filtered-valintaperuste-list-test-post
  (:require [clojure.test :refer :all]
            [kouta-index.api :refer :all]
            [kouta-index.test-tools :refer :all]))

(intern 'clj-log.access-log 'service "kouta-index")

(use-fixtures :once mock-organisaatio with-elastic-dump)

(deftest filtered-valintaperuste-list-test
  (testing "Filter valintaperuste"
    (testing "by organisaatio"
      (let [ids (post-200-ids "valintaperuste" [valintaperusteId1])]
        (is (= [valintaperusteId1] ids))))
    (testing "by oid"
      (let [ids (post-200-ids "valintaperuste" defaultValintaperusteIds (str "?nimi=" valintaperusteId2))]
        (is (= [valintaperusteId2] ids))))
    (testing "by muokkaajan oid"
      (let [ids (post-200-ids "valintaperuste" defaultValintaperusteIds "?muokkaaja=1.2.246.562.24.55555555555")]
        (is (= [valintaperusteId3] ids))))
    (testing "by tila"
      (let [ids (post-200-ids "valintaperuste" defaultValintaperusteIds "?tila=tallennettu")]
        (is (= [valintaperusteId5] ids))))
    (comment testing "julkinen"
             (let [oids (post-200-ids "valintaperuste" (valintaperuste-url Oppilaitos2))]
               (is (= [valintaperusteId1 valintaperusteId2] oids))))
    (testing "monella tilalla"
      (let [ids (post-200-ids "valintaperuste" defaultValintaperusteIds "?tila=tallennettu,julkaistu")]
        (is (= [valintaperusteId3 valintaperusteId5 valintaperusteId2] ids))))
    (testing "monella parametrilla"
      (let [ids (post-200-ids "valintaperuste" defaultValintaperusteIds "?tila=julkaistu&muokkaaja=1.2.246.562.24.55555555555")]
        (is (= [valintaperusteId3] ids))))
    (testing "by nakyvyys"
      (let [ids (post-200-ids "valintaperuste" defaultValintaperusteIds "?julkinen=true")]
        (= [valintaperusteId2] ids))))

  (testing "Sort haku result"
    (testing "by tila asc"
      (let [ids (post-200-ids "valintaperuste" defaultValintaperusteIds "?order-by=tila&order=asc")]
        (is (= [valintaperusteId4 valintaperusteId3 valintaperusteId2 valintaperusteId5] ids))))
    (testing "by tila desc"
      (let [ids (post-200-ids "valintaperuste" defaultValintaperusteIds "?order-by=tila&order=desc")]
        (is (= [valintaperusteId5 valintaperusteId3 valintaperusteId2 valintaperusteId4] ids))))
    (testing "by modified asc"
      (let [ids (post-200-ids "valintaperuste" defaultValintaperusteIds "?tila=julkaistu&order-by=modified&order=asc")]
        (is (= [valintaperusteId3 valintaperusteId2] ids))))
    (testing "by modified desc"
      (let [ids (post-200-ids "valintaperuste" defaultValintaperusteIds "?tila=julkaistu&order-by=modified&order=desc")]
        (is (= [valintaperusteId2 valintaperusteId3] ids))))
    (testing "by nimi asc"
      (let [ids (post-200-ids "valintaperuste" defaultValintaperusteIds "?tila=julkaistu&order-by=nimi&order=asc")]
        (is (= [valintaperusteId3 valintaperusteId2] ids))))
    (testing "by nimi desc"
      (let [ids (post-200-ids "valintaperuste" defaultValintaperusteIds "?tila=julkaistu&order-by=nimi&order=desc")]
        (is (= [valintaperusteId2 valintaperusteId3] ids))))
    (comment testing "by hakukohde count asc"             ;TODO
             (let [ids (post-200-ids "valintaperuste" defaultValintaperusteIds "?order-by=hakukohteet&order=asc")]
               (is (= [valintaperusteId4 valintaperusteId5 valintaperusteId2 valintaperusteId3] ids))))
    (comment testing "by hakukohde count desc"            ;TODO
             (let [ids (post-200-ids "valintaperuste" defaultValintaperusteIds "?order-by=hakukohteet&order=desc")]
               (is (= [valintaperusteId3 valintaperusteId2 valintaperusteId4 valintaperusteId5] ids))))
    (comment testing "by muokkaaja asc"                 ;TODO: muokkaajan nimi onr:stä / nimen mockaus
             (let [ids (post-200-ids "valintaperuste" defaultValintaperusteIds "?tila=julkaistu&order-by=muokkaaja&order=asc")]
               (is (= [koulutusOid2 koulutusOid3] ids))))
    (comment testing "by muokkaaja desc"                ;TODO: muokkaajan nimi onr:stä / nimen mockaus
             (let [ids (post-200-ids "valintaperuste" defaultValintaperusteIds "?tila=julkaistu&order-by=muokkaaja&order=desc")]
               (is (= [koulutusOid3 koulutusOid2] ids)))))

  (testing "Page valintaperuste result"
    (testing "return first page"
      (let [ids (post-200-ids "valintaperuste" defaultValintaperusteIds "?page=1&size=2&order-by=tila")]
        (is (= [valintaperusteId4 valintaperusteId3] ids))))
    (testing "return first page when negative page"
      (let [ids (post-200-ids "valintaperuste" defaultValintaperusteIds "?page=-1&size=2&order-by=tila")]
        (is (= [valintaperusteId4 valintaperusteId3] ids))))
    (testing "return second page"
      (let [ids (post-200-ids "valintaperuste" defaultValintaperusteIds "?page=2&size=2&order-by=tila")]
        (is (= [valintaperusteId2 valintaperusteId5] ids))))
    (testing "return empty page when none left"
      (let [ids (post-200-ids "valintaperuste" defaultValintaperusteIds "?page=3&size=2&order-by=tila")]
        (is (= [] ids)))))

  (testing "haku valintaperuste contain proper fields"
    (let [res (post-200 "valintaperuste" defaultValintaperusteIds (str "?nimi=" valintaperusteId3))]
      (is (= 1 (:totalCount res)))
      (let [valintaperuste (first (:result res))
            muokkaaja (:nimi (:muokkaaja valintaperuste))]    ;TODO: muokkaajan nimi onr:stä / nimen mockaus
        (is (= {:id valintaperusteId3
                :tila "julkaistu"
                :julkinen false
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
