/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.service.jms;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ServiceDispatcher;
import org.ofbiz.service.config.ServiceConfigUtil;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilXml;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * JmsListenerFactory
 */
public class JmsListenerFactory implements Runnable {

    public static final String module = JmsListenerFactory.class.getName();

    public static final String TOPIC_LISTENER_CLASS = "org.ofbiz.service.jms.JmsTopicListener";
    public static final String QUEUE_LISTENER_CLASS = "org.ofbiz.service.jms.JmsQueueListener";

    protected static Map listeners = new HashMap();
    protected static Map servers = new HashMap();

    protected ServiceDispatcher dispatcher;
    protected boolean firstPass = true;
    protected int  loadable = 0;
    protected int connected = 0;
    protected Thread thread;

    public JmsListenerFactory(ServiceDispatcher dispatcher) {
        this.dispatcher = dispatcher;
        thread = new Thread(this, this.toString());
        thread.setDaemon(false);
        thread.start();
    }

    public void run() {
        Debug.logInfo("Starting JMS Listener Factory Thread", module);
        while (firstPass || connected < loadable) {
            if (Debug.verboseOn()) Debug.logVerbose("First Pass: " + firstPass + " Connected: " + connected + " Available: " + loadable, module);
            this.loadListeners();
            firstPass = false;
            try {
                Thread.sleep(20000);
            } catch (InterruptedException ie) {}
            continue;
        }
        Debug.logInfo("JMS Listener Factory Thread Finished; All listeners connected.", module);
    }

    // Load the JMS listeners
    private void loadListeners() {
        try {
            Element rootElement = ServiceConfigUtil.getXmlRootElement();
            NodeList nodeList = rootElement.getElementsByTagName("jms-service");

            if (Debug.verboseOn()) Debug.logVerbose("[ServiceDispatcher] : Loading JMS Listeners.", module);
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element element = (Element) nodeList.item(i);
                List serverList = UtilXml.childElementList(element, "server");
                Iterator serverIter = serverList.iterator();

                while (serverIter.hasNext()) {
                    Element server = (Element) serverIter.next();

                    try {
                        String listenerEnabled = server.getAttribute("listen");

                        if (listenerEnabled.equalsIgnoreCase("true")) {
                            // create a server key
                            StringBuffer serverKey = new StringBuffer();

                            serverKey.append(server.getAttribute("jndi-server-name") + ":");
                            serverKey.append(server.getAttribute("jndi-name") + ":");
                            serverKey.append(server.getAttribute("topic-queue"));
                            // store the server element
                            servers.put(serverKey.toString(), server);
                            // load the listener
                            GenericMessageListener listener = loadListener(serverKey.toString(), server);

                            // store the listener w/ the key
                            if (serverKey.length() > 0 && listener != null)
                                listeners.put(serverKey.toString(), listener);
                        }
                    } catch (GenericServiceException gse) {
                        Debug.logVerbose("Cannot load message listener (" + gse.toString() + ").", module);
                    } catch (Exception e) {
                        Debug.logError(e, "Uncaught exception.", module);
                    }
                }
            }
        } catch (org.ofbiz.base.config.GenericConfigException gce) {
            Debug.logError(gce, "Cannot get serviceengine.xml root element.", module);
        } catch (Exception e) {
            Debug.logError(e, "Uncaught exception.", module);
        }
    }

    private GenericMessageListener loadListener(String serverKey, Element server) throws GenericServiceException {
        String serverName = server.getAttribute("jndi-server-name");
        String jndiName = server.getAttribute("jndi-name");
        String queueName = server.getAttribute("topic-queue");
        String type = server.getAttribute("type");
        String userName = server.getAttribute("username");
        String password = server.getAttribute("password");
        String className = server.getAttribute("listener-class");

        if (className == null || className.length() == 0) {
            if (type.equals("topic"))
                className = JmsListenerFactory.TOPIC_LISTENER_CLASS;
            else if (type.equals("queue"))
                className = JmsListenerFactory.QUEUE_LISTENER_CLASS;
        }

        GenericMessageListener listener = (GenericMessageListener) listeners.get(serverKey);

        if (listener == null) {
            synchronized (this) {
                listener = (GenericMessageListener) listeners.get(serverKey);
                if (listener == null) {
                    ClassLoader cl = this.getClass().getClassLoader();
                    Class[] paramTypes = new Class[] {ServiceDispatcher.class, String.class, String.class, String.class, String.class, String.class};
                    Object[] params = new Object[] {dispatcher, serverName, jndiName, queueName, userName, password};

                    try {
                        Class c = cl.loadClass(className);
                        Constructor cn = c.getConstructor(paramTypes);

                        listener = (GenericMessageListener) cn.newInstance(params);
                    } catch (Exception e) {
                        throw new GenericServiceException(e.getMessage(), e);
                    }
                    if (listener != null)
                        listeners.put(serverKey, listener);
                    loadable++;
                }
            }

        }
        if (listener != null && !listener.isConnected()) {
            listener.load();
            if (listener.isConnected())
                connected++;
        }
        return listener;
    }

    /**
     * Load a JMS message listener.
     * @param serverKey Name of the jms-service
     * @throws GenericServiceException
     */
    public void loadListener(String serverKey) throws GenericServiceException {
        Element server = (Element) servers.get(serverKey);

        if (server == null)
            throw new GenericServiceException("No listener found with that serverKey.");
        loadListener(serverKey, server);
    }
        
    /**
     * Close all the JMS message listeners.
     * @throws GenericServiceException
     */
    public void closeListeners() throws GenericServiceException {
        loadable = 0;
        Set listenerKeys = listeners.keySet();
        Iterator listenerIterator = listenerKeys.iterator();
        while (listenerIterator.hasNext()) {
            String serverKey = (String) listenerIterator.next();
            closeListener(serverKey);
        }
    }

    /**
     * Close a JMS message listener.
     * @param serverKey Name of the jms-service
     * @throws GenericServiceException
     */
    public void closeListener(String serverKey) throws GenericServiceException {
        GenericMessageListener listener = (GenericMessageListener) listeners.get(serverKey);

        if (listener == null)
            throw new GenericServiceException("No listener found with that serverKey.");
        listener.close();
    }

    /**
     * Refresh a JMS message listener.
     * @param serverKey Name of the jms-service
     * @throws GenericServiceException
     */
    public void refreshListener(String serverKey) throws GenericServiceException {
        GenericMessageListener listener = (GenericMessageListener) listeners.get(serverKey);

        if (listener == null)
            throw new GenericServiceException("No listener found with that serverKey.");
        listener.refresh();
    }

    /**
     * Gets a Map of JMS Listeners.
     * @return Map of JMS Listeners
     */
    public Map getJMSListeners() {
        return new HashMap(listeners);
    }

}
