(ns reflexes.etiquette
  (:require [clojure.string :as str])
  (:use katybot.core
        katybot.brain
        katybot.utils))

(defcommand on-hello
  "hello - welcome your robot"
  #"(?iu)(hello|hi)"
  [robot event groups]
  (let [user (user robot (:user-id event))]
    (say robot (format+
      "Well, [:group 1] there, [:user :name]"
      {:group groups :user user}))))

(defreflex on-join
  :join
  [robot event]
  (let [user (user robot (:user-id event))]
    (say robot (format+ "Glad to see you again, [:user :name]" {:user user}))))

(defreflex on-leave
  :leave
  [robot event]
  (let [user (user robot (:user-id event))]
    (say robot (format+ "[:user :name] was a good man." {:user user}))
    (say robot "I guess")))

(defn badwords-reflex [badwords reply]
  (let [re (re-pattern (str "(?iu)\\b(" (str/join "|" badwords) ")\\b"))]
    (fn [robot {:keys [text user-id]}]
      (if text ; any text event
        (when-let [[_ bw] (re-find re text)]
          (say robot (format+ reply {:user (user robot user-id) :bw bw}))))
      nil)))

(def on-badwords
  (badwords-reflex ["fuck" "ass" "dick" "gay" "pussy" "node.js" "perl"]
    "[:user :name], please don’t say words like [:bw] anymore"))
(register-reflex 'on-badwords)
