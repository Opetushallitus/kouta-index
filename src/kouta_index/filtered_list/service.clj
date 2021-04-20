(ns kouta-index.filtered-list.service
  (:require
    [kouta-index.filtered-list.search :refer [default-source-fields search ->basic-oid-query ->basic-id-query]]
    [kouta-index.rest.organisaatio :refer :all]
    [kouta-index.util.search :refer [->terms-query ->match-query]]))

(defn search-koulutukset
  [oids params]
  (let [base-query (->basic-oid-query oids)
        source-fields (conj default-source-fields
                            "metadata.eperuste"
                            "toteutukset.tila"
                            "toteutukset.organisaatiot")]
    (search "koulutus-kouta-virkailija" source-fields base-query params)))

(defn search-toteutukset
  [oids params]
  (let [base-query (->basic-oid-query oids)
        source-fields (conj default-source-fields
                            "organisaatiot"
                            "hakutiedot.hakukohteet.tila"
                            "hakutiedot.hakukohteet.organisaatioOid")]
    (search "toteutus-kouta-virkailija" source-fields base-query params)))

(defn search-haut
  [oids params]
  (let [base-query (->basic-oid-query oids)]
    (search "haku-kouta-virkailija" default-source-fields base-query params)))

(defn search-hakukohteet
  [oids params]
  (let [base-query (->basic-oid-query oids)]
    (search "hakukohde-kouta-virkailija" default-source-fields base-query params)))

(defn search-valintaperusteet
  [ids params]
  (let [base-query (->basic-id-query ids)
        source-fields (conj (remove #(= % "oid") default-source-fields) "id")]
    (search "valintaperuste-kouta-virkailija" source-fields base-query params)))
