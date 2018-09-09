(ns farm.config)

(def debug?
  ^boolean goog.DEBUG)

(defonce game-speed 3000) ; Real seconds per game day
(defonce length-of-year 360) ; Days
(defonce field-size 300)
(defonce sapling-age 73) ; Steps
(defonce plant-age 146) ; Steps
(defonce max-plant-water 30)
(defonce optimal-temperature 19)
(defonce plant-green [20 160 80])
(defonce plant-brown [200 150 30])

(defonce new-plant {:age 0
                    :water max-plant-water})
