(ns farm.events
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx]]
            [farm.db :as db]
            [farm.economy :refer [consume-food]]))

(reg-event-db
 :initialize-db
 (fn [_ _]
   db/default-db))

(reg-event-fx
 :step
 (fn [_ _]
   {:dispatch [:consume-food]}))

(reg-event-db
 :trade-resource
 (fn [db [_ action resource amount]]
   (prn action resource amount)
   db))

(reg-event-db
 :consume-food
 consume-food)

;; (defn trade-resource
;;   "Trade `number` amount of `resource` for the current price.
;;   `:resource` needs to be the key of the resource counter in global state, and
;;   its price needs to be `:{resource}-price`. Action must be either `:buy` or
;;   `:sell`."
;;   [action resource number]
;;   (swap!
;;    state
;;    (fn [current]
;;      (let* [price-key (resource-price-key resource)
;;             resource-cost (-> current price-key (* number) (quot 10))]
;;        (case action
;;          :buy (if (-> current :money (< resource-cost))
;;                 current
;;                 (-> current
;;                     (update-in [:money] #(- % resource-cost))
;;                     (update-in [resource] #(+ % number))))
;;          :sell (if (-> current resource (< number))
;;                  current
;;                  (-> current
;;                      (update-in [:money] #(+ % resource-cost))
;;                      (update-in [resource] #(- % number)))))))))

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
