(ns farm.plant
  (:require [farm.config :as config]
            [farm.utils :refer [within-bounds]]))

(defn plant-seeds
  "Plant some seeds on a specific position."
  [db [_ position]]
  (let [new-seed (-> db :seed (- config/plant-seed-cost))
        current-plants (-> db :plants)
        new-plants (let* [head (take position current-plants)
                          tail (-> position (+ 1) (drop current-plants))]
                     (concat head [config/new-plant] tail))]
    (if (> 0 new-seed)
      db
      (into db
            {:seed new-seed
             :plants new-plants}))))

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

(defn water-plants
  "Manually water plants."
  [db _]
  (update-in db[:plants]
             (partial map #(update-plant-water % :manual))))

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
  "Return plant life status. Plants die if they drie out, or freeze.
  Temperature-based death occurs the further temperature drops below 8 degrees."
  [plant weather temperature]
  (let [freezing-temperature (if (-> plant :age (> config/plant-age))
                               8 0)]
    (and (-> plant :water (> 0))
         (-> freezing-temperature rand-int (- temperature) neg?))))

(defn update-plants
  "Update all plants."
  [db _]
  (let* [weather (-> db :weather)
         temperature (-> db :temperature)]
    (update-in db [:plants]
               (fn [plants]
                 (->> plants
                      (map #(grow-plant % weather temperature))
                      (map #(update-plant-water % weather))
                      (map #(if (plant-alive? % weather temperature) % nil)))))))

(defn harvest
  "Harvest a plant in a position adding some food."
  [db [_ position]]
  (let* [plants (-> db :plants)
         plant (nth position plants)
         head (take position plants)
         tail (-> position (+ 1) (drop plants))
         new-plants (concat head [nil] tail)
         new-food (-> db :food (+ config/food-per-plant))]
    (if (-> plant :age (< config/plant-age))
      db
      (into db
            {:food new-food
             :plants new-plants}))))
