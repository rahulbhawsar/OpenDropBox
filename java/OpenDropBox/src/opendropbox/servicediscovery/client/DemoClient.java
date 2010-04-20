/*
 * Copyright 2010 OpenDropBox
 * http://www.opendropbox.com/
 */

package opendropbox.servicediscovery.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Vector;
import opendropbox.servicediscovery.ServiceDescription;

/**
 *
 * @author Walter
 */
public class DemoClient {

    public static void main(String[] args) {
        new DemoClient();
    }
    ServiceBrowser browser;

    public DemoClient() {

        ServiceListManager manager = new ServiceListManager("ODB v0.1");

        manager.refresh(2000);

        DemoRefreshListener listener = new DemoRefreshListener();

        System.out.println("begin");
        manager.refresh(2000, listener);
        System.out.println("end");
        
        Vector<ServiceDescription> descriptors = manager.getServiceList();

        if (descriptors.size() > 0) {
            System.out.println("\n---DEMO SERVERS---");
            for (ServiceDescription descriptor : descriptors) {
                System.out.println(descriptor.toString());
            }

            System.out.println("\n---FIRST SERVER'S TIME IS---");
            ServiceDescription descriptor = descriptors.get(0);
            try {
                Socket socket = new Socket(descriptor.getAddress(), descriptor.getPort());
                InputStreamReader reader = new InputStreamReader(socket.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(reader);
                String line = bufferedReader.readLine();
                System.out.println(line);
                socket.close();
            } catch (IOException ie) {
                System.err.println("Exception: " + ie);
                System.exit(1);
            }
        } else {
            System.out.println("\n---NO DEMO SERVERS FOUND---");
        }

        System.out.println("\nThat's all folks.");

        try {
        Thread.sleep(5000);
        } catch (Exception e) {}

        System.exit(0);
    }

}
