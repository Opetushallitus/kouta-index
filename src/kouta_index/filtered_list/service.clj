(ns kouta-index.filtered-list.service
  (:require
   [kouta-index.filtered-list.search :refer [default-source-fields search ->basic-oid-query ->basic-id-query]]
   [kouta-index.rest.organisaatio :refer :all]))

(defn- map-results
  [response f]
  (update response :result #(map f %)))

(defn search-koulutukset
  [oids params]
  (let [base-query (->basic-oid-query oids)
        source-fields (conj default-source-fields
                            "julkinen",
                            "koulutustyyppi"
                            "metadata.eperuste"
                            "toteutukset.oid"
                            "toteutukset.tila"
                            "toteutukset.modified"
                            "toteutukset.nimi"
                            "toteutukset.organisaatio"
                            "toteutukset.organisaatiot")]
    (-> (search "koulutus-kouta-virkailija" source-fields base-query params)
        (map-results (fn [koulutus] (-> koulutus
                                        (#(assoc % :eperuste (get-in % [:metadata :eperuste])))
                                        (dissoc :metadata)))))))

(defn search-toteutukset
  [oids params]
  (let [base-query (->basic-oid-query oids)
        source-fields (conj default-source-fields
                            "koulutustyyppi"
                            "organisaatiot"
                            "hakutiedot.hakukohteet.hakukohdeOid"
                            "hakutiedot.hakukohteet.tila"
                            "hakutiedot.hakukohteet.modified"
                            "hakutiedot.hakukohteet.nimi"
                            "hakutiedot.hakukohteet.organisaatio")]
    (-> (search "toteutus-kouta-virkailija" source-fields base-query params)
        (map-results (fn [toteutus] (-> toteutus
                                        (#(assoc % :hakukohteet (->> (get % :hakutiedot [])
                                                                     (map :hakukohteet)
                                                                     (flatten))))
                                        (dissoc :hakutiedot)))))))

(defn search-haut
  [oids params]
  (let [base-query (->basic-oid-query oids)
        source-fields (conj default-source-fields
                            "hakutapa"
                            "metadata.koulutuksenAlkamiskausi"
                            "hakukohteet.oid"
                            "hakukohteet.tila"
                            "hakukohteet.modified"
                            "hakukohteet.nimi"
                            "hakukohteet.organisaatio")]
    (search "haku-kouta-virkailija" source-fields base-query params)))

(defn search-hakukohteet
  [oids params]
  (let [base-query (->basic-oid-query oids)
        source-fields (conj default-source-fields "koulutustyyppi")]
    (search "hakukohde-kouta-virkailija" source-fields base-query params)))

(defn search-valintaperusteet
  [ids params]
  (let [base-query (->basic-id-query ids)
        source-fields (conj (remove #(= % "oid") default-source-fields) "julkinen" "koulutustyyppi" "id")]
    (search "valintaperuste-kouta-virkailija" source-fields base-query params)))
