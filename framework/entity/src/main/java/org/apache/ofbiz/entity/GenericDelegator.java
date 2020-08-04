/*
/*
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
 */
package org.apache.ofbiz.entity;

import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.concurrent.ConstantFuture;
import org.apache.ofbiz.base.concurrent.ExecutionPool;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralRuntimeException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.cache.Cache;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.config.model.Datasource;
import org.apache.ofbiz.entity.config.model.DelegatorElement;
import org.apache.ofbiz.entity.config.model.EntityConfig;
import org.apache.ofbiz.entity.datasource.GenericHelper;
import org.apache.ofbiz.entity.datasource.GenericHelperFactory;
import org.apache.ofbiz.entity.datasource.GenericHelperInfo;
import org.apache.ofbiz.entity.eca.EntityEcaHandler;
import org.apache.ofbiz.entity.model.DynamicViewEntity;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelEntityChecker;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.entity.model.ModelFieldType;
import org.apache.ofbiz.entity.model.ModelFieldTypeReader;
import org.apache.ofbiz.entity.model.ModelGroupReader;
import org.apache.ofbiz.entity.model.ModelKeyMap;
import org.apache.ofbiz.entity.model.ModelReader;
import org.apache.ofbiz.entity.model.ModelRelation;
import org.apache.ofbiz.entity.model.ModelViewEntity;
import org.apache.ofbiz.entity.serialize.SerializeException;
import org.apache.ofbiz.entity.serialize.XmlSerializer;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.DistributedCacheClear;
import org.apache.ofbiz.entity.util.EntityCrypto;
import org.apache.ofbiz.entity.util.EntityFindOptions;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityStoreOptions;
import org.apache.ofbiz.entity.util.SequenceUtil;
import org.apache.ofbiz.entityext.eca.EntityEcaUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * The default implementation of the <code>Delegator</code> interface.
 *
 */
public class GenericDelegator implements Delegator {

    private static final String MODULE = GenericDelegator.class.getName();

    protected ModelReader modelReader = null;
    protected ModelGroupReader modelGroupReader = null;
    /** This flag is only here for lower level technical testing, it shouldn't be user configurable (or at least I don't think so yet); when true all operations without a transaction will be wrapped in one; seems to be necessary for some (all?) XA aware connection pools, and should improve overall stability and consistency */
    public static final boolean alwaysUseTransaction = true;
    // TODO should this is be handled by tenant?
    public static final boolean saveEntitySyncRemoveInfo = UtilProperties.getPropertyAsBoolean("general", "saveEntitySyncRemove", false);

    protected String delegatorBaseName = null;
    protected String delegatorFullName = null;
    protected String delegatorTenantId = null;
    private String originalDelegatorName = null;

    protected DelegatorElement delegatorInfo = null;

    protected Cache cache = null;

    protected final AtomicReference<Future<DistributedCacheClear>> distributedCacheClear = new AtomicReference<>();
    protected boolean warnNoEcaHandler = false;
    protected final AtomicReference<Future<EntityEcaHandler<?>>> entityEcaHandler = new AtomicReference<>();
    protected final AtomicReference<SequenceUtil> AtomicRefSequencer = new AtomicReference<>(null);
    protected EntityCrypto crypto = null;

    /** A ThreadLocal variable to allow other methods to specify a user identifier (usually the userLoginId, though technically the Entity Engine doesn't know anything about the UserLogin entity) */
    private static final ThreadLocal<List<String>> userIdentifierStack = new ThreadLocal<>();
    /** A ThreadLocal variable to allow other methods to specify a session identifier (usually the visitId, though technically the Entity Engine doesn't know anything about the Visit entity) */
    private static final ThreadLocal<List<String>> sessionIdentifierStack = new ThreadLocal<>();

    private boolean testMode = false;
    private boolean testRollbackInProgress = false;
    private static final AtomicReferenceFieldUpdater<GenericDelegator, LinkedBlockingDeque<?>> testOperationsUpdater = UtilGenerics.cast(AtomicReferenceFieldUpdater.newUpdater(GenericDelegator.class, LinkedBlockingDeque.class, "testOperations"));
    private volatile LinkedBlockingDeque<TestOperation> testOperations = null;

    protected static List<String> getUserIdentifierStack() {
        List<String> curValList = userIdentifierStack.get();
        if (curValList == null) {
            curValList = new LinkedList<>();
            userIdentifierStack.set(curValList);
        }
        return curValList;
    }

    public static void pushUserIdentifier(String userIdentifier) {
        if (userIdentifier == null) {
            return;
        }
        List<String> curValList = getUserIdentifierStack();
        curValList.add(0, userIdentifier);
    }

    public static String popUserIdentifier() {
        List<String> curValList = getUserIdentifierStack();
        if (curValList.size() == 0) {
            return null;
        }
        return curValList.remove(0);
    }

    public static void clearUserIdentifierStack() {
        List<String> curValList = getUserIdentifierStack();
        curValList.clear();
    }

    protected static List<String> getSessionIdentifierStack() {
        List<String> curValList = sessionIdentifierStack.get();
        if (curValList == null) {
            curValList = new LinkedList<>();
            sessionIdentifierStack.set(curValList);
        }
        return curValList;
    }

    public static void pushSessionIdentifier(String sessionIdentifier) {
        if (sessionIdentifier == null) {
            return;
        }
        List<String> curValList = getSessionIdentifierStack();
        curValList.add(0, sessionIdentifier);
    }

    public static String popSessionIdentifier() {
        List<String> curValList = getSessionIdentifierStack();
        if (curValList.size() == 0) {
            return null;
        }
        return curValList.remove(0);
    }

    public static void clearSessionIdentifierStack() {
        List<String> curValList = getSessionIdentifierStack();
        curValList.clear();
    }

    /** Only allow creation through the factory method */
    protected GenericDelegator() { }

    /** Only allow creation through the factory method */
    protected GenericDelegator(String delegatorFullName) throws GenericEntityException {
        this.setDelegatorNames(delegatorFullName);
        this.delegatorInfo = EntityConfig.getInstance().getDelegator(delegatorBaseName);

        if (this.delegatorInfo == null) {
            throw new GenericEntityException("No configuration found for delegator [" + delegatorFullName + "]");
        }

        String kekText;
        // before continuing, if there is a tenantId use the base delegator to see if it is valid
        if (UtilValidate.isNotEmpty(this.delegatorTenantId)) {
            Delegator baseDelegator = DelegatorFactory.getDelegator(this.delegatorBaseName);
            GenericValue tenant = EntityQuery.use(baseDelegator).from("Tenant").where("tenantId", this.delegatorTenantId).cache(true).queryOne();
            if (tenant == null) {
                throw new GenericEntityException("No Tenant record found for delegator [" + this.delegatorFullName + "] with tenantId [" + this.delegatorTenantId + "]");
            } else if ("Y".equals(tenant.getString("disabled"))) {
                throw new GenericEntityException("No Tenant record found for delegator [" + this.delegatorFullName + "] with tenantId [" + this.delegatorTenantId + "]");
            }
            GenericValue kekValue = EntityQuery.use(baseDelegator).from("TenantKeyEncryptingKey").where("tenantId", getDelegatorTenantId()).cache(true).queryOne();
            if (kekValue != null) {
                kekText = kekValue.getString("kekText");
            } else {
                kekText = this.delegatorInfo.getKeyEncryptingKey();
            }
        } else {
            kekText = this.delegatorInfo.getKeyEncryptingKey();
        }

        this.modelReader = ModelReader.getModelReader(delegatorBaseName);
        this.modelGroupReader = ModelGroupReader.getModelGroupReader(delegatorBaseName);

        cache = new Cache(delegatorFullName);

        // do the entity model check
        List<String> warningList = new LinkedList<>();
        Debug.logInfo("Doing entity definition check...", MODULE);
        ModelEntityChecker.checkEntities(this, warningList);
        if (warningList.size() > 0) {
            Debug.logWarning("=-=-=-=-= Found " + warningList.size() + " warnings when checking the entity definitions:", MODULE);
            for (String warning: warningList) {
                Debug.logWarning(warning, MODULE);
            }
        }

        // initialize helpers by group
        Set<String> groupNames = getModelGroupReader().getGroupNames(delegatorBaseName);
        List<Future<Void>> futures = new LinkedList<>();
        for (String groupName: groupNames) {
            futures.add(ExecutionPool.GLOBAL_BATCH.submit(createHelperCallable(groupName)));
        }
        ExecutionPool.getAllFutures(futures);

        // NOTE: doing some things before the ECAs and such to make sure it is in place just in case it is used in a service engine startup thing or something

        // setup the crypto class; this also after the delegator is in the cache otherwise we get infinite recursion
        this.crypto = new EntityCrypto(this, kekText);
    }

    private void initializeOneGenericHelper(String groupName) {
        GenericHelperInfo helperInfo = this.getGroupHelperInfo(groupName);
        if (helperInfo == null) {
            if (Debug.infoOn()) {
                Debug.logInfo("Delegator \"" + delegatorFullName + "\" NOT initializing helper for entity group \"" + groupName + "\" because the group is not associated to this delegator.", MODULE);
            }
            return;
        }
        String helperBaseName = helperInfo.getHelperBaseName();

        if (Debug.infoOn()) {
            Debug.logInfo("Delegator \"" + delegatorFullName + "\" initializing helper \"" + helperBaseName + "\" for entity group \"" + groupName + "\".", MODULE);
        }
        if (UtilValidate.isNotEmpty(helperInfo.getHelperFullName())) {
            // pre-load field type defs, the return value is ignored
            ModelFieldTypeReader.getModelFieldTypeReader(helperBaseName);
            // get the helper and if configured, do the datasource check
            GenericHelper helper = GenericHelperFactory.getHelper(helperInfo);

            try {
                Datasource datasource = EntityConfig.getDatasource(helperBaseName);
                if (datasource.getCheckOnStart()) {
                    if (Debug.infoOn()) {
                        Debug.logInfo("Doing database check as requested in entityengine.xml with addMissing=" + datasource.getAddMissingOnStart(), MODULE);
                    }
                    helper.checkDataSource(this.getModelEntityMapByGroup(groupName), null, datasource.getAddMissingOnStart());
                }
            } catch (GenericEntityException e) {
                Debug.logWarning(e, e.getMessage(), MODULE);
            }
        }
    }

    protected Callable<Void> createHelperCallable(final String groupName) {
        return () -> {
            initializeOneGenericHelper(groupName);
            return null;
        };
    }

    protected void setDelegatorNames(String delegatorFullName) {
        this.delegatorFullName = delegatorFullName;

        int hashSymbolIndex = delegatorFullName.indexOf('#');
        if (hashSymbolIndex == -1) {
            this.delegatorBaseName = delegatorFullName;
        } else {
            this.delegatorBaseName = delegatorFullName.substring(0, hashSymbolIndex);
            this.delegatorTenantId = delegatorFullName.substring(hashSymbolIndex + 1);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#initEntityEcaHandler()
     */
    @Override
    public synchronized void initEntityEcaHandler() {
        // Nothing to do if already assigned: the class loader has already been called, the class instantiated and casted to EntityEcaHandler
        if (this.entityEcaHandler.get() != null || this.warnNoEcaHandler) {
            return;
        }

        Callable<EntityEcaHandler<?>> creator = this::createEntityEcaHandler;
        FutureTask<EntityEcaHandler<?>> futureTask = new FutureTask<>(creator);
        if (this.entityEcaHandler.compareAndSet(null, futureTask)) {
            // This needs to use BATCH, as the service engine might add it's own items into a thread pool.
            ExecutionPool.GLOBAL_BATCH.submit(futureTask);
        }
    }

    protected EntityEcaHandler<?> createEntityEcaHandler() {
        // If useEntityEca is false do nothing: the entityEcaHandler member field with a null value would cause its code to do nothing
        if (this.delegatorInfo.getEntityEcaEnabled()) {
            //time to do some tricks with manual class loading that resolves circular dependencies, like calling services
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            // initialize the entity eca handler
            String entityEcaHandlerClassName = this.delegatorInfo.getEntityEcaHandlerClassName();

            try {
                Class<?> eecahClass = loader.loadClass(entityEcaHandlerClassName);
                EntityEcaHandler<?> entityEcaHandler = UtilGenerics.cast(eecahClass.getDeclaredConstructor().newInstance());
                entityEcaHandler.setDelegator(this);
                return entityEcaHandler;
            } catch (ReflectiveOperationException e) {
                Debug.logWarning(e, "EntityEcaHandler class with name " + entityEcaHandlerClassName + " was not found, Entity ECA Rules will be disabled", MODULE);
            } catch (ClassCastException e) {
                Debug.logWarning(e, "EntityEcaHandler class with name " + entityEcaHandlerClassName + " does not implement the EntityEcaHandler interface, Entity ECA Rules will be disabled", MODULE);
            }
        } else if (!this.warnNoEcaHandler) {
            Debug.logInfo("Entity ECA Handler disabled for delegator [" + delegatorFullName + "]", MODULE);
            this.warnNoEcaHandler = true;
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#getDelegatorName()
     */
    @Override
    public String getDelegatorName() {
        return this.delegatorFullName;
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#getDelegatorBaseName()
     */
    @Override
    public String getDelegatorBaseName() {
        return this.delegatorBaseName;
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#getDelegatorBaseName()
     */
    @Override
    public String getDelegatorTenantId() {
        return this.delegatorTenantId;
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#getOriginalDelegatorName()
     */
    @Override
    public String getOriginalDelegatorName() {
        return this.originalDelegatorName == null ? this.delegatorFullName : this.originalDelegatorName;
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#getModelReader()
     */
    @Override
    public ModelReader getModelReader() {
        return this.modelReader;
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#getModelGroupReader()
     */
    @Override
    public ModelGroupReader getModelGroupReader() {
        return this.modelGroupReader;
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#getModelEntity(java.lang.String)
     */
    @Override
    public ModelEntity getModelEntity(String entityName) {
        try {
            return getModelReader().getModelEntity(entityName);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting entity definition from model", MODULE);
            return null;
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#getEntityGroupName(java.lang.String)
     */
    @Override
    public String getEntityGroupName(String entityName) {
        return getModelGroupReader().getEntityGroupName(entityName, this.delegatorBaseName);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#getModelEntityMapByGroup(java.lang.String)
     */
    @Override
    public Map<String, ModelEntity> getModelEntityMapByGroup(String groupName) throws GenericEntityException {
        Set<String> entityNameSet = getModelGroupReader().getEntityNamesByGroup(delegatorBaseName, groupName);

        if (this.delegatorInfo.getDefaultGroupName().equals(groupName)) {
            // add all entities with no group name to the Set
            Set<String> allEntityNames = this.getModelReader().getEntityNames();
            for (String entityName: allEntityNames) {
                if (this.delegatorInfo.getDefaultGroupName().equals(getModelGroupReader().getEntityGroupName(entityName, this.delegatorBaseName))) {
                    entityNameSet.add(entityName);
                }
            }
        }

        Map<String, ModelEntity> entities = new HashMap<>();
        if (UtilValidate.isEmpty(entityNameSet)) {
            return entities;
        }

        int errorCount = 0;
        for (String entityName: entityNameSet) {
            try {
                ModelEntity entity = getModelReader().getModelEntity(entityName);
                if (entity != null) {
                    entities.put(entity.getEntityName(), entity);
                } else {
                    throw new IllegalStateException("Could not find entity with name " + entityName);
                }
            } catch (GenericEntityException ex) {
                errorCount++;
                Debug.logError("Entity [" + entityName + "] named in Entity Group with name " + groupName + " are not defined in any Entity Definition file", MODULE);
            }
        }

        if (errorCount > 0) {
            Debug.logError(errorCount + " entities were named in ModelGroup but not defined in any EntityModel", MODULE);
        }

        return entities;
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#getGroupHelperName(java.lang.String)
     */
    @Override
    public String getGroupHelperName(String groupName) {
        return this.delegatorInfo.getGroupDataSource(groupName);
    }

    @Override
    public GenericHelperInfo getGroupHelperInfo(String entityGroupName) {
        if (entityGroupName == null) {
            return null;
        }
        String helperBaseName = this.getGroupHelperName(entityGroupName);
        if (helperBaseName == null) {
            return null;
        }
        if (UtilValidate.isNotEmpty(this.delegatorTenantId) && "org.apache.ofbiz.tenant".equals(entityGroupName)) {
            Debug.logInfo("Can't access entity of entityGroup = " + entityGroupName + " using tenant delegator "+ this.getDelegatorName()+", use base delegator instead", MODULE);
            return null;
        }

        GenericHelperInfo helperInfo = new GenericHelperInfo(entityGroupName, helperBaseName);
        if (UtilValidate.isNotEmpty(this.delegatorTenantId)) {
            // get the JDBC parameters from the DB for the entityGroupName and tenantId
            try {
                // NOTE: instead of caching the GenericHelpInfo object do a cached query here and create a new object each time, will avoid issues when the database data changes during run time
                // NOTE: always use the base delegator for this to avoid problems when this is being initialized
                Delegator baseDelegator = DelegatorFactory.getDelegator(this.delegatorBaseName);
                GenericValue tenantDataSource = EntityQuery.use(baseDelegator).from("TenantDataSource").where("tenantId", this.delegatorTenantId, "entityGroupName", entityGroupName).cache(true).queryOne();
                if (tenantDataSource != null) {
                    helperInfo.setTenantId(this.delegatorTenantId);
                    helperInfo.setOverrideJdbcUri(tenantDataSource.getString("jdbcUri"));
                    helperInfo.setOverrideUsername(tenantDataSource.getString("jdbcUsername"));
                    helperInfo.setOverridePassword(tenantDataSource.getString("jdbcPassword"));
                } else {
                    return null;
                }
            } catch (GenericEntityException e) {
                // don't complain about this too much, just log the error if there is one
                Debug.logInfo(e, "Error getting TenantDataSource info for tenantId=" + this.delegatorTenantId + ", entityGroupName=" + entityGroupName, MODULE);
            }
        }
        return helperInfo;
    }

    protected GenericHelperInfo getEntityHelperInfo(String entityName) {
        return this.getGroupHelperInfo(this.getEntityGroupName(entityName));
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#getEntityHelperName(java.lang.String)
     */
    @Override
    public String getEntityHelperName(String entityName) {
        return this.getGroupHelperName(this.getEntityGroupName(entityName));
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#getEntityHelperName(org.apache.ofbiz.entity.model.ModelEntity)
     */
    @Override
    public String getEntityHelperName(ModelEntity entity) {
        if (entity == null) {
            return null;
        }
        return getEntityHelperName(entity.getEntityName());
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#getEntityHelper(java.lang.String)
     */
    @Override
    public GenericHelper getEntityHelper(String entityName) throws GenericEntityException {
        GenericHelperInfo helperInfo = getEntityHelperInfo(entityName);

        if (helperInfo != null) {
            return GenericHelperFactory.getHelper(helperInfo);
        }
        throw new GenericEntityException("There is no datasource (Helper) configured for the entity-group [" + this.getEntityGroupName(entityName) + "]; was trying to find datasource (helper) for entity [" + entityName + "]");
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#getEntityHelper(org.apache.ofbiz.entity.model.ModelEntity)
     */
    @Override
    public GenericHelper getEntityHelper(ModelEntity entity) throws GenericEntityException {
        return getEntityHelper(entity.getEntityName());
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#getEntityFieldType(org.apache.ofbiz.entity.model.ModelEntity, java.lang.String)
     */
    @Override
    public ModelFieldType getEntityFieldType(ModelEntity entity, String type) throws GenericEntityException {
        return this.getModelFieldTypeReader(entity).getModelFieldType(type);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#getModelFieldTypeReader(org.apache.ofbiz.entity.model.ModelEntity)
     */
    @Override
    public ModelFieldTypeReader getModelFieldTypeReader(ModelEntity entity) {
        String helperName = getEntityHelperName(entity);
        if (UtilValidate.isEmpty(helperName)) {
            return null;
        }
        ModelFieldTypeReader modelFieldTypeReader = ModelFieldTypeReader.getModelFieldTypeReader(helperName);
        if (modelFieldTypeReader == null) {
            throw new IllegalArgumentException("ModelFieldTypeReader not found for entity " + entity.getEntityName() + " with helper name " + helperName);
        }
        return modelFieldTypeReader;
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#makeValue(java.lang.String)
     */
    @Override
    public GenericValue makeValue(String entityName) {
        ModelEntity entity = this.getModelEntity(entityName);
        if (entity == null) {
            throw new IllegalArgumentException("[GenericDelegator.makeValue] could not find entity for entityName: " + entityName);
        }
        GenericValue value = GenericValue.create(entity);
        value.setDelegator(this);
        return value;
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#makeValue(java.lang.String, java.lang.Object)
     */
    @Override
    public GenericValue makeValue(String entityName, Object... fields) {
        return makeValue(entityName, UtilMisc.<String, Object>toMap(fields));
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#makeValue(java.lang.String, java.util.Map)
     */
    @Override
    public GenericValue makeValue(String entityName, Map<String, ? extends Object> fields) {
        ModelEntity entity = this.getModelEntity(entityName);
        if (entity == null) {
            throw new IllegalArgumentException("[GenericDelegator.makeValue] could not find entity for entityName: " + entityName);
        }
        return GenericValue.create(this, entity, fields);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#makeValueSingle(java.lang.String, java.lang.Object)
     */
    @Override
    public GenericValue makeValueSingle(String entityName, Object singlePkValue) {
        ModelEntity entity = this.getModelEntity(entityName);
        if (entity == null) {
            throw new IllegalArgumentException("[GenericDelegator.makeValue] could not find entity for entityName: " + entityName);
        }
        return GenericValue.create(this, entity, singlePkValue);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#makeValidValue(java.lang.String, java.lang.Object)
     */
    @Override
    public GenericValue makeValidValue(String entityName, Object... fields) {
        return makeValidValue(entityName, UtilMisc.<String, Object>toMap(fields));
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#makeValidValue(java.lang.String, java.util.Map)
     */
    @Override
    public GenericValue makeValidValue(String entityName, Map<String, ? extends Object> fields) {
        ModelEntity entity = this.getModelEntity(entityName);
        if (entity == null) {
            throw new IllegalArgumentException("[GenericDelegator.makeValidValue] could not find entity for entityName: " + entityName);
        }
        GenericValue value = GenericValue.create(entity);
        value.setDelegator(this);
        value.setAllFields(fields, true, null, null);
        return value;
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#makePK(java.lang.String)
     */
    @Override
    public GenericPK makePK(String entityName) {
        return this.makePK(entityName, (Map<String, Object>) null);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#makePK(java.lang.String, java.lang.Object)
     */
    @Override
    public GenericPK makePK(String entityName, Object... fields) {
        return makePK(entityName, UtilMisc.<String, Object>toMap(fields));
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#makePK(java.lang.String, java.util.Map)
     */
    @Override
    public GenericPK makePK(String entityName, Map<String, ? extends Object> fields) {
        ModelEntity entity = this.getModelEntity(entityName);
        if (entity == null) {
            throw new IllegalArgumentException("[GenericDelegator.makePK] could not find entity for entityName: " + entityName);
        }
        return GenericPK.create(this, entity, fields);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#makePKSingle(java.lang.String, java.lang.Object)
     */
    @Override
    public GenericPK makePKSingle(String entityName, Object singlePkValue) {
        ModelEntity entity = this.getModelEntity(entityName);
        if (entity == null) {
            throw new IllegalArgumentException("[GenericDelegator.makePKSingle] could not find entity for entityName: " + entityName);
        }
        return GenericPK.create(this, entity, singlePkValue);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#create(org.apache.ofbiz.entity.GenericPK)
     */
    @Override
    public GenericValue create(GenericPK primaryKey) throws GenericEntityException {
        if (primaryKey == null) {
            throw new GenericEntityException("Cannot create from a null primaryKey");
        }

        return this.create(GenericValue.create(primaryKey));
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#create(java.lang.String, java.lang.Object)
     */
    @Override
    public GenericValue create(String entityName, Object... fields) throws GenericEntityException {
        return create(entityName, UtilMisc.<String, Object>toMap(fields));
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#create(java.lang.String, java.util.Map)
     */
    @Override
    public GenericValue create(String entityName, Map<String, ? extends Object> fields) throws GenericEntityException {
        if (entityName == null || fields == null) {
            return null;
        }
        ModelEntity entity = this.getModelReader().getModelEntity(entityName);
        GenericValue genericValue = GenericValue.create(this, entity, fields);

        return this.create(genericValue);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#createSingle(java.lang.String, java.lang.Object)
     */
    @Override
    public GenericValue createSingle(String entityName, Object singlePkValue) throws GenericEntityException {
        if (entityName == null || singlePkValue == null) {
            return null;
        }
        ModelEntity entity = this.getModelReader().getModelEntity(entityName);
        GenericValue genericValue = GenericValue.create(this, entity, singlePkValue);

        return this.create(genericValue);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#createSetNextSeqId(org.apache.ofbiz.entity.GenericValue)
     */
    @Override
    public GenericValue createSetNextSeqId(GenericValue value) throws GenericEntityException {
        if (value == null) {
            throw new GenericEntityException("Cannot create a null value");
        }

        GenericHelper helper = getEntityHelper(value.getEntityName());
        // just make sure it is this delegator...
        value.setDelegator(this);
        // this will throw an IllegalArgumentException if the entity for the value does not have one pk field, or if it already has a value set for the one pk field
        value.setNextSeqId();

        boolean beganTransaction = false;
        try {
            if (alwaysUseTransaction) {
                beganTransaction = TransactionUtil.begin();
            }

            EntityEcaRuleRunner<?> ecaRunner = this.getEcaRuleRunner(value.getEntityName());
            ecaRunner.evalRules(EntityEcaHandler.EV_VALIDATE, EntityEcaHandler.OP_CREATE, value, false);

            ecaRunner.evalRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_CREATE, value, false);

            value.setDelegator(this);

            // if audit log on for any fields, save new value with no old value because it's a create
            if (value.getModelEntity().getHasFieldWithAuditLog()) {
                createEntityAuditLogAll(value, false, false);
            }

            try {
                value = helper.create(value);

                if (testMode) {
                    storeForTestRollback(new TestOperation(OperationType.INSERT, value));
                }
            } catch (IllegalStateException | GenericEntityException e) {
                // see if this was caused by an existing record before resetting the sequencer and trying again
                // NOTE: use the helper directly so ECA rules, etc won't be run

                GenericValue existingValue = null;
                try {
                    existingValue = helper.findByPrimaryKey(value.getPrimaryKey());
                } catch (GenericEntityException e1) {
                    // ignore this error, if not found it'll probably be a GenericEntityNotFoundException
                    // it is important to not let this get thrown because it will mask the original exception
                }
                if (existingValue == null) {
                    throw e;
                }
                if (Debug.infoOn()) {
                    Debug.logInfo("Error creating entity record with a sequenced value [" + value.getPrimaryKey() + "], trying again about to refresh bank for entity [" + value.getEntityName() + "]", MODULE);
                }

                // found an existing value... was probably a duplicate key, so clean things up and try again
                this.AtomicRefSequencer.get().forceBankRefresh(value.getEntityName(), 1);

                value.setNextSeqId();
                value = helper.create(value);
                if (Debug.infoOn()) {
                    Debug.logInfo("Successfully created new entity record on retry with a sequenced value [" + value.getPrimaryKey() + "], after getting refreshed bank for entity [" + value.getEntityName() + "]", MODULE);
                }

                if (testMode) {
                    storeForTestRollback(new TestOperation(OperationType.INSERT, value));
                }
            }

            if (value != null) {
                value.setDelegator(this);
                if (value.lockEnabled()) {
                    refresh(value);
                } else {
                    // doCacheClear
                    ecaRunner.evalRules(EntityEcaHandler.EV_CACHE_CLEAR, EntityEcaHandler.OP_CREATE, value, false);
                    this.clearCacheLine(value);
                }
                ecaRunner.evalRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_CREATE, value, false);
            }

            TransactionUtil.commit(beganTransaction);
            return value;
        } catch (GenericEntityException e) {
            String entityName = value != null ? value.getEntityName() : "invalid Generic Value";
            String errMsg = "Failure in createSetNextSeqId operation for entity [" + entityName + "]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, MODULE);
            TransactionUtil.rollback(beganTransaction, errMsg, e);
            throw new GenericEntityException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#create(org.apache.ofbiz.entity.GenericValue)
     */
    @Override
    public GenericValue create(GenericValue value) throws GenericEntityException {
        boolean beganTransaction = false;
        try {
            if (alwaysUseTransaction) {
                beganTransaction = TransactionUtil.begin();
            }

            if (value == null) {
                throw new GenericEntityException("Cannot create a null value");
            }

            EntityEcaRuleRunner<?> ecaRunner = this.getEcaRuleRunner(value.getEntityName());
            ecaRunner.evalRules(EntityEcaHandler.EV_VALIDATE, EntityEcaHandler.OP_CREATE, value, false);

            GenericHelper helper = getEntityHelper(value.getEntityName());

            ecaRunner.evalRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_CREATE, value, false);

            value.setDelegator(this);

            // if audit log on for any fields, save new value with no old value because it's a create
            if (value.getModelEntity().getHasFieldWithAuditLog()) {
                createEntityAuditLogAll(value, false, false);
            }

            value = helper.create(value);

            if (testMode) {
                storeForTestRollback(new TestOperation(OperationType.INSERT, value));
            }
            if (value != null) {
                value.setDelegator(this);
                if (value.lockEnabled()) {
                    refresh(value);
                } else {
                    // doCacheClear
                    ecaRunner.evalRules(EntityEcaHandler.EV_CACHE_CLEAR, EntityEcaHandler.OP_CREATE, value, false);
                    this.clearCacheLine(value);
                }
                ecaRunner.evalRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_CREATE, value, false);
            }

            TransactionUtil.commit(beganTransaction);
            return value;
        } catch (IllegalStateException | GenericEntityException e) {
            String errMsg = "Failure in create operation for entity [" + (value != null ? value.getEntityName() : "value is null") + "]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(errMsg, MODULE);
            TransactionUtil.rollback(beganTransaction, errMsg, e);
            throw new GenericEntityException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#createOrStore(org.apache.ofbiz.entity.GenericValue)
     */
    @Override
    public GenericValue createOrStore(GenericValue value) throws GenericEntityException {
        boolean beganTransaction = false;
        try {
            if (alwaysUseTransaction) {
                beganTransaction = TransactionUtil.begin();
            }

            GenericValue checkValue = this.findOne(value.getEntityName(), value.getPrimaryKey(), false);
            if (checkValue != null) {
                this.store(value);
            } else {
                this.create(value);
            }
            if (value.lockEnabled()) {
                this.refresh(value);
            }
            TransactionUtil.commit(beganTransaction);
            return value;
        } catch (Exception e) {
            String errMsg = "Failure in createOrStore operation for entity [" + (value != null ? value.getEntityName() : "value is null") + "]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, MODULE);
            TransactionUtil.rollback(beganTransaction, errMsg, e);
            throw new GenericEntityException(e);
        }
    }

    protected void saveEntitySyncRemoveInfo(GenericEntity dummyPK) throws GenericEntityException {
        // don't store remove info on entities where it is disabled
        if (dummyPK.getModelEntity().getNoAutoStamp() || this.testRollbackInProgress) {
            return;
        }

        // don't store remove info on things removed on an entity sync
        if (dummyPK.getIsFromEntitySync()) {
            return;
        }

        String serializedPK = null;
        try {
            serializedPK = XmlSerializer.serialize(dummyPK);
        } catch (SerializeException | IOException e) {
            Debug.logError(e, "Could not serialize primary key to save EntitySyncRemove", MODULE);
        }

        if (serializedPK != null) {
            GenericValue entitySyncRemove = this.makeValue("EntitySyncRemove");
            entitySyncRemove.set("primaryKeyRemoved", serializedPK);
            this.createSetNextSeqId(entitySyncRemove);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#removeByPrimaryKey(org.apache.ofbiz.entity.GenericPK)
     */
    @Override
    public int removeByPrimaryKey(GenericPK primaryKey) throws GenericEntityException {
        boolean beganTransaction = false;
        try {
            if (alwaysUseTransaction) {
                beganTransaction = TransactionUtil.begin();
            }

            EntityEcaRuleRunner<?> ecaRunner = this.getEcaRuleRunner(primaryKey.getEntityName());
            ecaRunner.evalRules(EntityEcaHandler.EV_VALIDATE, EntityEcaHandler.OP_REMOVE, primaryKey, false);

            GenericHelper helper = getEntityHelper(primaryKey.getEntityName());

            ecaRunner.evalRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_REMOVE, primaryKey, false);

            // if audit log on for any fields, save old value before removing so it's still there
            if (primaryKey.getModelEntity().getHasFieldWithAuditLog()) {
                createEntityAuditLogAll(this.findOne(primaryKey.getEntityName(), primaryKey, false), true, true);
            }

            GenericValue removedEntity = null;
            if (testMode) {
                removedEntity = this.findOne(primaryKey.getEntityName(), primaryKey, false);
            }
            int num = helper.removeByPrimaryKey(primaryKey);

            // doCacheClear
            ecaRunner.evalRules(EntityEcaHandler.EV_CACHE_CLEAR, EntityEcaHandler.OP_REMOVE, primaryKey, false);
            this.clearCacheLine(primaryKey);

            if (saveEntitySyncRemoveInfo) {
                this.saveEntitySyncRemoveInfo(primaryKey);
            }

            if (testMode) {
                if (removedEntity != null) {
                    storeForTestRollback(new TestOperation(OperationType.DELETE, removedEntity));
                }
            }

            ecaRunner.evalRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_REMOVE, primaryKey, false);
            TransactionUtil.commit(beganTransaction);
            return num;
        } catch (IllegalStateException | GenericEntityException e) {
            String errMsg = "Failure in removeByPrimaryKey operation for entity [" + primaryKey.getEntityName() + "]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, MODULE);
            TransactionUtil.rollback(beganTransaction, errMsg, e);
            throw new GenericEntityException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#removeValue(org.apache.ofbiz.entity.GenericValue)
     */
    @Override
    public int removeValue(GenericValue value) throws GenericEntityException {
        // NOTE: this does not call the GenericDelegator.removeByPrimaryKey method because it has more information to pass to the ECA rule hander
        boolean beganTransaction = false;
        try {
            if (alwaysUseTransaction) {
                beganTransaction = TransactionUtil.begin();
            }

            EntityEcaRuleRunner<?> ecaRunner = this.getEcaRuleRunner(value.getEntityName());
            ecaRunner.evalRules(EntityEcaHandler.EV_VALIDATE, EntityEcaHandler.OP_REMOVE, value, false);

            GenericHelper helper = getEntityHelper(value.getEntityName());

            ecaRunner.evalRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_REMOVE, value, false);

            // if audit log on for any fields, save old value before actual remove
            if (value.getModelEntity().getHasFieldWithAuditLog()) {
                createEntityAuditLogAll(value, true, true);
            }

            GenericValue removedValue = null;
            if (testMode) {
                removedValue = this.findOne(value.getEntityName(), value.getPrimaryKey(), false);
            }

            int num = helper.removeByPrimaryKey(value.getPrimaryKey());
            // Need to call removedFromDatasource() here because the helper calls removedFromDatasource() on the PK instead of the GenericEntity.
            value.removedFromDatasource();

            // doCacheClear
            ecaRunner.evalRules(EntityEcaHandler.EV_CACHE_CLEAR, EntityEcaHandler.OP_REMOVE, value, false);
            this.clearCacheLine(value);


            if (testMode) {
                if (removedValue != null) {
                    storeForTestRollback(new TestOperation(OperationType.DELETE, removedValue));
                }
            }

            if (saveEntitySyncRemoveInfo) {
                this.saveEntitySyncRemoveInfo(value.getPrimaryKey());
            }

            ecaRunner.evalRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_REMOVE, value, false);
            TransactionUtil.commit(beganTransaction);
            return num;
        } catch (IllegalStateException | GenericEntityException e) {
            String errMsg = "Failure in removeValue operation for entity [" + value.getEntityName() + "]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, MODULE);
            TransactionUtil.rollback(beganTransaction, errMsg, e);
            throw new GenericEntityException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#removeByAnd(java.lang.String, java.lang.Object)
     */
    @Override
    public int removeByAnd(String entityName, Object... fields) throws GenericEntityException {
        return removeByAnd(entityName, UtilMisc.<String, Object>toMap(fields));
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#removeByAnd(java.lang.String, java.util.Map)
     */
    @Override
    public int removeByAnd(String entityName, Map<String, ? extends Object> fields) throws GenericEntityException {
        EntityCondition ecl = EntityCondition.makeCondition(fields);
        return removeByCondition(entityName, ecl);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#removeByCondition(java.lang.String, org.apache.ofbiz.entity.condition.EntityCondition)
     */
    @Override
    public int removeByCondition(String entityName, EntityCondition condition) throws GenericEntityException {
        boolean beganTransaction = false;
        try {
            if (alwaysUseTransaction) {
                beganTransaction = TransactionUtil.begin();
            }

            ModelEntity modelEntity = getModelReader().getModelEntity(entityName);
            GenericHelper helper = getEntityHelper(entityName);

            // We check if there are eca rules for this entity
            String entityEcaReaderName = EntityEcaUtil.getEntityEcaReaderName(this.delegatorBaseName);
            boolean hasEntityEcaRules = UtilValidate.isNotEmpty(
                    EntityEcaUtil.getEntityEcaCache(entityEcaReaderName).get(entityName));

            // When we delete in mass, if we are in test mode or the entity have an eeca linked we will remove one by one
            // for test mode to help the rollback
            // for eeca to analyse each value to check if a condition match
            List<GenericValue> removedEntities = (testMode || hasEntityEcaRules)
                ? findList(entityName, condition, null, null, null, false)
                : Collections.emptyList();

            int rowsAffected = 0;
            if (!removedEntities.isEmpty()) {
                for (GenericValue entity : removedEntities) {
                    rowsAffected += removeValue(entity);
                }
            } else {
                rowsAffected = helper.removeByCondition(this, modelEntity, condition);
                if (rowsAffected > 0) {
                    this.clearCacheLine(entityName);
                }
            }

            if (testMode) {
                for (GenericValue entity : removedEntities) {
                    storeForTestRollback(new TestOperation(OperationType.DELETE, entity));
                }
            }
            TransactionUtil.commit(beganTransaction);
            return rowsAffected;
        } catch (IllegalStateException | GenericEntityException e) {
            String errMsg = "Failure in removeByCondition operation for entity [" + entityName + "]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, MODULE);
            TransactionUtil.rollback(beganTransaction, errMsg, e);
            throw new GenericEntityException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#removeRelated(java.lang.String, org.apache.ofbiz.entity.GenericValue)
     */
    @Override
    public int removeRelated(String relationName, GenericValue value) throws GenericEntityException {
        ModelEntity modelEntity = value.getModelEntity();
        ModelRelation relation = modelEntity.getRelation(relationName);

        if (relation == null) {
            throw new GenericModelException("Could not find relation for relationName: " + relationName + " for value " + value);
        }

        Map<String, Object> fields = new HashMap<>();
        for (ModelKeyMap keyMap : relation.getKeyMaps()) {
            fields.put(keyMap.getRelFieldName(), value.get(keyMap.getFieldName()));
        }

        return this.removeByAnd(relation.getRelEntityName(), fields);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#refresh(org.apache.ofbiz.entity.GenericValue)
     */
    @Override
    public void refresh(GenericValue value) throws GenericEntityException {
        // always clear cache before the operation
        clearCacheLine(value);

        GenericPK pk = value.getPrimaryKey();
        GenericValue newValue = this.findOne(pk.getEntityName(), pk, false);
        value.refreshFromValue(newValue);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#refreshFromCache(org.apache.ofbiz.entity.GenericValue)
     */
    @Override
    public void refreshFromCache(GenericValue value) throws GenericEntityException {
        GenericPK pk = value.getPrimaryKey();
        GenericValue newValue = findOne(pk.getEntityName(), pk, true);
        value.refreshFromValue(newValue);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#storeByCondition(java.lang.String, java.util.Map, org.apache.ofbiz.entity.condition.EntityCondition)
     */
    @Override
    public int storeByCondition(String entityName, Map<String, ? extends Object> fieldsToSet, EntityCondition condition) throws GenericEntityException {
        boolean beganTransaction = false;
        try {
            if (alwaysUseTransaction) {
                beganTransaction = TransactionUtil.begin();
            }

            ModelEntity modelEntity = getModelReader().getModelEntity(entityName);
            GenericHelper helper = getEntityHelper(entityName);

            List<GenericValue> updatedEntities = null;
            if (testMode) {
                updatedEntities = this.findList(entityName, condition, null, null, null, false);
            }

            int rowsAffected = helper.storeByCondition(this, modelEntity, fieldsToSet, condition);
            if (rowsAffected > 0) {
                this.clearCacheLine(entityName);
            }

            if (testMode) {
                for (GenericValue entity : updatedEntities) {
                    storeForTestRollback(new TestOperation(OperationType.UPDATE, entity));
                }
            }
            TransactionUtil.commit(beganTransaction);
            return rowsAffected;
        } catch (IllegalStateException | GenericEntityException e) {
            String errMsg = "Failure in storeByCondition operation for entity [" + entityName + "]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, MODULE);
            TransactionUtil.rollback(beganTransaction, errMsg, e);
            throw new GenericEntityException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#store(org.apache.ofbiz.entity.GenericValue)
     */
    @Override
    public int store(GenericValue value) throws GenericEntityException {
        boolean beganTransaction = false;
        try {
            if (alwaysUseTransaction) {
                beganTransaction = TransactionUtil.begin();
            }

            EntityEcaRuleRunner<?> ecaRunner = this.getEcaRuleRunner(value.getEntityName());
            ecaRunner.evalRules(EntityEcaHandler.EV_VALIDATE, EntityEcaHandler.OP_STORE, value, false);
            GenericHelper helper = getEntityHelper(value.getEntityName());

            ecaRunner.evalRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_STORE, value, false);

            // if audit log on for any fields, save old value before the update so we still have both
            if (value.getModelEntity().getHasFieldWithAuditLog()) {
                createEntityAuditLogAll(value, true, false);
            }

            GenericValue updatedEntity = null;

            if (testMode) {
                updatedEntity = this.findOne(value.getEntityName(), value.getPrimaryKey(), false);
            }

            int retVal = helper.store(value);

            // doCacheClear
            ecaRunner.evalRules(EntityEcaHandler.EV_CACHE_CLEAR, EntityEcaHandler.OP_STORE, value, false);
            this.clearCacheLine(value);

            if (testMode) {
                storeForTestRollback(new TestOperation(OperationType.UPDATE, updatedEntity));
            }
            // refresh the valueObject to get the new version
            if (value.lockEnabled()) {
                refresh(value);
            }

            ecaRunner.evalRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_STORE, value, false);
            TransactionUtil.commit(beganTransaction);
            return retVal;
        } catch (IllegalStateException | GenericEntityException e) {
            String errMsg = "Failure in store operation for entity [" + value.getEntityName() + "]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, MODULE);
            TransactionUtil.rollback(beganTransaction, errMsg, e);
            throw new GenericEntityException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#storeAll(java.util.List)
     */
    @Override
    public int storeAll(List<GenericValue> values) throws GenericEntityException {
        return this.storeAll(values, new EntityStoreOptions());
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#storeAll(java.util.List, org.apache.ofbiz.entity.util.EntityStoreOptions)
     */
    @Override
    public int storeAll(List<GenericValue> values, EntityStoreOptions storeOptions) throws GenericEntityException {
        if (values == null) {
            return 0;
        }

        // if no store options passed, use default
        if (storeOptions == null) {
            storeOptions = new EntityStoreOptions();
        }

        int numberChanged = 0;

        boolean beganTransaction = false;
        try {
            beganTransaction = TransactionUtil.begin();

            for (GenericValue value: values) {
                String entityName = value.getEntityName();
                GenericPK primaryKey = value.getPrimaryKey();
                GenericHelper helper = getEntityHelper(entityName);

                // exists?
                // NOTE: don't use findByPrimaryKey because we don't want to the ECA events to fire and such
                if (!primaryKey.isPrimaryKey()) {
                    throw new GenericModelException("[GenericDelegator.storeAll] One of the passed primary keys is not a valid primary key: " + primaryKey);
                }
                GenericValue existing;
                try {
                    existing = helper.findByPrimaryKey(primaryKey);
                } catch (GenericEntityNotFoundException e) {
                    existing = null;
                }

                if (existing == null) {
                    if (storeOptions.isCreateDummyFks()) {
                        value.checkFks(true);
                    }
                    this.create(value);
                    numberChanged++;
                } else {
                    // don't send fields that are the same, and if no fields have changed, update nothing
                    ModelEntity modelEntity = value.getModelEntity();
                    GenericValue toStore = GenericValue.create(this, modelEntity, value.getPrimaryKey());
                    boolean atLeastOneField = false;
                    Iterator<ModelField> nonPksIter = modelEntity.getNopksIterator();
                    while (nonPksIter.hasNext()) {
                        ModelField modelField = nonPksIter.next();
                        String fieldName = modelField.getName();
                        if (value.containsKey(fieldName)) {
                            Object fieldValue = value.get(fieldName);
                            Object oldValue = existing.get(fieldName);
                            if (!Objects.equals(oldValue, fieldValue)) {
                                toStore.put(fieldName, fieldValue);
                                atLeastOneField = true;
                            }
                        }
                    }

                    if (atLeastOneField) {
                        if (storeOptions.isCreateDummyFks()) {
                            value.checkFks(true);
                        }
                        numberChanged += this.store(toStore);
                    }
                }
            }
            TransactionUtil.commit(beganTransaction);
            return numberChanged;
        } catch (GenericEntityException e) {
            String errMsg = "Failure in storeAll operation: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, MODULE);
            TransactionUtil.rollback(beganTransaction, errMsg, e);
            throw new GenericEntityException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#removeAll(java.lang.String)
     */
    @Override
    public int removeAll(String entityName) throws GenericEntityException {
        return removeByAnd(entityName, (Map<String, Object>) null);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#removeAll(java.util.List)
     */
    @Override
    public int removeAll(List<? extends GenericEntity> dummyPKs) throws GenericEntityException {
        if (dummyPKs == null) {
            return 0;
        }

        boolean beganTransaction = false;
        int numRemoved = 0;

        try {
            for (GenericEntity value: dummyPKs) {
                if (value.containsPrimaryKey()) {
                    numRemoved += this.removeByPrimaryKey(value.getPrimaryKey());
                } else {
                    numRemoved += this.removeByAnd(value.getEntityName(), value.getAllFields());
                }
            }
            TransactionUtil.commit(beganTransaction);
            return numRemoved;
        } catch (GenericEntityException e) {
            String errMsg = "Failure in removeAll operation: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, MODULE);
            TransactionUtil.rollback(beganTransaction, errMsg, e);
            throw new GenericEntityException(e);
        }
    }

    // ======================================
    // ======= Find Methods =================
    // ======================================

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#findOne(java.lang.String, boolean, java.lang.Object)
     */
    @Override
    public GenericValue findOne(String entityName, boolean useCache, Object... fields) throws GenericEntityException {
        return findOne(entityName, UtilMisc.toMap(fields), useCache);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#findOne(java.lang.String, java.util.Map, boolean)
     */
    @Override
    public GenericValue findOne(String entityName, Map<String, ? extends Object> fields, boolean useCache) throws GenericEntityException {
        GenericPK primaryKey = this.makePK(entityName, fields);
        if (!primaryKey.isPrimaryKey()) {
            throw new GenericModelException("[GenericDelegator.findOne] Passed primary key is not a valid primary key: " + primaryKey);
        }
        EntityEcaRuleRunner<?> ecaRunner = this.getEcaRuleRunner(entityName);
        if (useCache) {
            ecaRunner.evalRules(EntityEcaHandler.EV_CACHE_CHECK, EntityEcaHandler.OP_FIND, primaryKey, false);
            GenericValue value = cache.get(primaryKey);
            if (value == GenericValue.NULL_VALUE) {
                return null;
            }
            if (value != null) {
                return value;
            }
        }

        boolean beganTransaction = false;
        try {
            if (alwaysUseTransaction) {
                beganTransaction = TransactionUtil.begin();
            }

            ecaRunner.evalRules(EntityEcaHandler.EV_VALIDATE, EntityEcaHandler.OP_FIND, primaryKey, false);

            GenericHelper helper = getEntityHelper(entityName);
            GenericValue value = null;

            ecaRunner.evalRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_FIND, primaryKey, false);
            try {
                value = helper.findByPrimaryKey(primaryKey);
            } catch (GenericEntityNotFoundException e) {
            }
            if (value != null) {
                value.setDelegator(this);
            }

            if (useCache) {
                if (value != null) {
                    ecaRunner.evalRules(EntityEcaHandler.EV_CACHE_PUT, EntityEcaHandler.OP_FIND, value, false);
                    this.putInPrimaryKeyCache(primaryKey, value);
                } else {
                    this.putInPrimaryKeyCache(primaryKey, GenericValue.NULL_VALUE);
                }
            }

            ecaRunner.evalRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_FIND, (value == null ? primaryKey : value), false);
            TransactionUtil.commit(beganTransaction);
            return value;
        } catch (GenericEntityException e) {
            String errMsg = "Failure in findOne operation for entity [" + entityName + "]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, MODULE);
            TransactionUtil.rollback(beganTransaction, errMsg, e);
            throw new GenericEntityException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#findByPrimaryKeyPartial(org.apache.ofbiz.entity.GenericPK, java.util.Set)
     */
    @Override
    public GenericValue findByPrimaryKeyPartial(GenericPK primaryKey, Set<String> keys) throws GenericEntityException {
        boolean beganTransaction = false;
        try {
            if (alwaysUseTransaction) {
                beganTransaction = TransactionUtil.begin();
            }

            EntityEcaRuleRunner<?> ecaRunner = this.getEcaRuleRunner(primaryKey.getEntityName());
            ecaRunner.evalRules(EntityEcaHandler.EV_VALIDATE, EntityEcaHandler.OP_FIND, primaryKey, false);

            GenericHelper helper = getEntityHelper(primaryKey.getEntityName());
            GenericValue value = null;

            if (!primaryKey.isPrimaryKey()) {
                throw new GenericModelException("[GenericDelegator.findByPrimaryKey] Passed primary key is not a valid primary key: " + primaryKey);
            }

            ecaRunner.evalRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_FIND, primaryKey, false);
            try {
                value = helper.findByPrimaryKeyPartial(primaryKey, keys);
            } catch (GenericEntityNotFoundException e) {
            }
            if (value != null) {
                value.setDelegator(this);
            }

            ecaRunner.evalRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_FIND, primaryKey, false);
            TransactionUtil.commit(beganTransaction);
            return value;
        } catch (GenericEntityException e) {
            String errMsg = "Failure in findByPrimaryKeyPartial operation for entity [" + primaryKey.getEntityName() + "]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, MODULE);
            TransactionUtil.rollback(beganTransaction, errMsg, e);
            throw new GenericEntityException(e);
        }
    }

    /** Finds all Generic entities
     *@param entityName The Name of the Entity as defined in the entity XML file
     * @see org.apache.ofbiz.entity.Delegator#findAll(java.lang.String, boolean)
     */
    @Override
    public List<GenericValue> findAll(String entityName, boolean useCache) throws GenericEntityException {
        return this.findList(entityName, null, null, null, null, useCache);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#findByAnd(java.lang.String, java.util.Map, java.util.List, boolean)
     */
    @Override
    public List<GenericValue> findByAnd(String entityName, Map<String, ? extends Object> fields, List<String> orderBy, boolean useCache) throws GenericEntityException {
        return this.findList(entityName, EntityCondition.makeCondition(fields), null, orderBy, null, useCache);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#find(java.lang.String, org.apache.ofbiz.entity.condition.EntityCondition, org.apache.ofbiz.entity.condition.EntityCondition, java.util.Set, java.util.List, org.apache.ofbiz.entity.util.EntityFindOptions)
     */
    @Override
    public EntityListIterator find(String entityName, EntityCondition whereEntityCondition, EntityCondition havingEntityCondition, Set<String> fieldsToSelect, List<String> orderBy, EntityFindOptions findOptions) throws GenericEntityException {

        // if there is no transaction throw an exception, we don't want to create a transaction here since closing it would mess up the ELI
        if (!TransactionUtil.isTransactionInPlace()) {
            //throw new GenericEntityException("ERROR: Cannot do a find that returns an EntityListIterator with no transaction in place. Wrap this call in a transaction.");

            //throwing an exception is a little harsh for now, just display a really big error message since we want to get all of these fixed...
            Exception newE = new Exception("Stack Trace");
            Debug.logError(newE, "ERROR: Cannot do a find that returns an EntityListIterator with no transaction in place. Wrap this call in a transaction.", MODULE);
        }

        ModelEntity modelEntity = getModelReader().getModelEntity(entityName);
        GenericValue dummyValue = GenericValue.create(modelEntity);
        EntityEcaRuleRunner<?> ecaRunner = this.getEcaRuleRunner(modelEntity.getEntityName());
        ecaRunner.evalRules(EntityEcaHandler.EV_VALIDATE, EntityEcaHandler.OP_FIND, dummyValue, false);

        if (whereEntityCondition != null) {
            whereEntityCondition.checkCondition(modelEntity);
        }
        if (havingEntityCondition != null) {
            havingEntityCondition.checkCondition(modelEntity);
        }

        ecaRunner.evalRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_FIND, dummyValue, false);
        GenericHelper helper = getEntityHelper(modelEntity.getEntityName());
        EntityListIterator eli = helper.findListIteratorByCondition(this, modelEntity, whereEntityCondition, havingEntityCondition, fieldsToSelect, orderBy, findOptions);
        eli.setDelegator(this);

        ecaRunner.evalRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_FIND, dummyValue, false);
        return eli;
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#findList(java.lang.String, org.apache.ofbiz.entity.condition.EntityCondition,
     *  org.apache.ofbiz.entity.condition.EntityCondition, org.apache.ofbiz.entity.condition.EntityCondition, java.util.Set,
     *  java.util.List, org.apache.ofbiz.entity.util.EntityFindOptions, boolean)
     */
    @Override
    public List<GenericValue> findList(String entityName, EntityCondition entityCondition, Set<String> fieldsToSelect, List<String> orderBy,
                                       EntityFindOptions findOptions, boolean useCache) throws GenericEntityException {
        return findList(entityName, entityCondition, null, fieldsToSelect, orderBy, findOptions, useCache);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#findList(java.lang.String, org.apache.ofbiz.entity.condition.EntityCondition, java.util.Set, java.util.List, org.apache.ofbiz.entity.util.EntityFindOptions, boolean)
     */
    @Override
    public List<GenericValue> findList(String entityName, EntityCondition entityCondition, EntityCondition havingCondition,
                  Set<String> fieldsToSelect, List<String> orderBy, EntityFindOptions findOptions, boolean useCache) throws GenericEntityException {

        EntityEcaRuleRunner<?> ecaRunner = null;
        GenericValue dummyValue = null;
        if (useCache) {
            ecaRunner = this.getEcaRuleRunner(entityName);
            ModelEntity modelEntity = getModelReader().getModelEntity(entityName);
            dummyValue = GenericValue.create(modelEntity);
            ecaRunner.evalRules(EntityEcaHandler.EV_CACHE_CHECK, EntityEcaHandler.OP_FIND, dummyValue, false);

            List<GenericValue> cacheList = this.cache.get(entityName, entityCondition, orderBy);
            if (cacheList != null) {
                return cacheList;
            }
        }

        boolean beganTransaction = false;
        try {
            if (alwaysUseTransaction) {
                beganTransaction = TransactionUtil.begin();
            }

            List<GenericValue> list = null;
            try (EntityListIterator eli = this.find(entityName, entityCondition, havingCondition, fieldsToSelect, orderBy, findOptions)) {
                list = eli.getCompleteList();
            }

            if (useCache) {
                ecaRunner.evalRules(EntityEcaHandler.EV_CACHE_PUT, EntityEcaHandler.OP_FIND, dummyValue, false);
                this.cache.put(entityName, entityCondition, orderBy, list);
            }
            TransactionUtil.commit(beganTransaction);
            return list;
        } catch (GenericEntityException e) {
            String errMsg = "Failure in findByCondition operation for entity [" + entityName + "]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, MODULE);
            TransactionUtil.rollback(beganTransaction, errMsg, e);
            throw new GenericEntityException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#findListIteratorByCondition(org.apache.ofbiz.entity.model.DynamicViewEntity, org.apache.ofbiz.entity.condition.EntityCondition, org.apache.ofbiz.entity.condition.EntityCondition, java.util.Collection, java.util.List, org.apache.ofbiz.entity.util.EntityFindOptions)
     */
    @Override
    public EntityListIterator findListIteratorByCondition(DynamicViewEntity dynamicViewEntity, EntityCondition whereEntityCondition, EntityCondition havingEntityCondition, Collection<String> fieldsToSelect, List<String> orderBy, EntityFindOptions findOptions) throws GenericEntityException {

        // if there is no transaction throw an exception, we don't want to create a transaction here since closing it would mess up the ELI
        if (!TransactionUtil.isTransactionInPlace()) {
            //throw new GenericEntityException("ERROR: Cannot do a find that returns an EntityListIterator with no transaction in place. Wrap this call in a transaction.");

            //throwing an exception is a little harsh for now, just display a really big error message since we want to get all of these fixed...
            Exception newE = new Exception("Stack Trace");
            Debug.logError(newE, "ERROR: Cannot do a find that returns an EntityListIterator with no transaction in place. Wrap this call in a transaction.", MODULE);
        }

        ModelViewEntity modelViewEntity = dynamicViewEntity.makeModelViewEntity(this);
        if (whereEntityCondition != null) {
            whereEntityCondition.checkCondition(modelViewEntity);
        }
        if (havingEntityCondition != null) {
            havingEntityCondition.checkCondition(modelViewEntity);
        }

        GenericHelper helper = getEntityHelper(dynamicViewEntity.getOneRealEntityName());
        EntityListIterator eli = helper.findListIteratorByCondition(this, modelViewEntity, whereEntityCondition,
                havingEntityCondition, fieldsToSelect, orderBy, findOptions);
        eli.setDelegator(this);
        //TODO: add decrypt fields
        return eli;
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#findCountByCondition(java.lang.String, org.apache.ofbiz.entity.condition.EntityCondition,
     *  org.apache.ofbiz.entity.condition.EntityCondition, org.apache.ofbiz.entity.util.EntityFindOptions)
     */
    @Override
    public long findCountByCondition(String entityName, EntityCondition whereEntityCondition,
            EntityCondition havingEntityCondition, EntityFindOptions findOptions) throws GenericEntityException {
        return findCountByCondition(entityName, whereEntityCondition, null, havingEntityCondition, findOptions);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#findCountByCondition(java.lang.String, org.apache.ofbiz.entity.condition.EntityCondition,
     *  java.util.Set, org.apache.ofbiz.entity.condition.EntityCondition, org.apache.ofbiz.entity.util.EntityFindOptions)
     */
    @Override
    public long findCountByCondition(String entityName, EntityCondition whereEntityCondition, Set<String> fieldsToSelect,
                                     EntityCondition havingEntityCondition, EntityFindOptions findOptions) throws GenericEntityException {

        boolean beganTransaction = false;
        try {
            if (alwaysUseTransaction) {
                beganTransaction = TransactionUtil.begin();
            }

            ModelEntity modelEntity = getModelReader().getModelEntity(entityName);
            GenericValue dummyValue = GenericValue.create(modelEntity);
            EntityEcaRuleRunner<?> ecaRunner = this.getEcaRuleRunner(modelEntity.getEntityName());
            ecaRunner.evalRules(EntityEcaHandler.EV_VALIDATE, EntityEcaHandler.OP_FIND, dummyValue, false);

            if (whereEntityCondition != null) {
                whereEntityCondition.checkCondition(modelEntity);
            }
            if (havingEntityCondition != null) {
                havingEntityCondition.checkCondition(modelEntity);
            }

            List<ModelField> selectFields = new LinkedList<>();
            if (UtilValidate.isNotEmpty(fieldsToSelect)) {
                for (String fieldToSelect : fieldsToSelect) {
                    ModelField curField = modelEntity.getField(fieldToSelect);
                    if (curField != null) {
                        selectFields.add(curField);
                    }
                }
            }

            ecaRunner.evalRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_FIND, dummyValue, false);
            GenericHelper helper = getEntityHelper(modelEntity.getEntityName());
            long count = helper.findCountByCondition(this, modelEntity, whereEntityCondition,
                                            havingEntityCondition, selectFields, findOptions);

            ecaRunner.evalRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_FIND, dummyValue, false);
            TransactionUtil.commit(beganTransaction);
            return count;
        } catch (GenericEntityException e) {
            String errMsg = "Failure in findListIteratorByCondition operation for entity [DynamicView]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, MODULE);
            TransactionUtil.rollback(beganTransaction, errMsg, e);
            throw new GenericEntityException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#getMultiRelation(org.apache.ofbiz.entity.GenericValue, java.lang.String, java.lang.String, java.util.List)
     */
    @Override
    public List<GenericValue> getMultiRelation(GenericValue value, String relationNameOne, String relationNameTwo, List<String> orderBy) throws GenericEntityException {
        boolean beganTransaction = false;
        try {
            if (alwaysUseTransaction) {
                beganTransaction = TransactionUtil.begin();
            }

            //TODO: add eca eval calls
            // traverse the relationships
            ModelEntity modelEntity = value.getModelEntity();
            ModelRelation modelRelationOne = modelEntity.getRelation(relationNameOne);
            ModelEntity modelEntityOne = getModelEntity(modelRelationOne.getRelEntityName());
            ModelRelation modelRelationTwo = modelEntityOne.getRelation(relationNameTwo);
            ModelEntity modelEntityTwo = getModelEntity(modelRelationTwo.getRelEntityName());

            GenericHelper helper = getEntityHelper(modelEntity);
            List<GenericValue> result = helper.findByMultiRelation(value, modelRelationOne, modelEntityOne, modelRelationTwo, modelEntityTwo, orderBy);
            TransactionUtil.commit(beganTransaction);
            return result;
        } catch (GenericEntityException e) {
            String errMsg = "Failure in getMultiRelation operation for entity [" + value.getEntityName() + "]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, MODULE);
            TransactionUtil.rollback(beganTransaction, errMsg, e);
            throw new GenericEntityException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#getRelated(java.lang.String, java.util.Map, java.util.List, org.apache.ofbiz.entity.GenericValue, boolean)
     */
    @Override
    public List<GenericValue> getRelated(String relationName, Map<String, ? extends Object> byAndFields, List<String> orderBy, GenericValue value, boolean useCache) throws GenericEntityException {
        ModelEntity modelEntity = value.getModelEntity();
        ModelRelation relation = modelEntity.getRelation(relationName);

        if (relation == null) {
            throw new GenericModelException("Could not find relation for relationName: " + relationName + " for value " + value);
        }

        // put the byAndFields (if not null) into the hash map first,
        // they will be overridden by value's fields if over-specified this is important for security and cleanliness
        Map<String, Object> fields = new HashMap<>();
        if (byAndFields != null) {
            fields.putAll(byAndFields);
        }
        for (ModelKeyMap keyMap : relation.getKeyMaps()) {
            fields.put(keyMap.getRelFieldName(), value.get(keyMap.getFieldName()));
        }

        return this.findByAnd(relation.getRelEntityName(), fields, orderBy, useCache);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#getRelatedDummyPK(java.lang.String, java.util.Map, org.apache.ofbiz.entity.GenericValue)
     */
    @Override
    public GenericPK getRelatedDummyPK(String relationName, Map<String, ? extends Object> byAndFields, GenericValue value) throws GenericEntityException {
        ModelEntity modelEntity = value.getModelEntity();
        ModelRelation relation = modelEntity.getRelation(relationName);

        if (relation == null) {
            throw new GenericModelException("Could not find relation for relationName: " + relationName + " for value " + value);
        }
        ModelEntity relatedEntity = getModelReader().getModelEntity(relation.getRelEntityName());

        // put the byAndFields (if not null) into the hash map first,
        // they will be overridden by value's fields if over-specified this is important for security and cleanliness
        Map<String, Object> fields = new HashMap<>();
        if (byAndFields != null) {
            fields.putAll(byAndFields);
        }
        for (ModelKeyMap keyMap : relation.getKeyMaps()) {
            fields.put(keyMap.getRelFieldName(), value.get(keyMap.getFieldName()));
        }

        return GenericPK.create(this, relatedEntity, fields);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#getRelatedOne(java.lang.String, org.apache.ofbiz.entity.GenericValue, boolean)
     */
    @Override
    public GenericValue getRelatedOne(String relationName, GenericValue value, boolean useCache) throws GenericEntityException {
        ModelRelation relation = value.getModelEntity().getRelation(relationName);

        if (relation == null) {
            throw new GenericModelException("Could not find relation for relationName: " + relationName + " for value " + value);
        }
        if (!"one".equals(relation.getType()) && !"one-nofk".equals(relation.getType())) {
            throw new GenericModelException("Relation is not a 'one' or a 'one-nofk' relation: " + relationName + " of entity " + value.getEntityName());
        }

        Map<String, Object> fields = new HashMap<>();
        for (ModelKeyMap keyMap : relation.getKeyMaps()) {
            fields.put(keyMap.getRelFieldName(), value.get(keyMap.getFieldName()));
        }

        return this.findOne(relation.getRelEntityName(), fields, useCache);
    }


    // ======================================
    // ======= Cache Related Methods ========
    // ======================================

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#clearAllCaches()
     */
    @Override
    public void clearAllCaches() {
        this.clearAllCaches(true);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#clearAllCaches(boolean)
     */
    @Override
    public void clearAllCaches(boolean distribute) {
        cache.clear();

        if (!distribute) {
            return;
        }
        DistributedCacheClear dcc = getDistributedCacheClear();
        if (dcc != null) {
            dcc.clearAllCaches();
        }
        if (this.crypto != null) {
            this.crypto.clearKeyCache();
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#clearCacheLine(java.lang.String)
     */
    @Override
    public void clearCacheLine(String entityName) {
        cache.remove(entityName);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#clearCacheLine(java.lang.String, java.lang.Object)
     */
    @Override
    public void clearCacheLine(String entityName, Object... fields) {
        clearCacheLine(entityName, UtilMisc.<String, Object>toMap(fields));
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#clearCacheLine(java.lang.String, java.util.Map)
     */
    @Override
    public void clearCacheLine(String entityName, Map<String, ? extends Object> fields) {
        // if no fields passed, do the all cache quickly and return
        if (fields == null) {
            cache.remove(entityName);
            return;
        }

        ModelEntity entity = this.getModelEntity(entityName);
        if (entity == null) {
            throw new IllegalArgumentException("[GenericDelegator.clearCacheLine] could not find entity for entityName: " + entityName);
        }
        //if never cached, then don't bother clearing
        if (entity.getNeverCache()) {
            return;
        }

        GenericValue dummyValue = GenericValue.create(this, entity, fields);
        this.clearCacheLineFlexible(dummyValue);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#clearCacheLineFlexible(org.apache.ofbiz.entity.GenericEntity)
     */
    @Override
    public void clearCacheLineFlexible(GenericEntity dummyPK) {
        this.clearCacheLineFlexible(dummyPK, true);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#clearCacheLineFlexible(org.apache.ofbiz.entity.GenericEntity, boolean)
     */
    @Override
    public void clearCacheLineFlexible(GenericEntity dummyPK, boolean distribute) {
        if (dummyPK != null) {
            //if never cached, then don't bother clearing
            if (dummyPK.getModelEntity().getNeverCache()) {
                return;
            }

            cache.remove(dummyPK);

            if (!distribute) {
                return;
            }

            DistributedCacheClear dcc = getDistributedCacheClear();
            if (dcc != null) {
                dcc.distributedClearCacheLineFlexible(dummyPK);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#clearCacheLineByCondition(java.lang.String, org.apache.ofbiz.entity.condition.EntityCondition)
     */
    @Override
    public void clearCacheLineByCondition(String entityName, EntityCondition condition) {
        clearCacheLineByCondition(entityName, condition, true);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#clearCacheLineByCondition(java.lang.String, org.apache.ofbiz.entity.condition.EntityCondition, boolean)
     */
    @Override
    public void clearCacheLineByCondition(String entityName, EntityCondition condition, boolean distribute) {
        if (entityName != null) {
            //if never cached, then don't bother clearing
            if (getModelEntity(entityName).getNeverCache()) {
                return;
            }

            cache.remove(entityName, condition);

            if (!distribute) {
                return;
            }

            DistributedCacheClear dcc = getDistributedCacheClear();
            if (dcc != null) {
                dcc.distributedClearCacheLineByCondition(entityName, condition);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#clearCacheLine(org.apache.ofbiz.entity.GenericPK)
     */
    @Override
    public void clearCacheLine(GenericPK primaryKey) {
        this.clearCacheLine(primaryKey, true);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#clearCacheLine(org.apache.ofbiz.entity.GenericPK, boolean)
     */
    @Override
    public void clearCacheLine(GenericPK primaryKey, boolean distribute) {
        if (primaryKey == null) {
            return;
        }

        //if never cached, then don't bother clearing
        if (primaryKey.getModelEntity().getNeverCache()) {
            return;
        }

        cache.remove(primaryKey);

        if (!distribute) {
            return;
        }

        DistributedCacheClear dcc = getDistributedCacheClear();
        if (dcc != null) {
            dcc.distributedClearCacheLine(primaryKey);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#clearCacheLine(org.apache.ofbiz.entity.GenericValue)
     */
    @Override
    public void clearCacheLine(GenericValue value) {
        this.clearCacheLine(value, true);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#clearCacheLine(org.apache.ofbiz.entity.GenericValue, boolean)
     */
    @Override
    public void clearCacheLine(GenericValue value, boolean distribute) {
        if (value == null) {
            return;
        }

        //if never cached, then don't bother clearing
        if (value.getModelEntity().getNeverCache()) {
            return;
        }

        cache.remove(value);

        if (!distribute) {
            return;
        }

        DistributedCacheClear dcc = getDistributedCacheClear();
        if (dcc != null) {
            dcc.distributedClearCacheLine(value);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#clearAllCacheLinesByDummyPK(java.util.Collection)
     */
    @Override
    public void clearAllCacheLinesByDummyPK(Collection<GenericPK> dummyPKs) {
        if (dummyPKs == null) {
            return;
        }
        for (GenericEntity entity: dummyPKs) {
            this.clearCacheLineFlexible(entity);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#clearAllCacheLinesByValue(java.util.Collection)
     */
    @Override
    public void clearAllCacheLinesByValue(Collection<GenericValue> values) {
        if (values == null) {
            return;
        }
        for (GenericValue value: values) {
            this.clearCacheLine(value);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#getFromPrimaryKeyCache(org.apache.ofbiz.entity.GenericPK)
     */
    @Override
    public GenericValue getFromPrimaryKeyCache(GenericPK primaryKey) {
        if (primaryKey == null) {
            return null;
        }
        GenericValue value = cache.get(primaryKey);
        if (value == GenericValue.NULL_VALUE) {
            return null;
        }
        return value;
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#putInPrimaryKeyCache(org.apache.ofbiz.entity.GenericPK, org.apache.ofbiz.entity.GenericValue)
     */
    @Override
    public void putInPrimaryKeyCache(GenericPK primaryKey, GenericValue value) {
        if (primaryKey == null) {
            return;
        }

        if (primaryKey.getModelEntity().getNeverCache()) {
            if (Debug.warningOn()) {
                Debug.logWarning("Tried to put a value of the " + value.getEntityName() + " entity in the BY PRIMARY KEY cache but this entity has never-cache set to true, not caching.", MODULE);
            }
            return;
        }

        // before going into the cache, make this value immutable
        value.setImmutable();
        cache.put(primaryKey, value);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#putAllInPrimaryKeyCache(java.util.List)
     */
    @Override
    public void putAllInPrimaryKeyCache(List<GenericValue> values) {
        if (values == null) {
            return;
        }
        for (GenericValue value: values) {
            this.putInPrimaryKeyCache(value.getPrimaryKey(), value);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#setDistributedCacheClear(org.apache.ofbiz.entity.util.DistributedCacheClear)
     */
    @Override
    public void setDistributedCacheClear(DistributedCacheClear distributedCacheClear) {
        this.distributedCacheClear.set(new ConstantFuture<>(distributedCacheClear));
    }

    // ======= XML Related Methods ========
    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#readXmlDocument(java.net.URL)
     */
    @Override
    public List<GenericValue> readXmlDocument(URL url) throws SAXException, ParserConfigurationException, java.io.IOException {
        if (url == null) {
            return null;
        }
        return this.makeValues(UtilXml.readXmlDocument(url, false));
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#makeValues(org.w3c.dom.Document)
     */
    @Override
    public List<GenericValue> makeValues(Document document) {
        if (document == null) {
            return null;
        }
        List<GenericValue> values = new LinkedList<>();

        Element docElement = document.getDocumentElement();

        if (docElement == null) {
            return null;
        }
        if (!"entity-engine-xml".equals(docElement.getTagName())) {
            Debug.logError("[GenericDelegator.makeValues] Root node was not <entity-engine-xml>", MODULE);
            throw new java.lang.IllegalArgumentException("Root node was not <entity-engine-xml>");
        }
        docElement.normalize();
        Node curChild = docElement.getFirstChild();

        if (curChild != null) {
            do {
                if (curChild.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) curChild;
                    GenericValue value = this.makeValue(element);

                    if (value != null) {
                        values.add(value);
                    }
                }
            } while ((curChild = curChild.getNextSibling()) != null);
        } else {
            Debug.logWarning("[GenericDelegator.makeValues] No child nodes found in document.", MODULE);
        }

        return values;
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#makePK(org.w3c.dom.Element)
     */
    @Override
    public GenericPK makePK(Element element) {
        GenericValue value = makeValue(element);

        return value.getPrimaryKey();
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#makeValue(org.w3c.dom.Element)
     */
    @Override
    public GenericValue makeValue(Element element) {
        if (element == null) {
            return null;
        }
        String entityName = element.getTagName();

        // if a dash or colon is in the tag name, grab what is after it
        if (entityName.indexOf('-') > 0) {
            entityName = entityName.substring(entityName.indexOf('-') + 1);
        }
        if (entityName.indexOf(':') > 0) {
            entityName = entityName.substring(entityName.indexOf(':') + 1);
        }
        GenericValue value = this.makeValue(entityName);

        ModelEntity modelEntity = value.getModelEntity();

        Iterator<ModelField> modelFields = modelEntity.getFieldsIterator();

        while (modelFields.hasNext()) {
            ModelField modelField = modelFields.next();
            String name = modelField.getName();
            String attr = element.getAttribute(name);

            if (UtilValidate.isNotEmpty(attr)) {
                // GenericEntity.makeXmlElement() sets null values to GenericEntity.NULL_FIELD.toString(), so look for
                //     that and treat it as null
                if (GenericEntity.NULL_FIELD.toString().equals(attr)) {
                    value.set(name, null);
                } else {
                    value.setString(name, attr);
                }
            } else {
                // if no attribute try a subelement
                Element subElement = UtilXml.firstChildElement(element, name);

                if (subElement != null) {
                    value.setString(name, UtilXml.elementValue(subElement));
                }
            }
        }

        return value;
    }

    // ======= Misc Methods ========

    protected static class EntityEcaRuleRunner<T> {
        protected EntityEcaHandler<T> entityEcaHandler;
        protected Map<String, List<T>> eventMap;

        protected EntityEcaRuleRunner(EntityEcaHandler<T> entityEcaHandler, Map<String, List<T>> eventMap) {
            this.entityEcaHandler = entityEcaHandler;
            this.eventMap = eventMap;
        }

        protected void evalRules(String event, String currentOperation, GenericEntity value, boolean isError) throws GenericEntityException {
            if (entityEcaHandler == null) {
                return;
            }
            entityEcaHandler.evalRules(currentOperation, eventMap, event, value, isError);
        }
    }

    protected EntityEcaRuleRunner<?> getEcaRuleRunner(String entityName) {
        if (this.testRollbackInProgress) {
            return createEntityEcaRuleRunner(null, null);
        }
        return createEntityEcaRuleRunner(getEntityEcaHandler(), entityName);
    }

    protected static <T> EntityEcaRuleRunner<T> createEntityEcaRuleRunner(EntityEcaHandler<T> entityEcaHandler, String entityName) {
        return new EntityEcaRuleRunner<>(entityEcaHandler, entityEcaHandler != null ? entityEcaHandler.getEntityEventMap(entityName) : null);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#setEntityEcaHandler(org.apache.ofbiz.entity.eca.EntityEcaHandler)
     */
    @Override
    public <T> void setEntityEcaHandler(EntityEcaHandler<T> entityEcaHandler) {
        this.entityEcaHandler.set(new ConstantFuture<>(entityEcaHandler));
        this.warnNoEcaHandler = false;
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#getEntityEcaHandler()
     */
    @Override
    public <T> EntityEcaHandler<T> getEntityEcaHandler() {
        Future<EntityEcaHandler<?>> future = this.entityEcaHandler.get();
        try {
            return UtilGenerics.cast(future != null ? future.get() : null);
        } catch (ExecutionException | InterruptedException e) {
            Debug.logError(e, "Could not fetch EntityEcaHandler from the asynchronous instantiation", MODULE);
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#getNextSeqId(java.lang.String)
     */
    @Override
    public String getNextSeqId(String seqName) {
        return this.getNextSeqId(seqName, 1);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#getNextSeqId(java.lang.String, long)
     */
    @Override
    public String getNextSeqId(String seqName, long staggerMax) {
        Long nextSeqLong = this.getNextSeqIdLong(seqName, staggerMax);

        if (nextSeqLong == null) {
            // NOTE: the getNextSeqIdLong method SHOULD throw a runtime exception when no sequence value is found, which means we should never see it get here
            throw new IllegalArgumentException("Could not get next sequenced ID for sequence name: " + seqName);
        }

        if (UtilValidate.isNotEmpty(this.delegatorInfo.getSequencedIdPrefix())) {
            return this.delegatorInfo.getSequencedIdPrefix() + nextSeqLong.toString();
        }
        return nextSeqLong.toString();
    }

    @Override
    public Long getNextSeqIdLong(String seqName) {
        return this.getNextSeqIdLong(seqName, 1);
    }

    @Override
    public Long getNextSeqIdLong(String seqName, long staggerMax) {
        try {
            SequenceUtil sequencer = this.AtomicRefSequencer.get();
            if (sequencer == null) {
                ModelEntity seqEntity = this.getModelEntity("SequenceValueItem");
                sequencer = new SequenceUtil(this.getEntityHelperInfo("SequenceValueItem"), seqEntity, "seqName", "seqId");
                if (!AtomicRefSequencer.compareAndSet(null, sequencer)) {
                    sequencer = this.AtomicRefSequencer.get();
                }
            }
            ModelEntity seqModelEntity = null;
            try {
                seqModelEntity = getModelReader().getModelEntity(seqName);
            } catch (GenericEntityException e) {
                Debug.logInfo("Entity definition not found for sequence name " + seqName, MODULE);
            }
            Long newSeqId = sequencer == null ? null : sequencer.getNextSeqId(seqName, staggerMax, seqModelEntity);
            return newSeqId;
        } catch (Exception e) {
            String errMsg = "Failure in getNextSeqIdLong operation for seqName [" + seqName + "]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, MODULE);
            throw new GeneralRuntimeException(errMsg, e);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#setSequencer(org.apache.ofbiz.entity.util.SequenceUtil)
     */
    @Override
    public void setSequencer(SequenceUtil sequencer) {
        this.AtomicRefSequencer.set(sequencer);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#refreshSequencer()
     */
    @Override
    public void refreshSequencer() {
        this.AtomicRefSequencer.set(null);
    }


    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#setNextSubSeqId(org.apache.ofbiz.entity.GenericValue, java.lang.String, int, int)
     */
    @Override
    public void setNextSubSeqId(GenericValue value, String seqFieldName, int numericPadding, int incrementBy) {
        if (value != null && UtilValidate.isEmpty(value.getString(seqFieldName))) {
            String sequencedIdPrefix = this.delegatorInfo.getSequencedIdPrefix();

            value.remove(seqFieldName);
            GenericValue lookupValue = this.makeValue(value.getEntityName());
            lookupValue.setPKFields(value);

            boolean beganTransaction = false;
            try {
                if (alwaysUseTransaction) {
                    beganTransaction = TransactionUtil.begin();
                }

                // get values in whatever order, we will go through all of them to find the highest value
                List<GenericValue> allValues = this.findByAnd(value.getEntityName(), lookupValue, null, false);
                Integer highestSeqVal = null;
                for (GenericValue curValue: allValues) {
                    String currentSeqId = curValue.getString(seqFieldName);
                    if (currentSeqId != null) {
                        if (UtilValidate.isNotEmpty(sequencedIdPrefix)) {
                            if (currentSeqId.startsWith(sequencedIdPrefix)) {
                                currentSeqId = currentSeqId.substring(sequencedIdPrefix.length());
                            } else {
                                continue;
                            }
                        }
                        try {
                            int seqVal = Integer.parseInt(currentSeqId);
                            if (highestSeqVal == null || seqVal > highestSeqVal) {
                                highestSeqVal = seqVal;
                            }
                        } catch (NumberFormatException e) {
                            Debug.logWarning("Error in make-next-seq-id converting SeqId [" + currentSeqId + "] in field: " + seqFieldName + " from entity: " + value.getEntityName() + " to a number: " + e.toString(), MODULE);
                        }
                    }
                }

                int seqValToUse = (highestSeqVal == null ? 1 : highestSeqVal + incrementBy);
                String newSeqId = sequencedIdPrefix + UtilFormatOut.formatPaddedNumber(seqValToUse, numericPadding);
                value.set(seqFieldName, newSeqId);

                // only commit the transaction if we started one...
                TransactionUtil.commit(beganTransaction);
            } catch (GenericEntityException e) {
                String errMsg = "Failure in setNextSubSeqId operation for entity [" + value.getEntityName() + "]: " + e.toString() + ". Rolling back transaction.";
                Debug.logError(e, errMsg, MODULE);
                try {
                    // only rollback the transaction if we started one...
                    TransactionUtil.rollback(beganTransaction, errMsg, e);
                } catch (GenericEntityException e2) {
                    Debug.logError(e2, "[GenericDelegator] Could not rollback transaction: " + e2.toString(), MODULE);
                }
                Debug.logError(e, "Error making next seqId", MODULE);
            }
        }
    }

    @Override
    public Object encryptFieldValue(String entityName, ModelField.EncryptMethod encryptMethod, Object fieldValue) throws EntityCryptoException {
        if (encryptMethod == null) {
            encryptMethod = ModelField.EncryptMethod.TRUE;
        }
        if (fieldValue != null) {
            if (fieldValue instanceof String && UtilValidate.isEmpty(fieldValue)) {
                return fieldValue;
            }
            return this.crypto.encrypt(entityName, encryptMethod, fieldValue);
        }
        return fieldValue;
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#encryptFieldValue(java.lang.String, java.lang.Object)
     */
    @Override
    public Object decryptFieldValue(String entityName, ModelField.EncryptMethod encryptMethod, String encValue) throws EntityCryptoException {
        if (UtilValidate.isNotEmpty(encValue)) {
            return this.crypto.decrypt(entityName, encryptMethod, encValue);
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#setEntityCrypto(org.apache.ofbiz.entity.util.EntityCrypto)
     */
    @Override
    public void setEntityCrypto(EntityCrypto crypto) {
        this.crypto = crypto;
    }

    protected void absorbList(List<GenericValue> lst) {
        if (lst == null) {
            return;
        }
        for (GenericValue value: lst) {
            value.setDelegator(this);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#getCache()
     */
    @Override
    public Cache getCache() {
        return cache;
    }

    protected void createEntityAuditLogAll(GenericValue value, boolean isUpdate, boolean isRemove) throws GenericEntityException {
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        for (ModelField mf: value.getModelEntity().getFieldsUnmodifiable()) {
            if (mf.getEnableAuditLog()) {
                createEntityAuditLogSingle(value, mf, isUpdate, isRemove, nowTimestamp);
            }
        }
    }

    protected void createEntityAuditLogSingle(GenericValue value, ModelField mf, boolean isUpdate, boolean isRemove, Timestamp nowTimestamp) throws GenericEntityException {
        if (value == null || mf == null || !mf.getEnableAuditLog() || this.testRollbackInProgress) {
            return;
        }

        String newValueText = null;
        String oldValueText = null;

        GenericValue oldGv = null;
        if (isUpdate) {
            // it's an update, get it from the database
            oldGv = this.findOne(value.getEntityName(), value.getPrimaryKey(), false);
        } else if (isRemove) {
            oldGv = value;
        }
        if (oldGv == null) {
            if (isUpdate || isRemove) {
                oldValueText = "[ERROR] Old value not found even though it was an update or remove";
            }
        } else {
            // lookup old value
            Object oldValue = oldGv.get(mf.getName());
            if (oldValue != null) {
                oldValueText = oldValue.toString();
                if (oldValueText.length() > 250) {
                    oldValueText = oldValueText.substring(0, 250);
                }
            }
        }

        if (!isRemove) {
            Object newValue = value.get(mf.getName());
            if (newValue != null) {
                newValueText = newValue.toString();
                if (newValueText.length() > 250) {
                    newValueText = newValueText.substring(0, 250);
                }
            }
        }

        if (!(newValueText == null ? "" : newValueText).equals((oldValueText == null ? "" : oldValueText))) {
            // only save changed values
            GenericValue entityAuditLog = this.makeValue("EntityAuditLog");
            entityAuditLog.set("auditHistorySeqId", this.getNextSeqId("EntityAuditLog"));
            entityAuditLog.set("changedEntityName", value.getEntityName());
            entityAuditLog.set("changedFieldName", mf.getName());

            String pkCombinedValueText = value.getPkShortValueString();
            if (pkCombinedValueText.length() > 250) {
                // uh-oh, the string is too long!
                pkCombinedValueText = pkCombinedValueText.substring(0, 250);
            }
            entityAuditLog.set("pkCombinedValueText", pkCombinedValueText);
            entityAuditLog.set("newValueText", newValueText);
            entityAuditLog.set("oldValueText", oldValueText);
            entityAuditLog.set("changedDate", nowTimestamp);
            entityAuditLog.set("changedByInfo", getCurrentUserIdentifier());
            entityAuditLog.set("changedSessionInfo", getCurrentSessionIdentifier());
            this.create(entityAuditLog);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#cloneDelegator(java.lang.String)
     */
    @Override
    public Delegator cloneDelegator(String delegatorFullName) {
        // creates an exact clone of the delegator; except for the sequencer
        // note that this will not be cached and should be used only when
        // needed to change something for single instance (use).
        GenericDelegator newDelegator = new GenericDelegator();
        newDelegator.modelReader = this.modelReader;
        newDelegator.modelGroupReader = this.modelGroupReader;
        newDelegator.setDelegatorNames(UtilValidate.isNotEmpty(delegatorFullName) ? delegatorFullName : this.delegatorFullName);
        // set the delegatorBaseName to be the same so that configuration settings all work the same as the current
        //   delegator, allowing the new delegatorFullName to not match a delegator name in the entityengine.xml file
        newDelegator.delegatorBaseName = this.delegatorBaseName;
        newDelegator.delegatorInfo = this.delegatorInfo;
        newDelegator.cache = this.cache;
        newDelegator.distributedCacheClear.set(this.distributedCacheClear.get());
        newDelegator.originalDelegatorName = getOriginalDelegatorName();
        newDelegator.entityEcaHandler.set(this.entityEcaHandler.get());
        newDelegator.crypto = this.crypto;
        // In case this delegator is in testMode give it a reference to
        // the rollback list
        newDelegator.testMode = this.testMode;
        testOperationsUpdater.set(newDelegator, this.testOperations);
        // not setting the sequencer so that we have unique sequences.

        return newDelegator;
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#cloneDelegator()
     */
    @Override
    public Delegator cloneDelegator() {
        return this.cloneDelegator(this.delegatorFullName);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#makeTestDelegator(java.lang.String)
     */
    @Override
    public Delegator makeTestDelegator(String delegatorName) {
        GenericDelegator testDelegator = (GenericDelegator) this.cloneDelegator(delegatorName);
        testDelegator.entityEcaHandler.set(null);
        testDelegator.initEntityEcaHandler();
        testDelegator.setTestMode(true);
        return testDelegator;
    }

    private void setTestMode(boolean testMode) {
        this.testMode = testMode;
        if (testMode) {
            testOperationsUpdater.set(this, new LinkedBlockingDeque<TestOperation>());
        } else {
            this.testOperations.clear();
        }
    }

    private void storeForTestRollback(TestOperation testOperation) {
        if (!this.testMode || this.testRollbackInProgress) {
            throw new IllegalStateException("An attempt was made to store a TestOperation during rollback or outside of test mode");
        }
        this.testOperations.add(testOperation);
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#rollback()
     */
    @Override
    public void rollback() {
        if (!this.testMode) {
            Debug.logError("Rollback requested outside of testmode", MODULE);
        }
        this.testMode = false;
        this.testRollbackInProgress = true;
        if (Debug.infoOn()) {
            Debug.logInfo("Rolling back " + testOperations.size() + " entity operations", MODULE);
        }
        while (!this.testOperations.isEmpty()) {
            TestOperation testOperation = this.testOperations.pollLast();
            if (testOperation == null) {
                break;
            }
            try {
                if (testOperation.getOperation().equals(OperationType.INSERT)) {
                    this.removeValue(testOperation.getValue());
                } else if (testOperation.getOperation().equals(OperationType.UPDATE)) {
                    this.store(testOperation.getValue());
                } else if (testOperation.getOperation().equals(OperationType.DELETE)) {
                    this.create(testOperation.getValue());
                }
            } catch (GenericEntityException e) {
                Debug.logWarning(e.toString(), MODULE);
            }
        }
        this.testOperations.clear();
        this.testRollbackInProgress = false;
        this.testMode = true;
    }

    public static final class TestOperation {
        private final OperationType operation;
        private final GenericValue value;

        public TestOperation(OperationType operation, GenericValue value) {
            this.operation = operation;
            this.value = value;
        }

        public OperationType getOperation() {
            return operation;
        }

        public GenericValue getValue() {
            return value;
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ofbiz.entity.Delegator#initDistributedCacheClear()
     */
    @Override
    public void initDistributedCacheClear() {
        // Nothing to do if already assigned: the class loader has already been called, the class instantiated and casted to DistributedCacheClear
        if (this.distributedCacheClear.get() != null) {
            return;
        }

        Callable<DistributedCacheClear> creator = this::createDistributedCacheClear;
        FutureTask<DistributedCacheClear> futureTask = new FutureTask<>(creator);
        if (distributedCacheClear.compareAndSet(null, futureTask)) {
            ExecutionPool.GLOBAL_BATCH.submit(futureTask);
        }
    }

    protected DistributedCacheClear createDistributedCacheClear() {
        // If useDistributedCacheClear is false do nothing: the distributedCacheClear member field with a null value would cause dcc code to do nothing
        if (useDistributedCacheClear()) {
            //time to do some tricks with manual class loading that resolves circular dependencies, like calling services
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            // initialize the distributedCacheClear mechanism
            String distributedCacheClearClassName = this.delegatorInfo.getDistributedCacheClearClassName();

            try {
                Class<?> dccClass = loader.loadClass(distributedCacheClearClassName);
                DistributedCacheClear distributedCacheClear = UtilGenerics.cast(dccClass.getDeclaredConstructor().newInstance());
                distributedCacheClear.setDelegator(this, this.delegatorInfo.getDistributedCacheClearUserLoginId());
                return distributedCacheClear;
            } catch (ReflectiveOperationException e) {
                Debug.logWarning(e, "DistributedCacheClear class with name " + distributedCacheClearClassName + " was not found, distributed cache clearing will be disabled", MODULE);
            } catch (ClassCastException e) {
                Debug.logWarning(e, "DistributedCacheClear class with name " + distributedCacheClearClassName + " does not implement the DistributedCacheClear interface, distributed cache clearing will be disabled", MODULE);
            }
        } else {
            if (Debug.verboseOn()) {
                Debug.logVerbose("Distributed Cache Clear System disabled for delegator [" + delegatorFullName + "]", MODULE);
            }
        }
        return null;
    }

    protected DistributedCacheClear getDistributedCacheClear() {
        Future<DistributedCacheClear> future = this.distributedCacheClear.get();
        try {
            return future != null ? future.get() : null;
        } catch (ExecutionException | InterruptedException e) {
            Debug.logError(e, "Could not fetch DistributedCacheClear from the asynchronous instantiation", MODULE);
        }
        return null;
    }

    @Override
    public boolean useDistributedCacheClear() {
        return this.delegatorInfo.getDistributedCacheClearEnabled();
    }

    @Override
    public String getCurrentSessionIdentifier() {
        List<String> curValList = getSessionIdentifierStack();
        return curValList.size() > 0 ? curValList.get(0) : null;
    }

    @Override
    public String getCurrentUserIdentifier() {
        List<String> curValList = getUserIdentifierStack();
        return curValList.size() > 0 ? curValList.get(0) : null;
    }
}
