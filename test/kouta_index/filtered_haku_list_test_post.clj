(ns kouta-index.filtered-haku-list-test-post
  (:require [clojure.test :refer :all]
            [kouta-index.api :refer :all]
            [kouta-index.test-tools :refer :all]
            ))

(intern 'clj-log.access-log 'service "kouta-index")

(use-fixtures :once mock-organisaatio with-elastic-dump)

(deftest filtered-haku-list-test
  (testing "Filter haku"
    (testing "by organisaatio"
      (let [oids (post-200-oids "haku" [hakuOid1])]
        (is (= [hakuOid1] oids))))
    (testing "by oid"
      (let [oids (post-200-oids "haku" defaultHakuOids (str "?nimi=" hakuOid2))]
        (is (= [hakuOid2] oids))))
    (testing "by muokkaajan oid"
      (let [oids (post-200-oids "haku" defaultHakuOids "?muokkaaja=1.2.246.562.24.55555555555")]
        (is (= [hakuOid3] oids))))
    (testing "by tila"
      (let [oids (post-200-oids "haku" defaultHakuOids "?tila=tallennettu")]
        (is (= [hakuOid5] oids))))
    (testing "monella tilalla"
      (let [oids (post-200-oids "haku" defaultHakuOids "?tila=tallennettu,julkaistu")]
        (is (= [hakuOid3 hakuOid5 hakuOid2] oids))))
    (testing "monella parametrilla"
      (let [oids (post-200-oids "haku" defaultHakuOids "?tila=julkaistu&muokkaaja=1.2.246.562.24.55555555555")]
        (is (= [hakuOid3] oids))))
    (testing "hakutavalla"
      (let [oids (post-200-oids "haku" defaultHakuOids "?hakutapa=hakutapa_01#1")]
        (is (= [hakuOid2] oids))))
    (testing "koulutuksen alkamisvuodella"
      (let [oids (post-200-oids "haku" defaultHakuOids "?koulutuksenAlkamisvuosi=2020")]
        (is (= [hakuOid4 hakuOid2] oids))))
    (testing "koulutuksen alkamiskaudella"
      (let [oids (post-200-oids "haku" defaultHakuOids "?koulutuksenAlkamiskausi=kausi_s#1")]
        (is (= [hakuOid2] oids)))))

  (testing "Sort haku result"
    (testing "without order-by"
      (let [oids (post-200-oids "haku" defaultHakuOids)]
        (is (= [hakuOid4 hakuOid3 hakuOid5 hakuOid2] oids))))
    (testing "by tila asc"
      (let [oids (post-200-oids "haku" defaultHakuOids "?order-by=tila&order=asc")]
        (is (= [hakuOid4 hakuOid3 hakuOid2 hakuOid5] oids))))
    (testing "by tila desc"
      (let [oids (post-200-oids "haku" defaultHakuOids "?order-by=tila&order=desc")]
        (is (= [hakuOid5 hakuOid3 hakuOid2 hakuOid4] oids))))
    (testing "by modified asc"
      (let [oids (post-200-oids "haku" defaultHakuOids "?tila=julkaistu&order-by=modified&order=asc")]
        (is (= [hakuOid3 hakuOid2] oids))))
    (testing "by modified desc"
      (let [oids (post-200-oids "haku" defaultHakuOids "?tila=julkaistu&order-by=modified&order=desc")]
        (is (= [hakuOid2 hakuOid3] oids))))
    (testing "by nimi asc"
      (let [oids (post-200-oids "haku" defaultHakuOids "?tila=julkaistu&order-by=nimi&order=asc")]
        (is (= [hakuOid3 hakuOid2] oids))))
    (testing "by nimi desc"
      (let [oids (post-200-oids "haku" defaultHakuOids "?tila=julkaistu&order-by=nimi&order=desc")]
        (is (= [hakuOid2 hakuOid3] oids))))
    (testing "by hakutapa asc"
      (let [oids (post-200-oids "haku" defaultHakuOids "?order-by=hakutapa&order=asc")]
        (is (= [hakuOid2 hakuOid4 hakuOid5 hakuOid3] oids))))
    (testing "by hakutapa desc"
      (let [oids (post-200-oids "haku" defaultHakuOids "?order-by=hakutapa&order=desc")]
        (is (= [hakuOid5 hakuOid3 hakuOid4 hakuOid2] oids))))
    (testing "by koulutuksenAlkamiskausi asc"
      (let [oids (post-200-oids "haku" defaultHakuOids "?order-by=koulutuksenAlkamiskausi&order=asc")]
        (is (= [hakuOid4 hakuOid5 hakuOid3 hakuOid2] oids))))
    (testing "by koulutuksenAlkamiskausi desc"
      (let [oids (post-200-oids "haku" defaultHakuOids "?order-by=koulutuksenAlkamiskausi&order=desc")]
        (is (= [hakuOid2 hakuOid4 hakuOid5 hakuOid3] oids))))
    (comment testing "by hakukohde count asc"
             (let [oids (post-200-oids "haku" defaultHakuOids "?order-by=hakukohteet&order=asc")]
               (is (= [hakuOid4 hakuOid5 hakuOid2 hakuOid3] oids))))
    (comment testing "by hakukohde count desc"
             (let [oids (post-200-oids "haku" defaultHakuOids "?order-by=hakukohteet&order=desc")]
               (is (= [hakuOid3 hakuOid2 hakuOid4 hakuOid5] oids))))
    (comment testing "by muokkaaja asc"                   ;TODO: muokkaajan nimi onr:stä / nimen mockaus
             (let [oids (post-200-oids "haku" defaultHakuOids "?tila=julkaistu&order-by=muokkaaja&order=asc")]
               (is (= [koulutusOid2 koulutusOid3] oids))))
    (comment testing "by muokkaaja desc"                  ;TODO: muokkaajan nimi onr:stä / nimen mockaus
             (let [oids (post-200-oids "haku" defaultHakuOids "?tila=julkaistu&order-by=muokkaaja&order=desc")]
               (is (= [koulutusOid3 koulutusOid2] oids)))))

  (testing "Page haku result"
    (testing "return first page"
      (let [oids (post-200-oids "haku" defaultHakuOids "?page=1&size=2&order-by=tila")]
        (is (= [hakuOid4 hakuOid3] oids))))
    (testing "return first page when negative page"
      (let [oids (post-200-oids "haku" defaultHakuOids "?page=-1&size=2&order-by=tila")]
        (is (= [hakuOid4 hakuOid3] oids))))
    (testing "return second page"
      (let [oids (post-200-oids "haku" defaultHakuOids "?page=2&size=2&order-by=tila")]
        (is (= [hakuOid2 hakuOid5] oids))))
    (testing "return empty page when none left"
      (let [oids (post-200-oids "haku" defaultHakuOids "?page=3&size=2&order-by=tila")]
        (is (= [] oids)))))

  (testing "haku result contain proper fields"
    (let [res (post-200 "haku" defaultHakuOids (str "?nimi=" hakuOid3))]
      (is (= 1 (:totalCount res)))
      (let [haku (first (:result res))
            muokkaaja (:nimi (:muokkaaja haku))]          ;TODO: muokkaajan nimi onr:stä / nimen mockaus
        (is (= {:oid hakuOid3
                :tila "julkaistu"
                :nimi {:fi "Jatkuva haku fi"
                       :sv "Jatkuva haku sv"}
                :organisaatio {:oid Oppilaitos1
                               :nimi {:fi "Kiva ammattikorkeakoulu"
                                      :sv "Kiva ammattikorkeakoulu sv"}
                               :paikkakunta {:koodiUri "kunta_091"
                                             :nimi {:fi "kunta_091 nimi fi"
                                                    :sv "kunta_091 nimi sv"}}}
                :muokkaaja {:oid "1.2.246.562.24.55555555555"
                            :nimi muokkaaja}
                :modified "2018-05-05T12:02:23"
                :koulutuksenAlkamiskausi
                {:koulutuksenAlkamiskausi
                 {:koodiUri "kausi_k"
                  :nimi {:fi "kausi_k nimi fi" :sv "kausi_k nimi sv"}}
                 :koulutuksenAlkamisvuosi "2022"
                 :alkamiskausityyppi "alkamiskausi ja -vuosi"
                 :henkilokohtaisenSuunnitelmanLisatiedot {}}
                :hakutapa
                {:koodiUri "hakutapa_03"
                 :nimi {:fi "hakutapa_03 nimi fi" :sv "hakutapa_03 nimi sv"}}} haku))))))
