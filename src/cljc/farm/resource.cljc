(ns farm.resource
  (:require [farm.config :as config]))

(defn chop-wood-handler
  "Increases wood by `config/wood-per-chop`."
  [db _]
  (update-in db [:wood] (partial + config/wood-per-chop)))

(defn hunt-handler
  "Increases food by `config/food-per-hunt`."
  [db _]
  (update-in db [:food] (partial + config/food-per-hunt)))
