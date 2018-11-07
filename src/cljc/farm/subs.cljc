(ns farm.subs
  (:require [re-frame.core :as re-frame :refer [reg-sub subscribe]]
            #?(:cljs [reagent.format :refer [format]])
            [clojure.string :as str]
            [farm.config :as config]
            [farm.climate :refer [time->season]]))

(reg-sub
 :state
 (fn state-sub [db _]
   db))

;; MESSAGES

(reg-sub
 :messages
 (fn messages-sub [db _]
   (->> db :messages (take 3))))

;; TIME

(reg-sub
 :game-time
 (fn game-time-sub [db _]
   (:game-time db)))

(reg-sub
 :year
 :<- [:game-time]
 (fn year-sub [game-time _]
   (-> game-time
       (quot config/length-of-year)
       (+ 1))))

(reg-sub
 :day
 :<- [:game-time]
 (fn day-sub [game-time _]
   (-> game-time
       (mod config/length-of-year)
       (+ 1))))

(reg-sub
 :day-of-the-week
 :<- [:game-time]
 (fn day-of-the-week-sub [game-time _]
   (nth config/days-of-the-week
        (mod game-time (count config/days-of-the-week)))))

(reg-sub
 :season
 :<- [:game-time]
 (fn season-sub [game-time _]
   (time->season game-time)))

(reg-sub
 :formatted-date
 :<- [:day-of-the-week]
 :<- [:day]
 :<- [:year]
 :<- [:season]
 (fn formatted-date-sub [[dow day year season] _]
   (format "%s, Day %i of Year %i (%s)"
           dow day year season)))

;; WEATHER

(reg-sub
 :weather
 (fn weather-sub [db _]
   (-> db
       :weather
       :name
       str/capitalize)))

;; ACTIVITY

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
       (not= :idle))))

(reg-sub
 :current-activity-time
 :<- [:current-activity]
 (fn current-activity-time-sub [activity _]
   (-> activity
       second
       (/ 1000))))
