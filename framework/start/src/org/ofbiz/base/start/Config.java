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
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

public class Config {
    public static final double REQUIRED_JDK = 1.6;

    private static String getConfigFileName(String command) {
        // default command is "start"
        if (command == null || command.trim().length() == 0) {
            command = "start";
        }
        // strip off the leading dash
        if (command.startsWith("-")) {
            command = command.substring(1);
        }
        // shutdown & status hack
        if (command.equalsIgnoreCase("shutdown")) {
            command = "start";
        } else if (command.equalsIgnoreCase("status")) {
            command = "start";
        }
        return "org/ofbiz/base/start/" + command + ".properties";
    }

    public static Config getInstance(String[] args) throws IOException {
        String firstArg = args.length > 0 ? args[0] : "";
        String configFileName = getConfigFileName(firstArg);
        Config result = new Config();
        result.readConfig(configFileName);
        return result;
    }

    public InetAddress adminAddress;
    public String adminKey;
    public int adminPort;
    public String awtHeadless;
    public String baseConfig;
    public String baseDtd;
    public String baseJar;
    public String baseLib;
    public String commJar;
    public String containerConfig;
    public String instrumenterClassName;
    public String instrumenterFile;
    public List<String> loaders;
    public String logDir;
    public String ofbizHome;
    public boolean requireCommJar = false;
    public boolean requireToolsJar = false;
    public boolean shutdownAfterLoad = false;
    public String splashLogo;
    public String testConfig;
    public String toolsJar;
    public boolean useShutdownHook = true;

    private String findSystemJar(Properties props, String javaVendor, String javaVersion, String jarName, boolean required) {
        String fileSep = System.getProperty("file.separator");
        String javaHome = System.getProperty("java.home");
        String errorMsg = "Unable to locate " + jarName + " - ";
        // String foundMsg = "Found " + jarName + " - ";
        String jarLoc = "lib" + fileSep + jarName;
        File tj = null;

        if ("tools.jar".equals(jarName) && javaVendor.startsWith("Apple")) {
            // tools.jar is always available in Apple's JDK implementation
            return null;
        }

        // check to see if it is in the OFBIZ_HOME directory
        tj = new File(ofbizHome + fileSep + jarName);
        if (tj.exists()) {
            return null;
        }

        // check to see if it is in the base/lib directory
        tj = new File(baseLib + fileSep + jarName);
        if (tj.exists()) {
            return null;
        }

        // try to locate tools.jar from the properties file
        String jarProps = props.getProperty("java." + jarName, null);
        if (jarProps != null) {
            tj = new File(jarProps);
            if (!tj.exists()) {
                if (required) {
                    System.err.println(errorMsg + tj.getAbsolutePath());
                }
            } else {
                // System.out.println(foundMsg + tj.getAbsolutePath());
                return jarProps;
            }
        }

        // next check the JAVA_HOME lib dir
        tj = new File(javaHome + fileSep + jarLoc);
        if (!tj.exists()) {
            if (required) {
                System.err.println(errorMsg + tj.getAbsolutePath());
            }
        } else {
            // System.out.println(foundMsg + tj.getAbsolutePath());
            return tj.getAbsolutePath();
        }

        // next if we are a JRE dir check the parent dir
        String jreExt = fileSep + "jre";
        if (javaHome.toLowerCase().endsWith(jreExt)) {
            javaHome = javaHome.substring(0, javaHome.lastIndexOf(fileSep));
            tj = new File(javaHome + fileSep + jarLoc);
            if (!tj.exists()) {
                if (required) {
                    System.err.println(errorMsg + tj.getAbsolutePath());
                }
            } else {
                // System.out.println(foundMsg + tj.getAbsolutePath());
                return tj.getAbsolutePath();
            }
        }

        // special windows checking
        if (javaHome.toLowerCase().charAt(1) == ':') {
            String driveLetter = javaHome.substring(0, 2);
            String windowsPath = driveLetter + fileSep + "j2sdk" + javaVersion;
            tj = new File(windowsPath + fileSep + jarLoc);
            if (!tj.exists()) {
                if (required) {
                    System.err.println(errorMsg + tj.getAbsolutePath());
                }
            } else {
                // System.out.println(foundMsg + tj.getAbsolutePath());
                return tj.getAbsolutePath();
            }
        }

        if (required) {
            System.err.println("");
            System.err.println("Required library " + jarName + " could not be located.");
            System.err.println("Make sure you using Java2 SDK " + REQUIRED_JDK + "+ and NOT the JRE.");
            System.err.println("You may need to copy " + jarName + " into a loadable lib directory");
            System.err.println("(i.e. OFBIZ_HOME or OFBIZ_HOME/base/lib)");
            System.err.println("");
            System.exit(-1);
        }

        return null;
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
                    } else {
                        throw new FileNotFoundException();
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

    public void initClasspath(Classpath classPath) throws IOException {
        // load tools.jar
        if (this.toolsJar != null) {
            classPath.addComponent(this.toolsJar);
        }
        // load comm.jar
        if (this.commJar != null) {
            classPath.addComponent(this.commJar);
        }
        // add OFBIZ_HOME to class path & load libs
        classPath.addClasspath(this.ofbizHome);
        loadLibs(classPath, this.ofbizHome, false);
        // load the lib directory
        if (this.baseLib != null) {
            loadLibs(classPath, this.baseLib, true);
        }
        // load the ofbiz-base.jar
        if (this.baseJar != null) {
            classPath.addComponent(this.baseJar);
        }
        // load the base schema directory
        if (this.baseDtd != null) {
            classPath.addComponent(this.baseDtd);
        }
        // load the config directory
        if (this.baseConfig != null) {
            classPath.addComponent(this.baseConfig);
        }
        classPath.instrument(this.instrumenterFile, this.instrumenterClassName);
    }

    private void loadLibs(Classpath classPath, String path, boolean recurse) throws IOException {
        File libDir = new File(path);
        if (libDir.exists()) {
            File files[] = libDir.listFiles();
            for (File file: files) {
                String fileName = file.getName();
                // FIXME: filter out other files?
                if (file.isDirectory() && !"CVS".equals(fileName) && !".svn".equals(fileName) && recurse) {
                    loadLibs(classPath, file.getCanonicalPath(), recurse);
                } else if (fileName.endsWith(".jar") || fileName.endsWith(".zip")) {
                    classPath.addComponent(file);
                }
            }
        }
    }

    public void readConfig(String config) throws IOException {
        // check the java_version
        String javaVersion = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");
        double version = Double.parseDouble(javaVersion.substring(0, javaVersion.indexOf(".") + 2));
        if (REQUIRED_JDK > version) {
            System.err.println("");
            System.err.println("Java Version - " + javaVendor + " " + javaVersion + " - is not supported by OFBiz.");
            System.err.println("Please install Java2 SDK " + REQUIRED_JDK + "+");
            System.err.println("");
            System.exit(-1);
        }

        Properties props = this.getPropertiesFile(config);
        System.out.println("Start.java using configuration file " + config);

        // set the ofbiz.home
        if (ofbizHome == null) {
            ofbizHome = props.getProperty("ofbiz.home", ".");
            // get a full path
            if (ofbizHome.equals(".")) {
                ofbizHome = System.getProperty("user.dir");
                ofbizHome = ofbizHome.replace('\\', '/');
                System.out.println("Set OFBIZ_HOME to - " + ofbizHome);
            }
        }
        System.setProperty("ofbiz.home", ofbizHome);

        // base config directory
        baseConfig = getOfbizHomeProp(props, "ofbiz.base.config", "framework/base/config");

        // base schema directory
        baseDtd = getOfbizHomeProp(props, "ofbiz.base.schema", "framework/base/dtd");

        // base lib directory
        baseLib = getOfbizHomeProp(props, "ofbiz.base.lib", "framework/base/lib");

        // base jar file
        baseJar = getOfbizHomeProp(props, "ofbiz.base.jar", "framework/base/build/lib/ofbiz-base.jar");

        // tools jar
        String reqTJ = getProp(props, "java.tools.jar.required", "false");
        requireToolsJar = "true".equalsIgnoreCase(reqTJ);
        toolsJar = this.findSystemJar(props, javaVendor, javaVersion, "tools.jar", requireToolsJar);

        // comm jar
        String reqCJ = getProp(props, "java.comm.jar.required", "false");
        requireCommJar = "true".equalsIgnoreCase(reqCJ);
        commJar = this.findSystemJar(props, javaVendor, javaVersion, "comm.jar", requireCommJar);

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
        try {
            adminPort = Integer.parseInt(adminPortStr);
        } catch (Exception e) {
            adminPort = 0;
        }

        // set the Derby system home
        String derbyPath = getProp(props, "derby.system.home", "runtime/data/derby");
        System.setProperty("derby.system.home", derbyPath);

        // set the property to tell Log4J to use log4j.xml
        String log4jConfig = getProp(props, "log4j.configuration", "log4j.xml");

        // set the log4j configuration property so we don't pick up one inside jars by
        // mistake
        System.setProperty("log4j.configuration", log4jConfig);

        // check for shutdown hook
        if (System.getProperty("ofbiz.enable.hook") != null && System.getProperty("ofbiz.enable.hook").length() > 0) {
            useShutdownHook = "true".equalsIgnoreCase(System.getProperty("ofbiz.enable.hook"));
        } else if (props.getProperty("ofbiz.enable.hook") != null && props.getProperty("ofbiz.enable.hook").length() > 0) {
            useShutdownHook = "true".equalsIgnoreCase(props.getProperty("ofbiz.enable.hook"));
        }

        // check for auto-shutdown
        if (System.getProperty("ofbiz.auto.shutdown") != null && System.getProperty("ofbiz.auto.shutdown").length() > 0) {
            shutdownAfterLoad = "true".equalsIgnoreCase(System.getProperty("ofbiz.auto.shutdown"));
        } else if (props.getProperty("ofbiz.auto.shutdown") != null && props.getProperty("ofbiz.auto.shutdown").length() > 0) {
            shutdownAfterLoad = "true".equalsIgnoreCase(props.getProperty("ofbiz.auto.shutdown"));
        }

        // set AWT headless mode
        awtHeadless = getProp(props, "java.awt.headless", null);
        if (awtHeadless != null) {
            System.setProperty("java.awt.headless", awtHeadless);
        }

        // get the splash logo
        splashLogo = props.getProperty("ofbiz.start.splash.logo", null);

        // set the property to tell Jetty to use 2.4 SessionListeners
        System.setProperty("org.mortbay.jetty.servlet.AbstractSessionManager.24SessionDestroyed", "true");

        // set the default locale
        String localeString = props.getProperty("ofbiz.locale.default");
        if (localeString != null && localeString.length() > 0) {
            String args[] = localeString.split("_");
            switch (args.length) {
                case 1:
                    Locale.setDefault(new Locale(args[0]));
                    break;
                case 2:
                    Locale.setDefault(new Locale(args[0], args[1]));
                    break;
                case 3:
                    Locale.setDefault(new Locale(args[0], args[1], args[2]));
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
        loaders = new ArrayList<String>();
        int currentPosition = 1;
        while (true) {
            String loaderClass = props.getProperty("ofbiz.start.loader" + currentPosition);
            if (loaderClass == null || loaderClass.length() == 0) {
                break;
            } else {
                loaders.add(loaderClass);
                currentPosition++;
            }
        }
    }

}
