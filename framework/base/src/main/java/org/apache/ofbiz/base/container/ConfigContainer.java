/*
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
 */
package org.apache.ofbiz.base.container;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.ofbiz.base.config.ConfigurationFactory;
import org.apache.ofbiz.base.start.Config;
import org.apache.ofbiz.base.start.Start;
import org.apache.ofbiz.base.start.StartupCommand;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.FileUtil;
import org.apache.ofbiz.base.util.StringUtil;

import static org.apache.ofbiz.base.start.StartupCommandUtil.StartupOption.CONFIG;

import org.apache.ofbiz.base.util.UtilURL;
import org.apache.ofbiz.base.util.UtilValidate;

/**
 * This component inits the unified configuration system.
 * It must run before all other components in order to load all needed configs (like entityengine or serviceengine).
 * Once it is loaded, other container can access the configs that has been built.
 */
public class ConfigContainer implements Container {

    private static final String MODULE = ConfigContainer.class.getName();
    private Config cfg = Start.getInstance().getConfig();

    private String name;

    @Override
    public void init(List<StartupCommand> ofbizCommands, String name, String configFile) throws ContainerException {
        this.name = name;
        Map<String, String> configProps = ofbizCommands.stream()
                .filter(command -> command.getName().equals(CONFIG.getName()))
                .map(StartupCommand::getProperties)
                .findFirst().orElse(Map.of());
        if ((configProps.isEmpty()
                && !cfg.isConfigOverloadEnable())
                || "false".equals(configProps.get("enable"))) {
            Debug.logWarning("[Config override] disabled ", MODULE);
            return;
        }
        Debug.logWarning("[Config override] enabled ", MODULE);

        List<File> hoconFiles = new ArrayList<>(parseFileListToLoadFromStartArguments(configProps.get("file")));
        try {
            List<File> configFilesInConfDir = FileUtil.findFiles("conf", null, "config", null);
            if (!configFilesInConfDir.isEmpty()) {
                hoconFiles.addAll(configFilesInConfDir);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (UtilValidate.isEmpty(hoconFiles)) {
            Debug.logWarning("[Config override] config file loaded", MODULE);
            return;
        }
        if (Debug.warningOn()) {
            hoconFiles.forEach(file -> Debug.logWarning("[Config override] Loading file: " + file, MODULE));
        }
        ConfigurationFactory.resetAndGet(hoconFiles).setUseOverrideValue(true);
    }

    @Override
    public boolean start() throws ContainerException {
        return true;
    }

    @Override
    public void stop() throws ContainerException {
    }

    @Override
    public String getName() {
        return name;
    }

    private static List<File> parseFileListToLoadFromStartArguments(String fileProp) throws ContainerException {
        List<String> fileNames = new ArrayList<>();
        Optional.ofNullable(fileProp).ifPresent(props -> fileNames.addAll(StringUtil.split(props, ",")));

        List<URL> fileUrls = new ArrayList<>();
        for (String file : fileNames) {
            URL url = UtilURL.fromResource(file);
            if (url == null) {
                throw new ContainerException("Unable to locate config file: " + file);
            } else {
                fileUrls.add(url);
            }
        }
        return fileUrls.stream().map(url -> {
            try {
                return new File(url.toURI());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }

}
