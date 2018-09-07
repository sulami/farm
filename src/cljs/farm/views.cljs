(ns farm.views
  (:require
   [clojure.string :as str]
   [reagent.format :refer [format]]
   [re-frame.core :as re-frame :refer [dispatch subscribe]]
   [farm.config :as config]
   [farm.subs :as subs]
   [farm.utils :refer [set-in avg within-bounds fuzz]]))

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
      (mod config/length-of-year) ; Day in the year
      (/ config/length-of-year) ; %age of the year
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

(defn update-plant-water
  "Update water on a plant depending on the weather."
  [plant weather]
  (if (nil? plant)
    nil
    (update-in plant
               [:water]
               (-> (case weather
                     :manual #(+ % 10)
                     :sunny #(- % 2)
                     :rain inc
                     :hail inc
                     :thunderstorm inc
                     #(- % 1))
                   (within-bounds 0 config/max-plant-water)))))

(defn grow-plant
  "Grow a plant, depending on the current environment, and return it.
  The formula makes the chance of growth `-(temperature - 19)^2 + 90`% each
  step."
  [plant weather temperature]
  (if (nil? plant)
    nil
    (let* [roll (rand-int 100)
           bar (-> temperature
                   (- config/optimal-temperature)
                   (Math/pow 2)
                   (* -1)
                   (+ 90)
                   (+ (-> plant :water (quot 3))))]
      (if (> roll bar)
        plant
        (update-in plant [:age] inc)))))

(defn plant-alive?
  "Plants die if they drie out, or freeze.
  Temperature-based death occurs the further temperature drops below 8 degrees."
  [plant weather temperature]
  (let [freezing-temperature (if (-> plant :age (> config/plant-age))
                               8 0)]
    (and (-> plant :water (> 0))
         (-> freezing-temperature rand-int (- temperature) neg?))))

#_(defonce timer
  (js/setInterval #(dispatch :step) game-speed))

#_(defn lose []
  (js/clearInterval timer)
  (js/alert "You starve."))

(defn format-person [person]
  (format "%s (%i)"
          (:name person)
          (:age person)))

(defn format-date [game-time]
  (let* [year (+ 1 (quot game-time config/length-of-year))
         day (+ 1 (mod game-time config/length-of-year))
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

(defn draw-plant [plant]
  (let* [age (-> plant :age)
         attrs (cond
                 (nil? plant) {:char "_" :color "brown"}
                 (< age config/sapling-age) {:char "." :color "brown"}
                 (< age config/plant-age) {:char "i" :color "green"}
                 :else {:char "Y" :color "green"})]
    [:span
     {:style {:color (-> attrs :color)}}
     (-> attrs :char)]))

(defn resource-block [resource]
  (let [state (subscribe [:state])
        resource-name (-> resource str (str/replace-first ":" "") str/capitalize)
        price-key (resource-price-key resource)]
    [:tr
     [:td (format "%s: %d" resource-name (-> @state resource))]
     [:td (format "Price/10: %ip" (-> @state price-key))]
     [:td [:input {:type "button"
                   :value "Buy 10"
                   :on-click #(dispatch [:trade-resource :buy resource 10])}]]
     [:td [:input {:type "button"
                   :value "Sell 10"
                   :on-click #(dispatch [:trade-resource :sell resource 10])}]]]))

(defn game-page []
  (let [state (subscribe [:state])]
    [:div #_[:h2 "Medieval Farm"]

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
               :disabled (or (-> @state :seed (< 12))
                             (->> @state :plants (filter nil?) empty?))
               :on-click #(dispatch [:plant-seeds])}]
      [:input {:type "button"
               :value "Harvest"
               :on-click #(dispatch [:harvest])}]
      [:input {:type "button"
               :value "Water plants"
               :on-click #(dispatch [:water-plants])}]]

     ;; Field
     [:div
      [:p {:style {:font-family "monospace"}}
       (interleave
        (map draw-plant (-> @state :plants))
        (repeat " "))]]]))
