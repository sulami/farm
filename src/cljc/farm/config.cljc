(ns farm.config)

#?(:cljs (def debug? ^boolean goog.DEBUG)
   :clj (def debug? true))

(def game-speed 3000) ; Real seconds per game day
(def length-of-year 360) ; Days
(def field-size 300)
(def loss-messages
  {:starving "You run out of food and slowly starve to death."
   :debt "After being in debt, your farm gets taken over by your lord. You die in his dungeon shortly after."})

; Time
(def days-of-the-week
  ["Monday"
   "Tuesday"
   "Wednesday"
   "Thursday"
   "Friday"
   "Saturday"
   "Sunday"])
(def length-of-week
  (count days-of-the-week))
(def seasons
  ["Spring"
   "Summer"
   "Autumn"
   "Winter"])

; Weather
(def weathers
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
(def sapling-age 73) ; Steps
(def plant-age 146) ; Steps
(def max-plant-water 30)
(def optimal-temperature 19)

; Farming - TODO Make these dynamic and move them to db
(def water-capacity 5) ; How many plants can be watered in one go
(def water-amount 5) ; How much plants get watered per go

; Colors
(def plant-green [20 160 80])
(def plant-brown [200 150 30])

; Plant prototype
(def new-plant {:type :wheat
                    :age 0
                    :water max-plant-water})

; Economy
(def taxes 50)
(def plant-seed-cost 12) ; Seeds / plant
(def food-per-plant 18) ; Gained upon harvesting
(def wood-per-chop 12)

; Happenings
(def manual-happenings
  [{:game-time 7
    :event [:send-message "You survived one week. Congratulations!"]}
   {:game-time 360
    :event [:send-message "It's a new year."]}])
(def tax-happenings
  (map
   (fn [game-time]
     {:game-time game-time
      :event [:collect-taxes]})
   (range 30 (* 30 12 30) 30)))
(def happenings
  (vec (concat manual-happenings
               tax-happenings)))
