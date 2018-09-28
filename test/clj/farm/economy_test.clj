(ns farm.economy-test
  (:require [clojure.test :refer :all]
            [farm.economy :refer :all]
            [farm.events :refer [initialize-db]]))

(deftest resource-price-key-test
  (testing "is appending '-price'"
    (is (= :foo-price
           (resource-price-key :foo)))))

(deftest food-price-test
  (testing "is within bounds"
    (let [prices (repeatedly 1000 food-price)
          lower-bound 2
          upper-bound 12]
      (is (every? #(<= lower-bound % upper-bound) prices)))))

;; Event Handler Tests

(deftest consume-food-test
  (testing "food is reduced by number of family members"
    (let* [db (initialize-db {} [:initialize-db])
           family (-> db :family count)
           event [:consume-food]
           db' (consume-food db event)]
      (is (= (-> db :food)
             (-> db' :food (+ family)))))))

(deftest trade-resource-test
  (let* [db (initialize-db {} [:initialize-db])
         resource :food
         resource-key (resource-price-key resource)
         amount 10
         price (-> db resource-key (* amount) (quot 10))]

    (testing "buying"
      (let [db' (trade-resource db [:trade-resource :buy resource amount])]

        (testing "adds the right amount of resource"
          (is (= (-> db resource)
                 (-> db' resource (- amount)))))

        (testing "deducts the right amount of money"
          (is (= (-> db :money)
                 (-> db' :money (+ price)))))))

    (testing "selling"
      (let [db' (trade-resource db [:trade-resource :sell resource amount])]

        (testing "deducts the right amont of resource"
          (is (= (-> db resource)
                 (-> db' resource (+ amount)))))

        (testing "adds the right amount of money"
          (is (= (-> db :money)
                 (-> db' :money (- price)))))))))
