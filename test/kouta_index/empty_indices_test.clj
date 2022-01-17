(ns kouta-index.empty-indices-test
  (:require [clojure.test :refer :all]
            [kouta-index.api :refer :all]
            [kouta-index.test-tools :refer :all]))

(intern 'clj-log.access-log 'service "kouta-index")

(use-fixtures :once with-empty-indices)

(deftest koulutus-list-empty-index-test
  (testing "search in empty index"
    (post-200-oids "koulutus" ["1.2.246.562.13.000001"] ""))
  (testing "search in empty index sort by nimi"
    (post-200-oids "koulutus" ["1.2.246.562.13.000001"] "?order-by=nimi"))
  (testing "search in empty index sort by tila"
    (post-200-oids "koulutus" ["1.2.246.562.13.000001"] "?order-by=tila"))
  (testing "search in empty index sort by muokkaaja"
    (post-200-oids "koulutus" ["1.2.246.562.13.000001"] "?order-by=muokkaaja"))
  (testing "search in empty index sort by modified"
    (post-200-oids "koulutus" ["1.2.246.562.13.000001"] "?order-by=modified")))

(deftest toteutus-list-empty-index-test
  (testing "search in empty index"
    (post-200-oids "toteutus" ["1.2.246.562.17.000001"]))
  (testing "search in empty index sort by nimi"
    (post-200-oids "toteutus" ["1.2.246.562.17.000001"] "?order-by=nimi"))
  (testing "search in empty index sort by tila"
    (post-200-oids "toteutus" ["1.2.246.562.17.000001"] "?order-by=tila"))
  (testing "search in empty index sort by muokkaaja"
    (post-200-oids "toteutus" ["1.2.246.562.17.000001"] "?order-by=muokkaaja"))
  (testing "search in empty index sort by modified"
    (post-200-oids "toteutus" ["1.2.246.562.17.000001"] "?order-by=modified")))

(deftest hakukohde-list-empty-index-test
  (testing "search in empty index"
    (post-200-oids "hakukohde" ["1.2.246.562.20.000001"] ""))
  (testing "search in empty index sort by nimi"
    (post-200-oids "hakukohde" ["1.2.246.562.20.000001"] "?order-by=nimi"))
  (testing "search in empty index sort by tila"
    (post-200-oids "hakukohde" ["1.2.246.562.20.000001"] "?order-by=tila"))
  (testing "search in empty index sort by muokkaaja"
    (post-200-oids "hakukohde" ["1.2.246.562.20.000001"] "?order-by=muokkaaja"))
  (testing "search in empty index sort by modified"
    (post-200-oids "hakukohde" ["1.2.246.562.20.000001"] "?order-by=modified")))

(deftest haku-list-empty-index-test
  (testing "search in empty index"
    (post-200-oids "haku" ["1.2.246.562.29.000001"] ""))
  (testing "search in empty index sort by nimi"
    (post-200-oids "haku" ["1.2.246.562.29.000001"] "?order-by=nimi"))
  (testing "search in empty index sort by tila"
    (post-200-oids "haku" ["1.2.246.562.29.000001"] "?order-by=tila"))
  (testing "search in empty index sort by muokkaaja"
    (post-200-oids "haku" ["1.2.246.562.29.000001"] "?order-by=muokkaaja"))
  (testing "search in empty index sort by modified"
    (post-200-oids "haku" ["1.2.246.562.29.000001"] "?order-by=modified")))

(deftest valintaperuste-list-empty-index-test
  (testing "search in empty index"
    (post-200-ids "valintaperuste" ["31972648-ebb7-4185-ac64-31fa6b841e34"]))
  (testing "search in empty index sort by nimi"
    (post-200-ids "valintaperuste" ["31972648-ebb7-4185-ac64-31fa6b841e34"] "?order-by=nimi"))
  (testing "search in empty index sort by tila"
    (post-200-ids "valintaperuste" ["31972648-ebb7-4185-ac64-31fa6b841e34"] "?order-by=tila"))
  (testing "search in empty index sort by muokkaaja"
    (post-200-ids "valintaperuste" ["31972648-ebb7-4185-ac64-31fa6b841e34"] "?order-by=muokkaaja"))
  (testing "search in empty index sort by modified"
    (post-200-ids "valintaperuste" ["31972648-ebb7-4185-ac64-31fa6b841e34"] "?order-by=modified")))

