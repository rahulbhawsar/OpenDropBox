/*
 * Copyright 2010 OpenDropBox
 * http://www.opendropbox.com/
 */

package opendropbox.servicediscovery.client;

import java.util.Stack;
import java.util.Vector;
import opendropbox.servicediscovery.ServiceDescription;

/**
 *
 * @author Walter
 */
public class ServiceListManager implements ServiceBrowserListener {

    private Vector<ServiceDescription> _serviceListWrite;
    private Vector<ServiceDescription> _serviceListRead;
    private final Object _serviceListLock;
    private ServiceBrowser _serviceBrowser;

    public ServiceListManager(String serviceName) {
        _serviceBrowser = new ServiceBrowser(this, serviceName);
        _serviceListLock = new Object();
        _serviceListWrite = new Vector<ServiceDescription>();
        _serviceListRead = new Vector<ServiceDescription>();
    }

    /**
     * Performs a refresh of the service list.  This
     * method blocks.  If you would prefer a non-blocking
     * method, please see:
     *
     * ServiceListManager.refresh(long duration, boolean blocking)
     * 
     * @param duration
     * @return true if the list has changed, false otherwise
     */
    public boolean refresh(long duration) {

        // synchronize on the service list to ensure we only
        // run one refresh at a time
        synchronized (_serviceListLock) {
            
            // clear the service list
            _serviceListWrite.clear();

            // start the listener and the lookup
            _serviceBrowser.startListener();
            _serviceBrowser.startLookup();

            // sleep for the specified amount of time to allow the lookup
            // to run
            try {
                Thread.sleep(duration);
            } catch (InterruptedException e) {
            }

            // stop in the reverse order
            _serviceBrowser.stopLookup();
            _serviceBrowser.stopListener();

            boolean changed = true;

            // check to see if the list has changed
            if (_serviceListWrite.containsAll(_serviceListRead) && _serviceListWrite.size() == _serviceListRead.size()) {
                changed = false;
            }

            // create a copy of this service list so that we can return the
            // list during a refresh
            _serviceListRead = new Vector<ServiceDescription>(_serviceListWrite);

            // finally, return whether or not there has been a change
            return changed;
        }
    }

    /**
     * Refreshes the list in a non-blocking manner by returning
     * the list and boolean to the ServiceListManagerResfreshListener.
     * 
     * @param duration - the amount of time to spend browsing
     * @param listener - the listener to which return values will be sent back
     */
    public void refresh(long duration, ServiceListManagerRefreshListener listener) {

        // copy these to final local variables to ensure that they won't be
        // changed while the thread runs
        final long finalDuration = duration;
        final ServiceListManagerRefreshListener finalListener = listener;

        // create a new thread to call refresh and set the callback values
        // when the refresh completes.
        Thread refreshThread = new Thread(new Runnable() {
            public void run() {
                boolean changed = refresh(finalDuration);
                finalListener.refreshCallback(changed, _serviceListRead);
            }
        });

        // start the refresh thread
        refreshThread.start();
    }

    /**
     * Determines if this ServiceListManager knows of an instance
     * running on the local network.  Useful for determining whether
     * or not a server should start up (e.g. it won't start if there
     * already exists a service with the same instance name).
     *
     * Note: a return value of false does not not guarantee that no
     * such service exists; it only guarantees that it was not found
     * during the last refresh.
     *
     * @param instanceName - the name of the instance
     * @return true if it contains a reference, false otherwise.
     */
    public boolean containsInstance(String instanceName) {

        for (ServiceDescription s : _serviceListRead)
        {
            if (s.getInstanceName().equals(instanceName))
                return true;
        }

        return false;

    }

    /**
     * Returns the service name associated with this ServiceListManager.
     *
     * @return - the service name
     */
    public String getServiceName() {

        return _serviceBrowser.getServiceName();
    }

    /**
     * Get a Vector containing the list of ServiceDescriptions
     * that were valid as of the last refresh.
     *
     * @return - a Vector<ServiceDescription> containing a reference
     * to the ServiceDescriptions of servers on the local network.
     */
    public Vector<ServiceDescription> getServiceList() {

        // return the copy of the list to avoid returning an empty list
        // during a refresh
        return _serviceListRead;
    }

    /**
     * Required to be a ServiceBrowserListener.  This method is called
     * everytime a new service is discovered.  The behavior of this method
     * is such that duplicate services will be thrown out. However, if
     * the network is functioning properly, and all servers check to ensure
     * that they are unique before starting, there should never be duplicates.
     * 
     * @param description - the ServiceDescription representing the server
     * to be added
     */
    public void serviceEncountered(ServiceDescription description) {
        int pos = _serviceListWrite.indexOf(description);
        if (pos > -1) {
            _serviceListWrite.removeElementAt(pos);
        }
        _serviceListWrite.add(description);
    }
}
