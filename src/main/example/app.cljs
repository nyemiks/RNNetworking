(ns example.app
  (:require 
            
            [expo.root :as expo-root]   
            ["react" :as react]     
            [re-frame.core :as rf]
            ["react-native" :as rn :refer [Button SafeAreaView View Text TextInput StatusBar ActivityIndicator]]
            [reagent.core :as r]
         ;   ["@react-navigation/native" :as rnn]
         ;   ["@react-navigation/native-stack" :as rnn-stack] 
           ; [re-frisk.core :refer [enable-re-frisk!]]
              [re-frisk-remote.core :as re-frisk-remote]  ;
             [ajax.core :as ajax]    ;; so you can use this in the response-format below           
            [re-statecharts.core :as rs]
             [statecharts.core :as fsm :refer [assign]] 
             [statecharts.integrations.re-frame :as fsm.rf]
            [example.events :as events]
            [example.subs :as subs]
             [day8.re-frame.http-fx]
             [clojure.string :as str]
            
             [re-frame.core :as re-frame])
  )

;(defonce shadow-splash (js/require "../assets/shadow-cljs.png"))
;(defonce cljs-splash (js/require "../assets/cljs.png"))


(def styles
  ^js (->
         {
          :container  {
                        :flex 1
                        :backgroundColor "#f5f5f5" 
                        :paddingTop StatusBar.currentHeight
                      }
          :listContainer {
                           :flex 1
                           :paddingHorizontal 16
                         }
          :card {
                 :backgroundColor "white"
                 :padding 16
                 :borderRadius 8
                 :borderWidth 1
          }
          :titleText {
                    :fontSize 30  
          }
          :bodyText {
                     :fontSize 24
                     :color "#666666"
          }
          :loadingContainer {
                :flex 1
                :backgroundColor "#F5F5F5" 
                :paddingTop StatusBar.currentHeight
                :justifyContent "center"
                :alignItems "center"             
                            }
          :inputContainer {
                             :backgroundColor "white"
                             :padding 16
                             :borderRadius 8
                             :borderWidth 1
                             :margin 16
                          }
          :input {
                  :height 40
                  :borderColor "gray"
                  :borderWidth 1
                  :marginBottom 8
                  :padding 8
                  :borderRadius 8
          }
          :errorContainer {
                            :backgroundColor "#FFC0CB"
                            :padding 16
                            :borderRadius 8
                            :borderWidth 1
                            :margin  16
                            :alignItems "center"
                          }
          :errorText {
                        :color  "#D8000C"
                        :fontSize 16
                        :textAlign "center"
                     }
         }
       (clj->js)
          (rn/StyleSheet.create)
       )
  )


(defn fetch-posts [state data]
  
   (.info js/console " -- fetch-posts -- ")
  (println " context:  " state)
   (println " data:  " data)
  
  
  ;(.info js/console " -- invoke post api -- ")

  (
   let [
           {:keys [limit]} state
        ]
     (println "fetch limit: " limit)
   (fsm.rf/call-fx
   {:http-xhrio
    {
    ;  :uri "https://jsonplaceholder.typicode.com/posts?_limit=10"
      :uri (str/join "" ["https://jsonplaceholder.typicode.com/posts?_limit=" limit])
     :method :get
     :response-format (ajax/json-response-format {:keywords? true})  ;; IMPORTANT!: You must provide this.
     :on-failure [::rs/transition :posts ::fail-load]
     :on-success [::rs/transition :posts ::success-load]
     }
     }
   ))
  

  )


(defn add-posts [context evt]
  
  (.info js/console " -- add-posts -- ")

   (println " context:  " context)
   (println " event:  " evt)
  
    
  (
   let [
           ;{:keys [limit]} context
          _  ()
          data    (:data evt)
         
        ]
   ;  (println "fetch limit: " limit)


   ;(comment
    
    (fsm.rf/call-fx
   {:http-xhrio
    {
     :uri "https://jsonplaceholder.typicode.com/posts"     
     :method :post
     :params  data 
     :timeout 5000 
     :format (ajax/json-request-format)
     :response-format (ajax/json-response-format {:keywords? true})  ;; IMPORTANT!: You must provide this.
     :on-failure [::rs/transition :posts  ::post-error]
     :on-success [::rs/transition :posts  ::post-success]
     }
     }
   )
     ;)
   
   
   )
  
  )

(defn update-posts [data] 
  (let  
      [  
     ;  url (.createObjectURL js/URL data) 
         _  (.info js/console " -- update posts in app db -- ")
       ] 
      (rf/dispatch [::events/update-posts data])  ;;  
    
    )
  
  )


(defn update-limit [state event]
 ; (update state :counter inc)
    (println "-- update fetch limit to 20 records --")
    (assoc state :limit 20)
  )


(def posts-fsm
    {
     :id :posts,
     :initial ::ready,
     :entry   (fn [& _]
                           (.info js/console "fm initialized ! just before ready state ")
                            )
     :context {:limit 10}
     :states {
              ::ready {
                          :entry  (fn [& _]
                             (.info js/console "now in ready state ! ")
                                   ; (rf/dispatch [::rs/transition :posts ::FETCH_POST_CLICKED]) 
                             )
                       
                            :on {
                                   ::FETCH_POST_CLICKED {
                                            :target ::loading
                                            :actions (fn [& _]
                                                        (.info js/console "transition to loading state ! ")
                                                       )
                                            }
                                   ::SAVE_POST_CLICKED {
                                                       :target ::posting
                                                       :actions (fn [& _]
                                                        (.info js/console "transition to posting state ! ")
                                                       )
                                                     }
                                } 
                       }
                ::loading  {
                             :entry  fetch-posts
                               
                             :on {
                                  ::success-load  {
                                                    :target  ::success
                                                   }
                                  ::fail-load   {
                                                  :target  ::error
                                                 }
                                 }
                             }
                ::success {
                           :entry  (fn [state evt]
                             (.info js/console "now in success state ! ")
                                 ;  (println "state: " state)
                                ;   (println "evt: " evt)

                                   (let 
                                        [
                                           {:keys [data]} evt
                                        ]
                                         (update-posts data)  
                                     ; (.info js/console "data: " data)
                                     ; ; update in app db the error state clear any errors that were present
                                      (rf/dispatch [::events/update-error ""])
                                     )
                             )
                           :on {
                                ::load-more {
                                                 :target ::refreshing
                                                 :actions  (assign update-limit)
                                               }
                                ::SAVE_POST_CLICKED {
                                                       :target ::posting
                                                       :actions (fn [& _]
                                                        (.info js/console "transition to posting state ! ")
                                                       )
                                                     }
                           }
                }
                ::error {
                         :entry  (fn [state evt]
                                    (.info js/console "now in error state ! ")
                                   (.info js/console "state: " state)
                                   (.info js/console "evt: " evt)
                                   (println "error fetching data: " evt)
                                   ; update in app db the error state "failed to fetch posts"
                                    (rf/dispatch [::events/update-error "failed to fetch posts"])
                                  )
                        }
                ::refreshing {
                               :entry  (fn [state evt]
                                    (.info js/console "now in refreshing state ! ")
                                 ;  (.info js/console "state: " state)
                                  ; (.info js/console "evt: " evt)
                                 ; (println "error: " evt)
                                          (fetch-posts state evt)
                                  )
                              
                                :on {
                                  ::success-load  {
                                                    :target  ::success
                                                   }
                                  ::fail-load   {
                                                  :target  ::error
                                                 }
                                 }

                }
                ::posting {
                             :entry  (fn [state evt]
                                    (.info js/console "now in posting state ! ")
                                    (.info js/console "state: " state)
                                    (.info js/console "evt: " evt)
                                  ;  (println "error: " evt) 
                                     (add-posts state evt)
                                  )
                           
                              :on {
                                  ::post-success  {
                                                    :target  ::add-post-success
                                                   }
                                  ::post-error   {
                                                    :target  ::add-post-error
                                                 }
                                 }
                           
                              
                           }
                    ::add-post-success {
                                           :entry  (fn [state evt]
                             (.info js/console "now in add post success state ! ")
                                   (println "state: " state)
                                   (println "evt: " evt)

                                   (let 
                                        [
                                           {:keys [data]} evt
                                            postList @(re-frame/subscribe [::subs/posts])
                                            updatedList (conj postList data)
                                        ]
                                       ;  (update-posts data)  
                                      (.info js/console "data: " data)
                                        (rf/dispatch [::events/update-posts updatedList])
                                     
                                    
                                     ; restart machine
                                    ;  (rf/dispatch [::rs/restart (:id posts-fsm) ])
                                     ; 

                                      ; update error in app db clear any errors that were present
                                       (rf/dispatch [::events/update-error ""])

                                     )
                             )
                    }
                    ::add-post-error {
                                         :entry  (fn [state evt]
                                          (.info js/console "now in error state ! ")
                                           (.info js/console "state: " state)
                                          (.info js/console "evt: " evt)
                                          (println "error: " evt)
                                                   
                                                   (println "error fetching data: " evt)
                                   ; update in app db the error state "failed to add new post"
                                                (rf/dispatch [::events/update-error "failed to add new post"])
                                           )
                                    }
              }
     }
    
  )


(defn render-record [item]
       (let [
                                      cljsrecord (js->clj item)
                                      record (get cljsrecord "item")
                                     ]
                               ;  (println " record: " record)
                                  
                                (r/as-element
                                   [:> rn/View {:style (.-card styles)}
                                      [:> rn/Text {:style (.-titleText styles)} (get record "title")]
                                      [:> rn/Text {:style (.-bodyText styles)} (get record "body")]
                                    ]
                                 )
 
                                 )
  )


(defn render-record2 [item]
       (let [
                                      jsitem (.-item item)
                                      
                                     ]
                                ; (println " js record: " jsitem)
                                  
                                (r/as-element
                                   [:> rn/View {:style (.-card styles)}
                                      [:> rn/Text {:style (.-titleText styles)} (.-title jsitem)]
                                      [:> rn/Text {:style (.-bodyText styles)} (.-body jsitem)]
                                    ]
                                 )
 
                                 )
  )


(defn handleRefresh []
  (println "-- handle refresh --")
  ; 
  ; if this is used data key will have nil value e.g. data:   {:type :example.app/load-more, :data nil}
 (rf/dispatch [::rs/transition :posts ::load-more])  

  ; if this is used data key will have a value data:   {:type :example.app/load-more, :data {:limit 25}}
  ; use this when u need to pass additional data to be made available in other state actions
 ; (rf/dispatch [::rs/transition :posts ::load-more {:limit 25}])    
  )





(defn app []
     
    (let [
           _ (.info js/console "render root view")
          ; name (re-frame/subscribe [::subs/name])
           _     (rf/dispatch [::rs/start posts-fsm])
          
         state (rf/subscribe [::rs/state :posts])
       
         postLists (rf/subscribe [::subs/posts])
        
           _  (println "current state" @state)

         ;  _ (.info js/console "trigger fetch posts transition ... ")

        ;   _  (rf/dispatch [::rs/transition :posts ::BUTTON_CLICKED]) 
           
           ; _  (println "current state" @state)
        
           [postTitle setPostTitle] (react/useState "")
           [postBody setPostBody]   (react/useState "")
          ; [error setError]   (react/useState "")
           error (re-frame/subscribe [::subs/error])
         ]
      
        (case @state
          ::loading [:> SafeAreaView  {:style (.-loadingContainer styles)}
                     [:> ActivityIndicator {
                                            :size "large" :color "0000ff"
                                            }
                      ]
                     [:> Text "Loading..."]
                     ]          
         ;default 
         [:> rn/SafeAreaView {                                
                                :style (.-container styles)
                                }
          (case @state
           ::error [:> View {
                               :style (.-errorContainer styles)
                             }
                      [:> Text  {
                                 :style (.-errorText styles)
                               }
                         @error 
                      ]
                     ]
            ;default
           [:<>
           [:> View {
                     :style (.-inputContainer styles)
                     }
            [:> TextInput  {
                            :style (.-input styles)
                            :placeHolder "Post title"
                            :value postTitle
                            :onChangeText (fn [val]
                                                ;  (println "entered val: " val)
                                            (setPostTitle val)
                                            )
                            }
             ]
            [:> TextInput  {
                            :style (.-input styles)
                            :placeHolder "Post Body"
                            :value postBody
                            :onChangeText (fn [val]
                                                 ; (println "entered val: " val)
                                            (setPostBody val)
                                            )
                            }
             ]
            [:> Button {
                        :title (if (= @state ::posting) "Add...." "Add Post")
                        :onPress (fn []
                                   (println "save post...")
                                   (println "title: " postTitle " body: " postBody)
                                   (rf/dispatch [::rs/transition :posts ::SAVE_POST_CLICKED {:title postTitle :body postBody}])
                                   (setPostTitle "")
                                   (setPostBody "")
                                   )
                        :disabled (if (= @state ::posting) true false)
                        }
             ]
            ]
          
            [:> rn/View {
                         :style (.-listContainer styles)
                         }
        ;[:> rn/Text "Hello world !!!"]
             [:> rn/Text "Blog Post"]
             [:> rn/Button {
                            :title "Load Posts"
                            :color "midnightblue"
                            :onPress (fn []
                                       (.log js/console "load posts ...")
                                ;  (setModalVisible false)
                                       (.info js/console "trigger fetch posts transition ... ")

                                  ; (rf/dispatch [::rs/transition :posts ::FETCH_POST_CLICKED]) 
                                       
                                       (if (= @state ::ready) 
                                         (rf/dispatch [::rs/transition :posts ::FETCH_POST_CLICKED]) 
                                         (do  
                                           (rf/dispatch [::rs/restart (:id posts-fsm) ])
                                           (rf/dispatch [::rs/transition :posts ::FETCH_POST_CLICKED]) 
                                           )
                                         )

                                       )
                            }
              ]
             [:> rn/FlatList 
              {
               :data @postLists
               :renderItem (fn [item]
                             (render-record2 item)                         
                             )
               
               :ItemSeparatorComponent (fn []
                                         (r/as-element
                                          [:> rn/View {
                                                       :style {
                                                               :height 16
                                                               }
                                                       }
                                           
                                           ])
                                         )
               :ListEmptyComponent (fn []
                                     (r/as-element [:> rn/Text "No Posts Found"])
                                     )
               :ListHeaderComponent (fn []
                                      (r/as-element [:> rn/Text {:style (.-headerText styles)} "Post List"] )
                                      )
               :ListFooterComponent (fn []
                                      (r/as-element [:> rn/Text {:style (.-footerText styles)} "End of List"] )
                                      )
               :refreshing (if (= @state ::refreshing) true false)
               :onRefresh handleRefresh
               }
              ]
             ]
            ]
            )
           ]
          
          )
       )

  )


(defn root []
  ;; The save and restore of the navigation root state is for development time bliss
    ; [:> app]     
      [:f> app]   ; if use state hooks are used in app function
  )

(defn start
  {:dev/after-load true}
  []
   (expo-root/render-root (r/as-element [root]))
  ;(expo-root/render-root (r/as-element [:> app]) )
  )

(defn init []
  ; (enable-re-frisk!)
  (re-frisk-remote/enable)
  (rf/dispatch-sync [:initialize-db])
  (start))