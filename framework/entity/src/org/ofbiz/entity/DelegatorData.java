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

import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.cache.Cache;
import org.ofbiz.entity.config.DelegatorInfo;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.ofbiz.entity.eca.EntityEcaHandler;
import org.ofbiz.entity.model.ModelGroupReader;
import org.ofbiz.entity.model.ModelReader;
import org.ofbiz.entity.util.DistributedCacheClear;
import org.ofbiz.entity.util.EntityCrypto;
import org.ofbiz.entity.util.SequenceUtil;

/** Data source for the DelegatorImpl Class. */
public class DelegatorData implements Cloneable {

    public static final String module = DelegatorData.class.getName();
    /**
     * the delegatorDataCache will now be a HashMap, allowing reload of
     * definitions, but the delegator will always be the same object for the
     * given name
     */
    protected static final Map<String, DelegatorData> delegatorDataCache = FastMap.newInstance();

    /**
     * keeps a list of field key sets used in the by and cache, a Set (of Sets
     * of fieldNames) for each entityName
     */
    protected Map<?, ?> andCacheFieldSets = FastMap.newInstance();
    protected Cache cache = null;
    protected EntityCrypto crypto = null;
    protected DelegatorInfo delegatorInfo = null;
    protected String delegatorName = null;
    protected DistributedCacheClear distributedCacheClear = null;
    protected EntityEcaHandler<?> entityEcaHandler = null;
    protected boolean initialized = false;
    protected ModelGroupReader modelGroupReader = null;
    protected ModelReader modelReader = null;
    protected String originalDelegatorName = null;
    protected SequenceUtil sequencer = null;

    public static synchronized DelegatorData getInstance(String delegatorName) throws GenericEntityException {
        if (delegatorName == null) {
            delegatorName = "default";
            Debug.logWarning(new Exception("Location where getting delegator with null name"), "Got a getGenericDelegator call with a null delegatorName, assuming \"default\" for the name.", module);
        }
        DelegatorData delegatorData = delegatorDataCache.get(delegatorName);
        if (delegatorData == null) {
            if (Debug.infoOn()) {
                Debug.logInfo("Creating new delegator data instance [" + delegatorName + "] (" + Thread.currentThread().getName() + ")", module);
            }
            delegatorData = new DelegatorData(delegatorName);
            delegatorDataCache.put(delegatorName, delegatorData);
        }
        return delegatorData;
    }

    /** Only allow creation through the factory method */
    protected DelegatorData() {
    }

    /** Only allow creation through the factory method */
    protected DelegatorData(String delegatorName) throws GenericEntityException {
        this.delegatorName = delegatorName;
        this.originalDelegatorName = delegatorName;
        this.modelReader = ModelReader.getModelReader(delegatorName);
        this.modelGroupReader = ModelGroupReader.getModelGroupReader(delegatorName);
        this.cache = new Cache(delegatorName);
        this.delegatorInfo = EntityConfigUtil.getDelegatorInfo(delegatorName);
    }

    @Override
    protected Object clone() {
        DelegatorData delegatorData = new DelegatorData();
        delegatorData.andCacheFieldSets = this.andCacheFieldSets;
        delegatorData.cache = this.cache;
        delegatorData.crypto = this.crypto;
        delegatorData.delegatorInfo = this.delegatorInfo;
        delegatorData.delegatorName = this.delegatorName;
        delegatorData.distributedCacheClear = this.distributedCacheClear;
        delegatorData.entityEcaHandler = this.entityEcaHandler;
        delegatorData.initialized = this.initialized;
        delegatorData.modelGroupReader = this.modelGroupReader;
        delegatorData.modelReader = this.modelReader;
        delegatorData.originalDelegatorName = this.originalDelegatorName;
        delegatorData.sequencer = this.sequencer;
        return delegatorData;
    }
}
