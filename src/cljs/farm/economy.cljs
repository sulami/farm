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

(defn trade-resource
  "Trade `number` amount of `resource` for the current price.
  `:resource` needs to be the key of the resource counter in global state, and
  its price needs to be `:{resource}-price`. Action must be either `:buy` or
  `:sell`."
  [db [_ action resource number]]
  (let* [price-key (resource-price-key resource)
         resource-cost (-> db price-key (* number) (quot 10))]
    (case action
      :buy (if (-> db :money (< resource-cost))
             db
             (-> db
                 (update-in [:money] #(- % resource-cost))
                 (update-in [resource] #(+ % number))))
      :sell (if (-> db resource (< number))
              db
              (-> db
                  (update-in [:money] #(+ % resource-cost))
                  (update-in [resource] #(- % number)))))))
