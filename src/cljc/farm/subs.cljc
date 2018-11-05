(ns farm.subs
  (:require [re-frame.core :as re-frame :refer [reg-sub subscribe]]
            [clojure.string :as str]))

;; TODO Break this up.
(reg-sub
 :state
 (fn state-sub [db]
   db))

(reg-sub
 :weather
 (fn weather-sub [db _]
   (-> db
       :weather
       name
       str/capitalize)))

(reg-sub
 :current-activity
 (fn current-activity-sub [db _]
   (:activity db)))

(reg-sub
 :current-activity-name
 (fn current-activity-name-sub [_ _]
   (subscribe [:current-activity]))
 (fn current-activity-name-sub [activity _]
   (-> activity
       first
       first
       name
       str/capitalize)))

(reg-sub
 :active
 (fn active-sub [_ _]
   (subscribe [:current-activity]))
 (fn active-sub [activity _]
   (-> activity
       first
       first
       (not= :nothing))))

(reg-sub
 :current-activity-time
 (fn current-activity-time-sub [_ _]
   (subscribe [:current-activity]))
 (fn current-activity-time-sub [activity _]
   (-> activity
       second
       (/ 1000))))
