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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.service.DispatchContext;
import org.w3c.dom.Element;

/**
 * EntityEcaRule
 */
public class EntityEcaRule implements java.io.Serializable {

    public static final String module = EntityEcaRule.class.getName();

    protected String entityName = null;
    protected String operationName = null;
    protected String eventName = null;
    protected boolean runOnError = false;
    protected List conditions = new LinkedList();
    protected List actions = new LinkedList();
    protected boolean enabled = true;

    protected EntityEcaRule() {}

    public EntityEcaRule(Element eca) {
        this.entityName = eca.getAttribute("entity");
        this.operationName = eca.getAttribute("operation");
        this.eventName = eca.getAttribute("event");
        this.runOnError = "true".equals(eca.getAttribute("run-on-error"));

        List condList = UtilXml.childElementList(eca, "condition");
        Iterator ci = condList.iterator();
        while (ci.hasNext()) {
            conditions.add(new EntityEcaCondition((Element) ci.next(), true));
        }

        List condFList = UtilXml.childElementList(eca, "condition-field");
        Iterator cfi = condFList.iterator();
        while (cfi.hasNext()) {
            conditions.add(new EntityEcaCondition((Element) cfi.next(), false));
        }

        if (Debug.verboseOn()) Debug.logVerbose("Conditions: " + conditions, module);

        List actList = UtilXml.childElementList(eca, "action");
        Iterator ai = actList.iterator();
        while (ai.hasNext()) {
            actions.add(new EntityEcaAction((Element) ai.next()));
        }

        if (Debug.verboseOn()) Debug.logVerbose("Actions: " + actions, module);
    }

    public void eval(String currentOperation, DispatchContext dctx, GenericEntity value, boolean isError, Set actionsRun) throws GenericEntityException {
        if (!enabled) {
            Debug.logInfo("Entity ECA [" + this.entityName + "] on [" + this.eventName + "] is disabled; not running.", module);
            return;
        }

        //Debug.logInfo("eval eeca rule: operation=" + currentOperation + ", in event=" + this.eventName + ", on entity=" + this.entityName + ", for value=" + value, module);
        if (isError && !this.runOnError) {
            return;
        }

        if (!"any".equals(this.operationName) && this.operationName.indexOf(currentOperation) == -1) {
            return;
        }

        boolean allCondTrue = true;
        Iterator c = conditions.iterator();
        while (c.hasNext()) {
            EntityEcaCondition ec = (EntityEcaCondition) c.next();
            if (!ec.eval(dctx, value)) {
                allCondTrue = false;
                break;
            }
        }

        if (allCondTrue) {
            Iterator a = actions.iterator();
            while (a.hasNext()) {
                EntityEcaAction ea = (EntityEcaAction) a.next();
                // in order to enable OR logic without multiple calls to the given service,
                //only execute a given service name once per service call phase
                if (!actionsRun.contains(ea.serviceName)) {
                    if (Debug.infoOn()) Debug.logInfo("Running Entity ECA Service: " + ea.serviceName + ", triggered by rule on Entity: " + value.getEntityName(), module);
                    ea.runAction(dctx, value);
                    actionsRun.add(ea.serviceName);
                }
            }
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return this.enabled;
    }
}
