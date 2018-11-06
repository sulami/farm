(ns farm.climate-test
  (:require  [clojure.test :refer :all]
             [farm.climate :refer :all]
             [farm.config :as config]))

(def default-weather (first config/weathers))

(deftest time->season-test
  (let [seasons (map time->season (range 1000))]

    (testing "it follows the right sequence"
      (let [deduped-seasons (dedupe seasons)]
        (is (= (->> config/seasons cycle (take (count deduped-seasons)))
               deduped-seasons))))))

(deftest temperature-test
  (testing "it is within bounds"
    (let [temperatures (map #(temperature % default-weather) (range 1000))]
      (is (<= (apply max temperatures) 26))
      (is (<= 0 (apply min temperatures)))))

  (testing "it is warmer in summer than in winter"
    (let* [length-of-season (quot config/length-of-year 4)
           summer (temperature (* length-of-season 1.5) default-weather)
           winter (temperature (* length-of-season -1.5) default-weather)]
      (is (< winter summer))))

  (testing "it applies the weather modifier"
    (let [clear-weather {:temperature-mod 0}
          sunny-weather {:temperature-mod 5}
          clear-temperature (temperature 0 clear-weather)
          sunny-temperature (temperature 0 sunny-weather)]
      (is (= 5.0
             (- sunny-temperature clear-temperature))))))

(deftest weather-test
  (testing "it is always a valid weather"
    (let [weathers (take 1000 (iterate weather (first config/weathers)))
          valid-weathers config/weathers]
      (is (->> weathers
               (some (partial contains? valid-weathers))
               nil?)))))
