(ns katybot.campfire
  (:require [clojure.string :as str]
            [katybot.campfire-api :as api])
  (:use katybot.core
        katybot.utils))

(defn- user-from-campfire [user]
  (change-keys user :avatar_url :avatar))

(defn type-from-campfire [type]
  (case type
    "TextMessage"  :text
    "EnterMessage" :join
    "LeaveMessage" :leave
    type))

(defn- item-from-campfire [item]
  (-> item
    (change-keys
      :body       :text
      :user_id    :user-id
      :created_at :timestamp)
    (update-in [:timestamp] #(.parse (java.text.SimpleDateFormat. "yyyy/MM/dd HH:mm:ss Z") %))
    (update-in [:type] type-from-campfire)))

(defn- msg-callback [robot me-id msg]
  (let [item (item-from-campfire msg)]
    (when (not= me-id (:user-id item))
      (consider robot item))))

(defn +campfire-receptor [robot account room token]
  (assoc robot
    :receptor ::campfire-receptor
    ::api     (api/campfire-async-api account token)
    ::room    room))

(defmethod listen ::campfire-receptor [{api ::api, room ::room  :as  robot}]
  (let [me (api/user-me api)]
    (api/listen api room (partial msg-callback robot (:id me)))
    (api/say    api room "Hi everybody!"))
  :listening)

(defmethod say ::campfire-receptor [{api ::api, room ::room} msg]
  (api/say api room msg))

(defmethod say-img ::campfire-receptor [robot url]
  (say robot [url "#.png"]))

(defmethod user ::campfire-receptor [{api ::api} user-id]
  (user-from-campfire (api/user-info api user-id)))

(defmethod users ::campfire-receptor [{api ::api, room ::room}]
  (let [room (api/room-info api room)]
    (into {}
      (for [u (:users room)]
        [(:id u) (user-from-campfire u)]))))

(defmethod shutdown ::campfire-receptor [{api ::api, room ::room}]
  (api/stop-listening api room))