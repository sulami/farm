(ns farm.views
  (:require
   [clojure.string :as str]
   #?(:cljs [reagent.format :refer [format]])
   [re-frame.core :as re-frame :refer [dispatch subscribe]]
   [farm.climate :refer [time->season]]
   [farm.config :as config]
   [farm.economy :refer [resource-price-key]]
   [farm.subs :as subs]
   [farm.utils :refer [avg]]))

#?(:cljs (defonce timer
  (js/setInterval #(dispatch [:step]) config/game-speed))
   :clj (defonce timer nil))

(defn cljs->rgb
  "Build an RGB(A) color attribute from numbers."
  ([r g b] (format "rgb(%i,%i,%i)" r g b))
  ([r g b a] (format "rgba(%i,%i,%i,%i)" r g b a)))

(defn plant-color
  "Return a plant color RGB vector depending on the water status."
  [water]
  (let [f (fn [start end]
            (-> start
                (- end)
                (* water)
                (quot config/max-plant-water)
                (+ end)))]
    (map f config/plant-green config/plant-brown)))

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

(defn draw-plant [plant position]
  (let* [age (-> plant :age)
         color (->> plant :water plant-color (apply cljs->rgb))
         attrs (cond
                 (nil? plant) {:char "_" :color "brown" :action [:plant-seeds position]}
                 (< age config/sapling-age) {:char "." :color color :action []}
                 (< age config/plant-age) {:char "i" :color color :action []}
                 :else {:char "Y"
                        :color "green"
                        :action [:harvest position]})]
    [:a
     [:div
      {:style {:color (-> attrs :color)}
       :href "#"
       :on-click #(-> attrs :action dispatch)}
      (-> attrs :char)]]))

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
    [:div [:h2 "Farm"]

     ;; XXX Somehow need to get some CSS in there.
     [:div
      {:dangerouslySetInnerHTML
       {:__html "<style>@keyframes bar {from {width: 0%} to {width: 100%}}</style>"}}]

     [:div
      (map #(into [:p {:style {:margin "0" :color %2}} %1])
           (->> @state :messages (take 3))
           ["black" "darkgrey" "lightgrey"])]

     ;; XXX Clean all of this up.
     [:div
      (format "Activity: %s" @(subscribe [:current-activity-name]))
      (let [active (subscribe [:active])]
        (when @active
          [:div {:style {:background-color "lightgrey"
                         :height "10px"
                         :width "300px"}}
           [:div {:style {:animation (if @active
                                       (format "bar %fs infinite"
                                               @(subscribe [:current-activity-time]))
                                       "none")
                          :background-color "blue"
                          :height "8px"
                          :width "0%"}}]]))]

     ;; State
     [:div
      [:p (-> @state :game-time format-date)]
      [:table
       [:tbody
        [:tr (str "Family: " (->> @state :family
                                  (map format-person)
                                  (str/join ", ")))]
        [:tr (format "Money: %ip" (-> @state :money))]
        (resource-block :seed)
        (resource-block :food)
        [:tr (format "Temperature: %.1f°C" (-> @state :temperature))]
        [:tr (format "Weather: %s" @(subscribe [:weather]))]
        ;; XXX for debugging purposes only
        [:tr (format "Field humidity: %i" (->> @state :plants (map :water) avg))]]]]

     ;; Actions
     [:div
      [:input {:type "button"
               :value "Water plants"
               :on-click #(dispatch [:delayed-action 1500 [:water-plants]])}]]

     ;; Field
     [:div
      {:style {:display "flex"
               :flex-wrap "wrap"
               :font-family "monospace"
               :font-size "20px"}}
      (interleave
       (map draw-plant (-> @state :plants) (range))
       (repeat " "))]]))
