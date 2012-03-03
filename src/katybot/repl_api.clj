(ns katybot.repl-api
  (:use katybot.utils
        katybot.campfire-api))

(def ^:dynamic *room*    "483271")
(def ^:dynamic *account* "katybot")
(def ^:dynamic *token*   "036e950c60968b68b8e9f59039998fea91103ddd")

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
  (stop-listening api *room*))