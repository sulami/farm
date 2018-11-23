(ns farm.utils
  (:require [farm.config :as config]))

(defn debug-print
  "Print a value and return it."
  [x]
  (prn x)
  x)

(defn in?
  "True if coll contains element.
  Contrary to `contains?`, this works for lists."
  [coll element]
  (some #(= element %) coll))

(defn update-when
  "Update value with f if pred, otherwise return value."
  [value pred f]
  (if pred (f value) value))

(defn insert-at
  "Inserts elm into coll at idx, overwriting whatever was there before."
  [elm idx coll]
  (let [head (vec (take idx coll))
        tail (-> idx (+ 1) (drop coll) vec)]
    (reduce into [head [elm] tail])))

(defn avg
  "Average the values of coll. Zero if empty."
  [coll]
  (let [filtered (filter some? coll)]
    (if (empty? filtered)
      0
      (/ (reduce + filtered) (count filtered)))))

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

(defn check-lose-handler
  "Checks whether a loss condition has been hit and triggers loss."
  [{:keys [db]} _]
  (let [freezing (and (-> db
                          :temperature
                          (< config/livable-temperature))
                      (-> db
                          :wood
                          (<= 0)))]
    (cond
      freezing {:dispatch [:lose :freezing]}
      (-> db :food zero?) {:dispatch [:lose :starving]}
      (-> db :money (< 0)) {:dispatch [:lose :debt]}
      :else {})))

(defn lose
  "Incognicto event handler that sends out a message describing the loss."
  [db reason]
  {:db (assoc-in db [:alive] false)
   :dispatch [:send-message (reason config/loss-messages)]})
