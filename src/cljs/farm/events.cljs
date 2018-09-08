(ns farm.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [farm.db :as db]
            [farm.economy :refer [consume-food trade-resource]]))

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

;; (defn plant-seeds []
;;   (swap!
;;    state
;;    (fn [current]
;;      (let [new-seed (-> current :seed (- 12))
;;            current-plants (-> current :plants)
;;            new-plants (let* [head (take-while #(not (nil? %)) current-plants)
;;                              tail (drop (+ 1 (count head)) current-plants)]
;;                         (concat head [config/new-plant] tail))]
;;        (if (> 0 new-seed)
;;          current
;;          (into current
;;                {:seed new-seed
;;                 :plants new-plants}))))))

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

;; (defn water-plants
;;   "Manually water plants."
;;   []
;;   (swap!
;;    state
;;    (fn [current]
;;      (update-in current [:plants]
;;                 (partial map #(update-plant-water % :manual))))))

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
