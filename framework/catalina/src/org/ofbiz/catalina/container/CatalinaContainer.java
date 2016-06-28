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
package org.ofbiz.catalina.container;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.catalina.Cluster;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Globals;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Manager;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.filters.RequestDumperFilter;
import org.apache.catalina.ha.tcp.ReplicationValve;
import org.apache.catalina.ha.tcp.SimpleTcpCluster;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.tribes.group.GroupChannel;
import org.apache.catalina.tribes.membership.McastService;
import org.apache.catalina.tribes.transport.MultiPointSender;
import org.apache.catalina.tribes.transport.ReplicationTransmitter;
import org.apache.catalina.tribes.transport.nio.NioReceiver;
import org.apache.catalina.util.ServerInfo;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.util.IntrospectionUtils;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.concurrent.ExecutionPool;
import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerConfig;
import org.ofbiz.base.container.ContainerConfig.Container.Property;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.start.Start;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.SSLUtil;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.w3c.dom.Document;

/*
 * --- Access Log Pattern Information - From Tomcat 5 AccessLogValve.java
 * <p>Patterns for the logged message may include constant text or any of the
 * following replacement strings, for which the corresponding information
 * from the specified Response is substituted:</p>
 * <ul>
 * <li><b>%a</b> - Remote IP address
 * <li><b>%A</b> - Local IP address
 * <li><b>%b</b> - Bytes sent, excluding HTTP headers, or '-' if no bytes
 *     were sent
 * <li><b>%B</b> - Bytes sent, excluding HTTP headers
 * <li><b>%h</b> - Remote host name
 * <li><b>%H</b> - Request protocol
 * <li><b>%l</b> - Remote logical username from identd (always returns '-')
 * <li><b>%m</b> - Request method
 * <li><b>%p</b> - Local port
 * <li><b>%q</b> - Query string (prepended with a '?' if it exists, otherwise
 *     an empty string
 * <li><b>%r</b> - First line of the request
 * <li><b>%s</b> - HTTP status code of the response
 * <li><b>%S</b> - User session ID
 * <li><b>%t</b> - Date and time, in Common Log Format format
 * <li><b>%u</b> - Remote user that was authenticated
 * <li><b>%U</b> - Requested URL path
 * <li><b>%v</b> - Local server name
 * <li><b>%D</b> - Time taken to process the request, in millis
 * <li><b>%T</b> - Time taken to process the request, in seconds
 * </ul>
 * <p>In addition, the caller can specify one of the following aliases for
 * commonly utilized patterns:</p>
 * <ul>
 * <li><b>common</b> - <code>%h %l %u %t "%r" %s %b</code>
 * <li><b>combined</b> -
 *   <code>%h %l %u %t "%r" %s %b "%{Referer}i" "%{User-Agent}i"</code>
 * </ul>
 *
 * <p>
 * There is also support to write information from the cookie, incoming
 * header, the Session or something else in the ServletRequest.<br/>
 * It is modeled after the apache syntax:
 * <ul>
 * <li><code>%{xxx}i</code> for incoming headers
 * <li><code>%{xxx}c</code> for a specific cookie
 * <li><code>%{xxx}r</code> xxx is an attribute in the ServletRequest
 * <li><code>%{xxx}s</code> xxx is an attribute in the HttpSession
 * </ul>
 * </p>
 */

/**
 * CatalinaContainer -  Tomcat
 *
 */
public class CatalinaContainer implements Container {

    public static final String CATALINA_HOSTS_HOME = System.getProperty("ofbiz.home") + "/framework/catalina/hosts";
    public static final String J2EE_SERVER = "OFBiz Container 3.1";
    public static final String J2EE_APP = "OFBiz";
    public static final String module = CatalinaContainer.class.getName();
    private static final ThreadGroup CATALINA_THREAD_GROUP = new ThreadGroup("CatalinaContainer");

    // load the JSSE properties (set the trust store)
    static {
        SSLUtil.loadJsseProperties();
    }

    private Tomcat tomcat = null;
    protected Map<String, ContainerConfig.Container.Property> clusterConfig = new HashMap<String, ContainerConfig.Container.Property>();

    protected boolean contextReloadable = false;
    protected boolean crossContext = false;
    protected boolean distribute = false;

    protected String catalinaRuntimeHome;

    private String name;

    @Override
    public void init(String[] args, String name, String configFile) throws ContainerException {
        this.name = name;
        // get the container config
        ContainerConfig.Container cc = ContainerConfig.getContainer(name, configFile);
        if (cc == null) {
            throw new ContainerException("No catalina-container configuration found in container config!");
        }

        // embedded properties
        boolean useNaming = ContainerConfig.getPropertyValue(cc, "use-naming", false);
        //int debug = ContainerConfig.getPropertyValue(cc, "debug", 0);

        // grab some global context settings
        this.contextReloadable = ContainerConfig.getPropertyValue(cc, "apps-context-reloadable", false);
        this.crossContext = ContainerConfig.getPropertyValue(cc, "apps-cross-context", true);
        this.distribute = ContainerConfig.getPropertyValue(cc, "apps-distributable", true);

        this.catalinaRuntimeHome = ContainerConfig.getPropertyValue(cc, "catalina-runtime-home", "runtime/catalina");

        // set catalina_home
        System.setProperty(Globals.CATALINA_HOME_PROP, System.getProperty("ofbiz.home") + "/" + this.catalinaRuntimeHome);
        System.setProperty(Globals.CATALINA_BASE_PROP, System.getProperty(Globals.CATALINA_HOME_PROP));

        // create the instance of embedded Tomcat
        System.setProperty("catalina.useNaming", String.valueOf(useNaming));
        tomcat = new Tomcat();
        tomcat.setBaseDir(System.getProperty("ofbiz.home"));

        // configure JNDI in the StandardServer
        StandardServer server = (StandardServer) tomcat.getServer();
        if (useNaming) {
            tomcat.enableNaming();
        }
        try {
            server.setGlobalNamingContext(new InitialContext());
        } catch (NamingException e) {
            throw new ContainerException(e);
        }

        // create the engine
        List<ContainerConfig.Container.Property> engineProps = cc.getPropertiesWithValue("engine");
        if (UtilValidate.isEmpty(engineProps)) {
            throw new ContainerException("Cannot load CatalinaContainer; no engines defined.");
        }
        if (engineProps.size() > 1) {
            throw new ContainerException("Cannot load CatalinaContainer; more than one engine configuration found; only one is supported.");
        }
        createEngine(engineProps.get(0));

        // create the connectors
        List<ContainerConfig.Container.Property> connectorProps = cc.getPropertiesWithValue("connector");
        if (UtilValidate.isEmpty(connectorProps)) {
            throw new ContainerException("Cannot load CatalinaContainer; no connectors defined!");
        }
        for (ContainerConfig.Container.Property connectorProp: connectorProps) {
            createConnector(connectorProp);
        }
    }

    public boolean start() throws ContainerException {
        // load the web applications
        loadComponents();

        // Start the Tomcat server
        try {
            tomcat.getServer().start();
        } catch (LifecycleException e) {
            throw new ContainerException(e);
        }

        for (Connector con: tomcat.getService().findConnectors()) {
            Debug.logInfo("Connector " + con.getProtocol() + " @ " + con.getPort() + " - " +
                (con.getSecure() ? "secure" : "not-secure") + " [" + con.getProtocolHandlerClassName() + "] started.", module);
        }
        Debug.logInfo("Started " + ServerInfo.getServerInfo(), module);
        return true;
    }

    private Engine createEngine(ContainerConfig.Container.Property engineConfig) throws ContainerException {
        if (tomcat == null) {
            throw new ContainerException("Cannot create Engine without Tomcat instance!");
        }

        ContainerConfig.Container.Property defaultHostProp = engineConfig.getProperty("default-host");
        if (defaultHostProp == null) {
            throw new ContainerException("default-host element of server property is required for catalina!");
        }

        String engineName = engineConfig.name;
        String hostName = defaultHostProp.value;

        tomcat.setHostname(hostName);
        Engine engine = tomcat.getEngine();
        engine.setName(engineName);

        // set the JVM Route property (JK/JK2)
        String jvmRoute = ContainerConfig.getPropertyValue(engineConfig, "jvm-route", null);
        if (jvmRoute != null) {
            engine.setJvmRoute(jvmRoute);
        }

        // create a default virtual host; others will be created as needed
        Host host = tomcat.getHost();
        configureHost(host);

        // configure clustering
        List<ContainerConfig.Container.Property> clusterProps = engineConfig.getPropertiesWithValue("cluster");
        if (clusterProps != null && clusterProps.size() > 1) {
            throw new ContainerException("Only one cluster configuration allowed per engine");
        }

        if (UtilValidate.isNotEmpty(clusterProps)) {
            ContainerConfig.Container.Property clusterProp = clusterProps.get(0);
            createCluster(clusterProp, host);
            clusterConfig.put(engineName, clusterProp);
        }

        // configure the CrossSubdomainSessionValve
        boolean enableSessionValve = ContainerConfig.getPropertyValue(engineConfig, "enable-cross-subdomain-sessions", false);
        if (enableSessionValve) {
            CrossSubdomainSessionValve sessionValve = new CrossSubdomainSessionValve();
            ((StandardEngine)engine).addValve(sessionValve);
        }

        // configure the access log valve
        String logDir = ContainerConfig.getPropertyValue(engineConfig, "access-log-dir", null);
        AccessLogValve al = null;
        if (logDir != null) {
            al = new AccessLogValve();
            if (!logDir.startsWith("/")) {
                logDir = System.getProperty("ofbiz.home") + "/" + logDir;
            }
            File logFile = new File(logDir);
            if (!logFile.isDirectory()) {
                throw new ContainerException("Log directory [" + logDir + "] is not available; make sure the directory is created");
            }
            al.setDirectory(logFile.getAbsolutePath());
        }

        // configure the SslAcceleratorValve
        String sslAcceleratorPortStr = ContainerConfig.getPropertyValue(engineConfig, "ssl-accelerator-port", null);
        if (UtilValidate.isNotEmpty(sslAcceleratorPortStr)) {
            Integer sslAcceleratorPort = Integer.valueOf(sslAcceleratorPortStr);
            SslAcceleratorValve sslAcceleratorValve = new SslAcceleratorValve();
            sslAcceleratorValve.setSslAcceleratorPort(sslAcceleratorPort);
            ((StandardEngine)engine).addValve(sslAcceleratorValve);
        }


        String alp2 = ContainerConfig.getPropertyValue(engineConfig, "access-log-pattern", null);
        if (al != null && !UtilValidate.isEmpty(alp2)) {
            al.setPattern(alp2);
        }

        String alp3 = ContainerConfig.getPropertyValue(engineConfig, "access-log-prefix", null);
        if (al != null && !UtilValidate.isEmpty(alp3)) {
            al.setPrefix(alp3);
        }

        boolean alp5 = ContainerConfig.getPropertyValue(engineConfig, "access-log-rotate", false);
        if (al != null) {
            al.setRotatable(alp5);
        }

        if (al != null) {
            ((StandardEngine)engine).addValve(al);
        }

        return engine;
    }

    private static Host createHost(String hostName) {
        Host host = new StandardHost();
        host.setName(hostName);
        configureHost(host);
        return host;
    }
    private static void configureHost(Host host) {
        host.setAppBase(CATALINA_HOSTS_HOME);
        host.setDeployOnStartup(false);
        host.setBackgroundProcessorDelay(5);
        host.setAutoDeploy(false);
        ((StandardHost)host).setWorkDir(new File(System.getProperty(Globals.CATALINA_HOME_PROP), "work" + File.separator + host.getName()).getAbsolutePath());
    }

    protected Cluster createCluster(ContainerConfig.Container.Property clusterProps, Host host) throws ContainerException {
        String defaultValveFilter = ".*\\.gif;.*\\.js;.*\\.jpg;.*\\.htm;.*\\.html;.*\\.txt;.*\\.png;.*\\.css;.*\\.ico;.*\\.htc;";

        ReplicationValve clusterValve = new ReplicationValve();
        clusterValve.setFilter(ContainerConfig.getPropertyValue(clusterProps, "rep-valve-filter", defaultValveFilter));

        String mcb = ContainerConfig.getPropertyValue(clusterProps, "mcast-bind-addr", null);
        String mca = ContainerConfig.getPropertyValue(clusterProps, "mcast-addr", null);
        int mcp = ContainerConfig.getPropertyValue(clusterProps, "mcast-port", -1);
        int mcf = ContainerConfig.getPropertyValue(clusterProps, "mcast-freq", 500);
        int mcd = ContainerConfig.getPropertyValue(clusterProps, "mcast-drop-time", 3000);

        if (mca == null || mcp == -1) {
            throw new ContainerException("Cluster configuration requires mcast-addr and mcast-port properties");
        }

        McastService mcast = new McastService();
        if (mcb != null) {
            mcast.setMcastBindAddress(mcb);
        }

        mcast.setAddress(mca);
        mcast.setPort(mcp);
        mcast.setMcastDropTime(mcd);
        mcast.setFrequency(mcf);

        String tla = ContainerConfig.getPropertyValue(clusterProps, "tcp-listen-host", "auto");
        int tlp = ContainerConfig.getPropertyValue(clusterProps, "tcp-listen-port", 4001);
        int tlt = ContainerConfig.getPropertyValue(clusterProps, "tcp-sector-timeout", 100);
        int tlc = ContainerConfig.getPropertyValue(clusterProps, "tcp-thread-count", 6);
        //String tls = getPropertyValue(clusterProps, "", "");

        if (tlp == -1) {
            throw new ContainerException("Cluster configuration requires tcp-listen-port property");
        }

        NioReceiver listener = new NioReceiver();
        listener.setAddress(tla);
        listener.setPort(tlp);
        listener.setSelectorTimeout(tlt);
        listener.setMaxThreads(tlc);
        listener.setMinThreads(tlc);
        //listener.setIsSenderSynchronized(false);

        ReplicationTransmitter trans = new ReplicationTransmitter();
        try {
            MultiPointSender mps = (MultiPointSender)Class.forName(ContainerConfig.getPropertyValue(clusterProps, "replication-mode", "org.apache.catalina.tribes.transport.bio.PooledMultiSender")).newInstance();
            trans.setTransport(mps);
        } catch (Exception exc) {
            throw new ContainerException("Cluster configuration requires a valid replication-mode property: " + exc.getMessage());
        }
        String mgrClassName = ContainerConfig.getPropertyValue(clusterProps, "manager-class", "org.apache.catalina.ha.session.DeltaManager");
        //int debug = ContainerConfig.getPropertyValue(clusterProps, "debug", 0);
        // removed since 5.5.9? boolean expireSession = ContainerConfig.getPropertyValue(clusterProps, "expire-session", false);
        // removed since 5.5.9? boolean useDirty = ContainerConfig.getPropertyValue(clusterProps, "use-dirty", true);

        SimpleTcpCluster cluster = new SimpleTcpCluster();
        cluster.setClusterName(clusterProps.name);
        Manager manager = null;
        try {
            manager = (Manager)Class.forName(mgrClassName).newInstance();
        } catch (Exception exc) {
            throw new ContainerException("Cluster configuration requires a valid manager-class property: " + exc.getMessage());
        }
        //cluster.setManagerClassName(mgrClassName);
        //host.setManager(manager);
        //cluster.registerManager(manager);
        cluster.setManagerTemplate((org.apache.catalina.ha.ClusterManager)manager);
        //cluster.setDebug(debug);
        // removed since 5.5.9? cluster.setExpireSessionsOnShutdown(expireSession);
        // removed since 5.5.9? cluster.setUseDirtyFlag(useDirty);

        GroupChannel channel = new GroupChannel();
        channel.setChannelReceiver(listener);
        channel.setChannelSender(trans);
        channel.setMembershipService(mcast);

        cluster.setChannel(channel);
        cluster.addValve(clusterValve);
        // removed since 5.5.9? cluster.setPrintToScreen(true);

        // set the cluster to the host
        host.setCluster(cluster);
        Debug.logInfo("Catalina Cluster [" + cluster.getClusterName() + "] configured for host - " + host.getName(), module);

        return cluster;
    }

    protected Connector createConnector(ContainerConfig.Container.Property connectorProp) throws ContainerException {
        if (tomcat == null) {
            throw new ContainerException("Cannot create Connector without Tomcat instance!");
        }
        Connector connector = null;
        if (UtilValidate.isNotEmpty(connectorProp.properties)) {
            String protocol = ContainerConfig.getPropertyValue(connectorProp, "protocol", "HTTP/1.1");
            int port = ContainerConfig.getPropertyValue(connectorProp, "port", 0) + Start.getInstance().getConfig().portOffset;

            // set the protocol and the port first
            connector = new Connector(protocol);
            connector.setPort(port);
            // then set all the other parameters
            for (ContainerConfig.Container.Property prop: connectorProp.properties.values()) {
                if ("protocol".equals(prop.name) || "port".equals(prop.name)) {
                    // protocol and port are already set
                    continue;
                }
                if (IntrospectionUtils.setProperty(connector, prop.name, prop.value)) {
                    if (prop.name.indexOf("Pass") != -1) {
                        // this property may be a password, do not include its value in the logs
                        Debug.logInfo("Tomcat " + connector + ": set " + prop.name, module);
                    } else {
                        Debug.logInfo("Tomcat " + connector + ": set " + prop.name + "=" + prop.value, module);
                    }
                } else {
                    Debug.logWarning("Tomcat " + connector + ": ignored parameter " + prop.name, module);
                }
            }

            tomcat.getService().addConnector(connector);
        }
        return connector;
    }

    private Callable<Context> createContext(final ComponentConfig.WebappInfo appInfo) throws ContainerException {
        Debug.logInfo("Creating context [" + appInfo.name + "]", module);
        final Engine engine = tomcat.getEngine();

        List<String> virtualHosts = appInfo.getVirtualHosts();
        final Host host;
        if (UtilValidate.isEmpty(virtualHosts)) {
            host = tomcat.getHost();
        } else {
            // assume that the first virtual-host will be the default; additional virtual-hosts will be aliases
            Iterator<String> vhi = virtualHosts.iterator();
            String hostName = vhi.next();

            org.apache.catalina.Container childContainer = engine.findChild(hostName);
            if (childContainer instanceof Host) {
                host = (Host)childContainer;
            } else {
                host = createHost(hostName);
                engine.addChild(host);
            }
            while (vhi.hasNext()) {
                host.addAlias(vhi.next());
            }
        }
        return new Callable<Context>() {
            public Context call() throws ContainerException, LifecycleException {
                StandardContext context = configureContext(engine, host, appInfo);
                host.addChild(context);
                return context;
            }
        };
    }

    private StandardContext configureContext(Engine engine, Host host, ComponentConfig.WebappInfo appInfo) throws ContainerException {
        // webapp settings
        Map<String, String> initParameters = appInfo.getInitParameters();

        // set the root location (make sure we set the paths correctly)
        String location = appInfo.componentConfig.getRootLocation() + appInfo.location;
        location = location.replace('\\', '/');
        if (location.endsWith("/")) {
            location = location.substring(0, location.length() - 1);
        }

        // get the mount point
        String mount = appInfo.mountPoint;
        if (mount.endsWith("/*")) {
            mount = mount.substring(0, mount.length() - 2);
        }

        final String webXmlFilePath = new StringBuilder().append("file:///").append(location).append("/WEB-INF/web.xml").toString();
        boolean appIsDistributable = distribute;
        URL webXmlUrl = null;
        try {
            webXmlUrl = FlexibleLocation.resolveLocation(webXmlFilePath);
        } catch (MalformedURLException e) {
            throw new ContainerException(e);
        }
        File webXmlFile = new File(webXmlUrl.getFile());
        if (webXmlFile.exists()) {
            Document webXmlDoc = null;
            try {
                webXmlDoc = UtilXml.readXmlDocument(webXmlUrl);
            } catch (Exception e) {
                throw new ContainerException(e);
            }
            appIsDistributable = webXmlDoc.getElementsByTagName("distributable").getLength() > 0;
        } else {
            Debug.logInfo(webXmlFilePath + " not found.", module);
        }
        final boolean contextIsDistributable = distribute && appIsDistributable;

        // create the web application context
        StandardContext context = new StandardContext();
        context.setParent(host);
        context.setDocBase(location);
        context.setPath(mount);
        context.addLifecycleListener(new ContextConfig());
        Tomcat.initWebappDefaults(context);
        // configure persistent sessions
        // important: the call to context.setManager(...) must be done after Tomcat.initWebappDefaults(...)
        Property clusterProp = clusterConfig.get(engine.getName());
        if (clusterProp != null && contextIsDistributable) {
            Manager sessionMgr = null;
            String mgrClassName = ContainerConfig.getPropertyValue(clusterProp, "manager-class", "org.apache.catalina.ha.session.DeltaManager");
            try {
                sessionMgr = (Manager)Class.forName(mgrClassName).newInstance();
            } catch (Exception exc) {
                throw new ContainerException("Cluster configuration requires a valid manager-class property: " + exc.getMessage());
            }
            context.setManager(sessionMgr);
        }

        JarScanner jarScanner = context.getJarScanner();
        if (jarScanner instanceof StandardJarScanner) {
            StandardJarScanner standardJarScanner = (StandardJarScanner) jarScanner;
            standardJarScanner.setJarScanFilter(new FilterJars());
            standardJarScanner.setScanClassPath(true);
        }

        context.setJ2EEApplication(J2EE_APP);
        context.setJ2EEServer(J2EE_SERVER);
        context.setLoader(new WebappLoader(Thread.currentThread().getContextClassLoader()));

        context.setDisplayName(appInfo.name);
        context.setDocBase(location);
        
        StandardRoot resources = new StandardRoot(context);
        resources.setAllowLinking(true);
        context.setResources(resources);

        context.setReloadable(contextReloadable);

        context.setDistributable(contextIsDistributable);

        context.setCrossContext(crossContext);
        context.setPrivileged(appInfo.privileged);
        context.getServletContext().setAttribute("_serverId", appInfo.server);
        context.getServletContext().setAttribute("componentName", appInfo.componentConfig.getComponentName());

        // request dumper filter
        String enableRequestDump = initParameters.get("enableRequestDump");
        if ("true".equals(enableRequestDump)) {
            // create the Requester Dumper Filter instance
            FilterDef requestDumperFilterDef = new FilterDef();
            requestDumperFilterDef.setFilterClass(RequestDumperFilter.class.getName());
            requestDumperFilterDef.setFilterName("RequestDumper");
            FilterMap requestDumperFilterMap = new FilterMap();
            requestDumperFilterMap.setFilterName("RequestDumper");
            requestDumperFilterMap.addURLPattern("*");
            context.addFilterMap(requestDumperFilterMap);
        }

        // set the init parameters
        for (Map.Entry<String, String> entry: initParameters.entrySet()) {
            context.addParameter(entry.getKey(), entry.getValue());
        }

        return context;
    }

    protected void loadComponents() throws ContainerException {
        if (tomcat == null) {
            throw new ContainerException("Cannot load web applications without Tomcat instance!");
        }

        // load the applications
        List<ComponentConfig.WebappInfo> webResourceInfos = ComponentConfig.getAllWebappResourceInfos();
        List<String> loadedMounts = new ArrayList<String>();
        if (webResourceInfos == null) {
            return;
        }

        ScheduledExecutorService executor = ExecutionPool.getScheduledExecutor(CATALINA_THREAD_GROUP, "catalina-startup", Runtime.getRuntime().availableProcessors(), 0, true);
        try {
            List<Future<Context>> futures = new ArrayList<Future<Context>>();

            for (int i = webResourceInfos.size(); i > 0; i--) {
                ComponentConfig.WebappInfo appInfo = webResourceInfos.get(i - 1);
                String engineName = appInfo.server;
                List<String> virtualHosts = appInfo.getVirtualHosts();
                String mount = appInfo.getContextRoot();
                List<String> keys = new ArrayList<String>();
                if (virtualHosts.isEmpty()) {
                    keys.add(engineName + ":DEFAULT:" + mount);
                } else {
                    for (String virtualHost: virtualHosts) {
                        keys.add(engineName + ":" + virtualHost + ":" + mount);
                    }
                }
                if (!keys.removeAll(loadedMounts)) {
                    // nothing was removed from the new list of keys; this
                    // means there are no existing loaded entries that overlap
                    // with the new set
                    if (!appInfo.location.isEmpty()) {
                        futures.add(executor.submit(createContext(appInfo)));
                    }
                    loadedMounts.addAll(keys);
                } else {
                    appInfo.setAppBarDisplay(false); // disable app bar display on overridden apps
                    Debug.logInfo("Duplicate webapp mount; not loading : " + appInfo.getName() + " / " + appInfo.getLocation(), module);
                }
            }
            ExecutionPool.getAllFutures(futures);
        } finally {
            executor.shutdown();
        }
    }

    public void stop() throws ContainerException {
        try {
            tomcat.stop();
        } catch (LifecycleException e) {
            // don't throw this; or it will kill the rest of the shutdown process
            Debug.logVerbose(e, module); // happens usually when running tests, disabled unless in verbose
        }
    }

    public String getName() {
        return name;
    }
}
