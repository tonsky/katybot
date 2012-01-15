(ns katybot.core
  (:require [clojure.string :as str]))

(defprotocol Adapter
  (start   [this on-event])
  (say     [this msg])
  (say-img [this url])
  (user    [this user-id])
  (users   [this]))

(defn change-keys [m & keys]
  (reduce (fn [acc [old new]] (dissoc (assoc acc new (m old)) old)) m (apply hash-map keys)))

(defn wrap-commands [aliases fns]
  (let [alias-re (re-pattern (str "(?i)^(" (str/join "|" aliases) ")[:,]?\\s*(.*)"))
        process  (fn [adapter event] (some #(% adapter event) fns))]
    (fn [adapter event]
      (if (= (:type event) :text)
        (when-let [[_ alias cmd] (re-find alias-re (:text event))]
          (process adapter (assoc event :text cmd :alias alias)))
        (process adapter event)))))
