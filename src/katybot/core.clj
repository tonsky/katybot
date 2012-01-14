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
