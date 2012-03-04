(ns katybot.file-memory
  (:use [katybot.core]))

(defn- read-file [file]
  (if (.exists (clojure.java.io/as-file file))
    (read-string (slurp file))
    {}))

(defn- store [state k v]
  (let [{file :file, memory :memory} state
        updated-memory (assoc memory k v)]
    (spit file updated-memory)
    (assoc state :memory updated-memory)))

(defn +file-memory [robot file]
  (assoc robot
    :memory ::file-memory 
    ::agent (agent {:file file :memory (read-file file)})))

(defmethod memorize ::file-memory [robot k v]
  (send (::agent robot) store k v)
  robot)

(defmethod recall ::file-memory [robot k]
  (get-in @(::agent robot) [:memory k]))

