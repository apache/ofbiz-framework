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
package org.ofbiz.base.start;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TimeZone;

public final class Config {

    public final String ofbizHome;
    public final String awtHeadless;
    public final InetAddress adminAddress;
    public final String adminKey;
    public final int portOffset;
    public final int adminPort;
    public final String containerConfig;
    public final String instrumenterClassName;
    public final String instrumenterFile;
    public final List<Map<String, String>> loaders;
    public final String logDir;
    public final boolean shutdownAfterLoad;
    public final String splashLogo;
    public final boolean useShutdownHook;
    public final String classpathAddComponent;
    public final String classpathAddFilesFromPath;

    Config(List<StartupCommand> ofbizCommands) throws StartupException {

        // fetch OFBiz Properties object
        Properties props;
        try {
            props = getPropertiesFile(ofbizCommands);
        } catch (IOException e) {
            throw new StartupException(e);
        }

        // set this class fields
        ofbizHome = getOfbizHome(props);
        awtHeadless = getProperty(props, "java.awt.headless", "false");
        adminAddress = getAdminAddress(props);
        adminKey = getProperty(props, "ofbiz.admin.key", "NA");
        portOffset = getPortOffsetValue(ofbizCommands);
        adminPort = getAdminPort(props, portOffset);
        containerConfig = getAbsolutePath(props, "ofbiz.container.config", "framework/base/config/ofbiz-containers.xml", ofbizHome);
        instrumenterClassName = getProperty(props, "ofbiz.instrumenterClassName", null);
        instrumenterFile = getProperty(props, "ofbiz.instrumenterFile", null);
        loaders = getLoaders(props);
        logDir = getAbsolutePath(props, "ofbiz.log.dir", "runtime/logs", ofbizHome);
        shutdownAfterLoad = isShutdownAfterLoad(props);
        splashLogo = props.getProperty("ofbiz.start.splash.logo", null);
        useShutdownHook = isUseShutdownHook(props);
        classpathAddComponent = props.getProperty("ofbiz.start.classpath.addComponent");
        classpathAddFilesFromPath = props.getProperty("ofbiz.start.classpath.addFilesFromPath");

        System.out.println("Set OFBIZ_HOME to - " + ofbizHome);

        // set system properties
        System.setProperty("ofbiz.home", ofbizHome);
        System.setProperty("java.awt.headless", awtHeadless);
        System.setProperty("derby.system.home", getProperty(props, "derby.system.home", "runtime/data/derby"));

        // set the default locale
        setDefaultLocale(props);

        // set the default timezone
        String tzString = props.getProperty("ofbiz.timeZone.default");
        if (tzString != null && tzString.length() > 0) {
            TimeZone.setDefault(TimeZone.getTimeZone(tzString));
        }
    }

    private String getAbsolutePath(Properties props, String key, String def, String ofbizHome) {
        String value = System.getProperty(key);
        if (value != null) {
            return value;
        } else {
            return ofbizHome + "/" + props.getProperty(key, def);
        }
    }

    private String getProperty(Properties props, String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value != null) {
            return value;
        } else {
            return props.getProperty(key, defaultValue);
        }
    }

    private Properties getPropertiesFile(List<StartupCommand> ofbizCommands) throws IOException {
        String fileName = determineOfbizPropertiesFileName(ofbizCommands);
        String fullyQualifiedFileName = "org/ofbiz/base/start/" + fileName + ".properties";
        InputStream propsStream = null;
        Properties props = new Properties();
        try {
            // first try classpath
            propsStream = getClass().getClassLoader().getResourceAsStream(fullyQualifiedFileName);
            if (propsStream != null) {
                props.load(propsStream);
            } else {
                throw new IOException();
            }
        } catch (IOException e) {
            // next try file location
            File propsFile = new File(fullyQualifiedFileName);
            if (propsFile != null) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(propsFile);
                    if (fis != null) {
                        props.load(fis);
                    }
                } catch (FileNotFoundException e2) {
                    // do nothing; we will see empty props below
                } finally {
                    if (fis != null) {
                        fis.close();
                    }
                }
            }
        } finally {
            if (propsStream != null) {
                propsStream.close();
            }
        }

        // check for empty properties
        if (props.isEmpty()) {
            throw new IOException("Cannot load configuration properties : " + fullyQualifiedFileName);
        }
        System.out.println("Start.java using configuration file " + fullyQualifiedFileName);
        return props;
    }

    private String determineOfbizPropertiesFileName(List<StartupCommand> ofbizCommands) {
        String fileName = null;
        if (ofbizCommands.stream().anyMatch(command ->
        command.getName() == StartupCommandUtil.StartupOption.START.getName()
        || command.getName() == StartupCommandUtil.StartupOption.SHUTDOWN.getName()
        || command.getName() == StartupCommandUtil.StartupOption.STATUS.getName() )
        || ofbizCommands.isEmpty()
        || ofbizCommands.stream().allMatch(command ->
            command.getName() == StartupCommandUtil.StartupOption.PORTOFFSET.getName()) 
                ){
            fileName = "start";
        } else if(ofbizCommands.stream().anyMatch(
                option -> option.getName() == StartupCommandUtil.StartupOption.BOTH.getName())) {
            fileName = "both";
        } else if(ofbizCommands.stream().anyMatch(
                option -> option.getName() == StartupCommandUtil.StartupOption.LOAD_DATA.getName())) {
            fileName = "load-data";
        } else if(ofbizCommands.stream().anyMatch(
                option -> option.getName() == StartupCommandUtil.StartupOption.POS.getName())) {
            fileName = "pos";
        } else if(ofbizCommands.stream().anyMatch(
                option -> option.getName() == StartupCommandUtil.StartupOption.TEST.getName())) {
            fileName = "test";
        } else if(ofbizCommands.stream().anyMatch(
                option -> option.getName() == StartupCommandUtil.StartupOption.TEST_LIST.getName())) {
            fileName = "testlist";
        }
        return fileName;
    }

    private void setDefaultLocale(Properties props) {
        String localeString = props.getProperty("ofbiz.locale.default");
        if (localeString != null && localeString.length() > 0) {
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

    private int getPortOffsetValue(List<StartupCommand> ofbizCommands) throws StartupException {
        int extractedPortOffset = 0;
        Optional<StartupCommand> portOffsetCommand = ofbizCommands.stream()
                .filter(command -> command.getName().equals(StartupCommandUtil.StartupOption.PORTOFFSET.getName()))
                .findFirst();
        if(portOffsetCommand.isPresent()) {
            Map<String,String> commandArgs = portOffsetCommand.get().getProperties();
            try {
                extractedPortOffset = Integer.parseInt(commandArgs.keySet().iterator().next());
            } catch(NumberFormatException e) {
                throw new StartupException("invalid portoffset number", e);
            }
        }
        return extractedPortOffset;
    }

    private InetAddress getAdminAddress(Properties props) throws StartupException {
        String serverHost = getProperty(props, "ofbiz.admin.host", "127.0.0.1");
        try {
            return InetAddress.getByName(serverHost);
        } catch (UnknownHostException e) {
            throw new StartupException(e);
        }
    }

    private int getAdminPort(Properties props, int portOffsetValue) {
        String adminPortStr = getProperty(props, "ofbiz.admin.port", "0");
        int calculatedAdminPort;
        try {
            calculatedAdminPort = Integer.parseInt(adminPortStr);
            calculatedAdminPort = calculatedAdminPort != 0 ? calculatedAdminPort : 10523; // This is necessary because the ASF machines don't allow ports 1 to 3, see  INFRA-6790
            calculatedAdminPort += portOffsetValue;
        } catch (Exception e) {
            System.out.println("Error while parsing admin port number (so default to 10523) = " + e);
            calculatedAdminPort = 10523;
        }
        return calculatedAdminPort;
    }

    private List<Map<String, String>> getLoaders(Properties props) {
        ArrayList<Map<String, String>> loadersTmp = new ArrayList<Map<String, String>>();
        int currentPosition = 1;
        Map<String, String> loader = null;
        while (true) {
            loader = new HashMap<String, String>();
            String loaderClass = props.getProperty("ofbiz.start.loader" + currentPosition);
            if (loaderClass == null || loaderClass.length() == 0) {
                break;
            } else {
                loader.put("class", loaderClass);
                loader.put("profiles", props.getProperty("ofbiz.start.loader" + currentPosition + ".loaders"));
                loadersTmp.add(Collections.unmodifiableMap(loader));
                currentPosition++;
            }
        }
        loadersTmp.trimToSize();
        return Collections.unmodifiableList(loadersTmp);
    }

    private String getOfbizHome(Properties props) {
        String extractedOfbizHome = props.getProperty("ofbiz.home", ".");
        if (extractedOfbizHome.equals(".")) {
            extractedOfbizHome = System.getProperty("user.dir");
            extractedOfbizHome = extractedOfbizHome.replace('\\', '/');
        }
        return extractedOfbizHome;
    }

    private boolean isShutdownAfterLoad(Properties props) {
        if (System.getProperty("ofbiz.auto.shutdown") != null && System.getProperty("ofbiz.auto.shutdown").length() > 0) {
            return "true".equalsIgnoreCase(System.getProperty("ofbiz.auto.shutdown"));
        } else if (props.getProperty("ofbiz.auto.shutdown") != null && props.getProperty("ofbiz.auto.shutdown").length() > 0) {
            return "true".equalsIgnoreCase(props.getProperty("ofbiz.auto.shutdown"));
        } else {
            return false;
        }
    }

    private boolean isUseShutdownHook(Properties props) {
        if (System.getProperty("ofbiz.enable.hook") != null && System.getProperty("ofbiz.enable.hook").length() > 0) {
            return "true".equalsIgnoreCase(System.getProperty("ofbiz.enable.hook"));
        } else if (props.getProperty("ofbiz.enable.hook") != null && props.getProperty("ofbiz.enable.hook").length() > 0) {
            return "true".equalsIgnoreCase(props.getProperty("ofbiz.enable.hook"));
        } else {
            return true;
        }
    }
}
