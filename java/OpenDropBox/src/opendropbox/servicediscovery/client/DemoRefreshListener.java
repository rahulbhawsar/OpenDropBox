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
public class DemoRefreshListener implements ServiceListManagerRefreshListener {

    public void refreshCallback(boolean changed, Vector<ServiceDescription> list) {
        System.out.println("Changed: " + changed);
        System.out.println("List: " + list);
    }
}
