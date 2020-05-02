package network;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ServerManager {
    private static final String END_MSG = "\r\n";
    private Socket IRCSock;
    private final InetAddress serverIP;
    private InetSocketAddress addr;
    private final int port;
    private BufferedWriter writer;
    private BufferedReader reader;

    public ServerManager(InetAddress serverIP, int port) throws IOException {
        this.serverIP = serverIP;
        this.port = port;
        addr = new InetSocketAddress(serverIP, port);
        IRCSock = new Socket();
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
     * Reads a line from the reader linked to the socket
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
    }

    public BufferedWriter getWriter(){
        return writer;
    }

}
