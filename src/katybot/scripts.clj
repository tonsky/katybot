(ns katybot.scripts
  (:require [clojure.string :as str])
  (:use [katybot.core]))

(defn on-stop [adapter {:keys [type text]}]
  (when (and (= type :text) (re-find #"(?i)stop|shutdown|cancel|exit|quit" text))
    (say adapter "I’m out")
    :shutdown))

(defn on-welcome [adapter {:keys [type user-id]}]
  (if (= type :join)
    (let [user-info (user adapter user-id)]
      (say adapter (format "Glad to see you again, %s" (:name user-info)))
      :answered)))

(defn on-bye [adapter {:keys [type user-id]}]
  (if (= type :leave)
    (let [user-info (user adapter user-id)]
      (say adapter (format "%s was a good man" (:name user-info)))
      (say adapter "I guess")
      :answered)))

(defn on-privet [adapter {:keys [type text user-id]}]
  (when (and (= type :text) (re-find #"(?i)hello|hi" text))
    (let [user-info (user adapter user-id)]
      (say adapter (format "Nice to see you again, %s" (:name user-info)))
      (say-img adapter (:avatar user-info))
      :answered)))

(defn on-unknown [adapter {:keys [type text]}]
  (when (= type :text)
    (say adapter (str "I don’t get it: " text))
    :answered))

(def on-event (wrap-commands ["/" "Kate" "Katy"] [on-stop on-welcome on-bye on-privet on-unknown]))
