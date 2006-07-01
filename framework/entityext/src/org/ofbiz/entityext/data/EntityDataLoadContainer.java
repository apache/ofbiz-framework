/*
 * $Id: EntityDataLoadContainer.java 7425 2006-04-26 23:04:59Z jonesde $
 *
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.entityext.data;

import java.net.URL;
import java.net.MalformedURLException;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.io.File;

import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerConfig;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.util.EntityDataLoader;
import org.ofbiz.service.ServiceDispatcher;


/**
 * Some utility routines for loading seed data.
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @since      3.1
 */
public class EntityDataLoadContainer implements Container {

    public static final String module = EntityDataLoadContainer.class.getName();

    protected String overrideDelegator = null;
    protected String overrideGroup = null;
    protected String configFile = null;
    protected String readers = null;
    protected String directory = null;
    protected String file = null;
    protected boolean useDummyFks = false;
    protected boolean maintainTxs = false;
    protected boolean tryInserts = false;
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
            for (int i = 0; i < args.length; i++) {
                String argument = args[i];
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
                    Debug.log("Install Argument - " + argumentName + " = " + argumentVal, module);
                    if ("readers".equalsIgnoreCase(argumentName)) {
                        this.readers = argumentVal;
                    } else if ("timeout".equalsIgnoreCase(argumentName)) {
                        try {
                            this.txTimeout = Integer.parseInt(argumentVal);
                        } catch (Exception e) {
                            this.txTimeout = -1;
                        }
                    } else if ("delegator".equalsIgnoreCase(argumentName)) {
                        this.overrideDelegator = argumentVal;
                    } else if ("group".equalsIgnoreCase(argumentName)) {
                        this.overrideGroup = argumentVal;
                    } else if ("file".equalsIgnoreCase(argumentName)) {
                        this.file = argumentVal;
                    } else if ("dir".equalsIgnoreCase(argumentName)) {
                        this.directory = argumentVal;
                    } else if ("createfks".equalsIgnoreCase(argumentName)) {
                        this.useDummyFks = "true".equalsIgnoreCase(argumentVal);
                    } else if ("maintainTxs".equalsIgnoreCase(argumentName)) {
                        this.maintainTxs = "true".equalsIgnoreCase(argumentVal);
                    } else if ("inserts".equalsIgnoreCase(argumentName)) {
                        this.tryInserts = "true".equalsIgnoreCase(argumentVal);
                    }
                }
    
                // special case
                if (this.readers == null && (this.file != null || this.directory != null)) {
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

        if (delegatorNameProp == null || delegatorNameProp.value == null || delegatorNameProp.value.length() == 0) {
            throw new ContainerException("Invalid delegator-name defined in container configuration");
        } else {
            delegatorName = delegatorNameProp.value;
        }

        if (entityGroupNameProp == null || entityGroupNameProp.value == null || entityGroupNameProp.value.length() == 0) {
            throw new ContainerException("Invalid entity-group-name defined in container configuration");
        } else {
            entityGroupName = entityGroupNameProp.value;
        }

        // parse the pass in list of readers to use
        List readerNames = null;
        if (this.readers != null && !"none".equalsIgnoreCase(this.readers)) {
            if (this.readers.indexOf(",") == -1) {
                readerNames = new LinkedList();
                readerNames.add(this.readers);
            } else {
                readerNames = StringUtil.split(this.readers, ",");
            }
        }

        String delegatorNameToUse = overrideDelegator != null ? overrideDelegator : delegatorName;
        String groupNameToUse = overrideGroup != null ? overrideGroup : entityGroupName;
        GenericDelegator delegator = GenericDelegator.getGenericDelegator(delegatorNameToUse);
        if (delegator == null) {
            throw new ContainerException("Invalid delegator name!");
        }

        String helperName = delegator.getGroupHelperName(groupNameToUse);
        if (helperName == null) {
            throw new ContainerException("Unable to locate the datasource helper for the group [" + groupNameToUse + "]");
        }

        // get the reader name URLs first
        List urlList = null;
        if (readerNames != null) {
            urlList = EntityDataLoader.getUrlList(helperName, readerNames);
        } else if (!"none".equalsIgnoreCase(this.readers)) {
            urlList = EntityDataLoader.getUrlList(helperName);
        }

        // need a list if it is empty
        if (urlList == null) {
            urlList = new ArrayList();
        }

        // add in the defined extra file
        if (this.file != null) {
            URL fileUrl = UtilURL.fromResource(this.file);
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
                    for (int i = 0; i < fileArray.length; i++) {
                        if (fileArray[i].getName().toLowerCase().endsWith(".xml")) {
                            try {
                                urlList.add(fileArray[i].toURL());
                            } catch (MalformedURLException e) {
                                Debug.logError(e, "Unable to load file (" + fileArray[i].getName() + "); not a valid URL.", module);
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
        
        List errorMessages = new LinkedList();
        List infoMessages = new LinkedList();
        int totalRowsChanged = 0;
        if (urlList != null && urlList.size() > 0) {
            Debug.logImportant("=-=-=-=-=-=-= Doing a data load with the following files:", module);
            Iterator urlIter = urlList.iterator();
            while (urlIter.hasNext()) {
                URL dataUrl = (URL) urlIter.next();
                Debug.logImportant(dataUrl.toExternalForm(), module);
            }

            Debug.logImportant("=-=-=-=-=-=-= Starting the data load...", module);

            urlIter = urlList.iterator();
            while (urlIter.hasNext()) {
                URL dataUrl = (URL) urlIter.next();
                try {
                    int rowsChanged = EntityDataLoader.loadData(dataUrl, helperName, delegator, errorMessages, txTimeout, useDummyFks, maintainTxs, tryInserts);
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
            Iterator infoIter = infoMessages.iterator();
            while (infoIter.hasNext()){
              Debug.logImportant((String) infoIter.next(), module);
            }
        }
        
        if (errorMessages.size() > 0) {
            Debug.logImportant("The following errors occured in the data load:", module);
            Iterator errIter = errorMessages.iterator();
            while (errIter.hasNext()){
              Debug.logImportant((String) errIter.next(), module);
            }
        }

        Debug.logImportant("=-=-=-=-=-=-= Finished the data load with " + totalRowsChanged + " rows changed.", module);
        
        return true;
    }

    /**
     * @see org.ofbiz.base.container.Container#stop()
     */
    public void stop() throws ContainerException {
    }
}
