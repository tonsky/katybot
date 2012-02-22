(ns reflexes.ru.images
  (:require [clojure.string :as str]
            [clojure.data.json :as json])
  (:use katybot.core
        katybot.brain
        katybot.utils))

(defcommand on-image-ru
  "нарисуй <запрос> - показать случайное изображение по запросу <запрос>"
  #"(?iu)(?:нарисуй)(?: мне)?\s+(.*)"
  [robot event [_ q]]
  (if-let [img (reflexes.images/find-img q)]
    (say-img robot img)
    (say     robot "Странные у тебя запросы")))

(defcommand on-mustachify-ru
  "подрисуй усы <кому или url> - попробуйте"
  #"(?iu)(?:на|под)рисуй усы(?: к)?\s+(.*)"
  [robot event [_ q]]
  (if-let [img-url (if (re-matches #"(?i)^https?://.*" q) q (reflexes.images/find-img q))]
    (say-img robot (str "http://mustachify.me/" (rand-int 3) "?src=" img-url))
    (say     robot (format+ "Не могу найти [0] в интернете" [q]))))

