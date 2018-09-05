(ns farm.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.format :refer [format]]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant]
            [clojure.string :as str]))

;; Constants

(defonce game-speed 6000) ; Real seconds per game day
(defonce length-of-year 360) ; Days
(defonce sapling-age 350) ; Steps
(defonce plant-age 700) ; Steps
(defonce optimal-temperature 19)

(defonce state
  (atom {:game-time 0
         :family [{:name "You"
                   :age 20}
                  {:name "Your wife"
                   :age 18}]
         ;; Resources
         :money 120
         :seed 250
         :seed-price 8
         :food 587
         :food-price 8
         ;; Farming
         :temperature 10
         :weather :clear
         :plants []}))

;; Game functions

(defn set-in
  "Like update in, but just sets."
  [m ks v]
  (update-in m ks (constantly v)))

(defn time->season [game-time]
  (-> game-time
      (quot 90)
      (mod 4)))

(defn weather
  "Update the weather, based on the current weather.
  It's more likely to stay unchanged than to change, and there are certain
  probabilities for each different weather. Random beyond that."
  [current]
  (rand-nth (concat (repeat 100 current)
                    (repeat 8 :sunny)
                    (repeat 8 :clear)
                    (repeat 8 :overcast)
                    (repeat 8 :rain)
                    (repeat 1 :hail)
                    (repeat 1 :thunderstorm))))

(defn food-price
  "Determine the food price, max(2d6), 7-8ish."
  [game-time]
  (max (+ (rand-int 6) (rand-int 6))
       (+ (rand-int 6) (rand-int 6))))

(defn temperature
  "Sine wave temperature between 23 and 3 degrees.
  TODO fluctuations"
  [game-time]
  (-> game-time
      (mod length-of-year) ; Day in the year
      (/ length-of-year) ; %age of the year
      (* 2 Math/PI)
      Math/sin
      (* 10) ; Modifier
      (+ 13))) ; Baseline

(defn resource-price-key
  "Convert a resource key to a resource price key, appending '-price'."
  [key]
  (-> key
      name
      (str "-price")
      keyword))

(defn trade-resource
  "Trade `number` amount of `resource` for the current price.
  `:resource` needs to be the key of the resource counter in global state, and
  its price needs to be `:{resource}-price`. Action must be either `:buy` or
  `:sell`."
  [action resource number]
  (swap!
   state
   (fn [current]
     (let* [price-key (resource-price-key resource)
            resource-cost (-> current price-key (* number) (quot 10))]
       (case action
         :buy (if (-> current :money (< resource-cost))
                current
                (-> current
                    (update-in [:money] #(- % resource-cost))
                    (update-in [resource] #(+ % number))))
         :sell (if (-> current resource (< number))
                 current
                 (-> current
                     (update-in [:money] #(+ % resource-cost))
                     (update-in [resource] #(- % number)))))))))

(defn plant-seeds []
  (swap!
   state
   (fn [current]
     (let [new-seed (-> current :seed (- 12))
           current-plants (-> current :plants)
           new-plants (concat current-plants [{:age 0 :water 30}])]
       (if (> 0 new-seed)
         current
         (into current
               {:seed new-seed
                :plants new-plants}))))))

(defn water-plant
  "Update water on a plant depending on the weather.
  Plant will die (return `nil`) if water runs out."
  [plant weather]
  (update-in plant
             [:water]
             (case weather
               :sunny #(max 0 (- % 2))
               :rain inc
               :hail inc
               :thunderstorm inc
               #(max 0 (- % 1)))))

(defn grow-plant
  "Grow a plant, depending on the current environment, and return it.
  The formula makes the chance of growth `-(temperature - 19)^2 + 90`% each
  step. TODO kill plants when it gets too hot/cold"
  [plant weather temperature]
  (let* [roll (rand-int 100)
         bar (-> temperature
                 (- optimal-temperature)
                 (Math/pow 2)
                 (* -1)
                 (+ 90))]
    (if (> roll bar)
      plant
      (update-in plant [:age] inc))))

(defn plant-alive?
  "Plants die if they drie out."
  [plant]
  (-> plant :water (> 0)))

(defn grow-plants []
  (swap!
   state
   (fn [current]
     (let* [weather (-> current :weather)
            temperature (-> current :temperature)]
       (update-in current [:plants]
                  (fn [plants]
                    (->> plants
                      (map #(grow-plant % weather temperature))
                      (map #(water-plant % weather))
                      (filter plant-alive?))))))))

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

(defn consume-food []
  (swap!
   state
   (fn [current]
     (let [consumption (-> current :family count)]
       (update-in current [:food] #(- % consumption))))))

(declare lose)

(defn step
  "A day in the game world. Updates global state over time."
  []
  (swap! state #(update-in % [:game-time] inc))
  (consume-food)
  (swap! state #(set-in % [:temperature]
                        (-> @state :game-time temperature)))
  (swap! state #(update-in % [:weather] weather))
  (grow-plants)
  (swap! state #(set-in % [:food-price] (food-price 0)))
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
  (let* [year (+ 1 (quot game-time length-of-year))
         day (+ 1 (mod game-time length-of-year))
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

(defn resource-block [resource]
  (let [resource-name (-> resource str (str/replace-first ":" "") str/capitalize)
        price-key (resource-price-key resource)]
    [:tr
     [:td (format "%s: %d" resource-name (-> @state resource))]
     [:td (format "Price/10: %ip" (-> @state price-key))]
     [:td [:input {:type "button"
                   :value "Buy 10"
                   :on-click #(trade-resource :buy resource 10)}]]
     [:td [:input {:type "button"
                   :value "Sell 10"
                   :on-click #(trade-resource :sell resource 10)}]]]))

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
     (resource-block :seed)
     (resource-block :food)
     [:tr (format "Temperature: %.1f°C" (-> @state :temperature))]
     [:tr (format "Weather: %s" (-> @state :weather name str/capitalize))]]]

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
