(ns farm.climate-test
  (:require  [clojure.test :refer :all]
             [farm.climate :refer :all]
             [farm.config :as config]))

(deftest time->season-test
  (let [seasons (map time->season (range 1000))]

    (testing "follows the right sequence"
      (let [deduped-seasons (dedupe seasons)]
        (is (= (->> config/seasons cycle (take (count deduped-seasons)))
               deduped-seasons))))))

(deftest temperature-test
  (testing "is within bounds"
    (let [temperatures (map temperature (range 1000))]
      (is (<= (apply max temperatures) 26))
      (is (<= 0 (apply min temperatures)))))

  (testing "is warmer in summer than in winter"
    (let* [length-of-season (quot config/length-of-year 4)
           summer (temperature (* length-of-season 1.5))
           winter (temperature (* length-of-season -1.5))]
      (is (< winter summer)))))

(deftest weather-test
  (testing "is always a weather symbol"
    (let [weathers (take 1000 (iterate weather :clear))
          valid-weathers [:sunny
                          :clear
                          :overcast
                          :rain
                          :hail
                          :thunderstorm]]
      (is (->> weathers
               (some (partial contains? valid-weathers))
               nil?)))))
