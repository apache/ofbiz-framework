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
package org.ofbiz.entity;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralRuntimeException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.cache.CacheLine;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.entity.cache.Cache;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.config.DelegatorInfo;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.ofbiz.entity.datasource.GenericHelper;
import org.ofbiz.entity.datasource.GenericHelperFactory;
import org.ofbiz.entity.eca.EntityEcaHandler;
import org.ofbiz.entity.model.DynamicViewEntity;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelEntityChecker;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.entity.model.ModelFieldType;
import org.ofbiz.entity.model.ModelFieldTypeReader;
import org.ofbiz.entity.model.ModelGroupReader;
import org.ofbiz.entity.model.ModelKeyMap;
import org.ofbiz.entity.model.ModelReader;
import org.ofbiz.entity.model.ModelRelation;
import org.ofbiz.entity.model.ModelViewEntity;
import org.ofbiz.entity.serialize.SerializeException;
import org.ofbiz.entity.serialize.XmlSerializer;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.DistributedCacheClear;
import org.ofbiz.entity.util.EntityCrypto;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.SequenceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Implementation of the <code>GenericDelegator</code> interface. The class
 * contains a reference to a shared instance of <code>DelegatorData</code> -
 * making it possible to have per-thread instances of this class while
 * sharing common delegator data.
 */
public class DelegatorImpl implements Cloneable, GenericDelegator {

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

    private enum OperationType {
        DELETE, INSERT, UPDATE
    }

    public class TestOperation {
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

    /**
     * This flag is only here for lower level technical testing, it shouldn't be
     * user configurable (or at least I don't think so yet); when true all
     * operations without a transaction will be wrapped in one; seems to be
     * necessary for some (all?) XA aware connection pools, and should improve
     * overall stability and consistency
     */
    public static final boolean alwaysUseTransaction = true;

    public static final String module = DelegatorImpl.class.getName();

    protected static <T> EntityEcaRuleRunner<T> createEntityEcaRuleRunner(EntityEcaHandler<T> entityEcaHandler, String entityName) {
        return new EntityEcaRuleRunner<T>(entityEcaHandler, entityEcaHandler != null ? entityEcaHandler.getEntityEventMap(entityName) : null);
    }

    protected final DelegatorData delegatorData;

    protected Locale locale = Locale.getDefault();

    private boolean testMode = false;

    private List<TestOperation> testOperations = null;

    private boolean testRollbackInProgress = false;

    protected String sessionIdentifier = "";

    protected String userIdentifier = "";

    protected DelegatorImpl(DelegatorData delegatorData) {
        this.delegatorData = delegatorData;
        if (!delegatorData.initialized) {
            synchronized (delegatorData) {
                if (delegatorData.initialized) {
                    return;
                }
                // do the entity model check
                List<String> warningList = FastList.newInstance();
                Debug.logImportant("Doing entity definition check...", module);
                try {
                    ModelEntityChecker.checkEntities(this, warningList);
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Error while checking entities: ", module);
                }
                if (warningList.size() > 0) {
                    Debug.logWarning("=-=-=-=-= Found " + warningList.size() + " warnings when checking the entity definitions:", module);
                    for (String warning : warningList) {
                        Debug.logWarning(warning, module);
                    }
                }

                // initialize helpers by group
                Set<String> groupNames = getModelGroupReader().getGroupNames(this.delegatorData.delegatorName);
                Iterator<String> groups = UtilMisc.toIterator(groupNames);
                while (groups != null && groups.hasNext()) {
                    String groupName = groups.next();
                    String helperName = this.getGroupHelperName(groupName);

                    if (Debug.infoOn())
                        Debug.logInfo("Delegator \"" + this.delegatorData.delegatorName + "\" initializing helper \"" + helperName + "\" for entity group \"" + groupName + "\".", module);
                    TreeSet<String> helpersDone = new TreeSet<String>();
                    if (helperName != null && helperName.length() > 0) {
                        // make sure each helper is only loaded once
                        if (helpersDone.contains(helperName)) {
                            if (Debug.infoOn())
                                Debug.logInfo("Helper \"" + helperName + "\" already initialized, not re-initializing.", module);
                            continue;
                        }
                        helpersDone.add(helperName);
                        // pre-load field type defs, the return value is ignored
                        ModelFieldTypeReader.getModelFieldTypeReader(helperName);
                        // get the helper and if configured, do the datasource check
                        GenericHelper helper = GenericHelperFactory.getHelper(helperName);

                        DatasourceInfo datasourceInfo = EntityConfigUtil.getDatasourceInfo(helperName);
                        if (datasourceInfo.checkOnStart) {
                            if (Debug.infoOn())
                                Debug.logInfo("Doing database check as requested in entityengine.xml with addMissing=" + datasourceInfo.addMissingOnStart, module);
                            try {
                                helper.checkDataSource(this.getModelEntityMapByGroup(groupName), null, datasourceInfo.addMissingOnStart);
                            } catch (GenericEntityException e) {
                                Debug.logWarning(e, e.getMessage(), module);
                            }
                        }
                    }
                }
                // Let other instances know the shared data is ready to use
                this.delegatorData.initialized = true;
                // setup the crypto class
                this.delegatorData.crypto = new EntityCrypto(this);

                // time to do some tricks with manual class loading that resolves
                // circular dependencies, like calling services...
                ClassLoader loader = Thread.currentThread().getContextClassLoader();

                // if useDistributedCacheClear is false do nothing since the
                // distributedCacheClear member field with a null value will cause the
                // dcc code to do nothing
                if (this.delegatorData.delegatorInfo.useDistributedCacheClear) {
                    // initialize the distributedCacheClear mechanism
                    String distributedCacheClearClassName = this.delegatorData.delegatorInfo.distributedCacheClearClassName;

                    try {
                        Class<?> dccClass = loader.loadClass(distributedCacheClearClassName);
                        this.delegatorData.distributedCacheClear = (DistributedCacheClear) dccClass.newInstance();
                        this.delegatorData.distributedCacheClear.setDelegator(this, this.delegatorData.delegatorInfo.distributedCacheClearUserLoginId);
                    } catch (ClassNotFoundException e) {
                        Debug.logWarning(e, "DistributedCacheClear class with name " + distributedCacheClearClassName + " was not found, distributed cache clearing will be disabled", module);
                    } catch (InstantiationException e) {
                        Debug.logWarning(e, "DistributedCacheClear class with name " + distributedCacheClearClassName + " could not be instantiated, distributed cache clearing will be disabled", module);
                    } catch (IllegalAccessException e) {
                        Debug.logWarning(e, "DistributedCacheClear class with name " + distributedCacheClearClassName + " could not be accessed (illegal), distributed cache clearing will be disabled", module);
                    } catch (ClassCastException e) {
                        Debug.logWarning(e, "DistributedCacheClear class with name " + distributedCacheClearClassName + " does not implement the DistributedCacheClear interface, distributed cache clearing will be disabled", module);
                    }
                } else {
                    Debug.logInfo("Distributed Cache Clear System disabled for delegator [" + this.delegatorData.delegatorName + "]", module);
                }

                // setup the Entity ECA Handler
                initEntityEcaHandler();
            }
        }
    }

    protected void absorbList(List<GenericValue> lst) {
        if (lst == null)
            return;
        for (GenericValue value : lst) {
            value.setDelegator(this);
        }
    }

    public void clearAllCacheLinesByDummyPK(Collection<GenericPK> dummyPKs) {
        if (dummyPKs == null)
            return;
        for (GenericEntity entity : dummyPKs) {
            this.clearCacheLineFlexible(entity);
        }
    }

    public void clearAllCacheLinesByValue(Collection<GenericValue> values) {
        if (values == null)
            return;
        for (GenericValue value : values) {
            this.clearCacheLine(value);
        }
    }

    public void clearAllCaches() {
        this.clearAllCaches(true);
    }

    public void clearAllCaches(boolean distribute) {
        this.delegatorData.cache.clear();
        if (distribute && this.delegatorData.distributedCacheClear != null) {
            this.delegatorData.distributedCacheClear.clearAllCaches();
        }
    }

    public void clearCacheLine(GenericPK primaryKey) {
        this.clearCacheLine(primaryKey, true);
    }

    public void clearCacheLine(GenericPK primaryKey, boolean distribute) {
        if (primaryKey == null)
            return;

        // if never cached, then don't bother clearing
        if (primaryKey.getModelEntity().getNeverCache())
            return;

        this.delegatorData.cache.remove(primaryKey);

        if (distribute && this.delegatorData.distributedCacheClear != null) {
            this.delegatorData.distributedCacheClear.distributedClearCacheLine(primaryKey);
        }
    }

    public void clearCacheLine(GenericValue value) {
        this.clearCacheLine(value, true);
    }

    public void clearCacheLine(GenericValue value, boolean distribute) {
        // TODO: make this a bit more intelligent by passing in the operation
        // being done (create, update, remove) so we can not do unnecessary
        // cache clears...
        // for instance:
        // on create don't clear by primary cache (and won't clear original
        // values because there won't be any)
        // on remove don't clear by and for new values, but do for original
        // values

        // Debug.logInfo("running clearCacheLine for value: " + value + ",
        // distribute: " + distribute, module);
        if (value == null)
            return;

        // if never cached, then don't bother clearing
        if (value.getModelEntity().getNeverCache())
            return;

        this.delegatorData.cache.remove(value);

        if (distribute && this.delegatorData.distributedCacheClear != null) {
            this.delegatorData.distributedCacheClear.distributedClearCacheLine(value);
        }
    }

    public void clearCacheLine(String entityName) {
        this.delegatorData.cache.remove(entityName);
    }

    public void clearCacheLine(String entityName, Map<String, ? extends Object> fields) {
        // if no fields passed, do the all cache quickly and return
        if (fields == null) {
            this.delegatorData.cache.remove(entityName);
            return;
        }

        ModelEntity entity = this.getModelEntity(entityName);
        if (entity == null) {
            throw new IllegalArgumentException("[GenericDelegator.clearCacheLine] could not find entity for entityName: " + entityName);
        }
        // if never cached, then don't bother clearing
        if (entity.getNeverCache())
            return;

        GenericValue dummyValue = GenericValue.create(entity, fields);
        dummyValue.setDelegator(this);
        this.clearCacheLineFlexible(dummyValue);
    }

    public void clearCacheLine(String entityName, Object... fields) {
        clearCacheLine(entityName, UtilMisc.<String, Object> toMap(fields));
    }

    public void clearCacheLineByCondition(String entityName, EntityCondition condition) {
        clearCacheLineByCondition(entityName, condition, true);
    }

    public void clearCacheLineByCondition(String entityName, EntityCondition condition, boolean distribute) {
        if (entityName != null) {
            // if never cached, then don't bother clearing
            if (getModelEntity(entityName).getNeverCache())
                return;

            this.delegatorData.cache.remove(entityName, condition);

            if (distribute && this.delegatorData.distributedCacheClear != null) {
                this.delegatorData.distributedCacheClear.distributedClearCacheLineByCondition(entityName, condition);
            }
        }
    }

    public void clearCacheLineFlexible(GenericEntity dummyPK) {
        this.clearCacheLineFlexible(dummyPK, true);
    }

    public void clearCacheLineFlexible(GenericEntity dummyPK, boolean distribute) {
        if (dummyPK != null) {
            // if never cached, then don't bother clearing
            if (dummyPK.getModelEntity().getNeverCache())
                return;

            this.delegatorData.cache.remove(dummyPK);

            if (distribute && this.delegatorData.distributedCacheClear != null) {
                this.delegatorData.distributedCacheClear.distributedClearCacheLineFlexible(dummyPK);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void clearCacheValues(UtilCache cache, String entityName, EntityCondition condition) {
        Iterator iterator = cache.cacheLineTable.values().iterator();
        while (iterator.hasNext()) {
            CacheLine line = (CacheLine) iterator.next();
            GenericValue value = (GenericValue) line.getValue();
            if (value != null && value.getEntityName().equals(entityName) && condition.entityMatches(value)) {
                iterator.remove();
            }
        }
    }

    @Override
    protected Object clone() {
        return this.cloneDelegator(this.delegatorData.delegatorName);
    }

    public GenericDelegator cloneDelegator() {
        return this.cloneDelegator(this.delegatorData.delegatorName);
    }

    public GenericDelegator cloneDelegator(String delegatorName) {
        // creates an exact clone of the delegator; except for the sequencer
        // note that this will not be cached and should be used only when
        // needed to change something for single instance (use).
        DelegatorImpl newDelegator = new DelegatorImpl((DelegatorData) this.delegatorData.clone());
        newDelegator.delegatorData.delegatorName = delegatorName;
        // In case this delegator is in testMode give it a reference to
        // the rollback list
        newDelegator.testOperations = this.testOperations;
        // not setting the sequencer so that we have unique sequences.
        newDelegator.delegatorData.sequencer = null;
        return newDelegator;
    }

    public GenericValue create(GenericPK primaryKey) throws GenericEntityException {
        return this.create(primaryKey, true);
    }

    public GenericValue create(GenericPK primaryKey, boolean doCacheClear) throws GenericEntityException {
        if (primaryKey == null) {
            throw new GenericEntityException("Cannot create from a null primaryKey");
        }

        return this.create(GenericValue.create(primaryKey), doCacheClear);
    }

    public GenericValue create(GenericValue value) throws GenericEntityException {
        return this.create(value, true);
    }

    public GenericValue create(GenericValue value, boolean doCacheClear) throws GenericEntityException {
        boolean beganTransaction = false;
        try {
            if (alwaysUseTransaction) {
                beganTransaction = TransactionUtil.begin();
            }

            EntityEcaRuleRunner<?> ecaRunner = this.getEcaRuleRunner(value.getEntityName());
            ecaRunner.evalRules(EntityEcaHandler.EV_VALIDATE, EntityEcaHandler.OP_CREATE, value, false);

            if (value == null) {
                throw new GenericEntityException("Cannot create a null value");
            }
            GenericHelper helper = getEntityHelper(value.getEntityName());

            ecaRunner.evalRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_CREATE, value, false);

            value.setDelegator(this);
            this.encryptFields(value);

            // if audit log on for any fields, save new value with no old value
            // because it's a create
            if (value != null && value.getModelEntity().getHasFieldWithAuditLog()) {
                createEntityAuditLogAll(value, false, false);
            }

            value = helper.create(value);

            if (testMode) {
                storeForTestRollback(new TestOperation(OperationType.INSERT, value));
            }
            if (value != null) {
                value.setDelegator(this);
                if (value.lockEnabled()) {
                    refresh(value, doCacheClear);
                } else {
                    if (doCacheClear) {
                        ecaRunner.evalRules(EntityEcaHandler.EV_CACHE_CLEAR, EntityEcaHandler.OP_CREATE, value, false);
                        this.clearCacheLine(value);
                    }
                }
            }

            ecaRunner.evalRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_CREATE, value, false);

            return value;
        } catch (GenericEntityException e) {
            String errMsg = "Failure in create operation for entity [" + value.getEntityName() + "]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, module);
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, errMsg, e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "[GenericDelegator] Could not rollback transaction: " + e2.toString(), module);
            }
            // after rolling back, rethrow the exception
            throw e;
        } finally {
            // only commit the transaction if we started one... this will throw
            // an exception if it fails
            TransactionUtil.commit(beganTransaction);
        }
    }

    public GenericValue create(String entityName, Map<String, ? extends Object> fields) throws GenericEntityException {
        if (entityName == null || fields == null) {
            return null;
        }
        ModelEntity entity = this.getModelReader().getModelEntity(entityName);
        GenericValue genericValue = GenericValue.create(entity, fields);

        return this.create(genericValue, true);
    }

    public GenericValue create(String entityName, Object... fields) throws GenericEntityException {
        return create(entityName, UtilMisc.<String, Object> toMap(fields));
    }

    protected void createEntityAuditLogAll(GenericValue value, boolean isUpdate, boolean isRemove) throws GenericEntityException {
        for (ModelField mf : value.getModelEntity().getFieldsUnmodifiable()) {
            if (mf.getEnableAuditLog()) {
                createEntityAuditLogSingle(value, mf, isUpdate, isRemove);
            }
        }
    }

    protected void createEntityAuditLogSingle(GenericValue value, ModelField mf, boolean isUpdate, boolean isRemove) throws GenericEntityException {
        if (value == null || mf == null || !mf.getEnableAuditLog() || this.testRollbackInProgress) {
            return;
        }

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

        GenericValue oldGv = null;
        if (isUpdate) {
            // it's an update, get it from the database
            oldGv = this.findOne(value.getEntityName(), value.getPrimaryKey(), false);
        } else if (isRemove) {
            oldGv = value;
        }
        if (oldGv == null) {
            if (isUpdate || isRemove) {
                entityAuditLog.set("oldValueText", "[ERROR] Old value not found even though it was an update or remove");
            }
        } else {
            // lookup old value
            String oldValueText = null;
            Object oldValue = oldGv.get(mf.getName());
            if (oldValue != null) {
                oldValueText = oldValue.toString();
                if (oldValueText.length() > 250) {
                    oldValueText = oldValueText.substring(0, 250);
                }
            }
            entityAuditLog.set("oldValueText", oldValueText);
        }

        if (!isRemove) {
            String newValueText = null;
            Object newValue = value.get(mf.getName());
            if (newValue != null) {
                newValueText = newValue.toString();
                if (newValueText.length() > 250) {
                    newValueText = newValueText.substring(0, 250);
                }
            }
            entityAuditLog.set("newValueText", newValueText);
        }

        entityAuditLog.set("changedDate", UtilDateTime.nowTimestamp());
        entityAuditLog.set("changedByInfo", this.userIdentifier);
        entityAuditLog.set("changedSessionInfo", this.sessionIdentifier);

        this.create(entityAuditLog);
    }

    public GenericValue createOrStore(GenericValue value) throws GenericEntityException {
        return createOrStore(value, true);
    }

    public GenericValue createOrStore(GenericValue value, boolean doCacheClear) throws GenericEntityException {
        boolean beganTransaction = false;
        try {
            if (alwaysUseTransaction) {
                beganTransaction = TransactionUtil.begin();
            }

            GenericValue checkValue = this.findOne(value.getEntityName(), value.getPrimaryKey(), false);
            if (checkValue != null) {
                this.store(value, doCacheClear);
            } else {
                this.create(value, doCacheClear);
            }
            if (value.lockEnabled()) {
                this.refresh(value);
            }

            return value;
        } catch (GenericEntityException e) {
            String errMsg = "Failure in createOrStore operation for entity [" + value.getEntityName() + "]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, module);
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, errMsg, e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "[GenericDelegator] Could not rollback transaction: " + e2.toString(), module);
            }
            // after rolling back, rethrow the exception
            throw e;
        } finally {
            // only commit the transaction if we started one... this will throw
            // an exception if it fails
            TransactionUtil.commit(beganTransaction);
        }
    }

    public GenericValue createSetNextSeqId(GenericValue value) throws GenericEntityException {
        boolean doCacheClear = true;

        GenericHelper helper = getEntityHelper(value.getEntityName());
        // just make sure it is this delegator...
        value.setDelegator(this);
        // this will throw an IllegalArgumentException if the entity for the
        // value does not have one pk field, or if it already has a value set
        // for the one pk field
        value.setNextSeqId();

        boolean beganTransaction = false;
        try {
            if (alwaysUseTransaction) {
                beganTransaction = TransactionUtil.begin();
            }

            EntityEcaRuleRunner<?> ecaRunner = this.getEcaRuleRunner(value.getEntityName());
            ecaRunner.evalRules(EntityEcaHandler.EV_VALIDATE, EntityEcaHandler.OP_CREATE, value, false);

            if (value == null) {
                throw new GenericEntityException("Cannot create a null value");
            }

            ecaRunner.evalRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_CREATE, value, false);

            value.setDelegator(this);
            this.encryptFields(value);

            // if audit log on for any fields, save new value with no old value
            // because it's a create
            if (value != null && value.getModelEntity().getHasFieldWithAuditLog()) {
                createEntityAuditLogAll(value, false, false);
            }

            try {
                value = helper.create(value);

                if (testMode) {
                    storeForTestRollback(new TestOperation(OperationType.INSERT, value));
                }
            } catch (GenericEntityException e) {
                // see if this was caused by an existing record before resetting
                // the sequencer and trying again
                // NOTE: use the helper directly so ECA rules, etc won't be run
                GenericValue existingValue = helper.findByPrimaryKey(value.getPrimaryKey());
                if (existingValue == null) {
                    throw e;
                } else {
                    Debug.logInfo("Error creating entity record with a sequenced value [" + value.getPrimaryKey() + "], trying again about to refresh bank for entity [" + value.getEntityName() + "]", module);

                    // found an existing value... was probably a duplicate key,
                    // so clean things up and try again
                    this.delegatorData.sequencer.forceBankRefresh(value.getEntityName(), 1);

                    value.setNextSeqId();
                    value = helper.create(value);
                    Debug.logInfo("Successfully created new entity record on retry with a sequenced value [" + value.getPrimaryKey() + "], after getting refreshed bank for entity [" + value.getEntityName() + "]", module);

                    if (testMode) {
                        storeForTestRollback(new TestOperation(OperationType.INSERT, value));
                    }
                }
            }

            if (value != null) {
                value.setDelegator(this);
                if (value.lockEnabled()) {
                    refresh(value, doCacheClear);
                } else {
                    if (doCacheClear) {
                        ecaRunner.evalRules(EntityEcaHandler.EV_CACHE_CLEAR, EntityEcaHandler.OP_CREATE, value, false);
                        this.clearCacheLine(value);
                    }
                }
            }

            ecaRunner.evalRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_CREATE, value, false);

            return value;
        } catch (GenericEntityException e) {
            String errMsg = "Failure in create operation for entity [" + value.getEntityName() + "]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, module);
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, errMsg, e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "[GenericDelegator] Could not rollback transaction: " + e2.toString(), module);
            }
            // after rolling back, rethrow the exception
            throw e;
        } finally {
            // only commit the transaction if we started one... this will throw
            // an exception if it fails
            TransactionUtil.commit(beganTransaction);
        }
    }

    public GenericValue createSingle(String entityName, Object singlePkValue) throws GenericEntityException {
        if (entityName == null || singlePkValue == null) {
            return null;
        }
        ModelEntity entity = this.getModelReader().getModelEntity(entityName);
        GenericValue genericValue = GenericValue.create(entity, singlePkValue);

        return this.create(genericValue, true);
    }

    public void decryptFields(GenericEntity entity) throws GenericEntityException {
        ModelEntity model = entity.getModelEntity();
        String entityName = model.getEntityName();

        Iterator<ModelField> i = model.getFieldsIterator();
        while (i.hasNext()) {
            ModelField field = i.next();
            if (field.getEncrypt()) {
                String keyName = entityName;
                if (model instanceof ModelViewEntity) {
                    ModelViewEntity modelView = (ModelViewEntity) model;
                    keyName = modelView.getAliasedEntity(modelView.getAlias(field.getName()).getEntityAlias(), this.delegatorData.modelReader).getEntityName();
                }

                String encHex = (String) entity.get(field.getName());
                if (UtilValidate.isNotEmpty(encHex)) {
                    try {
                        entity.dangerousSetNoCheckButFast(field, this.delegatorData.crypto.decrypt(keyName, encHex));
                    } catch (EntityCryptoException e) {
                        // not fatal -- allow returning of the encrypted value
                        Debug.logWarning(e, "Problem decrypting field [" + entityName + " / " + field.getName() + "]", module);
                    }
                }
            }
        }
    }

    public void decryptFields(List<? extends GenericEntity> entities) throws GenericEntityException {
        if (entities != null) {
            for (GenericEntity entity : entities) {
                this.decryptFields(entity);
            }
        }
    }

    public void encryptFields(GenericEntity entity) throws GenericEntityException {
        ModelEntity model = entity.getModelEntity();
        String entityName = model.getEntityName();

        Iterator<ModelField> i = model.getFieldsIterator();
        while (i.hasNext()) {
            ModelField field = i.next();
            if (field.getEncrypt()) {
                Object obj = entity.get(field.getName());
                if (obj != null) {
                    if (obj instanceof String && UtilValidate.isEmpty((String) obj)) {
                        continue;
                    }
                    entity.dangerousSetNoCheckButFast(field, this.encryptFieldValue(entityName, obj));
                }
            }
        }
    }

    public void encryptFields(List<? extends GenericEntity> entities) throws GenericEntityException {
        if (entities != null) {
            for (GenericEntity entity : entities) {
                this.encryptFields(entity);
            }
        }
    }

    public Object encryptFieldValue(String entityName, Object fieldValue) throws EntityCryptoException {
        if (fieldValue != null) {
            if (fieldValue instanceof String && UtilValidate.isEmpty((String) fieldValue)) {
                return fieldValue;
            }
            return this.delegatorData.crypto.encrypt(entityName, fieldValue);
        }
        return fieldValue;
    }

    public EntityListIterator find(String entityName, EntityCondition whereEntityCondition, EntityCondition havingEntityCondition, Set<String> fieldsToSelect, List<String> orderBy, EntityFindOptions findOptions) throws GenericEntityException {

        // if there is no transaction throw an exception, we don't want to
        // create a transaction here since closing it would mess up the ELI
        if (!TransactionUtil.isTransactionInPlace()) {
            // throw new GenericEntityException("ERROR: Cannot do a find that
            // returns an EntityListIterator with no transaction in place. Wrap
            // this call in a transaction.");

            // throwing an exception is a little harsh for now, just display a
            // really big error message since we want to get all of these
            // fixed...
            Exception newE = new Exception("Stack Trace");
            Debug.logError(newE, "ERROR: Cannot do a find that returns an EntityListIterator with no transaction in place. Wrap this call in a transaction.", module);
        }

        ModelEntity modelEntity = getModelReader().getModelEntity(entityName);
        GenericValue dummyValue = GenericValue.create(modelEntity);
        EntityEcaRuleRunner<?> ecaRunner = this.getEcaRuleRunner(modelEntity.getEntityName());
        ecaRunner.evalRules(EntityEcaHandler.EV_VALIDATE, EntityEcaHandler.OP_FIND, dummyValue, false);

        if (whereEntityCondition != null) {
            whereEntityCondition.checkCondition(modelEntity);
            whereEntityCondition.encryptConditionFields(modelEntity, this);
        }
        if (havingEntityCondition != null) {
            havingEntityCondition.checkCondition(modelEntity);
            havingEntityCondition.encryptConditionFields(modelEntity, this);
        }

        ecaRunner.evalRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_FIND, dummyValue, false);
        GenericHelper helper = getEntityHelper(modelEntity.getEntityName());
        EntityListIterator eli = helper.findListIteratorByCondition(modelEntity, whereEntityCondition, havingEntityCondition, fieldsToSelect, orderBy, findOptions);
        eli.setDelegator(this);

        ecaRunner.evalRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_FIND, dummyValue, false);
        return eli;
    }

    public List<GenericValue> findAll(String entityName) throws GenericEntityException {
        return this.findList(entityName, null, null, null, null, false);
    }

    public List<GenericValue> findAll(String entityName, List<String> orderBy) throws GenericEntityException {
        return this.findList(entityName, null, null, orderBy, null, false);
    }

    public List<GenericValue> findAll(String entityName, String... orderBy) throws GenericEntityException {
        return findList(entityName, null, null, Arrays.asList(orderBy), null, false);
    }

    public List<GenericValue> findAllByPrimaryKeys(Collection<GenericPK> primaryKeys) throws GenericEntityException {
        boolean beganTransaction = false;
        try {
            if (alwaysUseTransaction) {
                beganTransaction = TransactionUtil.begin();
            }

            // TODO: add eca eval calls
            // TODO: maybe this should use the internal findBy methods
            if (primaryKeys == null)
                return null;
            List<GenericValue> results = FastList.newInstance();

            // from the delegator level this is complicated because different
            // GenericPK
            // objects in the list may correspond to different helpers
            Map<String, List<GenericPK>> pksPerHelper = FastMap.newInstance();
            for (GenericPK curPK : primaryKeys) {
                String helperName = this.getEntityHelperName(curPK.getEntityName());
                List<GenericPK> pks = pksPerHelper.get(helperName);

                if (pks == null) {
                    pks = FastList.newInstance();
                    pksPerHelper.put(helperName, pks);
                }
                pks.add(curPK);
            }

            for (Map.Entry<String, List<GenericPK>> curEntry : pksPerHelper.entrySet()) {
                String helperName = curEntry.getKey();
                GenericHelper helper = GenericHelperFactory.getHelper(helperName);
                List<GenericValue> values = helper.findAllByPrimaryKeys(curEntry.getValue());

                results.addAll(values);
            }

            this.decryptFields(results);
            return results;
        } catch (GenericEntityException e) {
            String errMsg = "Failure in findAllByPrimaryKeys operation, rolling back transaction";
            Debug.logError(e, errMsg, module);
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, errMsg, e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "[GenericDelegator] Could not rollback transaction: " + e2.toString(), module);
            }
            // after rolling back, rethrow the exception
            throw e;
        } finally {
            // only commit the transaction if we started one... this will throw
            // an exception if it fails
            TransactionUtil.commit(beganTransaction);
        }
    }

    public List<GenericValue> findAllByPrimaryKeysCache(Collection<GenericPK> primaryKeys) throws GenericEntityException {
        boolean beganTransaction = false;
        try {
            if (alwaysUseTransaction) {
                beganTransaction = TransactionUtil.begin();
            }

            // TODO: add eca eval calls
            // TODO: maybe this should use the internal findBy methods
            if (primaryKeys == null)
                return null;
            List<GenericValue> results = FastList.newInstance();

            // from the delegator level this is complicated because different
            // GenericPK
            // objects in the list may correspond to different helpers
            Map<String, List<GenericPK>> pksPerHelper = FastMap.newInstance();
            for (GenericPK curPK : primaryKeys) {
                GenericValue value = this.getFromPrimaryKeyCache(curPK);

                if (value != null) {
                    // it is in the cache, so just put the cached value in the
                    // results
                    results.add(value);
                } else {
                    // is not in the cache, so put in a list for a call to the
                    // helper
                    String helperName = this.getEntityHelperName(curPK.getEntityName());
                    List<GenericPK> pks = pksPerHelper.get(helperName);

                    if (pks == null) {
                        pks = FastList.newInstance();
                        pksPerHelper.put(helperName, pks);
                    }
                    pks.add(curPK);
                }
            }

            for (Map.Entry<String, List<GenericPK>> curEntry : pksPerHelper.entrySet()) {
                String helperName = curEntry.getKey();
                GenericHelper helper = GenericHelperFactory.getHelper(helperName);
                List<GenericValue> values = helper.findAllByPrimaryKeys(curEntry.getValue());

                this.putAllInPrimaryKeyCache(values);
                results.addAll(values);
            }

            this.decryptFields(results);
            return results;
        } catch (GenericEntityException e) {
            String errMsg = "Failure in findAllByPrimaryKeysCache operation, rolling back transaction";
            Debug.logError(e, errMsg, module);
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, errMsg, e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "[GenericDelegator] Could not rollback transaction: " + e2.toString(), module);
            }
            // after rolling back, rethrow the exception
            throw e;
        } finally {
            // only commit the transaction if we started one... this will throw
            // an exception if it fails
            TransactionUtil.commit(beganTransaction);
        }
    }

    public List<GenericValue> findAllCache(String entityName) throws GenericEntityException {
        return this.findList(entityName, null, null, null, null, true);
    }

    public List<GenericValue> findAllCache(String entityName, List<String> orderBy) throws GenericEntityException {
        return this.findList(entityName, null, null, orderBy, null, true);
    }

    public List<GenericValue> findAllCache(String entityName, String... orderBy) throws GenericEntityException {
        return findList(entityName, null, null, Arrays.asList(orderBy), null, true);
    }

    public <T extends EntityCondition> List<GenericValue> findByAnd(String entityName, List<T> expressions) throws GenericEntityException {
        EntityConditionList<T> ecl = EntityCondition.makeCondition(expressions, EntityOperator.AND);
        return this.findList(entityName, ecl, null, null, null, false);
    }

    public <T extends EntityCondition> List<GenericValue> findByAnd(String entityName, List<T> expressions, List<String> orderBy) throws GenericEntityException {
        EntityConditionList<T> ecl = EntityCondition.makeCondition(expressions, EntityOperator.AND);
        return this.findList(entityName, ecl, null, orderBy, null, false);
    }

    public List<GenericValue> findByAnd(String entityName, Map<String, ? extends Object> fields) throws GenericEntityException {
        EntityCondition ecl = EntityCondition.makeCondition(fields);
        return this.findList(entityName, ecl, null, null, null, false);
    }

    public List<GenericValue> findByAnd(String entityName, Map<String, ? extends Object> fields, List<String> orderBy) throws GenericEntityException {
        EntityCondition ecl = EntityCondition.makeCondition(fields);
        return this.findList(entityName, ecl, null, orderBy, null, false);
    }

    public List<GenericValue> findByAnd(String entityName, Object... fields) throws GenericEntityException {
        return findByAnd(entityName, UtilMisc.<String, Object> toMap(fields));
    }

    public <T extends EntityCondition> List<GenericValue> findByAnd(String entityName, T... expressions) throws GenericEntityException {
        EntityConditionList<T> ecl = EntityCondition.makeCondition(EntityOperator.AND, expressions);
        return this.findList(entityName, ecl, null, null, null, false);
    }

    public List<GenericValue> findByAndCache(String entityName, Map<String, ? extends Object> fields) throws GenericEntityException {
        return this.findList(entityName, EntityCondition.makeCondition(fields), null, null, null, true);
    }

    public List<GenericValue> findByAndCache(String entityName, Map<String, ? extends Object> fields, List<String> orderBy) throws GenericEntityException {
        return this.findList(entityName, EntityCondition.makeCondition(fields), null, orderBy, null, true);
    }

    public List<GenericValue> findByAndCache(String entityName, Object... fields) throws GenericEntityException {
        return this.findByAndCache(entityName, UtilMisc.<String, Object> toMap(fields));
    }

    public List<GenericValue> findByCondition(String entityName, EntityCondition entityCondition, Collection<String> fieldsToSelect, List<String> orderBy) throws GenericEntityException {
        return this.findList(entityName, entityCondition, UtilMisc.toSet(fieldsToSelect), orderBy, null, false);
    }

    public List<GenericValue> findByCondition(String entityName, EntityCondition whereEntityCondition, EntityCondition havingEntityCondition, Collection<String> fieldsToSelect, List<String> orderBy, EntityFindOptions findOptions) throws GenericEntityException {
        boolean beganTransaction = false;
        try {
            if (alwaysUseTransaction) {
                beganTransaction = TransactionUtil.begin();
            }

            EntityListIterator eli = this.find(entityName, whereEntityCondition, havingEntityCondition, UtilMisc.toSet(fieldsToSelect), orderBy, findOptions);
            eli.setDelegator(this);
            List<GenericValue> list = eli.getCompleteList();
            eli.close();

            return list;
        } catch (GenericEntityException e) {
            String errMsg = "Failure in findByCondition operation for entity [" + entityName + "]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, module);
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, errMsg, e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "[GenericDelegator] Could not rollback transaction: " + e2.toString(), module);
            }
            // after rolling back, rethrow the exception
            throw e;
        } finally {
            // only commit the transaction if we started one... this will throw
            // an exception if it fails
            TransactionUtil.commit(beganTransaction);
        }
    }

    public List<GenericValue> findByConditionCache(String entityName, EntityCondition entityCondition, Collection<String> fieldsToSelect, List<String> orderBy) throws GenericEntityException {
        return this.findList(entityName, entityCondition, UtilMisc.collectionToSet(fieldsToSelect), orderBy, null, true);
    }

    public List<GenericValue> findByLike(String entityName, Map<String, ? extends Object> fields) throws GenericEntityException {
        List<EntityExpr> likeExpressions = FastList.newInstance();
        if (fields != null) {
            for (Map.Entry<String, ? extends Object> fieldEntry : fields.entrySet()) {
                likeExpressions.add(EntityCondition.makeCondition(fieldEntry.getKey(), EntityOperator.LIKE, fieldEntry.getValue()));
            }
        }
        EntityConditionList<EntityExpr> ecl = EntityCondition.makeCondition(likeExpressions, EntityOperator.AND);
        return this.findList(entityName, ecl, null, null, null, false);
    }

    public List<GenericValue> findByLike(String entityName, Map<String, ? extends Object> fields, List<String> orderBy) throws GenericEntityException {
        List<EntityExpr> likeExpressions = FastList.newInstance();
        if (fields != null) {
            for (Map.Entry<String, ? extends Object> fieldEntry : fields.entrySet()) {
                likeExpressions.add(EntityCondition.makeCondition(fieldEntry.getKey(), EntityOperator.LIKE, fieldEntry.getValue()));
            }
        }
        EntityConditionList<EntityExpr> ecl = EntityCondition.makeCondition(likeExpressions, EntityOperator.AND);
        return this.findList(entityName, ecl, null, orderBy, null, false);
    }

    public List<GenericValue> findByLike(String entityName, Object... fields) throws GenericEntityException {
        Map<String, ? extends Object> fieldMap = UtilMisc.<String, Object> toMap(fields);
        List<EntityExpr> likeExpressions = FastList.newInstance();
        if (fieldMap != null) {
            for (Map.Entry<String, ? extends Object> fieldEntry : fieldMap.entrySet()) {
                likeExpressions.add(EntityCondition.makeCondition(fieldEntry.getKey(), EntityOperator.LIKE, fieldEntry.getValue()));
            }
        }
        EntityConditionList<EntityExpr> ecl = EntityCondition.makeCondition(likeExpressions, EntityOperator.AND);
        return this.findList(entityName, ecl, null, null, null, false);
    }

    public <T extends EntityCondition> List<GenericValue> findByOr(String entityName, List<T> expressions) throws GenericEntityException {
        EntityConditionList<T> ecl = EntityCondition.makeCondition(expressions, EntityOperator.OR);
        return this.findList(entityName, ecl, null, null, null, false);
    }

    public <T extends EntityCondition> List<GenericValue> findByOr(String entityName, List<T> expressions, List<String> orderBy) throws GenericEntityException {
        EntityConditionList<T> ecl = EntityCondition.makeCondition(expressions, EntityOperator.OR);
        return this.findList(entityName, ecl, null, orderBy, null, false);
    }

    public List<GenericValue> findByOr(String entityName, Map<String, ? extends Object> fields) throws GenericEntityException {
        EntityCondition ecl = EntityCondition.makeCondition(fields, EntityOperator.OR);
        return this.findList(entityName, ecl, null, null, null, false);
    }

    public List<GenericValue> findByOr(String entityName, Map<String, ? extends Object> fields, List<String> orderBy) throws GenericEntityException {
        EntityCondition ecl = EntityCondition.makeCondition(fields, EntityOperator.OR);
        return this.findList(entityName, ecl, null, orderBy, null, false);
    }

    public List<GenericValue> findByOr(String entityName, Object... fields) throws GenericEntityException {
        EntityCondition ecl = EntityCondition.makeCondition(EntityOperator.OR, fields);
        return this.findList(entityName, ecl, null, null, null, false);
    }

    public <T extends EntityCondition> List<GenericValue> findByOr(String entityName, T... expressions) throws GenericEntityException {
        return this.findList(entityName, EntityCondition.makeCondition(EntityOperator.AND, expressions), null, null, null, false);
    }

    public GenericValue findByPrimaryKey(GenericPK primaryKey) throws GenericEntityException {
        return findOne(primaryKey.getEntityName(), primaryKey, false);
    }

    public GenericValue findByPrimaryKey(String entityName, Map<String, ? extends Object> fields) throws GenericEntityException {
        return findOne(entityName, fields, false);
    }

    public GenericValue findByPrimaryKey(String entityName, Object... fields) throws GenericEntityException {
        return findByPrimaryKey(entityName, UtilMisc.<String, Object> toMap(fields));
    }

    public GenericValue findByPrimaryKeyCache(GenericPK primaryKey) throws GenericEntityException {
        return findOne(primaryKey.getEntityName(), primaryKey, true);
    }

    public GenericValue findByPrimaryKeyCache(String entityName, Map<String, ? extends Object> fields) throws GenericEntityException {
        return findOne(entityName, fields, true);
    }

    public GenericValue findByPrimaryKeyCache(String entityName, Object... fields) throws GenericEntityException {
        return findByPrimaryKeyCache(entityName, UtilMisc.<String, Object> toMap(fields));
    }

    public GenericValue findByPrimaryKeyCacheSingle(String entityName, Object singlePkValue) throws GenericEntityException {
        return findOne(entityName, makePKSingle(entityName, singlePkValue), true);
    }

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
                value = null;
            }
            if (value != null)
                value.setDelegator(this);

            ecaRunner.evalRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_FIND, primaryKey, false);
            return value;
        } catch (GenericEntityException e) {
            String errMsg = "Failure in findByPrimaryKeyPartial operation for entity [" + primaryKey.getEntityName() + "]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, module);
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, errMsg, e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "[GenericDelegator] Could not rollback transaction: " + e2.toString(), module);
            }
            // after rolling back, rethrow the exception
            throw e;
        } finally {
            // only commit the transaction if we started one... this will throw
            // an exception if it fails
            TransactionUtil.commit(beganTransaction);
        }
    }

    public GenericValue findByPrimaryKeyPartial(GenericPK primaryKey, String... keys) throws GenericEntityException {
        return findByPrimaryKeyPartial(primaryKey, UtilMisc.makeSetWritable(Arrays.asList(keys)));
    }

    public GenericValue findByPrimaryKeySingle(String entityName, Object singlePkValue) throws GenericEntityException {
        return findOne(entityName, makePKSingle(entityName, singlePkValue), false);
    }

    public long findCountByAnd(String entityName) throws GenericEntityException {
        return findCountByCondition(entityName, null, null, null);
    }

    public long findCountByAnd(String entityName, Map<String, ? extends Object> fields) throws GenericEntityException {
        return findCountByCondition(entityName, EntityCondition.makeCondition(fields, EntityOperator.AND), null, null);
    }

    public long findCountByAnd(String entityName, Object... fields) throws GenericEntityException {
        return findCountByCondition(entityName, EntityCondition.makeCondition(UtilMisc.<String, Object> toMap(fields), EntityOperator.AND), null, null);
    }

    public long findCountByCondition(String entityName, EntityCondition whereEntityCondition, EntityCondition havingEntityCondition) throws GenericEntityException {
        return findCountByCondition(entityName, whereEntityCondition, havingEntityCondition, null);
    }

    public long findCountByCondition(String entityName, EntityCondition whereEntityCondition, EntityCondition havingEntityCondition, EntityFindOptions findOptions) throws GenericEntityException {

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
                whereEntityCondition.encryptConditionFields(modelEntity, this);
            }
            if (havingEntityCondition != null) {
                havingEntityCondition.checkCondition(modelEntity);
                havingEntityCondition.encryptConditionFields(modelEntity, this);
            }

            ecaRunner.evalRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_FIND, dummyValue, false);
            GenericHelper helper = getEntityHelper(modelEntity.getEntityName());
            long count = helper.findCountByCondition(modelEntity, whereEntityCondition, havingEntityCondition, findOptions);

            ecaRunner.evalRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_FIND, dummyValue, false);
            return count;
        } catch (GenericEntityException e) {
            String errMsg = "Failure in findListIteratorByCondition operation for entity [DynamicView]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, module);
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, errMsg, e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "[GenericDelegator] Could not rollback transaction: " + e2.toString(), module);
            }
            // after rolling back, rethrow the exception
            throw e;
        } finally {
            // only commit the transaction if we started one... this will throw
            // an exception if it fails
            TransactionUtil.commit(beganTransaction);
        }
    }

    public List<GenericValue> findList(String entityName, EntityCondition entityCondition, Set<String> fieldsToSelect, List<String> orderBy, EntityFindOptions findOptions, boolean useCache) throws GenericEntityException {

        EntityEcaRuleRunner<?> ecaRunner = null;
        GenericValue dummyValue = null;
        if (useCache) {
            ecaRunner = this.getEcaRuleRunner(entityName);
            ModelEntity modelEntity = getModelReader().getModelEntity(entityName);
            dummyValue = GenericValue.create(modelEntity);
            ecaRunner.evalRules(EntityEcaHandler.EV_CACHE_CHECK, EntityEcaHandler.OP_FIND, dummyValue, false);

            List<GenericValue> cacheList = this.delegatorData.cache.get(entityName, entityCondition, orderBy);
            if (cacheList != null) {
                return cacheList;
            }
        }

        boolean beganTransaction = false;
        try {
            if (alwaysUseTransaction) {
                beganTransaction = TransactionUtil.begin();
            }

            EntityListIterator eli = this.find(entityName, entityCondition, null, fieldsToSelect, orderBy, findOptions);
            eli.setDelegator(this);
            List<GenericValue> list = eli.getCompleteList();
            eli.close();

            if (useCache) {
                ecaRunner.evalRules(EntityEcaHandler.EV_CACHE_PUT, EntityEcaHandler.OP_FIND, dummyValue, false);
                this.delegatorData.cache.put(entityName, entityCondition, orderBy, list);
            }
            return list;
        } catch (GenericEntityException e) {
            String errMsg = "Failure in findByCondition operation for entity [" + entityName + "]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, module);
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, errMsg, e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "[GenericDelegator] Could not rollback transaction: " + e2.toString(), module);
            }
            // after rolling back, rethrow the exception
            throw e;
        } finally {
            // only commit the transaction if we started one... this will throw
            // an exception if it fails
            TransactionUtil.commit(beganTransaction);
        }
    }

    public EntityListIterator findListIteratorByCondition(DynamicViewEntity dynamicViewEntity, EntityCondition whereEntityCondition, EntityCondition havingEntityCondition, Collection<String> fieldsToSelect, List<String> orderBy, EntityFindOptions findOptions) throws GenericEntityException {

        // if there is no transaction throw an exception, we don't want to
        // create a transaction here since closing it would mess up the ELI
        if (!TransactionUtil.isTransactionInPlace()) {
            // throw new GenericEntityException("ERROR: Cannot do a find that
            // returns an EntityListIterator with no transaction in place. Wrap
            // this call in a transaction.");

            // throwing an exception is a little harsh for now, just display a
            // really big error message since we want to get all of these
            // fixed...
            Exception newE = new Exception("Stack Trace");
            Debug.logError(newE, "ERROR: Cannot do a find that returns an EntityListIterator with no transaction in place. Wrap this call in a transaction.", module);
        }

        ModelViewEntity modelViewEntity = dynamicViewEntity.makeModelViewEntity(this);
        if (whereEntityCondition != null)
            whereEntityCondition.checkCondition(modelViewEntity);
        if (havingEntityCondition != null)
            havingEntityCondition.checkCondition(modelViewEntity);

        GenericHelper helper = getEntityHelper(dynamicViewEntity.getOneRealEntityName());
        EntityListIterator eli = helper.findListIteratorByCondition(modelViewEntity, whereEntityCondition, havingEntityCondition, fieldsToSelect, orderBy, findOptions);
        eli.setDelegator(this);
        // TODO: add decrypt fields
        return eli;
    }

    public EntityListIterator findListIteratorByCondition(String entityName, EntityCondition entityCondition, Collection<String> fieldsToSelect, List<String> orderBy) throws GenericEntityException {
        return this.find(entityName, entityCondition, null, UtilMisc.collectionToSet(fieldsToSelect), orderBy, null);
    }

    public EntityListIterator findListIteratorByCondition(String entityName, EntityCondition whereEntityCondition, EntityCondition havingEntityCondition, Collection<String> fieldsToSelect, List<String> orderBy, EntityFindOptions findOptions) throws GenericEntityException {

        return this.find(entityName, whereEntityCondition, havingEntityCondition, UtilMisc.collectionToSet(fieldsToSelect), orderBy, findOptions);
    }

    public GenericValue findOne(String entityName, boolean useCache, Object... fields) throws GenericEntityException {
        return findOne(entityName, UtilMisc.toMap(fields), useCache);
    }

    public GenericValue findOne(String entityName, Map<String, ? extends Object> fields, boolean useCache) throws GenericEntityException {
        GenericPK primaryKey = this.makePK(entityName, fields);
        EntityEcaRuleRunner<?> ecaRunner = this.getEcaRuleRunner(entityName);
        if (useCache) {
            ecaRunner.evalRules(EntityEcaHandler.EV_CACHE_CHECK, EntityEcaHandler.OP_FIND, primaryKey, false);

            GenericValue value = this.getFromPrimaryKeyCache(primaryKey);
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

            if (!primaryKey.isPrimaryKey()) {
                throw new GenericModelException("[GenericDelegator.findOne] Passed primary key is not a valid primary key: " + primaryKey);
            }
            ecaRunner.evalRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_FIND, primaryKey, false);
            try {
                value = helper.findByPrimaryKey(primaryKey);
            } catch (GenericEntityNotFoundException e) {
                value = null;
            }
            if (value != null) {
                value.setDelegator(this);
                this.decryptFields(value);
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
            return value;
        } catch (GenericEntityException e) {
            String errMsg = "Failure in findOne operation for entity [" + entityName + "]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, module);
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, errMsg, e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "[GenericDelegator] Could not rollback transaction: " + e2.toString(), module);
            }
            // after rolling back, rethrow the exception
            throw e;
        } finally {
            // only commit the transaction if we started one... this will throw
            // an exception if it fails
            TransactionUtil.commit(beganTransaction);
        }
    }

    public Cache getCache() {
        return this.delegatorData.cache;
    }

    public GenericDelegator getDelegator(String delegatorName) {
        return DelegatorFactory.getGenericDelegator(delegatorName);
    }

    protected DelegatorInfo getDelegatorInfo() {
        return this.delegatorData.delegatorInfo;
    }

    public String getDelegatorName() {
        return this.delegatorData.delegatorName;
    }

    protected EntityEcaRuleRunner<?> getEcaRuleRunner(String entityName) {
        if (this.testRollbackInProgress)
            return createEntityEcaRuleRunner(null, null);
        return createEntityEcaRuleRunner(this.delegatorData.entityEcaHandler, entityName);
    }

    @SuppressWarnings("unchecked")
    public EntityEcaHandler getEntityEcaHandler() {
        return this.delegatorData.entityEcaHandler;
    }

    public ModelFieldType getEntityFieldType(ModelEntity entity, String type) throws GenericEntityException {
        return this.getModelFieldTypeReader(entity).getModelFieldType(type);
    }

    public Collection<String> getEntityFieldTypeNames(ModelEntity entity) throws GenericEntityException {
        String helperName = getEntityHelperName(entity);

        if (helperName == null || helperName.length() <= 0)
            return null;
        ModelFieldTypeReader modelFieldTypeReader = ModelFieldTypeReader.getModelFieldTypeReader(helperName);

        if (modelFieldTypeReader == null) {
            throw new GenericEntityException("ModelFieldTypeReader not found for entity " + entity.getEntityName() + " with helper name " + helperName);
        }
        return modelFieldTypeReader.getFieldTypeNames();
    }

    public String getEntityGroupName(String entityName) {
        return getModelGroupReader().getEntityGroupName(entityName, getOriginalDelegatorName());
    }

    public GenericHelper getEntityHelper(ModelEntity entity) throws GenericEntityException {
        return getEntityHelper(entity.getEntityName());
    }

    public GenericHelper getEntityHelper(String entityName) throws GenericEntityException {
        String helperName = getEntityHelperName(entityName);

        if (helperName != null && helperName.length() > 0) {
            return GenericHelperFactory.getHelper(helperName);
        } else {
            throw new GenericEntityException("There is no datasource (Helper) configured for the entity-group [" + this.getEntityGroupName(entityName) + "]; was trying to find datesource (helper) for entity [" + entityName + "]");
        }
    }

    public String getEntityHelperName(ModelEntity entity) {
        if (entity == null)
            return null;
        return getEntityHelperName(entity.getEntityName());
    }

    public String getEntityHelperName(String entityName) {
        return this.getGroupHelperName(this.getEntityGroupName(entityName));
    }

    public GenericValue getFromPrimaryKeyCache(GenericPK primaryKey) {
        if (primaryKey == null)
            return null;
        GenericValue value = (GenericValue) this.delegatorData.cache.get(primaryKey);
        if (value == GenericValue.NULL_VALUE) {
            return null;
        }
        return value;
    }

    public String getGroupHelperName(String groupName) {
        return this.getDelegatorInfo().groupMap.get(groupName);
    }

    public Locale getLocale() {
        return this.locale;
    }

    public ModelEntity getModelEntity(String entityName) {
        try {
            return getModelReader().getModelEntity(entityName);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting entity definition from model", module);
            return null;
        }
    }

    public Map<String, ModelEntity> getModelEntityMapByGroup(String groupName) throws GenericEntityException {
        Set<String> entityNameSet = getModelGroupReader().getEntityNamesByGroup(groupName);

        if (this.getDelegatorInfo().defaultGroupName.equals(groupName)) {
            // add all entities with no group name to the Set
            Set<String> allEntityNames = this.getModelReader().getEntityNames();
            for (String entityName : allEntityNames) {
                if (this.getDelegatorInfo().defaultGroupName.equals(getModelGroupReader().getEntityGroupName(entityName, getDelegatorName()))) {
                    entityNameSet.add(entityName);
                }
            }
        }

        Map<String, ModelEntity> entities = FastMap.newInstance();
        if (entityNameSet == null || entityNameSet.size() == 0) {
            return entities;
        }

        int errorCount = 0;
        for (String entityName : entityNameSet) {
            try {
                ModelEntity entity = getModelReader().getModelEntity(entityName);
                if (entity != null) {
                    entities.put(entity.getEntityName(), entity);
                } else {
                    throw new IllegalStateException("Could not find entity with name " + entityName);
                }
            } catch (GenericEntityException ex) {
                errorCount++;
                Debug.logError("Entity [" + entityName + "] named in Entity Group with name " + groupName + " are not defined in any Entity Definition file", module);
            }
        }

        if (errorCount > 0) {
            Debug.logError(errorCount + " entities were named in ModelGroup but not defined in any EntityModel", module);
        }

        return entities;
    }

    public ModelFieldTypeReader getModelFieldTypeReader(ModelEntity entity) {
        String helperName = getEntityHelperName(entity);
        if (helperName == null || helperName.length() <= 0) {
            return null;
        }
        ModelFieldTypeReader modelFieldTypeReader = ModelFieldTypeReader.getModelFieldTypeReader(helperName);
        if (modelFieldTypeReader == null) {
            throw new IllegalArgumentException("ModelFieldTypeReader not found for entity " + entity.getEntityName() + " with helper name " + helperName);
        }
        return modelFieldTypeReader;
    }

    public ModelGroupReader getModelGroupReader() {
        return this.delegatorData.modelGroupReader;
    }

    public ModelReader getModelReader() {
        return this.delegatorData.modelReader;
    }

    public List<GenericValue> getMultiRelation(GenericValue value, String relationNameOne, String relationNameTwo) throws GenericEntityException {
        return getMultiRelation(value, relationNameOne, relationNameTwo, null);
    }

    public List<GenericValue> getMultiRelation(GenericValue value, String relationNameOne, String relationNameTwo, List<String> orderBy) throws GenericEntityException {
        boolean beganTransaction = false;
        try {
            if (alwaysUseTransaction) {
                beganTransaction = TransactionUtil.begin();
            }
            // TODO: add eca eval calls
            // traverse the relationships
            ModelEntity modelEntity = value.getModelEntity();
            ModelRelation modelRelationOne = modelEntity.getRelation(relationNameOne);
            ModelEntity modelEntityOne = getModelEntity(modelRelationOne.getRelEntityName());
            ModelRelation modelRelationTwo = modelEntityOne.getRelation(relationNameTwo);
            ModelEntity modelEntityTwo = getModelEntity(modelRelationTwo.getRelEntityName());

            GenericHelper helper = getEntityHelper(modelEntity);

            return helper.findByMultiRelation(value, modelRelationOne, modelEntityOne, modelRelationTwo, modelEntityTwo, orderBy);
        } catch (GenericEntityException e) {
            String errMsg = "Failure in getMultiRelation operation for entity [" + value.getEntityName() + "]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, module);
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, errMsg, e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "[GenericDelegator] Could not rollback transaction: " + e2.toString(), module);
            }
            // after rolling back, rethrow the exception
            throw e;
        } finally {
            // only commit the transaction if we started one...
            // this will throw an exception if it fails
            TransactionUtil.commit(beganTransaction);
        }
    }

    public String getNextSeqId(String seqName) {
        return this.getNextSeqId(seqName, 1);
    }

    public String getNextSeqId(String seqName, long staggerMax) {
        Long nextSeqLong = this.getNextSeqIdLong(seqName, staggerMax);

        if (nextSeqLong == null) {
            // NOTE: the getNextSeqIdLong method SHOULD throw a runtime
            // exception when no sequence value is found, which means we
            // should never see it get here
            throw new IllegalArgumentException("Could not get next sequenced ID for sequence name: " + seqName);
        }

        if (UtilValidate.isNotEmpty(this.getDelegatorInfo().sequencedIdPrefix)) {
            return this.getDelegatorInfo().sequencedIdPrefix + nextSeqLong.toString();
        } else {
            return nextSeqLong.toString();
        }
    }

    public Long getNextSeqIdLong(String seqName) {
        return this.getNextSeqIdLong(seqName, 1);
    }

    public Long getNextSeqIdLong(String seqName, long staggerMax) {
        boolean beganTransaction = false;
        try {
            if (alwaysUseTransaction) {
                beganTransaction = TransactionUtil.begin();
            }

            if (this.delegatorData.sequencer == null) {
                synchronized (this.delegatorData) {
                    if (this.delegatorData.sequencer == null) {
                        String helperName = this.getEntityHelperName("SequenceValueItem");
                        ModelEntity seqEntity = this.getModelEntity("SequenceValueItem");
                        this.delegatorData.sequencer = new SequenceUtil(helperName, seqEntity, "seqName", "seqId");
                    }
                }
            }

            // might be null, but will usually match the entity name
            ModelEntity seqModelEntity = this.getModelEntity(seqName);

            Long newSeqId = this.delegatorData.sequencer == null ? null : this.delegatorData.sequencer.getNextSeqId(seqName, staggerMax, seqModelEntity);

            return newSeqId;
        } catch (GenericEntityException e) {
            String errMsg = "Failure in getNextSeqIdLong operation for seqName [" + seqName + "]: " + e.toString() + ". Rolling back transaction.";
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, errMsg, e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "[GenericDelegator] Could not rollback transaction: " + e2.toString(), module);
            }
            // rather than logging the problem and returning null, thus hiding the
            // problem, throw an exception
            throw new GeneralRuntimeException(errMsg, e);
        } finally {
            try {
                // only commit the transaction if we started one...
                TransactionUtil.commit(beganTransaction);
            } catch (GenericTransactionException e1) {
                Debug.logError(e1, "[GenericDelegator] Could not commit transaction: " + e1.toString(), module);
            }
        }
    }

    public String getOriginalDelegatorName() {
        return this.delegatorData.originalDelegatorName == null ? this.delegatorData.delegatorName : this.delegatorData.originalDelegatorName;
    }

    @Deprecated
    public List<GenericValue> getRelated(String relationName, GenericValue value) throws GenericEntityException {
        return getRelated(relationName, null, null, value);
    }

    public List<GenericValue> getRelated(String relationName, Map<String, ? extends Object> byAndFields, List<String> orderBy, GenericValue value) throws GenericEntityException {
        ModelEntity modelEntity = value.getModelEntity();
        ModelRelation relation = modelEntity.getRelation(relationName);

        if (relation == null) {
            throw new GenericModelException("Could not find relation for relationName: " + relationName + " for value " + value);
        }

        // put the byAndFields (if not null) into the hash map first,
        // they will be overridden by value's fields if over-specified
        // this is important for security and cleanliness
        Map<String, Object> fields = FastMap.newInstance();
        if (byAndFields != null)
            fields.putAll(byAndFields);
        for (int i = 0; i < relation.getKeyMapsSize(); i++) {
            ModelKeyMap keyMap = relation.getKeyMap(i);
            fields.put(keyMap.getRelFieldName(), value.get(keyMap.getFieldName()));
        }

        return this.findByAnd(relation.getRelEntityName(), fields, orderBy);
    }

    public List<GenericValue> getRelatedByAnd(String relationName, Map<String, ? extends Object> byAndFields, GenericValue value) throws GenericEntityException {
        return this.getRelated(relationName, byAndFields, null, value);
    }

    public List<GenericValue> getRelatedCache(String relationName, GenericValue value) throws GenericEntityException {
        ModelEntity modelEntity = value.getModelEntity();
        ModelRelation relation = modelEntity.getRelation(relationName);

        if (relation == null) {
            throw new GenericModelException("Could not find relation for relationName: " + relationName + " for value " + value);
        }

        Map<String, Object> fields = FastMap.newInstance();
        for (int i = 0; i < relation.getKeyMapsSize(); i++) {
            ModelKeyMap keyMap = relation.getKeyMap(i);
            fields.put(keyMap.getRelFieldName(), value.get(keyMap.getFieldName()));
        }

        return this.findByAndCache(relation.getRelEntityName(), fields, null);
    }

    public GenericPK getRelatedDummyPK(String relationName, Map<String, ? extends Object> byAndFields, GenericValue value) throws GenericEntityException {
        ModelEntity modelEntity = value.getModelEntity();
        ModelRelation relation = modelEntity.getRelation(relationName);

        if (relation == null) {
            throw new GenericModelException("Could not find relation for relationName: " + relationName + " for value " + value);
        }
        ModelEntity relatedEntity = getModelReader().getModelEntity(relation.getRelEntityName());

        // put the byAndFields (if not null) into the hash map first,
        // they will be overridden by value's fields if
        // over-specified this is important for security and
        // cleanliness
        Map<String, Object> fields = FastMap.newInstance();
        if (byAndFields != null)
            fields.putAll(byAndFields);
        for (int i = 0; i < relation.getKeyMapsSize(); i++) {
            ModelKeyMap keyMap = relation.getKeyMap(i);
            fields.put(keyMap.getRelFieldName(), value.get(keyMap.getFieldName()));
        }

        GenericPK dummyPK = GenericPK.create(relatedEntity, fields);
        dummyPK.setDelegator(this);
        return dummyPK;
    }

    public GenericValue getRelatedOne(String relationName, GenericValue value) throws GenericEntityException {
        ModelRelation relation = value.getModelEntity().getRelation(relationName);

        if (relation == null) {
            throw new GenericModelException("Could not find relation for relationName: " + relationName + " for value " + value);
        }
        if (!"one".equals(relation.getType()) && !"one-nofk".equals(relation.getType())) {
            throw new GenericModelException("Relation is not a 'one' or a 'one-nofk' relation: " + relationName + " of entity " + value.getEntityName());
        }

        Map<String, Object> fields = FastMap.newInstance();
        for (int i = 0; i < relation.getKeyMapsSize(); i++) {
            ModelKeyMap keyMap = relation.getKeyMap(i);
            fields.put(keyMap.getRelFieldName(), value.get(keyMap.getFieldName()));
        }

        return this.findByPrimaryKey(relation.getRelEntityName(), fields);
    }

    public GenericValue getRelatedOneCache(String relationName, GenericValue value) throws GenericEntityException {
        ModelEntity modelEntity = value.getModelEntity();
        ModelRelation relation = modelEntity.getRelation(relationName);

        if (relation == null) {
            throw new GenericModelException("Could not find relation for relationName: " + relationName + " for value " + value);
        }
        if (!"one".equals(relation.getType()) && !"one-nofk".equals(relation.getType())) {
            throw new GenericModelException("Relation is not a 'one' or a 'one-nofk' relation: " + relationName + " of entity " + value.getEntityName());
        }

        Map<String, Object> fields = FastMap.newInstance();
        for (int i = 0; i < relation.getKeyMapsSize(); i++) {
            ModelKeyMap keyMap = relation.getKeyMap(i);
            fields.put(keyMap.getRelFieldName(), value.get(keyMap.getFieldName()));
        }

        return this.findByPrimaryKeyCache(relation.getRelEntityName(), fields);
    }

    public List<GenericValue> getRelatedOrderBy(String relationName, List<String> orderBy, GenericValue value) throws GenericEntityException {
        return this.getRelated(relationName, null, orderBy, value);
    }

    protected void initEntityEcaHandler() {
        if (this.delegatorData.delegatorInfo.useEntityEca) {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            // initialize the entity eca handler
            String entityEcaHandlerClassName = this.delegatorData.delegatorInfo.entityEcaHandlerClassName;
            try {
                Class<?> eecahClass = loader.loadClass(entityEcaHandlerClassName);
                this.delegatorData.entityEcaHandler = (EntityEcaHandler<?>) eecahClass.newInstance();
                this.delegatorData.entityEcaHandler.setDelegator(this);
            } catch (ClassNotFoundException e) {
                Debug.logWarning(e, "EntityEcaHandler class with name " + entityEcaHandlerClassName + " was not found, Entity ECA Rules will be disabled", module);
            } catch (InstantiationException e) {
                Debug.logWarning(e, "EntityEcaHandler class with name " + entityEcaHandlerClassName + " could not be instantiated, Entity ECA Rules will be disabled", module);
            } catch (IllegalAccessException e) {
                Debug.logWarning(e, "EntityEcaHandler class with name " + entityEcaHandlerClassName + " could not be accessed (illegal), Entity ECA Rules will be disabled", module);
            } catch (ClassCastException e) {
                Debug.logWarning(e, "EntityEcaHandler class with name " + entityEcaHandlerClassName + " does not implement the EntityEcaHandler interface, Entity ECA Rules will be disabled", module);
            }
        } else {
            Debug.logInfo("Entity ECA Handler disabled for delegator [" + this.delegatorData.delegatorName + "]", module);
        }
    }

    public GenericPK makePK(Element element) {
        GenericValue value = makeValue(element);

        return value.getPrimaryKey();
    }

    public GenericPK makePK(String entityName) {
        return this.makePK(entityName, (Map<String, Object>) null);
    }

    public GenericPK makePK(String entityName, Map<String, ? extends Object> fields) {
        ModelEntity entity = this.getModelEntity(entityName);
        if (entity == null) {
            throw new IllegalArgumentException("[GenericDelegator.makePK] could not find entity for entityName: " + entityName);
        }
        GenericPK pk = GenericPK.create(entity, fields);

        pk.setDelegator(this);
        return pk;
    }

    public GenericPK makePK(String entityName, Object... fields) {
        return makePK(entityName, UtilMisc.<String, Object> toMap(fields));
    }

    public GenericPK makePKSingle(String entityName, Object singlePkValue) {
        ModelEntity entity = this.getModelEntity(entityName);
        if (entity == null) {
            throw new IllegalArgumentException("[GenericDelegator.makePKSingle] could not find entity for entityName: " + entityName);
        }
        GenericPK pk = GenericPK.create(entity, singlePkValue);

        pk.setDelegator(this);
        return pk;
    }

    public GenericDelegator makeTestDelegator(String delegatorName) {
        DelegatorImpl testDelegator = (DelegatorImpl) this.cloneDelegator(delegatorName);
        testDelegator.setTestMode(true);
        return testDelegator;
    }

    public GenericValue makeValidValue(String entityName, Map<String, ? extends Object> fields) {
        ModelEntity entity = this.getModelEntity(entityName);
        if (entity == null) {
            throw new IllegalArgumentException("[GenericDelegator.makeValidValue] could not find entity for entityName: " + entityName);
        }
        GenericValue value = GenericValue.create(entity);
        value.setPKFields(fields, true);
        value.setNonPKFields(fields, true);
        value.setDelegator(this);
        return value;
    }

    public GenericValue makeValidValue(String entityName, Object... fields) {
        return makeValidValue(entityName, UtilMisc.<String, Object> toMap(fields));
    }

    public GenericValue makeValue(Element element) {
        if (element == null)
            return null;
        String entityName = element.getTagName();

        // if a dash or colon is in the tag name, grab what is after it
        if (entityName.indexOf('-') > 0)
            entityName = entityName.substring(entityName.indexOf('-') + 1);
        if (entityName.indexOf(':') > 0)
            entityName = entityName.substring(entityName.indexOf(':') + 1);
        GenericValue value = this.makeValue(entityName);

        ModelEntity modelEntity = value.getModelEntity();

        Iterator<ModelField> modelFields = modelEntity.getFieldsIterator();

        while (modelFields.hasNext()) {
            ModelField modelField = modelFields.next();
            String name = modelField.getName();
            String attr = element.getAttribute(name);

            if (attr != null && attr.length() > 0) {
                value.setString(name, attr);
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

    public GenericValue makeValue(String entityName) {
        ModelEntity entity = this.getModelEntity(entityName);
        if (entity == null) {
            throw new IllegalArgumentException("[GenericDelegator.makeValue] could not find entity for entityName: " + entityName);
        }
        GenericValue value = GenericValue.create(entity);
        value.setDelegator(this);
        return value;
    }

    public GenericValue makeValue(String entityName, Map<String, ? extends Object> fields) {
        ModelEntity entity = this.getModelEntity(entityName);
        if (entity == null) {
            throw new IllegalArgumentException("[GenericDelegator.makeValue] could not find entity for entityName: " + entityName);
        }
        GenericValue value = GenericValue.create(entity, fields);
        value.setDelegator(this);
        return value;
    }

    public GenericValue makeValue(String entityName, Object... fields) {
        return makeValue(entityName, UtilMisc.<String, Object> toMap(fields));
    }

    public List<GenericValue> makeValues(Document document) {
        if (document == null)
            return null;
        List<GenericValue> values = FastList.newInstance();

        Element docElement = document.getDocumentElement();

        if (docElement == null)
            return null;
        if (!"entity-engine-xml".equals(docElement.getTagName())) {
            Debug.logError("[GenericDelegator.makeValues] Root node was not <entity-engine-xml>", module);
            throw new java.lang.IllegalArgumentException("Root node was not <entity-engine-xml>");
        }
        docElement.normalize();
        Node curChild = docElement.getFirstChild();

        if (curChild != null) {
            do {
                if (curChild.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) curChild;
                    GenericValue value = this.makeValue(element);

                    if (value != null)
                        values.add(value);
                }
            } while ((curChild = curChild.getNextSibling()) != null);
        } else {
            Debug.logWarning("[GenericDelegator.makeValues] No child nodes found in document.", module);
        }

        return values;
    }

    public GenericValue makeValueSingle(String entityName, Object singlePkValue) {
        ModelEntity entity = this.getModelEntity(entityName);
        if (entity == null) {
            throw new IllegalArgumentException("[GenericDelegator.makeValue] could not find entity for entityName: " + entityName);
        }
        GenericValue value = GenericValue.create(entity, singlePkValue);
        value.setDelegator(this);
        return value;
    }

    public void putAllInPrimaryKeyCache(List<GenericValue> values) {
        if (values == null)
            return;
        for (GenericValue value : values) {
            this.putInPrimaryKeyCache(value.getPrimaryKey(), value);
        }
    }

    public void putInPrimaryKeyCache(GenericPK primaryKey, GenericValue value) {
        if (primaryKey == null)
            return;

        if (primaryKey.getModelEntity().getNeverCache()) {
            Debug.logWarning("Tried to put a value of the " + value.getEntityName() + " entity in the BY PRIMARY KEY cache but this entity has never-cache set to true, not caching.", module);
            return;
        }

        // before going into the cache, make this value immutable
        value.setImmutable();
        this.delegatorData.cache.put(primaryKey, value);
    }

    // ======= XML Related Methods ========
    public List<GenericValue> readXmlDocument(URL url) throws SAXException, ParserConfigurationException, java.io.IOException {
        if (url == null)
            return null;
        return this.makeValues(UtilXml.readXmlDocument(url, false));
    }

    public void refresh(GenericValue value) throws GenericEntityException {
        this.refresh(value, true);
    }

    public void refresh(GenericValue value, boolean doCacheClear) throws GenericEntityException {
        if (doCacheClear) {
            // always clear cache before the operation
            clearCacheLine(value);
        }
        GenericPK pk = value.getPrimaryKey();
        GenericValue newValue = this.findOne(pk.getEntityName(), pk, false);
        value.refreshFromValue(newValue);
    }

    public void refreshFromCache(GenericValue value) throws GenericEntityException {
        GenericPK pk = value.getPrimaryKey();
        GenericValue newValue = findOne(pk.getEntityName(), pk, true);
        value.refreshFromValue(newValue);
    }

    public void refreshSequencer() {
        this.delegatorData.sequencer = null;
    }

    public int removeAll(List<? extends GenericEntity> dummyPKs) throws GenericEntityException {
        return this.removeAll(dummyPKs, true);
    }

    public int removeAll(List<? extends GenericEntity> dummyPKs, boolean doCacheClear) throws GenericEntityException {
        if (dummyPKs == null) {
            return 0;
        }

        boolean beganTransaction = false;
        int numRemoved = 0;

        try {
            for (GenericEntity value : dummyPKs) {
                if (value.containsPrimaryKey()) {
                    numRemoved += this.removeByPrimaryKey(value.getPrimaryKey(), doCacheClear);
                } else {
                    numRemoved += this.removeByAnd(value.getEntityName(), value.getAllFields(), doCacheClear);
                }
            }

            return numRemoved;
        } catch (GenericEntityException e) {
            String errMsg = "Failure in removeAll operation: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, module);
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, errMsg, e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "[GenericDelegator] Could not rollback transaction: " + e2.toString(), module);
            }
            // after rolling back, rethrow the exception
            throw e;
        } finally {
            // only commit the
                                                    // transaction if we started
                                                    // one... this will throw an
                                                    // exception if it fails
            TransactionUtil.commit(beganTransaction);
        }
    }

    public int removeAll(String entityName) throws GenericEntityException {
        return removeByAnd(entityName, (Map<String, Object>) null);
    }

    public int removeByAnd(String entityName, boolean doCacheClear, Object... fields) throws GenericEntityException {
        return removeByAnd(entityName, UtilMisc.<String, Object> toMap(fields), doCacheClear);
    }

    public int removeByAnd(String entityName, Map<String, ? extends Object> fields) throws GenericEntityException {
        return this.removeByAnd(entityName, fields, true);
    }

    public int removeByAnd(String entityName, Map<String, ? extends Object> fields, boolean doCacheClear) throws GenericEntityException {
        EntityCondition ecl = EntityCondition.makeCondition(fields);
        return removeByCondition(entityName, ecl, doCacheClear);
    }

    public int removeByAnd(String entityName, Object... fields) throws GenericEntityException {
        return removeByAnd(entityName, UtilMisc.<String, Object> toMap(fields));
    }

    public int removeByCondition(String entityName, EntityCondition condition) throws GenericEntityException {
        return this.removeByCondition(entityName, condition, true);
    }

    public int removeByCondition(String entityName, EntityCondition condition, boolean doCacheClear) throws GenericEntityException {
        boolean beganTransaction = false;
        try {
            if (alwaysUseTransaction) {
                beganTransaction = TransactionUtil.begin();
            }

            if (doCacheClear) {
                // always clear cache before the operation
                this.clearCacheLineByCondition(entityName, condition);
            }
            ModelEntity modelEntity = getModelReader().getModelEntity(entityName);
            GenericHelper helper = getEntityHelper(entityName);

            List<GenericValue> removedEntities = null;
            if (testMode) {
                removedEntities = this.findList(entityName, condition, null, null, null, false);
            }

            int rowsAffected = helper.removeByCondition(modelEntity, condition);

            if (testMode) {
                for (GenericValue entity : removedEntities) {
                    storeForTestRollback(new TestOperation(OperationType.DELETE, entity));
                }
            }

            return rowsAffected;
        } catch (GenericEntityException e) {
            String errMsg = "Failure in removeByCondition operation for entity [" + entityName + "]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, module);
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, errMsg, e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "[GenericDelegator] Could not rollback transaction: " + e2.toString(), module);
            }
            // after rolling back, rethrow the exception
            throw e;
        } finally {
            // only commit
                                                                // the
                                                                // transaction
                                                                // if we started
                                                                // one... this
                                                                // will throw an
                                                                // exception if
                                                                // it fails
            TransactionUtil.commit(beganTransaction);
        }
    }

    public int removeByPrimaryKey(GenericPK primaryKey) throws GenericEntityException {
        int retVal = this.removeByPrimaryKey(primaryKey, true);
        return retVal;
    }

    public int removeByPrimaryKey(GenericPK primaryKey, boolean doCacheClear) throws GenericEntityException {
        boolean beganTransaction = false;
        try {
            if (alwaysUseTransaction) {
                beganTransaction = TransactionUtil.begin();
            }

            EntityEcaRuleRunner<?> ecaRunner = this.getEcaRuleRunner(primaryKey.getEntityName());
            ecaRunner.evalRules(EntityEcaHandler.EV_VALIDATE, EntityEcaHandler.OP_REMOVE, primaryKey, false);

            GenericHelper helper = getEntityHelper(primaryKey.getEntityName());

            if (doCacheClear) {
                // always
                                                                    // clear
                                                                    // cache
                                                                    // before
                                                                    // the
                                                                    // operation
                ecaRunner.evalRules(EntityEcaHandler.EV_CACHE_CLEAR, EntityEcaHandler.OP_REMOVE, primaryKey, false);
                this.clearCacheLine(primaryKey);
            }

            ecaRunner.evalRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_REMOVE, primaryKey, false);

            // if audit log on for any fields, save old value before
            // removing so it's still there
            if (primaryKey != null && primaryKey.getModelEntity().getHasFieldWithAuditLog()) {
                createEntityAuditLogAll(this.findOne(primaryKey.getEntityName(), primaryKey, false), true, true);
            }

            GenericValue removedEntity = null;
            if (testMode) {
                removedEntity = this.findOne(primaryKey.entityName, primaryKey, false);
            }
            int num = helper.removeByPrimaryKey(primaryKey);
            this.saveEntitySyncRemoveInfo(primaryKey);

            if (testMode) {
                storeForTestRollback(new TestOperation(OperationType.DELETE, removedEntity));
            }

            ecaRunner.evalRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_REMOVE, primaryKey, false);

            return num;
        } catch (GenericEntityException e) {
            String errMsg = "Failure in removeByPrimaryKey operation for entity [" + primaryKey.getEntityName() + "]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, module);
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, errMsg, e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "[GenericDelegator] Could not rollback transaction: " + e2.toString(), module);
            }
            // after rolling back, rethrow the exception
            throw e;
        } finally {
            // only commit the transaction if we started one... this will throw an
    // exception if it fails
            TransactionUtil.commit(beganTransaction);
        }
    }

    public int removeRelated(String relationName, GenericValue value) throws GenericEntityException {
        return this.removeRelated(relationName, value, true);
    }

    public int removeRelated(String relationName, GenericValue value, boolean doCacheClear) throws GenericEntityException {
        ModelEntity modelEntity = value.getModelEntity();
        ModelRelation relation = modelEntity.getRelation(relationName);

        if (relation == null) {
            throw new GenericModelException("Could not find relation for relationName: " + relationName + " for value " + value);
        }

        Map<String, Object> fields = FastMap.newInstance();
        for (int i = 0; i < relation.getKeyMapsSize(); i++) {
            ModelKeyMap keyMap = relation.getKeyMap(i);
            fields.put(keyMap.getRelFieldName(), value.get(keyMap.getFieldName()));
        }

        return this.removeByAnd(relation.getRelEntityName(), fields, doCacheClear);
    }

    public int removeValue(GenericValue value) throws GenericEntityException {
        return this.removeValue(value, true);
    }

    public int removeValue(GenericValue value, boolean doCacheClear) throws GenericEntityException {
        // NOTE: this does not call the GenericDelegator.removeByPrimaryKey
        // method because it has more information to pass to the ECA rule hander
        boolean beganTransaction = false;
        try {
            if (alwaysUseTransaction) {
                beganTransaction = TransactionUtil.begin();
            }

            EntityEcaRuleRunner<?> ecaRunner = this.getEcaRuleRunner(value.getEntityName());
            ecaRunner.evalRules(EntityEcaHandler.EV_VALIDATE, EntityEcaHandler.OP_REMOVE, value, false);

            GenericHelper helper = getEntityHelper(value.getEntityName());

            if (doCacheClear) {
                ecaRunner.evalRules(EntityEcaHandler.EV_CACHE_CLEAR, EntityEcaHandler.OP_REMOVE, value, false);
                this.clearCacheLine(value);
            }

            ecaRunner.evalRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_REMOVE, value, false);

            // if audit log on for any fields, save old value before actual
            // remove
            if (value != null && value.getModelEntity().getHasFieldWithAuditLog()) {
                createEntityAuditLogAll(value, true, true);
            }

            GenericValue removedValue = null;
            if (testMode) {
                removedValue = this.findOne(value.getEntityName(), value.getPrimaryKey(), false);
            }

            int num = helper.removeByPrimaryKey(value.getPrimaryKey());

            if (testMode) {
                storeForTestRollback(new TestOperation(OperationType.DELETE, removedValue));
            }

            this.saveEntitySyncRemoveInfo(value.getPrimaryKey());

            ecaRunner.evalRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_REMOVE, value, false);

            return num;
        } catch (GenericEntityException e) {
            String errMsg = "Failure in removeValue operation for entity [" + value.getEntityName() + "]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, module);
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, errMsg, e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "[GenericDelegator] Could not rollback transaction: " + e2.toString(), module);
            }
            // after rolling back, rethrow the exception
            throw e;
        } finally {
            // only commit the transaction if we started one... this will throw
            // an exception if it fails
            TransactionUtil.commit(beganTransaction);
        }
    }

    public void rollback() {
        if (!this.testMode) {
            Debug.logError("Rollback requested outside of testmode", module);
        }
        this.testMode = false;
        this.testRollbackInProgress = true;
        synchronized (testOperations) {
            Debug.logInfo("Rolling back " + testOperations.size() + " entity operations", module);
            ListIterator<TestOperation> iterator = this.testOperations.listIterator(this.testOperations.size());
            while (iterator.hasPrevious()) {
                TestOperation testOperation = iterator.previous();
                try {
                    if (testOperation.getOperation().equals(OperationType.INSERT)) {
                        this.removeValue(testOperation.getValue());
                    } else if (testOperation.getOperation().equals(OperationType.UPDATE)) {
                        this.store(testOperation.getValue());
                    } else if (testOperation.getOperation().equals(OperationType.DELETE)) {
                        this.create(testOperation.getValue());
                    }
                } catch (GenericEntityException e) {
                    Debug.logWarning(e.toString(), module);
                }
            }
            this.testOperations.clear();
        }
        this.testRollbackInProgress = false;
        this.testMode = true;
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
        } catch (SerializeException e) {
            Debug.logError(e, "Could not serialize primary key to save EntitySyncRemove", module);
        } catch (FileNotFoundException e) {
            Debug.logError(e, "Could not serialize primary key to save EntitySyncRemove", module);
        } catch (IOException e) {
            Debug.logError(e, "Could not serialize primary key to save EntitySyncRemove", module);
        }

        if (serializedPK != null) {
            GenericValue entitySyncRemove = this.makeValue("EntitySyncRemove");
            entitySyncRemove.set("primaryKeyRemoved", serializedPK);
            this.createSetNextSeqId(entitySyncRemove);
        }
    }

    public void setDistributedCacheClear(DistributedCacheClear distributedCacheClear) {
        this.delegatorData.distributedCacheClear = distributedCacheClear;
    }

    public void setEntityCrypto(EntityCrypto crypto) {
        this.delegatorData.crypto = crypto;
    }

    @SuppressWarnings("unchecked")
    public void setEntityEcaHandler(EntityEcaHandler entityEcaHandler) {
        this.delegatorData.entityEcaHandler = entityEcaHandler;
    }

    public void setNextSubSeqId(GenericValue value, String seqFieldName, int numericPadding, int incrementBy) {
        if (value != null && UtilValidate.isEmpty(value.getString(seqFieldName))) {
            String sequencedIdPrefix = this.getDelegatorInfo().sequencedIdPrefix;

            value.remove(seqFieldName);
            GenericValue lookupValue = this.makeValue(value.getEntityName());
            lookupValue.setPKFields(value);

            boolean beganTransaction = false;
            try {
                if (alwaysUseTransaction) {
                    beganTransaction = TransactionUtil.begin();
                }

                // get values in whatever order, we will go through all of them
                // to find the highest value
                List<GenericValue> allValues = this.findByAnd(value.getEntityName(), lookupValue, null);
                // Debug.logInfo("Get existing values from entity " +
                // value.getEntityName() + " with lookupValue: " + lookupValue +
                // ", and the seqFieldName: " + seqFieldName + ", and the
                // results are: " + allValues, module);
                Integer highestSeqVal = null;
                for (GenericValue curValue : allValues) {
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
                            if (highestSeqVal == null || seqVal > highestSeqVal.intValue()) {
                                highestSeqVal = Integer.valueOf(seqVal);
                            }
                        } catch (Exception e) {
                            Debug.logWarning("Error in make-next-seq-id converting SeqId [" + currentSeqId + "] in field: " + seqFieldName + " from entity: " + value.getEntityName() + " to a number: " + e.toString(), module);
                        }
                    }
                }

                int seqValToUse = (highestSeqVal == null ? 1 : highestSeqVal.intValue() + incrementBy);
                String newSeqId = sequencedIdPrefix + UtilFormatOut.formatPaddedNumber(seqValToUse, numericPadding);
                value.set(seqFieldName, newSeqId);

                // only commit the transaction if we started one...
                TransactionUtil.commit(beganTransaction);
            } catch (Exception e) {
                String errMsg = "Failure in setNextSubSeqId operation for entity [" + value.getEntityName() + "]: " + e.toString() + ". Rolling back transaction.";
                Debug.logError(e, errMsg, module);
                try {
                    // only rollback the transaction if we started one...
                    TransactionUtil.rollback(beganTransaction, errMsg, e);
                } catch (GenericEntityException e2) {
                    Debug.logError(e2, "[GenericDelegator] Could not rollback transaction: " + e2.toString(), module);
                }
                Debug.logError(e, "Error making next seqId", module);
            }
        }
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public void setSessionIdentifier(String identifier) {
        this.sessionIdentifier = identifier;
    }

    public void setUserIdentifier(String identifier) {
        this.userIdentifier = identifier;
    }

    public void setSequencer(SequenceUtil sequencer) {
        this.delegatorData.sequencer = sequencer;
    }

    private void setTestMode(boolean testMode) {
        this.testMode = testMode;
        if (testMode) {
            this.testOperations = FastList.newInstance();
        } else {
            this.testOperations.clear();
        }
    }

    public int store(GenericValue value) throws GenericEntityException {
        return this.store(value, true);
    }

    public int store(GenericValue value, boolean doCacheClear) throws GenericEntityException {
        boolean beganTransaction = false;
        try {
            if (alwaysUseTransaction) {
                beganTransaction = TransactionUtil.begin();
            }

            EntityEcaRuleRunner<?> ecaRunner = this.getEcaRuleRunner(value.getEntityName());
            ecaRunner.evalRules(EntityEcaHandler.EV_VALIDATE, EntityEcaHandler.OP_STORE, value, false);
            GenericHelper helper = getEntityHelper(value.getEntityName());

            if (doCacheClear) {
                // always clear cache before the operation
                ecaRunner.evalRules(EntityEcaHandler.EV_CACHE_CLEAR, EntityEcaHandler.OP_STORE, value, false);
                this.clearCacheLine(value);
            }

            ecaRunner.evalRules(EntityEcaHandler.EV_RUN, EntityEcaHandler.OP_STORE, value, false);
            this.encryptFields(value);

            // if audit log on for any fields, save old value before the update
            // so we still have both
            if (value != null && value.getModelEntity().getHasFieldWithAuditLog()) {
                createEntityAuditLogAll(value, true, false);
            }

            GenericValue updatedEntity = null;

            if (testMode) {
                updatedEntity = this.findOne(value.entityName, value.getPrimaryKey(), false);
            }

            int retVal = helper.store(value);

            if (testMode) {
                storeForTestRollback(new TestOperation(OperationType.UPDATE, updatedEntity));
            }
            // refresh the valueObject to get the new version
            if (value.lockEnabled()) {
                refresh(value, doCacheClear);
            }

            ecaRunner.evalRules(EntityEcaHandler.EV_RETURN, EntityEcaHandler.OP_STORE, value, false);

            return retVal;
        } catch (GenericEntityException e) {
            String errMsg = "Failure in store operation for entity [" + value.getEntityName() + "]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, module);
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, errMsg, e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "[GenericDelegator] Could not rollback transaction: " + e2.toString(), module);
            }
            // after rolling back, rethrow the exception
            throw e;
        } finally {
            // only commit the transaction if we started one... this will throw
            // an exception if it fails
            TransactionUtil.commit(beganTransaction);
        }
    }

    public int storeAll(List<GenericValue> values) throws GenericEntityException {
        return this.storeAll(values, true);
    }

    public int storeAll(List<GenericValue> values, boolean doCacheClear) throws GenericEntityException {
        return this.storeAll(values, doCacheClear, false);
    }

    public int storeAll(List<GenericValue> values, boolean doCacheClear, boolean createDummyFks) throws GenericEntityException {
        if (values == null) {
            return 0;
        }

        int numberChanged = 0;

        boolean beganTransaction = false;
        try {
            beganTransaction = TransactionUtil.begin();

            for (GenericValue value : values) {
                String entityName = value.getEntityName();
                GenericPK primaryKey = value.getPrimaryKey();
                GenericHelper helper = getEntityHelper(entityName);

                // exists?
                // NOTE: don't use findByPrimaryKey because we don't want to the
                // ECA events to fire and such
                if (!primaryKey.isPrimaryKey()) {
                    throw new GenericModelException("[GenericDelegator.storeAll] One of the passed primary keys is not a valid primary key: " + primaryKey);
                }
                GenericValue existing = null;
                try {
                    existing = helper.findByPrimaryKey(primaryKey);
                    this.decryptFields(existing);
                } catch (GenericEntityNotFoundException e) {
                    existing = null;
                }

                if (existing == null) {
                    if (createDummyFks) {
                        value.checkFks(true);
                    }
                    this.create(value, doCacheClear);
                    numberChanged++;
                } else {
                    // don't send fields that are the same, and if no fields
                    // have changed, update nothing
                    ModelEntity modelEntity = value.getModelEntity();
                    GenericValue toStore = GenericValue.create(modelEntity, (Map<String, ? extends Object>) value.getPrimaryKey());
                    toStore.setDelegator(this);
                    boolean atLeastOneField = false;
                    Iterator<ModelField> nonPksIter = modelEntity.getNopksIterator();
                    while (nonPksIter.hasNext()) {
                        ModelField modelField = nonPksIter.next();
                        String fieldName = modelField.getName();
                        if (value.containsKey(fieldName)) {
                            Object fieldValue = value.get(fieldName);
                            Object oldValue = existing.get(fieldName);
                            if ((fieldValue == null && oldValue != null) || (fieldValue != null && !fieldValue.equals(oldValue))) {
                                toStore.put(fieldName, fieldValue);
                                atLeastOneField = true;
                            }
                        }
                    }

                    if (atLeastOneField) {
                        if (createDummyFks) {
                            value.checkFks(true);
                        }
                        numberChanged += this.store(toStore, doCacheClear);
                    }
                }
            }

            return numberChanged;
        } catch (GenericEntityException e) {
            String errMsg = "Failure in storeAll operation: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, module);
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, errMsg, e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "[GenericDelegator] Could not rollback transaction: " + e2.toString(), module);
            }
            // after rolling back, rethrow the exception
            throw e;
        } finally {
            // only commit the transaction if we started one... this will throw
            // an exception if it fails
            TransactionUtil.commit(beganTransaction);
        }
    }

    public int storeByCondition(String entityName, Map<String, ? extends Object> fieldsToSet, EntityCondition condition) throws GenericEntityException {
        return storeByCondition(entityName, fieldsToSet, condition, true);
    }

    public int storeByCondition(String entityName, Map<String, ? extends Object> fieldsToSet, EntityCondition condition, boolean doCacheClear) throws GenericEntityException {
        boolean beganTransaction = false;
        try {
            if (alwaysUseTransaction) {
                beganTransaction = TransactionUtil.begin();
            }

            if (doCacheClear) {
                // always clear cache before the operation
                this.clearCacheLineByCondition(entityName, condition);
            }
            ModelEntity modelEntity = getModelReader().getModelEntity(entityName);
            GenericHelper helper = getEntityHelper(entityName);

            List<GenericValue> updatedEntities = null;
            if (testMode) {
                updatedEntities = this.findList(entityName, condition, null, null, null, false);
            }

            int rowsAffected = helper.storeByCondition(modelEntity, fieldsToSet, condition);

            if (testMode) {
                for (GenericValue entity : updatedEntities) {
                    storeForTestRollback(new TestOperation(OperationType.UPDATE, entity));
                }
            }

            return rowsAffected;
        } catch (GenericEntityException e) {
            String errMsg = "Failure in storeByCondition operation for entity [" + entityName + "]: " + e.toString() + ". Rolling back transaction.";
            Debug.logError(e, errMsg, module);
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, errMsg, e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "[GenericDelegator] Could not rollback transaction: " + e2.toString(), module);
            }
            // after rolling back, rethrow the exception
            throw e;
        } finally {
            // only commit the transaction if we started one... this will throw
            // an exception if it fails
            TransactionUtil.commit(beganTransaction);
        }
    }

    private void storeForTestRollback(TestOperation testOperation) {
        if (!this.testMode || this.testRollbackInProgress) {
            throw new IllegalStateException("An attempt was made to store a TestOperation during rollback or outside of test mode");
        }
        this.testOperations.add(testOperation);
    }

}
