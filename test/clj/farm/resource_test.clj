(ns farm.resource-test
  (:require [clojure.test :refer :all]
            [farm.resource :refer :all]
            [farm.events :refer [initialize-db]]
            [farm.config :as config]))

(deftest chop-wood-handler-test
  (let [db (initialize-db {} [:intitialize-db])]

    (testing "it increases wood by the correct amount"
      (is (= (:wood db)
             (-> db
                 (chop-wood-handler [:chop-wood])
                 :wood
                 (- config/wood-per-chop)))))))

(deftest hunt-handler-test
  (let [db (initialize-db {} [:initialize-db])]

    (testing "it increases food by the correct amount"
      (is (= (:food db)
             (-> db
                 (hunt-handler [:hunt])
                 :food
                 (- config/food-per-hunt)))))))
