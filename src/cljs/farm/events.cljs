(ns farm.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [farm.db :as db]
            [farm.economy :refer [consume-food trade-resource]]
            [farm.plant :refer [plant-seeds water-plants]]))

(reg-event-db
 :initialize-db
 (fn [_ _]
   db/default-db))

(reg-event-fx
 :step
 (fn [_ _]
   {:dispatch-n
    (list [:inc-game-time]
          [:consume-food])}))

(reg-event-db
 :inc-game-time
 (fn [db _]
   (update-in db [:game-time] inc)))

(reg-event-db
 :consume-food
 consume-food)

(reg-event-db
 :trade-resource
 trade-resource)

(reg-event-db
 :plant-seeds
 plant-seeds)

(reg-event-db
 :water-plants
 water-plants)

;; (defn update-plants []
;;   (swap!
;;    state
;;    (fn [current]
;;      (let* [weather (-> current :weather)
;;             temperature (-> current :temperature)]
;;        (update-in current [:plants]
;;                   (fn [plants]
;;                     (->> plants
;;                          (map #(grow-plant % weather temperature))
;;                          (map #(update-plant-water % weather))
;;                          (map #(if (plant-alive? % weather temperature) % nil)))))))))

;; (defn harvest []
;;   (swap!
;;    state
;;    (fn [current]
;;      (let* [current-plants (-> current :plants)
;;             new-plants (filter #(-> % :age (< config/plant-age)) current-plants)
;;             harvested (- (count current-plants) (count new-plants))
;;             new-food (-> current :food (+ harvested))]
;;        (into current
;;              {:food new-food
;;               :plants new-plants})))))
