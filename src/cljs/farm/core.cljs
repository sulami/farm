(ns farm.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.format :refer [format]]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant]
            [clojure.string :as str]))

;; Constants

(defonce game-speed 1000) ; Real seconds per step
(defonce length-of-day 6) ; Steps
(defonce length-of-year 360) ; Days

(defonce state
  (atom {:game-time 0
         :money 120
         :seeds 250
         :food 600
         :food-price 8
         :temperature 10
         :family [{:name "You"
                   :age 20}
                  {:name "Your wife"
                   :age 18}]
         :plants []}))

;; Game functions

(defn set-in
  "Like update in, but just sets."
  [m ks v]
  (update-in m ks (constantly v)))

(defn time->season [game-time]
  (-> game-time
      (quot length-of-day)
      (quot 90)
      (mod 4)))

(defn food-price
  "Determine the food price, max(2d6), 7-8ish."
  [game-time]
  (max (+ (rand-int 6) (rand-int 6))
       (+ (rand-int 6) (rand-int 6))))

(defn temperature
  "Sine wave temperature between 30 and -10 degrees."
  [game-time]
  (-> game-time
      (/ length-of-day)
      (mod length-of-year) ; Day in the year
      (/ length-of-year) ; %age of the year
      (* 2 Math/PI)
      Math/sin
      (* 20) ; Modifier
      (+ 10))) ; Baseline

(defn buy-seeds []
  (swap!
   state
   (fn [current]
     (if (-> current :money (< 8))
       current
       (-> current
           (update-in [:money] #(- % 8))
           (update-in [:seeds] #(+ % 10)))))))

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

(defn grow-plant
  "Grow a plant, depending on the current environment, and return it.
  Wheat grows best between 21 and 24 degrees. The formula makes the chance of
  growth `-(temperature - 21)^2 + 90`%."
  [plant]
  (let* [roll (rand-int 100)
         bar (-> @state :temperature (- 21) (Math/pow 2) (* -1) (+ 90))]
    (if (> roll bar)
      plant
      (update-in plant [:age] inc))))

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
     (let* [sold (-> current :food)
            made (-> current :food-price (* sold))
            new-money (-> current :money (+ made))]
       (into current
             {:money new-money
              :food 0})))))

(defn grow-plants []
  (swap!
   state
   (fn [current]
     (update-in current [:plants] #(map grow-plant %)))))

(defn consume-food []
  (swap!
   state
   (fn [current]
     (let [consumption (-> current :family count)]
       (update-in current [:food] #(- % consumption))))))

(declare lose)

(defn step []
  (swap! state #(update-in % [:game-time] inc))
  (when (-> @state :game-time (mod length-of-day) (= 0))
    ; New day
    (do
      (consume-food)
      (swap! state #(set-in % [:food-price] (food-price 0)))
      (swap! state #(set-in % [:temperature]
                            (-> @state :game-time temperature)))))
  (grow-plants)
  (when (-> @state :food (= 0))
    (lose)))

(defonce timer
  (js/setInterval step game-speed))

(defn lose []
  (js/clearInterval timer)
  (js/alert "You starve."))

(defn draw-plant [plant]
  (let [age (-> plant :age)]
    (cond
      (< age 260) "."
      (< age 520) "i"
      :else [:a {:on-click lose} "I"])))

(defn format-person [person]
  (format "%s (%i)"
          (:name person)
          (:age person)))

(defn format-date [game-time]
  (let* [days (quot game-time length-of-day)
         year (+ 1 (quot days length-of-year))
         day (+ 1 (mod days length-of-year))
         season (time->season game-time)]
    (str "Year " year
         " / "
         "Day " day
         " / "
         (case season
           0 "Spring"
           1 "Summer"
           2 "Autumn"
           3 "Winter"))))

;; Views

(defn game-page []
  [:div [:h2 "Medieval Farm"]

   ;; State
   [:div
    [:p (-> @state :game-time format-date)]
    [:table
     [:tr (str "Family: " (->> @state :family
                               (map format-person)
                               (str/join ", ")))]
     [:tr (format "Money: %ip" (-> @state :money))]
     [:tr (str "Seeds: " (-> @state :seeds))]
     [:tr (str "Food: " (-> @state :food))]
     [:tr (format "Food price: %ip" (-> @state :food-price))]
     [:tr (format "Temperature: %.1f°C" (-> @state :temperature))]]]

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

;; Routes

(defonce page (atom #'game-page))

(defn current-page []
  [:div [@page]])

(secretary/defroute "/" []
  (reset! page #'game-page))

(secretary/defroute "/about" []
  (reset! page #'about-page))

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
