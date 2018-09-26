(ns farm.economy-test
  (:require [clojure.test :refer :all]
            [farm.economy :refer :all]
            [farm.events :refer [initialize-db]]))

(deftest sanity-test
  (testing "Sanity"
    (is (= 1 1) "Insanity")))

(deftest consume-food-test
  (testing "food is reduced by number of family members"
    (let* [db (initialize-db {} [:initialize-db])
           family (-> db :family count)
           event [:consume-food]
           db' (consume-food db event)]
      (is (= (-> db :food)
             (-> db' :food (+ family)))))))
