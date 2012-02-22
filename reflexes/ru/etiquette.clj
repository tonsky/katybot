(ns reflexes.ru.etiquette
  (:require [clojure.string :as str])
  (:use katybot.core
        katybot.brain
        katybot.utils))

(defcommand on-hello-ru
  "привет - поприветствуйте своего робота"
  #"(?iu)(привет(?:ик[ис]?)?|х[еэ]ллоу?|х[эае]й|зд[ао]рова)"
  [robot event groups]
  (let [user (user robot (:user-id event))]
    (say robot (format+
      "И тебе [:group 1], [:user :name]"
      {:group groups :user user}))))

(def on-badwords-ru
  (reflexes.etiquette/badwords-reflex ["писька" "титька" "попа" "путин"]
    "[:user :name], [:bw], что за язык?"))
(register-reflex 'on-badwords-ru)
