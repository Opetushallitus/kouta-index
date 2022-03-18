(ns kouta-index.filtered-list.search-unit-test
  (:require [clojure.test :refer [deftest testing is]]
            [kouta-index.filtered-list.search :as search]))

(deftest ->julkinen-filter
  (testing "returns nil if julkinen is not one of the filters"
    (let [filters {:tila "julkaistu"}]
      (is (= nil
             (search/->julkinen-filter filters)))))

  (testing "returns correct term for filter when searching for julkinen koulutus"
    (let [filters {:tila "julkaistu", :julkinen true}]
      (is (= {:term {"julkinen" true}}
             (search/->julkinen-filter filters)))))

  (testing "returns correct term for filter when searching for julkinen koulutus"
    (let [filters {:tila "julkaistu", :julkinen false}]
      (is (= {:term {"julkinen" false}}
             (search/->julkinen-filter filters))))))
