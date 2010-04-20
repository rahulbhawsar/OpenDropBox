/*
 * Copyright 2010 OpenDropBox
 * http://www.opendropbox.com/
 */

package opendropbox.servicediscovery.client;

import opendropbox.servicediscovery.ServiceDescription;

/**
 *
 * @author Walter
 */
public interface ServiceBrowserListener {

    /**
     * This method will be called whenever a ServiceDescription is
     * encountered on the local network.  Please note that these
     * values are not guaranteeed to be unique.  Your implementation
     * should provide some method for dealing with uniqueness if
     * it is required for your application.
     * 
     * @param description - the encountered ServiceDescription
     */
    public abstract void serviceEncountered(ServiceDescription description);
}
