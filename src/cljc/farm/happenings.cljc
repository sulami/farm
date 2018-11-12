(ns farm.happenings
  (:require [farm.config :as config]))

(defn fire-happenings-handler
  "Fires happenings at their game-time based on config.
  Always includes :dispatch-n, which most of the time will be empty."
  [{:keys [db]} _]
  (let [now (:game-time db)]
    {:dispatch-n
     (->> db
          :happenings
          (filter #(= (:game-time %) now))
          (map :event))}))

(defn collect-taxes-handler
  "Reduces money and sends out a message."
  [{:keys [db]} _]
  {:dispatch [:send-message "Taxes are being collected."]
   :db (update-in db [:money] #(- % config/taxes))})
