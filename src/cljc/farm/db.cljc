(ns farm.db
  (:require [clojure.spec.alpha :as s]
            [farm.config :as config]))

;; Specs
;; TODO These need proper naming and namespacing.

(s/def ::positive-int
  (s/and int? #(>= % 0)))

(s/def ::game-time
  ::positive-int)

(s/def ::alive boolean?)

(s/def ::event
  (s/and vector?
         #(-> % first keyword?)))

(s/def ::happening
  (s/keys :req-un [::game-time
                   ::event]))

(s/def ::happenings
  (s/coll-of ::happening
             ::into []))

(s/def ::messages
  (s/coll-of string?
             ::into '()))

(s/def ::activity
  (s/tuple ::event ::positive-int))

(s/def ::name string?)

(s/def ::age
  ::positive-int)

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
(s/def ::wood int?)
(s/def ::wood-price int?)
(s/def ::temperature float?)

(s/def ::probability int?)
(s/def ::temperature-mod int?)
(s/def ::water-mod int?)
(s/def ::survival-mod int?)

(s/def ::weather
  (s/keys :req-un [::name
                   ::probability
                   ::temperature-mod
                   ::water-mod
                   ::survival-mod]))

(s/def ::type keyword?)
(s/def ::water int?)

(s/def ::plant
  (s/nilable
   (s/keys :req-un [::type
                    ::age
                    ::water])))

(s/def ::plants
  (s/coll-of ::plant
             :into []
             :count config/field-size))

(s/def ::db
  (s/keys :req-un [::game-time
                   ::happenings
                   ::messages
                   ::activity
                   ::family
                   ::money
                   ::seed
                   ::seed-price
                   ::food
                   ::food-price
                   ::wood
                   ::wood-price
                   ::temperature
                   ::weather
                   ::plants]))

;; Default DB

(def default-db
  {:game-time 0
   :alive true
   :happenings config/happenings
   :messages '("Today is a wonderful day."
               "Let's plant some seeds."
               "Don't starve.")
   :activity [[:idle] 0]
   :family [{:name "You"
             :age 18}]

   ;; Resources
   :money 120
   :seed 250
   :seed-price 8
   :food 587
   :food-price 8
   :wood 80
   :wood-price 14

   ;; Farming
   :temperature 10
   :weather (first config/weathers)
   :plants (repeat config/field-size nil)})
