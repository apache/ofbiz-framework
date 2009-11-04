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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mortbay.jetty.AbstractConnector;
import org.mortbay.jetty.NCSARequestLog;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.ajp.Ajp13SocketConnector;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.RequestLogHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.security.SslSelectChannelConnector;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.servlet.HashSessionManager;
import org.mortbay.jetty.servlet.SessionHandler;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.thread.BoundedThreadPool;
import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerConfig;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.SSLUtil;


/**
 * JettyContainer - Container implementation for Jetty 6
 *
 */
public class JettyContainer implements Container {

    public static final String module = JettyContainer.class.getName();

    protected String configFile = null;
    private Map<String, Server> servers = new HashMap<String, Server>();

    /**
     * @see org.ofbiz.base.container.Container#init(java.lang.String[],java.lang.String)
     */
    public void init(String[] args, String configFile) {
        this.configFile = configFile;
    }

    private void initJetty() throws ContainerException {

        // configure JSSE properties
        SSLUtil.loadJsseProperties();

        // get the container
        ContainerConfig.Container jc = ContainerConfig.getContainer("jetty-container", configFile);

        // create the servers
        for (ContainerConfig.Container.Property property : jc.properties.values()) {
            servers.put(property.name, createServer(property));
        }

        // load the applications
        Collection<ComponentConfig> componentConfigs = ComponentConfig.getAllComponents();
        if (componentConfigs != null) {

            for (Object componentConfig : componentConfigs) {

                ComponentConfig component = (ComponentConfig) componentConfig;

                for (ComponentConfig.WebappInfo webappInfo : component.getWebappInfos()) {

                    List<String> virtualHosts = webappInfo.getVirtualHosts();
                    Map<String, String> initParameters = webappInfo.getInitParameters();

                    Server server = servers.get(webappInfo.server);

                    if (server == null) {
                        Debug.logWarning("Server with name [" + webappInfo.server + "] not found; not mounting [" + webappInfo.name + "]", module);
                    } else {

                        // set the root location (make sure we set the paths correctly)
                        String location = component.getRootLocation() + webappInfo.location;
                        location = location.replace('\\', '/');
                        if (location.endsWith("/")) {
                            location = location.substring(0, location.lastIndexOf("/"));
                        }

                        // load the application
                        String mountPoint = webappInfo.mountPoint;
                        if (mountPoint.endsWith("/*")) {
                            mountPoint = mountPoint.substring(0, mountPoint.lastIndexOf("/"));
                        }

                        WebAppContext context = new WebAppContext(location, mountPoint);
                        context.setAttribute("_serverId", webappInfo.server);
                        context.setLogUrlOnStart(true);

                         // set the session manager
                        HashSessionManager sm = new HashSessionManager();
                        context.setSessionHandler(new SessionHandler(sm));

                        // set the virtual hosts
                        if (virtualHosts != null && !virtualHosts.isEmpty()) {
                            context.setVirtualHosts((String[]) virtualHosts.toArray());
                        }

                        // set the init parameters
                        if (initParameters != null && !initParameters.isEmpty()) {
                            context.setInitParams(initParameters);
                        }

                        server.addHandler(context);
                    }
                }
            }
        }
    }

    private Server createServer(ContainerConfig.Container.Property serverConfig) throws ContainerException {

        Server server = new Server();

        // configure the connectors / loggers
        for (ContainerConfig.Container.Property props : serverConfig.properties.values()) {

            if ("send-server-version".equals(props.name)) {
                if ("false".equalsIgnoreCase(props.value)) {
                    server.setSendServerVersion(false);
                }
            } else if ("connector".equals(props.value)) {

                if ("http".equals(props.getProperty("type").value)) {

                    AbstractConnector connector = new SocketConnector();
                    setConnectorOptions(connector, props);
                    server.addConnector(connector);

                } else if ("https".equals(props.getProperty("type").value)) {

                    SslSocketConnector connector = new SslSocketConnector();
                    setConnectorOptions(connector, props);

                    if (props.getProperty("keystore") != null) {
                        connector.setKeystore(props.getProperty("keystore").value);
                    }
                    if (props.getProperty("password") != null) {
                        connector.setPassword(props.getProperty("password").value);
                    }
                    if (props.getProperty("key-password") != null) {
                        connector.setKeyPassword(props.getProperty("key-password").value);
                    }
                    if (props.getProperty("client-auth") != null) {
                        if ("need".equals(props.getProperty("client-auth").value)) {
                            connector.setNeedClientAuth(true);
                        } else if ("want".equals(props.getProperty("client-auth").value)) {
                            connector.setWantClientAuth(true);
                        }
                    }

                    server.addConnector(connector);

                } else if ("nio-http".equals(props.getProperty("type").value)) {

                    AbstractConnector connector = new SelectChannelConnector();
                    setConnectorOptions(connector, props);
                    server.addConnector(connector);

                } else if ("nio-https".equals(props.getProperty("type").value)) {

                    SslSelectChannelConnector connector = new SslSelectChannelConnector();
                    setConnectorOptions(connector, props);

                    if (props.getProperty("keystore") != null) {
                        connector.setKeystore(props.getProperty("keystore").value);
                    }
                    if (props.getProperty("password") != null) {
                        connector.setPassword(props.getProperty("password").value);
                    }
                    if (props.getProperty("key-password") != null) {
                        connector.setKeyPassword(props.getProperty("key-password").value);
                    }
                    if (props.getProperty("need-client-auth") != null) {
                        boolean needClientAuth = "true".equalsIgnoreCase(props.getProperty("need-client-auth").value);
                        connector.setNeedClientAuth(needClientAuth);
                    }

                    server.addConnector(connector);

                } else if ("ajp13".equals(props.getProperty("type").value)) {

                    AbstractConnector connector = new Ajp13SocketConnector();
                    setConnectorOptions(connector, props);
                    server.addConnector(connector);
                }

            } else if ("request-log".equals(props.value)) {

                RequestLogHandler requestLogHandler = new RequestLogHandler();

                NCSARequestLog requestLog = new NCSARequestLog();

                if (props.getProperty("filename") != null) {
                    requestLog.setFilename(props.getProperty("filename").value);
                }

                if (props.getProperty("append") != null) {
                    requestLog.setAppend("true".equalsIgnoreCase(props.getProperty("append").value));
                }

                if (props.getProperty("extended") != null) {
                    requestLog.setExtended("true".equalsIgnoreCase(props.getProperty("extended").value));
                }

                if (props.getProperty("timezone") != null) {
                    requestLog.setLogTimeZone(props.getProperty("timezone").value);
                }

                if (props.getProperty("date-format") != null) {
                    requestLog.setLogDateFormat(props.getProperty("date-format").value);
                }

                if (props.getProperty("retain-days") != null) {
                    int days = 90;
                    try {
                        days = Integer.parseInt(props.getProperty("retain-days").value);
                    } catch (NumberFormatException e) {
                        days = 90;
                    }
                    requestLog.setRetainDays(days);
                }

                requestLogHandler.setRequestLog(requestLog);
                server.addHandler(requestLogHandler);
            }
        }

        return server;
    }

    private void setConnectorOptions(AbstractConnector connector, ContainerConfig.Container.Property props) throws ContainerException {

        String systemHost = null;
        if ("default".equals(props.getProperty("type").value)) {
            systemHost = System.getProperty(props.name + ".host");
        }
        if (props.getProperty("host") != null && systemHost == null) {
            connector.setHost(props.getProperty("host").value);
        } else {
            String host = "0.0.0.0";
            if (systemHost != null) {
                host = systemHost;
            }
            connector.setHost(host);
        }

        String systemPort = null;
        if ("default".equals(props.getProperty("type").value)) {
            systemPort = System.getProperty(props.name + ".port");
        }
        if (props.getProperty("port") != null && systemPort == null) {
            int value = 8080;
            try {
                value = Integer.parseInt(props.getProperty("port").value);
            } catch (NumberFormatException e) {
                value = 8080;
            }
            if (value == 0) value = 8080;

            connector.setPort(value);
        } else {
            int port = 8080;
            if (systemPort != null) {
                try {
                    port = Integer.parseInt(systemPort);
                } catch (NumberFormatException e) {
                    port = 8080;
                }
            }
            connector.setPort(port);
        }

        if (props.getProperty("buffer-size") != null) {
            int value = 0;
            try {
                value = Integer.parseInt(props.getProperty("buffer-size").value);
            } catch (NumberFormatException e) {
                value = 0;
            }
            if (value > 0) {
                connector.setResponseBufferSize(value);
            }
        }

        if (props.getProperty("linger-time") != null) {
            int value = 0;
            try {
                value = Integer.parseInt(props.getProperty("linger-time").value);
            } catch (NumberFormatException e) {
                value = 0;
            }
            if (value > 0) {
                connector.setSoLingerTime(value);
            }
        }

        if (props.getProperty("low-resource-max-idle-time") != null) {
            int value = 0;
            try {
                value = Integer.parseInt(props.getProperty("low-resource-max-idle-time").value);
            } catch (NumberFormatException e) {
                value = 0;
            }
            if (value > 0) {
                connector.setLowResourceMaxIdleTime(value);
            }
        }


        BoundedThreadPool threadPool = new BoundedThreadPool();

        if (props.getProperty("min-threads") != null) {
            int value = 0;
            try {
                value = Integer.parseInt(props.getProperty("min-threads").value);
            } catch (NumberFormatException e) {
                value = 0;
            }
            if (value > 0) {
                threadPool.setMinThreads(value);
            }
        }

        if (props.getProperty("max-threads") != null) {
            int value = 0;
            try {
                value = Integer.parseInt(props.getProperty("max-threads").value);
            } catch (NumberFormatException e) {
                value = 0;
            }
            if (value > 0) {
                threadPool.setMaxThreads(value);
            }
        }

        if (props.getProperty("max-idle-time") != null) {
            int value = 0;
            try {
                value = Integer.parseInt(props.getProperty("max-idle-time").value);
            } catch (NumberFormatException e) {
                value = 0;
            }
            if (value > 0) {
                threadPool.setMaxIdleTimeMs(value);
            }
        }

        if (props.getProperty("low-threads") != null) {
            int value = 0;
            try {
                value = Integer.parseInt(props.getProperty("low-threads").value);
            } catch (NumberFormatException e) {
                value = 0;
            }
            if (value > 0) {
                threadPool.setLowThreads(value);
            }
        }

        connector.setThreadPool(threadPool);

    }

    /**
     * @see org.ofbiz.base.container.Container#start()
     */
    public boolean start() throws ContainerException {
        // start the server(s)
        this.initJetty();
        if (servers != null) {
            for (Server server : servers.values()) {
                try {
                    server.start();
                    server.join();
                } catch (Exception e) {
                    Debug.logError(e, "Jetty Server Exception", module);
                    throw new ContainerException(e);
                }
            }
        }
        return true;
    }

    /**
     * @see org.ofbiz.base.container.Container#stop()
     */
    public void stop() throws ContainerException {
        if (servers != null) {
            for (Server server : servers.values()) {
                try {
                    server.stop();
                } catch (Exception e) {
                    Debug.logWarning(e, module);
                }
            }
        }
    }
}
