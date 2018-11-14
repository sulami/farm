(ns farm.resource
  (:require [farm.config :as config]))

(defn chop-wood
  "Increase wood by config/wood-per-chop."
  [db _]
  (update-in db [:wood] (partial + config/wood-per-chop)))
