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

(deftest collect-taxes-test
  (let* [db (initialize-db {} [:initialize-db])
         rv (collect-taxes {:db db} [:collect-taxes])]

    (testing "it fires a message"
      (is (= :send-message
             (-> rv
                 :dispatch
                 first))))

    (testing "it reduces money"
      (is (= config/taxes
             (-> rv
                 :db
                 :money
                 (- (:money db))
                 (* -1)))))))
