(ns farm.economy-test
  (:require [clojure.test :refer :all]
            [farm.economy :refer :all]
            [farm.events :refer [initialize-db]]
            [farm.config :as config]
            [farm.utils :refer [set-in]]))

(deftest resource-price-key-test
  (testing "is appending '-price'"
    (is (= :foo-price
           (resource-price-key :foo)))))

(deftest resource-price-test
  (testing "is within bounds"
    (let [prices (repeatedly 1000 resource-price)
          lower-bound 2
          upper-bound 12]
      (is (every? #(<= lower-bound % upper-bound) prices)))))

;; Event Handler Tests

(deftest consume-food-test

  (testing "food is reduced by number of family members"
    (let* [db (initialize-db {} [:initialize-db])
           family (-> db :family count)
           db' (consume-food-handler db [:consume-food])]
      (is (= (-> db :food)
             (-> db' :food (+ family))))))

  (testing "food isn't reduced below zero"
    (let* [db (-> (initialize-db {} [:initialize-db])
                  (set-in [:family] [1 2 3])
                  (set-in [:food] 1))
           family (-> db :family count)
           db' (consume-food-handler db [:consume-food])]
      (is (= 0
             (:food db'))))))

(deftest trade-resource-handler-test
  (let* [db (initialize-db {} [:initialize-db])
         resource :food
         resource-key (resource-price-key resource)
         amount 10
         price (-> db resource-key (* amount) (quot 10))]

    (testing "buying"
      (let [db' (trade-resource-handler db [:trade-resource :buy resource amount])]

        (testing "adds the right amount of resource"
          (is (= (-> db resource)
                 (-> db' resource (- amount)))))

        (testing "deducts the right amount of money"
          (is (= (-> db :money)
                 (-> db' :money (+ price)))))))

    (testing "selling"
      (let [db' (trade-resource-handler db [:trade-resource :sell resource amount])]

        (testing "deducts the right amont of resource"
          (is (= (-> db resource)
                 (-> db' resource (+ amount)))))

        (testing "adds the right amount of money"
          (is (= (-> db :money)
                 (-> db' :money (- price)))))))))
