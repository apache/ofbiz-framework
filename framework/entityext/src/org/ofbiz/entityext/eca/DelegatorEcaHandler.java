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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;

import static org.ofbiz.base.util.UtilGenerics.checkList;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.eca.EntityEcaHandler;
import org.ofbiz.entityext.EntityServiceFactory;
import org.ofbiz.service.DispatchContext;

/**
 * EntityEcaUtil
 */
public class DelegatorEcaHandler implements EntityEcaHandler<EntityEcaRule> {

    public static final String module = DelegatorEcaHandler.class.getName();

    protected Delegator delegator = null;
    protected String delegatorName = null;
    protected String entityEcaReaderName = null;
    protected DispatchContext dctx = null;

    public DelegatorEcaHandler() { }

    public void setDelegator(Delegator delegator) {
        this.delegator = delegator;
        this.delegatorName = delegator.getDelegatorName();
        this.entityEcaReaderName = EntityEcaUtil.getEntityEcaReaderName(delegator.getOriginalDelegatorName());
        this.dctx = EntityServiceFactory.getDispatchContext(delegator);

        //preload the cache
        EntityEcaUtil.getEntityEcaCache(this.entityEcaReaderName);
    }

    public Map<String, List<EntityEcaRule>> getEntityEventMap(String entityName) {
        Map<String, Map<String, List<EntityEcaRule>>> ecaCache = EntityEcaUtil.getEntityEcaCache(this.entityEcaReaderName);
        if (ecaCache == null) return null;
        return ecaCache.get(entityName);
    }

    public void evalRules(String currentOperation, Map<String, List<EntityEcaRule>> eventMap, String event, GenericEntity value, boolean isError) throws GenericEntityException {
        // if the eventMap is passed we save a HashMap lookup, but if not that's okay we'll just look it up now
        if (eventMap == null) eventMap = this.getEntityEventMap(value.getEntityName());
        if (UtilValidate.isEmpty(eventMap)) {
            //Debug.logInfo("Handler.evalRules for entity " + value.getEntityName() + ", event " + event + ", no eventMap for this entity", module);
            return;
        }

        List<EntityEcaRule> rules = eventMap.get(event);
        //Debug.logInfo("Handler.evalRules for entity " + value.getEntityName() + ", event " + event + ", num rules=" + (rules == null ? 0 : rules.size()), module);

        if (UtilValidate.isEmpty(rules)) {
            return;
        }

        if (!rules.isEmpty() && Debug.verboseOn()) Debug.logVerbose("Running ECA (" + event + ").", module);
        Set<String> actionsRun = new TreeSet<String>();
        for (EntityEcaRule eca: rules) {
            eca.eval(currentOperation, this.dctx, value, isError, actionsRun);
        }
    }
}
