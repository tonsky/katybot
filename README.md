#### Starting

To run Katybot in repl:

    user=> (load "katybot/core" "katybot/scripts" "katybot/console")
    user=> (katybot.core/start (katybot.console.Console.) katybot.scripts/on-event)

To run her in Campfire:

    user=> (load "katybot/core" "katybot/scripts" "katybot/campfire")
    user=> (def account "...") ; account is your third-level domain on campfirenow.com
    user=> (def room-id "...")
    user=> (def token   "...")
    user=> (katybot.campfire/start-campfire account room-id token katybot.scripts/on-event)

#### Testing

To test the bot, use

    > Kate, hello!
    < Nice to see you again
    > /hi
    < Nice to see you again

`katybot.scripts/on-event` uses `["/" "Kate" "Katy"]` aliases, so bot will respond on any commands starting with these words.

#### Stopping

To make her exit, tell her 

    > /stop
    < I'm out

or type an empty string (console only).
