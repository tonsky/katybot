(ns katybot.campfire
  (:require [http.async.client :as httpc])
  (:require [clojure.data.json :as json])
  (:require [clojure.string :as str])
  (:use [katybot.core]))

(def headers {"Content-Type" "application/json; charset=utf-8"
              "Accept" "application/json"})

(defn- log [& msg] (println (apply str msg)))
(defn- log-debug [& msg] );(log "\u001b[1;36m" (apply str msg) "\u001b[m"))
(defn- log-info  [& msg] (log "\u001b[1;32m" (apply str msg) "\u001b[m"))
(defn- log-err   [& msg] (log "\u001b[1;31m" (apply str msg) "\u001b[m"))

(defn- get-json [url client]
  (log-debug "HTTP GET: " url)
  (let [resp   (httpc/await (httpc/GET client url :headers headers))
        status (:code (httpc/status resp))
        res    (httpc/string resp)]
    (when-not (= 200 status)
      (log-err "BAD RESPONSE: " res)
      (throw (Exception. (str url ": " status res "\n" (httpc/headers resp)))))
    (json/read-json res)))

(defn- post [url client body]
  (log-debug "HTTP POST:\n  " url "\n  " body)
  (let [resp (httpc/await (httpc/POST client url :body body :headers headers))
        status (:code (httpc/status resp))
        res (httpc/string resp)]
    (when-not (<= 200 status 299)
      (log-err "BAD RESPONSE: " res)
      (throw (Exception. (str url ": " status res "\n" (httpc/headers resp)))))
    res))

(defn join [room client]
  (post (format "https://%s.campfirenow.com/room/%s/join.json" account room) client nil)
  (log-info "Joined a room " room))

(defn leave [room client]
  (post (format "https://%s.campfirenow.com/room/%s/leave.json" account room) client nil)
  (log-info "Leaved a room " room))

(defn- user-of-campfire [user-campfire]
  (change-keys user-campfire :avatar_url :avatar))

(def type-of-campfire {"TextMessage" :text "JoinMessage" :join})

(defn- item-of-campfire [item-campfire]
  (let [item1 (change-keys item-campfire :body :text :user_id :user-id :createed_at :timestamp)
        type (:type item1)]
    (assoc item1 :type (get type-of-campfire type type))))

(defn- user-me [client account]
  (let [url (format "https://%s.campfirenow.com/users/me.json" account)]
    (get-in (get-json url client) [:user :id])))

(defrecord Campfire [client account room]
  Adapter

  (start [this on-event]
    (let [me (user-me client account)
          endpoint  (format "https://streaming.campfirenow.com/room/%s/live.json" room)
          chunk-seq (httpc/stream-seq client :get endpoint)]
    (join room client)
    (say this "Hi everybody!")
      (doseq [chunk (httpc/string chunk-seq)
              item-str (str/split chunk #"(?<=})\r")
              :when (not (str/blank? item-str))
              :let [item (item-of-campfire (json/read-json item-str))]
              :when (not= (:user-id item) me)
              :let [action (on-event this item)]]
        ((if (#{:answered :shutdown} action) log-info log-debug) "[ " action " ] " item)
        (if (= action :shutdown)
          (httpc/cancel chunk-seq)))
      (leave room client)))

  (say [_ msg]
    (let [url (format "https://%s.campfirenow.com/room/%s/speak.json" account room)
          body (json/json-str {:message {:body (apply str msg)}})]
      (post url client body)))

  (say-img [this url]
    (say this [url "#.png"]))

  (user [_ user-id]
    (let [url (format "https://%s.campfirenow.com/users/%s.json" account user-id)
          user-campfire (:user (get-json url client))]
      (user-of-campfire user-campfire)))

  (users [_] 
    (let [url (format "https://%s.campfirenow.com/room/%s.json" account room)
          room-info (get-json url client)
          users-list (get-in room-info [:room :users])]
      (into {} (for [uc users-list 
                     :let [u (user-of-campfire uc)]]
                    [(:id u) u]))))
) 

(defn start-campfire [account room token on-event]
  (with-open [client (httpc/create-client :user-agent "Katybot-clj/0.1" :auth {:user token :password "x" :preemptive true} )]
    (start (Campfire. client account room) on-event)))
