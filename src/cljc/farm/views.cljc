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
        resource-name (-> resource name str/capitalize)
        price-key (resource-price-key resource)
        price-info (if config/debug?
                     (format " (%sp/10)" (price-key @state))
                     "")]
    [:div {:class "flex-1"}
     (format "%s: %d%s" resource-name (resource @state) price-info)]))

(defn action-button
  [text action]
  (let [able-to-act @(subscribe [:able-to-act])]
    [:input {:type "button"
             :class (str "flex-none m-1 rounded px-2 py-1 text-white "
                         (if able-to-act
                           "bg-grey-darker hover:bg-grey-dark"
                           "bg-grey"))
             :value text
             :disabled (not able-to-act)
             :on-click action}]))

(defn game-page []
  (let [state (subscribe [:state])]
    [:div

     [:h2 {:class "text-center py-4"}
      "Farm"]

     (let [active (subscribe [:active])]
       [:div {:class "w-full text-center"}
        (format "Activity: %s" @(subscribe [:current-activity-name]))
        [:div {:class "bg-grey-lighter w-full rounded"
               :style {:height "8px"}}
         (when @active
           [:div {:class "bg-grey-darkest rounded h-full"
                  :style {:animation (if @active
                                       (format "bar %fs infinite"
                                               @(subscribe [:current-activity-time]))
                                       "none")}}])]])

     [:h4 "Messages"]
     (map #(into [:p {:class (format "m-0 text-%s" %2)} %1])
          @(subscribe [:messages])
          ["black" "grey-dark" "grey-light"])

     [:h4 "Status"]
     [:div @(subscribe [:formatted-date])]
     [:div (format "Temperature: %.1f°C (%s)"
                   (-> @state :temperature)
                   @(subscribe [:heating-status]))]
     [:div (format "Weather: %s" @(subscribe [:weather]))]
     (when config/debug?
       [:div (format "Field humidity: %i" (->> @state :plants (map :water) avg))])

     [:h4 "Inventory"]
     [:div (format "Money: %ip" (-> @state :money))]
     [:div {:class "flex"}
      (for [resource [:food :seed :wood]]
        [resource-block resource])]

     [:h4 "Marketplace"]
     [:div {:class "flex"}
      (for [resource [:food :seed :wood]
            action [:buy :sell]]
        [action-button
         (format "%s %s" (-> action name str/capitalize) (name resource))
         #(dispatch [:delayed-action 1500 [:trade-resource action resource 10]])])]

     [:h4 "Actions"]
     [:div {:class "flex"}
      [action-button "Water plants" #(dispatch [:delayed-action 1000 [:water-plants]])]
      [action-button "Chop wood" #(dispatch [:delayed-action 2500 [:chop-wood]])]
      [action-button "Go hunting" #(dispatch [:delayed-action 3000 [:hunt]])]]

     [:h4 "Field"]
     [:div
      {:style {:display "flex"
               :flex-wrap "wrap"
               :font-family "monospace"
               :font-size "20px"}}
      (interleave
       (map draw-plant (-> @state :plants) (range))
       (repeat " "))]]))
