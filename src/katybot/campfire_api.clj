(ns katybot.campfire-api
  (:require [http.async.client :as httpc]
            [http.async.client.request :as httpr]
            [clojure.data.json :as json]
            [clojure.string :as str])
  (:use katybot.utils))

(defprotocol CampfireAPI
  (join      [_ room])
  (say       [_ room msg])
  (listen    [_ room msg-callback])
  (stop-listening [_ room])
  (leave     [_ room])
  (user-info [_ user])
  (user-me   [_])
  (room-info [_ room]))

(def ^:dynamic *check-interval* 5000)
(def ^:dynamic *stuck-timeout* 10000)
(def ^:dynamic *headers* {"Content-Type" "application/json; charset=utf-8"
                          "Accept"       "application/json"})
(def ^:dynamic *user-agent* "katybot.campfire-api")
(def ^:dynamic *debug* false)

(declare room-agent)
(declare finish)
(declare get-json)
(declare post)

(defn campfire-async-api [account token]
  (let [create-client #(httpc/create-client
          ;:proxy {:host "127.0.0.1" :port 8443 :protocol :https}
          :auth {:user token :password "x" :preemptive true}
          :connection-timeout 3000
          :request-timeout   -1)
        baseurl (format "https://%s.campfirenow.com" account)
        connections (ref {})]
    (reify
      CampfireAPI

      (join [_ room]
        (with-open [client (create-client)]
          (post (format "%s/room/%s/join.json" baseurl room) client nil)
          (fyi "Joined a room " room)))

      (say [_ room msg]
        (with-open [client (create-client)]
          (let [url (format "%s/room/%s/speak.json" baseurl room)
                body (json/json-str {:message {:body (apply str msg)}})]
            (post url client body))))

      (listen [api room msg-callback]
        (dosync
          (stop-listening api room)
          (let [agnt (room-agent api (create-client) room msg-callback)]
            (alter connections assoc room agnt)
            agnt)))

      (stop-listening [_ room]
        (dosync
          (when-let [old-agnt (@connections room)]
            (send old-agnt finish)
            (alter connections dissoc room)
            old-agnt)))

      (leave [_ room]
        (with-open [client (create-client)]
          (post (format "%s/room/%s/leave.json" baseurl room) client nil)
          (fyi "Leaved a room " room)))

      (user-info [_ user]
        (with-open [client (create-client)]
          (let [url  (format "%s/users/%s.json" baseurl user)]
            (:user (get-json url client)))))

      (user-me [_]
        (with-open [client (create-client)]
          (let [url  (format "%s/users/me.json" baseurl)]
            (:user (get-json url client)))))

      (room-info [_ room]
        (with-open [client (create-client)]
          (let [url  (format "%s/room/%s.json" baseurl room)]
            (:room (get-json url client))))))))

(defn- debug [state module & msg]
  (when (:debug state)
    (btw "[" (:room state) " " module "] " (apply str msg))))

(defn- touch [state phase]
  (if (= :finished (:phase state))
    state
    (assoc state :phase phase, :last-accessed (now))))

(defn- part-callback [msg-callback agnt state baos]
  (debug @agnt "callback" "got part \"" baos "\"")
  (send agnt touch :listening)
  ; TODO client callback thread: if msg-callback will take long enough, watchman may restart client
  (let [body (.toString baos "UTF-8")]
    (if (not (str/blank? body))           ; filtering keep-alive " " messages
      (doseq [msg (str/split body #"\r")] ; splitting coerced message bodies
        (try
          (msg-callback (json/read-json msg))
          (catch Exception e
            (omg! "Callback call failed: " e))))))
  (send agnt touch :listening)
  [baos :continue])

(defn- err-callback [agnt resp thrwbl]
  (if (= (class thrwbl) java.util.concurrent.CancellationException) ; normal finish
    (debug @agnt "callback" thrwbl)
    (omg! "Campfire connection error: " thrwbl))
  (send agnt touch :broken)
  thrwbl)

(defn- done-callback [agnt resp]
  (omg! "Campfire connection closed by remote host")
  (send agnt touch :dropped)
  [true :continue])

(defn connect [state]
  (debug state "agent" "connecting")
  (let [{:keys [room api client msg-callback]} state
        url (format "https://streaming.campfirenow.com/room/%s/live.json" room)]
    (join api room) ; just in case we were kicked
    (binding [httpr/*default-callbacks* (merge httpr/*default-callbacks* 
              {:completed (partial done-callback *agent*)
               :error     (partial err-callback  *agent*)})]
      (-> state
        (touch :listening)
        (assoc :resp (httpc/request-stream client :get url (partial part-callback msg-callback *agent*)))))))

(defn disconnect [state]
  (debug state "agent" "disconnecting")
  (if (= (:phase state) :listening)
    (httpc/cancel (:resp state)))
  state)

(defn reconnect [state]
  (fyi "Reconnecting to room " (:room state) "...")
  (debug state "agent" "reconnecting from " (:phase state))
  (-> state
    disconnect
    connect))

(defn finish [state]
  (debug state "agent" "finish")
  (with-open [client (:client state)]
    (-> state
      disconnect
      (assoc :phase :finished))))

(defn- doctor [agnt e]
  "Fix agent if exception was thrown in agent thread"
  (omg! "Exception in room-agent " (:room @agnt) ": " e)
  (send agnt touch :broken))

(defn- watchman [agnt]
  "Check agent status every *check-interval* ms and restart if it stuck"
  (let [{:keys [phase resp last-accessed] :as state} @agnt
        delay (- (now) last-accessed)]
    (debug state "watchman" "agent is " phase ", " delay "ms since last activity")
    (cond
      (= phase :finished) nil ; stopping watchman
      (> delay *stuck-timeout*) (do
        (when (= phase :listening) (omg! "Fuck - we are stuck"))
        (send agnt reconnect)
        :continue)
      :else :continue)))

(defn- logger [room _ agnt old-state new-state]
  (when (not= (:phase old-state) (:phase new-state))
    (debug new-state "logger" (:phase old-state) " -> " (:phase new-state))))

(defn room-agent [api client room msg-callback]
  (let [agnt (agent {:api    api
                     :client client
                     :room   room
                     :msg-callback msg-callback
                     :debug *debug*}
                    :error-handler doctor
                    :clear-actions true)]
    (schedule (partial watchman agnt) *check-interval*)
    (doto agnt
      (add-watch  :logger-watcher (partial logger room))
      (send touch :init)
      (send connect))))

(defn- get-json [url client & query]
  (btw "HTTP GET: " url)
  (let [resp   (httpc/await (httpc/GET client url 
                              :headers *headers*
                              :user-agent *user-agent*
                              :timeout *stuck-timeout*
                              :query (apply hash-map query)))
        status (:code (httpc/status resp))
        res    (httpc/string resp)]
    (cond
      (httpc/failed? resp) (throw (httpc/error resp))
      (not= status 200)    (throw (Exception. (str url ": " status res "\n" (httpc/headers resp))))
      :else (json/read-json res))))

(defn- post [url client body]
  (btw "HTTP POST:\n  " url "\n  " body)
  (let [resp (httpc/await (httpc/POST client url
                            :body body
                            :headers *headers*
                            :timeout *stuck-timeout*
                            :user-agent *user-agent*))
        status (:code (httpc/status resp))
        res (httpc/string resp)]
    (cond
      (httpc/failed? resp) (throw (httpc/error resp))
      (> status 299)       (throw (Exception. (str url " returned " status res "\n" (httpc/headers resp))))
      :else res)))
