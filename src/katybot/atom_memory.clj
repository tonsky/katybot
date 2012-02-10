(ns katybot.atom-memory
  (:use [katybot.core]))

(defn +atom-memory [robot]
  (assoc robot :memory ::atom-memory ::storage (atom {})))

(defmethod memorize ::atom-memory [robot k v]
  (swap! (::storage robot) assoc k v)
  robot)

(defmethod recall ::atom-memory [robot k]
  (get @(::storage robot) k))

