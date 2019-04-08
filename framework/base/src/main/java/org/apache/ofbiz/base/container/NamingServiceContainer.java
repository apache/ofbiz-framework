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
package org.apache.ofbiz.base.container;

import java.net.UnknownHostException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

import org.apache.ofbiz.base.start.Start;
import org.apache.ofbiz.base.start.StartupCommand;
import org.apache.ofbiz.base.util.RMIExtendedSocketFactory;

/**
 * NamingServiceContainer
 *
 */

public class NamingServiceContainer implements Container {

    public static final String module = NamingServiceContainer.class.getName();

    protected String configFileLocation = null;
    protected boolean isRunning = false;
    protected Registry registry = null;
    protected int namingPort = 1099;
    protected String namingHost = null;

    protected RMIExtendedSocketFactory rmiSocketFactory;

    private String name;

    public void init(List<StartupCommand> ofbizCommands, String name, String configFile) throws ContainerException {
        this.name =name;
        this.configFileLocation = configFile;

        ContainerConfig.Configuration cfg = ContainerConfig.getConfiguration(name, configFileLocation);

        // get the naming (JNDI) port
        
        ContainerConfig.Configuration.Property port = cfg.getProperty("port");
        if (port.value != null) {
            try {
                this.namingPort = Integer.parseInt(port.value) + Start.getInstance().getConfig().portOffset;
            } catch (Exception e) {
                throw new ContainerException("Invalid port defined in container [naming-container] configuration or as portOffset; not a valid int");
            }
        }

        // get the naming (JNDI) server
        ContainerConfig.Configuration.Property host = cfg.getProperty("host");
        if (host != null && host.value != null) {
            this.namingHost =  host.value ;
        }

        try {
            rmiSocketFactory = new RMIExtendedSocketFactory( namingHost );
        } catch ( UnknownHostException uhEx ) {
            throw new ContainerException("Invalid host defined in container [naming-container] configuration; not a valid IP address", uhEx);
        }

    }

    public boolean start() throws ContainerException {
        try {
            registry = LocateRegistry.createRegistry(namingPort, rmiSocketFactory, rmiSocketFactory);
        } catch (RemoteException e) {
            throw new ContainerException("Unable to locate naming service", e);
        }

        isRunning = true;
        return isRunning;
    }

    public void stop() throws ContainerException {
        if (isRunning) {
            try {
                isRunning = !UnicastRemoteObject.unexportObject(registry, true);
            } catch (NoSuchObjectException e) {
                throw new ContainerException("Unable to shutdown naming registry");
            }
        }
    }

    public String getName() {
        return name;
    }
}
