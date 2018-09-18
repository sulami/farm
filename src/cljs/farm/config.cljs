(ns farm.config)

(def debug?
  ^boolean goog.DEBUG)

(defonce game-speed 3000) ; Real seconds per game day
(defonce length-of-year 360) ; Days
(defonce field-size 300)

; Plant information
(defonce sapling-age 73) ; Steps
(defonce plant-age 146) ; Steps
(defonce max-plant-water 30)
(defonce optimal-temperature 19)

; Colors
(defonce plant-green [20 160 80])
(defonce plant-brown [200 150 30])

; Plant prototype
(defonce new-plant {:age 0
                    :water max-plant-water})

; Economy
(defonce plant-seed-cost 12) ; Seeds / plant
