package dev.reecemercer.network;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

/**
 * Holds details about and manages interactions with the currently connected IRC server
 */
public class ServerManager {
    private static final String END_MSG = "\r\n";
    private final BufferedWriter writer;
    private final BufferedReader reader;

    /**
     * Create a new ServerManager instance
     * @param serverIP The IP address of the server to connect the bot to
     * @param port The port of the server to connect the bot to
     * @throws IOException Errors raised when opening the socket connection (i.e. any timeouts)
     */
    public ServerManager(InetAddress serverIP, int port) throws IOException {
        InetSocketAddress addr = new InetSocketAddress(serverIP, port);
        Socket IRCSock = new Socket();
        IRCSock.connect(addr);
        writer = new BufferedWriter(new OutputStreamWriter(IRCSock.getOutputStream()));
        reader = new BufferedReader(new InputStreamReader(IRCSock.getInputStream()));
    }

    /**
     * Initiate a connection to the IRC server
     * @param nick The nickname to connect with
     * @param channel The channel to connect to
     * @throws IOException An error writing to the socket
     */
    public void connect(String nick, String channel) throws IOException {
        writer.write("NICK " + nick + END_MSG);
        writer.write("USER " + nick + " cityirc * :" + nick + END_MSG);
        writer.flush();
        writer.write("JOIN " + channel + END_MSG);
        writer.flush();
    }

    /**
     * Reads a line from the BufferedReader linked to the socket
     * @return A new line read from the socket's BufferedReader
     * @throws IOException Any errors reading the line
     */
    public String readline() throws IOException {
        return reader.readLine();
    }

    /**
     * Send a pong back to server in response to a ping message
     * @param server The name of the server to send a ping to
     * @throws IOException Any errors writing to the server
     */
    public void pong(String server) throws IOException {
        writer.write("PONG " + server + END_MSG);
        writer.flush();
        System.out.println("Bot received a ping and replied");
    }

    /**
     * Write a message to a channel
     * @param msg The message to send
     * @param channel The channel to send it to
     * @throws IOException Any errors thrown during socket write
     */
    public void writeToChannel(String msg, String channel) throws IOException {
        String message = "PRIVMSG " + channel + " :" + msg;
        System.out.println("Sending: " + message);
        writer.write(message + END_MSG);
        writer.flush();
    }

    /**
     * Make the bot quit the server
     * @param msg The exit message to display
     * @throws IOException Any errors thrown during socket write
     */
    public void quit(String msg) throws IOException {
        // quit server and close socket
        writer.write("QUIT :" + msg + END_MSG);
        writer.flush();
    }

    /**
     * Used to allow the bot to rename itself
     * @param newNick The new nickname to update to
     * @throws IOException Any errors that occur when writing to the socket
     */
    public void rename(String newNick) throws IOException {
        writer.write("NICK " + newNick + END_MSG);
        writer.flush();
    }

    /**
     * Asks the server to move the bot to another channel
     * @param newChannel The channel name to move to
     * @throws IOException Any errors thrown when writing the JOIN message to the socket
     */
    public void moveToChannel(String newChannel) throws IOException {
        writer.write("JOIN " + newChannel + END_MSG);
        writer.flush();
    }

    /**
     * Makes the bot leave a channel its a member of
     * @param oldChannel The name of the channel to leave
     * @throws IOException Any errors thrown when writing the PART message to the socket
     */
    public void leaveChannel(String oldChannel) throws IOException {
        writer.write("PART " + oldChannel + END_MSG);
        writer.flush();
    }

    /**
     * Invite users to a channel
     * @param nicks A list of nicks to use to send out invite messages
     * @param channel The channel to invite them to
     * @throws IOException Any errors thrown when writing the invite messages to the socket
     */
    public void invite(List<String> nicks, String channel) throws IOException {
        for(String nick: nicks){
            writer.write("INVITE " + nick + " " + channel + END_MSG);
        }
        writer.flush();
    }
}
