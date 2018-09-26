(ns farm.db
  (:require #?(:cljs [cljs.spec.alpha :as s]
               :clj [clojure.spec.alpha :as s])
            [farm.config :as config]
            [farm.utils :refer [in?]]))


;; Specs

(s/def ::game-time
  (s/and int? #(>= % 0)))

(s/def ::age
  (s/and int? #(>= % 0)))

(s/def ::family-member
  (s/keys :req-un [::name
                   ::age]))

(s/def ::family
  (s/coll-of ::family-member))

(s/def ::money int?)
(s/def ::seed int?)
(s/def ::seed-price int?)
(s/def ::food int?)
(s/def ::food-price int?)
(s/def ::temperature float?)

(s/def ::weather
  #(in? [:sunny
         :clear
         :overcast
         :rain
         :hail
         :thunderstorm] %))

(s/def ::plant
  (s/nilable
   (s/keys :req-un [::age
                    ::water])))

(s/def ::plants
  (s/coll-of ::plant))

(s/def ::db
  (s/keys :req-un [::game-time
                   ::family
                   ::money
                   ::seed
                   ::seed-price
                   ::food
                   ::food-price
                   ::temperature
                   ::weather
                   ::plants]))

;; Default DB

(def default-db
  {:game-time 0
   :family [{:name "You"
             :age 20}
            {:name "Your wife"
             :age 18}]

   ;; Resources
   :money 120
   :seed 250
   :seed-price 8
   :food 587
   :food-price 8

   ;; Farming
   :temperature 10
   :weather :clear
   :plants (repeat config/field-size nil)})
