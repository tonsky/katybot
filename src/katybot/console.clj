(ns katybot.console
  (:require [clojure.string :as str])
  (:use [katybot.core]))

(defrecord Console []
  Adapter
  (start [this on-event]
    (printf "\u001b[1;32m> ") (flush)
    (read-line)
    (doseq [line (line-seq (java.io.BufferedReader. *in*))
            :let [event {:type :text :text line :user-id 0 :timestamp (.getTime (java.util.Date.))}
                  action (on-event this event)]
            :while (not (str/blank? line))
            :while (not= action :shutdown)]
            (printf "\u001b[1;32m> ") (flush))
    (printf "\u001b[m") (flush))
  (say     [_ msg]      (println "\u001b[1;36m<" msg "\u001b[m"))
  (say-img [this url]   (println "\u001b[1;35m[" url "]\u001b[m"))
  (user    [_ user-id] {:id user-id :name "%username%" :avatar "http://example.com/nobody.png"})
  (users   [this]      [(user this 0) (user this 1) (user this 2)]))