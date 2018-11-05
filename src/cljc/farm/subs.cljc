(ns farm.subs
  (:require [re-frame.core :as re-frame :refer [reg-sub]]
            [clojure.string :as str]))

;; TODO Break this up.
(reg-sub
 :state
 (fn state-sub [db]
   db))

(reg-sub
 :weather
 (fn weather-sub [db]
   (-> db
       :weather
       name
       str/capitalize)))
