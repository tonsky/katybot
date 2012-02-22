(ns katybot.core
  (:require [clojure.string :as str]
            clojure.stacktrace))

(defmulti listen                        :receptor)
(defmulti say      (fn [robot msg]     (:receptor robot)))
(defmulti say-img  (fn [robot url]     (:receptor robot)))
(defmulti user     (fn [robot user-id] (:receptor robot)))
(defmulti users                         :receptor)
(defmulti shutdown                      :receptor)
(defmulti memorize (fn [robot k v]     (:memory robot)))
(defmulti recall   (fn [robot k]       (:memory robot)))
(defmulti consider (fn [robot event]   (:brain robot)))

