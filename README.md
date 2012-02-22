#### Starting

Best way to start Katybot is to use `lein repl`, this way `repl_helper.clj` will be loaded automatically with a bunch of useful fns.

To test Katybot in console:

    katybot.repl-helper=> (test-console)

To run her in Campfire, define following env variables before runnig `lein repl`:

    ~/katybot/$ export KATYBOT_CAMPFIRE_ACCOUNT=... # account is your third-level domain on campfirenow.com
    ~/katybot/$ export KATYBOT_CAMPFIRE_ROOM   =...
    ~/katybot/$ export KATYBOT_CAMPFIRE_TOKEN  =...
    ~/katybot/$ export KATYBOT_CAMPFIRE_ALIASES="/|Kat[ey]|robot"
    ~/katybot/$ lein repl

    katybot.repl-helper=> (test-campfire)


#### Testing

To test the bot, use

    > Kate, hello!
    < Nice to see you again
    > /hi
    < Nice to see you again

To see list of all available command use `help`:

    > /help
    < stop      — ask bot to shutdown gracefully
      calc me   — ...
      ...


#### Stopping

To make her exit, tell her `stop`:

    > /stop
    < I’m out

or type an empty string (console only).


#### Extending

Take a look at `reflexes` directory for hints on how to implement your own extension scripts. `katybot.repl-helper` scans `reflexes` and `reflexes/ru` directroies by default and loads every `.clj` file as an robot’s extension script.

Extensions could be reloaded without stopping runnig robot by evaluating

    katybot.repl-helper=> (reload-reflexes)
