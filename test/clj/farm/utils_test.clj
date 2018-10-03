(ns farm.utils-test
  (:require  [clojure.test :refer :all]
             [farm.utils :refer :all]))

(deftest insert-at-test
  (testing "for vectors it"

    (testing "doesn't change the length of vec"
      (let* [v (vec (repeat 5 nil))
             v' (insert-at :elm 2 v)]
        (is (= (count v)
               (count v')))))

    (testing "inserts at the right index"
      (let* [v (vec (repeat 5 nil))
             v' (insert-at :elm 2 v)]
        (is (= :elm
               (nth v' 2)))))

    (testing "inserts at the right index for 0"
      (let* [v (vec (repeat 5 nil))
             v' (insert-at :elm 0 v)]
        (is (= :elm
               (nth v' 0)))))

    (testing "inserts at the right index for the end"
      (let* [v (vec (repeat 5 nil))
             v' (insert-at :elm 4 v)]
        (is (= :elm
               (nth v' 4))))))

  (testing "for lists it"

    (testing "doesn't change the length of l"
      (let* [l (repeat 5 nil)
             l' (insert-at :elm 2 l)]
        (is (= (count l)
               (count l')))))

    (testing "inserts at the right index"
      (let* [l (repeat 5 nil)
             l' (insert-at :elm 2 l)]
        (is (= :elm
               (nth l' 2)))))

    (testing "inserts at the right index for 0"
      (let* [l (repeat 5 nil)
             l' (insert-at :elm 0 l)]
        (is (= :elm
               (nth l' 0)))))

    (testing "inserts at the right index for the end"
      (let* [l (repeat 5 nil)
             l' (insert-at :elm 4 l)]
        (is (= :elm
               (nth l' 4)))))))
