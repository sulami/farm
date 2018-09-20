(ns farm.utils)

(defn debug-print
  "Print a value and return it."
  [x]
  (prn x)
  x)

(defn in?
  "True if coll contains element."
  [coll element]
  (some #(= element %) coll))

(defn set-in
  "Like update in, but just sets."
  [m ks v]
  (update-in m ks (constantly v)))

(defn avg
  "Average the values of coll. Zero if empty."
  [coll]
  (let [filtered (filter #(-> % nil? not) coll)]
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
