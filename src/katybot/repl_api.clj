(ns katybot.repl-api
  (:use katybot.utils
        katybot.campfire-api))

(def ^:dynamic *room*    (env "KATYBOT_CAMPFIRE_ROOM"))
(def ^:dynamic *account* (env "KATYBOT_CAMPFIRE_ACCOUNT"))
(def ^:dynamic *token*   (env "KATYBOT_CAMPFIRE_TOKEN"))

(defn reload []
  (fyi "Loading campfire_api")
  (load "campfire_api")
  (use 'katybot.campfire-api)
  (def api
    (campfire-async-api *account* *token*)))

(reload)

(defn test-join []
  (join api *room*))

(defn test-leave []
  (leave api *room*))

(defn test-listen []
  (binding [katybot.campfire-api/*debug* true]
    (def agnt
      (listen api *room* (fn [body] (fyi "Callback: " body))))))

(defn test-stop-listening []
  (stop-listening api *room*)
  :stopped)
