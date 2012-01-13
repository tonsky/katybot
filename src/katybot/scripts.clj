(ns katybot.scripts
  (:require [clojure.string :as str])
  (:use [katybot.core]))

(defn wrap [fn adapter env]
  (fn adapter env)) ;TODO wrap into try/catch 

(defn process [fns adapter env]
  (some #(wrap % adapter env) fns))

(defn privet [adapter {:keys [text user-id]}]
  (when (re-find #"(?i)hello|hi" text)
    (let [user-info (user adapter user-id)]
      (say adapter (format "Nice to see you again, %s" (:name user-info)))
      (say-img adapter (:avatar user-info))
      :answered)))

(defn shutdown [adapter {text :text}]
  (when (re-find #"(?i)stop|shutdown|cancel|exit|quit" text)
    (say adapter "I’m out")
    :shutdown))

(defn unknown [adapter _]
  (say adapter "Didn’t get it")
  :answered)

(defn personal [aliases fns adapter {text :text :as event}]
  (when-let [[_ alias cmd] (re-find aliases text)]
    (process fns adapter (assoc event :text cmd :alias alias))))

(defn log [adapter {:keys [timestamp type] :as event}]
  (if (not= type :text)
    (prn event))
  nil)

(defn filter-text [adapter {type :type}]
  (if (= type :text)
    nil
    :ignored))

(defn on-event [aliases adapter event]
  (let [aliases-re (re-pattern (str "(?i)^(" (str/join "|" aliases) ")[:,]?\\s*(.*)"))
        personal-fns [
          shutdown
          privet
          unknown]
        top_fns [
          log
          filter-text
          (partial personal aliases-re personal-fns)]]
    (process top_fns adapter event)))
