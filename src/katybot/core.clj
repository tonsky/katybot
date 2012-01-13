(ns katybot.core
  (:require [clojure.string :as str]))

(defprotocol Adapter
  (start   [this on-event])
  (say     [this msg])
  (say-img [this url])
  (user    [this user-id])
  (users   [this]))