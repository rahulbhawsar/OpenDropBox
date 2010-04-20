/*
 * Copyright 2010 OpenDropBox
 * http://www.opendropbox.com/
 */
package opendropbox.servicediscovery.monitor;

/**
 *
 * @author Walter
 */
public class DemoServiceMonitor {

    public static void main(String[] args) {

        DemoServiceMonitorCallback serviceMonitorCallback = new DemoServiceMonitorCallback();

        new ServiceMonitor(serviceMonitorCallback, 1000);
    }
}
