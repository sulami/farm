(ns farm.subs
  (:require [re-frame.core :as re-frame :refer [reg-sub subscribe]]
            [clojure.string :as str]))

;; TODO Break this up.
(reg-sub
 :state
 (fn state-sub [db _]
   db))

(reg-sub
 :game-time
 (fn game-time-sub [db _]
   (:game-time db)))

(reg-sub
 :day-of-the-week
 :<- [:game-time]
 (fn day-of-the-week-sub [game-time _]
   (nth ["Monday"
         "Tuesday"
         "Wednesday"
         "Thursday"
         "Friday"
         "Saturday"
         "Sunday"]
        (mod game-time 7))))

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
 :<- [:current-activity]
 (fn current-activity-name-sub [activity _]
   (-> activity
       first
       first
       name
       str/capitalize)))

(reg-sub
 :active
 :<- [:current-activity]
 (fn active-sub [activity _]
   (-> activity
       first
       first
       (not= :nothing))))

(reg-sub
 :current-activity-time
 :<- [:current-activity]
 (fn current-activity-time-sub [activity _]
   (-> activity
       second
       (/ 1000))))
