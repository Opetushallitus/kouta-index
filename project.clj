(defproject kouta-index "0.2.0-SNAPSHOT"
  :description "Kouta-index"
  :repositories [["oph-releases" "https://artifactory.opintopolku.fi/artifactory/oph-sade-release-local"]
                 ["oph-snapshots" "https://artifactory.opintopolku.fi/artifactory/oph-sade-snapshot-local"]
                 ["ext-snapshots" "https://artifactory.opintopolku.fi/artifactory/ext-snapshot-local"]]
  :managed-dependencies [[org.flatland/ordered "1.5.7"]]
  :dependencies [[org.clojure/clojure "1.10.0"]
                 ; Rest + server
                 [metosin/compojure-api "1.1.13"]
                 [compojure "1.6.1"]
                 [ring/ring-core "1.7.1"]
                 [ring/ring-jetty-adapter "1.7.1"]
                 [ring-cors "0.1.13"]
                 ; Logging
                 [oph/clj-log "0.3.1-SNAPSHOT" :exclusions [com.fasterxml.jackson.core/jackson-annotations]]
                 [org.clojure/tools.logging "1.1.0"]
                 [org.apache.logging.log4j/log4j-api "2.17.0"]
                 [org.apache.logging.log4j/log4j-core "2.17.0"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.17.0"]
                 [clj-log4j2 "0.3.0"]
                 ; Configuration
                 [fi.vm.sade.java-utils/java-properties "0.1.0-SNAPSHOT"]
                 [environ "1.1.0"]
                 [cprop "0.1.13"]
                 ; Elasticsearch
                 [oph/clj-elasticsearch "0.4.0-SNAPSHOT"]]
  :ring {:handler kouta-index.api/app
         :init kouta-index.api/init
         ;:destroy kouta-index.core/destroy
         :browser-uri "kouta-index/swagger"}
  :env {:name "kouta-index"}
  :jvm-opts ["-Dlog4j.configurationFile=test/resources/log4j2.properties" "-Dconf=dev-configuration/kouta-index.edn"]
  :target-path "target/%s"
  :plugins [[lein-ring "0.12.5"]
            [lein-environ "1.1.0"]]
  :profiles {:dev {:plugins [[lein-cloverage "1.0.11" :exclusions [org.clojure/clojure]]]}
             :test {:dependencies [[pjstadig/humane-test-output "0.11.0"]
                                   [ring/ring-mock "0.3.2"]
                                   [oph/clj-test-utils "0.3.0-SNAPSHOT"]]
                    :injections [(require 'pjstadig.humane-test-output)
                                 (pjstadig.humane-test-output/activate!)
                                 (require '[clj-test-utils.elasticsearch-docker-utils :as utils])
                                 (utils/global-docker-elastic-fixture)]}
             :ci-test {:dependencies [[ring/ring-mock "0.3.2"]] :jvm-opts ["-Dlog4j.configurationFile=test/resources/log4j2.properties" "-Dconf=ci-configuration/kouta-index.edn"]}
             :uberjar {:ring {:port 8080}}}
  :aliases {"run" ["ring" "server" "3006"]
            "uberjar" ["do" "clean" ["ring" "uberjar"]]
            "test" ["with-profile" "+test" "test"]
            "ci-test" ["with-profile" "+ci-test" "test"]
            "cloverage" ["with-profile" "+test" "cloverage"]})

