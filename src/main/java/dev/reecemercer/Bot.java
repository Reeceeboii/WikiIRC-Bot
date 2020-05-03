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
    private ServerManager serverManager;
    private HashMap<String, String> env;
    private String nick;
    private static final String ALIAS = "!wb";
    private final String CHANNEL;
    private static final String WIKI_PREFIX = "https://en.wikipedia.org/wiki/";
    ArticleLogger logger;
    private Wiki wiki;
    private boolean alive = true;

    /**
     * Create a new instance of the IRC bot
     * @param addr The remote address of the IRC server to join
     * @param port The port that the IRC server is running on
     * @param channel The channel to join (with or without the '#')
     * @throws IOException Any errors that occur during the process of joining the server
     */
    Bot(InetAddress addr, int port, String channel) throws IOException {
        CHANNEL = channel;
        nick = "WikiBot";

        // load environment variables
        env = loadEnv();

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
        String server_res = null;
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
                    ArrayList<String> cmd = new ArrayList<>(Arrays.asList(server_res.substring(server_res.indexOf(CHANNEL) + CHANNEL.length() + 2).split(" ")));
                    cmd.removeIf(token -> token.length() == 0);
                    System.out.println("Parsed args: " + cmd);

                    switch (cmd.size()){
                        case 2:
                            // one random Wikipedia article
                            if(cmd.get(1).equals("-r")){
                                // generate a random article, log it and send it back to the channel
                                final String URL = wikiURLEncode(wiki.getRandomPages(1, NS.MAIN).get(0));
                                logger.log(URL);
                                serverManager.writeToChannel(URL, CHANNEL);
                                break;
                            }

                            // quitting the server
                            if (cmd.get(1).equals("-q")) {
                                serverManager.quit("--------------- WikiBot says goodbye! ---------------");
                                alive = !alive;
                                break;
                            }
                            writeHelp();
                            break;
                        case 3:
                            // n number of random articles (<= 15)
                            if(cmd.get(1).equals("-r")) {
                                try {
                                    final int n = Integer.parseInt(cmd.get(2));
                                    if (n > 15) {
                                        serverManager.writeToChannel("No more than 15 articles at once please!", CHANNEL);
                                        break;
                                    }
                                    ArrayList<String> articles = wiki.getRandomPages(n, NS.MAIN);
                                    for (String article : articles) {
                                        article = wikiURLEncode(article);
                                        serverManager.writeToChannel(article, CHANNEL);
                                        logger.log(article);
                                    }
                                    break;
                                } catch (Exception e) {
                                    writeHelp();
                                    break;
                                }
                            }

                            // changing the name of the bot
                            if (cmd.get(1).equals("-name")){
                                if(cmd.get(2).length() > 0 && cmd.get(2).length() <= 9){
                                    serverManager.rename(cmd.get(2));
                                    break;
                                }
                            }
                            break;
                        default:
                            writeHelp();
                            break;
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
        serverManager.writeToChannel("------------------- WikiBot help -------------------", CHANNEL);
        serverManager.writeToChannel("| Usage...                                         |", CHANNEL);
        serverManager.writeToChannel("|                                                  |", CHANNEL);
        serverManager.writeToChannel("| • 1 Random article: !wb -r                       |", CHANNEL);
        serverManager.writeToChannel("| • Get n random articles: !wb -r <n>              |", CHANNEL);
        serverManager.writeToChannel("| • WikiParty: !wb -p [<nick>]                     |", CHANNEL);
        serverManager.writeToChannel("| • Rename me: !wb -name <name>                    |", CHANNEL);
        serverManager.writeToChannel("| • Quit WikiBot: !wb -q                           |", CHANNEL);
        serverManager.writeToChannel("----------------------------------------------------", CHANNEL);
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
     * @throws FileNotFoundException If there is no '.env' file to load
     */
    private HashMap<String, String> loadEnv() throws FileNotFoundException {
        HashMap<String, String> envMap = new HashMap<>();
        final File envFile = new File(".env");
        Scanner reader = new Scanner(envFile);
        while(reader.hasNextLine()){
            String data = reader.nextLine();
            String[] split = data.split("=");
            envMap.put(split[0], split[1]);
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
                final String CHANNEL = args[2].charAt(0) == '#' ? args[2] : "#" + args[2];
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
