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
package org.apache.ofbiz.entity.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.config.GenericConfigException;
import org.apache.ofbiz.base.config.MainResourceHandler;
import org.apache.ofbiz.base.config.ResourceHandler;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityConfException;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.config.model.Datasource;
import org.apache.ofbiz.entity.config.model.EntityConfig;
import org.apache.ofbiz.entity.config.model.EntityDataReader;
import org.apache.ofbiz.entity.config.model.ReadData;
import org.apache.ofbiz.entity.config.model.Resource;
import org.apache.ofbiz.entity.config.model.SqlLoadPath;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelReader;
import org.apache.ofbiz.entity.model.ModelUtil;
import org.apache.ofbiz.entity.model.ModelViewEntity;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Some utility routines for loading seed data.
 */
public class EntityDataLoader {

    public static final String module = EntityDataLoader.class.getName();

    public static String getPathsString(String helperName) {
        StringBuilder pathBuffer = new StringBuilder();
        if (UtilValidate.isNotEmpty(helperName)) {
            Datasource datasourceInfo = EntityConfig.getDatasource(helperName);
            for (SqlLoadPath sqlLoadPath : datasourceInfo.getSqlLoadPathList()) {
                String prependEnv = sqlLoadPath.getPrependEnv();
                pathBuffer.append(pathBuffer.length() == 0 ? "" : ";");
                if (UtilValidate.isNotEmpty(prependEnv)) {
                    pathBuffer.append(System.getProperty(prependEnv));
                    pathBuffer.append("/");
                }
                pathBuffer.append(sqlLoadPath.getPath());
            }
        }
        return pathBuffer.toString();
    }

    public static List<URL> getUrlList(String helperName) {
        Datasource datasourceInfo = EntityConfig.getDatasource(helperName);
        return getUrlList(helperName, null, datasourceInfo.getReadDataList());
    }

    public static List<URL> getUrlList(String helperName, String componentName) {
        Datasource datasourceInfo = EntityConfig.getDatasource(helperName);
        return getUrlList(helperName, componentName, datasourceInfo.getReadDataList());
    }

    public static <E> List<URL> getUrlList(String helperName, List<E> readerNames) {
        return getUrlList(helperName, null, readerNames);
    }

    public static <E> List<URL> getUrlList(String helperName, String componentName, List<E> readerNames) {
        String paths = getPathsString(helperName);
        List<URL> urlList = new LinkedList<>();

        // first get files from resources
        if (readerNames != null) {
            for (Object readerInfo: readerNames) {
                String readerName = null;
                if (readerInfo instanceof String) {
                    readerName = (String) readerInfo;
                } else if (readerInfo instanceof ReadData) {
                    readerName = ((ReadData) readerInfo).getReaderName();
                } else if (readerInfo instanceof Element) {
                    readerName = ((Element) readerInfo).getAttribute("reader-name");
                } else {
                    throw new IllegalArgumentException("Reader name list does not contain String(s) or Element(s)");
                }
                readerName = readerName.trim();

                // ignore the "tenant" reader if multitenant is disabled
                if ("tenant".equals(readerName) && !EntityUtil.isMultiTenantEnabled()) {
                    continue;
                }

                // get all of the main resource model stuff, ie specified in the entityengine.xml file
                EntityDataReader entityDataReaderInfo = null;
                try {
                    entityDataReaderInfo = EntityConfig.getInstance().getEntityDataReader(readerName);
                    if (entityDataReaderInfo == null) {
                        // create a reader name defined at runtime
                        Debug.logInfo("Could not find entity-data-reader named: " + readerName + ". Creating a new reader with this name. ", module);
                        entityDataReaderInfo = new EntityDataReader(readerName);
                    }
                } catch (GenericEntityConfException e) {
                    Debug.logWarning(e, "Exception thrown while getting entity data reader config: ", module);
                }
                if (entityDataReaderInfo != null) {
                    for (Resource resourceElement: entityDataReaderInfo.getResourceList()) {
                        ResourceHandler handler = new MainResourceHandler(EntityConfig.ENTITY_ENGINE_XML_FILENAME, resourceElement.getLoader(), resourceElement.getLocation());
                        try {
                            urlList.add(handler.getURL());
                        } catch (GenericConfigException e) {
                            String errorMsg = "Could not get URL for Main ResourceHandler: " + e.toString();
                            Debug.logWarning(errorMsg, module);
                        }
                    }

                    // get all of the component resource model stuff, ie specified in each ofbiz-component.xml file
                    for (ComponentConfig.EntityResourceInfo componentResourceInfo: ComponentConfig.getAllEntityResourceInfos("data", componentName)) {
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
                    String errorMsg = "Could not find entity-data-reader named: " + readerName;
                    Debug.logWarning(errorMsg, module);
                }
            }
        } else {
            String errorMsg = "Could not find datasource named: " + helperName;
            Debug.logWarning(errorMsg, module);
        }

        // get files from the paths string
        if (UtilValidate.isNotEmpty(paths)) {
            StringTokenizer tokenizer = new StringTokenizer(paths, ";");
            while (tokenizer.hasMoreTokens()) {
                String path = tokenizer.nextToken().toLowerCase(Locale.getDefault());
                File loadDir = new File(path);
                if (loadDir.exists() && loadDir.isDirectory()) {
                    File[] files = loadDir.listFiles();
                    List<File> tempFileList = new LinkedList<>();
                    if (files != null) {
                        for (File file : files) {
                            if (file.getName().toLowerCase(Locale.getDefault()).endsWith(".xml")) {
                                tempFileList.add(file);
                            }
                        }
                    }
                    Collections.sort(tempFileList);
                    for (File dataFile: tempFileList) {
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

    public static List<URL> getUrlByComponentList(String helperName, List<String> components, List<String> readerNames) {
        List<URL> urlList = new LinkedList<>();
        for (String readerName:  readerNames) {
            List<String> loadReaderNames = new LinkedList<>();
            loadReaderNames.add(readerName);
            for (String component : components) {
                urlList.addAll(getUrlList(helperName, component, loadReaderNames));
            }
        }
        return urlList;
    }

    public static List<URL> getUrlByComponentList(String helperName, List<String> components) {
        Datasource datasourceInfo = EntityConfig.getDatasource(helperName);
        List<String> readerNames = new LinkedList<>();
        for (ReadData readerInfo :  datasourceInfo.getReadDataList()) {
            String readerName = readerInfo.getReaderName();
            // ignore the "tenant" reader if the multitenant property is "N"
            if ("tenant".equals(readerName) && "N".equals(UtilProperties.getPropertyValue("general", "multitenant"))) {
                continue;
            }

            readerNames.add(readerName);
        }
        return getUrlByComponentList(helperName, components, readerNames);
    }

    public static int loadData(URL dataUrl, String helperName, Delegator delegator, List<Object> errorMessages) throws GenericEntityException {
        return loadData(dataUrl, helperName, delegator, errorMessages, -1);
    }

    public static int loadData(URL dataUrl, String helperName, Delegator delegator, List<Object> errorMessages, int txTimeout) throws GenericEntityException {
        return loadData(dataUrl, helperName, delegator, errorMessages, txTimeout, false, false, false);
    }

    public static int loadData(URL dataUrl, String helperName, Delegator delegator, List<Object> errorMessages, int txTimeout, boolean dummyFks, boolean maintainTxs, boolean tryInsert) throws GenericEntityException {
        return loadData(dataUrl, helperName, delegator, errorMessages, txTimeout, false, false, false, true);
    }

    public static int loadData(URL dataUrl, String helperName, Delegator delegator, List<Object> errorMessages, int txTimeout, boolean dummyFks, boolean maintainTxs, boolean tryInsert, boolean continueOnFail) throws GenericEntityException {
        int rowsChanged = 0;

        if (dataUrl == null) {
            String errMsg = "Cannot load data, dataUrl was null";
            errorMessages.add(errMsg);
            Debug.logError(errMsg, module);
            return 0;
        }

        if (Debug.verboseOn()) Debug.logVerbose("[loadData] Loading XML Resource: \"" + dataUrl.toExternalForm() + "\"", module);

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
            reader.setContinueOnFail(continueOnFail);
            rowsChanged += reader.parse(dataUrl);
        } catch (IOException | SAXException e) {
            String xmlError = "[loadData]: Error loading XML Resource \"" + dataUrl.toExternalForm() + "\"; Error was: " + e.getMessage();
            errorMessages.add(xmlError);
            if (continueOnFail) {
                Debug.logError(e, xmlError, module);
            } else {
                throw new GenericEntityException(xmlError, e);
            }
        }

        return rowsChanged;
    }

    public static int generateData(Delegator delegator, List<Object> errorMessages) throws GenericEntityException {
        int rowsChanged = 0;
        ModelReader reader = delegator.getModelReader();
        for (String entityName: reader.getEntityNames()) {
            ModelEntity entity = reader.getModelEntity(entityName);
            String baseName = entity.getPlainTableName();
            if (entity instanceof ModelViewEntity) {
                baseName = ModelUtil.javaNameToDbName(entity.getEntityName());
            }

            if (baseName != null) {
                try {
                    List<GenericValue> toBeStored = new LinkedList<>();
                    toBeStored.add(
                        delegator.makeValue(
                            "SecurityPermission",
                                "permissionId",
                                baseName + "_ADMIN",
                                "description",
                                "Permission to Administer a " + entity.getEntityName() + " entity."));
                    toBeStored.add(delegator.makeValue("SecurityGroupPermission", "groupId", "FULLADMIN", "permissionId", baseName + "_ADMIN", "fromDate", UtilDateTime.nowTimestamp()));
                    rowsChanged += delegator.storeAll(toBeStored);
                } catch (GenericEntityException e) {
                    errorMessages.add("[generateData] ERROR: Failed Security Generation for entity \"" + baseName + "\"");
                }

                /*
                toStore.add(delegator.makeValue("SecurityPermission", "permissionId", baseName + "_VIEW", "description", "Permission to View a " + entity.getEntityName() + " entity."));
                toStore.add(delegator.makeValue("SecurityPermission", "permissionId", baseName + "_CREATE", "description", "Permission to Create a " + entity.getEntityName() + " entity."));
                toStore.add(delegator.makeValue("SecurityPermission", "permissionId", baseName + "_UPDATE", "description", "Permission to Update a " + entity.getEntityName() + " entity."));
                toStore.add(delegator.makeValue("SecurityPermission", "permissionId", baseName + "_DELETE", "description", "Permission to Delete a " + entity.getEntityName() + " entity."));

                toStore.add(delegator.makeValue("SecurityGroupPermission", "groupId", "FLEXADMIN", "permissionId", baseName + "_VIEW"));
                toStore.add(delegator.makeValue("SecurityGroupPermission", "groupId", "FLEXADMIN", "permissionId", baseName + "_CREATE"));
                toStore.add(delegator.makeValue("SecurityGroupPermission", "groupId", "FLEXADMIN", "permissionId", baseName + "_UPDATE"));
                toStore.add(delegator.makeValue("SecurityGroupPermission", "groupId", "FLEXADMIN", "permissionId", baseName + "_DELETE"));
                */
            }
        }

        return rowsChanged;
    }
}
