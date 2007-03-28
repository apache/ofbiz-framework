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
package org.ofbiz.entity.util;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.w3c.dom.Element;

import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.config.MainResourceHandler;
import org.ofbiz.base.config.ResourceHandler;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.ofbiz.entity.config.EntityDataReaderInfo;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelReader;
import org.ofbiz.entity.model.ModelUtil;
import org.ofbiz.entity.model.ModelViewEntity;

/**
 * Some utility routines for loading seed data.
 */
public class EntityDataLoader {

    public static final String module = EntityDataLoader.class.getName();

    public static String getPathsString(String helperName) {
        StringBuffer pathBuffer = new StringBuffer();
        if (helperName != null && helperName.length() > 0) {
            DatasourceInfo datasourceInfo = EntityConfigUtil.getDatasourceInfo(helperName);
            List sqlLoadPathElements = datasourceInfo.sqlLoadPaths;
            Iterator slpIter = sqlLoadPathElements.iterator();
            while (slpIter.hasNext()) {
                Element sqlLoadPathElement = (Element) slpIter.next();
                String prependEnv = sqlLoadPathElement.getAttribute("prepend-env");
                pathBuffer.append(pathBuffer.length() == 0 ? "" : ";");
                if (prependEnv != null && prependEnv.length() > 0) {
                    pathBuffer.append(System.getProperty(prependEnv));
                    pathBuffer.append("/");
                }
                pathBuffer.append(sqlLoadPathElement.getAttribute("path"));
            }
        }
        return pathBuffer.toString();
    }

    public static List getUrlList(String helperName) {
        DatasourceInfo datasourceInfo = EntityConfigUtil.getDatasourceInfo(helperName);
        return getUrlList(helperName, null, datasourceInfo.readDatas);
    }

    public static List getUrlList(String helperName, String componentName) {
        DatasourceInfo datasourceInfo = EntityConfigUtil.getDatasourceInfo(helperName);
        return getUrlList(helperName, componentName, datasourceInfo.readDatas);
    }

    public static List getUrlList(String helperName, List readerNames) {
        return getUrlList(helperName, null, readerNames);
    }

    public static List getUrlList(String helperName, String componentName, List readerNames) {
        String paths = getPathsString(helperName);
        List urlList = new LinkedList();
        
        // first get files from resources
        if (readerNames != null) {
            Iterator readDataIter = readerNames.iterator();
            while (readDataIter.hasNext()) {
                Object readerInfo = readDataIter.next();
                String readerName = null;
                if (readerInfo instanceof String) {
                    readerName = (String) readerInfo;
                } else if (readerInfo instanceof Element) {
                    readerName = ((Element) readerInfo).getAttribute("reader-name");
                } else {
                    throw new IllegalArgumentException("Reader name list does not contain String(s) or Element(s)");
                }

                // get all of the main resource model stuff, ie specified in the entityengine.xml file
                EntityDataReaderInfo entityDataReaderInfo = EntityConfigUtil.getEntityDataReaderInfo(readerName);
                
                if (entityDataReaderInfo != null) {
                    List resourceElements = entityDataReaderInfo.resourceElements;
                    Iterator resIter = resourceElements.iterator();
                    while (resIter.hasNext()) {
                        Element resourceElement = (Element) resIter.next();
                        ResourceHandler handler = new MainResourceHandler(EntityConfigUtil.ENTITY_ENGINE_XML_FILENAME, resourceElement);
                        try {
                            urlList.add(handler.getURL());
                        } catch (GenericConfigException e) {
                            String errorMsg = "Could not get URL for Main ResourceHandler: " + e.toString();
                            Debug.logWarning(errorMsg, module);
                        }
                    }
        
                    // get all of the component resource model stuff, ie specified in each ofbiz-component.xml file
                    List componentResourceInfos = ComponentConfig.getAllEntityResourceInfos("data", componentName);
                    Iterator componentResourceInfoIter = componentResourceInfos.iterator();
                    while (componentResourceInfoIter.hasNext()) {
                        ComponentConfig.EntityResourceInfo componentResourceInfo = (ComponentConfig.EntityResourceInfo) componentResourceInfoIter.next();
                        if (readerName.equals(componentResourceInfo.readerName)) {
                            ResourceHandler handler = componentResourceInfo.createResourceHandler();
                            try {
                                urlList.add(handler.getURL());
                            } catch (GenericConfigException e) {
                                String errorMsg = "Could not get URL for Component ResourceHandler: " + e.toString();
                                Debug.logWarning(errorMsg, module);
                            }
                        }
                    }
                } else {
                    String errorMsg = "Could not find entity-date-reader named: " + readerName;
                    Debug.logWarning(errorMsg, module);
                }
            }
        } else {
            String errorMsg = "Could not find datasource named: " + helperName;
            Debug.logWarning(errorMsg, module);
        }
        
        // get files from the paths string
        if (paths != null && paths.length() > 0) {
            StringTokenizer tokenizer = new StringTokenizer(paths, ";");
            while (tokenizer.hasMoreTokens()) {
                String path = tokenizer.nextToken().toLowerCase();
                File loadDir = new File(path);
                if (loadDir.exists() && loadDir.isDirectory()) {
                    File[] files = loadDir.listFiles();
                    List tempFileList = new LinkedList();
                    for (int i = 0; i < files.length; i++) {
                        if (files[i].getName().toLowerCase().endsWith(".xml")) {
                            tempFileList.add(files[i]);
                        }
                    }
                    Collections.sort(tempFileList);
                    Iterator tempFileIter = tempFileList.iterator();
                    while (tempFileIter.hasNext()) {
                        File dataFile = (File) tempFileIter.next();
                        if (dataFile.exists()) {
                            URL url = null;
                            try {
                                url = dataFile.toURI().toURL();
                                urlList.add(url);
                            } catch (java.net.MalformedURLException e) {
                                String xmlError = "Error loading XML file \"" + dataFile.getAbsolutePath() + "\"; Error was: " + e.getMessage();
                                Debug.logError(xmlError, module);
                            }
                        } else {
                            String errorMsg = "Could not find file: \"" + dataFile.getAbsolutePath() + "\"";
                            Debug.logError(errorMsg, module);
                        }
                    }
                }
            }
        }

        return urlList;
    }

    public static int loadData(URL dataUrl, String helperName, GenericDelegator delegator, List errorMessages) throws GenericEntityException {
        return loadData(dataUrl, helperName, delegator, errorMessages, -1);
    }

    public static int loadData(URL dataUrl, String helperName, GenericDelegator delegator, List errorMessages, int txTimeout) throws GenericEntityException {
        return loadData(dataUrl, helperName, delegator, errorMessages, txTimeout, false, false, false);
    }

    public static int loadData(URL dataUrl, String helperName, GenericDelegator delegator, List errorMessages, int txTimeout, boolean dummyFks, boolean maintainTxs, boolean tryInsert) throws GenericEntityException {
        int rowsChanged = 0;
        
        if (dataUrl == null) {
            String errMsg = "Cannot load data, dataUrl was null";
            errorMessages.add(errMsg);
            Debug.logError(errMsg, module);
            return 0;
        }

        Debug.logVerbose("[install.loadData] Loading XML Resource: \"" + dataUrl.toExternalForm() + "\"", module);

        try {
            /* The OLD way
              List toBeStored = delegator.readXmlDocument(url);
              delegator.storeAll(toBeStored);
              rowsChanged += toBeStored.size();
             */

            EntitySaxReader reader = null;
            if (txTimeout > 0) {
                reader = new EntitySaxReader(delegator, txTimeout);
            } else {
                reader = new EntitySaxReader(delegator);
            }
            reader.setCreateDummyFks(dummyFks);
            reader.setMaintainTxStamps(maintainTxs);
            rowsChanged += reader.parse(dataUrl);
        } catch (Exception e) {
            String xmlError = "[install.loadData]: Error loading XML Resource \"" + dataUrl.toExternalForm() + "\"; Error was: " + e.getMessage();
            errorMessages.add(xmlError);
            Debug.logError(e, xmlError, module);
        }

        return rowsChanged;
    }

    public static int generateData(GenericDelegator delegator, List errorMessages) throws GenericEntityException {
        int rowsChanged = 0;
        ModelReader reader = delegator.getModelReader();
        Collection entityCol = reader.getEntityNames();
        Iterator classNamesIterator = entityCol.iterator();
        while (classNamesIterator != null && classNamesIterator.hasNext()) {
            ModelEntity entity = reader.getModelEntity((String) classNamesIterator.next());
            String baseName = entity.getPlainTableName();
            if (entity instanceof ModelViewEntity) {
                baseName = ModelUtil.javaNameToDbName(entity.getEntityName());
            }

            if (baseName != null) {
                try {
                    List toBeStored = new LinkedList();
                    toBeStored.add(
                        delegator.makeValue(
                            "SecurityPermission",
                            UtilMisc.toMap(
                                "permissionId",
                                baseName + "_ADMIN",
                                "description",
                                "Permission to Administer a " + entity.getEntityName() + " entity.")));
                    toBeStored.add(delegator.makeValue("SecurityGroupPermission", UtilMisc.toMap("groupId", "FULLADMIN", "permissionId", baseName + "_ADMIN")));
                    rowsChanged += delegator.storeAll(toBeStored);
                } catch (GenericEntityException e) {
                    errorMessages.add("[install.generateData] ERROR: Failed Security Generation for entity \"" + baseName + "\"");
                }

                /*
                toStore.add(delegator.makeValue("SecurityPermission", UtilMisc.toMap("permissionId", baseName + "_VIEW", "description", "Permission to View a " + entity.getEntityName() + " entity.")));
                toStore.add(delegator.makeValue("SecurityPermission", UtilMisc.toMap("permissionId", baseName + "_CREATE", "description", "Permission to Create a " + entity.getEntityName() + " entity.")));
                toStore.add(delegator.makeValue("SecurityPermission", UtilMisc.toMap("permissionId", baseName + "_UPDATE", "description", "Permission to Update a " + entity.getEntityName() + " entity.")));
                toStore.add(delegator.makeValue("SecurityPermission", UtilMisc.toMap("permissionId", baseName + "_DELETE", "description", "Permission to Delete a " + entity.getEntityName() + " entity.")));
                
                toStore.add(delegator.makeValue("SecurityGroupPermission", UtilMisc.toMap("groupId", "FLEXADMIN", "permissionId", baseName + "_VIEW")));
                toStore.add(delegator.makeValue("SecurityGroupPermission", UtilMisc.toMap("groupId", "FLEXADMIN", "permissionId", baseName + "_CREATE")));
                toStore.add(delegator.makeValue("SecurityGroupPermission", UtilMisc.toMap("groupId", "FLEXADMIN", "permissionId", baseName + "_UPDATE")));
                toStore.add(delegator.makeValue("SecurityGroupPermission", UtilMisc.toMap("groupId", "FLEXADMIN", "permissionId", baseName + "_DELETE")));
                */
            }
        }

        return rowsChanged;
    }
}
