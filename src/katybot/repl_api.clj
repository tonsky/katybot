(ns katybot.repl-api
  (:use katybot.utils
        katybot.campfire-api))

(defn reload []
  (fyi "Loading campfire_api")
  (load "campfire_api")
  (use 'katybot.campfire-api))

(defn redef-api []
  (def api
    (campfire-async-api "katybot" "036e950c60968b68b8e9f59039998fea91103ddd")))

(reload)
(let [v (def api)]
  (when-not (.hasRoot v)
    (redef-api)))

(defn test-join []
  (join api "483271"))

(defn test-leave []
  (leave api "483271"))

(defn test-listen []
  (join api "483271")
  (def agnt
    (listen api "483271" (fn [& args] (apply fyi "Callback: " args)))))

(defn test-stop-listening []
  (stop-listening api "483271"))