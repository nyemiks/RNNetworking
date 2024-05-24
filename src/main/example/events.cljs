(ns example.events
  (:require
   [re-frame.core :as rf]
   [example.db :as db :refer [app-db]]))


(rf/reg-event-db
 :initialize-db
 (fn [_ _]
   app-db))

(rf/reg-event-db
 :inc-counter
 (fn [db [_ _]]
   (update db :counter inc)))

(rf/reg-event-db
 :navigation/set-root-state
 (fn [db [_ navigation-root-state]]
   (assoc-in db [:navigation :root-state] navigation-root-state)))


(rf/reg-event-db
 ::update-posts
 (fn [db [_ posts]]
  (.info js/console "posts updated.")        
          
            (assoc db :postLists posts)
   )
 )


(rf/reg-event-db
 ::update-error
 (fn [db [_ error]]
  (.info js/console " update error")        
          
            (assoc db :error error)
   )
 )