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
package org.apache.ofbiz.service.rmi;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.ofbiz.base.container.Container;
import org.apache.ofbiz.base.container.ContainerConfig;
import org.apache.ofbiz.base.container.ContainerException;
import org.apache.ofbiz.base.start.Start;
import org.apache.ofbiz.base.start.StartupCommand;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceContainer;

/**
 * RMI Service Engine Container / Dispatcher
 */
public class RmiServiceContainer implements Container {

    public static final String module = RmiServiceContainer.class.getName();

    protected RemoteDispatcherImpl remote = null;
    protected String configFile = null;
    protected String name = null;
    private String containerName;
    // Container methods

    @Override
    public void init(List<StartupCommand> ofbizCommands, String name, String configFile) {
        this.containerName = name;
        this.configFile = configFile;
    }

    public boolean start() throws ContainerException {
        // get the container config
        ContainerConfig.Configuration cfg = ContainerConfig.getConfiguration(containerName, configFile);
        ContainerConfig.Configuration.Property initialCtxProp = cfg.getProperty("use-initial-context");
        ContainerConfig.Configuration.Property lookupHostProp = cfg.getProperty("bound-host");
        ContainerConfig.Configuration.Property lookupPortProp = cfg.getProperty("bound-port");
        ContainerConfig.Configuration.Property lookupNameProp = cfg.getProperty("bound-name");
        ContainerConfig.Configuration.Property delegatorProp = cfg.getProperty("delegator-name");
        ContainerConfig.Configuration.Property clientProp = cfg.getProperty("client-factory");
        ContainerConfig.Configuration.Property serverProp = cfg.getProperty("server-factory");

        // check the required lookup-name property
        if (lookupNameProp == null || UtilValidate.isEmpty(lookupNameProp.value)) {
            throw new ContainerException("Invalid lookup-name defined in container configuration");
        } else {
            this.name = lookupNameProp.value;
        }

        // check the required delegator-name property
        if (delegatorProp == null || UtilValidate.isEmpty(delegatorProp.value)) {
            throw new ContainerException("Invalid delegator-name defined in container configuration");
        }

        String useCtx = initialCtxProp == null || initialCtxProp.value == null ? "false" : initialCtxProp.value;
        String host = lookupHostProp == null || lookupHostProp.value == null ? "localhost" : lookupHostProp.value;
        String port = lookupPortProp == null || lookupPortProp.value == null ? "1099" : lookupPortProp.value;
        if (Start.getInstance().getConfig().portOffset != 0) {
            Integer portValue = Integer.valueOf(port);
            portValue += Start.getInstance().getConfig().portOffset;
            port = portValue.toString();
        }                
        String keystore = ContainerConfig.getPropertyValue(cfg, "ssl-keystore", null);
        String ksType = ContainerConfig.getPropertyValue(cfg, "ssl-keystore-type", "JKS");
        String ksPass = ContainerConfig.getPropertyValue(cfg, "ssl-keystore-pass", null);
        String ksAlias = ContainerConfig.getPropertyValue(cfg, "ssl-keystore-alias", null);
        boolean clientAuth = ContainerConfig.getPropertyValue(cfg, "ssl-client-auth", false);

        // setup the factories
        RMIClientSocketFactory csf = null;
        RMIServerSocketFactory ssf = null;

        // get the classloader
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        // load the factories
        if (clientProp != null && UtilValidate.isNotEmpty(clientProp.value)) {
            try {
                Class<?> c = loader.loadClass(clientProp.value);
                csf = (RMIClientSocketFactory) c.newInstance();
            } catch (Exception e) {
                throw new ContainerException(e);
            }
        }
        if (serverProp != null && UtilValidate.isNotEmpty(serverProp.value)) {
            try {
                Class<?> c = loader.loadClass(serverProp.value);
                ssf = (RMIServerSocketFactory) c.newInstance();
            } catch (Exception e) {
                throw new ContainerException(e);
            }
        }

        // set the client auth flag on our custom SSL socket factory
        if (ssf != null && ssf instanceof org.apache.ofbiz.service.rmi.socket.ssl.SSLServerSocketFactory) {
            ((org.apache.ofbiz.service.rmi.socket.ssl.SSLServerSocketFactory) ssf).setNeedClientAuth(clientAuth);
            ((org.apache.ofbiz.service.rmi.socket.ssl.SSLServerSocketFactory) ssf).setKeyStoreAlias(ksAlias);
            if (keystore != null) {
                ((org.apache.ofbiz.service.rmi.socket.ssl.SSLServerSocketFactory) ssf).setKeyStore(keystore, ksType, ksPass);
            }
        }

        // get the delegator for this container
        Delegator delegator = DelegatorFactory.getDelegator(delegatorProp.value);

        // create the LocalDispatcher
        LocalDispatcher dispatcher = ServiceContainer.getLocalDispatcher(name, delegator);

        // create the RemoteDispatcher
        try {
            remote = new RemoteDispatcherImpl(dispatcher, csf, ssf);
        } catch (RemoteException e) {
            throw new ContainerException("Unable to start the RMI dispatcher", e);
        }

        if (!useCtx.equalsIgnoreCase("true")) {
            // bind RMIDispatcher to RMI Naming (Must be JRMP protocol)
            try {
                Naming.rebind("//" + host + ":" + port + "/" + name, remote);
            } catch (RemoteException e) {
                throw new ContainerException("Unable to bind RMIDispatcher to RMI on " + "//host[" + host + "]:port[" + port + "]/name[" + name + "] - with remote=" + remote , e);
            } catch (java.net.MalformedURLException e) {
                throw new ContainerException("Invalid URL for binding", e);
            }
        } else {
            // bind RMIDispatcher to InitialContext (must be RMI protocol not IIOP)
            try {
                InitialContext ic = new InitialContext();
                ic.rebind(name, remote);
            } catch (NamingException e) {
                throw new ContainerException("Unable to bind RMIDispatcher to JNDI", e);
            }

            // check JNDI
            try {
                InitialContext ic = new InitialContext();
                Object o = ic.lookup(name);
                if (o == null) {
                    throw new NamingException("Object came back null");
                }
            } catch (NamingException e) {
                throw new ContainerException("Unable to lookup bound objects", e);
            }
        }

        return true;
    }

    public void stop() throws ContainerException {
        remote.deregister();
    }

    public String getName() {
        return containerName;
    }
}
