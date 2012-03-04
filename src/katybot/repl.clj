(ns katybot.repl
  (:use katybot.utils)
  (:require [clojure.string :as str]))

(defn reload-robot []
  (doseq [module ["utils" "core" "brain" "campfire" "console" "atom_memory" "file_memory"]]
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
  (fyi "Loading repl")
  (load "repl")
  (use 'katybot.repl))

(reload-robot)
(reload-reflexes)

(defn test-console []
  (-> {}
    (+file-memory "robot.memory")
    (+console-receptor)
    (+global-brain ["/" "Katy" "Kate" "Катя"])
    (listen)))

(defn test-campfire []
  (-> {}
    (+file-memory "robot.memory")
    (+campfire-receptor (env "KATYBOT_CAMPFIRE_ACCOUNT")
                        (env "KATYBOT_CAMPFIRE_ROOM")
                        (env "KATYBOT_CAMPFIRE_TOKEN"))
    (+global-brain [(env "KATYBOT_CAMPFIRE_ALIASES")])
    (listen)))

