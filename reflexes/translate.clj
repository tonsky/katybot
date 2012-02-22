(ns reflexes.translate
  (:require [clojure.string :as str]
            [clojure.data.json :as json])
  (:use katybot.core
        katybot.brain
        katybot.utils))

(defn translate [q from to]
  (let [from (or from (if (re-matches #"(?i)[\p{InCyrillic}\d\p{Punct}\s]+" q) "ru" "auto"))
        to   (or to "en")
        resp (http-get "http://translate.google.com/translate_a/t"
              :query { :sl from  :tl to  :text (str \" q \")
                       :client "t"  :hl "en"  :multires 1  :sc 1  :ssel 0  :tsel 0  :uptl "en" }
              :user-agent "Mozilla/5.0")
        res (json/read-json resp)]
  {:translation (get-in res [0 0 0]) :q q :from (res 1) :to to}))

(defcommand on-translate
  "translate [from <lg>] [to <lg>] <phrase> - translates <phrase>, <lg> is two-letter lang code (en, ru, de, fr...)"
  #"(?i)translate(?:\s+me)?(?:\s+from\s+([a-z]{2}))?(?:\s+to\s+([a-z]{2}))?\s+(.*)"
  [robot event [_ from to q]]
  (let [res (translate q from to)]
    (if (:translation res)
      (say robot (format+ "[:translation] is [:to] for \" [:q] \" ([:from])" res))
      (say robot (format+ "\" [:q] \" will lose too much in translation to [:to]" res)))))

