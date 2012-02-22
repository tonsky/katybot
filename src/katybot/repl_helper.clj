(ns katybot.repl-helper
  (:use katybot.utils)
  (:require [clojure.string :as str]))

(defn reload-robot []
  (doseq [module ["utils" "core" "brain" "campfire" "console" "atom_memory" ]]
    (fyi "Loading " module)
    (load module)
    (use (-> (str "katybot." module) (str/replace "_" "-") symbol))))

(defn- load-dir [dir]
  (fyi "Loading " dir ":")
  (doseq [f (file-children (.getCanonicalFile dir) #".*\.clj")]
    (fyi "  " (.getName f))
    (load-file (.getCanonicalPath f))))

(defn reload-reflexes []
  (let [dir (file ".")]
    (load-dir (file-child dir "reflexes"))
    (load-dir (file-child dir "reflexes/ru"))))

(defn reload-all []
  (fyi "Loading repl_helper")
  (load "repl_helper")
  (use 'katybot.repl-helper))

(reload-robot)
(reload-reflexes)

(defn test-console []
  (-> {}
    (+atom-memory)
    (+console-receptor)
    (+global-brain ["/" "Katy" "Kate" "Катя"])
    (listen)))

(defn- env [v]
  (-> (System/getenv) (.get v)))

(defn test-campfire []
  (-> {}
    (+atom-memory)
    (+campfire-receptor (env "KATYBOT_CAMPFIRE_ACCOUNT")
                        (env "KATYBOT_CAMPFIRE_ROOM")
                        (env "KATYBOT_CAMPFIRE_TOKEN"))
    (+global-brain (env "KATYBOT_CAMPFIRE_ALIASES"))
    (listen)))

(defn test-campfire-bg []
  (-> (Thread. #(test-campfire)) (.start)))

