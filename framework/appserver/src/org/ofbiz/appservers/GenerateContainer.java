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

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.io.*;

import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.base.start.Classpath;
import org.ofbiz.base.component.ComponentConfig;

/**
 * GenerateContainer - Generates Configuration Files For Application Servers
 * ** This container requires StartInfoLoader to be loaded at startup.
 * ** This container requires the ComponentContainer to be loaded first.
 * 
 */
public class GenerateContainer implements Container {

    public static final String module = GenerateContainer.class.getName();
    public static final String source = "/framework/appserver/templates/";
    public static final String target = "/setup/";

    protected String configFile = null;
    protected String ofbizHome = null;
    protected String args[] = null;


    /**
     * @see org.ofbiz.base.container.Container#init(java.lang.String[], java.lang.String)
     */
    public void init(String[] args, String configFile) {
        this.ofbizHome = System.getProperty("ofbiz.home");
        this.configFile = configFile;
        this.args = args;
    }

    /**
     * @see org.ofbiz.base.container.Container#start()
     */
    public boolean start() throws ContainerException {
        this.generateFiles();
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
        File files[] = getTemplates();
        Map dataMap = buildDataMap();

        //Debug.log("Using Data : " + dataMap, module);
        for (int i = 0; i < files.length; i++) {
            if (!files[i].isDirectory() && !files[i].isHidden()) {
                parseTemplate(files[i], dataMap);
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

    private Map buildDataMap() {
        Map dataMap = new HashMap();
        List c[] = getClasspath();
        dataMap.put("classpathJars", c[0]);
        dataMap.put("classpathDirs", c[1]);
        dataMap.put("env", System.getProperties());
        dataMap.put("webApps", ComponentConfig.getAllWebappResourceInfos());
        return dataMap;
    }

    private List[] getClasspath() {
        Classpath classPath = new Classpath(System.getProperty("java.class.path"));
        List elements = classPath.getElements();
        List jar = new ArrayList();
        List dir = new ArrayList();

        Iterator i = elements.iterator();
        while (i.hasNext()) {
            File f = (File) i.next();
            if (f.exists()) {
                if (f.isDirectory()) {
                    dir.add(f.getAbsolutePath());
                } else {
                    jar.add(f.getAbsolutePath());
                }
            }
        }

        List[] lists = { jar, dir };
        return lists;
    }

    private void parseTemplate(File templateFile, Map dataMap) throws ContainerException {
        Debug.log("Parsing template : " + templateFile.getAbsolutePath(), module);

        // create the target file/directory
        String targetDirectoryName = args.length > 1 ? args[1] : null;
        if (targetDirectoryName == null) {
            targetDirectoryName = target;
        }
        String targetDirectory = ofbizHome + targetDirectoryName + args[0];
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

        // write the template to the target directory
        Writer writer = null;
        try {
            writer = new FileWriter(targetDirectory + templateFile.getName());
        } catch (IOException e) {
            throw new ContainerException(e);
        }
        try {
            FreeMarkerWorker.renderTemplate(UtilURL.fromFilename(templateFile.getAbsolutePath()).toExternalForm(), dataMap, writer);
        } catch (Exception e) {
            throw new ContainerException(e);
        }

        try {
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new ContainerException(e);
        }
    }
}
