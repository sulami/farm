(ns farm.plant
  (:require [farm.config :as config]
            [farm.utils :refer [within-bounds]]))

(defn update-plant-water
  "Update water on a plant depending on the weather."
  [plant weather]
  (if (nil? plant)
    nil
    (update-in plant
               [:water]
               (-> (case weather
                     :manual #(+ % 10)
                     :sunny #(- % 2)
                     :rain inc
                     :hail inc
                     :thunderstorm inc
                     #(- % 1))
                   (within-bounds 0 config/max-plant-water)))))

(defn grow-plant
  "Grow a plant, depending on the current environment, and return it.
  The formula makes the chance of growth `-(temperature - 19)^2 + 90`% each
  step."
  [plant weather temperature]
  (if (nil? plant)
    nil
    (let* [roll (rand-int 100)
           bar (-> temperature
                   (- config/optimal-temperature)
                   (Math/pow 2)
                   (* -1)
                   (+ 90)
                   (+ (-> plant :water (quot 3))))]
      (if (> roll bar)
        plant
        (update-in plant [:age] inc)))))

(defn plant-alive?
  "Plants die if they drie out, or freeze.
  Temperature-based death occurs the further temperature drops below 8 degrees."
  [plant weather temperature]
  (let [freezing-temperature (if (-> plant :age (> config/plant-age))
                               8 0)]
    (and (-> plant :water (> 0))
         (-> freezing-temperature rand-int (- temperature) neg?))))
