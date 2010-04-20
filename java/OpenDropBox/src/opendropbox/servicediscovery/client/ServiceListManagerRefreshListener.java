/*
 * Copyright 2010 OpenDropBox
 * http://www.opendropbox.com/
 */

package opendropbox.servicediscovery.client;

import java.util.Vector;
import opendropbox.servicediscovery.ServiceDescription;

/**
 *
 * @author Walter
 */
public interface ServiceListManagerRefreshListener {

    public void refreshCallback(boolean changed, Vector<ServiceDescription> list);
    
}
