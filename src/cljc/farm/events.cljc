(ns farm.events
  (:require [clojure.spec.alpha :as s]
            #?(:cljs [reagent.format :refer [format]])
            [re-frame.core :refer [->interceptor after reg-event-db reg-event-fx]]
            [farm.db :as db]
            [farm.climate :refer [consume-wood-handler temperature weather]]
            [farm.economy :refer [consume-food-handler resource-price trade-resource-handler]]
            [farm.happenings :refer [collect-taxes-handler fire-happenings-handler]]
            [farm.messages :refer [send-message]]
            [farm.plant :refer [harvest-handler plant-seeds-handler update-plants water-plants]]
            [farm.resource :refer [chop-wood-handler hunt-handler]]
            [farm.utils :refer [check-lose-handler lose]]
            [farm.views :refer [timer]]))

;; Spec Validation

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

(def check-spec-interceptor
  (after (partial check-and-throw :farm.db/db)))

(def db-spec-interceptors
  [check-spec-interceptor])

;; Boilerplate

(defn initialize-db [_ _]
  db/default-db)

(reg-event-db
 :initialize-db
 initialize-db)

;; Iterations

(reg-event-fx
 :step
 db-spec-interceptors
 (fn step-handler [_ _]
   {:dispatch-n
    (list [:inc-game-time]
          [:update-weather]
          [:update-temperature]
          [:consume-food]
          [:consume-wood]
          [:update-prices]
          [:update-plants]
          [:fire-happenings]
          [:check-lose])}))

(reg-event-db
 :inc-game-time
 db-spec-interceptors
 (fn inc-game-time-handler [db _]
   (update-in db [:game-time] inc)))

(reg-event-db
 :update-weather
 db-spec-interceptors
 (fn update-weather-handler [db _]
   (update-in db [:weather] weather)))

(reg-event-db
 :update-temperature
 db-spec-interceptors
 (fn update-temperature-handler [db _]
   (assoc-in db [:temperature]
           (-> db
               :game-time
               (temperature (:weather db))))))

(reg-event-db
 :consume-food
 db-spec-interceptors
 consume-food-handler)

(reg-event-db
 :consume-wood
 db-spec-interceptors
 consume-wood-handler)

(reg-event-db
 :update-prices
 db-spec-interceptors
 (fn update-prices-handler [db _]
   (-> db
       (assoc-in [:food-price] (resource-price))
       (assoc-in [:seed-price] (resource-price))
       (assoc-in [:wood-price] (resource-price)))))

(reg-event-db
 :update-plants
 db-spec-interceptors
 update-plants)

(reg-event-fx
 :fire-happenings
 fire-happenings-handler)

(reg-event-fx
 :check-lose
 check-lose-handler)

(reg-event-fx
 :lose
 (fn lose-handler
   [{:keys [db]} [_ reason]]
   #?(:cljs (js/clearInterval timer))
   (lose db reason)))

;; Happenings
(reg-event-fx
 :collect-taxes
 collect-taxes-handler)

;; Messages

(reg-event-db
 :send-message
 db-spec-interceptors
 (fn send-message-handler [db [_ message]]
   (send-message db message)))

;; Interactive

;; Deleady actions work like this:
;; You dispatch `[:delayed-action <time> <event>]`
;; The delayed action handler triggers the animation and schedules the actual
;; triggered without a delay.
(reg-event-fx
;; action to happen afterwards. For simple testing, the actions can still be
 :delayed-action
 (fn delayed-action-handler
   [{:keys [db]} [_ delay action]]
   {:db (assoc-in db [:activity] [action delay])
    :dispatch-later [{:ms delay :dispatch [:finish-action action]}]}))

(reg-event-fx
 :finish-action
 (fn finish-action-handler [{:keys [db]} [_ action]]
   {:db (assoc-in db [:activity] [[:idle] 0])
    :dispatch action}))

(reg-event-db
 :trade-resource
 db-spec-interceptors
 trade-resource-handler)

(reg-event-db
 :water-plants
 db-spec-interceptors
 water-plants)

(reg-event-db
 :plant-seeds
 db-spec-interceptors
 plant-seeds-handler)

(reg-event-db
 :harvest
 db-spec-interceptors
 harvest-handler)

(reg-event-db
 :chop-wood
 db-spec-interceptors
 chop-wood-handler)

(reg-event-db
 :hunt
 db-spec-interceptors
 hunt-handler)
