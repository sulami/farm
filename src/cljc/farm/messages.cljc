(ns farm.messages)

(defn send-message
  [db message]
  (update-in db [:messages] #(conj % message)))
