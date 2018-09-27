(ns farm.plant
  (:require [farm.config :as config]
            [farm.utils :refer [insert-at in? update-when within-bounds]]))

(defn plant-seeds
  "Plant some seeds on a specific position."
  [db [_ position]]
  (let [new-seed (-> db :seed (- config/plant-seed-cost))
        current-plants (-> db :plants)
        new-plants (insert-at config/new-plant position current-plants)]
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
  "Manually water the N plants with the lowest water. N = water-power.
  XXX This needs some refactoring."
  [db _]
  (let* [add-position (fn add-position [plant position]
                        {:position position
                         :plant plant})
         plants (-> db :plants)
         plants-with-position (map add-position plants (range))
         positions-to-water (->> plants-with-position
                                 (filter #(-> % :plant nil? not))
                                 (sort-by #(-> % :plant :water))
                                 (take config/water-capacity)
                                 (map :position))
         water (fn water [plant]
                 (if (->> plant :position (in? positions-to-water))
                   (update-in plant [:plant :water]
                              (within-bounds #(+ % config/water-amount)
                                             0
                                             config/max-plant-water))
                   plant))]
    (->> plants-with-position
         (map water)
         (map :plant)
         (assoc db :plants))))

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
      (update-when plant (<= roll bar)
                   #(update-in % [:age] inc)))))

(defn plant-alive?
  "Return plant life status. Plants die if they dry out, or freeze.
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
         plant (nth plants position)
         new-plants (insert-at nil position plants)
         new-food (-> db :food (+ config/food-per-plant))]
    (update-when db (-> plant :age (< config/plant-age))
                 #(into % {:food new-food
                           :plants new-plants}))))
