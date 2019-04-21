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
package org.apache.ofbiz.service.eca;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.w3c.dom.Element;

/**
 * ServiceEcaRule
 */
@SuppressWarnings("serial")
public final class ServiceEcaRule implements java.io.Serializable {

    public static final String module = ServiceEcaRule.class.getName();

    public final String serviceName;
    public final String eventName;
    public final boolean runOnFailure;
    public final boolean runOnError;
    public final List<ServiceEcaCondition> conditions = new ArrayList<>();
    public final List<Object> actionsAndSets = new ArrayList<>();
    public boolean enabled = true;
    public final String definitionLocation;

    public ServiceEcaRule(Element eca, String definitionLocation) {
        this.definitionLocation = definitionLocation;
        this.serviceName = eca.getAttribute("service");
        this.eventName = eca.getAttribute("event");
        this.runOnFailure = "true".equals(eca.getAttribute("run-on-failure"));
        this.runOnError = "true".equals(eca.getAttribute("run-on-error"));
        this.enabled = !"false".equals(eca.getAttribute("enabled"));

        for (Element element: UtilXml.childElementList(eca, "condition")) {
            conditions.add(new ServiceEcaCondition(element, true, false));
        }

        for (Element element: UtilXml.childElementList(eca, "condition-field")) {
            conditions.add(new ServiceEcaCondition(element, false, false));
        }

        for (Element element: UtilXml.childElementList(eca, "condition-service")) {
            conditions.add(new ServiceEcaCondition(element, false, true));
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("Conditions: " + conditions, module);
        }

        Set<String> nameSet = UtilMisc.toSet("set", "action");
        for (Element actionOrSetElement: UtilXml.childElementList(eca, nameSet)) {
            if ("action".equals(actionOrSetElement.getNodeName())) {
                this.actionsAndSets.add(new ServiceEcaAction(actionOrSetElement, this.eventName));
            } else {
                this.actionsAndSets.add(new ServiceEcaSetField(actionOrSetElement));
            }
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("actions and sets (intermixed): " + actionsAndSets, module);
        }
    }

    public String getShortDisplayName() {
        return this.serviceName + ":" + this.eventName;
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public String getEventName() {
        return this.eventName;
    }

    public String getDefinitionLocation() {
        return this.definitionLocation;
    }

    public List<ServiceEcaAction> getEcaActionList() {
        List<ServiceEcaAction> actionList = new LinkedList<>();
        for (Object actionOrSet: this.actionsAndSets) {
            if (actionOrSet instanceof ServiceEcaAction) {
                actionList.add((ServiceEcaAction) actionOrSet);
            }
        }
        return actionList;
    }

    public List<ServiceEcaCondition> getEcaConditionList() {
        List<ServiceEcaCondition> condList = new LinkedList<>();
        condList.addAll(this.conditions);
        return condList;
    }

    public void eval(String serviceName, DispatchContext dctx, Map<String, Object> context, Map<String, Object> result, boolean isError, boolean isFailure, Set<String> actionsRun) throws GenericServiceException {
        if (!enabled) {
            Debug.logInfo("Service ECA [" + this.serviceName + "] on [" + this.eventName + "] is disabled; not running.", module);
            return;
        }
        if (isFailure && !this.runOnFailure) {
            return;
        }
        if (isError && !this.runOnError) {
            return;
        }

        boolean allCondTrue = true;
        for (ServiceEcaCondition ec: conditions) {
            if (!ec.eval(serviceName, dctx, context)) {
                if (Debug.infoOn()) {
                    Debug.logInfo("For Service ECA [" + this.serviceName + "] on [" + this.eventName + "] got false for condition: " + ec, module);
                }
                allCondTrue = false;
                break;
            } else {
                if (Debug.verboseOn()) {
                    Debug.logVerbose("For Service ECA [" + this.serviceName + "] on [" + this.eventName + "] got true for condition: " + ec, module);
                }
            }
        }

        // if all conditions are true
        if (allCondTrue) {
            for (Object setOrAction: actionsAndSets) {
                if (setOrAction instanceof ServiceEcaAction) {
                    ServiceEcaAction ea = (ServiceEcaAction) setOrAction;
                    // in order to enable OR logic without multiple calls to the given service,
                    // only execute a given service name once per service call phase
                    if (!actionsRun.contains(ea.serviceName)) {
                        if (Debug.infoOn()) {
                            Debug.logInfo("Running Service ECA Service: " + ea.serviceName + ", triggered by rule on Service: " + serviceName, module);
                        }
                        if (ea.runAction(serviceName, dctx, context, result)) {
                            actionsRun.add(ea.serviceName);
                        }
                    }
                } else {
                    ServiceEcaSetField sf = (ServiceEcaSetField) setOrAction;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((actionsAndSets == null) ? 0 : actionsAndSets.hashCode());
        result = prime * result + ((conditions == null) ? 0 : conditions.hashCode());
        result = prime * result + ((definitionLocation == null) ? 0 : definitionLocation.hashCode());
        result = prime * result + (enabled ? 1231 : 1237);
        result = prime * result + ((eventName == null) ? 0 : eventName.hashCode());
        result = prime * result + (runOnError ? 1231 : 1237);
        result = prime * result + (runOnFailure ? 1231 : 1237);
        result = prime * result + ((serviceName == null) ? 0 : serviceName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ServiceEcaRule) {
            ServiceEcaRule other = (ServiceEcaRule) obj;
            if (!UtilValidate.areEqual(this.serviceName, other.serviceName)) {
                return false;
            }
            if (!UtilValidate.areEqual(this.eventName, other.eventName)) {
                return false;
            }
            if (!this.conditions.equals(other.conditions)) {
                return false;
            }
            if (!this.actionsAndSets.equals(other.actionsAndSets)) {
                return false;
            }

            if (this.runOnFailure != other.runOnFailure) {
                return false;
            }
            if (this.runOnError != other.runOnError) {
                return false;
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "ServiceEcaRule:" + this.serviceName + ":" + this.eventName + ":runOnError=" + this.runOnError + ":runOnFailure=" + this.runOnFailure + ":enabled=" + this.enabled + ":conditions=" + this.conditions + ":actionsAndSets=" + this.actionsAndSets;
    }
}
