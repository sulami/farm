(ns farm.economy-test
  (:require [clojure.test :refer :all]
            [farm.economy :refer :all]
            [farm.events :refer [initialize-db]]))

(deftest sanity-test
  (testing "Sanity"
    (is (= 1 1) "Insanity")))

(deftest resource-price-key-test
  (testing "is appending '-price'"
    (is (= (resource-price-key :foo)
           :foo-price))))

(deftest food-price-test
  (testing "is within bounds"
    (let [prices (for [n (range 1000)]
                   (food-price))
          lower-bound 2
          upper-bound 12]
      (is (= (->> prices
                  (filter #(or (< % lower-bound)
                               (< upper-bound %)))
                  distinct)
             [])))))

;; Event Handler Tests

(deftest consume-food-test
  (testing "food is reduced by number of family members"
    (let* [db (initialize-db {} [:initialize-db])
           family (-> db :family count)
           event [:consume-food]
           db' (consume-food db event)]
      (is (= (-> db' :food (+ family))
             (-> db :food))))))

(deftest trade-resource-test
  (let* [db (initialize-db {} [:initialize-db])
         resource :food
         resource-key (resource-price-key resource)
         amount 10
         price (-> db resource-key (* amount) (quot 10))]

    (testing "buying"
      (let [db' (trade-resource db [:trade-resource :buy resource amount])]
        (testing "adds the right amount of resource"
          (is (= (-> db' resource (- amount))
                 (-> db resource))))
        (testing "deducts the right amount of money"
          (is (= (-> db' :money (+ price))
                 (-> db :money))))))

    (testing "selling"
      (let [db' (trade-resource db [:trade-resource :sell resource amount])]
        (testing "deducts the right amont of resource"
          (is (= (-> db' resource (+ amount))
                 (-> db resource))))
        (testing "adds the right amount of money"
          (is (= (-> db' :money (- price))
                 (-> db :money))))))))
