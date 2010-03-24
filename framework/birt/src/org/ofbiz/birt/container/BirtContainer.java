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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javolution.util.FastMap;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.core.framework.PlatformFileContext;
import org.eclipse.birt.report.engine.api.EngineConfig;
import org.eclipse.birt.report.engine.api.EngineConstants;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportEngineFactory;
import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerConfig;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.FileUtil;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.LocalDispatcher;

public class BirtContainer implements Container {

    public static final String module = BirtContainer.class.getName();

    public final static String CONFIG_FILE = "birt.properties";

    protected EngineConfig config;
    protected String configFile;

    private static IReportEngine engine;
    private static String delegatorGroupHelperName;
    private static String delegatorName;
    private static String dispatcherName;
    private static Delegator delegator;
    private static LocalDispatcher dispatcher;

    public void init(String[] args, String configFile)
            throws ContainerException {
        this.configFile = configFile;
    }

    /**
     * start container
     */
    public boolean start() throws ContainerException {
        Debug.logInfo("Start birt container", module);

        // make sure the subclass sets the config name
        if (this.getContainerConfigName() == null) {
            throw new ContainerException("Unknown container config name");
        }
        // get the container config
        ContainerConfig.Container cc = ContainerConfig.getContainer(this.getContainerConfigName(), configFile);
        if (cc == null) {
            throw new ContainerException("No " + this.getContainerConfigName() + " configuration found in container config!");
        }

        config = new EngineConfig();

        // set osgi config
        Map<String, String> osgiConfig = FastMap.newInstance();
        osgiConfig.put("osgi.configuration.area", new File(System.getProperty("ofbiz.home"), "runtime" + File.separator + "tempfiles").getPath());
        config.setOSGiConfig(osgiConfig);

        HashMap<String, Object> context = UtilGenerics.cast(config.getAppContext());

        // set delegator, dispatcher and security objects to report

        delegatorGroupHelperName = ContainerConfig.getPropertyValue(cc, "delegator-group-helper-name", "org.ofbiz");

        // get the delegator
        delegatorName = ContainerConfig.getPropertyValue(cc, "delegator-name", "default");
        delegator = DelegatorFactory.getDelegator(delegatorName);

        // get the dispatcher
        dispatcher = GenericDispatcher.getLocalDispatcher(dispatcherName, delegator);

        context.put("delegator", delegator);
        context.put("dispatcher", dispatcher);

        // set classloader for engine
        context.put(EngineConstants.APPCONTEXT_CLASSLOADER_KEY, BirtContainer.class.getClassLoader());
        context.put(EngineConstants.WEBAPP_CLASSPATH_KEY, BirtContainer.class.getClassLoader());

        // set log config to show all level in console
        config.setLogConfig(null, Level.ALL);

        // set engine home
        String reportEnginePath = FileUtil.getFile("component://birt/lib/platform").getPath();
        config.setEngineHome(reportEnginePath);
        config.setBIRTHome(reportEnginePath);

        // set OSGi arguments specific in properties
        String argumentsString = UtilProperties.getPropertyValue(BirtContainer.CONFIG_FILE, "birt.osgi.arguments");
        config.setOSGiArguments(argumentsString.split(","));

        // set platform file context
        config.setPlatformContext(new PlatformFileContext(config));
        config.setAppContext(context);

        // startup platform
        try {
            Debug.logInfo("Startup birt platform", module);
            Platform.startup( config );
        } catch ( BirtException e ) {
            throw new ContainerException(e);
        }

        // create report engine
        Debug.logInfo("Create factory object", module);
        IReportEngineFactory factory = (IReportEngineFactory) Platform
              .createFactoryObject( IReportEngineFactory.EXTENSION_REPORT_ENGINE_FACTORY );
        if (factory == null) {
            throw new ContainerException("can not create birt engine factory");
        }
        Debug.logInfo("Create report engine", module);
        engine = factory.createReportEngine( config );
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

    public String getContainerConfigName() {
        return "birt-container";
    }

    public static IReportEngine getReportEngine() {
        return engine;
    }

    public static String getDelegatorGroupHelperName() {
        return delegatorGroupHelperName;
    }

    public static String getDelegatorName() {
        return delegatorName;
    }

    public static String getDispatcherName() {
        return dispatcherName;
    }

    public static Delegator getDelegator() {
        return delegator;
    }

    public static LocalDispatcher getDispatcher() {
        return dispatcher;
    }
}
