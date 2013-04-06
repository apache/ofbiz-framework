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
package org.ofbiz.jetty.container;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerConfig;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.SSLUtil;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;

import org.eclipse.jetty.ajp.Ajp13SocketConnector;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.server.ssl.SslConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;


/**
 * JettyContainer - Container implementation for Jetty
 */
public class JettyContainer implements Container {

    public static final String module = JettyContainer.class.getName();

    private String name;
    private Map<String, Server> servers = new HashMap<String, Server>();

    @Override
    public void init(String[] args, String name, String configFile) throws ContainerException {
        this.name = name;
        // configure JSSE properties
        SSLUtil.loadJsseProperties();

        // session store directory
        File sessionStoreDirectory = new File(UtilProperties.getPropertyValue("jetty", "session.store.directory", "runtime/jetty/sessions"));
        if (!sessionStoreDirectory.exists()) {
            if (!sessionStoreDirectory.mkdirs()) {
                throw new ContainerException("error creating session store directory: " + sessionStoreDirectory.getAbsolutePath());
            }
        }

        // get the jetty container config
        ContainerConfig.Container jettyContainerConfig = ContainerConfig.getContainer(name, configFile);

        // create the servers
        for (ContainerConfig.Container.Property serverConfig : jettyContainerConfig.getPropertiesWithValue("server")) {
            servers.put(serverConfig.name, createServer(serverConfig));
        }

        // create the webapp contexts
        for (ComponentConfig componentConfig : ComponentConfig.getAllComponents()) {

            for (ComponentConfig.WebappInfo webappInfo : componentConfig.getWebappInfos()) {

                Server server = servers.get(webappInfo.server);
                if (server == null) {

                    Debug.logError("Server with name [" + webappInfo.server + "] not found; not mounting [" + webappInfo.name + "]", module);

                } else {

                    // set the root location (make sure we set the paths correctly)
                    String location = componentConfig.getRootLocation() + webappInfo.location;
                    location = location.replace('\\', '/');
                    if (location.endsWith("/")) {
                        location = location.substring(0, location.lastIndexOf("/"));
                    }

                    String mountPoint = webappInfo.mountPoint;
                    if (mountPoint.endsWith("/*")) {
                        mountPoint = mountPoint.substring(0, mountPoint.lastIndexOf("/"));
                    }

                    WebAppContext context = new WebAppContext(location, mountPoint);
                    context.setAttribute("_serverId", webappInfo.server);
                    context.setLogUrlOnStart(true);

                    // set the session manager
                    HashSessionManager sm = new HashSessionManager();
                    sm.setStoreDirectory(sessionStoreDirectory);
                    sm.setLazyLoad(true);
                    context.setSessionHandler(new SessionHandler(sm));

                    // set the virtual hosts
                    List<String> virtualHosts = webappInfo.getVirtualHosts();
                    if (UtilValidate.isNotEmpty(virtualHosts)) {
                        context.setVirtualHosts(virtualHosts.toArray(new String[virtualHosts.size()]));
                    }

                    // set the init parameters
                    Map<String, String> initParameters = webappInfo.getInitParameters();
                    if (UtilValidate.isNotEmpty(initParameters)) {
                        for (Map.Entry<String, String> e : initParameters.entrySet()) {
                            context.setInitParameter(e.getKey(), e.getValue());
                        }
                    }

                    ((HandlerCollection) server.getHandler()).addHandler(context);
                }
            }
        }
    }

    private Server createServer(ContainerConfig.Container.Property serverConfig) {

        Server server = new Server();
        server.setHandler(new HandlerCollection());

        // send server version?
        if (UtilValidate.isNotEmpty(serverConfig.getProperty("send-server-version"))) {
            String sendServerVersionPropertyValue = serverConfig.getProperty("send-server-version").value;
            try {
                server.setSendServerVersion(Boolean.parseBoolean(sendServerVersionPropertyValue));
            } catch (NumberFormatException e) {
                Debug.logError(e, "invalid value for send-server-version: " + sendServerVersionPropertyValue, module);
            }
        }

        // thread pool
        server.setThreadPool(createThreadPool(serverConfig));

        // connectors
        for (ContainerConfig.Container.Property connectorConfig : serverConfig.getPropertiesWithValue("connector")) {

            Connector connector = null;

            String connectorType = connectorConfig.getProperty("type").value;

            if ("http".equals(connectorType)) {
                connector = new SocketConnector();
            } else if ("https".equals(connectorType)) {
                connector = new SslSocketConnector();
            } else if ("nio-http".equals(connectorType)) {
                connector = new SelectChannelConnector();
            } else if ("nio-https".equals(connectorType)) {
                connector = new SslSelectChannelConnector();
            } else if ("ajp13".equals(connectorType)) {
                connector = new Ajp13SocketConnector();
            }

            if (connector != null) {
                setConnectorOptions(connector, connectorConfig);
                server.addConnector(connector);
            }
        }

        // request logs
        for (ContainerConfig.Container.Property props : serverConfig.getPropertiesWithValue("request-log")) {

            NCSARequestLog requestLog = new NCSARequestLog();

            if (UtilValidate.isNotEmpty(props.getProperty("filename"))) {
                requestLog.setFilename(props.getProperty("filename").value);
            }
            if (UtilValidate.isNotEmpty(props.getProperty("append"))) {
                requestLog.setAppend("true".equalsIgnoreCase(props.getProperty("append").value));
            }
            if (UtilValidate.isNotEmpty(props.getProperty("extended"))) {
                requestLog.setExtended("true".equalsIgnoreCase(props.getProperty("extended").value));
            }
            if (UtilValidate.isNotEmpty(props.getProperty("log-dispatch"))) {
                requestLog.setLogDispatch("true".equalsIgnoreCase(props.getProperty("log-dispatch").value));
            }
            if (UtilValidate.isNotEmpty(props.getProperty("log-latency"))) {
                requestLog.setLogLatency("true".equalsIgnoreCase(props.getProperty("log-latency").value));
            }
            if (UtilValidate.isNotEmpty(props.getProperty("log-server"))) {
                requestLog.setLogServer("true".equalsIgnoreCase(props.getProperty("log-server").value));
            }
            if (UtilValidate.isNotEmpty(props.getProperty("prefer-proxied-for-address"))) {
                requestLog.setPreferProxiedForAddress("true".equalsIgnoreCase(props.getProperty("prefer-proxied-for-address").value));
            }
            if (UtilValidate.isNotEmpty(props.getProperty("timezone"))) {
                requestLog.setLogTimeZone(props.getProperty("timezone").value);
            }
            if (UtilValidate.isNotEmpty(props.getProperty("date-format"))) {
                requestLog.setLogDateFormat(props.getProperty("date-format").value);
            }
            if (UtilValidate.isNotEmpty(props.getProperty("retain-days"))) {
                String retainDaysPropertyValue = props.getProperty("retain-days").value;
                try {
                    requestLog.setRetainDays(Integer.parseInt(retainDaysPropertyValue));
                } catch (NumberFormatException e) {
                    Debug.logError(e, "invalid value for retain-days: " + retainDaysPropertyValue, module);
                }
            }

            RequestLogHandler requestLogHandler = new RequestLogHandler();
            requestLogHandler.setRequestLog(requestLog);

            ((HandlerCollection) server.getHandler()).addHandler(requestLogHandler);
        }

        return server;
    }

    private ThreadPool createThreadPool(ContainerConfig.Container.Property serverConfig) {

        QueuedThreadPool threadPool = new QueuedThreadPool();

        if (UtilValidate.isNotEmpty(serverConfig.getProperty("min-threads"))) {
            String minThreadsPropertyValue = serverConfig.getProperty("min-threads").value;
            try {
                threadPool.setMinThreads(Integer.parseInt(minThreadsPropertyValue));
            } catch (NumberFormatException e) {
                Debug.logError(e, "invalid value for min-threads: " + minThreadsPropertyValue, module);
            }
        }
        if (UtilValidate.isNotEmpty(serverConfig.getProperty("max-threads"))) {
            String maxThreadsPropertyValue = serverConfig.getProperty("max-threads").value;
            try {
                threadPool.setMaxThreads(Integer.parseInt(maxThreadsPropertyValue));
            } catch (NumberFormatException e) {
                Debug.logError(e, "invalid value for max-threads: " + maxThreadsPropertyValue, module);
            }
        }
        if (UtilValidate.isNotEmpty(serverConfig.getProperty("max-idle-time-ms"))) {
            String maxIdleTimeMsPropertyValue = serverConfig.getProperty("max-idle-time-ms").value;
            try {
                threadPool.setMaxIdleTimeMs(Integer.parseInt(maxIdleTimeMsPropertyValue));
            } catch (NumberFormatException e) {
                Debug.logError(e, "invalid value for max-idle-time-ms: " + maxIdleTimeMsPropertyValue, module);
            }
        }
        if (UtilValidate.isNotEmpty(serverConfig.getProperty("max-stop-time-ms"))) {
            String maxStopTimeMsPropertyValue = serverConfig.getProperty("max-stop-time-ms").value;
            try {
                threadPool.setMaxStopTimeMs(Integer.parseInt(maxStopTimeMsPropertyValue));
            } catch (NumberFormatException e) {
                Debug.logError(e, "invalid value for max-stop-time-ms: " + maxStopTimeMsPropertyValue, module);
            }
        }

        return threadPool;
    }

    private void setConnectorOptions(Connector connector, ContainerConfig.Container.Property connectorConfig) {

        if (UtilValidate.isNotEmpty(connectorConfig.getProperty("host"))) {
            connector.setHost(connectorConfig.getProperty("host").value);
        }
        if (UtilValidate.isNotEmpty(connectorConfig.getProperty("port"))) {
            String portPropertyValue = connectorConfig.getProperty("port").value;
            try {
                connector.setPort(Integer.parseInt(portPropertyValue));
            } catch (NumberFormatException e) {
                Debug.logError(e, "invalid value for port: " + portPropertyValue, module);
            }
        }
        if (UtilValidate.isNotEmpty(connectorConfig.getProperty("request-buffer-size"))) {
            String requestBufferSizePropertyValue = connectorConfig.getProperty("request-buffer-size").value;
            try {
                connector.setRequestBufferSize(Integer.parseInt(requestBufferSizePropertyValue));
            } catch (NumberFormatException e) {
                Debug.logError(e, "invalid value for request-buffer-size: " + requestBufferSizePropertyValue, module);
            }
        }
        if (UtilValidate.isNotEmpty(connectorConfig.getProperty("request-header-size"))) {
            String requestHeaderSizePropertyValue = connectorConfig.getProperty("request-header-size").value;
            try {
                connector.setRequestHeaderSize(Integer.parseInt(requestHeaderSizePropertyValue));
            } catch (NumberFormatException e) {
                Debug.logError(e, "invalid value for request-header-size: " + requestHeaderSizePropertyValue, module);
            }
        }
        if (UtilValidate.isNotEmpty(connectorConfig.getProperty("response-buffer-size"))) {
            String responseBufferSizePropertyValue = connectorConfig.getProperty("response-buffer-size").value;
            try {
                connector.setResponseBufferSize(Integer.parseInt(responseBufferSizePropertyValue));
            } catch (NumberFormatException e) {
                Debug.logError(e, "invalid value for response-buffer-size: " + responseBufferSizePropertyValue, module);
            }
        }
        if (UtilValidate.isNotEmpty(connectorConfig.getProperty("response-header-size"))) {
            String responseHeaderSizePropertyValue = connectorConfig.getProperty("response-header-size").value;
            try {
                connector.setResponseHeaderSize(Integer.parseInt(responseHeaderSizePropertyValue));
            } catch (NumberFormatException e) {
                Debug.logError(e, "invalid value for response-header-size: " + responseHeaderSizePropertyValue, module);
            }
        }
        if (UtilValidate.isNotEmpty(connectorConfig.getProperty("low-resource-max-idle-time"))) {
            String lowResourceMaxIdleTimePropertyValue = connectorConfig.getProperty("low-resource-max-idle-time").value;
            try {
                connector.setLowResourceMaxIdleTime(Integer.parseInt(lowResourceMaxIdleTimePropertyValue));
            } catch (NumberFormatException e) {
                Debug.logError(e, "invalid value for low-resource-max-idle-time: " + lowResourceMaxIdleTimePropertyValue, module);
            }
        }

        // SSL options
        if (connector instanceof SslConnector) {

            SslContextFactory cf = ((SslConnector) connector).getSslContextFactory();

            if (UtilValidate.isNotEmpty(connectorConfig.getProperty("keystore"))) {
                cf.setKeyStorePath(connectorConfig.getProperty("keystore").value);
            }
            if (connectorConfig.getProperty("password") != null) {
                cf.setKeyStorePassword(connectorConfig.getProperty("password").value);
            }
            if (connectorConfig.getProperty("key-password") != null) {
                cf.setKeyManagerPassword(connectorConfig.getProperty("key-password").value);
            }
            if (UtilValidate.isNotEmpty(connectorConfig.getProperty("client-auth"))) {
                if ("need".equals(connectorConfig.getProperty("client-auth").value)) {
                    cf.setNeedClientAuth(true);
                } else if ("want".equals(connectorConfig.getProperty("client-auth").value)) {
                    cf.setWantClientAuth(true);
                }
            }
        }
    }

    /**
     * @see org.ofbiz.base.container.Container#start()
     */
    public boolean start() throws ContainerException {
        for (Server server : servers.values()) {
            try {
                server.start();
            } catch (Exception e) {
                throw new ContainerException(e);
            }
        }
        return true;
    }

    /**
     * @see org.ofbiz.base.container.Container#stop()
     */
    public void stop() throws ContainerException {
        for (Server server : servers.values()) {
            try {
                server.stop();
            } catch (Exception e) {
                throw new ContainerException(e);
            }
        }
    }

    public String getName() {
        return name;
    }

}
