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
package org.ofbiz.shark.container;

import java.util.Properties;
import java.util.StringTokenizer;

import org.enhydra.shark.Shark;
import org.enhydra.shark.api.SharkTransaction;
import org.enhydra.shark.api.TransactionException;
import org.enhydra.shark.api.client.wfbase.BaseException;
import org.enhydra.shark.api.client.wfservice.AdminInterface;
import org.enhydra.shark.api.client.wfservice.ConnectFailed;
import org.enhydra.shark.api.client.wfservice.ExecutionAdministration;
import org.enhydra.shark.api.client.wfservice.NotConnected;
import org.enhydra.shark.api.client.wfservice.RepositoryMgr;
import org.enhydra.shark.api.client.wfservice.SharkConnection;
import org.enhydra.shark.corba.poa.SharkCORBAServer;
import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerConfig;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralRuntimeException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;

/**
 * Shark Workflow Engine Container
 */

public class SharkContainer implements Container, Runnable 
{
    public static final String module = SharkContainer.class.getName();

    private static GenericDelegator delegator = null;
    private static LocalDispatcher dispatcher = null;
    private static GenericValue adminUser = null;
    private static String adminPass = null;
    private static Shark shark = null;

    protected String configFile = null;
    private SharkCORBAServer corbaServer = null;
    private Thread orbThread = null;
    
    /**
     * @see org.ofbiz.base.container.Container#init(java.lang.String[], java.lang.String)
     */
    public void init(String[] args, String configFile) {
        this.configFile = configFile;
    }

    public boolean start() throws ContainerException {
        ContainerConfig.Container cfg = ContainerConfig.getContainer("shark-container", configFile);
        ContainerConfig.Container.Property dispatcherProp = cfg.getProperty("dispatcher-name");
        ContainerConfig.Container.Property delegatorProp = cfg.getProperty("delegator-name");
        ContainerConfig.Container.Property adminProp = cfg.getProperty("admin-user");
        ContainerConfig.Container.Property adminPassProp = cfg.getProperty("admin-pass");
        ContainerConfig.Container.Property engineName = cfg.getProperty("engine-name");
        ContainerConfig.Container.Property iiopHost = cfg.getProperty("iiop-host");
        ContainerConfig.Container.Property iiopPort = cfg.getProperty("iiop-port");

        // check the required delegator-name property
        if (delegatorProp == null || delegatorProp.value == null || delegatorProp.value.length() == 0) {
            throw new ContainerException("Invalid delegator-name defined in container configuration");
        }

        // check the required dispatcher-name property
        if (dispatcherProp == null || dispatcherProp.value == null || dispatcherProp.value.length() == 0) {
            throw new ContainerException("Invalid dispatcher-name defined in container configuration");
        }

        // check the required admin-user property
        if (adminProp == null || adminProp.value == null || adminProp.value.length() == 0) {
            throw new ContainerException("Invalid admin-user defined in container configuration");
        }

        if (adminPassProp == null || adminPassProp.value == null || adminPassProp.value.length() == 0) {
            throw new ContainerException("Invalid admin-pass defined in container configuration");
        }

        if (engineName == null || engineName.value == null || engineName.value.length() == 0) {
            throw new ContainerException("Invalid engine-name defined in container configuration");
        }

        // get the delegator and dispatcher objects
        SharkContainer.delegator = GenericDelegator.getGenericDelegator(delegatorProp.value);
        try {
            SharkContainer.dispatcher = GenericDispatcher.getLocalDispatcher(dispatcherProp.value, SharkContainer.delegator);
        } catch (GenericServiceException e) {
            throw new ContainerException(e);
        }

        // get the admin user
        try {
            SharkContainer.adminUser = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", adminProp.value));
        } catch (GenericEntityException e) {
            throw new ContainerException(e);
        }

        // make sure the admin user exists
        if (SharkContainer.adminUser == null) {
            Debug.logWarning("Invalid admin-user; UserLogin not found not starting Shark!", module);
            return false;
        }

        SharkContainer.adminPass = adminPassProp.value;

        // set the Shark configuration
        Properties props = UtilProperties.getProperties("shark.properties");
        Shark.configure(props);

        SharkContainer.shark = Shark.getInstance();
        Debug.logInfo("Started Shark workflow service", module);

        // create the CORBA server and bind to iiop
        if (iiopHost != null && iiopHost.value != null && iiopHost.value.length() > 0) {
            if (iiopPort != null && iiopPort.value != null && iiopPort.value.length() > 0) {
                try {
                    corbaServer = new SharkCORBAServer(engineName.value, iiopHost.value, iiopPort.value, shark);
                    orbThread = new Thread(this);
                    orbThread.setDaemon(false);
                    orbThread.setName(this.getClass().getName());
                    orbThread.start();
                    Debug.logInfo("Started Shark CORBA service", module);
                } catch (IllegalArgumentException e) {
                    throw new ContainerException(e);
                } catch (GeneralRuntimeException e) {
                    throw new ContainerException(e);
                }
            }
        }
        // re-eval current assignments
        ExecutionAdministration exAdmin = SharkContainer.getAdminInterface().getExecutionAdministration();
        try {
            //exAdmin.connect(adminUser.getString("userLoginId"), SharkContainer.adminPass, null, null);
            exAdmin.connect(adminUser.getString("userLoginId"), adminUser.getString("currentPassword"), null, null);
            // this won't work with encrypted passwords: exAdmin.connect(adminUser.getString("userLoginId"), adminUser.getString("currentPassword"), null, null);
            exAdmin.reevaluateAssignments();
            exAdmin.disconnect();
        } catch (ConnectFailed e) {
            String errMsg = "Shark Connection error (if it is a password wrong error, check the admin-pass property in the container config file, probably ofbiz-containers.xml): " + e.toString();
            throw new ContainerException(errMsg, e);
        } catch (NotConnected e) {
            throw new ContainerException(e);
        } catch (BaseException e) {
            throw new ContainerException(e);
        }

        return true;
    }

    public void run() {
        try {
            corbaServer.startCORBAServer();
        } catch (BaseException e) {
            throw new GeneralRuntimeException(e);
        }
    }

    public void stop() throws ContainerException {
        // shut down the dispatcher
        if (dispatcher != null) {
            dispatcher.deregister();
        }

        // shutdown the corba server
        if (corbaServer != null) {
            corbaServer.shutdownORB();
        }
        Debug.logInfo("stop Shark", module);
    }

    // static helper methods
    public static GenericDelegator getDelegator() {
        return SharkContainer.delegator;
    }

    public static LocalDispatcher getDispatcher() {
        return SharkContainer.dispatcher;
    }

    public static GenericValue getAdminUser() {
        return SharkContainer.adminUser;
    }

    public static AdminInterface getAdminInterface() {
        return shark.getAdminInterface();
    }

    public static RepositoryMgr getRepositoryMgr() {
        return shark.getRepositoryManager();
    }

    public static SharkConnection getSharkConntection() {
        return shark.getSharkConnection();
    }

    public static SharkTransaction getTransaction() throws TransactionException {
        return shark.createTransaction();
    }
}
