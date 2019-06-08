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
package org.apache.ofbiz.entityext.data;

import java.io.File;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.container.Container;
import org.apache.ofbiz.base.container.ContainerConfig;
import org.apache.ofbiz.base.container.ContainerConfig.Configuration;
import org.apache.ofbiz.base.container.ContainerConfig.Configuration.Property;
import org.apache.ofbiz.base.container.ContainerException;
import org.apache.ofbiz.base.start.StartupCommand;
import org.apache.ofbiz.base.start.StartupCommandUtil;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilURL;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.datasource.GenericHelperInfo;
import org.apache.ofbiz.entity.jdbc.DatabaseUtil;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.util.EntityDataLoader;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.ServiceDispatcher;

/**
 * Container responsible for loading all types of data e.g. seed, seed-initial, etc.
 * This container is the one used when the user issues --load-data commands.
 */
public class EntityDataLoadContainer implements Container {

    public static final String module = EntityDataLoadContainer.class.getName();
    private String name;

    // possible command line properties passed by user
    private static final String DATA_READERS = "readers";
    private static final String DATA_FILE = "file";
    private static final String DATA_DIR = "dir";
    private static final String DATA_COMPONENT = "component";
    private static final String DELEGATOR_NAME = "delegator";
    private static final String DATA_GROUP = "group";
    private static final String TIMEOUT = "timeout";
    private static final String CREATE_P_KEYS = "create-pks";
    private static final String DROP_P_KEYS = "drop-pks";
    private static final String CREATE_CONSTRAINTS = "create-constraints";
    private static final String DROP_CONSTRAINTS = "drop-constraints";
    private static final String CREATE_F_KEYS = "create-fks";
    private static final String MAINTAIN_TXS = "maintain-txs";
    private static final String TRY_INSERTS = "try-inserts";
    private static final String REPAIR_COLUMNS = "repair-columns";
    private static final String CONTINUE_ON_FAIL = "continue-on-failure";

    @Override
    public void init(List<StartupCommand> ofbizCommands, String name, String configFile) throws ContainerException {
        this.name = name;

        // get the data-load properties passed by the user in the command line
        Map<String, String> loadDataProps = ofbizCommands.stream()
                .filter(command -> command.getName().equals(StartupCommandUtil.StartupOption.LOAD_DATA.getName()))
                .map(command -> command.getProperties())
                .findFirst().get();

        /* disable job scheduler, JMS listener and startup services
         * FIXME: This is not thread-safe. */
        ServiceDispatcher.enableJM(false);
        ServiceDispatcher.enableJMS(false);
        ServiceDispatcher.enableSvcs(false);

        Configuration configuration = ContainerConfig.getConfiguration(name, configFile);
        Property delegatorNameProp = configuration.getProperty("delegator-name");
        String overrideDelegator = loadDataProps.get(DELEGATOR_NAME);

        if ("all-tenants".equals(overrideDelegator)) {
            // load data for all tenants
            for (GenericValue tenant : getTenantList(delegatorNameProp)) {
                String tenantDelegator = delegatorNameProp.value + "#" + tenant.getString("tenantId");
                loadDataForDelegator(loadDataProps, configuration, delegatorNameProp, tenantDelegator);
            }
        } else {
            // load data for a single delegator
            loadDataForDelegator(loadDataProps, configuration, delegatorNameProp,  overrideDelegator);
        }
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public void stop() {
    }

    @Override
    public String getName() {
        return name;
    }

    private static List<GenericValue> getTenantList(Property delegatorNameProp) throws ContainerException {
        if (!EntityUtil.isMultiTenantEnabled()) {
            throw new ContainerException("Multitenant is disabled, must be enabled in general.properties -> multitenant=Y");
        }

        Delegator delegator = getDelegator(delegatorNameProp, null);
        List<EntityExpr> expr = new ArrayList<>();
        expr.add(EntityCondition.makeCondition("disabled", EntityOperator.EQUALS, "N"));
        expr.add(EntityCondition.makeCondition("disabled", EntityOperator.EQUALS, null));

        try {
            return EntityQuery.use(delegator).from("Tenant").where(expr, EntityOperator.OR).queryList();
        } catch (GenericEntityException e) {
            throw new ContainerException(e);
        }
    }

    private static void loadDataForDelegator(Map<String, String> loadDataProps, Configuration configuration,
            Property delegatorNameProp, String overrideDelegator) throws ContainerException{

        // prepare command line properties passed by user
        boolean createPks = isPropertySet(loadDataProps, CREATE_P_KEYS);
        boolean dropPks = isPropertySet(loadDataProps, DROP_P_KEYS);
        boolean createConstraints = isPropertySet(loadDataProps, CREATE_CONSTRAINTS);
        boolean dropConstraints = isPropertySet(loadDataProps, DROP_CONSTRAINTS);
        boolean repairColumns = isPropertySet(loadDataProps, REPAIR_COLUMNS);
        String entityGroup = getEntityGroupNameFromConfig(configuration, loadDataProps.get(DATA_GROUP));

        // prepare objects needed for the data loading logic
        Delegator delegator = getDelegator(delegatorNameProp, overrideDelegator);
        Delegator baseDelegator = getBaseDelegator(delegator);
        GenericHelperInfo helperInfo = getHelperInfo(delegator, entityGroup);
        DatabaseUtil dbUtil = new DatabaseUtil(helperInfo);
        Map<String, ModelEntity> modelEntities = getModelEntities(delegator, entityGroup);
        TreeSet<String> modelEntityNames = new TreeSet<>(modelEntities.keySet());
        Collection<ComponentConfig> allComponents = ComponentConfig.getAllComponents();

        // data loading logic starts here
        createOrUpdateComponentEntities(baseDelegator, allComponents);

        if (dropConstraints) {
            dropDbConstraints(dbUtil, modelEntities, modelEntityNames);
        }
        if (dropPks) {
            dropPrimaryKeys(dbUtil, modelEntities, modelEntityNames);
        }
        if (repairColumns) {
            repairDbColumns(dbUtil, modelEntities);
        }

        loadData(delegator, baseDelegator, allComponents, helperInfo, loadDataProps);

        if (createPks) {
            createPrimaryKeys(dbUtil, modelEntities, modelEntityNames);
        }
        if (createConstraints) {
            createDbConstraints(dbUtil, modelEntities, modelEntityNames);
        }
    }

    /**
     * Checks if a key is associated with either the string {@code "true"} or {@code null}.
     *
     * @param props  the map associating keys to values
     * @param key  the key to look for in {@code props}
     * @return {@code true} if {@code key} is associated with {@code "true"} or {@code null} in {@code props}.
     */
    private static boolean isPropertySet(Map<String, String> props, String key) {
        String value = props.get(key);
        return props.containsKey(key) && (value == null || "true".equalsIgnoreCase(value));
    }

    /*
     * Gets the default entity-group-name defined in the container definition
     * unless overridden by the user
     */
    private static String getEntityGroupNameFromConfig(Configuration cfg, String overrideGroup)
            throws ContainerException {
        if (overrideGroup != null) {
            return overrideGroup;
        } else {
            ContainerConfig.Configuration.Property entityGroupNameProp = cfg.getProperty("entity-group-name");
            if (entityGroupNameProp == null || UtilValidate.isEmpty(entityGroupNameProp.value)) {
                throw new ContainerException("Invalid entity-group-name defined in container configuration");
            } else {
                return entityGroupNameProp.value;
            }
        }
    }

    /*
     * Gets the default delegator defined in the container definition unless
     * overridden by the user. This method will create all the tables, keys and
     * indices if missing and hence might take a long time.
     */
    private static Delegator getDelegator(Property delegatorNameProp, String overrideDelegator)
            throws ContainerException {
        if (overrideDelegator != null) {
            return DelegatorFactory.getDelegator(overrideDelegator);
        } else {
            return getDelegatorFromProp(delegatorNameProp);
        }
    }

    private static Delegator getDelegatorFromProp(Property delegatorNameProp) throws ContainerException {
        if (delegatorNameProp != null && UtilValidate.isNotEmpty(delegatorNameProp.value)) {
            Delegator delegator = DelegatorFactory.getDelegator(delegatorNameProp.value);
            if (delegator != null) {
                return delegator;
            } else {
                throw new ContainerException("Invalid delegator name: " + delegatorNameProp.value);
            }
        } else {
            throw new ContainerException("Invalid delegator name defined in container configuration");
        }
    }

    private static Delegator getBaseDelegator(Delegator delegator) {
        if (delegator.getDelegatorTenantId() != null) {
            return DelegatorFactory.getDelegator(delegator.getDelegatorBaseName());
        } else {
            return delegator;
        }
    }

    private static GenericHelperInfo getHelperInfo(Delegator delegator, String entityGroup) throws ContainerException {
        GenericHelperInfo helperInfo = delegator.getGroupHelperInfo(entityGroup);
        if (helperInfo == null) {
            throw new ContainerException("Unable to locate the datasource helper for the group: " + entityGroup);
        }
        return helperInfo;
    }

    private static Map<String, ModelEntity> getModelEntities(Delegator delegator, String entityGroup)
            throws ContainerException {
        try {
            return delegator.getModelEntityMapByGroup(entityGroup);
        } catch (GenericEntityException e) {
            throw new ContainerException(e);
        }
    }

    private static void createOrUpdateComponentEntities(Delegator baseDelegator,
            Collection<ComponentConfig> allComponents) {

        for (ComponentConfig config : allComponents) {
            GenericValue componentEntry = baseDelegator.makeValue("Component");
            componentEntry.set("componentName", config.getComponentName());
            componentEntry.set("rootLocation", config.getRootLocation());
            try {
                GenericValue componentCheck = EntityQuery.use(baseDelegator)
                        .from("Component")
                        .where("componentName", config.getComponentName())
                        .queryOne();
                if (UtilValidate.isEmpty(componentCheck)) {
                    componentEntry.create();
                } else {
                    componentEntry.store();
                }
            } catch (GenericEntityException e) {
                Debug.logError(e.getMessage(), module);
            }
        }
    }

    private static void dropDbConstraints(DatabaseUtil dbUtil, Map<String, ModelEntity> modelEntities,
            TreeSet<String> modelEntityNames) {

        List<String> messages = new ArrayList<>();

        Debug.logImportant("Dropping foreign key indices...", module);
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

        logMessageList(messages);
    }

    private static void createDbConstraints(DatabaseUtil dbUtil, Map<String, ModelEntity> modelEntities,
            TreeSet<String> modelEntityNames) {

        List<String> messages = new ArrayList<>();

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

        logMessageList(messages);
    }

    private static void dropPrimaryKeys(DatabaseUtil dbUtil, Map<String, ModelEntity> modelEntities,
            TreeSet<String> modelEntityNames) {

        List<String> messages = new ArrayList<>();

        Debug.logImportant("Dropping primary keys...", module);
        for (String entityName : modelEntityNames) {
            ModelEntity modelEntity = modelEntities.get(entityName);
            if (modelEntity != null) {
                dbUtil.deletePrimaryKey(modelEntity, messages);
            }
        }

        logMessageList(messages);
    }

    private static void createPrimaryKeys(DatabaseUtil dbUtil, Map<String, ModelEntity> modelEntities,
            TreeSet<String> modelEntityNames) {

        List<String> messages = new ArrayList<>();

        Debug.logImportant("Creating primary keys...", module);
        for (String entityName : modelEntityNames) {
            ModelEntity modelEntity = modelEntities.get(entityName);
            if (modelEntity != null) {
                dbUtil.createPrimaryKey(modelEntity, messages);
            }
        }

        logMessageList(messages);
    }

    private static void repairDbColumns(DatabaseUtil dbUtil, Map<String, ModelEntity> modelEntities) {
        List<String> fieldsToRepair = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        dbUtil.checkDb(modelEntities, fieldsToRepair, messages, false, false, false, false);
        if (UtilValidate.isNotEmpty(fieldsToRepair)) {
            dbUtil.repairColumnSizeChanges(modelEntities, fieldsToRepair, messages);
        }

        logMessageList(messages);
    }

    private static void logMessageList(List<String> messages) {
        if (Debug.infoOn()) {
            messages.forEach(message -> Debug.logInfo(message, module));
        }
    }

    private static void loadData(Delegator delegator, Delegator baseDelegator,
            Collection<ComponentConfig> allComponents, GenericHelperInfo helperInfo,
            Map<String, String> loadDataProps) throws ContainerException {

        // prepare command line properties passed by user
        int txTimeout = getTransactionTimeout(loadDataProps.get(TIMEOUT));
        boolean useDummyFks = isPropertySet(loadDataProps, CREATE_F_KEYS);
        boolean maintainTxs = isPropertySet(loadDataProps, MAINTAIN_TXS);
        boolean tryInserts = isPropertySet(loadDataProps, TRY_INSERTS);
        boolean continueOnFail = isPropertySet(loadDataProps, CONTINUE_ON_FAIL);

        List<URL> urlList = prepareDataUrls(delegator, baseDelegator, allComponents, helperInfo, loadDataProps);
        List<String> infoMessages = new ArrayList<>();
        List<Object> errorMessages = new ArrayList<>();
        int totalRowsChanged = 0;

        logDataLoadingPlan(urlList, delegator.getDelegatorName());

        for (URL dataUrl: urlList) {
            try {
                int rowsChanged = EntityDataLoader.loadData(dataUrl, helperInfo.getHelperBaseName(),
                        delegator, errorMessages, txTimeout, useDummyFks, maintainTxs, tryInserts, continueOnFail);
                totalRowsChanged += rowsChanged;
                infoMessages.add(createDataLoadMessage(dataUrl, rowsChanged, totalRowsChanged));
            } catch (GenericEntityException e) {
                if (continueOnFail) {
                    Debug.logError(e, "Error loading data file: " + dataUrl.toExternalForm(), module);
                } else {
                    throw new ContainerException(e);
                }
            }
        }

        logDataLoadingResults(infoMessages, errorMessages, totalRowsChanged);
    }

    private static int getTransactionTimeout(String timeout) {
        try {
            return Integer.parseInt(timeout);
        } catch (Exception e) {
            return -1;
        }
    }

    private static List<URL> prepareDataUrls(Delegator delegator, Delegator baseDelegator,
            Collection<ComponentConfig> allComponents, GenericHelperInfo helperInfo,
            Map<String, String> loadDataProps) throws ContainerException {

        List<URL> urlList = new ArrayList<>();

        // prepare command line properties passed by user
        List<String> files = getLoadFiles(loadDataProps.get(DATA_FILE));
        String directory = loadDataProps.get(DATA_DIR);
        String component = loadDataProps.get(DATA_COMPONENT);
        String readers = loadDataProps.get(DATA_READERS);

        boolean readersEnabled = isDataReadersEnabled(files, directory, readers);
        String helperBaseName = helperInfo.getHelperBaseName();
        List<String> loadComponents = prepareTenantLoadComponents(delegator, baseDelegator, allComponents, component);
        List<String> readerNames = StringUtil.split(readers, ",");

        // retrieve URLs from readers
        if (UtilValidate.isNotEmpty(loadComponents)) {
            if (UtilValidate.isNotEmpty(readerNames)) {
                urlList.addAll(EntityDataLoader.getUrlByComponentList(helperBaseName, loadComponents, readerNames));
            } else if (readersEnabled) {
                urlList.addAll(EntityDataLoader.getUrlByComponentList(helperBaseName, loadComponents));
            }
        } else {
            if (UtilValidate.isNotEmpty(readerNames)) {
                urlList.addAll(EntityDataLoader.getUrlList(helperBaseName, component, readerNames));
            } else if (readersEnabled) {
                urlList.addAll(EntityDataLoader.getUrlList(helperBaseName, component));
            }
        }

        // retrieve URLs from files
        urlList.addAll(retireveDataUrlsFromFileList(files));

        // retrieve URLs from all data files in "directory"
        urlList.addAll(retrieveDataUrlsFromDirectory(directory));

        return urlList;
    }

    static private List<String> getLoadFiles(String fileProp) {
        List<String> fileList = new ArrayList<>();
        Optional.ofNullable(fileProp)
                .ifPresent(props -> fileList.addAll(StringUtil.split(props, ",")));
        return fileList;
    }

    private static boolean isDataReadersEnabled(List<String> files, String directory, String readers) {
        /* if files or directories are passed by the user and no readers are
         * passed then set readers to "none" */
        return readers != null || (files.isEmpty() && directory == null);
    }

    private static List<String> prepareTenantLoadComponents(Delegator delegator, Delegator baseDelegator,
            Collection<ComponentConfig> allComponents, String component) {

        List<String> loadComponents = new ArrayList<>();
        List<EntityCondition> queryConditions = new ArrayList<>();

        if (UtilValidate.isNotEmpty(delegator.getDelegatorTenantId()) && EntityUtil.isMultiTenantEnabled()) {

            queryConditions.add(EntityCondition.makeCondition("tenantId", delegator.getDelegatorTenantId()));
            if (UtilValidate.isEmpty(component)) {
                allComponents.forEach(config -> loadComponents.add(config.getComponentName()));
            } else {
                queryConditions.add(EntityCondition.makeCondition("componentName", component));
            }

            try {
                List<GenericValue> tenantComponents = EntityQuery.use(baseDelegator)
                        .from("TenantComponent")
                        .where(queryConditions)
                        .orderBy("sequenceNum")
                        .queryList();
                tenantComponents.forEach(comp -> loadComponents.add(comp.getString("componentName")));
                Debug.logInfo("Loaded : " + loadComponents.size() + " components", module);
            } catch (GenericEntityException e) {
                Debug.logError(e.getMessage(), module);
            }
        }
        return loadComponents;
    }

    private static List<URL> retireveDataUrlsFromFileList(List<String> files) throws ContainerException {
        List<URL> fileUrls = new ArrayList<>();
        for(String file: files) {
            URL url = UtilURL.fromResource(file);
            if (url == null) {
                throw new ContainerException("Unable to locate data file: " + file);
            } else {
                fileUrls.add(url);
            }
        }
        return fileUrls;
    }

    private static List<URL> retrieveDataUrlsFromDirectory(String directory) {
        return Optional.ofNullable(directory)
                .map(dir -> Arrays.asList(new File(dir).listFiles()).stream()
                        .filter(file -> file.getName().toLowerCase(Locale.getDefault()).endsWith(".xml"))
                        .map(file -> UtilURL.fromFilename(file.getPath()))
                        .collect(Collectors.toList()))
                .orElse(new ArrayList<URL>());
    }

    private static void logDataLoadingPlan(List<URL> urlList, String delegatorName) {
        if (UtilValidate.isNotEmpty(urlList)) {
            Debug.logImportant("=-=-=-=-=-=-= Doing a data load using delegator '"
                    + delegatorName + "' with the following files:", module);
            urlList.forEach(dataUrl -> Debug.logImportant(dataUrl.toExternalForm(), module));
            Debug.logImportant("=-=-=-=-=-=-= Starting the data load...", module);
        } else {
            Debug.logImportant("=-=-=-=-=-=-= No data load files found.", module);
        }
    }

    private static String createDataLoadMessage(URL dataUrl, int rowsChanged, int totalRowsChanged) {
        NumberFormat formatter = NumberFormat.getIntegerInstance();
        formatter.setMinimumIntegerDigits(5);
        formatter.setGroupingUsed(false);
        return formatter.format(rowsChanged)
                + " of " + formatter.format(totalRowsChanged)
                + " from " + dataUrl.toExternalForm();
    }

    private static void logDataLoadingResults(List<String> infoMessages,
            List<Object> errorMessages, int totalRowsChanged) {

        if (UtilValidate.isNotEmpty(infoMessages)) {
            Debug.logImportant("=-=-=-=-=-=-= Here is a summary of the data load:", module);
            infoMessages.forEach(message -> Debug.logImportant(message, module));
        }
        if (UtilValidate.isNotEmpty(errorMessages)) {
            Debug.logImportant("The following errors occurred in the data load:", module);
            errorMessages.forEach(message -> Debug.logImportant(message.toString(), module));
        }
        Debug.logImportant("=-=-=-=-=-=-= Finished the data load with "
                + totalRowsChanged + " rows changed.", module);
    }
}
