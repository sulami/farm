(ns farm.climate
  (:require [farm.config :as config]
            [farm.utils :refer [fuzz]]))

(defn time->season [game-time]
  (nth
   config/seasons
   (-> game-time
       (quot (quot config/length-of-year 4))
       (mod 4))))

(defn temperature
  "Sine wave temperature between 26 and 0 degrees."
  [game-time]
  (-> game-time
      (mod config/length-of-year) ; Day in the year
      (/ config/length-of-year) ; %age of the year
      (* 2 Math/PI)
      Math/sin
      (* 10) ; Modifier
      (+ 13)
      (fuzz 3))) ; Baseline

(defn weather
  "Update the weather, based on the current weather.
  It's more likely to stay unchanged than to change, and there are certain
  probabilities for each different weather. Random beyond that."
  [current]
  (rand-nth (concat (repeat 100 current)
                    (repeat 8 :sunny)
                    (repeat 8 :clear)
                    (repeat 8 :overcast)
                    (repeat 8 :rain)
                    (repeat 1 :hail)
                    (repeat 1 :thunderstorm))))
