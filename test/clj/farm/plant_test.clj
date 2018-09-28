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
      (is (= (-> db :seed)
             (-> db' :seed (+ config/plant-seed-cost)))))

    (testing "inserts a new plant"
      (is (= config/new-plant
             (-> db' :plants (nth position)))))

    (testing "inserts exactly one plant"
      (is (= (-> db :plants count (- 1))
             (->> db' :plants (filter nil?) count))))

    (testing "doesn't change the field size"
      (is (= (-> db :plants count)
             (-> db' :plants count))))))

(defn grow-plants-helper
  "Helper to grow n plants on the field in db."
  ([db start end]
   (reduce (fn planting-reducer [acc x]
             (plant-seeds acc [:plant-seeds x]))
           db
           (range start end)))
  ([db end]
   (grow-plants-helper db 0 end)))

(deftest water-plant-water-test
  ;; Sets up 5 plants with -2 and 5 plants with -1 water
  (let* [db (initialize-db {} [:initialize-db])
         db' (-> db
                 (grow-plants-helper 5)
                 (update-plants [:update-plants])
                 (update-plants [:update-plants])
                 (grow-plants-helper 5 10)
                 (update-plants [:update-plants])
                 (water-plants [:water-plants]))]

    (testing "doesn't change the field size"
      (is (= (-> db :plants count)
             (-> db' :plants count))))

    (testing "leaves plants alive"
      (is (= 10
             (->> db' :plants (filter some?) count))))

    (testing "waters only the plants with the lowest water"
      (is (= (repeat config/water-capacity config/max-plant-water)
             (->> db'
                  :plants
                  (take config/water-capacity)
                  (map :water)))))))

(deftest update-plants-water-test
  (let* [db (initialize-db {} [:initialize-db])
         db' (-> db
                 #_(update-plants [:update-plants]))]))

(deftest harvest-test
  (let* [db (initialize-db {} [:initialize-db])
         db' (-> db
                 (grow-plants-helper 2)
                 (harvest [:harvest 0]))]

    (testing "removes the plant"
      (is (= (-> db :plants count (- 1))
             (->> db' :plants (filter nil?) count))))

    (testing "leaves other plants in place"
      (is (-> db' :plants (nth 1) some?)))

    (testing "doesn't change the field size"
      (is (= (-> db :plants count)
             (-> db' :plants count))))

    (testing "increases food amount"
      (is (= (-> db :food)
             (-> db' :food (- config/food-per-plant)))))))
