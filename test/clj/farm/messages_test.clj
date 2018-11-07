(ns farm.messages-test
  (:require [clojure.test :refer :all]
            [farm.events :refer [initialize-db]]
            [farm.messages :refer :all]))

(deftest send-message-handler-test
  (let* [db (initialize-db {} [:initialize-db])
         db' (send-message db "toast")]

    (testing "it preserves messages as a list"
      (is (-> db' :messages list?)))

    (testing "it adds a new message in the beginning"
      (is (= "toast"
             (-> db' :messages first))))

    (testing "it preserves the old messages"
      (is (= (:messages db)
             (->> db' :messages (drop 1)))))))
