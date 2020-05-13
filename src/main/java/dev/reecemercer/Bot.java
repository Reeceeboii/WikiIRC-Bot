package dev.reecemercer;

import dev.reecemercer.logging.ArticleLogger;
import dev.reecemercer.network.NetUtils;
import dev.reecemercer.network.ServerManager;

// library to allow easy interfacing with MediaWiki APIs without fiddling with the network stuff myself
import org.fastily.jwiki.core.NS;
import org.fastily.jwiki.core.Wiki;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Main bot driver class
 */
public class Bot {
    private final ServerManager serverManager;
    private String nick;
    private static final String ALIAS = "!wb";
    private String channel;
    private static final String WIKI_PREFIX = "https://en.wikipedia.org/wiki/";
    ArticleLogger logger;
    private final Wiki wiki;
    private boolean alive = true;

    /**
     * Create a new instance of the IRC bot
     * @param addr The remote address of the IRC server to join
     * @param port The port that the IRC server is running on
     * @param channel The channel to join (with or without the '#')
     * @throws IOException Any errors that occur during the process of joining the server
     */
    Bot(InetAddress addr, int port, String channel) throws IOException {
        this.channel = channel;
        nick = "WikiBot";

        // load environment variables
        final HashMap<String, String> env = loadEnv();

        // create a new wiki instance
        wiki = new Wiki.Builder().build();
        wiki.login(env.get("WIKIUSERNAME"), env.get("WIKIPASS"));

        // create a server manager instance and connect to the server
        serverManager = new ServerManager(addr, port);
        serverManager.connect(nick, channel);

        // create a new ArticleLogger instance so we can log previously created article links
        logger = new ArticleLogger("./res/prev_article_log.txt");

        mainLoop();
    }

    /*
        city message: 	:reece!~reece@bastion0.vmware-dc.city.ac.uk PRIVMSG #help :!wb -r
	                    :BenFrost!~BenFrost@trowbridge.unix1.city.ac.uk PRIVMSG #help :!wb -r 15

     */
    private void mainLoop() throws IOException {
        String server_res;
        //ArrayList<String> articles = wiki.getRandomPages(5, NS.MAIN);
        //articles.forEach(article -> System.out.println(WIKI_PREFIX + article.replace(' ', '_')));
        while(alive){
            while((server_res = serverManager.readline()) != null){
                System.out.println("\t" + server_res);

                // look out for PINGs from the server
                if(server_res.toLowerCase().startsWith("ping")){
                    serverManager.pong(server_res.substring(5));
                }

                // if the bot is being spoken to
                if (server_res.toLowerCase().contains(ALIAS) && server_res.toLowerCase().contains("privmsg")) {
                    // load message into list and remove empty args
                    ArrayList<String> cmd = new ArrayList<>(Arrays.asList(server_res.substring(server_res.indexOf(channel) + channel.length() + 2).split(" ")));
                    cmd.removeIf(token -> token.length() == 0);
                    System.out.println("Parsed args: " + cmd);

                    // process commands based on num of args given in the message to the bot
                    //      number of arguments includes '!wb' itself. I.e. '!wb -r' counts as two args
                    // let's start with processing commands that contain 2 arguments
                    if(cmd.size() == 2){
                        if(cmd.get(1).equals("-r")){
                            // generate a random article, log it and send it back to the channel
                            final String URL = wikiURLEncode(wiki.getRandomPages(1, NS.MAIN).get(0));
                            logger.log(URL);
                            serverManager.writeToChannel(URL, channel);
                        } else if (cmd.get(1).equals("-q")) { // quitting the server
                            serverManager.quit("WikiBot says goodbye!");
                            System.out.println("Bot is exiting");
                            // close the logger's file writing handle to make sure everything is writer
                            logger.closeFileHandle();
                            alive = !alive;
                        } else {
                            writeHelp();
                        }
                    // 3 arguments
                    } else if (cmd.size() == 3){
                        // n number of random articles (<= 15)
                        if(cmd.get(1).equals("-r")) {
                            try {
                                final int n = Integer.parseInt(cmd.get(2));
                                if (n > 15) {
                                    serverManager.writeToChannel("No more than 15 articles at once please!", channel);
                                }
                                // generate n random articles, URL encode them, send them, log them. Sorted.
                                ArrayList<String> articles = wiki.getRandomPages(n, NS.MAIN);
                                for (String article : articles) {
                                    article = wikiURLEncode(article);
                                    serverManager.writeToChannel(article, channel);
                                    logger.log(article);
                                }
                            } catch (Exception e) {
                                writeHelp();
                            }
                        } else if (cmd.get(1).equals("-name")){ // changing the name of the bot
                            if(cmd.get(2).length() > 0 && cmd.get(2).length() <= 9){
                                serverManager.writeToChannel("Sure! Renaming myself to ".concat(cmd.get(2)), channel);
                                nick = cmd.get(2);
                                serverManager.rename(cmd.get(2));
                            }
                        } else {
                            writeHelp();
                        }
                    // 4 or more arguments
                    } else if (cmd.size() >= 4) {
                        if(cmd.get(1).equals("-p")) {
                            final String oldChannel = channel;
                            // updated local channel copy to the one given in the argument by the user
                            channel = cmd.get(2).startsWith("#") ? cmd.get(2) : "#".concat(cmd.get(2));
                            serverManager.writeToChannel("Moving to ".concat(channel).concat("!"), oldChannel);
                            serverManager.moveToChannel(channel);
                            if(cmd.size() > 4) {
                                serverManager.invite(cmd.subList(3, cmd.size()), channel);
                            } else {
                                List<String> nicks = Collections.singletonList(cmd.get(3));
                                serverManager.invite(nicks, channel);                            }
                            serverManager.leaveChannel(oldChannel);
                        } else {
                            writeHelp();
                        }
                    } else {
                        writeHelp();
                    }
                }
            }
        }
    }

    /**
     * Output the bot's help message
     * @throws IOException An error occurring during the socket write
     */
    private void writeHelp() throws IOException {
        serverManager.writeToChannel("------------------- WikiBot help -------------------", channel);
        serverManager.writeToChannel("| Usage...                                         |", channel);
        serverManager.writeToChannel("|                                                  |", channel);
        serverManager.writeToChannel("| • 1 Random article: !wb -r                       |", channel);
        serverManager.writeToChannel("| • Get n random articles: !wb -r <n>              |", channel);
        serverManager.writeToChannel("| • WikiParty: !wb -p <channel name> [<nick>]      |", channel);
        serverManager.writeToChannel("| • Rename me: !wb -name <name>                    |", channel);
        serverManager.writeToChannel("| • Quit WikiBot: !wb -q                           |", channel);
        serverManager.writeToChannel("----------------------------------------------------", channel);
    }

    /**
     * Replace article name spaces with underscores and then append to Wikipedia domain to create a URL
     * @param articleName The name of the article to encode into a URL
     * @return A URL linking to the article name's corresponding Wikipedia page
     */
    public static String wikiURLEncode(String articleName){
        return WIKI_PREFIX.concat(articleName.replace(" ", "_"));
    }

    /**
     * Load the bot's environment variables
     * @return A hashmap in the form <Variable name, variable value>
     */
    private HashMap<String, String> loadEnv() {
        HashMap<String, String> envMap = new HashMap<>();
        try {
            final File envFile = new File(".env");
            Scanner reader = new Scanner(envFile);
            while (reader.hasNextLine()) {
                String data = reader.nextLine();
                String[] split = data.split("=");
                envMap.put(split[0], split[1]);
            }
            // make sure Wikipedia credentials are given in the .env file
            if(!(envMap.containsKey("WIKIUSERNAME")) || !(envMap.containsKey("WIKIPASS"))){
                System.err.println(".env file error - Make sure WIKIUSERNAME and WIKIPASS values are provided. See README for details.");
                System.exit(1);
            }
            return envMap;
        } catch (FileNotFoundException e){
            System.err.println(".env file needs to be provided");
            System.exit(1);
        }
        return envMap;
    }

    /**
     * Main method to kick off the program
     * @param args Command line arguments that contain server, port and channel information for the bot
     * @throws IOException Errors thrown when testing for server connections
     */
    public static void main(String[] args) throws IOException {
        if(args.length != 3) {
            System.err.println("Please provide at least 3 arguments - server IP, server port and channel (with or without #)");
            System.err.println("USAGE: Bot <ip> <port> <channel>");
            System.err.println("i.e: Bot 123.123.123.123 6667 #chan");
            System.exit(1);
        } else {
            try {
                // create new IP and port combination to test if server is reachable
                final InetAddress IP = InetAddress.getByName(args[0].toLowerCase().equals("localhost") ? "127.0.0.1" : args[0]);
                // parse channel arg and add # if needed
                final String CHANNEL = args[2].charAt(0) == '#' ? args[2] : "#".concat(args[2]);
                final int PORT = Integer.parseInt(args[1]);

                // is there actually a server at IP:PORT?
                if(!NetUtils.isHostAlive(IP, PORT)){
                    System.err.println(IP + ":" + PORT + " is not reachable");
                    System.exit(1);
                } else {
                    // create a new bot instance using the host and port
                    new Bot(IP, PORT, CHANNEL);
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
    }
}
