import network.NetUtils;
import network.ServerManager;

import org.fastily.jwiki.core.Wiki;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

public class Bot {
    private ServerManager serverManager;
    private HashMap<String, String> env;
    private static final String NICK = "WikiBot";
    private static final String ALIAS = "wb";
    private final String CHANNEL;
    private static final String WIKI_PREFIX = "https://en.wikipedia.org/wiki/";
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
        // load environment variables
        env = loadEnv();
        // create a new wiki instance
        wiki = new Wiki.Builder().build();
        wiki.login(env.get("WIKIUSERNAME"), env.get("WIKIPASS"));
        // create a server manager instance and connect to the server
        serverManager = new ServerManager(addr, port);
        serverManager.connect(NICK, channel);
        mainLoop();
    }

    private void mainLoop() throws IOException {
        String server_res = null;
        //ArrayList<String> articles = wiki.getRandomPages(5, NS.MAIN);
        //articles.forEach(article -> System.out.println(WIKI_PREFIX + article.replace(' ', '_')));
        while(alive){
            while((server_res = serverManager.readline()) != null){
                System.out.println("\t" + server_res);
                if(server_res.toLowerCase().startsWith("ping")){
                    serverManager.pong(server_res.substring(5));
                }
                if (server_res.toLowerCase().contains("wb")) {
                    try {
                        final String[] parsed = server_res.substring(server_res.indexOf(ALIAS) + ALIAS.length() + 1).split(" ");
                        System.out.println("Parsed args: " + Arrays.toString(parsed));
                    } catch (StringIndexOutOfBoundsException e){
                        System.err.println("I was mentioned, but no args were given");
                        break;
                    }

                }
            }
        }
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

    public static void main(String[] args) throws IOException {
        if(args.length != 3) {
            System.err.println("Please provide at least 3 arguments - the IRC server's IP and port and a channel name");
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
