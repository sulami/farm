(ns farm.resource-test
  (:require [clojure.test :refer :all]
            [farm.resource :refer :all]
            [farm.events :refer [initialize-db]]
            [farm.config :as config]))

(deftest chop-wood-test
  (let [db (initialize-db {} [:intitialize-db])]

    (testing "it increases wood by the correct amount"
      (is (= (:wood db)
             (-> db
                 (chop-wood [:chop-wood])
                 :wood
                 (- config/wood-per-chop)))))))
