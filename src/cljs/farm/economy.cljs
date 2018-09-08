(ns farm.economy)

(defn consume-food
  "Update the db to consume food."
  [db _]
  (let [consumption (-> db :family count)]
    (update-in db [:food] #(- % consumption))))

(defn food-price
  "Determine the food price, max(2d6), 7-8ish."
  []
  (max (+ (rand-int 6) (rand-int 6))
       (+ (rand-int 6) (rand-int 6))))

(defn resource-price-key
  "Convert a resource key to a resource price key, appending '-price'."
  [key]
  (-> key
      name
      (str "-price")
      keyword))
