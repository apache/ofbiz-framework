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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileFilter;
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
        List<Map<String, String>> loadersTmp = new ArrayList<Map<String, String>>();
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

    void initClasspath(Classpath classPath, Classpath libraryPath) throws Exception {
        // add OFBIZ_HOME to class path
        classPath.addClasspath(this.ofbizHome);
        File home = new File(this.ofbizHome);
        collectClasspathEntries(new File(home, "framework"), classPath, libraryPath);
        collectClasspathEntries(new File(home, "applications"), classPath, libraryPath);
        collectClasspathEntries(new File(home, "specialpurpose"), classPath, libraryPath);
        collectClasspathEntries(new File(home, "hot-deploy"), classPath, libraryPath);
        System.setProperty("java.library.path", libraryPath.toString());
        classPath.instrument(this.instrumenterFile, this.instrumenterClassName);
    }

    private void collectClasspathEntries(File folder, Classpath classpath, Classpath libraryPath) throws ParserConfigurationException, IOException, SAXException {
        if (!folder.exists() && !folder.isDirectory()) {
            return;
        }
        FileFilter componentLoadFilter = new FileFilter() {
            public boolean accept(File pathname) {
                return "component-load.xml".equals(pathname.getName());
            }
        };
        FileFilter folderFilter = new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        };
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        File[] componentLoadFiles;
        List<File> ofbizComponents = new ArrayList<File>();
        componentLoadFiles = folder.listFiles(componentLoadFilter);
        if (componentLoadFiles != null && componentLoadFiles.length == 1) {
            File componentLoadFile = componentLoadFiles[0];
            // parse and get folder names to be processed
            Document document = builder.parse(componentLoadFile);
            Element element = document.getDocumentElement();
            NodeList loadComponents = element.getElementsByTagName("load-component");
            for (int i = 0; i < loadComponents.getLength(); i++) {
                Node loadComponent = loadComponents.item(i);
                NamedNodeMap attributes = loadComponent.getAttributes();
                Node componentLocation = attributes.getNamedItem("component-location");
                if (componentLocation == null) {
                    continue;
                }
                ofbizComponents.add(new File(new File(folder, componentLocation.getNodeValue()), "ofbiz-component.xml"));
            }
        } else {
            File[] componentFolders = folder.listFiles(folderFilter);
            for (File componentFolder: componentFolders) {
                File ofbizComponent = new File(componentFolder, "ofbiz-component.xml");
                if (ofbizComponent.exists()) {
                    ofbizComponents.add(ofbizComponent);
                }
            }
        }
        String nativeLibExt = System.mapLibraryName("someLib").replace("someLib", "").toLowerCase();
        for (File ofbizComponent: ofbizComponents) {
            Document document = builder.parse(ofbizComponent);
            Element element = document.getDocumentElement();
            if (element.hasAttribute("enabled")) {
                if ("false".equals(element.getAttribute("enabled"))) {
                    continue;
                }
            }
            NodeList classpathEntries = element.getElementsByTagName("classpath");
            for (int i = 0; i < classpathEntries.getLength(); i++) {
                Node classpathEntry = classpathEntries.item(i);
                NamedNodeMap attributes = classpathEntry.getAttributes();
                Node type = attributes.getNamedItem("type");
                if (type == null || !("jar".equals(type.getNodeValue()) || "dir".equals(type.getNodeValue()))) {
                    continue;
                }
                Node location = attributes.getNamedItem("location");
                String locationValue = location.getNodeValue();
                locationValue = locationValue.replace('\\', '/');
                // set the location to not have a leading slash
                if (locationValue.startsWith("/")) {
                    locationValue = locationValue.substring(1);
                }
                String dirLoc = locationValue;
                if (dirLoc.endsWith("/*")) {
                    // strip off the slash splat
                    dirLoc = locationValue.substring(0, locationValue.length() - 2);
                }

                String fileNameSeparator = ("\\".equals(File.separator) ? "\\" + File.separator : File.separator);
                dirLoc = dirLoc.replaceAll("/+|\\\\+", fileNameSeparator);
                File path = new File(ofbizComponent.getParent(), dirLoc);
                if (path.exists()) {
                    if (path.isDirectory()) {
                        if ("dir".equals(type.getNodeValue())) {
                            classpath.addComponent(path.toString());
                        }
                        // load all .jar, .zip files and native libs in this directory
                        boolean containsNativeLibs = false;
                        for (File file: path.listFiles()) {
                            String fileName = file.getName().toLowerCase();
                            if (fileName.endsWith(".jar") || fileName.endsWith(".zip")) {
                                classpath.addComponent(file);
                            } else if (fileName.endsWith(nativeLibExt)) {
                                containsNativeLibs = true;
                            }
                        }
                        if (containsNativeLibs) {
                            libraryPath.addComponent(path);
                        }
                    } else {
                        classpath.addComponent(path.toString());
                    }
                }
            }
        }
    }

}
