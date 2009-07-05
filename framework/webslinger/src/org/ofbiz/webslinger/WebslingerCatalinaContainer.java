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
package org.ofbiz.webslinger;

import java.io.File;

import org.apache.catalina.Engine;
import org.apache.catalina.core.StandardEngine;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.container.ContainerConfig;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.catalina.container.CatalinaContainer;

import org.webslinger.catalina.WebslingerAccessLogValve;

public class WebslingerCatalinaContainer extends CatalinaContainer {
    @Override
    protected Engine createEngine(ContainerConfig.Container.Property engineConfig) throws ContainerException {
        Engine engine = super.createEngine(engineConfig);
        String logDir = ContainerConfig.getPropertyValue(engineConfig, "access-log-dir", null);
        if (logDir == null) return engine;
        WebslingerAccessLogValve al = new WebslingerAccessLogValve();
        if (!logDir.startsWith("/")) logDir = System.getProperty("ofbiz.home") + "/" + logDir;
        File logFile = new File(logDir);
        if (!logFile.isDirectory()) throw new ContainerException("Log directory [" + logDir + "] is not available; make sure the directory is created");
        al.setDirectory(logFile.getAbsolutePath());
        String alp2 = ContainerConfig.getPropertyValue(engineConfig, "access-log-pattern", null);
        if (!UtilValidate.isEmpty(alp2)) al.setPattern(alp2);
        String alp3 = ContainerConfig.getPropertyValue(engineConfig, "access-log-prefix", null);
        if (!UtilValidate.isEmpty(alp3)) al.setPrefix(alp3);
        al.setResolveHosts(ContainerConfig.getPropertyValue(engineConfig, "access-log-resolve", true));
        al.setRotatable(ContainerConfig.getPropertyValue(engineConfig, "access-log-rotate", false));
        ((StandardEngine) engine).addValve(al);
        return engine;
    }
}
