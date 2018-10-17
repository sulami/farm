(ns farm.config)

#?(:cljs (def debug? ^boolean goog.DEBUG)
   :clj (def debug? true))

(defonce game-speed 3000) ; Real seconds per game day
(defonce length-of-year 360) ; Days
(defonce field-size 300)

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
(defonce plant-seed-cost 12) ; Seeds / plant
(defonce food-per-plant 1) ; Gained upon harvesting
