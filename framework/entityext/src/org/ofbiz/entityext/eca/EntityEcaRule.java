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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastMap;

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

    private static final Set<String> nameSet = new HashSet<String>(2);
    static {
        nameSet.add("set");
        nameSet.add("action");
    }

    protected String entityName = null;
    protected String operationName = null;
    protected String eventName = null;
    protected boolean runOnError = false;
    protected List<EntityEcaCondition> conditions = FastList.newInstance();
    protected List<Object> actionsAndSets = FastList.newInstance();
    protected boolean enabled = true;

    protected EntityEcaRule() {}

    public EntityEcaRule(Element eca) {
        this.entityName = eca.getAttribute("entity");
        this.operationName = eca.getAttribute("operation");
        this.eventName = eca.getAttribute("event");
        this.runOnError = "true".equals(eca.getAttribute("run-on-error"));

        for (Element element: UtilXml.childElementList(eca, "condition")) {
            conditions.add(new EntityEcaCondition(element, true));
        }

        for (Element element: UtilXml.childElementList(eca, "condition-field")) {
            conditions.add(new EntityEcaCondition(element, false));
        }

        if (Debug.verboseOn()) Debug.logVerbose("Conditions: " + conditions, module);

        for (Element actionOrSetElement: UtilXml.childElementList(eca, nameSet)) {
            if ("action".equals(actionOrSetElement.getNodeName())) {
                this.actionsAndSets.add(new EntityEcaAction(actionOrSetElement));
            } else {
                this.actionsAndSets.add(new EntityEcaSetField(actionOrSetElement));
            }
        }

        if (Debug.verboseOn()) Debug.logVerbose("actions and sets (intermixed): " + actionsAndSets, module);
    }

    public void eval(String currentOperation, DispatchContext dctx, GenericEntity value, boolean isError, Set<String> actionsRun) throws GenericEntityException {
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

        Map<String, Object> context = FastMap.newInstance();
        context.putAll(value);

        boolean allCondTrue = true;
        for (EntityEcaCondition ec: conditions) {
            if (!ec.eval(dctx, value)) {
                allCondTrue = false;
                break;
            }
        }

        if (allCondTrue) {
            for (Object actionOrSet: actionsAndSets) {
                if (actionOrSet instanceof EntityEcaAction) {
                    EntityEcaAction ea = (EntityEcaAction) actionOrSet;
                    // in order to enable OR logic without multiple calls to the given service,
                    //only execute a given service name once per service call phase
                    if (actionsRun.add(ea.serviceName)) {
                        if (Debug.infoOn()) Debug.logInfo("Running Entity ECA Service: " + ea.serviceName + ", triggered by rule on Entity: " + value.getEntityName(), module);
                        ea.runAction(dctx, context, value);
                    }
                } else {
                    EntityEcaSetField sf = (EntityEcaSetField) actionOrSet;
                    sf.eval(context);
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
