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

package org.ofbiz.appservers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.start.Classpath;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.w3c.dom.Document;

/**
 * GenerateContainer - Generates Configuration Files For Application Servers
 * ** This container requires StartInfoLoader to be loaded at startup.
 * ** This container requires the ComponentContainer to be loaded first.
 *
 */
public class GenerateContainer implements Container {

    public static final String module = GenerateContainer.class.getName();
    public static final String source = "/framework/appserver/templates/";
    public static String target = "/setup/";

    protected String configFile = null;
    protected String ofbizHome = null;
    protected String args[] = null;

    private boolean isGeronimo = false;
    private String geronimoHome = null;

    /**
     * @see org.ofbiz.base.container.Container#init(java.lang.String[], java.lang.String)
     */
    public void init(String[] args, String configFile) {
        ofbizHome = System.getProperty("ofbiz.home");
        this.configFile = configFile;
        this.args = args;
        isGeronimo = args[0].toLowerCase().contains("geronimo") || args[0].toLowerCase().contains("wasce");
        if (isGeronimo) {
            target="/META-INF/";
            geronimoHome = System.getenv("GERONIMO_HOME");
            if (geronimoHome == null) {
                geronimoHome = UtilProperties.getPropertyValue("appserver", "geronimoHome", null);
            }
        }
    }

    /**
     * @see org.ofbiz.base.container.Container#start()
     */
    public boolean start() throws ContainerException {
        generateFiles();
        System.exit(1);
        return true;
    }

    /**
     * Stop the container
     *
     * @throws org.ofbiz.base.container.ContainerException
     *
     */
    public void stop() throws ContainerException {
    }

    private void generateFiles() throws ContainerException {
        if (isGeronimo) {
            if (geronimoHome == null) {
                Debug.logFatal("*** 'GERONIMO_HOME' was not found in your environment. Please set the location of Geronimo into a GERONIMO_HOME env var or as a geronimoHome property in appserver.properties file.", module);
                throw new ContainerException("Error in Geronimo deployment, please check the log");
            }
        }
        File files[] = getTemplates();
        Map<String, Object> dataMap = buildDataMap();

        String user = UtilProperties.getPropertyValue("appserver", "user", "system");
        String password = UtilProperties.getPropertyValue("appserver", "password", "manager");

        boolean offline = UtilProperties.propertyValueEqualsIgnoreCase("appserver", "offline", "true");
        boolean redeploy = UtilProperties.propertyValueEqualsIgnoreCase("appserver", "redeploy", "true");

        String geronimoHostHome = UtilProperties.getPropertyValue("appserver", "geronimoHostHome", null);
        String host = UtilProperties.getPropertyValue("appserver", "host", "");
        String port = UtilProperties.getPropertyValue("appserver", "port", "");
        boolean pauseInGeronimoScript = UtilProperties.propertyValueEqualsIgnoreCase("appserver", "pauseInGeronimoScript", "true");

        int instancesNumber = (int) UtilProperties.getPropertyNumber("appserver", "instancesNumber");
        String instanceNumber = "";

        if (isGeronimo) {
            File geronimoHomeDir = new File (geronimoHome);
            if (!(geronimoHomeDir.isDirectory())) {
                Debug.logFatal("*** " + geronimoHome + " does not exist or is not a directoy. Please set the location of Geronimo into a GERONIMO_HOME env var or as a geronimoHome property in appserver.properties file.", module);
                throw new ContainerException("Error in Geronimo deployment, please check the log");
            }

            if (UtilValidate.isNotEmpty(host) && UtilValidate.isNotEmpty(geronimoHostHome)) {
                geronimoHomeDir = new File ("//" + host + "/" + geronimoHostHome);
                if (!(geronimoHomeDir.isDirectory())) {
                    Debug.logFatal("*** " + geronimoHostHome + " does not exist or is not a directoy. Please set the location of Geronimo on host as a geronimoHostHome property in appserver.properties file.", module);
                    throw new ContainerException("Error in Geronimo deployment, please check the log");
                }
            } else {
                geronimoHostHome = geronimoHome;
            }

            if (redeploy && offline) {
                Debug.logFatal("*** You can't use redeploy with a server offline.", module);
                    throw new ContainerException("Error in Geronimo deployment, please check the log");
                }

            for(int inst = 0; inst <= instancesNumber; inst++) {
                instanceNumber = (inst == 0 ? "" : inst).toString();
                GenerateGeronimoDeployment geronimoDeployment = new GenerateGeronimoDeployment();
                List<String> classpathJars = geronimoDeployment.generate(args[0], geronimoHostHome, instanceNumber);
                if (classpathJars == null) {
                    throw new ContainerException("Error in Geronimo deployment, please check the log");
                }
                dataMap.put("classpathJars", classpathJars);
                dataMap.put("pathSeparatorChar", File.pathSeparatorChar);
                dataMap.put("instanceNumber", instanceNumber);
                //                if (UtilValidate.isNotEmpty(instanceNumber)) {
                //                    List webApps = (List) dataMap.get("webApps");
                //                    for (Object webAppObject: webApps) {
                //                        WebappInfo webAppInfo = (ComponentConfig.WebappInfo) webAppObject;
                //                        String webAppLocation = webAppInfo.getLocation();
                //                        String webXmlLocation = webAppLocation + "/WEB-INF/web.xml";
                //                        if (isFileExistsAndCanWrite(webXmlLocation)) {
                //                            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                //                            DocumentBuilder docBuilder = null;
                //                            try {
                //                                docBuilder = docFactory.newDocumentBuilder();
                //                            } catch (ParserConfigurationException e) {
                //                                throw new ContainerException(e);
                //                            }
                //                            Document doc = null;
                //                            try {
                //                                doc = docBuilder.parse(webXmlLocation);
                //                            } catch (SAXException e) {
                //                                throw new ContainerException(e);
                //                            } catch (IOException e) {
                //                                throw new ContainerException(e);
                //                            }
                //                            Node webApp = doc.getFirstChild();
                //                            Node contextParam = doc.createElement("context-param");
                //                            NamedNodeMap contextParamAttributes = contextParam.getAttributes();
                //
                //                            Attr paramName = doc.createAttribute("param-name");
                //                            paramName.setValue("instanceNumber");
                //                            contextParamAttributes.setNamedItem(paramName);
                //
                //                            Attr paramValue = doc.createAttribute("param-value");
                //                            paramValue.setValue(instanceNumber);
                //                            contextParamAttributes.setNamedItem(paramValue);
                //        //                    Node nodeToAppend = doc.importNode(contextParam, true); this should not be needed
                //        //                    webApp.appendChild(nodeToAppend);
                //
                //        //                    webApp.appendChild(contextParam); this is the line needed but commented for now
                //
                //                            Transformer transformer;
                //                            try {
                //                                transformer = TransformerFactory.newInstance().newTransformer();
                //                            } catch (TransformerConfigurationException e) {
                //                                throw new ContainerException(e);
                //                            } catch (TransformerFactoryConfigurationError e) {
                //                                throw new ContainerException(e);
                //                            }
                //                            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                //
                //                            StreamResult result = new StreamResult(new StringWriter());
                //                            DOMSource source = new DOMSource(doc);
                //                            try {
                //                                transformer.transform(source, result);
                //                            } catch (TransformerException e) {
                //                                throw new ContainerException(e);
                //                            }
                //                            String xmlString = result.getWriter().toString();
                //                            System.out.println(xmlString); //TODO write to file using writeToXmlFile
                //                            break; // Only the 1st web.xml file need to be modified
                //                        } else {
                //                            Debug.logInfo("Unable to change the deployment descriptor : " + webXmlLocation + ". Maybe it does not exist, or is in read only mode ?", module);
                //                        }
                //                    }
                //                }

                //Debug.log("Using Data : " + dataMap, module);
                for (int i = 0; i < files.length; i++) {
                    if (!(files[i].isDirectory() || files[i].isHidden() || files[i].getName().equalsIgnoreCase("geronimo-web.xml"))) {
                        parseTemplate(files[i], dataMap);
                    }
                }

                String ofbizName = "ofbiz" + instanceNumber;
                String separator = File.separator;
                File workingDir = new File(geronimoHome + separator + "bin");
                ProcessBuilder processBuilder = null;
                Process process = null;
                String command = null;
                String commandCommonPart = null;
                if ("\\".equals(separator)) {   //Windows
                    commandCommonPart = "deploy --user " + user +  " --password " +  password;
                } else {                        // Linux
                    commandCommonPart = workingDir + "/deploy.sh --user " + user +  " --password " +  password;
                }
                if (UtilValidate.isNotEmpty(host)) {
                    commandCommonPart += " --host " + host + (UtilValidate.isNotEmpty(port) ? " --port " + port : "");
                }

                if (!redeploy) {
                if ("\\".equals(separator)) { //Windows
                    if (offline) {
                        command = commandCommonPart + " --offline undeploy " + ofbizName;
                    } else {
                        command = commandCommonPart + " undeploy " + ofbizName;
                    }
                        processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
                } else {                        // Linux
                    if (offline) {
                        command = commandCommonPart + " --offline undeploy " + ofbizName;
                    } else {
                        command = commandCommonPart + " undeploy " + ofbizName;
                    }
                        processBuilder = new ProcessBuilder("sh", "-c", command);
                }

                if (pauseInGeronimoScript) {
                        Map<String, String> env = processBuilder.environment();
                    env.put("GERONIMO_BATCH_PAUSE", "on");
                }
                    processBuilder.directory(workingDir);

                try {
                    System.out.println("Currently undeploying " + ofbizName + ", using : <<" + command + ">>, please wait ...");
                        processBuilder.redirectErrorStream(true);
                        process = processBuilder.start();
                        java.io.InputStream is = process.getInputStream();
                    byte[] buf = new byte[2024];
                    int readLen = 0;
                    while ((readLen = is.read(buf,0,buf.length)) != -1) {
                        if ("\\".equals(separator)) {   //Windows
                            System.out.print(new String(buf,0,readLen));
                        } else {
                            System.out.println(new String(buf,0,readLen));
                        }
                    }
                    is.close();
                        process.waitFor();
    //                    System.out.println(process.waitFor());
    //                    System.out.println("exit value" + process.exitValue());
                    Debug.logInfo(ofbizName + " undeployment ended" , module);
                } catch (IOException e) {
                    throw new ContainerException(e);
                } catch (InterruptedException e) {
                    throw new ContainerException(e);
                    } finally {
                        process.destroy();
                    }
                }

                if (redeploy) {
                    if ("\\".equals(separator)) { //Windows
                        command = commandCommonPart + " redeploy " + ofbizHome;
                        processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
                    } else {                      // Linux
                        command = commandCommonPart + " redeploy " + ofbizHome;
                        processBuilder = new ProcessBuilder("sh", "-c", command);
                }

                } else {
                if ("\\".equals(separator)) { //Windows
                    if (offline) {
                            command = commandCommonPart + " --offline deploy --inPlace " + ofbizHome;
                    } else {
                            command = commandCommonPart + " deploy --inPlace " + ofbizHome;
                    }
                        processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
                } else {                      // Linux
                    if (offline) {
                            command = commandCommonPart + " --offline deploy --inPlace " + ofbizHome;
                    } else {
                            command = commandCommonPart + " deploy --inPlace " + ofbizHome;
                        }
                        processBuilder = new ProcessBuilder("sh", "-c", command);
                    }
                }

                if (pauseInGeronimoScript) {
                    Map<String, String> env = processBuilder.environment();
                    env.put("GERONIMO_BATCH_PAUSE", "on");
                }
                processBuilder.directory(workingDir);

                try {
                    System.out.println("Currently deploying " + ofbizName + ", using : <<" + command + ">>, please wait ...");
                    processBuilder.redirectErrorStream(true);
                    process = processBuilder.start();
                    java.io.InputStream is = process.getInputStream();
                    byte[] buf = new byte[2024];
                    int readLen = 0;
                    while ((readLen = is.read(buf,0,buf.length)) != -1) {
                        if ("\\".equals(separator)) {   //Windows
                            System.out.print(new String(buf,0,readLen));
                        } else {
                            System.out.println(new String(buf,0,readLen));
                        }
                    }
                    is.close();
                    process.waitFor();
//                    System.out.println(process.waitFor());
//                    System.out.println("exit value" + process.exitValue());
                    Debug.logInfo(ofbizName + " deployment ended" , module);
                } catch (IOException e) {
                    throw new ContainerException(e);
                } catch (InterruptedException e) {
                    throw new ContainerException(e);
                } finally {
                    process.destroy();
                }
            }
        } else {
            //Debug.log("Using Data : " + dataMap, module);
            for (int i = 0; i < files.length; i++) {
                if (!files[i].isDirectory() && !files[i].isHidden()) {
                    parseTemplate(files[i], dataMap);
                }
            }
        }
    }

    private File[] getTemplates() throws ContainerException {
        if (args == null) {
            throw new ContainerException("Invalid application server type argument passed");
        }

        String templateLocation = args[0];
        if (templateLocation == null) {
            throw new ContainerException("Unable to locate Application Server template directory");
        }

        File parentDir = new File(ofbizHome + source + templateLocation);
        if (!parentDir.exists() || !parentDir.isDirectory()) {
            throw new ContainerException("Template location - " + templateLocation + " does not exist!");
        }

        return parentDir.listFiles();
    }

    private Map<String, Object> buildDataMap() throws ContainerException {
        Map<String, Object> dataMap = FastMap.newInstance();
        List<?> c[] = getClasspath();
        dataMap.put("targetDirectory", getTargetDirectory());
        dataMap.put("pathSeparatorChar", File.pathSeparatorChar);
        dataMap.put("classpath", System.getProperty("java.class.path"));
        dataMap.put("classpathJars", c[0]);
        dataMap.put("classpathDirs", c[1]);
        dataMap.put("env", System.getProperties());
        dataMap.put("webApps", ComponentConfig.getAllWebappResourceInfos());
        dataMap.put("ofbizHome", System.getProperty("ofbiz.home"));
        return dataMap;
    }

    private List<?>[] getClasspath() {
        Classpath classPath = new Classpath(System.getProperty("java.class.path"));
        List<File> elements = classPath.getElements();
        List<String> jar = FastList.newInstance();
        List<String> dir = FastList.newInstance();

        for (File f: elements) {
            if (f.exists()) {
                if (f.isDirectory()) {
                    String path = f.getAbsolutePath();
                    dir.add(path.replace('\\','/'));
                } else {
                    String path = f.getAbsolutePath();
                    jar.add(path.replace('\\','/'));
                }
            }
        }
        List<?>[] lists = { jar, dir };
        return lists;
    }

    private String getTargetDirectory() throws ContainerException {
        // create the target file/directory
        String targetDirectoryName = args.length > 1 ? args[1] : null;
        if (targetDirectoryName == null) {
            targetDirectoryName = target;
        }
        String targetDirectory = null;
        if (!isGeronimo) {
            targetDirectory = ofbizHome + targetDirectoryName + args[0];
        } else {
            targetDirectory = ofbizHome + targetDirectoryName;
        }
        File targetDir = new File(targetDirectory);
        if (!targetDir.exists()) {
            boolean created = targetDir.mkdirs();
            if (!created) {
                throw new ContainerException("Unable to create target directory - " + targetDirectory);
            }
        }

        if (!targetDirectory.endsWith("/")) {
            targetDirectory = targetDirectory + "/";
        }

        return targetDirectory;
    }

    private void parseTemplate(File templateFile, Map<String, Object> dataMap) throws ContainerException {
        Debug.log("Parsing template : " + templateFile.getAbsolutePath(), module);
        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(templateFile));
        } catch (FileNotFoundException e) {
            throw new ContainerException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    throw new ContainerException(e);
                }
            }
        }

        // create the target file/directory
        String targetDirectory = getTargetDirectory();

        // write the template to the target directory
        Writer writer = null;
        try {
            writer = new FileWriter(targetDirectory + templateFile.getName());
            try {
                FreeMarkerWorker.renderTemplate(UtilURL.fromFilename(templateFile.getAbsolutePath()).toExternalForm(), dataMap, writer);
            } catch (Exception e) {
                throw new ContainerException(e);
            }
        } catch (IOException e) {
            throw new ContainerException(e);
        } finally {
            try {
                if (writer != null) {
                    writer.flush();
                    writer.close();                    
                }
            } catch (IOException e) {
                throw new ContainerException(e);
            }            
        }

    }

    // This method writes a DOM document to a file
    public static void writeToXmlFile(Document doc, String filename) {
        try {
            // Prepare the DOM document for writing
            Source source = new DOMSource(doc);

            // Prepare the output file
            File file = new File(filename);
            Result result = new StreamResult(file);

            // Write the DOM document to the file
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
        } catch (TransformerException e) {
        }
    }

    public boolean isFileExistsAndCanWrite(String fileName) {
        File f = new File(fileName);
        return f.exists() && f.canWrite();
    }
}
