(ns farm.happenings)

(defn fire-happenings-handler
  [{:keys [db]} _]
  (let [now (:game-time db)]
    {:dispatch-n
     (->> db
          :happenings
          (filter #(= (:game-time %) now))
          (map :event))}))
