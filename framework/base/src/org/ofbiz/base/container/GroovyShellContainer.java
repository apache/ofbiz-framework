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

import org.apache.log4j.Logger;

import org.ofbiz.base.container.ContainerConfig.Container.Property;
import org.ofbiz.base.container.groovy.GroovyService;
import org.ofbiz.base.container.groovy.GroovyShellService;

public class GroovyShellContainer implements Container {

    private static final Logger log = Logger.getLogger(GroovyShellContainer.class);

    private String configFileLocation = null;
    private GroovyService gsh = null;

    /**
     * @see org.ofbiz.base.container.Container#init(java.lang.String[], java.lang.String)
     */
    @Override
    public void init(String[] args, String configFile) {
        configFileLocation = configFile;
    }

    /**
     * @see org.ofbiz.base.container.Container#start()
     */
    @Override
    public boolean start() throws ContainerException {
        int port = getTelnetPort();
        gsh = new GroovyShellService(port);
        gsh.launchInBackground();

        log.info("Started Groovy telnet service on port [" + port + "].");
        log.info("NOTICE: The Groovy service port is not secure. Please protect it.");

        return true;
    }

    /**
     * @see org.ofbiz.base.container.Container#stop()
     */
    @Override
    public void stop() throws ContainerException {
        gsh = null;
    }

    private int getTelnetPort() throws ContainerException {
        ContainerConfig.Container config = ContainerConfig.getContainer("groovyshell-container", configFileLocation);
        Property telnetPort = config.getProperty("telnet-port");
        try {
            return Integer.parseInt(telnetPort.value);
        } catch (NumberFormatException e) {
            throw new ContainerException("Invalid telnet port [" + telnetPort.value + "] defined in container configuration.");
        }
    }
}
