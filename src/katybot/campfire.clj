(ns katybot.campfire
  (:require [http.async.client :as httpc]
            [clojure.data.json :as json]
            [clojure.string :as str])
  (:use katybot.core
        katybot.utils))

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

(defn join [{account ::account room ::room client-gen ::client-gen}]
  (with-open [client (client-gen)]
    (post (format "https://%s.campfirenow.com/room/%s/join.json" account room) client nil)
    (fyi "Joined a room " room)))

(defn leave [{account ::account room ::room client-gen ::client-gen}]
  (with-open [client (client-gen)]
    (post (format "https://%s.campfirenow.com/room/%s/leave.json" account room) client nil)
    (fyi "Leaved a room " room)))

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
    (update-in [:timestamp] #(.parse (java.text.SimpleDateFormat. "yyyy/MM/dd HH:mm:ss Z") %))
    (update-in [:type] type-from-campfire)))

(defn- user-me [{account ::account client-gen ::client-gen}]
  (with-open [client (client-gen)]
    (let [url (format "https://%s.campfirenow.com/users/me.json" account)]
      (get-in (get-json url client) [:user]))))

(defn- process-chunk [chunk robot me-id]
  (try
    (loop [items (->> (str/split chunk #"(?<=})\r")
                      (filter (comp not str/blank?))
                      (map (comp item-from-campfire json/read-json))
                      (filter #(not= me-id (:user-id %))))]
      (when-let [item (first items)]
        (let [res (consider robot item)]
          ((if res fyi btw) "[ " res " ] " item)
          (case res
            (:shutdown :reconnect) res ; sorry, skipping the rest
            (recur (rest items))))))
    (catch Exception e
      :reconnect)))

(defn +campfire-receptor [robot account room token]
  (let [client-gen (fn [] (httpc/create-client :user-agent version :auth {:user token :password "x" :preemptive true}))]
    (assoc robot :receptor ::campfire-receptor ::client-gen client-gen ::account account ::room room)))

(defmethod listen ::campfire-receptor [{client-gen ::client-gen account ::account room ::room :as robot}]
  (with-open [client (client-gen)]
    (let [me       (user-me robot)
          me-id    (:id me)
          me-name  (:name me)
          endpoint (format "https://streaming.campfirenow.com/room/%s/live.json" room)]
      (join robot)
      (say robot "Hi everybody!")
      (loop [stream    (httpc/stream-seq client :get endpoint)
             chunk-seq (httpc/string stream)
             just-connected true]
        (if just-connected
          (fyi "Connected to room " room " as " me-name " (" me-id ")"))
        (if-let [chunk (first chunk-seq)]
          (do
            (case (process-chunk chunk robot me-id)
              :shutdown  (httpc/cancel stream) ; escape from the loop
              :reconnect (recur stream [] false)
              (recur stream (rest chunk-seq) false)))
          (do
            (omg! "Disconnected")
            (httpc/cancel stream)
            (Thread/sleep 5000)
            (fyi "Reconnecting...")
            (join robot) ; in case we were kicked
            (let [new-stream    (httpc/stream-seq client :get endpoint)
                  new-chunk-seq (httpc/string new-stream)]
              (recur new-stream new-chunk-seq true)))))
      (leave robot))))

(defmethod say ::campfire-receptor [{client-gen ::client-gen account ::account room ::room} msg]
  (with-open [client (client-gen)]
    (let [url (format "https://%s.campfirenow.com/room/%s/speak.json" account room)
          body (json/json-str {:message {:body (apply str msg)}})]
      (post url client body))))

(defmethod say-img ::campfire-receptor [robot url]
  (say robot [url "#.png"]))

(defmethod user ::campfire-receptor [{client-gen ::client-gen account ::account} user-id]
  (with-open [client (client-gen)]
    (let [url  (format "https://%s.campfirenow.com/users/%s.json" account user-id)
          user (:user (get-json url client))]
      (user-from-campfire user))))

(defmethod users ::campfire-receptor [{client-gen ::client-gen account ::account room ::room}]
  (with-open [client (client-gen)]
    (let [url        (format "https://%s.campfirenow.com/room/%s.json" account room)
          room-info  (get-json url client)
          users-list (get-in room-info [:room :users])]
      (into {}
        (for [uc   users-list
              :let [u (user-from-campfire uc)]]
             [(:id u) u])))))

