/*
 * Copyright 2010 OpenDropBox
 * http://www.opendropbox.com/
 */

package opendropbox.servicediscovery;

/**
 *
 * @author Walter
 */
public class ServiceConstants {

        // for testing
        public static final String SERVICE_NAME = "ODB v0.1";

        // specify constants for server and clients to communicate with
	public static final String MULTICAST_ADDRESS_GROUP = "230.0.0.1";
	public static final int MULTICAST_PORT = 4321;
	public static final int DATAGRAM_LENGTH = 1024;

	// set these for best performance on local network
	public static final int RESPONDER_SOCKET_TIMEOUT = 250;
	public static final int BROWSER_SOCKET_TIMEOUT = 250;
	public static final int BROWSER_QUERY_INTERVAL = 500;

        // specify constants for service monitor
        public static final int DEFAULT_POLLING_INTERVAL = 60000;
}
