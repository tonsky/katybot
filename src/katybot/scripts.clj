(ns katybot.scripts
  (:require [clojure.string :as str])
  (:require [http.async.client :as httpc])
  (:require [clojure.data.json :as json])
  (:use [katybot.core]))

(defn http-get [url & {:keys [query user-agent] :or {query {} user-agent "Katybot-clj/0.1"}}]
  (log-debug "HTTP GET:\n  " url "\n  " query)
  (with-open [client (httpc/create-client :user-agent user-agent)]
    (let [resp   (httpc/await (httpc/GET client url :query query))
          status (:code (httpc/status resp))
          res    (httpc/string resp)]
    (when-not (= 200 status)
      (log-err "BAD RESPONSE: " res)
      (throw (Exception. (str url ": " status res "\n" (httpc/headers resp)))))
    res)))

(defn on-stop [adapter {:keys [type text]}]
  (when (and (= type :text) (re-find #"(?i)stop|shutdown|cancel|exit|quit" text))
    (say adapter "I’m out")
    :shutdown))

(defn on-join [adapter {:keys [type user-id]}]
  (if (= type :join)
    (let [user-info (user adapter user-id)]
      (say adapter (format "Glad to see you again, %s" (:name user-info)))
      :answered)))

(defn on-leave [adapter {:keys [type user-id]}]
  (if (= type :leave)
    (let [user-info (user adapter user-id)]
      (say adapter (format "%s was a good man" (:name user-info)))
      (say adapter "I guess")
      :answered)))

(defn on-hello [adapter {:keys [type text user-id]}]
  (when (and (= type :text) (re-find #"(?i)hello|hi" text))
    (let [user-info (user adapter user-id)]
      (say adapter (format "Nice to see you again, %s" (:name user-info)))
      ;(say-img adapter (:avatar user-info))
      :answered)))

(defn img [q]
  (let [resp   (json/read-json (http-get "http://ajax.googleapis.com/ajax/services/search/images" :query {:v 1.0 :rsz 8 :q q}))
        images (get-in resp [:responseData :results])]
    (if (empty? images)
      nil
      (rand-nth images))))

(defn on-images [adapter {:keys [type text]}]
  (if (= type :text)
    (when-let [[_ q] (re-find #"(?i)(?:image|img)(?: me)?(.*)" text)]
      (let [img (img q)]
        (if img
          (say-img adapter (:unescapedUrl img))
          (say     adapter "I have nothing to show you"))
        :answered))))

(defn on-mustachify [adapter {:keys [type text]}]
  (if (= type :text)
    (when-let [[_ q] (re-find #"(?i)(?:mo?u)?sta(?:s|c)he?(?: me)?\s+(.*)" text)]
      (let [img-url (if (re-matches #"(?i)^https?://.*" q) q (:unescapedUrl (img q)))]
        (if img-url
          (say-img adapter (str "http://mustachify.me/" (rand-int 3) "?src=" img-url))
          (say     adapter "Couldn't find any of his images"))
        :answered))))

(defn on-translate [adapter {:keys [type text]}]
  (if (= type :text)
    (when-let [[_ f t q] (re-find #"(?i)translate(?:\s+me)?(?:\s+from\s+([a-z]{2}))?(?:\s+to\s+([a-z]{2}))?\s+(.*)" text)]
      (let [from (or f "auto")
            to   (or t "en")
            resp (http-get "http://translate.google.com/translate_a/t"
                  :query { :sl from  :tl to  :text (str \" q \")
                           :client "t"  :hl "en"  :multires 1  :sc 1  :ssel 0  :tsel 0  :uptl "en" }
                  :user-agent "Mozilla/5.0")
            res (json/read-json resp)]
        (if-let [translation (get-in res [0 0 0])]
          (say adapter (str translation " is " to " for " \" q \" " (" (res 1) ")"))
          (say adapter (str \" q \" " will lose too much in translation to " to)))
      :answered))))

(defn on-calculate [adapter {:keys [type text]}]
  (if (= type :text)
    (when-let [[_ q] (re-find #"(?i)calc(?:ulate)?(?: me)?\s+(.*)" text)]
      (let [resp (http-get "http://www.google.com/ig/calculator" :hl "en" :q q)
            lhs   ((re-find #"lhs\s*:\s*\"(.*?)\"" resp) 1)
            rhs   ((re-find #"rhs\s*:\s*\"(.*?)\"" resp) 1)
            error ((re-find #"error\s*:\s*\"(.*?)\"" resp) 1)]
        (if (str/blank? error)
          (say adapter (str lhs " = " rhs))
          (say adapter (str "It’s too hard:" error)))
        :answered))))          

(defn on-unknown [adapter {:keys [type text]}]
  (when (= type :text)
    (say adapter (str "I don’t get it: " text))
    :answered))

(def on-event
  (wrap-commands 
    [ "/" "Kate" "Katy" ]
    [ on-stop
      on-join
      on-leave
      on-calculate
      on-translate
      on-images
      on-mustachify
      on-hello
      on-unknown]))
