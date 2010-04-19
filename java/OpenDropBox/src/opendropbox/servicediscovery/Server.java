/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package opendropbox.servicediscovery;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

/**
 *
 * @author Walter
 */
public class Server {

    public static final String SERVICE_NAME = "odp 0.1";
    public static final String INSTANCE_NAME = "odp server";

    public static void main(String[] args) {

        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(InetAddress.getLocalHost(), 0));
        }
        catch (IOException e) {
            System.err.println("Could not bind a server socket to free port.");
            System.exit(1);
        }

        // create a description for this service
    }
}
