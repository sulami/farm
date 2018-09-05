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
(defonce sapling-age 350) ; Steps
(defonce plant-age 700) ; Steps
(defonce optimal-temperature 19)

(defonce state
  (atom {:game-time 0
         :money 120
         :seeds 250
         :food 587
         :seed-price 8
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
  "Sine wave temperature between 23 and 3 degrees."
  [game-time]
  (-> game-time
      (/ length-of-day)
      (mod length-of-year) ; Day in the year
      (/ length-of-year) ; %age of the year
      (* 2 Math/PI)
      Math/sin
      (* 10) ; Modifier
      (+ 13))) ; Baseline

(defn buy-seeds [number]
  (swap!
   state
   (fn [current]
     (let [seed-cost (-> current :seed-price (* number) (quot 10))]
       (if (-> current :money (< seed-cost))
         current
         (-> current
             (update-in [:money] #(- % seed-cost))
             (update-in [:seeds] #(+ % number))))))))

(defn sell-food [number]
  (swap!
   state
   (fn [current]
     (let [food-cost (-> current :food-price (* number) (quot 10))]
       (if (-> current :food (< number))
         current
         (-> current
             (update-in [:money] #(+ % food-cost))
             (update-in [:food] #(- % number))))))))

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
  The formula makes the chance of growth `-(temperature - 19)^2 + 90`% each
  step."
  [plant]
  (let* [roll (rand-int 100)
         bar (-> @state :temperature (- optimal-temperature) (Math/pow 2) (* -1) (+ 90))]
    (if (> roll bar)
      plant
      (update-in plant [:age] inc))))

(defn harvest []
  (swap!
   state
   (fn [current]
     (let* [current-plants (-> current :plants)
            new-plants (filter #(-> % :age (< plant-age)) current-plants)
            harvested (- (count current-plants) (count new-plants))
            new-food (-> current :food (+ harvested))]
       (into current
             {:food new-food
              :plants new-plants})))))


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
  (when (-> @state :game-time (mod length-of-day) zero?)
    ; New day
    (do
      (consume-food)
      (swap! state #(set-in % [:food-price] (food-price 0)))
      (swap! state #(set-in % [:temperature]
                            (-> @state :game-time temperature)))))
  (grow-plants)
  (when (-> @state :food zero?)
    (lose)))

(defonce timer
  (js/setInterval step game-speed))

(defn lose []
  (js/clearInterval timer)
  (js/alert "You starve."))

(defn draw-plant [plant]
  (let [age (-> plant :age)]
    (cond
      (< age sapling-age) "."
      (< age plant-age) "i"
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
     [:tr
      [:td (str "Seeds: " (-> @state :seeds))]
      [:td (format "Price/10: %ip" (-> @state :seed-price))]
      [:td [:input {:type "button"
                    :value "Buy 10"
                    :on-click #(buy-seeds 10)}]]]
     [:tr
      [:td (str "Food: " (-> @state :food))]
      [:td (format "Price/10: %ip" (-> @state :food-price))]
      [:td [:input {:type "button"
                    :value "Sell 10"
                    :on-click #(sell-food 10)}]]]
     [:tr (format "Temperature: %.1f°C" (-> @state :temperature))]]]

   ;; Actions
   [:div
    [:input {:type "button"
             :value "Plant 12 seeds"
             :on-click plant-seeds}]
    [:input {:type "button"
             :value "Harvest"
             :on-click harvest}]]

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
