(ns katybot.campfire
  (:require [http.async.client :as httpc])
  (:require [clojure.data.json :as json])
  (:require [clojure.string :as str])
  (:use [katybot.core]))

(def headers {"Content-Type" "application/json; charset=utf-8"
              "Accept" "application/json"})

(defn- get-json [url client & query]
  (btw "HTTP GET: " url)
  (let [resp   (httpc/await (httpc/GET client url :headers headers :query (apply hash-map query)))
        status (:code (httpc/status resp))
        res    (httpc/string resp)]
    (when-not (= 200 status)
      (omg! "BAD RESPONSE: " res)
      (throw (Exception. (str url ": " status res "\n" (httpc/headers resp)))))
    (json/read-json res)))

(defn- post [url client body]
  (btw "HTTP POST:\n  " url "\n  " body)
  (let [resp (httpc/await (httpc/POST client url :body body :headers headers))
        status (:code (httpc/status resp))
        res (httpc/string resp)]
    (when-not (<= 200 status 299)
      (omg! "BAD RESPONSE: " res)
      (throw (Exception. (str url ": " status res "\n" (httpc/headers resp)))))
    res))

(defn join [{:keys [account room client]}]
  (post (format "https://%s.campfirenow.com/room/%s/join.json" account room) client nil)
  (fyi "Joined a room " room))

(defn leave [{:keys [account room client]}]
  (post (format "https://%s.campfirenow.com/room/%s/leave.json" account room) client nil)
  (fyi "Leaved a room " room))

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
    (change-keys :body :text  :user_id :user-id  :created_at :timestamp)
    ; TODO parse timestamp and convert to tick
    (update-in [:type] type-from-campfire)))

(defn- user-me-id [client account]
  (let [url (format "https://%s.campfirenow.com/users/me.json" account)]
    (get-in (get-json url client) [:user :id])))

(defrecord Campfire [client account room]
  Adapter

  (start [this on-event]
    (let [me-id     (user-me-id client account)
          endpoint  (format "https://streaming.campfirenow.com/room/%s/live.json" room)
          chunk-seq (httpc/stream-seq client :get endpoint)]
      (fyi "Logged in as #" me-id " " (:name (user this me-id)))
      (join this)
      (say this "Hi everybody!")
      (doseq [chunk    (httpc/string chunk-seq)
              item-str (str/split chunk #"(?<=})\r")
              :when    (not (str/blank? item-str))
              :let     [item (item-from-campfire (json/read-json item-str))]
              :when    (not= (:user-id item) me-id)
              :let     [res (on-event this item)]]
        ((if (#{:answered :shutdown} res) fyi btw) "[ " res " ] " item)
        (if (= res :shutdown)
          (httpc/cancel chunk-seq)))
      (leave this)))

  (say [_ msg]
    (let [url (format "https://%s.campfirenow.com/room/%s/speak.json" account room)
          body (json/json-str {:message {:body (apply str msg)}})]
      (post url client body)))

  (say-img [this url]
    (say this [url "#.png"]))

  (user [_ user-id]
    (let [url  (format "https://%s.campfirenow.com/users/%s.json" account user-id)
          user (:user (get-json url client))]
      (user-from-campfire user)))

  (users [_] 
    (let [url (format "https://%s.campfirenow.com/room/%s.json" account room)
          room-info (get-json url client)
          users-list (get-in room-info [:room :users])]
      (into {}
        (for [uc   users-list 
              :let [u (user-from-campfire uc)]]
             [(:id u) u]))))
)

(defn start-campfire [account room token on-event]
  (with-open [client (httpc/create-client :user-agent "Katybot-clj/0.1" :auth {:user token :password "x" :preemptive true} )]
    (start (Campfire. client account room) on-event)))
