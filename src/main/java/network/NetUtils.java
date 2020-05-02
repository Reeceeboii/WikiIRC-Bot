package network;

import java.io.IOException;
import java.net.*;

public class NetUtils {
    /**
     * Given an IP address and a port, check if there is actually anything alive and running there
     * @param addr The IP address of the remote host
     * @param port The port to attempt to open a socket with
     * @return A true or false value denoting if IP:Port is reachable or not
     */
    public static boolean isHostAlive(InetAddress addr, int port) throws IOException {
        System.out.print("\tChecking if " + addr + ":" + port + " is reachable... ");

        Socket s = new Socket();
        final SocketAddress sockAddr = new InetSocketAddress(addr, port);
        try {
            s.connect(sockAddr, 1500); // if it times out after 1.5 secs it's basically dead
            System.out.println("yes!");
            s.close();
            return true;
        } catch  (IOException e) {
            return false;
        }
    }
}
