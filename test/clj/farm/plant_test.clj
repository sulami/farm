(ns farm.plant-test
  (:require  [clojure.test :refer :all]
             [farm.config :as config]
             [farm.events :refer [initialize-db]]
             [farm.plant :refer :all]))

(deftest plant-seeds-test
  (let* [db (initialize-db {} [:initialize-db])
         position 5
         db' (plant-seeds db [:plant-seeds position])]

    (testing "reduces seed amount"
      (is (= (-> db' :seed (+ config/plant-seed-cost))
             (-> db :seed))))

    (testing "inserts a new plant"
      (is (= (-> db' :plants (nth position))
             config/new-plant)))

    (testing "inserts exactly one plant"
      (is (= (->> db' :plants (filter nil?) count)
             (-> db :plants count (- 1)))))

    (testing "doesn't change the field size"
      (is (= (-> db' :plants count)
             (-> db :plants count))))))

(deftest water-plant-water-test
  (let* [db (initialize-db {} [:initialize-db])
         db' (water-plants db [:water-plants])]))

(deftest update-plants-water-test
  (let* [db (initialize-db {} [:initialize-db])
         db' (update-plants db [:update-plants])]))

(deftest harvest-test
  (let* [db (initialize-db {} [:initialize-db])
         position 5
         db' (-> db
                 (plant-seeds [:plant-seeds position])
                 (harvest [:harvest position]))]

    (testing "removes the plant"
      (is (= (->> db' :plants (filter nil?) count)
             (-> db :plants count))))

    (testing "doesn't change the field size"
      (is (= (-> db' :plants count)
             (-> db :plants count))))

    (testing "increases food amount"
      (is (= (-> db' :food (- config/food-per-plant))
             (-> db :food))))))
