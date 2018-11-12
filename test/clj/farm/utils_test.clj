(ns farm.utils-test
  (:require  [clojure.test :refer :all]
             [farm.utils :refer :all]
             [farm.db :refer [default-db]]))

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

(deftest avg-test
  (testing "min < avg < max"
    (let [coll (repeatedly 10 #(rand-int 10))]
      (is (< (apply min coll) (avg coll) (apply max coll)))))

  (testing "empty input"
    (is (= 0 (avg [])))))

(deftest within-bounds-test
  (testing "respects upper bound"
    (let [upper-bound 10]
      (is (= upper-bound ((within-bounds #(+ % 5) 0 upper-bound) 8)))))

  (testing "respects lower bound"
    (let [lower-bound 10]
      (is (= lower-bound ((within-bounds #(- % 5) lower-bound 10) 3)))))

  (testing "doesn't change result within bounds"
    (is (= (+ 3 5)
           ((within-bounds #(+ % 5) 0 10) 3)))))

(deftest fuzz-test
  (testing "can return all values in range"
    (let [fuzz-base 10
          fuzz-range 2
          expected (range (- fuzz-base fuzz-range) (+ fuzz-base fuzz-range 1))]
      (is (= expected
             (->> (repeatedly #(fuzz fuzz-base fuzz-range))
                  distinct
                  (take 5)
                  sort))))))

(deftest check-lose-handler-test
  (testing "it doesn't do anything on the default game state"
    (is (-> {:db default-db}
            (check-lose-handler [:check-lose])
            :dispatch
            nil?)))

  (testing "it triggers loss if running out of food"
    (is (= [:lose :starving]
           (-> {:db (set-in default-db [:food] 0)}
               (check-lose-handler [:check-lose])
               :dispatch))))

  (testing "it triggers loss if in negative standing"
    (is (= [:lose :debt]
           (-> {:db (set-in default-db [:money] -1)}
               (check-lose-handler [:check-lose])
               :dispatch))))

  (testing "it doesn't trigger loss if at zero money"
    (is (-> {:db (set-in default-db [:money] 0)}
            (check-lose-handler [:check-lose])
            :dispatch
            nil?))))
