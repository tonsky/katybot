(ns reflexes.help
  (:require [clojure.string :as str])
  (:use katybot.core
        katybot.brain
        katybot.utils))

(defn list-commands []
  (->> @katybot.brain/reflexes
       (map (comp :doc meta))
       (remove nil?)
       sort
       (str/join "\n")))

(defcommand on-help
  "help - print this help"
  #"(?i)(help|^h$)"
  [robot event groups]
  (say robot (list-commands)))
