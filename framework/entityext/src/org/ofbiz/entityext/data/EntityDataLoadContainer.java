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
package org.ofbiz.entityext.data;

import java.net.URL;
import java.net.MalformedURLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.io.File;

import javolution.util.FastList;

import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerConfig;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.datasource.GenericHelperInfo;
import org.ofbiz.entity.jdbc.DatabaseUtil;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.util.EntityDataLoader;
import org.ofbiz.service.ServiceDispatcher;


/**
 * Some utility routines for loading seed data.
 */
public class EntityDataLoadContainer implements Container {

    public static final String module = EntityDataLoadContainer.class.getName();

    protected String overrideDelegator = null;
    protected String overrideGroup = null;
    protected String configFile = null;
    protected String readers = null;
    protected String directory = null;
    protected List<String> files = FastList.newInstance();
    protected String component = null;
    protected boolean useDummyFks = false;
    protected boolean maintainTxs = false;
    protected boolean tryInserts = false;
    protected boolean repairColumns = false;
    protected boolean dropPks = false;
    protected boolean createPks = false;
    protected boolean dropConstraints = false;
    protected boolean createConstraints = false;
    protected int txTimeout = -1;

    public EntityDataLoadContainer() {
        super();
    }

    /**
     * @see org.ofbiz.base.container.Container#init(java.lang.String[], java.lang.String)
     */
    public void init(String[] args, String configFile) throws ContainerException {
        this.configFile = configFile;
        // disable job scheduler, JMS listener and startup services
        ServiceDispatcher.enableJM(false);
        ServiceDispatcher.enableJMS(false);
        ServiceDispatcher.enableSvcs(false);

        /*
           install arguments:
           readers (none, all, seed, demo, ext, etc - configured in entityengine.xml and associated via ofbiz-component.xml)
           timeout (transaction timeout default 7200)
           delegator (overrides the delegator name configured for the container)
           group (overrides the entity group name configured for the container)
           dir (imports all XML files in a directory)
           file (import a specific XML file)

           Example:
           $ java -jar ofbiz.jar -install -readers=seed,demo,ext -timeout=7200 -delegator=default -group=org.ofbiz
           $ java -jar ofbiz.jar -install -file=/tmp/dataload.xml
        */
        if (args != null) {
            for (String argument: args) {
                // arguments can prefix w/ a '-'. Just strip them off
                if (argument.startsWith("-")) {
                    int subIdx = 1;
                    if (argument.startsWith("--")) {
                        subIdx = 2;
                    }
                    argument = argument.substring(subIdx);
                }

                // parse the arguments
                String argumentName;
                String argumentVal;
                if (argument.indexOf("=") != -1) {
                    argumentName = argument.substring(0, argument.indexOf("="));
                    argumentVal = argument.substring(argument.indexOf("=") + 1);
                } else {
                    argumentName = argument;
                    argumentVal = "";
                }
                Debug.log("Install Argument - " + argumentName + " = " + argumentVal, module);

                if ("readers".equalsIgnoreCase(argumentName)) {
                    this.readers = argumentVal;
                } else if ("timeout".equalsIgnoreCase(argumentName)) {
                    try {
                        this.txTimeout = Integer.parseInt(argumentVal);
                    } catch (Exception e) {
                        this.txTimeout = -1;
                    }
                } else if ("component".equalsIgnoreCase(argumentName)) {
                    this.component = argumentVal;
                } else if ("delegator".equalsIgnoreCase(argumentName)) {
                    this.overrideDelegator = argumentVal;
                } else if ("group".equalsIgnoreCase(argumentName)) {
                    this.overrideGroup = argumentVal;
                } else if ("file".equalsIgnoreCase(argumentName)) {
                    this.files.addAll(StringUtil.split(argumentVal, ","));
                } else if ("dir".equalsIgnoreCase(argumentName)) {
                    this.directory = argumentVal;
                } else if ("createfks".equalsIgnoreCase(argumentName)) {
                    this.useDummyFks = "true".equalsIgnoreCase(argumentVal);
                } else if ("maintainTxs".equalsIgnoreCase(argumentName)) {
                    this.maintainTxs = "true".equalsIgnoreCase(argumentVal);
                } else if ("inserts".equalsIgnoreCase(argumentName)) {
                    this.tryInserts = "true".equalsIgnoreCase(argumentVal);
                } else if ("repair-columns".equalsIgnoreCase(argumentName)) {
                    if (UtilValidate.isEmpty(argumentVal) || "true".equalsIgnoreCase(argumentVal)) {
                        repairColumns = true;
                    }
                } else if ("drop-pks".equalsIgnoreCase(argumentName)) {
                    if (UtilValidate.isEmpty(argumentVal) || "true".equalsIgnoreCase(argumentVal)) {
                        dropPks = true;
                    }
                } else if ("create-pks".equalsIgnoreCase(argumentName)) {
                    if (UtilValidate.isEmpty(argumentVal) || "true".equalsIgnoreCase(argumentVal)) {
                        createPks = true;
                    }
                } else if ("drop-constraints".equalsIgnoreCase(argumentName)) {
                    if (UtilValidate.isEmpty(argumentVal) || "true".equalsIgnoreCase(argumentVal)) {
                        dropConstraints = true;
                    }
                } else if ("create-constraints".equalsIgnoreCase(argumentName)) {
                    if (UtilValidate.isEmpty(argumentVal) || "true".equalsIgnoreCase(argumentVal)) {
                        createConstraints = true;
                    }
                } else if ("help".equalsIgnoreCase(argumentName)) {
                    Debug.log("--------------------------------------", module);
                    Debug.log("java -jar ofbiz.jar -install [options]", module);
                    Debug.log("-component=[name] .... only load from a specific component");
                    Debug.log("-delegator=[name] .... use the defined delegator (default-no-eca", module);
                    Debug.log("-group=[name] ........ override the entity group (org.ofbiz)", module);
                    Debug.log("-file=[path] ......... load a single file from location, several files separated by commas.", module);
                    Debug.log("-createfks ........... create dummy (placeholder) FKs", module);
                    Debug.log("-maintainTxs ......... maintain timestamps in data file", module);
                    Debug.log("-inserts ............. use mostly inserts option", module);
                    Debug.log("-repair-columns ........... repair column sizes", module);
                    Debug.log("-drop-pks ............ drop primary keys", module);
                    Debug.log("-create-pks .......... create primary keys", module);
                    Debug.log("-drop-constraints..... drop indexes and foreign keys before loading", module);
                    Debug.log("-create-constraints... create indexes and foreign keys after loading (default is true w/ drop-constraints)", module);
                    Debug.log("-help ................ display this information", module);
                    System.exit(1);
                }

                // special case
                if (this.readers == null && (!this.files.isEmpty() || this.directory != null)) {
                    this.readers = "none";
                }
            }
        }
    }

    /**
     * @see org.ofbiz.base.container.Container#start()
     */
    public boolean start() throws ContainerException {
        ContainerConfig.Container cfg = ContainerConfig.getContainer("dataload-container", configFile);
        ContainerConfig.Container.Property delegatorNameProp = cfg.getProperty("delegator-name");
        ContainerConfig.Container.Property entityGroupNameProp = cfg.getProperty("entity-group-name");

        String delegatorName = null;
        String entityGroupName = null;

        if (delegatorNameProp == null || UtilValidate.isEmpty(delegatorNameProp.value)) {
            throw new ContainerException("Invalid delegator-name defined in container configuration");
        } else {
            delegatorName = delegatorNameProp.value;
        }

        if (entityGroupNameProp == null || UtilValidate.isEmpty(entityGroupNameProp.value)) {
            throw new ContainerException("Invalid entity-group-name defined in container configuration");
        } else {
            entityGroupName = entityGroupNameProp.value;
        }

        // parse the pass in list of readers to use
        List<String> readerNames = null;
        if (this.readers != null && !"none".equalsIgnoreCase(this.readers)) {
            if (this.readers.indexOf(",") == -1) {
                readerNames = FastList.newInstance();
                readerNames.add(this.readers);
            } else {
                readerNames = StringUtil.split(this.readers, ",");
            }
        }

        String delegatorNameToUse = overrideDelegator != null ? overrideDelegator : delegatorName;
        String groupNameToUse = overrideGroup != null ? overrideGroup : entityGroupName;
        Delegator delegator = DelegatorFactory.getDelegator(delegatorNameToUse);
        if (delegator == null) {
            throw new ContainerException("Invalid delegator name!");
        }

        GenericHelperInfo helperInfo = delegator.getGroupHelperInfo(groupNameToUse);
        if (helperInfo == null) {
            throw new ContainerException("Unable to locate the datasource helper for the group [" + groupNameToUse + "]");
        }

        // get the database util object
        DatabaseUtil dbUtil = new DatabaseUtil(helperInfo);
        Map<String, ModelEntity> modelEntities;
        try {
            modelEntities = delegator.getModelEntityMapByGroup(groupNameToUse);
        } catch (GenericEntityException e) {
            throw new ContainerException(e.getMessage(), e);
        }
        TreeSet<String> modelEntityNames = new TreeSet<String>(modelEntities.keySet());

        // check for drop index/fks
        if (dropConstraints) {
            List<String> messages = FastList.newInstance();

            Debug.logImportant("Dropping foreign key indcies...", module);
            for (String entityName : modelEntityNames) {
                ModelEntity modelEntity = modelEntities.get(entityName);
                if (modelEntity != null) {
                    dbUtil.deleteForeignKeyIndices(modelEntity, messages);
                }
            }

            Debug.logImportant("Dropping declared indices...", module);
            for (String entityName : modelEntityNames) {
                ModelEntity modelEntity = modelEntities.get(entityName);
                if (modelEntity != null) {
                    dbUtil.deleteDeclaredIndices(modelEntity, messages);
                }
            }

            Debug.logImportant("Dropping foreign keys...", module);
            for (String entityName : modelEntityNames) {
                ModelEntity modelEntity = modelEntities.get(entityName);
                if (modelEntity != null) {
                    dbUtil.deleteForeignKeys(modelEntity, modelEntities, messages);
                }
            }

            if (messages.size() > 0) {
                if (Debug.infoOn()) {
                    for (String message : messages) {
                        Debug.logInfo(message, module);
                    }
                }
            }
        }

        // drop pks
        if (dropPks) {
            List<String> messages = FastList.newInstance();
            Debug.logImportant("Dropping primary keys...", module);
            for (String entityName : modelEntityNames) {
                ModelEntity modelEntity = modelEntities.get(entityName);
                if (modelEntity != null) {
                    dbUtil.deletePrimaryKey(modelEntity, messages);
                }
            }

            if (messages.size() > 0) {
                if (Debug.infoOn()) {
                    for (String message : messages) {
                        Debug.logInfo(message, module);
                    }
                }
            }
        }

        // repair columns
        if (repairColumns) {
            List<String> fieldsToRepair = FastList.newInstance();
            List<String> messages = FastList.newInstance();
            dbUtil.checkDb(modelEntities, fieldsToRepair, messages, false, false, false, false);
            if (fieldsToRepair.size() > 0) {
                messages = FastList.newInstance();
                dbUtil.repairColumnSizeChanges(modelEntities, fieldsToRepair, messages);
                if (messages.size() > 0) {
                    if (Debug.infoOn()) {
                        for (String message : messages) {
                            Debug.logInfo(message, module);
                        }
                    }
                }
            }
        }

        // get the reader name URLs first
        List<URL> urlList = null;
        if (readerNames != null) {
            urlList = EntityDataLoader.getUrlList(helperInfo.getHelperBaseName(), component, readerNames);
        } else if (!"none".equalsIgnoreCase(this.readers)) {
            urlList = EntityDataLoader.getUrlList(helperInfo.getHelperBaseName(), component);
        }

        // need a list if it is empty
        if (urlList == null) {
            urlList = FastList.newInstance();
        }

        // add in the defined extra files
        for (String fileName: this.files) {
            URL fileUrl = UtilURL.fromResource((String) fileName);
            if (fileUrl != null) {
                urlList.add(fileUrl);
            }
        }

        // next check for a directory of files
        if (this.directory != null) {
            File dir = new File(this.directory);
            if (dir.exists() && dir.isDirectory() && dir.canRead()) {
                File[] fileArray = dir.listFiles();
                if (fileArray != null && fileArray.length > 0) {
                    for (File file: fileArray) {
                        if (file.getName().toLowerCase().endsWith(".xml")) {
                            try {
                                urlList.add(file.toURI().toURL());
                            } catch (MalformedURLException e) {
                                Debug.logError(e, "Unable to load file (" + file.getName() + "); not a valid URL.", module);
                            }
                        }
                    }
                }
            }
        }

        // process the list of files
        NumberFormat changedFormat = NumberFormat.getIntegerInstance();
        changedFormat.setMinimumIntegerDigits(5);
        changedFormat.setGroupingUsed(false);

        List<Object> errorMessages = FastList.newInstance();
        List<String> infoMessages = FastList.newInstance();
        int totalRowsChanged = 0;
        if (UtilValidate.isNotEmpty(urlList)) {
            Debug.logImportant("=-=-=-=-=-=-= Doing a data load with the following files:", module);
            for (URL dataUrl: urlList) {
                Debug.logImportant(dataUrl.toExternalForm(), module);
            }

            Debug.logImportant("=-=-=-=-=-=-= Starting the data load...", module);

            for (URL dataUrl: urlList) {
                try {
                    int rowsChanged = EntityDataLoader.loadData(dataUrl, helperInfo.getHelperBaseName(), delegator, errorMessages, txTimeout, useDummyFks, maintainTxs, tryInserts);
                    totalRowsChanged += rowsChanged;
                    infoMessages.add(changedFormat.format(rowsChanged) + " of " + changedFormat.format(totalRowsChanged) + " from " + dataUrl.toExternalForm());
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Error loading data file: " + dataUrl.toExternalForm(), module);
                }
            }
        } else {
            Debug.logImportant("=-=-=-=-=-=-= No data load files found.", module);
        }

        if (infoMessages.size() > 0) {
            Debug.logImportant("=-=-=-=-=-=-= Here is a summary of the data load:", module);
            for (String message: infoMessages) {
              Debug.logImportant(message, module);
            }
        }

        if (errorMessages.size() > 0) {
            Debug.logImportant("The following errors occured in the data load:", module);
            for (Object message: errorMessages) {
              Debug.logImportant(message.toString(), module);
            }
        }

        Debug.logImportant("=-=-=-=-=-=-= Finished the data load with " + totalRowsChanged + " rows changed.", module);

        // create primary keys
        if (createPks) {
            List<String> messages = FastList.newInstance();

            Debug.logImportant("Creating primary keys...", module);
            for (String entityName : modelEntityNames) {
                ModelEntity modelEntity = modelEntities.get(entityName);
                if (modelEntity != null) {
                    dbUtil.createPrimaryKey(modelEntity, messages);
                }
            }
            if (messages.size() > 0) {
                if (Debug.infoOn()) {
                    for (String message : messages) {
                        Debug.logInfo(message, module);
                    }
                }
            }
        }

        // create constraints
        if (createConstraints) {
            List<String> messages = FastList.newInstance();

            Debug.logImportant("Creating foreign keys...", module);
            for (String entityName : modelEntityNames) {
                ModelEntity modelEntity = modelEntities.get(entityName);
                if (modelEntity != null) {
                    dbUtil.createForeignKeys(modelEntity, modelEntities, messages);
                }
            }

            Debug.logImportant("Creating foreign key indcies...", module);
            for (String entityName : modelEntityNames) {
                ModelEntity modelEntity = modelEntities.get(entityName);
                if (modelEntity != null) {
                    dbUtil.createForeignKeyIndices(modelEntity, messages);
                }
            }

            Debug.logImportant("Creating declared indices...", module);
            for (String entityName : modelEntityNames) {
                ModelEntity modelEntity = modelEntities.get(entityName);
                if (modelEntity != null) {
                    dbUtil.createDeclaredIndices(modelEntity, messages);
                }
            }

            if (messages.size() > 0) {
                if (Debug.infoOn()) {
                    for (String message : messages) {
                        Debug.logInfo(message, module);
                    }
                }
            }
        }

        return true;
    }

    /**
     * @see org.ofbiz.base.container.Container#stop()
     */
    public void stop() throws ContainerException {
    }
}
