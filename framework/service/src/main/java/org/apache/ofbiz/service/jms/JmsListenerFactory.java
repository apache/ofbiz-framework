/*
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
import java.util.concurrent.atomic.AtomicBoolean;
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
 * @deprecated since 26.11, removed with the JMS service engine (OFBIZ-13829); servers clear each other's caches through
 *             the cache invalidation bus instead, see {@code org.apache.ofbiz.entity.cache.EntityCacheInvalidator}.
 */
@Deprecated(since = "26.11", forRemoval = true)
@SuppressWarnings("removal") // uses the other JMS classes, deprecated with it
public final class JmsListenerFactory implements Runnable {

    private static final String MODULE = JmsListenerFactory.class.getName();
    private static final String DEPRECATION = "; it is deprecated and will be removed after 26.11 (OFBIZ-13829). To clear caches across servers, "
            + "select org.apache.ofbiz.entity.cache.EntityCacheInvalidator as the delegator's distributed-cache-clear-class-name "
            + "and install a cache invalidation bus provider plugin; see UPGRADING.md.";
    /** The deprecation is logged once, when the first listener is loaded; listeners are loaded again every 20 seconds. */
    private static final AtomicBoolean DEPRECATION_LOGGED = new AtomicBoolean();

    public static final String TOPIC_LISTENER_CLASS = "org.apache.ofbiz.service.jms.JmsTopicListener";
    public static final String QUEUE_LISTENER_CLASS = "org.apache.ofbiz.service.jms.JmsQueueListener";

    private static Map<String, GenericMessageListener> listeners = new ConcurrentHashMap<>();
    private static Map<String, Server> servers = new ConcurrentHashMap<>();

    private static final AtomicReference<JmsListenerFactory> JL_FACTORY_REF = new AtomicReference<>(null);

    private final Delegator delegator;
    // set by closeListeners; once set, the loader thread stops and no listener is (re)connected
    private volatile boolean closed = false;
    private volatile Thread thread;


    public static JmsListenerFactory getInstance(Delegator delegator) {
        JmsListenerFactory instance = JL_FACTORY_REF.get();
        if (instance == null) {
            JmsListenerFactory created = new JmsListenerFactory(delegator);
            if (JL_FACTORY_REF.compareAndSet(null, created)) {
                // only the published instance runs the loader thread
                created.start();
                instance = created;
            } else {
                instance = JL_FACTORY_REF.get();
            }
        }
        return instance;
    }

    private JmsListenerFactory(Delegator delegator) {
        this.delegator = delegator;
    }

    private void start() {
        thread = new Thread(this, this.toString());
        thread.setDaemon(false);
        thread.start();
    }

    @Override
    public void run() {
        Debug.logInfo("Starting JMS Listener Factory Thread", MODULE);
        while (!closed) {
            this.loadListeners();
            if (listeners.values().stream().allMatch(GenericMessageListener::isConnected)) {
                // nothing to load, or every listener is connected
                Debug.logInfo("JMS Listener Factory Thread Finished; All listeners connected.", MODULE);
                return;
            }
            try {
                Thread.sleep(20000);
            } catch (InterruptedException ie) {
                if (!closed) {
                    Debug.logError(ie, MODULE);
                }
            }
        }
        Debug.logInfo("JMS Listener Factory Thread Finished; listeners closed.", MODULE);
    }

    // Load the JMS listeners
    private void loadListeners() {
        try {
            List<JmsService> jmsServices = ServiceConfigUtil.getServiceEngine().getJmsServices();

            if (Debug.verboseOn()) {
                Debug.logVerbose("Loading JMS Listeners.", MODULE);
            }
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
                            if (!DEPRECATION_LOGGED.getAndSet(true)) {
                                Debug.logWarning("A JMS listener is configured for " + serverKey + DEPRECATION, MODULE);
                            }
                            // load the listener
                            GenericMessageListener listener = loadListener(serverKey.toString(), server);

                            // store the listener w/ the key
                            if (serverKey.length() > 0 && listener != null) {
                                listeners.put(serverKey.toString(), listener);
                            }
                        }
                    } catch (GenericServiceException gse) {
                        Debug.logInfo("Cannot load message listener " + serverKey + " error: (" + gse.toString() + ").", MODULE);
                    } catch (Exception e) {
                        Debug.logError(e, "Uncaught exception.", MODULE);
                    }
                }
            }
        } catch (GenericConfigException e) {
            Debug.logError(e, "Exception thrown while loading JMS listeners: ", MODULE);
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
            if ("topic".equals(type)) {
                className = JmsListenerFactory.TOPIC_LISTENER_CLASS;
            } else if ("queue".equals(type)) {
                className = JmsListenerFactory.QUEUE_LISTENER_CLASS;
            }
        }

        GenericMessageListener listener = listeners.get(serverKey);

        if (listener == null) {
            synchronized (this) {
                listener = listeners.get(serverKey);
                if (listener == null) {
                    ClassLoader cl = this.getClass().getClassLoader();

                    try {
                        Class<?> c = cl.loadClass(className);
                        Constructor<GenericMessageListener> cn = UtilGenerics.cast(c.getConstructor(Delegator.class,
                                String.class, String.class, String.class, String.class, String.class));

                        listener = cn.newInstance(delegator, serverName, jndiName, queueName, userName, password);
                    } catch (RuntimeException | NoSuchMethodException | InstantiationException | IllegalAccessException
                            | InvocationTargetException | ClassNotFoundException e) {
                        throw new GenericServiceException(e.getMessage(), e);
                    }
                    if (listener != null) {
                        listeners.put(serverKey, listener);
                    }
                }
            }

        }
        if (listener != null && !closed && !listener.isConnected()) {
            listener.load();
            if (closed) {
                // closeListeners ran while this listener was connecting
                listener.close();
            }
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

        if (server == null) {
            throw new GenericServiceException("No listener found with that serverKey.");
        }
        loadListener(serverKey, server);
    }

    /**
     * Close all the JMS message listeners.
     * @throws GenericServiceException
     */
    public void closeListeners() throws GenericServiceException {
        closed = true;
        Thread loader = thread;
        if (loader != null) {
            // wake the loader thread from its retry sleep, or a connection attempt, so that it ends now
            loader.interrupt();
        }
        for (Map.Entry<String, GenericMessageListener> entry: listeners.entrySet()) {
            if (entry.getValue() instanceof AbstractJmsListener jmsListener) {
                jmsListener.stopReconnecting();
            }
            try {
                closeListener(entry.getKey());
            } catch (GenericServiceException e) {
                // keep closing the other listeners
                Debug.logError(e, "Unable to close the JMS listener " + entry.getKey(), MODULE);
            }
        }
    }

    /**
     * Close a JMS message listener.
     * @param serverKey Name of the jms-service
     * @throws GenericServiceException
     */
    public void closeListener(String serverKey) throws GenericServiceException {
        GenericMessageListener listener = listeners.get(serverKey);

        if (listener == null) {
            throw new GenericServiceException("No listener found with that serverKey.");
        }
        listener.close();
    }

    /**
     * Refresh a JMS message listener.
     * @param serverKey Name of the jms-service
     * @throws GenericServiceException
     */
    public void refreshListener(String serverKey) throws GenericServiceException {
        GenericMessageListener listener = listeners.get(serverKey);

        if (listener == null) {
            throw new GenericServiceException("No listener found with that serverKey.");
        }
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
