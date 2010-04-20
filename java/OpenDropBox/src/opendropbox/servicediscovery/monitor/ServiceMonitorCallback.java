/*
 * Copyright 2010 OpenDropBox
 * http://www.opendropbox.com/
 */
package opendropbox.servicediscovery.monitor;

import opendropbox.servicediscovery.ServiceDescription;

/**
 *
 * @author Walter
 */
public interface ServiceMonitorCallback {

    public void serviceJoined(ServiceDescription serviceDescription);
    public void serviceDeparted(ServiceDescription serviceDescription);

}
