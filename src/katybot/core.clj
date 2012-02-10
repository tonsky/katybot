(ns katybot.core
  (:require [clojure.string :as str]
            clojure.stacktrace))

(defmulti listen                        :receptor)
(defmulti say      (fn [robot msg]     (:receptor robot)))
(defmulti say-img  (fn [robot url]     (:receptor robot)))
(defmulti user     (fn [robot user-id] (:receptor robot)))
(defmulti users                         :receptor)
(defmulti memorize (fn [robot k v]     (:memory robot)))
(defmulti recall   (fn [robot k]       (:memory robot)))
(defmulti consider (fn [robot event]   [(:brain robot) (:type event)]))


(defn change-keys [m & keys]
  (reduce (fn [acc [old new]] (dissoc (assoc acc new (m old)) old)) m (apply hash-map keys)))

(defn log  [& msg] (println (apply str msg)))
(defn btw  [& msg] (log "\u001b[1;30m" (apply str msg) "\u001b[m"))
(defn fyi  [& msg] (log "\u001b[1;32m" (apply str msg) "\u001b[m"))
(defn omg! [& msg] (log "\u001b[1;31m" (apply str msg) "\u001b[m"))

(defn- on-help
  "help      — display this help"
  [fns robot {:keys [type text]}]
  (if (= type :text)
    (when (re-find #"(?i)help" text)
      (let [helps (remove nil? (map #(:doc (meta %)) (conj fns #'on-help)))]
        (say robot (str/join "\n" helps))
      :answered))))

(defn- on-unknown [robot {:keys [type text]}]
  (when (= type :text)
    (say robot ["I don’t get it: " text])
    :answered))

(defn- run-safe [f robot event]
  (try
    (f robot event)
    (catch Exception e
      (let [cause (clojure.stacktrace/root-cause e)
            msg (into ["I’m broke and that’s why:" 
                       ""
                       (.toString cause)] 
                      (map #(str "  " (.toString %)) (.getStackTrace cause)))]
        (say robot (str/join "\n" msg)))
      :died)))

(defn wrap-commands [aliases fns]
  (let [alias-re (re-pattern (str "(?i)^(" (str/join "|" aliases) ")[:,]?\\s*(.*)"))
        help     (partial on-help fns)
        process  (fn [robot event] (some #(run-safe % robot event) (conj fns help on-unknown)))]
    (fn [robot event]
      (if (= (:type event) :text)
        (when-let [[_ alias cmd] (re-find alias-re (:text event))]
          (process robot (assoc event :text cmd :alias alias)))
        (process robot event)))))
