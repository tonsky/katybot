(ns katybot.scripts
  (:require [clojure.string :as str])
  (:require [http.async.client :as httpc])
  (:require [clojure.data.json :as json])
  (:use [katybot.core]))

(defn http-get [url & {:keys [query user-agent] :or {query {} user-agent "Katybot-clj/0.1"}}]
  (btw "HTTP GET:\n  " url "\n  " query)
  (with-open [client (httpc/create-client :user-agent user-agent)]
    (let [resp   (httpc/await (httpc/GET client url :query query))
          status (:code (httpc/status resp))
          res    (httpc/string resp)]
    (when-not (= 200 status)
      (omg! "BAD RESPONSE: " res)
      (throw (Exception. (str url ": " status res "\n" (httpc/headers resp)))))
    res)))

(defn on-stop
  "stop      — ask bot to shutdown gracefully"
  [adapter {:keys [type text]}]
  (when (and (= type :text) (re-find #"(?i)stop|shutdown|cancel|exit|quit" text))
    (say adapter "I’m out")
    :shutdown))

(defn on-join [adapter {:keys [type user-id]}]
  (if (= type :join)
    (let [user-info (user adapter user-id)]
      (say adapter ["Glad to see you again, " (:name user-info)])
      :answered)))

(defn on-leave [adapter {:keys [type user-id]}]
  (if (= type :leave)
    (let [user-info (user adapter user-id)]
      (say adapter [(:name user-info) " was a good man"])
      (say adapter "I guess")
      :answered)))

(defn on-hello
  "hello     — welcome your robot"
  [adapter {:keys [type text user-id]}]
  (when (and (= type :text) (re-find #"(?i)hello|hi" text))
    (let [user-info (user adapter user-id)]
      (say adapter ["Well, hello there, " (:name user-info)])
      ;(say-img adapter (:avatar user-info))
      :answered)))

(defn img [q]
  (let [resp   (json/read-json (http-get "http://ajax.googleapis.com/ajax/services/search/images" :query {:v 1.0 :rsz 8 :q q}))
        images (get-in resp [:responseData :results])]
    (if (empty? images)
      nil
      (rand-nth images))))

(defn on-images
  "image me  <smth> — display random image from google image search"
  [adapter {:keys [type text]}]
  (if (= type :text)
    (when-let [[_ q] (re-find #"(?i)(?:image|img)(?: me)?(.*)" text)]
      (let [img (img q)]
        (if img
          (say-img adapter (:unescapedUrl img))
          (say     adapter "I have nothing to show you"))
        :answered))))

(defn on-mustachify
  "mustache  <smbd or url> — try it"
  [adapter {:keys [type text]}]
  (if (= type :text)
    (when-let [[_ q] (re-find #"(?i)(?:mo?u)?sta(?:s|c)he?(?: me)?\s+(.*)" text)]
      (let [img-url (if (re-matches #"(?i)^https?://.*" q) q (:unescapedUrl (img q)))]
        (if img-url
          (say-img adapter (str "http://mustachify.me/" (rand-int 3) "?src=" img-url))
          (say     adapter "Couldn't find any of his images"))
        :answered))))

(defn on-translate
  "translate [from <lg>] [to <lg>] <phrase> — translates <phrase>, <lg> is two-letter lang code (en, ru, de, fr...)"
  [adapter {:keys [type text]}]
  (if (= type :text)
    (when-let [[_ f t q] (re-find #"(?i)translate(?:\s+me)?(?:\s+from\s+([a-z]{2}))?(?:\s+to\s+([a-z]{2}))?\s+(.*)" text)]
      (let [from (if (re-matches #"(?i)[\p{InCyrillic}\d\p{Punct}\s]+" q) "ru" "auto")
            to   (or t "en")
            resp (http-get "http://translate.google.com/translate_a/t"
                  :query { :sl from  :tl to  :text (str \" q \")
                           :client "t"  :hl "en"  :multires 1  :sc 1  :ssel 0  :tsel 0  :uptl "en" }
                  :user-agent "Mozilla/5.0")
            res (json/read-json resp)]
        (if-let [translation (get-in res [0 0 0])]
          (say adapter [translation " is " to " for " \" q \" " (" (res 1) ")"])
          (say adapter [\" q \" " will lose too much in translation to " to]))
      :answered))))

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
  (if-let [next-s (condp re-find s
        #"(.*?)\\x([0-9a-f]{2})(.*)" :>> 
          (fn [[_ g1 g2 g3]]
            (str g1 (-> g2 (Integer/parseInt 16) (char)) g3))
        #"(.*?)&#(\d{2,4});(.*)" :>>
          (fn [[_ g1 g2 g3]]
            (str g1 (-> g2 (Integer/parseInt 10) (char)) g3))
        #"(.*?)<sup>([\-+\d]*)</sup>(.*)" :>>
          (fn [[_ g1 g2 g3]]
            (str g1 (apply str (map (comp to-sup char) (.getBytes g2))) g3))
        nil)]
    (recur next-s)
    s))

(defn- google-json-attr-str [a json]
  (-> (re-pattern (str a "\\s*:\\s*\"(.*?)\""))
      (re-find json)
      (second)
      (google-unquote)))

(defn on-calculate
  "calc me   <expr> — calculates <expr> (2+2, 200 USD to Rub, 100C to F)"
  [adapter {:keys [type text]}]
  (if (= type :text)
    (when-let [[_ q] (re-find #"(?i)calc(?:ulate)?(?:\s+me)?\s+(.*)" text)]
      (let [resp (http-get "http://www.google.com/ig/calculator" :query {:hl "en"  :q q} :user-agent "Mozilla/5.0")
            lhs   (google-json-attr-str "lhs" resp)
            rhs   (google-json-attr-str "rhs" resp)
            error (google-json-attr-str "error" resp)]
        (if (str/blank? error)
          (say adapter [lhs " = " rhs])
          (say adapter ["It’s too hard:" error]))
        :answered))))

(defn on-badwords [adapter {:keys [type text]}]
  (if (= type :text)
    (when-let [[_ bw] (re-find #"(?i)(fuck|ass|\b(?:gay|pussy)\b)" text)]
      (say adapter ["Please don’s say such things: " bw])
      nil)))

(def on-event
  (wrap-commands 
    [ "/" "Kate" "Katy" ]
    [ #'on-stop
      #'on-join
      #'on-leave
      #'on-badwords
      #'on-calculate
      #'on-translate
      #'on-images
      #'on-mustachify
      #'on-hello]))
