(ns reflexes.images
  (:require [clojure.string :as str]
            [clojure.data.json :as json])
  (:use katybot.core
        katybot.brain
        katybot.utils))

(defn find-img [q]
  (let [resp   (json/read-json (http-get "http://ajax.googleapis.com/ajax/services/search/images" :query {:v 1.0 :rsz 8 :q q}))
        images (get-in resp [:responseData :results])]
    (if (empty? images)
      nil
      (:unescapedUrl (rand-nth images)))))

(defcommand on-image
  "image me <smth> - display random image from google image search"
  #"(?i)(?:image|img)(?: me)?(.*)"
  [robot event [_ q]]
  (if-let [img (find-img q)]
    (say-img robot img)
    (say     robot "I have nothing to show you")))

(defcommand on-mustachify
  "mustache <smbd or url> - just try it"
  #"(?i)(?:mo?u)?sta(?:s|c)he?(?: me)?\s+(.*)"
  [robot event [_ q]]
  (if-let [img-url (if (re-matches #"(?i)^https?://.*" q) q (find-img q))]
    (say-img robot (str "http://mustachify.me/" (rand-int 3) "?src=" img-url))
    (say     robot "Couldn't find any of his images")))

