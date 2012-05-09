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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.start.Classpath;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.template.FreeMarkerWorker;

/**
 * GenerateGeronimoRepository - Generate needed jars in Geronimo (or WASCE) repository
 *
 */
public class GenerateGeronimoDeployment {

    public static final String module = GenerateGeronimoDeployment.class.getName();
    public static final String source = "/framework/appserver/templates/";

    protected  String geronimoRepository = null;

    public List<String> generate(String geronimoVersion, String geronimoHome, String instanceNumber) {
        geronimoRepository = geronimoHome + "/repository";
        Debug.logInfo("The WASCE or Geronimo Repository is " + geronimoRepository, module);
        Classpath classPath = new Classpath(System.getProperty("java.class.path"));
        List<File> elements = classPath.getElements();
        List<String> jar_version = new ArrayList<String>();
        String jarPath = null;
        String jarName = null;
        String newJarName = null;
        String jarNameSimple = null;
        String jarVersion = "1.0";
        int lastDash = -1;

        for (File f: elements) {
            if (f.exists()) {
                if (f.isFile()) {
                    jarPath = f.getAbsolutePath();
                    jarName = f.getName();
                    String jarNameWithoutExt = (String) jarName.subSequence(0, jarName.length()-4);
                    lastDash = jarNameWithoutExt.lastIndexOf("-");
                    if (lastDash > -1) {
                        // get string between last dash and extension = version ?
                        jarVersion = jarNameWithoutExt.substring(lastDash+1, jarNameWithoutExt.length());
                        // get string before last dash
                        jarNameSimple = jarNameWithoutExt.substring(0, lastDash);
                        // Remove all but digits and "." and if length > 0 then it's already versioned
                        boolean alreadyVersioned = 0 < StringUtil.removeRegex(jarVersion, "[^.0123456789]").length();
                        if (! alreadyVersioned) {
                            jarVersion = "1.0"; // by default put 1.0 version
                            jarNameSimple = jarNameWithoutExt;
                            newJarName = jarNameWithoutExt + "-" + jarVersion + ".jar";
                        } else {
                            newJarName = jarName;
                        }
                    } else {
                        jarVersion = "1.0"; // by default put 1.0 version
                        jarNameSimple = jarNameWithoutExt;
                        newJarName = jarNameWithoutExt + "-" + jarVersion + ".jar";
                    }

                    jar_version.add(jarNameSimple + "#" + jarVersion);

                    String targetDirectory = geronimoRepository + "/org/ofbiz/" + jarNameSimple + "/" + jarVersion;
                    File targetDir = new File(targetDirectory);
                    if (!targetDir.exists()) {
                        boolean created = targetDir.mkdirs();
                        if (!created) {
                            Debug.logFatal("Unable to create target directory - " + targetDirectory, module);
                            return null;
                        }
                    }

                    if (!targetDirectory.endsWith("/")) {
                        targetDirectory = targetDirectory + "/";
                    }
                    String newCompleteJarName= targetDirectory + newJarName;

                    // copy the jar to the target directory
                    try {
                        // Create channel on the source
                        FileChannel srcChannel = new FileInputStream(jarPath).getChannel();

                        // Create channel on the destination
                        FileChannel dstChannel = new FileOutputStream(newCompleteJarName).getChannel();

                        // Copy file contents from source to destination
                        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                        Debug.logInfo("Created jar file : " + newJarName + " in WASCE or Geronimo repository", module);

                        // Close the channels
                        srcChannel.close();
                        dstChannel.close();
                    } catch (IOException e) {
                        Debug.logFatal("Unable to create jar file - " + newJarName + " in WASCE or Geronimo repository (certainly already exists)", module);
                        return null;
                    }
                }
            }
        }
        List<ComponentConfig.WebappInfo> webApps  = ComponentConfig.getAllWebappResourceInfos();
        File geronimoWebXml = new File(System.getProperty("ofbiz.home") + "/framework/appserver/templates/" + geronimoVersion + "/geronimo-web.xml");
        for (ComponentConfig.WebappInfo webApp: webApps) {
            if (null != webApp) {
                parseTemplate(geronimoWebXml, webApp);
            }
        }
        return jar_version;
    }

    private void parseTemplate(File templateFile, ComponentConfig.WebappInfo webApp) {
        Debug.logInfo("Parsing template : " + templateFile.getAbsolutePath() + " for web app " + webApp.getName(), module);

        Map<String, Object> dataMap= new HashMap<String, Object>();
        dataMap.put("webApp", webApp);
        String webAppGeronimoWebXmlFileName = webApp.getLocation() + "/WEB-INF/geronimo-web.xml";
        String webAppGeronimoWebInfDirName = webApp.getLocation() + "/WEB-INF";
        File webAppGeronimoWebInfDir = new File(webAppGeronimoWebInfDirName);

        if (!(webAppGeronimoWebInfDir.exists() && webAppGeronimoWebInfDir.isDirectory())) {
            Debug.logFatal("Unable to create - " + webAppGeronimoWebXmlFileName, module);
            Debug.logFatal("The directory "+ webAppGeronimoWebInfDirName + " does not exist", module);
            return;
        }

        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(templateFile));
        } catch (FileNotFoundException e) {
            Debug.logFatal("Unable to create - " + webAppGeronimoWebXmlFileName, module);
            return;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Debug.logError("Trouble closing - " + webAppGeronimoWebXmlFileName, module);
                }
            }
        }

        // write the template to the target directory
        Writer writer = null;
        try {
            writer = new FileWriter(webAppGeronimoWebXmlFileName);
        } catch (IOException e) {
            Debug.logFatal("Unable to create - " + webAppGeronimoWebXmlFileName, module);
            return;
        }
        try {
            FreeMarkerWorker.renderTemplate(UtilURL.fromFilename(templateFile.getAbsolutePath()).toExternalForm(), dataMap, writer);
        } catch (Exception e) {
            Debug.logFatal("Unable to create - " + webAppGeronimoWebXmlFileName, module);
            return;
        }

        try {
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Debug.logFatal("Unable to create - " + webAppGeronimoWebXmlFileName, module);
            return;
        }
    }
}
