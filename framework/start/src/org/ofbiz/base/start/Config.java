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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

public class Config {

    public final InetAddress adminAddress;
    public final String adminKey;
    public final int adminPort;
    public final String awtHeadless;
    public final String containerConfig;
    public final String instrumenterClassName;
    public final String instrumenterFile;
    public final List<Map<String, String>> loaders;
    public final String logDir;
    public final String ofbizHome;
    public final boolean shutdownAfterLoad;
    public final String splashLogo;
    public final boolean useShutdownHook;
    public final Integer portOffset;
    public final String classpathAddComponent;
    public final String classpathAddFilesFromPath;

    Config(String[] args) throws IOException {
        String firstArg = args.length > 0 ? args[0] : "";
        // Needed when portoffset is used with these commands, start.properties fits for all of them
        if ("start-batch".equalsIgnoreCase(firstArg)
                || "start-debug".equalsIgnoreCase(firstArg)
                || "stop".equalsIgnoreCase(firstArg)
                || "-shutdown".equalsIgnoreCase(firstArg) // shutdown & status hack (was pre-existing to portoffset introduction, also useful with it)
                || "-status".equalsIgnoreCase(firstArg)) {
            firstArg = "start";
        }
        // default command is "start"
        if (firstArg == null || firstArg.trim().length() == 0) {
            firstArg = "start";
        }
        String config =  "org/ofbiz/base/start/" + firstArg + ".properties";

        Properties props = this.getPropertiesFile(config);
        System.out.println("Start.java using configuration file " + config);

        // set the ofbiz.home
        String ofbizHomeTmp = props.getProperty("ofbiz.home", ".");
        // get a full path
        if (ofbizHomeTmp.equals(".")) {
            ofbizHomeTmp = System.getProperty("user.dir");
            ofbizHomeTmp = ofbizHomeTmp.replace('\\', '/');
        }
        ofbizHome = ofbizHomeTmp;
        System.setProperty("ofbiz.home", ofbizHome);
        System.out.println("Set OFBIZ_HOME to - " + ofbizHome);
        // Class paths
        classpathAddComponent = props.getProperty("ofbiz.start.classpath.addComponent");
        classpathAddFilesFromPath = props.getProperty("ofbiz.start.classpath.addFilesFromPath");
        // log directory
        logDir = getOfbizHomeProp(props, "ofbiz.log.dir", "runtime/logs");

        // container configuration
        containerConfig = getOfbizHomeProp(props, "ofbiz.container.config", "framework/base/config/ofbiz-containers.xml");

        // get the admin server info
        String serverHost = getProp(props, "ofbiz.admin.host", "127.0.0.1");

        String adminPortStr = getProp(props, "ofbiz.admin.port", "0");
        // set the admin key
        adminKey = getProp(props, "ofbiz.admin.key", "NA");

        // create the host InetAddress
        adminAddress = InetAddress.getByName(serverHost);

        // parse the port number
        int adminPortTmp;
        try {
            adminPortTmp = Integer.parseInt(adminPortStr);
            if (args.length > 0) {
                for (String arg : args) {
                    if (arg.toLowerCase().contains("portoffset=") && !arg.toLowerCase().contains("${portoffset}")) {
                        adminPortTmp = adminPortTmp != 0 ? adminPortTmp : 10523; // This is necessary because the ASF machines don't allow ports 1 to 3, see  INFRA-6790
                        adminPortTmp += Integer.parseInt(arg.split("=")[1]);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error while parsing admin port number (so default to 10523) = " + e);
            adminPortTmp = 10523;
        }
        adminPort = adminPortTmp;

        // set the Derby system home
        String derbyPath = getProp(props, "derby.system.home", "runtime/data/derby");
        System.setProperty("derby.system.home", derbyPath);

        // check for shutdown hook
        if (System.getProperty("ofbiz.enable.hook") != null && System.getProperty("ofbiz.enable.hook").length() > 0) {
            useShutdownHook = "true".equalsIgnoreCase(System.getProperty("ofbiz.enable.hook"));
        } else if (props.getProperty("ofbiz.enable.hook") != null && props.getProperty("ofbiz.enable.hook").length() > 0) {
            useShutdownHook = "true".equalsIgnoreCase(props.getProperty("ofbiz.enable.hook"));
        } else {
            useShutdownHook = true;
        }

        // check for auto-shutdown
        if (System.getProperty("ofbiz.auto.shutdown") != null && System.getProperty("ofbiz.auto.shutdown").length() > 0) {
            shutdownAfterLoad = "true".equalsIgnoreCase(System.getProperty("ofbiz.auto.shutdown"));
        } else if (props.getProperty("ofbiz.auto.shutdown") != null && props.getProperty("ofbiz.auto.shutdown").length() > 0) {
            shutdownAfterLoad = "true".equalsIgnoreCase(props.getProperty("ofbiz.auto.shutdown"));
        } else {
            shutdownAfterLoad = false;
        }

        // set AWT headless mode
        awtHeadless = getProp(props, "java.awt.headless", null);
        if (awtHeadless != null) {
            System.setProperty("java.awt.headless", awtHeadless);
        }

        // get the splash logo
        splashLogo = props.getProperty("ofbiz.start.splash.logo", null);

        // set the default locale
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

        // set the default time zone
        String tzString = props.getProperty("ofbiz.timeZone.default");
        if (tzString != null && tzString.length() > 0) {
            TimeZone.setDefault(TimeZone.getTimeZone(tzString));
        }

        instrumenterClassName = getProp(props, "ofbiz.instrumenterClassName", null);
        instrumenterFile = getProp(props, "ofbiz.instrumenterFile", null);

        // loader classes
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
        loaders = Collections.unmodifiableList(loadersTmp);

        // set the port offset
        Integer portOffset = 0;
        if (args != null) {
            for (String argument : args) {
                // arguments can prefix w/ a '-'. Just strip them off
                if (argument.startsWith("-")) {
                    int subIdx = 1;
                    if (argument.startsWith("--")) {
                        subIdx = 2;
                    }
                    argument = argument.substring(subIdx);
                }
                // parse the arguments
                if (argument.indexOf("=") != -1) {
                    String argumentName = argument.substring(0, argument.indexOf("="));
                    String argumentVal = argument.substring(argument.indexOf("=") + 1);
                    if ("portoffset".equalsIgnoreCase(argumentName) && !"${portoffset}".equals(argumentVal)) {
                        try {
                            portOffset = Integer.valueOf(argumentVal);
                        } catch (NumberFormatException e) {
                            System.out.println("Error while parsing portoffset (the default value 0 will be used) = " + e);
                        }
                    }
                }
            }
        }
        this.portOffset = portOffset;

    }

    private String getOfbizHomeProp(Properties props, String key, String def) {
        String value = System.getProperty(key);
        if (value != null)
            return value;
        return ofbizHome + "/" + props.getProperty(key, def);
    }

    private String getProp(Properties props, String key, String def) {
        String value = System.getProperty(key);
        if (value != null)
            return value;
        return props.getProperty(key, def);
    }

    private Properties getPropertiesFile(String config) throws IOException {
        InputStream propsStream = null;
        Properties props = new Properties();
        try {
            // first try classpath
            propsStream = getClass().getClassLoader().getResourceAsStream(config);
            if (propsStream != null) {
                props.load(propsStream);
            } else {
                throw new IOException();
            }
        } catch (IOException e) {
            // next try file location
            File propsFile = new File(config);
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
            throw new IOException("Cannot load configuration properties : " + config);
        }
        return props;
    }
}
