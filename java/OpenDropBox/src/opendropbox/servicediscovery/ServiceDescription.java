/*
 * Copyright 2010 OpenDropBox
 * http://www.opendropbox.com/
 */

package opendropbox.servicediscovery;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;

/**
 *
 * @author Walter
 */
public class ServiceDescription {

    private String _serviceInstanceName;
    private int _port;
    private InetAddress _address;

    public ServiceDescription() {
    }

    public InetAddress getAddress() {
        return _address;
    }

    public void setAddress(InetAddress serviceAddress) {
        _address = serviceAddress;
    }

    protected String getAddressAsString() {
        return getAddress().getHostAddress();
    }

    public String getInstanceName() {
        return _serviceInstanceName;
    }

    public void setInstanceName(String serviceDescription) {
        _serviceInstanceName = serviceDescription;
    }

    protected String getEncodedInstanceName() {
        try {
            return URLEncoder.encode(this.getInstanceName(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public int getPort() {
        return _port;
    }

    public void setPort(int servicePort) {
        _port = servicePort;
    }

    protected String getPortAsString() {
        return Integer.toString(this.getPort());
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(getEncodedInstanceName());
        buf.append(" ");
        buf.append(getAddressAsString());
        buf.append(" ");
        buf.append(getPortAsString());
        return buf.toString();
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ServiceDescription)) {
            return false;
        }
        ServiceDescription descriptor = (ServiceDescription) o;
        return descriptor.getInstanceName().equals(getInstanceName());
    }

    public int hashCode() {
        return getInstanceName().hashCode();
    }

    public int compareTo(ServiceDescription sd) throws ClassCastException {
        if (sd == null) {
            throw new NullPointerException();
        }
        if (sd == this) {
            return 0;
        }

        return getInstanceName().compareTo(sd.getInstanceName());
    }

    public static ServiceDescription parse(String encodedInstanceName,
            String addressAsString, String portAsString) {

        ServiceDescription descriptor = new ServiceDescription();
        try {
            String name = URLDecoder.decode(encodedInstanceName, "UTF-8");
            if (name == null || name.length() == 0) {
                /* warning: check API docs for exact behavior of 'decode' */
                return null;
            }
            descriptor.setInstanceName(name);
        } catch (UnsupportedEncodingException e) {
            System.err.println("Unexpected exception: " + e);
            e.printStackTrace();
            return null;
        }

        try {
            InetAddress addr = InetAddress.getByName(addressAsString);
            descriptor.setAddress(addr);
        } catch (UnknownHostException e) {
            System.err.println("Unexpected exception: " + e);
            e.printStackTrace();
            return null;
        }

        try {
            int p = Integer.parseInt(portAsString);
            descriptor.setPort(p);
        } catch (NumberFormatException e) {
            System.err.println("Unexpected exception: " + e);
            e.printStackTrace();
            return null;
        }

        return descriptor;
    }
}
