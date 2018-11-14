(ns farm.climate
  (:require [farm.config :as config]))

(defn time->season [game-time]
  (nth
   config/seasons
   (-> game-time
       (quot (quot config/length-of-year 4))
       (mod 4))))

(defn temperature
  "Sine wave temperature between 26 and 0 degrees."
  [game-time weather]
  (-> game-time
      (mod config/length-of-year)
      (/ config/length-of-year)
      (* 2 Math/PI)
      Math/sin
      (* 10)
      (+ 13)
      (+ (:temperature-mod weather))))

(defn weather
  "Update the weather, based on the current weather.
  It's more likely to stay unchanged than to change, and there are certain
  probabilities for each different weather. Random beyond that."
  [current]
  (let [weathers (->> config/weathers
                      (map #(repeat (:probability %) %))
                      (apply concat))]
    (rand-nth (concat (repeat 100 current)
                      weathers))))

(defn consume-wood-handler
  "Consumes wood based on the temperature."
  [db _]
  (let [amount (-> db
                   :temperature
                   (- config/livable-temperature)
                   (* -1)
                   (max 0)
                   (/ 2)
                   int)]
    (update-in db [:wood] #(-> (- % amount)
                               (max 0)))))
