(ns farm.climate-test
  (:require  [clojure.test :refer :all]
             [farm.climate :refer :all]
             [farm.events :refer [initialize-db]]
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

(deftest consume-wood-handler-test
  (let [db (initialize-db {} [:initialize-db])]

    (testing "it consumes no wood above livable temperature"
      (is (= (:wood db)
             (-> db
                 (assoc-in [:temperature] config/livable-temperature)
                 (consume-wood-handler [:consume-wood])
                 :wood))))

    (testing "it consumes some wood below livable temperature"
      (is (= (:wood db)
             (-> db
                 (assoc-in [:temperature] (- config/livable-temperature 4))
                 (consume-wood-handler [:consume-wood])
                 :wood
                 (+ 2)))))

    (testing "it consumes some more wood far below livable temperature"
      (is (= (:wood db)
             (-> db
                 (assoc-in [:temperature] (- config/livable-temperature 8))
                 (consume-wood-handler [:consume-wood])
                 :wood
                 (+ 4)))))

    (testing "it doesn't consume more wood then there is"
      (is (= 0
             (-> db
                 (assoc-in [:temperature] (- config/livable-temperature 8))
                 (assoc-in [:wood] 1)
                 (consume-wood-handler [:consume-wood])
                 :wood))))))
