(ns farm.config)

#?(:cljs (def debug? ^boolean goog.DEBUG)
   :clj (def debug? true))

(defonce game-speed 3000) ; Real seconds per game day
(defonce length-of-year 360) ; Days
(defonce field-size 300)

; Time
(defonce days-of-the-week
  ["Monday"
   "Tuesday"
   "Wednesday"
   "Thursday"
   "Friday"
   "Saturday"
   "Sunday"])
(defonce seasons
  ["Spring"
   "Summer"
   "Autumn"
   "Winter"])

; Weather
(defonce weathers
  [{:name "Clear"
    :probability 8
    :temperature-mod 1
    :water-mod 0
    :survival-mod 0}
   {:name "Overcast"
    :probability 8
    :temperature-mod 0
    :water-mod 0
    :survival-mod 0}
   {:name "Sunny"
    :probability 8
    :temperature-mod 3
    :water-mod -1
    :survival-mod 0}
   {:name "Rain"
    :probability 8
    :temperature-mod -2
    :water-mod 5
    :survival-mod -5}
   {:name "Hail"
    :probability 1
    :temperature-mod -3
    :water-mod 3
    :survival-mod -15}
   {:name "Thunderstorm"
    :probability 1
    :temperature-mod -1
    :water-mod 4
    :survival-mod -10}])

; Plant information
(defonce sapling-age 73) ; Steps
(defonce plant-age 146) ; Steps
(defonce max-plant-water 30)
(defonce optimal-temperature 19)

; Farming - TODO Make these dynamic and move them to db
(defonce water-capacity 5) ; How many plants can be watered in one go
(defonce water-amount 5) ; How much plants get watered per go

; Colors
(defonce plant-green [20 160 80])
(defonce plant-brown [200 150 30])

; Plant prototype
(defonce new-plant {:type :wheat
                    :age 0
                    :water max-plant-water})

; Economy
(defonce taxes 50)
(defonce plant-seed-cost 12) ; Seeds / plant
(defonce food-per-plant 18) ; Gained upon harvesting
(defonce wood-per-chop 12)

; Happenings
(defonce manual-happenings
  [{:game-time 7
    :event [:send-message "You survived one week. Congratulations!"]}
   {:game-time 360
    :event [:send-message "It's a new year."]}])
(defonce tax-happenings
  (map
   (fn [game-time]
     {:game-time game-time
      :event [:collect-taxes]})
   (range 30 (* 30 12 30) 30)))
(defonce happenings
  (vec (concat manual-happenings
               tax-happenings)))
