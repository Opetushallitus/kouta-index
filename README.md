# Kouta-index

Uuden tarjonnan (kouta) palvelu, joka jakaa uuden tarjonnan indeksoitua dataa virkalijan palveluille.

## Vaatimukset

Lokaalia ajoa varten tarvitaan lokaali Elasticsearch, josta löytyy indeksoitua dataa.

## Lokaali ajo

Lokaalia ajoa varten kopioi konfiguraatiotiedoston template `dev-configuration/kouta-index.end.template`
tiedostoksi `dev-configuration/kouta-index.edn` ja lisää tiedostoon oikeat arvot:

```
{
    :elastic-url "http://127.0.0.1:9200"
    :elastic-timeout 120000
    :log-elastic-queries false
}
```

Sovelluksen voi käynnistää komennolla:

`lein run`

## ElasticSearch-kyselyiden lokitus

Jos kehittäiessä tai yksikkötestejä ajaessa haluaa nähdä lokilla ElasticSearchiin menevät kyselyt 
ja sieltä tulevat tulokset, `dev-configuration/kouta-index.edn` tiedostoon voi vaihtaa `:log-elastic-queries true`. 

Parametrilla `kouta_index_log_elastic_queries` voi myös konfiguroida sovelluksen lokittamaan 
kyselyt ja tulokset esim. pilviympäristöissä.
