(ns reflexes.ru.translate
  (:require [clojure.string :as str]
            [clojure.data.json :as json])
  (:use katybot.core
        katybot.brain
        katybot.utils))

(defcommand on-translate-ru
  "переведи [с <lg>] [на <lg>] <фразу> - переводит <фразу>, где <lg> — en, ru, de, fr..."
  #"(?iu)переведи(?:\s+мне)?(?:\s+с\s+([a-z]{2}))?(?:\s+на\s+([a-z]{2}))?\s+(.*)"
  [robot event [_ from to q]]
  (let [res (reflexes.translate/translate q from to)]
    (if (:translation res)
      (say robot (format+ "[:translation] ([:to]) = «[:q]» ([:from])" res))
      (say robot (format+ "«[:q]» слишком многое потерят в переводе на [:to]" res)))))

