(ns katybot.repl-helper
  (:use katybot.core
        katybot.scripts
        katybot.console
        katybot.campfire
        katybot.atom-memory))

(declare reload-code)

(defn reload-all []
  (load "repl_helper")
  (use 'katybot.repl-helper)
  (reload-code))

(defn reload-code []
  (doseq [module ["core" "scripts" "campfire" "console" "atom_memory"]
          :let [ns (symbol (str "katybot." (clojure.string/replace module "_" "-")))]]
    (load module)
    (use ns)))

(defn reload-scripts []
  (load "scripts")
  (use 'katybot.scripts))

(defn test-console []
  (-> {}
    (+atom-memory)
    (+console-receptor)
    (+test-brain)
    (listen)))

(defn- env [v]
  (-> (System/getenv) (.get v)))

(defn test-campfire []
  (-> {}
    (+atom-memory)
    (+campfire-receptor (env "KATYBOT_CAMPFIRE_ACCOUNT")
                        (env "KATYBOT_CAMPFIRE_ROOM")
                        (env "KATYBOT_CAMPFIRE_TOKEN"))
    (+test-brain)
    (listen)))

(defn test-campfire-bg []
  (-> (Thread. #(test-campfire)) (.start)))

