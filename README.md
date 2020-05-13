# WikiIRC-Bot
### This was developed as part of a university module: IN2011 Computer Networks
WikiIRC-Bot is an IRC bot with MediaWiki API integrations.

## Running the bot
The bot takes 3 arguments: the address of a remote (or local for testing) host system, the port of the remote host's IRC 
instance and then the initial channel to attempt to join.
The format is `Bot <ip> <port> <channel>`.

For example `$~ Bot 123.123.123.123 6667 #chan` would attempt to join the `#chan` channel at `123.123.123.123:6667`. The 
bot does make sure that a remote host provided is actually reachable before attempting an IRC connection and will report 
any errors it encounters during this process.

## Usage and commands
**The bot's trigger string is `!wb`**

Type just `!wb` to get help output.

* `-r` gives a random Wikipedia article.
  * example: `!wb -r`
* `-r <n>` gives `n` number of random Wikipedia articles.
  * example: `!wb -r 5`
* `-p <channel name> [<nick>]` WikiParty! Move channels and invite others.
  * example: `!wb -p party reece john james`
* `-name <name>` renames the bot.
  * example: `!wb -name NewName`
* `-q` quit. Make the bot exit the server and close.
  * example: `!wb -q`
  
## Environment variables file
The bot needs a set of Wikipedia credentials (username and password) in order to function. These need to be 
provided in the form of a `.env` file in the project's root. An example file, `.env.example`, has been given with some 
example credentials inside. The file is just an `=` delimited set of key/value pairs. The values are equal to whatever 
your Wikipedia credentials are, but `WIKIUSERNAME` and `WIKIPASS` need to be present as keys.
Remember that the required file's name is `.env`, not `.env.example`.

## Building from source
**Building from source requires Maven to be installed.**
1. Clone this repository.
2. Rename `.env.example` to `.env` and change the values inside it to your Wikipedia credentials.
3. In the project directory, execute `mvn package`. The `.jar` file will be built in the `./target` directory.
4. Execute the `.jar` file using aforementioned arguments: `java -jar WikiIRC-Bot-1.0-SNAPSHOT-jar-with-dependencies 127.0.0.1 6667 help`

**Please note that in order to make sure the bot picks up the `.env` file correctly, make sure that the shell's working 
directory when executing the `.jar` file is root of the project (i.e. the same level as `.env`.**

If you want to run without building manually, opening as a Maven project within any modern Java IDE and running 
`dev.reecemercer.Bot` with the same argument set will also do the trick.

## External libraries
WikiBot makes use of [jwiki](https://github.com/fastily/jwiki) (released under the **GNU General Public License v3.0**) as a wrapper for the WikiMedia API. This is listed in 
`pom.xml` as a Maven build dependency.