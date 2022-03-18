(ns kouta-index.filtered-koulutus-list-test-post
  (:require [clojure.test :refer :all]
            [kouta-index.api :refer :all]
            [kouta-index.test-tools :refer :all]))

(intern 'clj-log.access-log 'service "kouta-index")

(use-fixtures :once mock-organisaatio with-elastic-dump)

(deftest filtered-koulutus-list-test
  (testing "Filter koulutus"
    (testing "by organisaatio"
      (let [oids (post-200-oids "koulutus" [koulutusOid1])]
        (is (= [koulutusOid1] oids))))
    (testing "by oid"
      (let [oids (post-200-oids "koulutus" defaultKoulutusOids (str "?nimi=" koulutusOid2))]
        (is (= [koulutusOid2] oids)
    (testing "by koulutustyyppi"
      (let [oids (post-200-oids "koulutus" (conj defaultKoulutusOids yoKoulutusOid1) "?koulutustyyppi=yo")]
        (is (= [yoKoulutusOid1] oids)))))))
    (testing "by muokkaajan oid"
      (let [oids (post-200-oids "koulutus" defaultKoulutusOids "?muokkaaja=1.2.246.562.24.55555555555")]
        (is (= [koulutusOid3] oids))))
    (testing "by tila"
      (let [oids (post-200-oids "koulutus" defaultKoulutusOids "?tila=tallennettu")]
        (is (= [koulutusOid5] oids))))
    (testing "monella tilalla"
      (let [oids (post-200-oids "koulutus" defaultKoulutusOids "?tila=tallennettu,julkaistu&order-by=tila")]
        (is (= [koulutusOid2 koulutusOid3 koulutusOid5] oids))))
    (testing "monella parametrilla"
      (let [oids (post-200-oids "koulutus" defaultKoulutusOids "?tila=julkaistu&muokkaaja=1.2.246.562.24.55555555555")]
        (is (= [koulutusOid3] oids))))
    (testing "by nakyvyys"
      (let [oids (post-200-oids "koulutus" defaultKoulutusOids "?julkinen=true")]
        (is (= [koulutusOid5] oids)))))

  (testing "Sort koulutus result"
    (testing "by tila asc"
      (let [oids (post-200-oids "koulutus" defaultKoulutusOids "?order-by=tila&order=asc")]
        (is (= [koulutusOid4 koulutusOid2 koulutusOid3 koulutusOid5] oids))))
    (testing "by tila desc"
      (let [oids (post-200-oids "koulutus" defaultKoulutusOids "?order-by=tila&order=desc")]
        (is (= [koulutusOid5 koulutusOid2 koulutusOid3 koulutusOid4] oids))))
    (testing "by modified asc"
      (let [oids (post-200-oids "koulutus" defaultKoulutusOids "?tila=julkaistu&order-by=modified&order=asc")]
        (is (= [koulutusOid2 koulutusOid3] oids))))
    (testing "by modified desc"
      (let [oids (post-200-oids "koulutus" defaultKoulutusOids "?tila=julkaistu&order-by=modified&order=desc")]
        (is (= [koulutusOid3 koulutusOid2] oids))))
    (testing "by nimi asc"
      (let [oids (post-200-oids "koulutus" defaultKoulutusOids "?tila=julkaistu&order-by=nimi&order=asc")]
        (is (= [koulutusOid2 koulutusOid3] oids))))
    (testing "by desc desc"
      (let [oids (post-200-oids "koulutus" defaultKoulutusOids "?tila=julkaistu&order-by=nimi&order=desc")]
        (is (= [koulutusOid3 koulutusOid2] oids))))
    (comment testing "by toteutus count asc"
             (let [oids (post-200-oids "koulutus" defaultKoulutusOids "?order-by=toteutukset&order=asc")]
               (is (= [koulutusOid5 koulutusOid3 koulutusOid4 koulutusOid2] oids))))
    (comment testing "by toteutus count desc"
             (let [oids (post-200-oids "koulutus" defaultKoulutusOids "?order-by=toteutukset&order=desc")]
               (is (= [koulutusOid2 koulutusOid4 koulutusOid5 koulutusOid3] oids))))
    (comment testing "by muokkaaja asc"                 ;TODO: muokkaajan nimi onr:stä / nimen mockaus
             (let [oids (post-200-oids "koulutus" defaultKoulutusOids "?tila=julkaistu&order-by=muokkaaja&order=asc")]
               (is (= [koulutusOid2 koulutusOid3] oids))))
    (comment testing "by muokkaaja desc"                ;TODO: muokkaajan nimi onr:stä / nimen mockaus
             (let [oids (post-200-oids "koulutus" defaultKoulutusOids "?tila=julkaistu&order-by=muokkaaja&order=desc")]
               (is (= [koulutusOid3 koulutusOid2] oids)))))

  (testing "Page koulutus result"
    (testing "return first page"
      (let [oids (post-200-oids "koulutus" defaultKoulutusOids "?page=1&size=2&order-by=tila")]
        (is (= [koulutusOid4 koulutusOid2] oids))))
    (testing "return first page when negative page"
      (let [oids (post-200-oids "koulutus" defaultKoulutusOids "?page=-1&size=2&order-by=tila")]
        (is (= [koulutusOid4 koulutusOid2] oids))))
    (testing "return second page"
      (let [oids (post-200-oids "koulutus" defaultKoulutusOids "?page=2&size=2&order-by=tila")]
        (is (= [koulutusOid3 koulutusOid5] oids))))
    (testing "return empty page when none left"
      (let [oids (post-200-oids "koulutus" defaultKoulutusOids "?page=3&size=2&order-by=tila")]
        (is (= [] oids)))))

  (testing "Koulutus result contain proper fields"
    (let [res (post-200 "koulutus" defaultKoulutusOids (str "?nimi=" koulutusOid2))]
      (is (= 1 (:totalCount res)))
      (let [koulutus (first (:result res))
            muokkaaja (:nimi (:muokkaaja koulutus))]    ;TODO: muokkaajan nimi onr:stä / nimen mockaus
        (is (= {:oid koulutusOid2
                :tila "julkaistu"
                :julkinen false
                :koulutustyyppi "amm"
                :nimi {:fi "Tietojenkäsittelytieteen perusopinnot fi"
                       :sv "Tietojenkäsittelytieteen perusopinnot sv"}
                :organisaatio {:oid Oppilaitos1
                               :nimi {:fi "Kiva ammattikorkeakoulu"
                                      :sv "Kiva ammattikorkeakoulu sv"}
                               :paikkakunta {:koodiUri "kunta_091"
                                             :nimi {:fi "kunta_091 nimi fi"
                                                    :sv "kunta_091 nimi sv"}}}
                :muokkaaja {:oid "1.2.246.562.24.10000000000"
                            :nimi muokkaaja}
                :modified "2018-05-05T12:02:23"
                :eperuste {:voimassaoloLoppuu "2018-01-01T00:00:00", :diaarinumero "1111-OPH-2021", :id 1234}} koulutus)))))


  (comment testing "Filter koulutus by nimi"              ;TODO Tämä eivät saisi feilata!
           (testing "by 'tieto'"
             (let [oids (post-200-oids "koulutus" defaultKoulutusOids "?nimi=tieto")]
               (is (= [koulutusOid2 koulutusOid3 koulutusOid4 koulutusOid5] oids))))
           (testing "by 'tietojenkäsittely'"
             (let [oids (post-200-oids "koulutus" defaultKoulutusOids "?nimi=tietojenkäsittely")]
               (is (= [koulutusOid2 koulutusOid4 koulutusOid5] oids))))
           (testing "by 'tietojenkäsittelytiede'"
             (let [oids (post-200-oids "koulutus" defaultKoulutusOids "?nimi=tietojenkäsittelytiede")]
               (is (= [koulutusOid2 koulutusOid4 koulutusOid5] oids))))
           (testing "by 'tietojenkäsittelytieteen perusopinnot'"
             (let [oids (post-200-oids "koulutus" defaultKoulutusOids (str "?nimi=" (enc "tietojenkäsittelytieteen perusopinnot")))]
               (is (= [koulutusOid2 koulutusOid4 koulutusOid5] oids))))))
