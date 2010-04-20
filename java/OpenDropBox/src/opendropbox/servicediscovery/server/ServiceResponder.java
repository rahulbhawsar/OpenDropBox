/*
 * Copyright 2010 OpenDropBox
 * http://www.opendropbox.com/
 */
package opendropbox.servicediscovery.server;

import opendropbox.servicediscovery.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.net.UnknownHostException;

/**
 *
 * @author Walter
 */
public class ServiceResponder implements Runnable {

    protected static InetAddress _multicastAddressGroup;
    protected static int _multicastPort;
    protected String _serviceName;
    protected ServiceDescription _description;
    protected boolean _shouldRun = true;
    protected MulticastSocket _socket;
    protected DatagramPacket _queuedPacket;
    protected DatagramPacket _receivedPacket;
    protected Thread _thread;


    static {
        try {
            _multicastAddressGroup = InetAddress.getByName(ServiceConstants.MULTICAST_ADDRESS_GROUP);
            _multicastPort = ServiceConstants.MULTICAST_PORT;
        } catch (UnknownHostException e) {
            System.err.println("Unexpected exception: " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Creates a new ServiceResponder with the given service
     * name.
     * 
     * @param serviceName - the name of the service for which
     * responses will be sent.
     */
    public ServiceResponder(String serviceName) {
        this._serviceName = serviceName;
        try {
            _socket = new MulticastSocket(_multicastPort);
            _socket.joinGroup(_multicastAddressGroup);
            _socket.setSoTimeout(ServiceConstants.RESPONDER_SOCKET_TIMEOUT);

        } catch (IOException ioe) {
            System.err.println("Unexpected exception: " + ioe);
            ioe.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Creates a new ServiceResponder with the given name, instance name,
     * address, and port.  Useful for quickly getting a responder up
     * and running.
     * 
     * @param serviceName - the name of the service for which responses
     * will be sent
     * @param serviceInstanceName - the name of the instance running on
     * this server
     * @param address - the address of the server (generally this machine)
     * @param port - the port for remote clients to connect to (sent out in
     * the multicast response)
     */
    public ServiceResponder(String serviceName, String serviceInstanceName, InetAddress address, int port) {

        // create a new service responder
        this(serviceName);

        // create a new description, set the address, port, and instance name
        ServiceDescription description = new ServiceDescription();
        description.setAddress(address);
        description.setPort(port);
        description.setInstanceName(serviceInstanceName);

        // set the discription
        this.setDescription(description);

        // add a shutdown handler to terminate this thread if the JVM gets shut down
        this.addShutdownHandler();
    }

    /**
     * Return the ServiceDescription associated with this ServiceResponder.
     * 
     * @return - the ServiceDescription
     */
    public ServiceDescription getDescription() {
        return _description;
    }

    /**
     * Set the description for this service responder.
     * @param descriptor
     */
    public void setDescription(ServiceDescription description) {
        this._description = description;
    }

    /**
     * Returns the service name associated with this ServiceResponder.
     *
     * @return - the service name
     */
    public String getServiceName() {
        return _serviceName;
    }

    /**
     * Return the UTF-8 encoded service name. Used in sending
     * messages to the clients.
     *
     * @return - UTF-8 encoded string of the service name.
     */
    protected String getEncodedServiceName() {
        try {
            return URLEncoder.encode(getServiceName(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    /**
     * Set the service name.
     *
     * @param serviceName - the new service name
     */
    public void setServiceName(String serviceName) {
        this._serviceName = serviceName;
    }

    /**
     * Starts the responder.
     */
    public void startResponder() {
        if (_thread == null || !_thread.isAlive()) {
            _shouldRun = true;
            _thread = new Thread(this, "ServiceResponder");
            _thread.setDaemon(true);
            _thread.start();
        }
    }

    /**
     * Stops the responder.
     */
    public void stopResponder() {
        if (_thread != null && _thread.isAlive()) {
            _shouldRun = false;
            _thread.interrupt();
        }
    }

    /**
     * Send a queued packet.
     */
    protected void sendQueuedPacket() {
        if (_queuedPacket == null) {
            return;
        }
        try {
            _socket.send(_queuedPacket);
            _queuedPacket = null;
        } catch (IOException e) {
            System.err.println("Unexpected exception: " + e);
            e.printStackTrace();
        /* resume operation */
        }
    }

    /**
     * The run method of ServiceResponder which handles
     * receiving and sending messages.
     */
    public void run() {

        while (_shouldRun) {

            byte[] buf = new byte[ServiceConstants.DATAGRAM_LENGTH];
            _receivedPacket = new DatagramPacket(buf, buf.length);

            try {

                // this will throw an exception once the timeout is reached
                _socket.receive(_receivedPacket);

                // check to see if this packet was meant for this service responder
                if (isQueryPacket()) {
                    DatagramPacket replyPacket = getReplyPacket();
                    _queuedPacket = replyPacket;
                    sendQueuedPacket();
                }
            } catch (SocketTimeoutException e) {
                // if we reach a timeout, we just continue
            } catch (IOException e) {
                // if we run into an issue, we just continues
            }

        }
    }

    /**
     * Determine whether or not this packet is destined for
     * this ServiceResponder.
     * 
     * @return - true if it is, false otherwise
     */
    protected boolean isQueryPacket() {

        // if the packet is null, return false
        if (_receivedPacket == null)
            return false;
        
        // get the data from the incoming packet
        String dataString = new String(_receivedPacket.getData());

        // read until we get a null character
        int position = dataString.indexOf((char) 0);

        // if we have some data, cut it off at the null character
        if (position > -1) {
            dataString = dataString.substring(0, position);
        } // if we don't return false
        else
            return false;

        // if this matches the format and service name we have specified, return true
        if (dataString.startsWith("SERVICE QUERY " + getEncodedServiceName()))
            return true;

        // if all else fails, return false
        return false;
    }

    /**
     * Generates the reply packet based off of the values
     * set for this ServiceResponder.
     *
     * @return - a DatagramPacket containing the service name and a
     * description of this server.
     */
    protected DatagramPacket getReplyPacket() {

        // create an empty string buffer
        StringBuffer buf = new StringBuffer();

        // append the service name and description to this buffer
        buf.append("SERVICE REPLY " + getEncodedServiceName() + " ");
        buf.append(_description.toString());

        // convert this buffer to a byte array
        byte[] bytes = buf.toString().getBytes();

        // set the packet to contain these bytes
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length);

        // finally, set the address and port that we are communicating on
        packet.setAddress(_multicastAddressGroup);
        packet.setPort(_multicastPort);

        // return the packet
        return packet;
    }

    /**
     * This method will ensure that stopResponder() is called if the
     * JVM is shut down.  Not essential, but its nice.
     */
    public void addShutdownHandler() {

        // add a shutdown hook to this thread
        Runtime.getRuntime().addShutdownHook(new Thread() {

            // call stopResponder
            public void run() {
                stopResponder();
            }
        });
    }
}
