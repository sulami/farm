(ns farm.core
    (:require [reagent.core :as reagent :refer [atom]]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]))

(defonce state
  (atom {:game-time 0
         :money 120
         :seeds 250
         :food 200
         :food-consumption 1
         :plants []}))

;; -------------------------
;; Game functions

(defn buy-seeds []
  (swap!
   state
   (fn [current]
     (let [new-money (-> current :money (- 8))
           new-seeds (-> current :seeds (+ 10))]
       (if (> 0 new-money)
         current
         (into current
               {:money new-money
                :seeds new-seeds}))))))

(defn plant-seeds []
  (swap!
   state
   (fn [current]
     (let [new-seeds (-> current :seeds (- 12))
           current-plants (-> current :plants)
           new-plants (->> [{:age 0}] (concat current-plants))]
       (if (> 0 new-seeds)
         current
         (into current
               {:seeds new-seeds
                :plants new-plants}))))))

(defn grow-plant [plant]
  (into plant
        {:age (-> plant :age inc)}))

(defn harvest []
  (swap!
   state
   (fn [current]
     (let* [current-plants (-> current :plants)
            new-plants (filter #(-> % :age (< 60)) current-plants)
            harvested (- (count current-plants) (count new-plants))
            new-food (-> current :food (+ harvested))]
       (into current
             {:food new-food
              :plants new-plants})))))

(defn sell []
  (swap!
   state
   (fn [current]
     (let [sold (-> current :food (* 3))
           new-money (-> current :money (+ sold))]
       (into current
             {:money new-money
              :food 0})))))

(defn grow-plants []
  (swap!
   state
   (fn [current]
     (into current
           {:plants (->> current :plants (map grow-plant))}))))

(defn consume-food []
  (swap!
   state
   (fn [current]
     (let [now (-> @state :game-time)
           consumption (-> current :food-consumption)]
       (if (-> now (mod 6) (= 0))
         (into current
               {:food (-> current :food (- consumption))})
         current)))))

(declare lose)

(defonce timer
  (let [func (fn []
               (swap! state
                      (fn [current]
                        (into current
                              {:game-time (-> current :game-time inc)})))
               (grow-plants)
               (consume-food)
               (when (-> @state :food (= 0))
                 (lose)))]
    (js/setInterval func 10)))

(defn lose []
  (js/clearInterval timer)
  (js/alert "You starve."))

(defn draw-plant [plant]
  (let [age (-> plant :age)]
    (cond
      (< age 90) "."
      (< age 180) "i"
      :else [:a {:on-click lose} "I"])))

(defn format-date [game-time]
  (let* [days (quot game-time 6)
         year (+ 1 (quot days 360))
         day (+ 1 (mod days 360))
         season (-> day (quot 90) (mod 4))]
    (str "Year " year
         " / "
         "Day " day
         " / "
         (case season
           0 "Spring"
           1 "Summer"
           2 "Autumn"
           3 "Winter"))))

;; -------------------------
;; Views

(defn game-page []
  [:div [:h2 "Medieval Farm"]

   ;; State
   [:div
    [:p (-> @state :game-time format-date)]
    [:table
     [:tr (str "Money: " (-> @state :money) "p")]
     [:tr (str "Seeds: " (-> @state :seeds))]
     [:tr (str "Food: " (-> @state :food))]]]

   ;; Actions
   [:div
    [:input {:type "button"
             :value "Buy seeds"
             :on-click buy-seeds}]
    [:input {:type "button"
             :value "Plant seeds"
             :on-click plant-seeds}]
    [:input {:type "button"
             :value "Harvest"
             :on-click harvest}]
    [:input {:type "button"
             :value "Sell food"
             :on-click sell}]]

   ;; Field
   [:div
    [:p
     (interleave
      (map draw-plant (-> @state :plants))
      (repeat " "))]]
   ])

(defn about-page []
  [:div [:h2 "About Farm"]
   [:p "The Farm is all about farming."]])

;; -------------------------
;; Routes

(defonce page (atom #'game-page))

(defn current-page []
  [:div [@page]])

(secretary/defroute "/" []
  (reset! page #'game-page))

(secretary/defroute "/about" []
  (reset! page #'about-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))
