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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

/**
 * Start - OFBiz Container(s) Startup Class
 *
 */
public class Start implements Runnable {

    private Classpath classPath = new Classpath(System.getProperty("java.class.path"));
    private ClassLoader classloader = null;
    private ServerSocket serverSocket = null;
    private Thread serverThread = null;
    private boolean serverStarted = false;
    private boolean serverStopping = false;
    private boolean serverRunning = true;
    private List<StartupLoader> loaders = null;
    private Config config = null;
    private String[] loaderArgs = null;

    private static final String SHUTDOWN_COMMAND = "SHUTDOWN";
    private static final String STATUS_COMMAND = "STATUS";
    private static final double REQUIRED_JDK = 1.6;

    public void init(String[] args, boolean fullInit) throws IOException {
        String globalSystemPropsFileName = System.getProperty("ofbiz.system.props");
        if (globalSystemPropsFileName != null) {
            try {
                System.getProperties().load(new FileInputStream(globalSystemPropsFileName));
            } catch (IOException e) {
                throw (IOException) new IOException("Couldn't load global system props").initCause(e);
            }
        }
        String firstArg = args.length > 0 ? args[0] : "";
        String cfgFile = Start.getConfigFileName(firstArg);

        this.loaders = new ArrayList<StartupLoader>();
        this.config = new Config();

        // read the default properties first
        config.readConfig(cfgFile);

        // parse the startup arguments
        if (args.length > 1) {
            this.loaderArgs = new String[args.length - 1];
            System.arraycopy(args, 1, this.loaderArgs, 0, this.loaderArgs.length);
        }

        if (fullInit) {
            // initialize the classpath
            initClasspath();

            // initialize the log directory
            initLogDirectory();

            // initialize the listener thread
            initListenerThread();

            // initialize the startup loaders
            initStartLoaders();

            // set the shutdown hook
            if (config.useShutdownHook) {
                setShutdownHook();
            } else {
                System.out.println("Shutdown hook disabled");
            }
        }
    }

    public void init(String[] args) throws IOException {
        init(args, true);
    }

    public void run() {
        while (serverRunning) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Received connection from - " + clientSocket.getInetAddress() + " : " + clientSocket.getPort());
                processClientRequest(clientSocket);
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        shutdownServer();
        System.exit(0);
    }

    private void processClientRequest(Socket client) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
        String request = reader.readLine();

        PrintWriter writer = new PrintWriter(client.getOutputStream(), true);
        writer.println(processRequest(request, client));
        writer.flush();

        writer.close();
        reader.close();
    }

    private String processRequest(String request, Socket client) {
        if (request != null) {
            String key = request.substring(0, request.indexOf(':'));
            String command = request.substring(request.indexOf(':') + 1);
            if (!key.equals(config.adminKey)) {
                return "FAIL";
            } else {
                if (command.equals(Start.SHUTDOWN_COMMAND)) {
                    if (serverStopping) return "IN-PROGRESS";
                    Thread t = new Thread() {
                        @Override
                        public void run() {
                            shutdownServer();
                        }
                    };
                    t.start();
                    return "OK";
                } else if (command.equals(Start.STATUS_COMMAND)) {
                    return serverStopping ? "Stopping" : serverStarted ? "Running" : "Starting";
                }
                return "FAIL";
            }
        } else {
            return "FAIL";
        }
    }

    private void initListenerThread() throws IOException {
        if (config.adminPort > 0) {
            this.serverSocket = new ServerSocket(config.adminPort, 1, config.adminAddress);
            this.serverThread = new Thread(this, this.toString());
            this.serverThread.setDaemon(false);
            System.out.println("Admin socket configured on - " + config.adminAddress + ":" + config.adminPort);
            this.serverThread.start();
        } else {
            System.out.println("Admin socket not configured; set to port 0");
        }
    }

    private void loadLibs(String path, boolean recurse) throws IOException {
        File libDir = new File(path);
        if (libDir.exists()) {
            File files[] = libDir.listFiles();
            for (File file: files) {
                String fileName = file.getName();
                // FIXME: filter out other files?
                if (file.isDirectory() && !"CVS".equals(fileName) && !".svn".equals(fileName) && recurse) {
                    loadLibs(file.getCanonicalPath(), recurse);
                } else if (fileName.endsWith(".jar") || fileName.endsWith(".zip")) {
                    classPath.addComponent(file);
                }
            }
        }
    }

    private void initClasspath() throws IOException {
        // load tools.jar
        if (config.toolsJar != null) {
            classPath.addComponent(config.toolsJar);
        }

        // load comm.jar
        if (config.commJar != null) {
            classPath.addComponent(config.commJar);
        }

        // add OFBIZ_HOME to CP & load libs
        classPath.addClasspath(config.ofbizHome);
        loadLibs(config.ofbizHome, false);

        // load the lib directory
        if (config.baseLib != null) {
            loadLibs(config.baseLib, true);
        }

        // load the ofbiz-base.jar
        if (config.baseJar != null) {
            classPath.addComponent(config.baseJar);
        }

        // load the base schema directory
        if (config.baseDtd != null) {
            classPath.addComponent(config.baseDtd);
        }

        // load the config directory
        if (config.baseConfig != null) {
            classPath.addComponent(config.baseConfig);
        }

        classPath.instrument(config.instrumenterFile, config.instrumenterClassName);
        // set the classpath/classloader
        System.setProperty("java.class.path", classPath.toString());
        this.classloader = classPath.getClassLoader();
        Thread.currentThread().setContextClassLoader(classloader);
        if (System.getProperty("DEBUG") != null) {
            System.out.println("Startup Classloader: " + classloader.toString());
            System.out.println("Startup Classpath: " + classPath.toString());
        }
    }

    private void initLogDirectory() {
        // stat the log directory
        boolean createdDir = false;
        File logDir = new File(config.logDir);
        if (!logDir.exists()) {
            logDir.mkdir();
            createdDir = true;
        }

        if (createdDir) {
            System.out.println("Created OFBiz log dir [" + logDir.getAbsolutePath() + "]");
        }
    }

    private void initStartLoaders() {
        // initialize the loaders
        for (String loaderClassName: config.loaders) {
            try {
                Class<?> loaderClass = classloader.loadClass(loaderClassName);
                StartupLoader loader = (StartupLoader) loaderClass.newInstance();
                loader.load(config, loaderArgs);
                loaders.add(loader);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(99);
            }
        }
    }

    private void startStartLoaders() {
        // start the loaders
        for (StartupLoader loader: loaders) {
            try {
                loader.start();
            } catch (StartupException e) {
                e.printStackTrace();
                System.exit(99);
            }
        }
        serverStarted = true;
    }

    private void setShutdownHook() {
        try {
            Method shutdownHook = java.lang.Runtime.class.getMethod("addShutdownHook", new Class[]{java.lang.Thread.class});
            Thread hook = new Thread() {
                @Override
                public void run() {
                    setName("OFBiz_Shutdown_Hook");
                    shutdownServer();
                    // Try to avoid JVM crash
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

            shutdownHook.invoke(Runtime.getRuntime(), new Object[]{hook});
        } catch (Exception e) {
            // VM Does not support shutdown hook
            e.printStackTrace();
        }
    }

    private void shutdownServer() {
        if (serverStopping) return;
        serverStopping = true;
        if (loaders != null && loaders.size() > 0) {
            for (StartupLoader loader: loaders) {
                try {
                    loader.unload();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        serverRunning = false;
    }

    private void startServer() {
        // start the startup loaders
        startStartLoaders();
    }

    public void start() {
        startServer();
        if (config.shutdownAfterLoad) {
            shutdownServer();
            System.exit(0);
        }
    }

    public void stop() {
        shutdownServer();
    }

    public void destroy() {
        this.serverSocket = null;
        this.serverThread = null;
        this.loaders = null;
        this.config = null;
        this.loaderArgs = null;
    }

    public String shutdown() throws IOException {
        return sendSocketCommand(Start.SHUTDOWN_COMMAND);
    }

    public String status() throws IOException {
        String status = null;
        try {
            status = sendSocketCommand(Start.STATUS_COMMAND);
        } catch (ConnectException e) {
            return "Not Running";
        } catch (IOException e) {
            throw e;
        }
        return status;
    }

    private String sendSocketCommand(String command) throws IOException, ConnectException {
        Socket socket = new Socket(config.adminAddress, config.adminPort);

        // send the command
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        writer.println(config.adminKey + ":" + command);
        writer.flush();

        // read the reply
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String response = reader.readLine();

        reader.close();

        // close the socket
        writer.close();
        socket.close();

        return response;
    }

    public static void main(String[] args) throws IOException {
        String firstArg = args.length > 0 ? args[0] : "";
        Start start = new Start();

        if (firstArg.equals("-help") || firstArg.equals("-?")) {
            System.out.println("");
            System.out.println("Usage: java -jar ofbiz.jar [command] [arguments]");
            System.out.println("-both    -----> Run simultaneously the POS (Point of Sales) application and OFBiz standard");
            System.out.println("-help, -? ----> This screen");
            System.out.println("-install -----> Run install (create tables/load data)");
            System.out.println("-pos     -----> Run the POS (Point of Sales) application");
            System.out.println("-setup -------> Run external application server setup");
            System.out.println("-start -------> Start the server");
            System.out.println("-status ------> Status of the server");
            System.out.println("-shutdown ----> Shutdown the server");
            System.out.println("-test --------> Run the JUnit test script");
            System.out.println("[no config] --> Use default config");
            System.out.println("[no command] -> Start the server w/ default config");
        } else {
            // hack for the status and shutdown commands
            if (firstArg.equals("-status")) {
                start.init(args, false);
                System.out.println("Current Status : " + start.status());
            } else if (firstArg.equals("-shutdown")) {
                start.init(args, false);
                System.out.println("Shutting down server : " + start.shutdown());
            } else {
                // general start
                start.init(args, true);
                start.start();
            }
        }
    }

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

    public static class Config {
        public String containerConfig;
        public String testConfig;
        public InetAddress adminAddress;
        public int adminPort;
        public String adminKey;
        public String ofbizHome;
        public String baseJar;
        public String toolsJar;
        public String commJar;
        public String baseLib;
        public String baseDtd;
        public String baseConfig;
        public String logDir;
        public String instrumenterClassName;
        public String instrumenterFile;
        public List<String> loaders;
        public String awtHeadless;
        public String splashLogo;
        public boolean shutdownAfterLoad = false;
        public boolean useShutdownHook = true;
        public boolean requireToolsJar = false;
        public boolean requireCommJar = false;

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

        private String getOfbizHomeProp(Properties props, String key, String def) {
            String value = System.getProperty(key);
            if (value != null) return value;
            return ofbizHome + "/" + props.getProperty(key, def);
        }

        private String getProp(Properties props, String key, String def) {
            String value = System.getProperty(key);
            if (value != null) return value;
            return props.getProperty(key, def);
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

            // set the log4j configuration property so we don't pick up one inside jars by mistake
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

        private String findSystemJar(Properties props, String javaVendor, String javaVersion, String jarName, boolean required) {
            String fileSep = System.getProperty("file.separator");
            String javaHome = System.getProperty("java.home");
            String errorMsg = "Unable to locate " + jarName + " - ";
            //String foundMsg = "Found " + jarName + " - ";
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
                    //System.out.println(foundMsg + tj.getAbsolutePath());
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
                //System.out.println(foundMsg + tj.getAbsolutePath());
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
                    //System.out.println(foundMsg + tj.getAbsolutePath());
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
                    //System.out.println(foundMsg + tj.getAbsolutePath());
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
    }
}



