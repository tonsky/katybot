(ns reflexes.calc
  (:require [clojure.string :as str])
  (:use katybot.core
        katybot.brain
        katybot.utils))

(defn- to-sup [ch]
  (char
    (case ch
      \- 0x207B
      \+ 0x207A
      \1 0x00B9
      \2 0x00B2
      \3 0x00B3
      (+ 0x2070 (Character/digit ch 10)))))

(defn- google-unquote [s]
  (-> s
    (str/replace #"\\x([0-9a-f]{2})"
      (fn [[_ s]] (-> s (Integer/parseInt 16) char str)))
    (str/replace #"&#(\d{2,4});"
      (fn [[_ s]] (-> s (Integer/parseInt 10) char str)))
    (str/replace #"<sup>([\-+\d]*)</sup>"
      (fn [[_ s]] (apply str (map (comp to-sup char) (.getBytes s)))))))

(defn- google-json-attr-str [a json]
  (-> (re-pattern (str a "\\s*:\\s*\"(.*?)\""))
      (re-find json)
      (second)
      (google-unquote)))

(defn calc [q]
  (let [resp  (http-get "http://www.google.com/ig/calculator" :query {:hl "en"  :q q} :user-agent "Mozilla/5.0")
        lhs   (google-json-attr-str "lhs" resp)
        rhs   (google-json-attr-str "rhs" resp)
        error (google-json-attr-str "error" resp)]
    [lhs rhs error]))

(defcommand on-calculate
  "calc me <expr> - calculates <expr> (2+2, 200 USD to Rub, 100C to F)"
  #"(?i)calc(?:ulate)?(?:\s+me)?\s*(.*)"
  [robot event [_ q]]
  (if (str/blank? q)
    (say robot "Calc you what?")
    (let [[lhs rhs error] (calc q)]
      (if (str/blank? error)
        (say robot [lhs " = " rhs])
        (say robot ["It’s too hard: " error])))))

