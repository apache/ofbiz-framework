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

import org.apache.ofbiz.base.container.ContainerConfig.Configuration;
import org.apache.ofbiz.base.start.Start;
import org.apache.ofbiz.base.start.StartupCommand;
import org.apache.ofbiz.base.util.RMIExtendedSocketFactory;

/**
 * NamingServiceContainer
 *
 */

public class NamingServiceContainer implements Container {

    private static final String MODULE = NamingServiceContainer.class.getName();

    private String configFileLocation = null;
    private boolean isRunning = false;
    private Registry registry = null;
    private int namingPort = 1099;
    private String namingHost = null;

    private RMIExtendedSocketFactory rmiSocketFactory;

    private String name;

    @Override
    public void init(List<StartupCommand> ofbizCommands, String name, String configFile) throws ContainerException {
        this.name = name;
        this.configFileLocation = configFile;

        Configuration cfg = ContainerConfig.getConfiguration(name);

        // get the naming (JNDI) port

        Configuration.Property port = cfg.getProperty("port");
        if (port.value() != null) {
            try {
                this.namingPort = Integer.parseInt(port.value()) + Start.getInstance().getConfig().getPortOffset();
            } catch (Exception e) {
                throw new ContainerException("Invalid port defined in container [naming-container] configuration or as portOffset; not a valid int");
            }
        }

        // get the naming (JNDI) server
        Configuration.Property host = cfg.getProperty("host");
        if (host != null && host.value() != null) {
            this.namingHost = host.value();
        }

        try {
            rmiSocketFactory = new RMIExtendedSocketFactory(namingHost);
        } catch (UnknownHostException uhEx) {
            throw new ContainerException("Invalid host defined in container [naming-container] configuration; not a valid IP address", uhEx);
        }

    }

    @Override
    public boolean start() throws ContainerException {
        try {
            registry = LocateRegistry.createRegistry(namingPort, rmiSocketFactory, rmiSocketFactory);
        } catch (RemoteException e) {
            throw new ContainerException("Unable to locate naming service", e);
        }

        isRunning = true;
        return isRunning;
    }

    @Override
    public void stop() throws ContainerException {
        if (isRunning) {
            try {
                isRunning = !UnicastRemoteObject.unexportObject(registry, true);
            } catch (NoSuchObjectException e) {
                throw new ContainerException("Unable to shutdown naming registry");
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }
}
