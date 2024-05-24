(ns example.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :get-counter
 (fn [db _]
   (:counter db)))

(rf/reg-sub
 :counter-tappable?
 (fn [db _]
   (:counter-tappable? db)))

(rf/reg-sub
 :navigation/root-state
 (fn [db _]
   (get-in db [:navigation :root-state])))


(rf/reg-sub
 ::posts
 (fn [db _]
   (:postLists db)))



(rf/reg-sub
 ::error
 (fn [db _]
   (:error db)))