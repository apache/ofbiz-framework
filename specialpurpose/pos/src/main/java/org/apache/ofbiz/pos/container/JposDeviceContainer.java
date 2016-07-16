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
package org.apache.ofbiz.pos.container;

import org.apache.ofbiz.base.container.Container;
import org.apache.ofbiz.base.container.ContainerConfig;
import org.apache.ofbiz.base.container.ContainerException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.pos.device.DeviceLoader;

public class JposDeviceContainer implements Container {

    public static final String module = JposDeviceContainer.class.getName();

    protected String configFile = null;

    private String name;

    public void init(String[] args, String name, String configFile) throws ContainerException {
        this.configFile = configFile;
        this.name = name;
    }


    public boolean start() throws ContainerException {
        ContainerConfig.Container cc = ContainerConfig.getContainer(name, configFile);
        if (cc == null) {
            throw new ContainerException("No jpos.device-container configuration found in container config!");
        }

        // load the devices
        try {
            DeviceLoader.load(cc.properties);
        } catch (GeneralException e) {
            Debug.logInfo("******************************************************", module);
            Debug.logInfo("Please verify that your receipt printer is connected !", module);
            Debug.logInfo("******************************************************", module);
            throw new ContainerException(e);
        }

        return true;
    }

    public void stop() throws ContainerException {
        try {
            DeviceLoader.stop();
        } catch (GeneralException e) {
            // we won't stop the shutdown process here; just log the error
            Debug.logError(e, module);
        }
        Debug.logInfo("JPOS Devices released and closed", module);
    }

    public String getName() {
        return name;
    }
}
