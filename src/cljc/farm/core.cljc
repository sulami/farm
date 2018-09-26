(ns farm.core
  (:require [reagent.core :as reagent :refer [atom]]
            #?(:cljs [reagent.format :refer [format]])
            [re-frame.core :as re-frame]
            [clojure.string :as str]
            [farm.events :as events]
            [farm.views :as views]
            [farm.config :as config]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/game-page]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (dev-setup)
  (mount-root))
