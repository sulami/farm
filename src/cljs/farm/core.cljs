(ns farm.core
    (:require [reagent.core :as reagent :refer [atom]]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]))

(defonce status
  (atom {:money 1000
         :seeds 0
         :produce 0
         :plants []}))

;; -------------------------
;; Game functions

(defn buy-seeds []
  (swap!
   status
   (fn [current]
     (let [new-money (-> current :money (- 50))
           new-seeds (-> current :seeds (+ 200))]
       (if (> 0 new-money)
         current
         (into current
               {:money new-money
                :seeds new-seeds}))))))

(defn plant-seeds []
  (swap!
   status
   (fn [current]
     (let [new-seeds (-> current :seeds (- 80))
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

(defn grow-plants []
  (swap!
   status
   (fn [current]
     (into current
           {:plants (->> current :plants (map grow-plant))}))))

(defn harvest []
  (swap!
   status
   (fn [current]
     (let* [current-plants (-> current :plants)
            new-plants (filter #(-> % :age (< 60)) current-plants)
            harvested (- (count current-plants) (count new-plants))
            new-produce (-> current :produce (+ harvested))]
       (into current
             {:produce new-produce
              :plants new-plants})))))

(defonce timer
  (js/setInterval grow-plants 1000))

(defn draw-plant [plant]
  (let [age (-> plant :age)]
    (cond
      (< age 30) "."
      (< age 60) "i"
      :else "I")))

;; -------------------------
;; Views

(defn home-page []
  [:div
   [:h2 "Welcome to the Farm"]
   [:ul
    [:li [:a {:href "/game"} "Start farming"]]
    [:li [:a {:href "/about"} "About"]]]])

(defn about-page []
  [:div [:h2 "About Farm"]
   [:p "The Farm is all about farming. "
    [:a {:href "/game"} "So start farming."]]])

(defn game-page []
  [:div [:h2 "Farm"]

   ;; Status
   [:div
    [:p (str "Money: $" (-> @status :money))]
    [:p (str "Seeds: " (-> @status :seeds))]
    [:p (str "Produce: " (-> @status :produce))]]

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
             :on-click harvest}]]

   ;; Field
   [:div
    [:p
     (interleave
      (map draw-plant (-> @status :plants))
      (repeat " "))]]
   ])

;; -------------------------
;; Routes

(defonce page (atom #'home-page))

(defn current-page []
  [:div [@page]])

(secretary/defroute "/" []
  (reset! page #'home-page))

(secretary/defroute "/game" []
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
