(ns farm.db
  (:require [farm.config :as config]))

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
