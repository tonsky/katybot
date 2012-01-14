(ns katybot.scripts
  (:require [clojure.string :as str])
  (:use [katybot.core]))

(defn process [fns adapter env]
  (some #(% adapter env) fns))

(defn on-privet [adapter {:keys [text user-id]}]
  (when (re-find #"(?i)hello|hi" text)
    (let [user-info (user adapter user-id)]
      (say adapter (format "Nice to see you again, %s" (:name user-info)))
      (say-img adapter (:avatar user-info))
      :answered)))

(defn on-stop [adapter {text :text}]
  (when (re-find #"(?i)stop|shutdown|cancel|exit|quit" text)
    (say adapter "I’m out")
    :shutdown))

(defn on-unknown [adapter {text :text}]
  (say adapter (str "I don’t get it: " text))
  :answered)

(defn on-command [alias-re fns adapter {:keys [type text] :as event}]
  (if (= type :text)
    (when-let [[_ alias cmd] (re-find alias-re text)]
      (process fns adapter (assoc event :text cmd :alias alias)))))

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

(defn on-event [aliases adapter event]
  (let [alias-re (re-pattern (str "(?i)^(" (str/join "|" aliases) ")[:,]?\\s*(.*)"))
        command-fns [
          on-stop
          on-privet
          on-unknown]
        top_fns [
          on-welcome
          on-bye
          (partial on-command alias-re command-fns)]]
    (process top_fns adapter event)))
