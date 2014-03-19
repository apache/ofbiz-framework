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
package org.ofbiz.birt.container;

import java.io.File;
import java.util.logging.Level;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.report.IBirtConstants;
import org.eclipse.birt.report.engine.api.EngineConfig;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportEngineFactory;
import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerConfig;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.birt.BirtFactory;

public class BirtContainer implements Container {

    public static final String module = BirtContainer.class.getName();
    
    protected String configFile;

    private String name;

    public void init(String[] args, String name, String configFile) throws ContainerException {
        this.name = name;
        this.configFile = configFile;
    }

    /**
     * start container
     */
    public boolean start() throws ContainerException {
        Debug.logInfo("Start BIRT container", module);

        // make sure the subclass sets the config name
        if (getName() == null) {
            throw new ContainerException("Unknown container config name");
        }
        // get the container config
        ContainerConfig.Container cc = ContainerConfig.getContainer(getName(), configFile);
        if (cc == null) {
            throw new ContainerException("No " + getName() + " configuration found in container config!");
        }

        // create engine config
        EngineConfig config = new EngineConfig();
        String ofbizHome = System.getProperty("ofbiz.home");
        config.setTempDir(ofbizHome + File.separatorChar + "runtime" + File.separatorChar + "tempfiles");
        config.setLogConfig(ofbizHome + File.separatorChar + "runtime" + File.separatorChar + "logs", Level.ALL);

        // set system properties
        System.setProperty(IBirtConstants.SYS_PROP_WORKING_PATH, config.getTempDir());

        // startup platform
        try {
            Debug.logInfo("Startup BIRT platform", module);
            Platform.startup(config);
        } catch (BirtException e) {
            throw new ContainerException(e);
        }

        // create report engine
        Debug.logInfo("Create factory object", module);
        IReportEngineFactory factory = (IReportEngineFactory) Platform
              .createFactoryObject(IReportEngineFactory.EXTENSION_REPORT_ENGINE_FACTORY);
        if (factory == null) {
            throw new ContainerException("can not create birt engine factory");
        }
        Debug.logInfo("Create report engine", module);
        IReportEngine engine = factory.createReportEngine(config);
        BirtFactory.setReportEngine(engine);
        
        // print supported formats
        String[] supportedFormats = engine.getSupportedFormats();
        String formatList = null;
        for (String supportedFormat : supportedFormats) {
            if (formatList != null) {
                formatList += ", " + supportedFormat;
            } else {
                formatList = supportedFormat;
            }
        }
        Debug.logInfo("BIRT supported formats: " + formatList, module);
        return false;
    }

    public void stop() throws ContainerException {
    }

    public String getName() {
        return name;
    }
}
