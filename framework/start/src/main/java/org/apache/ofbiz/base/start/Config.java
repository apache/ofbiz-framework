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
package org.apache.ofbiz.base.start;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.TimeZone;

/**
 * OFBiz server parameters needed on system startup and retrieved from
 * one of the properties files in the start component
 */
public final class Config {

    public final String ofbizHome;
    public final InetAddress adminAddress;
    public final String adminKey;
    public final int portOffset;
    public final int adminPort;
    public final String containerConfig;
    public final List<String> loaders;
    public final String logDir;
    public final boolean shutdownAfterLoad;
    public final boolean useShutdownHook;

    Config(List<StartupCommand> ofbizCommands) throws StartupException {

        // fetch OFBiz Properties object
        Properties props = getPropertiesFile(ofbizCommands);;

        // set this class fields
        ofbizHome = getOfbizHome(getProperty(props, "ofbiz.home", "."));
        adminAddress = getAdminAddress(getProperty(props, "ofbiz.admin.host", "127.0.0.1"));
        adminKey = getProperty(props, "ofbiz.admin.key", "so3du5kasd5dn");
        portOffset = getPortOffsetValue(ofbizCommands, "0");
        adminPort = getAdminPort(props, 10523, portOffset); // see INFRA-6790
        containerConfig = getAbsolutePath(props, "ofbiz.container.config",
                "framework/base/config/ofbiz-containers.xml", ofbizHome);
        loaders = Arrays.asList(getProperty(props, "ofbiz.start.loaders", "").split(","));
        logDir = getAbsolutePath(props, "ofbiz.log.dir", "runtime/logs", ofbizHome);
        shutdownAfterLoad = getProperty(props, "ofbiz.auto.shutdown", "false").equalsIgnoreCase("true");
        useShutdownHook = getProperty(props, "ofbiz.enable.hook", "true").equalsIgnoreCase("true");

        System.out.println("Set OFBIZ_HOME to - " + ofbizHome);

        // set system properties
        System.setProperty("ofbiz.home", ofbizHome);
        System.setProperty("java.awt.headless", getProperty(props, "java.awt.headless", "true"));
        System.setProperty("derby.system.home", getProperty(props, "derby.system.home", "runtime/data/derby"));

        // set the default locale
        setDefaultLocale(getProperty(props, "ofbiz.locale.default", ""));

        // set the default timezone
        String tzString = props.getProperty("ofbiz.timeZone.default", TimeZone.getDefault().getID());
        TimeZone.setDefault(TimeZone.getTimeZone(tzString));
    }

    private String getProperty(Properties props, String key, String defaultValue) {
        return Optional.ofNullable(System.getProperty(key))
                .orElse(props.getProperty(key, defaultValue));
    }

    private String getOfbizHome(String homeProp) {
        return homeProp.equals(".") ? System.getProperty("user.dir").replace('\\', '/') : homeProp;
    }

    private String getAbsolutePath(Properties props, String key, String def, String ofbizHome) {
        return getProperty(props, key, ofbizHome + "/" + props.getProperty(key, def));
    }

    private Properties getPropertiesFile(List<StartupCommand> ofbizCommands) throws StartupException {
        String fileName = determineOfbizPropertiesFileName(ofbizCommands);
        String fullyQualifiedFileName = "org/apache/ofbiz/base/start/" + fileName;
        Properties props = new Properties();

        try (InputStream propsStream = getClass().getClassLoader().getResourceAsStream(fullyQualifiedFileName)) {
            props.load(propsStream);
        } catch (IOException e) {
            throw new StartupException(e);
        }

        System.out.println("Config.java using configuration file " + fileName);
        return props;
    }

    private String determineOfbizPropertiesFileName(List<StartupCommand> ofbizCommands) {
        String fileName = null;
        if (ofbizCommands.stream().anyMatch(command ->
                command.getName().equals(StartupCommandUtil.StartupOption.START.getName())
                || command.getName().equals(StartupCommandUtil.StartupOption.SHUTDOWN.getName())
                || command.getName().equals(StartupCommandUtil.StartupOption.STATUS.getName()))
            || ofbizCommands.isEmpty()
            || ofbizCommands.stream().allMatch(command -> 
                command.getName().equals(StartupCommandUtil.StartupOption.PORTOFFSET.getName()))) {
            fileName = "start.properties";
        } else if(ofbizCommands.stream().anyMatch(
                option -> option.getName().equals(StartupCommandUtil.StartupOption.LOAD_DATA.getName()))) {
            fileName = "load-data.properties";
        } else if(ofbizCommands.stream().anyMatch(
                option -> option.getName().equals(StartupCommandUtil.StartupOption.TEST.getName()))) {
            fileName = "test.properties";
        }
        return fileName;
    }

    private int getPortOffsetValue(List<StartupCommand> ofbizCommands, String defaultOffset) throws StartupException {
        String extractedPortOffset = ofbizCommands.stream()
            .filter(command -> command.getName().equals(StartupCommandUtil.StartupOption.PORTOFFSET.getName()))
            .findFirst()
                .map(ofbizCommand -> ofbizCommand.getProperties().keySet().iterator().next())
                .orElse(defaultOffset);
        try {
            return Integer.parseInt(extractedPortOffset);
        } catch(NumberFormatException e) {
            throw new StartupException("invalid portoffset number: " + extractedPortOffset, e);
        }
    }

    private int getAdminPort(Properties props, int defaultAdminPort, int portOffsetValue) {
        String adminPortStr = getProperty(props, "ofbiz.admin.port", String.valueOf(defaultAdminPort));
        try {
            return Integer.parseInt(adminPortStr) + portOffsetValue;
        } catch (NumberFormatException e) {
            System.out.println("Error parsing admin port: " + adminPortStr + " -- " + e.getMessage());
            return defaultAdminPort + portOffsetValue;
        }
    }

    private InetAddress getAdminAddress(String serverHost) throws StartupException {
        try {
            return InetAddress.getByName(serverHost);
        } catch (UnknownHostException e) {
            throw new StartupException(e);
        }
    }

    private void setDefaultLocale(String localeString) {
        String locales[] = localeString.split("_");
        switch (locales.length) {
        case 1:
            Locale.setDefault(new Locale(locales[0]));
            break;
        case 2:
            Locale.setDefault(new Locale(locales[0], locales[1]));
            break;
        case 3:
            Locale.setDefault(new Locale(locales[0], locales[1], locales[2]));
        }
        System.setProperty("user.language", localeString);
    }
}
