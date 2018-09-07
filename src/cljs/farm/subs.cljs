(ns farm.subs
  (:require
   [re-frame.core :as re-frame :refer [reg-sub]]))

;; TODO Break this up.
(reg-sub
 :state
 (fn [db]
   db))
