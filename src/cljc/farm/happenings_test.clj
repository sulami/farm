(ns farm.happenings-test
  (:require [clojure.test :refer :all]
            [farm.happenings :refer :all]
            [farm.config :as config]
            [farm.events :refer [initialize-db]]))

(deftest fire-happenings-handler-test
  (testing "it doesn't fire an event at time 0"
    (let [db (initialize-db {} [:initialize-db])]
      (is (= []
             (-> (fire-happenings-handler {:db db} [:fire-happenings])
                 :dispatch-n)))))

  (testing "it fires an event at the time of the first happening"
    (let [happening (first config/happenings)
          db (-> (initialize-db {} [:initialize-db])
                 (update-in [:game-time] #(+ % (:game-time happening))))]
      (is (= [(:event happening)]
             (-> (fire-happenings-handler {:db db} [:fire-happenings])
                 :dispatch-n))))))
