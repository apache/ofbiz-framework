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
package org.ofbiz.entityext.eca;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.eca.EntityEcaHandler;
import org.ofbiz.entityext.EntityServiceFactory;
import org.ofbiz.service.DispatchContext;

/**
 * EntityEcaUtil
 */
public class DelegatorEcaHandler implements EntityEcaHandler {

    public static final String module = DelegatorEcaHandler.class.getName();

    protected GenericDelegator delegator = null;
    protected String delegatorName = null;
    protected String entityEcaReaderName = null;
    protected DispatchContext dctx = null;

    public DelegatorEcaHandler() { }

    public void setDelegator(GenericDelegator delegator) {
        this.delegator = delegator;
        this.delegatorName = delegator.getDelegatorName();
        this.entityEcaReaderName = EntityEcaUtil.getEntityEcaReaderName(this.delegatorName);
        this.dctx = EntityServiceFactory.getDispatchContext(delegator);
        
        //preload the cache
        EntityEcaUtil.getEntityEcaCache(this.entityEcaReaderName);
    }

    public Map getEntityEventMap(String entityName) {
        Map ecaCache = EntityEcaUtil.getEntityEcaCache(this.entityEcaReaderName);
        if (ecaCache == null) return null;
        return (Map) ecaCache.get(entityName);
    }

    public void evalRules(String currentOperation, Map eventMap, String event, GenericEntity value, boolean isError) throws GenericEntityException {
        // if the eventMap is passed we save a HashMap lookup, but if not that's okay we'll just look it up now
        if (eventMap == null) eventMap = this.getEntityEventMap(value.getEntityName());
        if (eventMap == null || eventMap.size() == 0) {
            //Debug.logInfo("Handler.evalRules for entity " + value.getEntityName() + ", event " + event + ", no eventMap for this entity", module);
            return;
        }

        List rules = (List) eventMap.get(event);
        //Debug.logInfo("Handler.evalRules for entity " + value.getEntityName() + ", event " + event + ", num rules=" + (rules == null ? 0 : rules.size()), module);
        
        if (rules == null || rules.size() == 0) {
            return;
        }
        
        Iterator i = rules.iterator();
        if (i.hasNext() && Debug.verboseOn()) Debug.logVerbose("Running ECA (" + event + ").", module);
        Set actionsRun = new TreeSet();
        while (i.hasNext()) {
            EntityEcaRule eca = (EntityEcaRule) i.next();
            eca.eval(currentOperation, this.dctx, value, isError, actionsRun);
        }
    }
}
