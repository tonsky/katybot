(ns reflexes.ru.calc
  (:require [clojure.string :as str])
  (:use katybot.core
        katybot.brain
        katybot.utils))

(defcommand on-calculate-ru
  "посчитай <выражение> - вычисляет <выражение> (2^100, 100 USD to Rub, 36C to F)"
  #"(?iu)(?:[пс]осчитай|вычисли|сколько будет)(?:\s+мне)?\s*(.+)"
  [robot event [_ q]]
  (let [[lhs rhs error] (reflexes.calc/calc q)]
    (if (str/blank? error)
      (say robot [lhs " = " rhs])
      (say robot ["Прости, слишком сложно для меня: " error]))))

