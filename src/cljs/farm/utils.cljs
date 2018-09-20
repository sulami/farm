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

(defn update-when
  "Update value with f if pred, otherwise return value."
  [value pred f]
  (if pred (f value) value))

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

(defn dedup
  "Returns a lazy sequence of the elements of coll with duplicates removed using a predicate"
  [coll pred]
  (let [step (fn step [xs seen]
               (lazy-seq
                ((fn [[f :as xs] seen]
                   (when-let [s (seq xs)]
                     (if (some pred seen)
                       (recur (rest s) seen)
                       (cons f (step (rest s) (conj seen f))))))
                 xs seen)))]
    (step coll #{})))

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
