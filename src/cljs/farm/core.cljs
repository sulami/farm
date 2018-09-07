(ns farm.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.format :refer [format]]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant]
            [clojure.string :as str]))

;; Constants

(defonce game-speed 3000) ; Real seconds per game day
(defonce length-of-year 360) ; Days
(defonce sapling-age 73) ; Steps
(defonce plant-age 146) ; Steps
(defonce max-plant-water 30)
(defonce optimal-temperature 19)

(defonce new-plant {:age 0
                    :water max-plant-water})

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

(defn avg [coll]
  (if (empty? coll)
    0
    (/ (reduce + coll) (count coll))))

(defn within-bounds
  "Modifies a function to add a lower and upper bound to the result."
  [f lower upper]
  (fn [& args]
    (-> f
        (apply args)
        (max lower)
        (min upper))))

(defn fuzz
  "Fuzz a number within `amount` in either direction."
  [n amount]
  (-> amount
      (* 2)
      (+ 1)
      rand-int
      (- amount)
      (+ n)))

(defn fuzz-function
  "Modify a function to fuzz the result."
  [f amount]
  (fn [& args]
    (-> f
        (apply args)
        (fuzz amount))))

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
  "Sine wave temperature between 23 and 3 degrees."
  [game-time]
  (-> game-time
      (mod length-of-year) ; Day in the year
      (/ length-of-year) ; %age of the year
      (* 2 Math/PI)
      Math/sin
      (* 10) ; Modifier
      (+ 13)
      (fuzz 3))) ; Baseline

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
           new-plants (concat current-plants [new-plant])]
       (if (> 0 new-seed)
         current
         (into current
               {:seed new-seed
                :plants new-plants}))))))

(defn update-plant-water
  "Update water on a plant depending on the weather."
  [plant weather]
  (update-in plant
             [:water]
             (-> (case weather
                   :manual #(+ % 10)
                   :sunny #(- % 2)
                   :rain inc
                   :hail inc
                   :thunderstorm inc
                   #(- % 1))
                 (within-bounds 0 max-plant-water))))

(defn grow-plant
  "Grow a plant, depending on the current environment, and return it.
  The formula makes the chance of growth `-(temperature - 19)^2 + 90`% each
  step."
  [plant weather temperature]
  (let* [roll (rand-int 100)
         bar (-> temperature
                 (- optimal-temperature)
                 (Math/pow 2)
                 (* -1)
                 (+ 90)
                 (+ (-> plant :water (quot 3))))]
    (if (> roll bar)
      plant
      (update-in plant [:age] inc))))

(defn plant-alive?
  "Plants die if they drie out, or freeze.
  Temperature-based death occurs the further temperature drops below 8 degrees."
  [plant weather temperature]
  (let [freezing-temperature (if (-> plant :age (> plant-age))
                               8 0)]
    (and (-> plant :water (> 0))
         (-> freezing-temperature rand-int (- temperature) (< 0)))))

(defn update-plants []
  (swap!
   state
   (fn [current]
     (let* [weather (-> current :weather)
            temperature (-> current :temperature)]
       (update-in current [:plants]
                  (fn [plants]
                    (->> plants
                      (map #(grow-plant % weather temperature))
                      (map #(update-plant-water % weather))
                      (filter #(plant-alive? % weather temperature)))))))))

(defn water-plants
  "Manually water plants."
  []
  (swap!
   state
   (fn [current]
     (update-in current [:plants]
                (partial map #(update-plant-water % :manual))))))

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
  (update-plants)
  (swap! state #(set-in % [:food-price] (food-price 0)))
  (when (-> @state :food (<= 0))
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
      :else [:a {:on-click lose} "Y"])))

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
     [:tr (format "Weather: %s" (-> @state :weather name str/capitalize))]
     [:tr (format "Plants %i" (-> @state :plants count))]
     [:tr (format "Field humidity: %i" (->> @state :plants (map :water) avg))]]]

   ;; Actions
   [:div
    [:input {:type "button"
             :value "Plant 12 seeds"
             :on-click plant-seeds}]
    [:input {:type "button"
             :value "Harvest"
             :on-click harvest}]
    [:input {:type "button"
             :value "Water plants"
             :on-click water-plants}]]

   ;; Field
   [:div
    [:p {:style {:font-family "monospace"}}
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
