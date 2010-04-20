/*
 * Copyright 2010 OpenDropBox
 * http://www.opendropbox.com/
 */
package opendropbox.servicediscovery.monitor;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import opendropbox.servicediscovery.ServiceConstants;
import opendropbox.servicediscovery.ServiceDescription;
import opendropbox.servicediscovery.client.ServiceListManager;

/**
 * This class listens for incoming connections that alert
 * it of a computer joining or leaving the network.  It
 * takes a ServiceMonitorCallback which it will utilize when
 * these events take place.  In addition, to ensure that
 * networks failures and crashes do not affect performance,
 * services will be polled on a specified interval, and
 * appropraite calls to ServiceMonitorCallback will be made
 * at this time.
 * 
 * @author Walter
 */
public class ServiceMonitor implements Runnable {

    // number of milliseconds to search for new services
    private static final int REFRESH_LENGTH = 5000;
    private ServiceMonitorCallback _callback;
    private int _pollingInterval;
    private Vector<ServiceDescription> _activeServices;
    private Timer _timer;
    private ServiceListManager _serviceListManager;
    private Boolean _currentlyPolling;

    /**
     * Creates a new ServiceMonitor with the default polling
     * interval of 60,000 milliseconds.
     * 
     * @param callback
     */
    public ServiceMonitor(ServiceMonitorCallback callback) {
        this(callback, ServiceConstants.DEFAULT_POLLING_INTERVAL);
    }

    /**
     * Creates a new ServiceMonitor with a custom polling interval. This interval
     * must be at least REFRESH_LENGTH in magnitude (defaults to 2000 milliseconds).
     * If it is less, it will be set at REFRESH_LENGTH;
     * 
     * @param callback - specify which class will receive updates when services join and leave
     * @param pollingInterval - in milliseconds
     */
    public ServiceMonitor(ServiceMonitorCallback callback, int pollingInterval) {

        _currentlyPolling = false;

        _callback = callback;

        if (pollingInterval < REFRESH_LENGTH) {
            _pollingInterval = REFRESH_LENGTH;
        } else {
            _pollingInterval = pollingInterval;
        }

        _activeServices = new Vector<ServiceDescription>();

        _serviceListManager = new ServiceListManager(ServiceConstants.SERVICE_NAME);

        // start the timer last
        _timer = new Timer("PollingTimer");
        _timer.scheduleAtFixedRate(new PollingTimer(), 0L, _pollingInterval);

    }

    protected void serviceJoined(ServiceDescription serviceDescription) {

        synchronized (_activeServices) {
            // notify the registered callback
            _callback.serviceJoined(serviceDescription);

            // add it to our list
            _activeServices.add(serviceDescription);
        }
    }

    protected void serviceDeparted(ServiceDescription serviceDescription) {

        synchronized (_activeServices) {
            // notify the registered callback
            _callback.serviceDeparted(serviceDescription);

            // remove it from our list
            _activeServices.remove(serviceDescription);
        }
    }

    public void run() {
    }

    private class PollingTimer extends TimerTask {

        public void run() {

            // check to see if we are already polling
            synchronized (_currentlyPolling) {

                // if we are currently polling, then set skip this task
                if (_currentlyPolling == true) {
                    System.out.println("Not polling.  Too many requests at once.");
                    return;
                } // otherwise, set it to true and continue with this task
                else {
                    _currentlyPolling = true;
                }
            }


            // refresh the service list.  if nothing has changed, we can return
            if (!_serviceListManager.refresh(2000)) {
                synchronized (_currentlyPolling) {
                    _currentlyPolling = false;
                }
                return;
            }

            // now, check to see if there are any new services that we don't yet know about
            Vector<ServiceDescription> refreshedServices = _serviceListManager.getServiceList();


            System.out.println(refreshedServices);

            // iterate through all of the refreshed services
            for (ServiceDescription s : refreshedServices) {

                // check to see if we know about this service
                boolean alreadyKnow = false;
                synchronized (_activeServices) {
                    alreadyKnow = _activeServices.contains(s);
                }

                if (!alreadyKnow) {
                    serviceJoined(s);
                }
            }

            // now that we have added every service we know about, we must check to see if any services have departed
            Vector<ServiceDescription> departed = new Vector<ServiceDescription>();

            synchronized (_activeServices) {
                for (ServiceDescription s : _activeServices) {

                    // check to see if this service is in in the refreshed list.
                    // if it is not, we should add it to the toDepart list

                    if (!refreshedServices.contains(s)) {
                        departed.add(s);
                    }
                }
            }

            // now, remove all the departed services
            for (ServiceDescription s : departed) {
                serviceDeparted(s);
            }

            // we are no longer polling, so set it to false
            synchronized (_currentlyPolling) {
                _currentlyPolling = false;
            }
        }
    }
}

