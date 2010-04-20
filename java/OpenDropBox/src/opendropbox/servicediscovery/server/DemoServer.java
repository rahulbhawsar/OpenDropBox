/*
 * Copyright 2010 OpenDropBox
 * http://www.opendropbox.com/
 */

package opendropbox.servicediscovery.server;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import opendropbox.servicediscovery.ServiceConstants;
import opendropbox.servicediscovery.client.ServiceListManager;

/**
 * 
 *
 * @author Walter
 */
public class DemoServer {

    public static void main(String[] args) {

        ServerSocket serverSocket = null;
        String serviceInstanceName = "ODB-B";

        // before we start this service, we must make sure that no service with this name
        // is already running
        ServiceListManager manager = new ServiceListManager(ServiceConstants.SERVICE_NAME);
        manager.refresh(2000);
        if (manager.containsInstance(serviceInstanceName))
        {
            System.err.println("An instance with the same name is already running on the local network.");
            System.exit(0);
        }

        try {

            // create a new server socket on an empty port
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(InetAddress.getLocalHost(), 0));

            // create a new service responder to respond to queries about services running on this machine
            new ServiceResponder(ServiceConstants.SERVICE_NAME, serviceInstanceName, serverSocket.getInetAddress(), serverSocket.getLocalPort()).startResponder();

        } catch (IOException e) {
            System.err.println("Could not bind a server socket to a free port: " + e);
            System.exit(1);
        }


        // create a simple server to respond with the current time
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                System.out.println("Connection from: " + socket.getInetAddress());
                OutputStreamWriter writer = new OutputStreamWriter(socket.getOutputStream());
                writer.write(new Date().toString() + "\r\n");
                writer.flush();
                socket.close();
            } catch (IOException ie) {
                System.err.println("Exception: " + ie);
            }
        }
    }
}
