(ns farm.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [farm.db :as db]
            [farm.climate :refer [temperature weather]]
            [farm.economy :refer [consume-food food-price trade-resource]]
            [farm.plant :refer [plant-seeds update-plants water-plants]]
            [farm.utils :refer [set-in]]
            [farm.views :refer [timer]]))

(reg-event-db
 :initialize-db
 (fn initialize-db-handler [_ _]
   db/default-db))

;; Iterations

(reg-event-fx
 :step
 (fn step-handler [_ _]
   {:dispatch-n
    (list [:inc-game-time]
          [:update-weather]
          [:update-temperature]
          [:consume-food]
          [:update-prices]
          [:update-plants]
          [:check-lose])}))

(reg-event-db
 :inc-game-time
 (fn inc-game-time-handler [db _]
   (update-in db [:game-time] inc)))

(reg-event-db
 :update-weather
 (fn update-weather-handler [db _]
   (update-in db [:weather] weather)))

(reg-event-db
 :update-temperature
 (fn update-temperature-handler [db _]
   (set-in db [:temperature] (-> db :game-time temperature))))

(reg-event-db
 :consume-food
 consume-food)

(reg-event-db
 :update-prices
 (fn update-prices-handler [db _]
   (set-in db [:food-price] (food-price))))

(reg-event-db
 :update-plants
 update-plants)

(reg-event-fx
 :check-lose
 (fn check-lose-handler [{:keys [db]} _]
   (when (-> db :food (<= 0))
     (js/clearInterval timer)
     (js/alert "You starve."))))


;; Interactive

(reg-event-db
 :trade-resource
 trade-resource)

(reg-event-db
 :plant-seeds
 plant-seeds)

(reg-event-db
 :water-plants
 water-plants)
