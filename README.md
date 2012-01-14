To run Katybot in repl:

    user=> (load "katybot/core" "katybot/scripts" "katybot/console")
    user=> (katybot.core/start (katybot.console.Console.) (partial katybot.scripts/on-event ["/" "Kate" "Katy"]))

To run her in Campfire:

    user=> (load "katybot/core" "katybot/scripts" "katybot/campfire")
    user=> (def account "...") ; account is your third-level domain on campfirenow.com
    user=> (def room-id "...")
    user=> (def token   "...")
    user=> (katybot.campfire/start-campfire account room-id token (partial katybot.scripts/on-event ["/" "Kate" "Katy"]))

To test the bot, use

    > Kate, hello!
    < Nice to see you again
    > /hi
    < Nice to see you again

To make her exit, tell her 

    > /stop
    < I'm out

or type an empty string (in console).
