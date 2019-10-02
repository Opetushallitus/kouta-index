(ns kouta-index.filtered-list.service
  (:require
    [kouta-index.filtered-list.search :refer [default-source-fields search ->basic-org-query]]
    [kouta-index.rest.organisaatio :refer :all]
    [kouta-index.util.search :refer [->terms-query]]))

(defn- org-list->comma-separated-string
  [org-list]
  (clojure.string/join "," (map #(str "'" % "'") (vec org-list))))

(defonce count-koulutuksen-toteutukset-script "
  int count = 0;
  List allowedOids = Arrays.asList(new String[] {%s});
  List toteutukset = params['_source']['toteutukset'];
  if(toteutukset != null) {
    for(int i = 0; i < toteutukset.getLength(); i++) {
      List organisaatiot = toteutukset.get(i).get('organisaatiot');
      if(organisaatiot != null) {
        for(int j = 0; j < organisaatiot.getLength(); j++) {
          if(allowedOids.contains(organisaatiot.get(j))) {
            count++;
            break;
          }
        }
      }
    }
  }
  return count;")

(defn search-koulutukset
  [organisaatio params]
  (let [orgs (get-oids organisaatio)
        base-query (->basic-org-query (with-parents-and-children orgs))
        script (format count-koulutuksen-toteutukset-script (org-list->comma-separated-string (with-children orgs)))]
    (search "koulutus-kouta" default-source-fields base-query script "toteutukset" params)))

(defonce count-toteutuksen-hakukohteet-script "
  int count = 0;
  if(params['_source']['hakukohteet'] != null) {
    count = params['_source']['hakukohteet'].getLength();
  }
  return count;")

(defn search-toteutukset
  [organisaatio params]
  (let [orgs (get-oids organisaatio)
        base-query (->basic-org-query (with-children orgs))]
    (search "toteutus-kouta" default-source-fields base-query count-toteutuksen-hakukohteet-script "hakukohteet" params)))

(defonce count-haun-hakukohteet-script "
  int count = 0;
  List allowedOids = Arrays.asList(new String[] {%s});
  List hakukohteet = params['_source']['hakukohteet'];
  if(hakukohteet != null) {
    for(int i = 0; i < hakukohteet.getLength(); i++) {
      Map toteutus = hakukohteet.get(i).get('toteutus');
      if(toteutus != null) {
        List organisaatiot = toteutus.get('organisaatiot');
        for(int j = 0; j < organisaatiot.getLength(); j++) {
          if(allowedOids.contains(organisaatiot.get(j))) {
            count++;
            break;
          }
        }
      }
    }
  }
  return count;")

(defn search-haut
  [organisaatio params]
  (let [orgs   (get-oids organisaatio)
        base-query (->basic-org-query (with-parents-and-children orgs))
        script (format count-haun-hakukohteet-script (org-list->comma-separated-string (with-children orgs)))]
    (search "haku-kouta" default-source-fields base-query script "hakukohteet" params)))

(defn search-hakukohteet
  [organisaatio params]
  (let [orgs (get-oids organisaatio)
        base-query {:bool {:should (vector (->basic-org-query (with-children orgs))
                                           (->terms-query :toteutus.organisaatiot (vec (with-children orgs))))
                           :minimum_should_match 1}}]
    (search "hakukohde-kouta" default-source-fields base-query nil nil params)))

(defn search-valintaperusteet
  [organisaatio params]
  (let [orgs (get-oids organisaatio)
        source-fields (conj (remove #(= % "oid") default-source-fields) "id")
        base-query (->basic-org-query (with-parents-and-children orgs))]
    (search "valintaperuste-kouta" source-fields base-query nil nil params)))