(ns farm.plant
  (:require [farm.config :as config]
            [farm.utils :refer [insert-at in? update-when within-bounds]]))

(defn plant-seeds
  "Plant some seeds on a specific position."
  [db [_ position]]
  (let [new-seed (-> db :seed (- config/plant-seed-cost))
        current-plants (:plants db)
        new-plants (insert-at config/new-plant position current-plants)]
    (if (neg? new-seed)
      db
      (into db
            {:seed new-seed
             :plants new-plants}))))

(defn water-plant
  "Water a plant with position."
  [plant-with-position]
  (update-in plant-with-position [:plant :water]
             (within-bounds (partial + config/water-amount)
                            0 config/max-plant-water)))

(defn water-plants
  "Manually water the N plants with the lowest water. N = water-capacity."
  [db _]
  (let* [add-position (fn add-position [plant position]
                        {:position position
                         :plant plant})
         plants (:plants db)
         plants-with-position (map add-position plants (range))
         positions-to-water (->> plants-with-position
                                 (filter #(-> % :plant some?))
                                 (sort-by #(-> % :plant :water))
                                 (take config/water-capacity)
                                 (map :position))
         water-fn (fn [plant]
                    (update-when plant (->> plant :position (in? positions-to-water))
                                  water-plant))]
    (->> plants-with-position
         (map water-fn)
         (map :plant)
         (assoc db :plants))))

(defn update-plant-water
  "Update water on a plant depending on the weather."
  [plant weather]
  (when-not (nil? plant)
    (let [weather-mod-fn (fn weather-mod-fn [water]
                           (-> weather
                               :water-mod
                               (+ water)))]
      (update-in plant
                 [:water]
                 (within-bounds (comp weather-mod-fn dec)
                                0 config/max-plant-water)))))

(defn grow-plant
  "Grow a plant, depending on the current environment, and return it.
  The formula makes the chance of growth `-(temperature - 19)^2 + 90`% each
  step."
  [plant weather temperature]
  (when-not (nil? plant)
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
  Temperature-based death occurs the further temperature drops below 8 degrees,
  or below 0 degrees if the plant is fully grown."
  [plant weather temperature]
  (and (some? plant)
       (let [freezing-temperature (if (-> plant :age (> config/plant-age))
                                    8 0)]
         (and (-> plant :water (> 0))
              (-> freezing-temperature rand-int (- temperature) neg?)
              (-> (rand-int 100) (- 100) (- (:survival-mod weather)) neg?)))))

(defn update-plants
  "Update all plants."
  [db _]
  (let* [weather (:weather db)
         temperature (:temperature db)]
    (update-in db [:plants]
               (fn [plants]
                 (->> plants
                      (map #(grow-plant % weather temperature))
                      (map #(update-plant-water % weather))
                      (map #(when (plant-alive? % weather temperature) %)))))))

(defn harvest
  "Harvest a plant in a position adding some food."
  [db [_ position]]
  (let* [plants (:plants db)
         plant (nth plants position)
         new-plants (insert-at nil position plants)
         new-food (-> db :food (+ config/food-per-plant))
         new-seed (-> db :seed (+ config/seed-per-plant))]
    (update-when db (-> plant :age (>= config/plant-age))
                 #(into % {:food new-food
                           :seed new-seed
                           :plants new-plants}))))
