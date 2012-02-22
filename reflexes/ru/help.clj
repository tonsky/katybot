(ns reflexes.ru.help
  (:require [clojure.string :as str])
  (:use katybot.core
        katybot.brain
        katybot.utils))

(defcommand on-help-ru
  "справка - показать эту справку"
  #"(?iu)(справка|помощь)"
  [robot event groups]
  (say robot (reflexes.help/list-commands)))
