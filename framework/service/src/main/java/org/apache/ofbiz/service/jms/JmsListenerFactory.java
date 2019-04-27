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
package org.apache.ofbiz.service.jms;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.ofbiz.base.config.GenericConfigException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.config.ServiceConfigUtil;
import org.apache.ofbiz.service.config.model.JmsService;
import org.apache.ofbiz.service.config.model.Server;

/**
 * JmsListenerFactory
 */
public class JmsListenerFactory implements Runnable {

    public static final String module = JmsListenerFactory.class.getName();

    public static final String TOPIC_LISTENER_CLASS = "org.apache.ofbiz.service.jms.JmsTopicListener";
    public static final String QUEUE_LISTENER_CLASS = "org.apache.ofbiz.service.jms.JmsQueueListener";

    protected static Map<String, GenericMessageListener> listeners = new ConcurrentHashMap<>();
    protected static Map<String, Server> servers = new ConcurrentHashMap<>();

    private static final AtomicReference<JmsListenerFactory> jlFactoryRef = new AtomicReference<>(null);

    protected Delegator delegator;
    protected boolean firstPass = true;
    protected int  loadable = 0;
    protected int connected = 0;
    protected Thread thread;


    public static JmsListenerFactory getInstance(Delegator delegator){
        JmsListenerFactory instance = jlFactoryRef.get();
        if (instance == null) {
            instance = new JmsListenerFactory(delegator);
            if (!jlFactoryRef.compareAndSet(null, instance)) {
                instance = jlFactoryRef.get();
            }
        }
        return instance;
    }

    public JmsListenerFactory(Delegator delegator) {
        this.delegator = delegator;
        thread = new Thread(this, this.toString());
        thread.setDaemon(false);
        thread.start();
    }

    @Override
    public void run() {
        Debug.logInfo("Starting JMS Listener Factory Thread", module);
        while (firstPass || connected < loadable) {
            if (Debug.verboseOn()) Debug.logVerbose("First Pass: " + firstPass + " Connected: " + connected + " Available: " + loadable, module);
            this.loadListeners();
            if (loadable == 0) {
                // if there is nothing to do then we can break without sleeping
                break;
            }
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
            List<JmsService> jmsServices = ServiceConfigUtil.getServiceEngine().getJmsServices();

            if (Debug.verboseOn()) Debug.logVerbose("Loading JMS Listeners.", module);
            for (JmsService service: jmsServices) {
                StringBuilder serverKey = new StringBuilder();
                for (Server server: service.getServers()) {
                    try {
                        if (server.getListen()) {
                            // create a server key
                            serverKey.append(server.getJndiServerName() + ":");
                            serverKey.append(server.getJndiName() + ":");
                            serverKey.append(server.getTopicQueue());
                            // store the server element
                            servers.put(serverKey.toString(), server);
                            // load the listener
                            GenericMessageListener listener = loadListener(serverKey.toString(), server);

                            // store the listener w/ the key
                            if (serverKey.length() > 0 && listener != null)
                                listeners.put(serverKey.toString(), listener);
                        }
                    } catch (GenericServiceException gse) {
                        Debug.logInfo("Cannot load message listener " + serverKey + " error: (" + gse.toString() + ").", module);
                    } catch (Exception e) {
                        Debug.logError(e, "Uncaught exception.", module);
                    }
                }
            }
        } catch (GenericConfigException e) {
            Debug.logError(e, "Exception thrown while loading JMS listeners: ", module);
        }
    }

    private GenericMessageListener loadListener(String serverKey, Server server) throws GenericServiceException {
        String serverName = server.getJndiServerName();
        String jndiName = server.getJndiName();
        String queueName = server.getTopicQueue();
        String type = server.getType();
        String userName = server.getUsername();
        String password = server.getPassword();
        String className = server.getListenerClass();

        if (UtilValidate.isEmpty(className)) {
            if ("topic".equals(type))
                className = JmsListenerFactory.TOPIC_LISTENER_CLASS;
            else if ("queue".equals(type))
                className = JmsListenerFactory.QUEUE_LISTENER_CLASS;
        }

        GenericMessageListener listener = listeners.get(serverKey);

        if (listener == null) {
            synchronized (this) {
                listener = listeners.get(serverKey);
                if (listener == null) {
                    ClassLoader cl = this.getClass().getClassLoader();

                    try {
                        Class<?> c = cl.loadClass(className);
                        Constructor<GenericMessageListener> cn = UtilGenerics.cast(c.getConstructor(Delegator.class, String.class, String.class, String.class, String.class, String.class));

                        listener = cn.newInstance(delegator, serverName, jndiName, queueName, userName, password);
                    } catch (RuntimeException | NoSuchMethodException | InstantiationException | IllegalAccessException
                            | InvocationTargetException | ClassNotFoundException e) {
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
        Server server = servers.get(serverKey);

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
        for (String serverKey: listeners.keySet()) {
            closeListener(serverKey);
        }
    }

    /**
     * Close a JMS message listener.
     * @param serverKey Name of the jms-service
     * @throws GenericServiceException
     */
    public void closeListener(String serverKey) throws GenericServiceException {
        GenericMessageListener listener = listeners.get(serverKey);

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
        GenericMessageListener listener = listeners.get(serverKey);

        if (listener == null)
            throw new GenericServiceException("No listener found with that serverKey.");
        listener.refresh();
    }

    /**
     * Gets a Map of JMS Listeners.
     * @return Map of JMS Listeners
     */
    public Map<String, GenericMessageListener> getJMSListeners() {
        return UtilMisc.makeMapWritable(listeners);
    }

}
