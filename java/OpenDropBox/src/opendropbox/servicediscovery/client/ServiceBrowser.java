/*
 * Copyright 2010 OpenDropBox
 * http://www.opendropbox.com/
 */
package opendropbox.servicediscovery.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import opendropbox.servicediscovery.ServiceConstants;
import opendropbox.servicediscovery.ServiceDescription;

/**
 *
 * @author Walter
 */
public class ServiceBrowser implements Runnable {

    protected static InetAddress _multicastAddressGroup;
    protected static int _multicastPort;
    protected String _serviceName;
    protected boolean _shouldRun = true;
    protected MulticastSocket _socket;
    protected DatagramPacket _queuedPacket;
    protected DatagramPacket _receivedPacket;
    protected Vector<ServiceBrowserListener> _listeners;
    protected Thread _thread;
    protected Timer _timer;

    static {
        try {
            _multicastAddressGroup = InetAddress.getByName(ServiceConstants.MULTICAST_ADDRESS_GROUP);
            _multicastPort = ServiceConstants.MULTICAST_PORT;
        } catch (UnknownHostException uhe) {
            System.err.println("Unexpected exception: " + uhe);
            uhe.printStackTrace();
            System.exit(1);
        }
    }

    public ServiceBrowser() {

        try {
            _socket = new MulticastSocket(_multicastPort);
            _socket.joinGroup(_multicastAddressGroup);
            _socket.setSoTimeout(ServiceConstants.BROWSER_SOCKET_TIMEOUT);

        } catch (IOException ioe) {
            System.err.println("Unexpected exception: " + ioe);
            ioe.printStackTrace();
            System.exit(1);
        }


        _listeners = new Vector<ServiceBrowserListener>();
    }

    public ServiceBrowser(ServiceBrowserListener listener, String serviceName) {
        this();
        this.addServiceBrowserListener(listener);
        this.setServiceName(serviceName);
    }

    public void run() {


        while (_shouldRun) {


            /* listen (briefly) for a reply packet */
            try {
                byte[] buf = new byte[ServiceConstants.DATAGRAM_LENGTH];
                _receivedPacket = new DatagramPacket(buf, buf.length);
                _socket.receive(_receivedPacket); // note timeout in effect


                if (isReplyPacket()) {

                    ServiceDescription descriptor;

                    /* notes on behavior of descriptors.indexOf(...)
                     * ServiceDescriptor objects check for 'equals()'
                     * based only on the instanceName field. An update
                     * to a descriptor implies we should replace an
                     * entry if we already have one. (Instead of bothing
                     * with the details to determine new vs. update, just
                     * quickly replace any current descriptor.)
                     */

                    descriptor = getReplyDescriptor();
                    if (descriptor != null) {
                        notifyReply(descriptor);
                        _receivedPacket = null;
                    }

                }

            } catch (SocketTimeoutException ste) {
                /* ignored; this exception is by design to
                 * break the blocking from socket.receive */
            } catch (IOException ioe) {
                System.err.println("Unexpected exception: " + ioe);
                ioe.printStackTrace();
                /* resume operation */
            }

            sendQueuedPacket();

        }
    }

    protected void sendQueuedPacket() {
        if (_queuedPacket == null) {
            return;
        }
        try {
            _socket.send(_queuedPacket);
            _queuedPacket = null;
        } catch (IOException ioe) {
            System.err.println("Unexpected exception: " + ioe);
            ioe.printStackTrace();
            /* resume operation */
        }
    }

    protected boolean isReplyPacket() {
        if (_receivedPacket == null) {
            return false;
        }

        String dataStr = new String(_receivedPacket.getData());
        int pos = dataStr.indexOf((char) 0);
        if (pos > -1) {
            dataStr = dataStr.substring(0, pos);
        }

        /* REQUIRED TOKEN TO START */
        if (dataStr.startsWith("SERVICE REPLY " + getEncodedServiceName())) {
            return true;
        }

        return false;
    }

    protected ServiceDescription getReplyDescriptor() {
        String dataStr = new String(_receivedPacket.getData());
        int pos = dataStr.indexOf((char) 0);
        if (pos > -1) {
            dataStr = dataStr.substring(0, pos);
        }

        // try to get this substring -- if it fails, return null
        try {
            StringTokenizer tokens = new StringTokenizer(dataStr.substring(15 + getEncodedServiceName().length()));

            if (tokens.countTokens() == 3) {
                return ServiceDescription.parse(tokens.nextToken(),
                        tokens.nextToken(), tokens.nextToken());
            } else {
                return null;
            }
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    protected DatagramPacket getQueryPacket() {
        StringBuffer buf = new StringBuffer();
        buf.append("SERVICE QUERY " + getEncodedServiceName());

        byte[] bytes = buf.toString().getBytes();
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
        packet.setAddress(_multicastAddressGroup);
        packet.setPort(_multicastPort);

        return packet;
    }

    public String getServiceName() {
        return _serviceName;
    }

    protected String getEncodedServiceName() {
        try {
            return URLEncoder.encode(getServiceName(), "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            return null;
        }
    }

    public void setServiceName(String serviceName) {
        this._serviceName = serviceName;
    }

    public void addServiceBrowserListener(ServiceBrowserListener l) {
        if (!_listeners.contains(l)) {
            _listeners.add(l);
        }
    }

    public void removeServiceBrowserListener(ServiceBrowserListener l) {
        _listeners.remove(l);
    }

    public void startLookup() {
        if (_timer == null) {
            _timer = new Timer("QueryTimer");
            _timer.scheduleAtFixedRate(new QueryTimerTask(), 0L, ServiceConstants.BROWSER_QUERY_INTERVAL);
        }
    }

    public void startSingleLookup() {
        if (_timer == null) {
            _timer = new Timer("QueryTimer");
            _timer.schedule(new QueryTimerTask(), 0L);
            _timer = null;
        }
    }

    public void stopLookup() {
        if (_timer != null) {
            _timer.cancel();
            _timer = null;
        }
    }

    protected void notifyReply(ServiceDescription descriptor) {
        for (ServiceBrowserListener l : _listeners) {
            l.serviceEncountered(descriptor);
        }
    }

    public void startListener() {
        if (_thread == null) {
            _shouldRun = true;
            _thread = new Thread(this, "ServiceBrowser");
            _thread.start();
        }
    }

    public void stopListener() {
        if (_thread != null) {
            _shouldRun = false;
            _thread.interrupt();
            _thread = null;
        }
    }

    private class QueryTimerTask extends TimerTask {

        public void run() {
            DatagramPacket packet = getQueryPacket();
            if (packet != null) {
                _queuedPacket = packet;
            }
        }
    }
}
