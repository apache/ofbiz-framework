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
package org.apache.ofbiz.catalina.container;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Globals;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Valve;
import org.apache.catalina.authenticator.SingleSignOn;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.filters.RequestDumperFilter;
import org.apache.catalina.ha.ClusterManager;
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
import org.apache.coyote.http2.Http2Protocol;
import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.concurrent.ExecutionPool;
import org.apache.ofbiz.base.container.Container;
import org.apache.ofbiz.base.container.ContainerConfig;
import org.apache.ofbiz.base.container.ContainerConfig.Configuration;
import org.apache.ofbiz.base.container.ContainerConfig.Configuration.Property;
import org.apache.ofbiz.base.container.ContainerException;
import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.start.Start;
import org.apache.ofbiz.base.start.StartupCommand;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.util.IntrospectionUtils;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * CatalinaContainer -  Tomcat
 *
 * For more information about the AccessLogValve pattern visit the
 * <a href="https://tomcat.apache.org/tomcat-8.0-doc/config/valve.html#Access_Log_Valve">Documentation</a>
 */
public class CatalinaContainer implements Container {

    public static final String module = CatalinaContainer.class.getName();

    private String name;
    private Tomcat tomcat;

    @Override
    public void init(List<StartupCommand> ofbizCommands, String name, String configFile) throws ContainerException {

        this.name = name;
        ContainerConfig.Configuration configuration = ContainerConfig.getConfiguration(name, configFile);
        Property engineConfig = retrieveTomcatEngineConfig(configuration);

        // tomcat setup
        tomcat = prepareTomcatServer(configuration, engineConfig);
        Engine engine = prepareTomcatEngine(tomcat, engineConfig);
        Host host = prepareHost(tomcat, null);

        // add realm and valve for Tomcat SSO
        if (EntityUtilProperties.propertyValueEquals("security", "security.login.tomcat.sso", "true")){
            boolean useEncryption = EntityUtilProperties.propertyValueEquals("security", "password.encrypt", "true");
            OFBizRealm ofBizRealm = new OFBizRealm();
            if (useEncryption){
                ofBizRealm.setCredentialHandler(new HashedCredentialHandler());
            } else {
                ofBizRealm.setCredentialHandler(new SimpleCredentialHandler());
            }
            host.setRealm(ofBizRealm);
            ((StandardHost)host).addValve(new SingleSignOn());
        }

        // clustering, valves and connectors setup
        Property clusterProps = prepareTomcatClustering(host, engineConfig);
        prepareTomcatEngineValves(engineConfig).forEach(valve -> ((StandardEngine)engine).addValve(valve));
        prepareTomcatConnectors(configuration).forEach(connector -> tomcat.getService().addConnector(connector));

        loadWebapps(tomcat, configuration, clusterProps);
    }

    public boolean start() throws ContainerException {
        try {
            tomcat.start();
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

    public void stop() {
        try {
            tomcat.stop();
        } catch (LifecycleException e) {
            /* Don't re-throw this exception or it will kill the rest of the shutdown process.
             * Happens usually when running tests. Output disabled unless in verbose */
            Debug.logVerbose(e, module);
        }
    }

    public String getName() {
        return name;
    }

    private Property retrieveTomcatEngineConfig(ContainerConfig.Configuration cc) throws ContainerException {
        List<ContainerConfig.Configuration.Property> engineProps = cc.getPropertiesWithValue("engine");
        if (UtilValidate.isEmpty(engineProps)) {
            throw new ContainerException("Cannot load CatalinaContainer; no engines defined.");
        }
        if (engineProps.size() > 1) {
            throw new ContainerException("Cannot load CatalinaContainer; more than one engine configuration found; only one is supported.");
        }
        return engineProps.get(0);
    }

    private Tomcat prepareTomcatServer(ContainerConfig.Configuration cc,
            ContainerConfig.Configuration.Property engineConfig) throws ContainerException {

        System.setProperty(Globals.CATALINA_HOME_PROP, System.getProperty("ofbiz.home") + "/" +
                    ContainerConfig.getPropertyValue(cc, "catalina-runtime-home", "runtime/catalina"));
        System.setProperty(Globals.CATALINA_BASE_PROP, System.getProperty(Globals.CATALINA_HOME_PROP));

        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(System.getProperty("ofbiz.home"));

        Property defaultHostProp = engineConfig.getProperty("default-host");
        if (defaultHostProp == null) {
            throw new ContainerException("default-host element of server property is required for catalina!");
        }
        tomcat.setHostname(defaultHostProp.value);

        if (ContainerConfig.getPropertyValue(cc, "use-naming", false)) {
            tomcat.enableNaming();
        }

        StandardServer server = (StandardServer) tomcat.getServer();
        try {
            server.setGlobalNamingContext(new InitialContext());
        } catch (NamingException e) {
            throw new ContainerException(e);
        }

        return tomcat;
    }

    private Engine prepareTomcatEngine(Tomcat tomcat, Property engineConfig) {
        Engine engine = tomcat.getEngine();
        engine.setName(engineConfig.name);

        // set the JVM Route property (JK/JK2)
        String jvmRoute = ContainerConfig.getPropertyValue(engineConfig, "jvm-route", null);
        if (jvmRoute != null) {
            engine.setJvmRoute(jvmRoute);
        }

        return engine;
    }

    private Host prepareHost(Tomcat tomcat, List<String> virtualHosts) {
        Host host;

        if (UtilValidate.isEmpty(virtualHosts)) {
            host = tomcat.getHost();
        } else {
            host = prepareVirtualHost(tomcat, virtualHosts);
        }

        host.setAppBase(System.getProperty("ofbiz.home") + "/framework/catalina/hosts");
        host.setDeployOnStartup(false);
        host.setBackgroundProcessorDelay(5);
        host.setAutoDeploy(false);
        ((StandardHost)host).setWorkDir(new File(System.getProperty(Globals.CATALINA_HOME_PROP),
                "work" + File.separator + host.getName()).getAbsolutePath());

        return host;
    }

    private Host prepareVirtualHost(Tomcat tomcat, List<String> virtualHosts) {
        // assume that the first virtual-host will be the default; additional virtual-hosts will be aliases
        String hostName = virtualHosts.get(0);
        Host host;
        Engine engine = tomcat.getEngine();

        org.apache.catalina.Container childContainer = engine.findChild(hostName);
        if (childContainer instanceof Host) {
            host = (Host) childContainer;
        } else {
            host = new StandardHost();
            host.setName(hostName);
            engine.addChild(host);
        }

        virtualHosts.stream()
            .filter(virtualHost -> virtualHost != hostName)
            .forEach(virtualHost -> host.addAlias(virtualHost));

        return host;
    }

    private Property prepareTomcatClustering(Host host, Property engineConfig) throws ContainerException {
        Property clusterProp = null;

        List<Property> clusterProps = engineConfig.getPropertiesWithValue("cluster");
        if (clusterProps.size() > 1) {
            throw new ContainerException("Only one cluster configuration allowed per engine");
        }

        if (UtilValidate.isNotEmpty(clusterProps)) {
            clusterProp = clusterProps.get(0);

            GroupChannel channel = new GroupChannel();
            channel.setChannelReceiver(prepareChannelReceiver(clusterProp));
            channel.setChannelSender(prepareChannelSender(clusterProp));
            channel.setMembershipService(prepareChannelMcastService(clusterProp));

            SimpleTcpCluster cluster = new SimpleTcpCluster();
            cluster.setClusterName(clusterProp.name);
            cluster.setManagerTemplate(prepareClusterManager(clusterProp));
            cluster.setChannel(channel);
            cluster.addValve(prepareClusterValve(clusterProp));

            host.setCluster(cluster);

            Debug.logInfo("Catalina Cluster [" + cluster.getClusterName() + "] configured for host - " + host.getName(), module);
        }
        return clusterProp;
    }

    private NioReceiver prepareChannelReceiver(Property clusterProp) throws ContainerException {
        NioReceiver listener = new NioReceiver();

        String tla = ContainerConfig.getPropertyValue(clusterProp, "tcp-listen-host", "auto");
        int tlp = ContainerConfig.getPropertyValue(clusterProp, "tcp-listen-port", 4001);
        int tlt = ContainerConfig.getPropertyValue(clusterProp, "tcp-sector-timeout", 100);
        int tlc = ContainerConfig.getPropertyValue(clusterProp, "tcp-thread-count", 6);

        if (tlp == -1) {
            throw new ContainerException("Cluster configuration requires tcp-listen-port property");
        }

        listener.setAddress(tla);
        listener.setPort(tlp);
        listener.setSelectorTimeout(tlt);
        listener.setMaxThreads(tlc);
        listener.setMinThreads(tlc);

        return listener;
    }

    private ReplicationTransmitter prepareChannelSender(Property clusterProp) throws ContainerException {
        ReplicationTransmitter trans = new ReplicationTransmitter();
        try {
            MultiPointSender mps = (MultiPointSender)Class.forName(ContainerConfig.getPropertyValue(clusterProp,
                    "replication-mode", "org.apache.catalina.tribes.transport.bio.PooledMultiSender")).getDeclaredConstructor().newInstance();
            trans.setTransport(mps);
        } catch (Exception exc) {
            throw new ContainerException("Cluster configuration requires a valid replication-mode property: " + exc.getMessage());
        }
        return trans;
    }

    private McastService prepareChannelMcastService(Property clusterProp) throws ContainerException {
        McastService mcast = new McastService();

        String mcb = ContainerConfig.getPropertyValue(clusterProp, "mcast-bind-addr", null);
        String mca = ContainerConfig.getPropertyValue(clusterProp, "mcast-addr", null);
        int mcp = ContainerConfig.getPropertyValue(clusterProp, "mcast-port", -1);
        int mcf = ContainerConfig.getPropertyValue(clusterProp, "mcast-freq", 500);
        int mcd = ContainerConfig.getPropertyValue(clusterProp, "mcast-drop-time", 3000);

        if (mca == null || mcp == -1) {
            throw new ContainerException("Cluster configuration requires mcast-addr and mcast-port properties");
        }

        if (mcb != null) {
            mcast.setMcastBindAddress(mcb);
        }

        mcast.setAddress(mca);
        mcast.setPort(mcp);
        mcast.setMcastDropTime(mcd);
        mcast.setFrequency(mcf);

        return mcast;
    }

    private ClusterManager prepareClusterManager(Property clusterProp) throws ContainerException {
        String mgrClassName = ContainerConfig.getPropertyValue(clusterProp, "manager-class", "org.apache.catalina.ha.session.DeltaManager");
        try {
            return (ClusterManager)Class.forName(mgrClassName).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new ContainerException("Cluster configuration requires a valid manager-class property", e);
        }
    }

    private ReplicationValve prepareClusterValve(Property clusterProp) {
        ReplicationValve clusterValve = new ReplicationValve();
        String defaultValveFilter = ".*\\.gif;.*\\.js;.*\\.jpg;.*\\.htm;.*\\.html;.*\\.txt;.*\\.png;.*\\.css;.*\\.ico;.*\\.htc;";
        clusterValve.setFilter(ContainerConfig.getPropertyValue(clusterProp, "rep-valve-filter", defaultValveFilter));
        return clusterValve;
    }

    private List<Valve> prepareTomcatEngineValves(Property engineConfig) throws ContainerException {
        List<Valve> engineValves = new ArrayList<>();

        // configure the CrossSubdomainSessionValve
        if (ContainerConfig.getPropertyValue(engineConfig, "enable-cross-subdomain-sessions", false)) {
            engineValves.add(new CrossSubdomainSessionValve());
        }

        // configure the SslAcceleratorValve
        String sslAcceleratorPortStr = ContainerConfig.getPropertyValue(engineConfig, "ssl-accelerator-port", null);
        if (UtilValidate.isNotEmpty(sslAcceleratorPortStr)) {
            Integer sslAcceleratorPort = Integer.valueOf(sslAcceleratorPortStr);
            SslAcceleratorValve sslAcceleratorValve = new SslAcceleratorValve();
            sslAcceleratorValve.setSslAcceleratorPort(sslAcceleratorPort);
            engineValves.add(sslAcceleratorValve);
        }

        // configure the AccessLogValve
        String logDir = ContainerConfig.getPropertyValue(engineConfig, "access-log-dir", null);
        if (logDir != null) {
            AccessLogValve accessLogValve = new AccessLogValve();

            logDir = logDir.startsWith("/") ? System.getProperty("ofbiz.home") + "/" + logDir : logDir;
            File logFile = new File(logDir);
            if (!logFile.isDirectory()) {
                throw new ContainerException("Log directory [" + logDir + "] is not available; make sure the directory is created");
            }

            accessLogValve.setDirectory(logFile.getAbsolutePath());
            String accessLogPattern = ContainerConfig.getPropertyValue(engineConfig, "access-log-pattern", null);
            if (UtilValidate.isNotEmpty(accessLogPattern)) {
                accessLogValve.setPattern(accessLogPattern);
            }
            String accessLogPrefix = ContainerConfig.getPropertyValue(engineConfig, "access-log-prefix", null);
            if (UtilValidate.isNotEmpty(accessLogPrefix)) {
                accessLogValve.setPrefix(accessLogPrefix);
            }
            accessLogValve.setRotatable(ContainerConfig.getPropertyValue(engineConfig, "access-log-rotate", false));

            engineValves.add(accessLogValve);
        }

        return engineValves;
    }

    private List<Connector> prepareTomcatConnectors(Configuration configuration) throws ContainerException {
        List<Property> connectorProps = configuration.getPropertiesWithValue("connector");
        if (UtilValidate.isEmpty(connectorProps)) {
            throw new ContainerException("Cannot load CatalinaContainer; no connectors defined!");
        }
        return connectorProps.stream()
            .filter(connectorProp -> UtilValidate.isNotEmpty(connectorProp.properties))
            .map(connectorProp -> prepareConnector(connectorProp))
            .collect(Collectors.toList());
    }

    private Connector prepareConnector(Property connectorProp) {
        Connector connector = new Connector(ContainerConfig.getPropertyValue(connectorProp, "protocol", "HTTP/1.1"));
        connector.setPort(ContainerConfig.getPropertyValue(connectorProp, "port", 0) + Start.getInstance().getConfig().portOffset);
        if ("true".equals(ContainerConfig.getPropertyValue(connectorProp, "upgradeProtocol", "false"))) {
            connector.addUpgradeProtocol(new Http2Protocol());
            Debug.logInfo("Tomcat " + connector + ": enabled HTTP/2", module);
        }
        connectorProp.properties.values().stream()
            .filter(prop -> !"protocol".equals(prop.name) && !"upgradeProtocol".equals(prop.name) && !"port".equals(prop.name))
            .forEach(prop -> {
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
            });
        return connector;
    }

    private void loadWebapps(Tomcat tomcat, ContainerConfig.Configuration configuration, Property clusterProp) {
        ScheduledExecutorService executor = ExecutionPool.getScheduledExecutor(new ThreadGroup(module),
                "catalina-startup", Runtime.getRuntime().availableProcessors(), 0, true);
        List<Future<Context>> futures = new ArrayList<>();

        List<ComponentConfig.WebappInfo> webResourceInfos = ComponentConfig.getAllWebappResourceInfos();
        Collections.reverse(webResourceInfos); // allow higher level webapps to override lower ones

        Set<String> webappsMounts = new HashSet<>();
        webResourceInfos.forEach(appInfo -> webappsMounts.addAll(getWebappMounts(appInfo)));

        for (ComponentConfig.WebappInfo appInfo: webResourceInfos) {
            if(webappsMounts.removeAll(getWebappMounts(appInfo))) {
                // webapp is not yet loaded
                if (!appInfo.location.isEmpty()) {
                    futures.add(executor.submit(createCallableContext(tomcat, appInfo, clusterProp, configuration)));
                }
            } else {
                /* webapp is loaded already (overridden). Therefore, disable
                 * app bar display on overridden apps and do not load */
                appInfo.setAppBarDisplay(false);
                Debug.logInfo("Duplicate webapp mount (overridding); not loading : " + appInfo.getName() + " / " + appInfo.getLocation(), module);
            }
        }
        ExecutionPool.getAllFutures(futures);
        executor.shutdown();
    }

    private List<String> getWebappMounts(ComponentConfig.WebappInfo webappInfo) {
        List<String> allAppsMounts = new ArrayList<>();
        String engineName = webappInfo.server;
        String mount = webappInfo.getContextRoot();
        List<String> virtualHosts = webappInfo.getVirtualHosts();
        if (virtualHosts.isEmpty()) {
            allAppsMounts.add(engineName + ":DEFAULT:" + mount);
        } else {
            virtualHosts.forEach(virtualHost -> allAppsMounts.add(engineName + ":" + virtualHost + ":" + mount));
        }
        return allAppsMounts;
    }

    private Callable<Context> createCallableContext(Tomcat tomcat, ComponentConfig.WebappInfo appInfo,
            Property clusterProp, ContainerConfig.Configuration configuration) {

        Debug.logInfo("Creating context [" + appInfo.name + "]", module);
        Host host = prepareHost(tomcat, appInfo.getVirtualHosts());

        return () -> {
            StandardContext context = prepareContext(host, configuration, appInfo, clusterProp);
            host.addChild(context);
            return context;
        };
    }

    private StandardContext prepareContext(Host host, ContainerConfig.Configuration configuration,
            ComponentConfig.WebappInfo appInfo, Property clusterProp) throws ContainerException {

        StandardContext context = new StandardContext();
        Tomcat.initWebappDefaults(context);

        String location = getWebappRootLocation(appInfo);
        boolean contextIsDistributable = isContextDistributable(configuration, location);

        context.setParent(host);
        context.setDocBase(location);
        context.setName(appInfo.name);
        context.setDisplayName(appInfo.name);
        context.setPath(getWebappMountPoint(appInfo));
        context.addLifecycleListener(new ContextConfig());
        context.setJ2EEApplication("OFBiz");
        context.setJ2EEServer("OFBiz Container");
        context.setLoader(new WebappLoader(Thread.currentThread().getContextClassLoader()));
        context.setDocBase(location);
        context.setReloadable(ContainerConfig.getPropertyValue(configuration, "apps-context-reloadable", false));
        context.setDistributable(contextIsDistributable);
        context.setCrossContext(ContainerConfig.getPropertyValue(configuration, "apps-cross-context", true));
        context.setPrivileged(appInfo.privileged);
        context.getServletContext().setAttribute("_serverId", appInfo.server);
        context.getServletContext().setAttribute("componentName", appInfo.componentConfig.getComponentName());

        if (clusterProp != null && contextIsDistributable) {
            context.setManager(prepareClusterManager(clusterProp));
        }

        StandardRoot resources = new StandardRoot(context);
        resources.setAllowLinking(true);
        context.setResources(resources);

        JarScanner jarScanner = context.getJarScanner();
        if (jarScanner instanceof StandardJarScanner) {
            StandardJarScanner standardJarScanner = (StandardJarScanner) jarScanner;
            standardJarScanner.setJarScanFilter(new FilterJars());
            standardJarScanner.setScanClassPath(true);
        }

        Map<String, String> initParameters = appInfo.getInitParameters();
        // request dumper filter
        if ("true".equals(initParameters.get("enableRequestDump"))) {
            FilterDef requestDumperFilterDef = new FilterDef();
            requestDumperFilterDef.setFilterClass(RequestDumperFilter.class.getName());
            requestDumperFilterDef.setFilterName("RequestDumper");
            context.addFilterDef(requestDumperFilterDef);

            FilterMap requestDumperFilterMap = new FilterMap();
            requestDumperFilterMap.setFilterName("RequestDumper");
            requestDumperFilterMap.addURLPattern("*");
            context.addFilterMap(requestDumperFilterMap);
        }

        // set the init parameters
        initParameters.entrySet().forEach(entry -> context.addParameter(entry.getKey(), entry.getValue()));

        return context;
    }

    private String getWebappRootLocation(ComponentConfig.WebappInfo appInfo) {
        String location = appInfo.componentConfig.getRootLocation() + appInfo.location;
        location = location.replace('\\', '/');
        if (location.endsWith("/")) {
            location = location.substring(0, location.length() - 1);
        }
        return location;
    }

    private String getWebappMountPoint(ComponentConfig.WebappInfo appInfo) {
        String mount = appInfo.mountPoint;
        if (mount.endsWith("/*")) {
            mount = mount.substring(0, mount.length() - 2);
        }
        return mount;
    }

    private boolean isContextDistributable(ContainerConfig.Configuration configuration, String location) throws ContainerException {
        String webXmlFilePath = new StringBuilder().append("file:///").append(location).append("/WEB-INF/web.xml").toString();
        boolean appIsDistributable = ContainerConfig.getPropertyValue(configuration, "apps-distributable", true);
        try {
            URL webXmlUrl = FlexibleLocation.resolveLocation(webXmlFilePath);
            File webXmlFile = new File(webXmlUrl.getFile());
            if (webXmlFile.exists()) {
                Document webXmlDoc = UtilXml.readXmlDocument(webXmlUrl);
                return appIsDistributable && webXmlDoc.getElementsByTagName("distributable").getLength() > 0;
            }
            Debug.logInfo(webXmlFilePath + " not found.", module);
            return appIsDistributable;
        } catch (SAXException | ParserConfigurationException | IOException e) {
            throw new ContainerException(e);
        }
    }
}
