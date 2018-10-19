(ns farm.events
  (:require #?(:cljs [cljs.spec.alpha :as s]
               :clj  [clojure.spec.alpha :as s])
            [re-frame.core :refer [reg-event-db reg-event-fx after]]
            [farm.db :as db]
            [farm.climate :refer [temperature weather]]
            [farm.economy :refer [consume-food food-price trade-resource]]
            [farm.happenings :refer [fire-happenings-handler]]
            [farm.plant :refer [harvest plant-seeds update-plants water-plants]]
            [farm.utils :refer [set-in]]
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
   (set-in db [:temperature] (-> db :game-time temperature))))

(reg-event-db
 :consume-food
 db-spec-interceptors
 consume-food)

(reg-event-db
 :update-prices
 db-spec-interceptors
 (fn update-prices-handler [db _]
   (set-in db [:food-price] (food-price))))

(reg-event-db
 :update-plants
 db-spec-interceptors
 update-plants)

(reg-event-fx
 :fire-happenings
 fire-happenings-handler)

(reg-event-fx
 :check-lose
 (fn check-lose-handler [{:keys [db]} _]
   (when (-> db :food (<= 0))
     #?(:cljs (do
        (js/clearInterval timer)
        (js/alert "You starve."))))))

;; Interactive

(reg-event-db
 :trade-resource
 db-spec-interceptors
 trade-resource)

(reg-event-db
 :water-plants
 db-spec-interceptors
 water-plants)

(reg-event-db
 :plant-seeds
 db-spec-interceptors
 plant-seeds)

(reg-event-db
 :harvest
 db-spec-interceptors
 harvest)

;; On demand

(reg-event-db
 :send-message
 db-spec-interceptors
 (fn send-message-handler [db [_ message]]
   (update-in db [:messages] #(conj % message))))
