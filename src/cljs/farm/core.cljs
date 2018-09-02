(ns farm.core
    (:require [reagent.core :as reagent :refer [atom]]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]))

(defonce status
  (atom {:money 1000
         :seeds 0}))

;; -------------------------
;; Game functions

(defn buy-seeds []
  (swap!
   status
   (fn [current]
     (let [new-money (-> current :money (- 50))
           new-seeds (-> current :seeds (+ 200))]
       (into current
             {:money new-money
              :seeds new-seeds})))))

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
    [:p (str "Seeds: " (-> @status :seeds))]]

   ;; Actions
   [:div
    [:input {:type "button"
             :value "Buy seeds"
             :on-click buy-seeds}]]
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
