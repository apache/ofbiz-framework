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
package org.ofbiz.base.container;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.NoSuchObjectException;
import java.rmi.server.UnicastRemoteObject;

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

    public void init(String[] args, String configFile) throws ContainerException {
        this.configFileLocation = configFile;

        ContainerConfig.Container cfg = ContainerConfig.getContainer("naming-container", configFileLocation);

        // get the telnet-port
        ContainerConfig.Container.Property port = cfg.getProperty("port");
        if (port.value != null) {
            try {
                this.namingPort = Integer.parseInt(port.value);
            } catch (Exception e) {
                throw new ContainerException("Invalid port defined in container [naming-container] configuration; not a valid int");
            }
        }

    }

    public boolean start() throws ContainerException {
        try {
            registry = LocateRegistry.createRegistry(namingPort);
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
}
