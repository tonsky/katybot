(ns katybot.brain
  (:require [clojure.string :as str])
  (:use katybot.core
        katybot.utils))

(defonce reflexes (atom #{}))

(defn register-reflex [name]
  (swap! reflexes conj (resolve name)))

(defmacro defreflex [name type [robot event] & body]
 `(do
    (defn ~name [~robot {type# :type :as event#}]
      (when (= type# ~type)
        (let [~event event#]
          ~@body
          :answered)))
    (register-reflex '~name)))

(defmacro defcommand [& body]
  (let [[name doc attr-map re [robot event groups] body]
        (consume [symbol? string? map? regex? vector?] body)]
   `(do
      (defn ~name
        ~(merge {:doc doc} attr-map)
        [~robot {type# :type text# :text :as event#}]
        (if (= type# :command)
          (when-let [~groups (re-find ~re text#)]
            (let [~event event#]
              ~@body
              :answered))))
      (register-reflex '~name))))

(defn- go-over [robot event reflexes]
  (->> (map #(% robot event) reflexes)
       (apply max-key #(get {:shutdown 100 :reconnect 50 :answered 10 nil 0} % 25))))

(defn- broke-msg [e]
  (let [cause (clojure.stacktrace/root-cause e)
        msg   (into ["I’m broke and that’s why:"
                     ""
                     (.toString cause)]
                     (map #(str "  " (.toString %)) (.getStackTrace cause)))]
    (str/join "\n" msg)))

(defn consider-event [robot {:keys [type text] :as event} aliases reflexes]
  (try
    (if-let [[_ salutation command] (and (= type :text) text (re-matches aliases text))]
      (let [event (assoc event :type :command :text command :salutation salutation)]
        (or (go-over robot event reflexes)
            (say robot "I don't get it, sorry")))
      (go-over robot event reflexes))
    (catch Exception e
      (say robot (broke-msg e))
      :died)))

(defn- aliases-re [aliases]
  (re-pattern (str "(?i)^(" (str/join "|" aliases) ")[\\s\\,\\.\\:]*(.*)")))

(defn +global-brain [robot aliases]
  (assoc robot
    :brain     ::global-brain
    ::aliases  (aliases-re aliases)))

(defmethod consider ::global-brain [robot event]
  (let [{aliases ::aliases} robot]
    (consider-event robot event aliases @reflexes)))

(defn +brain [robot aliases reflexes]
  (assoc robot
    :brain     ::brain
    ::aliases  (aliases-re aliases)
    ::reflexes reflexes))

(defmethod consider ::brain [robot event]
  (let [{aliases ::aliases reflexes ::reflexes} robot]
    (consider-event robot event aliases reflexes)))
