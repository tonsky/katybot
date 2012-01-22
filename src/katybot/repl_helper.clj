(ns katybot.repl-helper
  (:use katybot.core)
  (:use katybot.scripts)
  (:use katybot.console)
  (:use katybot.campfire)
  (:import katybot.console.Console)
  (:import katybot.campfire.Campfire))

(declare reload-code)

(defn reload-all []
  (load "repl_helper")
  (use 'katybot.repl-helper)
  (reload-code))

(defn reload-code []
  (doseq [module ["core" "scripts" "campfire" "console"]]
    (load module)
    (use (symbol (str "katybot." module)))))

(defn reload-scripts []
  (load "scripts")
  (use 'katybot.scripts))

(defn test-console []
  (start (Console.) #'on-event))

(defn- env [v]
  (-> (System/getenv) (.get v)))

(defn test-campfire []
  (start-campfire (env "KATYBOT_CAMPFIRE_ACCOUNT")
                  (env "KATYBOT_CAMPFIRE_ROOM")
                  (env "KATYBOT_CAMPFIRE_TOKEN")
                  #'on-event))

(defn test-campfire-bg []
  (-> (Thread. #(test-campfire)) (.start)))


