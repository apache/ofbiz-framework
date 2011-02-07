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
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.parsers.ParserConfigurationException;

import javolution.util.FastList;

import org.apache.catalina.Cluster;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Manager;
import org.apache.catalina.ServerFactory;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.ha.tcp.ReplicationValve;
import org.apache.catalina.ha.tcp.SimpleTcpCluster;
import org.apache.catalina.realm.MemoryRealm;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.Embedded;
import org.apache.catalina.tribes.group.GroupChannel;
import org.apache.catalina.tribes.membership.McastService;
import org.apache.catalina.tribes.transport.MultiPointSender;
import org.apache.catalina.tribes.transport.ReplicationTransmitter;
import org.apache.catalina.tribes.transport.nio.NioReceiver;
import org.apache.catalina.util.ServerInfo;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.RequestDumperValve;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.Http11Protocol;
import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.container.ClassLoaderContainer;
import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerConfig;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.container.ContainerConfig.Container.Property;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.SSLUtil;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

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
 * CatalinaContainer -  Tomcat 5
 *
 */
public class CatalinaContainer implements Container {

    public static final String CATALINA_HOSTS_HOME = System.getProperty("ofbiz.home") + "/framework/catalina/hosts";
    public static final String J2EE_SERVER = "OFBiz Container 3.1";
    public static final String J2EE_APP = "OFBiz";
    public static final String module = CatalinaContainer.class.getName();
    protected static Map<String, String> mimeTypes = new HashMap<String, String>();

    // load the JSSE propertes (set the trust store)
    static {
        SSLUtil.loadJsseProperties();
    }

    protected Delegator delegator = null;
    protected Embedded embedded = null;
    protected Map<String, ContainerConfig.Container.Property> clusterConfig = new HashMap<String, ContainerConfig.Container.Property>();
    protected Map<String, Engine> engines = new HashMap<String, Engine>();
    protected Map<String, Host> hosts = new HashMap<String, Host>();

    protected boolean contextReloadable = false;
    protected boolean crossContext = false;
    protected boolean distribute = false;

    protected boolean enableDefaultMimeTypes = true;

    protected String catalinaRuntimeHome;

    /**
     * @see org.ofbiz.base.container.Container#init(java.lang.String[], java.lang.String)
     */
    public void init(String[] args, String configFile) throws ContainerException {
        // get the container config
        ContainerConfig.Container cc = ContainerConfig.getContainer("catalina-container", configFile);
        if (cc == null) {
            throw new ContainerException("No catalina-container configuration found in container config!");
        }

        // embedded properties
        boolean useNaming = ContainerConfig.getPropertyValue(cc, "use-naming", false);
        //int debug = ContainerConfig.getPropertyValue(cc, "debug", 0);

        // grab some global context settings
        this.delegator = DelegatorFactory.getDelegator(ContainerConfig.getPropertyValue(cc, "delegator-name", "default"));
        this.contextReloadable = ContainerConfig.getPropertyValue(cc, "apps-context-reloadable", false);
        this.crossContext = ContainerConfig.getPropertyValue(cc, "apps-cross-context", true);
        this.distribute = ContainerConfig.getPropertyValue(cc, "apps-distributable", true);

        this.catalinaRuntimeHome = ContainerConfig.getPropertyValue(cc, "catalina-runtime-home", "runtime/catalina");

        // set catalina_home
        System.setProperty("catalina.home", System.getProperty("ofbiz.home") + "/" + this.catalinaRuntimeHome);

        // configure JNDI in the StandardServer
        StandardServer server = (StandardServer) ServerFactory.getServer();
        try {
            server.setGlobalNamingContext(new InitialContext());
        } catch (NamingException e) {
            throw new ContainerException(e);
        }

        // create the instance of Embedded
        embedded = new Embedded();
        embedded.setUseNaming(useNaming);

        // create the engines
        List<ContainerConfig.Container.Property> engineProps = cc.getPropertiesWithValue("engine");
        if (UtilValidate.isEmpty(engineProps)) {
            throw new ContainerException("Cannot load CatalinaContainer; no engines defined!");
        }
        for (ContainerConfig.Container.Property engineProp: engineProps) {
            createEngine(engineProp);
        }

        // load the web applications
        loadComponents();

        // create the connectors
        List<ContainerConfig.Container.Property> connectorProps = cc.getPropertiesWithValue("connector");
        if (UtilValidate.isEmpty(connectorProps)) {
            throw new ContainerException("Cannot load CatalinaContainer; no connectors defined!");
        }
        for (ContainerConfig.Container.Property connectorProp: connectorProps) {
            createConnector(connectorProp);
        }

        try {
            embedded.initialize();
        } catch (LifecycleException e) {
            throw new ContainerException(e);
        }
    }

    public boolean start() throws ContainerException {
        // Start the embedded server
        try {
            embedded.start();
        } catch (LifecycleException e) {
            throw new ContainerException(e);
        }

        for (Connector con: embedded.findConnectors()) {
            ProtocolHandler ph = con.getProtocolHandler();
            if (ph instanceof Http11Protocol) {
                Http11Protocol hph = (Http11Protocol) ph;
                Debug.logInfo("Connector " + hph.getProtocols() + " @ " + hph.getPort() + " - " +
                    (hph.getSecure() ? "secure" : "not-secure") + " [" + con.getProtocolHandlerClassName() + "] started.", module);
            } else {
                Debug.logInfo("Connector " + con.getProtocol() + " @ " + con.getPort() + " - " +
                    (con.getSecure() ? "secure" : "not-secure") + " [" + con.getProtocolHandlerClassName() + "] started.", module);
            }
        }
        Debug.logInfo("Started " + ServerInfo.getServerInfo(), module);
        return true;
    }

    protected Engine createEngine(ContainerConfig.Container.Property engineConfig) throws ContainerException {
        if (embedded == null) {
            throw new ContainerException("Cannot create Engine without Embedded instance!");
        }

        ContainerConfig.Container.Property defaultHostProp = engineConfig.getProperty("default-host");
        if (defaultHostProp == null) {
            throw new ContainerException("default-host element of server property is required for catalina!");
        }

        String engineName = engineConfig.name;
        String hostName = defaultHostProp.value;

        StandardEngine engine = (StandardEngine) embedded.createEngine();
        engine.setName(engineName);
        engine.setDefaultHost(hostName);

        // set the JVM Route property (JK/JK2)
        String jvmRoute = ContainerConfig.getPropertyValue(engineConfig, "jvm-route", null);
        if (jvmRoute != null) {
            engine.setJvmRoute(jvmRoute);
        }

        // create the default realm -- TODO: make this configurable
        String dbConfigPath = "catalina-users.xml";
        MemoryRealm realm = new MemoryRealm();
        realm.setPathname(dbConfigPath);
        engine.setRealm(realm);

        // cache the engine
        engines.put(engine.getName(), engine);

        // create a default virtual host; others will be created as needed
        Host host = createHost(engine, hostName);
        hosts.put(engineName + "._DEFAULT", host);

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

        // request dumper valve
        boolean enableRequestDump = ContainerConfig.getPropertyValue(engineConfig, "enable-request-dump", false);
        if (enableRequestDump) {
            RequestDumperValve rdv = new RequestDumperValve();
            engine.addValve(rdv);
        }

        // configure the CrossSubdomainSessionValve
        boolean enableSessionValve = ContainerConfig.getPropertyValue(engineConfig, "enable-cross-subdomain-sessions", false);
        if (enableSessionValve) {
            CrossSubdomainSessionValve sessionValve = new CrossSubdomainSessionValve();
            engine.addValve(sessionValve);
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
            engine.addValve(sslAcceleratorValve);
        }


        String alp2 = ContainerConfig.getPropertyValue(engineConfig, "access-log-pattern", null);
        if (al != null && !UtilValidate.isEmpty(alp2)) {
            al.setPattern(alp2);
        }

        String alp3 = ContainerConfig.getPropertyValue(engineConfig, "access-log-prefix", null);
        if (al != null && !UtilValidate.isEmpty(alp3)) {
            al.setPrefix(alp3);
        }


        boolean alp4 = ContainerConfig.getPropertyValue(engineConfig, "access-log-resolve", true);
        if (al != null) {
            al.setResolveHosts(alp4);
        }

        boolean alp5 = ContainerConfig.getPropertyValue(engineConfig, "access-log-rotate", false);
        if (al != null) {
            al.setRotatable(alp5);
        }

        if (al != null) {
            engine.addValve(al);
        }

        embedded.addEngine(engine);
        return engine;
    }

    protected Host createHost(Engine engine, String hostName) throws ContainerException {
        if (embedded == null) {
            throw new ContainerException("Cannot create Host without Embedded instance!");
        }

        Host host = embedded.createHost(hostName, CATALINA_HOSTS_HOME);
        host.setDeployOnStartup(true);
        host.setAutoDeploy(true);
        host.setRealm(engine.getRealm());
        engine.addChild(host);
        hosts.put(engine.getName() + hostName, host);

        return host;
    }

    protected Cluster createCluster(ContainerConfig.Container.Property clusterProps, Host host) throws ContainerException {
        String defaultValveFilter = ".*.gif;.*.js;.*.jpg;.*.htm;.*.html;.*.txt;";

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
        if (embedded == null) {
            throw new ContainerException("Cannot create Connector without Embedded instance!");
        }

        // need some standard properties
        String protocol = ContainerConfig.getPropertyValue(connectorProp, "protocol", "HTTP/1.1");
        String address = ContainerConfig.getPropertyValue(connectorProp, "address", "0.0.0.0");
        int port = ContainerConfig.getPropertyValue(connectorProp, "port", 0);
        boolean secure = ContainerConfig.getPropertyValue(connectorProp, "secure", false);
        if (protocol.toLowerCase().startsWith("ajp")) {
            protocol = "ajp";
        } else if ("memory".equals(protocol.toLowerCase())) {
            protocol = "memory";
        } else if (secure) {
            protocol = "https";
        } else {
            protocol = "http";
        }

        Connector connector = null;
        if (UtilValidate.isNotEmpty(connectorProp.properties)) {
            connector = embedded.createConnector(address, port, protocol);
            try {
                for (ContainerConfig.Container.Property prop: connectorProp.properties.values()) {
                    connector.setProperty(prop.name, prop.value);
                    //connector.setAttribute(prop.name, prop.value);
                }
                embedded.addConnector(connector);
            } catch (Exception e) {
                throw new ContainerException(e);
            }
        }
        return connector;
    }

    protected Context createContext(ComponentConfig.WebappInfo appInfo) throws ContainerException {
        // webapp settings
        Map<String, String> initParameters = appInfo.getInitParameters();
        List<String> virtualHosts = appInfo.getVirtualHosts();
        Engine engine = engines.get(appInfo.server);
        if (engine == null) {
            Debug.logWarning("Server with name [" + appInfo.server + "] not found; not mounting [" + appInfo.name + "]", module);
            return null;
        }

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

        // configure persistent sessions
        Property clusterProp = clusterConfig.get(engine.getName());

        Manager sessionMgr = null;
        if (clusterProp != null) {
            String mgrClassName = ContainerConfig.getPropertyValue(clusterProp, "manager-class", "org.apache.catalina.ha.session.DeltaManager");
            try {
                sessionMgr = (Manager)Class.forName(mgrClassName).newInstance();
            } catch (Exception exc) {
                throw new ContainerException("Cluster configuration requires a valid manager-class property: " + exc.getMessage());
            }
        } else {
            sessionMgr = new StandardManager();
        }

        // create the web application context
        StandardContext context = (StandardContext) embedded.createContext(mount, location);
        context.setJ2EEApplication(J2EE_APP);
        context.setJ2EEServer(J2EE_SERVER);
        context.setLoader(embedded.createLoader(ClassLoaderContainer.getClassLoader()));

        context.setCookies(appInfo.isSessionCookieAccepted());
        context.addParameter("cookies", appInfo.isSessionCookieAccepted() ? "true" : "false");

        context.setDisplayName(appInfo.name);
        context.setDocBase(location);
        context.setAllowLinking(true);

        context.setReloadable(contextReloadable);
        context.setDistributable(distribute);
        context.setCrossContext(crossContext);
        context.setPrivileged(appInfo.privileged);
        context.setManager(sessionMgr);
        context.getServletContext().setAttribute("_serverId", appInfo.server);
        context.getServletContext().setAttribute("componentName", appInfo.componentConfig.getComponentName());

        // create the Default Servlet instance to mount
        StandardWrapper defaultServlet = new StandardWrapper();
        defaultServlet.setServletClass("org.apache.catalina.servlets.DefaultServlet");
        defaultServlet.setServletName("default");
        defaultServlet.setLoadOnStartup(1);
        defaultServlet.addInitParameter("debug", "0");
        defaultServlet.addInitParameter("listing", "true");
        defaultServlet.addMapping("/");
        context.addChild(defaultServlet);
        context.addServletMapping("/", "default");

        // create the Jasper Servlet instance to mount
        StandardWrapper jspServlet = new StandardWrapper();
        jspServlet.setServletClass("org.apache.jasper.servlet.JspServlet");
        jspServlet.setServletName("jsp");
        jspServlet.setLoadOnStartup(1);
        jspServlet.addInitParameter("fork", "false");
        jspServlet.addInitParameter("xpoweredBy", "true");
        jspServlet.addMapping("*.jsp");
        jspServlet.addMapping("*.jspx");
        context.addChild(jspServlet);
        context.addServletMapping("*.jsp", "jsp");

        // default mime-type mappings
        configureMimeTypes(context);

        // set the init parameters
        for (Map.Entry<String, String> entry: initParameters.entrySet()) {
            context.addParameter(entry.getKey(), entry.getValue());
        }

        if (UtilValidate.isEmpty(virtualHosts)) {
            Host host = hosts.get(engine.getName() + "._DEFAULT");
            context.setRealm(host.getRealm());
            host.addChild(context);
            context.getMapper().setDefaultHostName(host.getName());
        } else {
            // assume that the first virtual-host will be the default; additional virtual-hosts will be aliases
            Iterator<String> vhi = virtualHosts.iterator();
            String hostName = vhi.next();

            boolean newHost = false;
            Host host = hosts.get(engine.getName() + "." + hostName);
            if (host == null) {
                host = createHost(engine, hostName);
                newHost = true;
            }
            while (vhi.hasNext()) {
                host.addAlias(vhi.next());
            }
            context.setRealm(host.getRealm());
            host.addChild(context);
            context.getMapper().setDefaultHostName(host.getName());

            if (newHost) {
                hosts.put(engine.getName() + "." + hostName, host);
            }
        }

        return context;
    }

    protected void loadComponents() throws ContainerException {
        if (embedded == null) {
            throw new ContainerException("Cannot load web applications without Embedded instance!");
        }

        // load the applications
        List<ComponentConfig.WebappInfo> webResourceInfos = ComponentConfig.getAllWebappResourceInfos();
        List<String> loadedMounts = FastList.newInstance();
        if (webResourceInfos != null) {
            for (int i = webResourceInfos.size(); i > 0; i--) {
                ComponentConfig.WebappInfo appInfo = webResourceInfos.get(i - 1);
                String mount = appInfo.getContextRoot();
                if (!loadedMounts.contains(mount)) {
                    createContext(appInfo);
                    loadedMounts.add(mount);
                } else {
                    appInfo.appBarDisplay = false; // disable app bar display on overrided apps
                    Debug.logInfo("Duplicate webapp mount; not loading : " + appInfo.getName() + " / " + appInfo.getLocation(), module);
                }
            }
        }
    }

    public void stop() throws ContainerException {
        try {
            embedded.stop();
        } catch (LifecycleException e) {
            // don't throw this; or it will kill the rest of the shutdown process
            Debug.logVerbose(e, module); // happens usually when running tests, disabled unless in verbose
        }
    }

    protected void configureMimeTypes(Context context) throws ContainerException {
        Map<String, String> mimeTypes = CatalinaContainer.getMimeTypes();
        if (UtilValidate.isNotEmpty(mimeTypes)) {
            for (Map.Entry<String, String> entry: mimeTypes.entrySet()) {
                context.addMimeMapping(entry.getKey(), entry.getValue());
            }
        }
    }

    protected static synchronized Map<String, String> getMimeTypes() throws ContainerException {
        if (UtilValidate.isNotEmpty(mimeTypes)) {
            return mimeTypes;
        }

        if (mimeTypes == null) mimeTypes = new HashMap<String, String>();
        URL xmlUrl = UtilURL.fromResource("mime-type.xml");

        // read the document
        Document mimeTypeDoc;
        try {
            mimeTypeDoc = UtilXml.readXmlDocument(xmlUrl, true);
        } catch (SAXException e) {
            throw new ContainerException("Error reading the mime-type.xml config file: " + xmlUrl, e);
        } catch (ParserConfigurationException e) {
            throw new ContainerException("Error reading the mime-type.xml config file: " + xmlUrl, e);
        } catch (IOException e) {
            throw new ContainerException("Error reading the mime-type.xml config file: " + xmlUrl, e);
        }

        if (mimeTypeDoc == null) {
            Debug.logError("Null document returned for mime-type.xml", module);
            return null;
        }

        // root element
        Element root = mimeTypeDoc.getDocumentElement();

        // mapppings
        for (Element curElement: UtilXml.childElementList(root, "mime-mapping")) {
            String extension = UtilXml.childElementValue(curElement, "extension");
            String type = UtilXml.childElementValue(curElement, "mime-type");
            mimeTypes.put(extension, type);
        }

        return mimeTypes;
    }
}
