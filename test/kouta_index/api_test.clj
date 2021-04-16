(ns kouta-index.api-test
  (:require [clojure.test :refer :all]
            [kouta-index.api :refer :all]
            [ring.mock.request :as mock]))

(intern 'clj-log.access-log 'service "kouta-index")

(deftest api-test
  (testing "Healthcheck API test"
    (let [response (app (mock/request :get "/kouta-index/healthcheck"))]
      (is (= (:status response) 200)))))