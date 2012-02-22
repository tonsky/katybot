(ns katybot.utils
  (:require [http.async.client :as httpc]
            [clojure.data.json :as json]
            [clojure.string :as str]))

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

(defn change-keys [m & keys]
  (reduce (fn [acc [old new]] (dissoc (assoc acc new (m old)) old)) m (apply hash-map keys)))

(defn update-in-def [m keys default f]
  (let [old (get-in m keys default)]
    (assoc-in m keys (f old))))

(defn log  [& msg] (println (apply str msg)))
(defn btw  [& msg] (log "\u001b[1;30m" (apply str msg) "\u001b[m"))
(defn fyi  [& msg] (log "\u001b[1;32m" (apply str msg) "\u001b[m"))
(defn omg! [& msg] (log "\u001b[1;31m" (apply str msg) "\u001b[m"))

(defn subs+ [s start end]
  (let [e (if (neg? end) (+ (count s) end) end)]
    (subs s start e)))

(defn format+ [s vals]
  (str/replace s #"(\[.+?\])"
    (fn [[_ keys]]
      (get-in vals (read-string keys) "<NOT FOUND>"))))

(defn http-get [url & {:keys [query user-agent] :or {query {} user-agent "Katybot-clj/0.2"}}]
  (btw "HTTP GET:\n  " url "\n  " query)
  (with-open [client (httpc/create-client :user-agent user-agent)]
    (let [resp   (httpc/await (httpc/GET client url :query query))
          status (:code (httpc/status resp))
          res    (httpc/string resp)]
    (when-not (= 200 status)
      (omg! "BAD RESPONSE: " res)
      (throw (Exception. (str url ": " status res "\n" (httpc/headers resp)))))
    res)))

(defn file [name]
  (let [f (java.io.File. name)]
    (if (.exists f) f nil)))

(defn file-parent [file]
  (.getParentFile file))

(defn file-child [file name]
  (java.io.File. file name))

(defn file-children [file re]
  (seq (.listFiles file
    (reify java.io.FilenameFilter
      (accept [_ f name]
        (and
          (.isFile (java.io.File. f name))
          (not (nil? (re-matches re name)))))))))

(defn file-subdirs [file]
  (seq (.listFiles file
    (reify java.io.FileFilter
      (accept [_ f] (.isDirectory f))))))

