(ns katybot.console
  (:require [clojure.string :as str])
  (:use katybot.core
        katybot.utils))

(defn +console-receptor [robot]
  (assoc robot
    :receptor ::console-receptor
    ::running (atom true)))

(defmethod listen ::console-receptor [robot]
  (printf "\u001b[1;32m> ") (flush)
  (doseq [line (line-seq (java.io.BufferedReader. *in*))
          :let [event {:type :text, :text line, :user-id 0, :timestamp (java.util.Date.)}
                action (consider robot event)]
          :while (not (str/blank? line))
          :while @(::running robot)]
    (printf "\u001b[1;32m> ") (flush))
  (printf "\u001b[m") (flush))

(defmethod say ::console-receptor [_ msg]
  (println "\u001b[1;36m<" (str/replace (apply str msg) "\n" "\n  ") "\u001b[m"))

(defmethod say-img ::console-receptor [_ url]
  (println "\u001b[1;35m[" url "]\u001b[m"))

(defmethod user ::console-receptor [_ user-id]
  {:id user-id, :name "%username%", :avatar "http://example.com/username.png"})

(defmethod users ::console-receptor [robot]
  [(user robot 0) (user robot 1) (user robot 2)])

(defmethod shutdown ::console-receptor [robot]
  (swap! (::running robot) (constantly false)))
