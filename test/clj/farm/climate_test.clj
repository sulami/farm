(ns farm.climate-test
  (:require  [clojure.test :refer :all]
             [farm.climate :refer :all]))

(deftest time->season-test
  (let [seasons (map time->season (range 1000))]

    (testing "is within bounds"
      (is (= (apply max seasons) 3))
      (is (= (apply min seasons) 0)))

    (testing "follows the right sequence"
      (let [deduped-seasons (dedupe seasons)]
        (is (= deduped-seasons
               (->> (range 4) cycle (take (count deduped-seasons)))))))))

(deftest temperature-test
  (let [temperatures (map temperature (range 1000))]
    (testing "is within bounds"
      (is (<= (apply max temperatures) 26))
      (is (<= 0 (apply min temperatures))))))
