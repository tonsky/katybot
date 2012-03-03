(defproject katybot "0.3.0-SNAPSHOT"
  :description "Campfire bot written in clojure"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [http.async.client "0.4.0"]
                 [org.slf4j/slf4j-nop "1.6.2"]
                 [org.clojure/data.json "0.1.1"]]
  :repl-init katybot.repl
  ;:repl-init katybot.repl-api
  :jvm-opts ["-Dfile.encoding=UTF-8"])
