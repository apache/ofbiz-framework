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

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.mortbay.http.NCSARequestLog;
import org.mortbay.http.SocketListener;
import org.mortbay.http.SunJsseListener;
import org.mortbay.http.ajp.AJP13Listener;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.WebApplicationContext;
import org.mortbay.jetty.servlet.SessionManager;
import org.mortbay.jetty.servlet.HashSessionManager;
import org.mortbay.util.Frame;
import org.mortbay.util.Log;
import org.mortbay.util.LogSink;
import org.mortbay.util.MultiException;
import org.mortbay.util.ThreadedServer;

import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.SSLUtil;
import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.container.ContainerConfig;

/**
 * JettyContainer - Container implementation for Jetty
 * This container depends on the ComponentContainer as well.
 */
public class JettyContainer implements Container {

    public static final String module = JettyContainer.class.getName();

    protected String configFile = null;
    private Map servers = new HashMap();

    /**
     * @see org.ofbiz.base.container.Container#init(java.lang.String[], java.lang.String)
     */
    public void init(String[] args, String configFile) {
        this.configFile = configFile;
    }

    private void initJetty() throws ContainerException {
        // configure JSSE properties
        SSLUtil.loadJsseProperties();

        // configure jetty logging
        Log log = Log.instance();
        log.disableLog();
        Log4jSink sink = new Log4jSink();
        log.add(sink);
        sink.setOptions(UtilURL.fromResource("debug.properties").toExternalForm());
        try {
            sink.start();
        } catch (Exception e) {
            Debug.logWarning(e, module);
        }

        // get the container
        ContainerConfig.Container jc = ContainerConfig.getContainer("jetty-container", configFile);

        // create the servers
        Iterator sci = jc.properties.values().iterator();
        while (sci.hasNext()) {
            ContainerConfig.Container.Property prop = (ContainerConfig.Container.Property) sci.next();
            servers.put(prop.name, createServer(prop));
        }

        // load the applications
        Collection componentConfigs = ComponentConfig.getAllComponents();
        if (componentConfigs != null) {
            Iterator components = componentConfigs.iterator();
            while (components.hasNext()) {
                ComponentConfig component = (ComponentConfig) components.next();
                Iterator appInfos = component.getWebappInfos().iterator();
                while (appInfos.hasNext()) {
                    ComponentConfig.WebappInfo appInfo = (ComponentConfig.WebappInfo) appInfos.next();
                    List virtualHosts = appInfo.getVirtualHosts();
                    Map initParameters = appInfo.getInitParameters();
                    Server server = (Server) servers.get(appInfo.server);
                    if (server == null) {
                        Debug.logWarning("Server with name [" + appInfo.server + "] not found; not mounting [" + appInfo.name + "]", module);
                    } else {
                        try {
                            // set the root location (make sure we set the paths correctly)
                            String location = component.getRootLocation() + appInfo.location;
                            location = location.replace('\\', '/');
                            if (!location.endsWith("/")) {
                                location = location + "/";
                            }

                            // load the application
                            WebApplicationContext ctx = server.addWebApplication(appInfo.mountPoint, location);
                            ctx.setAttribute("_serverId", appInfo.server);

                            // set the session manager
                            SessionManager sm = new HashSessionManager();
                            ctx.getWebApplicationHandler().setSessionManager(sm);
                            
                            // set the virtual hosts
                            Iterator vh = virtualHosts.iterator();
                            while (vh.hasNext()) {
                                ctx.addVirtualHost((String)vh.next());
                            }

                            // set the init parameters
                            Iterator ip = initParameters.keySet().iterator();
                            while (ip.hasNext()) {
                                String paramName = (String) ip.next();
                                ctx.setInitParameter(paramName, (String) initParameters.get(paramName));
                            }

                        } catch (IOException e) {
                            Debug.logError(e, "Problem mounting application [" + appInfo.name + " / " + appInfo.location + "]", module);
                        }
                    }
                }
            }
        }
    }

    private Server createServer(ContainerConfig.Container.Property serverConfig) throws ContainerException {
        Server server = new Server();

        // configure the listeners/loggers
        Iterator properties = serverConfig.properties.values().iterator();
        while (properties.hasNext()) {
            ContainerConfig.Container.Property props =
                    (ContainerConfig.Container.Property) properties.next();

            if ("listener".equals(props.value)) {
                if ("default".equals(props.getProperty("type").value)) {
                    SocketListener listener = new SocketListener();
                    setListenerOptions(listener, props);
                    if (props.getProperty("identify-listener") != null) {
                        boolean identifyListener = "true".equalsIgnoreCase(props.getProperty("identify-listener").value);
                        listener.setIdentifyListener(identifyListener);
                    }
                    if (props.getProperty("buffer-size") != null) {
                        int value = 0;
                        try {
                            value = Integer.parseInt(props.getProperty("buffer-size").value);
                        } catch (NumberFormatException e) {
                            value = 0;
                        }
                        if (value > 0) {
                            listener.setBufferSize(value);
                        }
                    }
                    if (props.getProperty("low-resource-persist-time") != null) {
                        int value = 0;
                        try {
                            value = Integer.parseInt(props.getProperty("low-resource-persist-time").value);
                        } catch (NumberFormatException e) {
                            value = 0;
                        }
                        if (value > 0) {
                            listener.setLowResourcePersistTimeMs(value);
                        }
                    }
                    server.addListener(listener);
                } else if ("sun-jsse".equals(props.getProperty("type").value)) {
                    SunJsseListener listener = new SunJsseListener();
                    setListenerOptions(listener, props);
                    if (props.getProperty("keystore") != null) {
                        listener.setKeystore(props.getProperty("keystore").value);
                    }
                    if (props.getProperty("password") != null) {
                        listener.setPassword(props.getProperty("password").value);
                    }
                    if (props.getProperty("key-password") != null) {
                        listener.setKeyPassword(props.getProperty("key-password").value);
                    }
                    if (props.getProperty("need-client-auth") != null) {
                        boolean needClientAuth = "true".equalsIgnoreCase(props.getProperty("need-client-auth").value);
                        listener.setNeedClientAuth(needClientAuth);
                    }
                    if (props.getProperty("identify-listener") != null) {
                        boolean identifyListener = "true".equalsIgnoreCase(props.getProperty("identify-listener").value);
                        listener.setIdentifyListener(identifyListener);
                    }
                    if (props.getProperty("buffer-size") != null) {
                        int value = 0;
                        try {
                            value = Integer.parseInt(props.getProperty("buffer-size").value);
                        } catch (NumberFormatException e) {
                            value = 0;
                        }
                        if (value > 0) {
                            listener.setBufferSize(value);
                        }
                    }
                    if (props.getProperty("low-resource-persist-time") != null) {
                        int value = 0;
                        try {
                            value = Integer.parseInt(props.getProperty("low-resource-persist-time").value);
                        } catch (NumberFormatException e) {
                            value = 0;
                        }
                        if (value > 0) {
                            listener.setLowResourcePersistTimeMs(value);
                        }
                    }
                    server.addListener(listener);
                } else if ("ibm-jsse".equals(props.getProperty("type").value)) {
                    throw new ContainerException("Listener not supported yet [" + props.getProperty("type").value + "]");
                } else if ("nio".equals(props.getProperty("type").value)) {
                    throw new ContainerException("Listener not supported yet [" + props.getProperty("type").value + "]");
                } else if ("ajp13".equals(props.getProperty("type").value)) {
                    AJP13Listener listener = new AJP13Listener();
                    setListenerOptions(listener, props);
                    if (props.getProperty("identify-listener") != null) {
                        boolean identifyListener = "true".equalsIgnoreCase(props.getProperty("identify-listener").value);
                        listener.setIdentifyListener(identifyListener);
                    }
                    if (props.getProperty("buffer-size") != null) {
                        int value = 0;
                        try {
                            value = Integer.parseInt(props.getProperty("buffer-size").value);
                        } catch (NumberFormatException e) {
                            value = 0;
                        }
                        if (value > 0) {
                            listener.setBufferSize(value);
                        }
                    }
                    server.addListener(listener);
                }
            } else if ("request-log".equals(props.value)) {
                NCSARequestLog rl = new NCSARequestLog();

                if (props.getProperty("filename") != null) {
                    rl.setFilename(props.getProperty("filename").value);
                }

                if (props.getProperty("append") != null) {
                    rl.setAppend("true".equalsIgnoreCase(props.getProperty("append").value));
                }

                if (props.getProperty("buffered") != null) {
                    rl.setBuffered("true".equalsIgnoreCase(props.getProperty("buffered").value));
                }

                if (props.getProperty("extended") != null) {
                    rl.setExtended("true".equalsIgnoreCase(props.getProperty("extended").value));
                }

                if (props.getProperty("timezone") != null) {
                    rl.setLogTimeZone(props.getProperty("timezone").value);
                }

                if (props.getProperty("date-format") != null) {
                    rl.setLogDateFormat(props.getProperty("date-format").value);
                }

                if (props.getProperty("retain-days") != null) {
                    int days = 90;
                    try {
                        days = Integer.parseInt(props.getProperty("retain-days").value);
                    } catch (NumberFormatException e) {
                        days = 90;
                    }
                    rl.setRetainDays(days);
                }
                server.setRequestLog(rl);
            }
        }
        return server;
    }

    private void setListenerOptions(ThreadedServer listener, ContainerConfig.Container.Property listenerProps) throws ContainerException {
        String systemHost = null;
        if ("default".equals(listenerProps.getProperty("type").value)) {
            systemHost = System.getProperty(listenerProps.name + ".host");
        }
        if (listenerProps.getProperty("host") != null && systemHost == null) {
            try {
                listener.setHost(listenerProps.getProperty("host").value);
            } catch (UnknownHostException e) {
                throw new ContainerException(e);
            }
        } else {
            String host = "0.0.0.0";
            if (systemHost != null) {
                host = systemHost;
            }
            try {
                listener.setHost(host);
            } catch (UnknownHostException e) {
                throw new ContainerException(e);
            }
        }

        String systemPort = null;
        if ("default".equals(listenerProps.getProperty("type").value)) {
            systemPort = System.getProperty(listenerProps.name + ".port");
        }
        if (listenerProps.getProperty("port") != null && systemPort == null) {
            int value = 8080;
            try {
                value = Integer.parseInt(listenerProps.getProperty("port").value);
            } catch (NumberFormatException e) {
                value = 8080;
            }
            if (value == 0) value = 8080;

            listener.setPort(value);
        } else {
            int port = 8080;
            if (systemPort != null) {
                try {
                    port = Integer.parseInt(systemPort);
                } catch (NumberFormatException e) {
                    port = 8080;
                }
            }
            listener.setPort(port);
        }

        if (listenerProps.getProperty("min-threads") != null) {
            int value = 0;
            try {
                value = Integer.parseInt(listenerProps.getProperty("min-threads").value);
            } catch (NumberFormatException e) {
                value = 0;
            }
            if (value > 0) {
                listener.setMinThreads(value);
            }
        }

        if (listenerProps.getProperty("max-threads") != null) {
            int value = 0;
            try {
                value = Integer.parseInt(listenerProps.getProperty("max-threads").value);
            } catch (NumberFormatException e) {
                value = 0;
            }
            if (value > 0) {
                listener.setMaxThreads(value);
            }
        }

        if (listenerProps.getProperty("max-idle-time") != null) {
            int value = 0;
            try {
                value = Integer.parseInt(listenerProps.getProperty("max-idle-time").value);
            } catch (NumberFormatException e) {
                value = 0;
            }
            if (value > 0) {
                listener.setMaxIdleTimeMs(value);
            }
        }

        if (listenerProps.getProperty("linger-time") != null) {
            int value = 0;
            try {
                value = Integer.parseInt(listenerProps.getProperty("linger-time").value);
            } catch (NumberFormatException e) {
                value = 0;
            }
            if (value > 0) {
                listener.setLingerTimeSecs(value);
            }
        }
    }

    /**
     * @see org.ofbiz.base.container.Container#start()
     */
    public boolean start() throws ContainerException {
        // start the server(s)
        this.initJetty();
        if (servers != null) {
            Iterator i = servers.values().iterator();
            while (i.hasNext()) {
                Server server = (Server) i.next();
                try {
                    server.start();
                } catch (MultiException e) {
                    Debug.logError(e, "Jetty Server Multi-Exception", module);
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
            Iterator i = servers.values().iterator();
            while(i.hasNext()) {
                Server server = (Server) i.next();
                try {
                    server.stop();
                } catch (InterruptedException e) {
                    Debug.logWarning(e, module);
                }
            }
        }
    }
}

// taken from JettyPlus
class Log4jSink implements LogSink {

    private String _options;
    private transient boolean _started;

    public void setOptions(String filename) {
        _options=filename;
    }

    public String getOptions() {
        return _options;
    }

    public void start() throws Exception {
        _started=true;
    }

    public void stop() {
        _started=false;
    }

    public boolean isStarted() {
        return _started;
    }

    public void log(String tag, Object msg, Frame frame, long time) {
        String method = frame.getMethod();
        int lb = method.indexOf('(');
        int ld = (lb > 0) ? method.lastIndexOf('.', lb) : method.lastIndexOf('.');
        if (ld < 0) ld = lb;
        String class_name = (ld > 0) ? method.substring(0,ld) : method;

        Logger log = Logger.getLogger(class_name);

        Priority priority = Priority.INFO;

        if (Log.DEBUG.equals(tag)) {
            priority = Priority.DEBUG;
        } else if (Log.WARN.equals(tag) || Log.ASSERT.equals(tag)) {
            priority = Priority.ERROR;
        } else if (Log.FAIL.equals(tag)) {
            priority = Priority.FATAL;
        }

        if (!log.isEnabledFor(priority)) {
            return;
        }

        log.log(Log4jSink.class.getName(), priority, "" + msg, null);
    }

    public synchronized void log(String s) {
        Logger.getRootLogger().log("jetty.log4jSink", Priority.INFO, s, null);
    }
}
