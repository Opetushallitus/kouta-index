(ns kouta-index.test-tools
  (:require [clojure.test :refer :all]
            [kouta-index.api :refer :all]
            [clojure.java.shell :as shell]
            [clj-elasticsearch.elastic-connect :as e]
            [clj-elasticsearch.elastic-utils :as e-utils]
            [kouta-index.rest.organisaatio]
            [cheshire.core :refer [parse-string]]
            [clojure.string :as string]
            [clojure.walk :refer [keywordize-keys]]))

(defonce Oppilaitos1 "1.2.246.562.10.54545454545")
(defonce Oppilaitos2 "1.2.246.562.10.55555555555")

(defonce koulutusOid1 "1.2.246.562.13.000001")
(defonce koulutusOid2 "1.2.246.562.13.000002")
(defonce koulutusOid3 "1.2.246.562.13.000003")
(defonce koulutusOid4 "1.2.246.562.13.000004")
(defonce koulutusOid5 "1.2.246.562.13.000005")
(defonce defaultKoulutusOids  [koulutusOid2 koulutusOid3 koulutusOid4 koulutusOid5])

(defonce sorakuvausId "31972648-ebb7-4185-ac64-31fa6b841e39")

(defonce hakuOid1    "1.2.246.562.29.0000001")
(defonce hakuOid2    "1.2.246.562.29.0000002")
(defonce hakuOid3    "1.2.246.562.29.0000003")
(defonce hakuOid4    "1.2.246.562.29.0000004")
(defonce hakuOid5    "1.2.246.562.29.0000005")
(defonce defaultHakuOids [hakuOid2 hakuOid3 hakuOid4 hakuOid5])

(defonce toteutusOid1  "1.2.246.562.17.000001")
(defonce toteutusOid2  "1.2.246.562.17.000002")
(defonce toteutusOid3  "1.2.246.562.17.000003")
(defonce toteutusOid4  "1.2.246.562.17.000004")
(defonce toteutusOid5  "1.2.246.562.17.000005")
(defonce defaultToteutusOids [toteutusOid2 toteutusOid3 toteutusOid4 toteutusOid5])

(defonce hakukohdeOid1     "1.2.246.562.20.0000001")
(defonce hakukohdeOid2     "1.2.246.562.20.0000002")
(defonce hakukohdeOid3     "1.2.246.562.20.0000003")
(defonce hakukohdeOid4     "1.2.246.562.20.0000004")
(defonce hakukohdeOid5     "1.2.246.562.20.0000005")
(defonce hakukohdeOid6     "1.2.246.562.20.0000006")
(defonce defaultHakukohdeOids [hakukohdeOid2 hakukohdeOid3 hakukohdeOid4 hakukohdeOid5])

(defonce valintaperusteId1 "31972648-ebb7-4185-ac64-31fa6b841e34")
(defonce valintaperusteId2 "31972648-ebb7-4185-ac64-31fa6b841e35")
(defonce valintaperusteId3 "31972648-ebb7-4185-ac64-31fa6b841e36")
(defonce valintaperusteId4 "31972648-ebb7-4185-ac64-31fa6b841e37")
(defonce valintaperusteId5 "31972648-ebb7-4185-ac64-31fa6b841e38")
(defonce defaultValintaperusteIds        [valintaperusteId2 valintaperusteId3 valintaperusteId4 valintaperusteId5])

(defn reset-elastic []
  (e/delete-index "_all"))

(defn prepare-elastic-test-data []
  (let [e-host (string/replace e-utils/elastic-host #"127\.0\.0\.1|localhost" "host.docker.internal")]
    (println (:out (shell/sh "test/resources/load_elastic_dump.sh" e-utils/elastic-host )))))

(defn prepare-empty-elastic-indices []
  (println (:out (shell/sh "multielasticdump" "--direction=load" "--input=./elastic_dump" (format "--output=%s" e-utils/elastic-host) "--includeType=mapping,analyzer,alias,settings,template"))))

(defn mock-organisaatio
  [tests]
  (defrecord Oids [parents oid children])
  (with-redefs [kouta-index.rest.organisaatio/get-oids (fn [oid] (map->Oids {:parents () :oid oid :children ()}))]
    (tests)))

(defn toteutus-url
  ([oids] (str "/kouta-index/toteutus/filtered-list?oids=" oids))
  ([] (toteutus-url (str toteutusOid2 "," toteutusOid3 "," toteutusOid4 "," toteutusOid5))))

(defn koulutus-url
  ([oids] (str "/kouta-index/koulutus/filtered-list?oids=" oids))
  ([] (koulutus-url (str koulutusOid2 "," koulutusOid3 "," koulutusOid4 "," koulutusOid5))))

(defn valintaperuste-url
  ([ids] (str "/kouta-index/valintaperuste/filtered-list?oids=" ids))
  ([] (valintaperuste-url (str valintaperusteId1 "," valintaperusteId2 "," valintaperusteId3 "," valintaperusteId4))))

(defn ->keywordized-json
  [string]
  (keywordize-keys (parse-string string)))