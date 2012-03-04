(ns katybot.utils
  (:require [http.async.client :as httpc]
            [clojure.data.json :as json]
            [clojure.string :as str]
            katybot.core))

(defn regex? [obj]
  (instance? java.util.regex.Pattern obj))

(defn consume [preds forms]
  (loop [res      []
         [p & ps] preds
         [f & fs] forms]
    (if (nil? p)
      (conj res (cons f fs))
      (if (p f)
        (recur (conj res f) ps fs)
        (recur (conj res nil) ps (cons f fs))))))

(defn change-keys [m & {:as keys}]
  (reduce
    (fn [acc [old new]]
      (-> acc
        (assoc new (m old))
        (dissoc old)))
    m keys))

(defn log  [& msg] 
  (let [ts (-> (java.text.SimpleDateFormat. "MMM dd HH:mm:ss") (.format (java.util.Date.)))]
    (println (apply str "\u001b[1;30m" ts "\u001b[m " msg))))
(defn btw  [& msg] (log "\u001b[1;30m" (apply str msg) "\u001b[m"))
(defn fyi  [& msg] (log "\u001b[1;32m" (apply str msg) "\u001b[m"))
(defn omg! [& msg] (log "\u001b[1;31m" (apply str msg) "\u001b[m"))

(defn format+ [s vals]
  (str/replace s #"(\[.+?\])"
    (fn [[_ keys]]
      (get-in vals (read-string keys) "<NOT FOUND>"))))

(defn http-get [url & {:keys [query user-agent] :or {query {} user-agent katybot.core/version}}]
  (btw "HTTP GET:\n  " url "\n  " query)
  (with-open [client (httpc/create-client :user-agent user-agent)]
    (let [resp   (httpc/await (httpc/GET client url :query query))
          status (:code (httpc/status resp))
          res    (httpc/string resp)]
      (cond
        (httpc/failed? resp) (throw (httpc/error resp))
        (not= status 200) (do
          (omg! "BAD RESPONSE: " res)
          (throw (Exception. (str url ": " status res "\n" (httpc/headers resp)))))
        :else res))))

(defn now []
  (-> (java.util.Date.) .getTime))

(defn schedule
  "Schedule function for repeating execution.
   'task' is a zero-arg function, returning :continue to continue executing or nil to stop"
  [task period]
  (let [timer      (java.util.Timer. true)
        timer-task (proxy [java.util.TimerTask] [] 
                     (run [] (when-not (task) (.cancel this))))]
    (.scheduleAtFixedRate timer timer-task period period)))

(defn env [v]
  (-> (System/getenv) (.get v)))
