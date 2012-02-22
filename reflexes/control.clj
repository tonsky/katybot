(ns reflexes.control
  (:require [clojure.string :as str])
  (:use katybot.core
        katybot.brain
        katybot.utils))

(defcommand on-shutdown
  "stop - ask bot to shutdown gracefully"
  #"(?i)(stop|shutdown|cancel|exit|quit)"
  [robot event groups]
    (say robot "I’m out")
    (shutdown robot))

