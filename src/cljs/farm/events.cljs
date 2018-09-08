(ns farm.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [farm.db :as db]
            [farm.economy :refer [consume-food trade-resource]]
            [farm.plant :refer [plant-seeds update-plants water-plants]]))

(reg-event-db
 :initialize-db
 (fn [_ _]
   db/default-db))

(reg-event-fx
 :step
 (fn [_ _]
   {:dispatch-n
    (list [:inc-game-time]
          [:consume-food]
          [:update-plants])}))

(reg-event-db
 :inc-game-time
 (fn [db _]
   (update-in db [:game-time] inc)))

(reg-event-db
 :consume-food
 consume-food)

(reg-event-db
 :update-plants
 update-plants)

(reg-event-db
 :trade-resource
 trade-resource)

(reg-event-db
 :plant-seeds
 plant-seeds)

(reg-event-db
 :water-plants
 water-plants)
