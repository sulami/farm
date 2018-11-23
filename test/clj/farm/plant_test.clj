(ns farm.plant-test
  (:require  [clojure.test :refer :all]
             [farm.config :as config]
             [farm.events :refer [initialize-db]]
             [farm.plant :refer :all]
             [farm.utils :refer [insert-at]]))

(def default-weather (first config/weathers))

(deftest plant-seeds-handler-test
  (let* [db (initialize-db {} [:initialize-db])
         position 5
         db' (plant-seeds-handler db [:plant-seeds position])]

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
             (plant-seeds-handler acc [:plant-seeds x]))
           db
           (range start end)))
  ([db end]
   (grow-plants-helper db 0 end)))

(deftest water-plants-test
  ;; Sets up 5 plants with -2 and 5 plants with -1 water
  (let* [db (initialize-db {} [:initialize-db])
         db' (-> db
                 (grow-plants-helper config/water-capacity)
                 (update-plants [:update-plants])
                 (update-plants [:update-plants])
                 (grow-plants-helper config/water-capacity (* config/water-capacity 2))
                 (update-plants [:update-plants])
                 (water-plants [:water-plants]))]

    (testing "doesn't change the field size"
      (is (= (-> db :plants count)
             (-> db' :plants count))))

    (testing "leaves plants alive"
      (is (= (* config/water-capacity 2)
             (->> db' :plants (filter some?) count))))

    (testing "it increases water only up to the maximum"
      (is (= config/max-plant-water
             (->> db' :plants (filter some?) (map :water) (apply max)))))

    (testing "it waters only the plants with the lowest water"
      (is (= (concat (repeat config/water-capacity config/max-plant-water)
                     (repeat config/water-capacity (- config/max-plant-water 1)))
             (->> db'
                  :plants
                  (take (* config/water-capacity 2))
                  (map :water)))))))

(deftest plant-alive?-test
  (testing "it keeps empty plots"
    (is (not (plant-alive? nil default-weather 10))))

  (testing "it kills plants if they run out of water"
    (is (-> config/new-plant
            (assoc-in [:water] 0)
            (plant-alive? default-weather 10)
            not)))

  (testing "it kills plants if it's cold"
    (is (not (plant-alive? config/new-plant default-weather 0))))

  (testing "it kills plants if the weather is bad"
    (is (not (plant-alive? config/new-plant {:survival-mod -100} 10))))

  (testing "it keeps plants alive if it's warm and they have water"
    (is (plant-alive? config/new-plant default-weather 8))))

(deftest update-plants-test
  (testing "with fresh plants it"
    (let* [db (initialize-db {} [:initialize-db])
           db' (-> db
                   (grow-plants-helper 5)
                   (update-plants [:update-plants]))]

      (testing "doesn't change the field size"
        (is (= (-> db :plants count)
               (-> db' :plants count))))

      (testing "it reduces plant water"
        (is (= (repeat 5 (dec config/max-plant-water))
               (->> db'
                    :plants
                    (map :water)
                    (take 5)))))

      (testing "it grows plants"
        (is (->> db'
                 :plants
                 (map :age)
                 (take 5)
                 (every? #(<= 0 % 1)))))))

  (testing "after many runs it"
    (let* [db (initialize-db {} [:initialize-db])
           db' (as-> db input
                 (grow-plants-helper input 5)
                 (iterate #(update-plants % [:update-plants]) input)
                 (nth input 20))]

      (testing "it reduces plant water"
        (is (= (repeat 5 (- config/max-plant-water 20))
               (->> db'
                    :plants
                    (map :water)
                    (take 5)))))

      (testing "it grows plants"
        (is (->> db'
                 :plants
                 (map :age)
                 (take 5)
                 (every? #(<= 0 % 20)))))))

  (testing "with plants running low on water"
    (let* [db (initialize-db {} [:initialize-db])
           low-water-plant (assoc-in config/new-plant [:water] 1)
           db' (-> db
                   (update-in [:plants] (partial insert-at low-water-plant 0))
                   (update-plants [:update-plants]))]

      (testing "it doesn't change the field size"
        (is (= (-> db :plants count)
               (-> db' :plants count))))

      (testing "it kills the plant"
        (is (->> db'
                 :plants
                 (every? nil?))))))

  (testing "with weather"
    (let* [medium-water (/ config/max-plant-water 2)
           medium-water-plant (assoc-in config/new-plant [:water] medium-water)
           db (-> (initialize-db {} [:intitialize-db])
                  (update-in [:plants] (partial insert-at medium-water-plant 0)))]

      (testing "it changes water according to the weather modifier"
        (doall
         (for [weather config/weathers]
           (is (= (:water-mod weather)
                  (-> db
                      (assoc-in [:weather] weather)
                      (update-plants [:update-plants])
                      :plants
                      first
                      :water
                      (+ 1)
                      (- medium-water))))))))))

(deftest harvest-handler-test
  (let [db (initialize-db {} [:initialize-db])]

    (testing "with a young plant"
      (let [db (grow-plants-helper db 2)
            db' (harvest-handler db [:harvest 0])]

        (testing "it doesn't change the field size"
          (is (= (-> db :plants count)
                 (-> db' :plants count))))

        (testing "it doesn't remove the plant"
          (is (= (->> db :plants (filter nil?) count)
                 (->> db' :plants (filter nil?) count))))

        (testing "it doesn't increase food amount"
          (is (= (-> db :food)
                 (-> db' :food))))))

    (testing "with a mature plant"
      (let* [mature-plant (assoc-in config/new-plant [:age] config/plant-age)
             db (-> db
                    (grow-plants-helper 2)
                    (update-in [:plants] #(insert-at mature-plant 3 %)))
             db' (harvest-handler db [:harvest 3])]

        (testing "it doesn't change the field size"
          (is (= (-> db :plants count)
                 (-> db' :plants count))))

        (testing "it removes the plant"
          (is (= (->> db :plants (filter nil?) count (+ 1))
                 (->> db' :plants (filter nil?) count))))

        (testing "it leaves other plants in place"
          (is (-> db' :plants (nth 1) some?)))

        (testing "it increases food amount"
          (is (= (-> db :food)
                 (-> db' :food (- config/food-per-plant)))))

        (testing "it increases seed amount"
          (is (= (-> db :seed)
                 (-> db' :seed (- config/seed-per-plant)))))))))
