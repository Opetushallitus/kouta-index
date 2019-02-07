(ns kouta-index.api-test
  (:require [clojure.test :refer :all]
            [kouta-index.api :refer :all]
            [ring.mock.request :as mock]
            [clj-test-utils.elasticsearch-mock-utils :as utils]))

(intern 'clj-log.access-log 'service "kouta-index")

(comment use-fixtures :once utils/mock-embedded-elasticsearch-fixture)

(deftest api-test
  (testing "Healthcheck API test"
    (let [response (app (mock/request :get "/kouta-index/healthcheck"))]
      (is (= (:status response) 200)))))