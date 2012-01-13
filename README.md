Usage:

    user=> (load "katybot/core" "katybot/scripts" "katybot/console")
    user=> (katybot.core/start (katybot.console.Console.) (partial katybot.scripts/on-event ["/" "Kate" "Katy"]))

To test the bot, use

    > Kate, hello!
    < Nice to see you again
    > /hi
    < Nice to see you again

To exit, type

    > /stop
    < I'm out

or type an empty string.
