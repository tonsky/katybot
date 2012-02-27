(ns katybot.campfire-api
  (:require [http.async.client :as httpc]
            [http.async.client.request :as httpr]
            [clojure.data.json :as json]
            [clojure.string :as str])
  (:use katybot.utils))

(defprotocol CampfireAPI
  (join      [_ room])
  (listen    [_ room msg-callback])
  (say       [_ room msg])
  (stop-listening [_ room])
  (leave     [_ room])
  (user-info [_ user])
  (room-info [_ room]))

(declare room-agent)
(declare stop-room-agent)
(declare get-json)
(declare post)

(defn campfire-async-api [account token]
  (let [client (httpc/create-client
          :proxy {:host "127.0.0.1" :port 8443 :protocol :https}
          :auth  {:user token :password "x" :preemptive true})
        baseurl (format "https://%s.campfirenow.com/" account)
        connections (atom {})]
    (reify
      CampfireAPI
      (join [_ room]
        (post (format "%sroom/%s/join.json" baseurl room) client nil)
        (fyi "Joined a room " room))
      (listen    [_ room msg-callback]
        (let [agnt (room-agent client room msg-callback)]
          (swap! connections assoc room agnt)
          agnt))
      (say       [_ room msg] :nop)
      (stop-listening [_ room]
        (send (@connections room) stop-room-agent)
        (swap! connections dissoc room))
      (leave     [_ room]
        (post (format "%sroom/%s/leave.json" baseurl room) client nil)
        (fyi "Leaved a room " room))
      (user-info [_ user] :nop)
      (room-info [_ room] :nop))))

(defn callback [msg-callback state body]
  (btw "callback" state)
  (try
    (condp apply [state]
      httpc/done?      (fyi "Request is done")
      httpc/failed?    (fyi "Request failed")
      httpc/cancelled? (fyi "Request was canceled")
      (do
        (btw "Received \"" body "\"")
        [body :continue]))
      (catch Exception e
        (omg! "Exception " e))))

(defn err-callback [msg-callback resp thrwbl]
  (omg! "ERROR\n" resp "\n" thrwbl))

(defn done-callback [msg-callback resp]
  (omg! "DONE\n" resp))

(defn init-agent [{:keys [client url msg-callback] :as state} agnt]
  (btw "init-agent")
  (binding [httpr/*default-callbacks* 
    (merge
      httpr/*default-callbacks* 
      {:completed (partial done-callback msg-callback)
       :error     (partial err-callback  msg-callback)})]
    (assoc state
      :self  agnt
      :phase :listening
      :resp  (httpc/request-stream client :get url (partial callback msg-callback)))))

(defn room-agent [client room msg-callback]
  (let [url  (format "https://streaming.campfirenow.com/room/%s/live.json" room)
        agnt (agent {:phase :init
                     :client client
                     :url url
                     :msg-callback msg-callback})]
    (send agnt init-agent agnt)))

(defn stop-room-agent [{:keys [phase resp] :as state}]
  (btw "stop-room-agent")
  (httpc/cancel resp)
  (assoc state
    :phase :finished))

(def ^{:private true} headers
  {"Content-Type" "application/json; charset=utf-8"
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
