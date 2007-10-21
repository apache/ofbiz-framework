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
package org.ofbiz.service.eca;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastSet;

import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilXml;
import org.w3c.dom.Element;

/**
 * ServiceEcaRule
 */
public class ServiceEcaRule implements java.io.Serializable {

    public static final String module = ServiceEcaRule.class.getName();

    protected String serviceName = null;
    protected String eventName = null;
    protected boolean runOnFailure = false;
    protected boolean runOnError = false;
    protected List<ServiceEcaCondition> conditions = FastList.newInstance();
    protected List<Object> actionsAndSets = FastList.newInstance();
    protected boolean enabled = true;

    protected ServiceEcaRule() {}

    public ServiceEcaRule(Element eca) {
        this.serviceName = eca.getAttribute("service");
        this.eventName = eca.getAttribute("event");
        this.runOnFailure = "true".equals(eca.getAttribute("run-on-failure"));
        this.runOnError = "true".equals(eca.getAttribute("run-on-error"));

        for (Element element: UtilXml.childElementList(eca, "condition")) {
            conditions.add(new ServiceEcaCondition(element, true, false));
        }

        for (Element element: UtilXml.childElementList(eca, "condition-field")) {
            conditions.add(new ServiceEcaCondition(element, false, false));
        }

        for (Element element: UtilXml.childElementList(eca, "condition-service")) {
            conditions.add(new ServiceEcaCondition(element, false, true));
        }

        if (Debug.verboseOn()) Debug.logVerbose("Conditions: " + conditions, module);
        
        Set<String> nameSet = UtilMisc.toSet("set", "action");
        for (Element actionOrSetElement: UtilXml.childElementList(eca, nameSet)) {
            if ("action".equals(actionOrSetElement.getNodeName())) {
                this.actionsAndSets.add(new ServiceEcaAction(actionOrSetElement, this.eventName));
            } else {
                this.actionsAndSets.add(new ServiceEcaSetField(actionOrSetElement));
            }
        }

        if (Debug.verboseOn()) Debug.logVerbose("actions and sets (intermixed): " + actionsAndSets, module);
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
                if (Debug.infoOn()) Debug.logInfo("For Service ECA [" + this.serviceName + "] on [" + this.eventName + "] got false for condition: " + ec, module);
                allCondTrue = false;
                break;
            } else {
                if (Debug.verboseOn()) Debug.logVerbose("For Service ECA [" + this.serviceName + "] on [" + this.eventName + "] got true for condition: " + ec, module);
            }
        }

        // if all conditions are true
        if (allCondTrue) {
            boolean allOkay = true;
            for (Object setOrAction: actionsAndSets) {
                if (setOrAction instanceof ServiceEcaAction) {
                    ServiceEcaAction ea = (ServiceEcaAction) setOrAction;
                    // in order to enable OR logic without multiple calls to the given service,
                    // only execute a given service name once per service call phase
                    if (!actionsRun.contains(ea.serviceName)) {
                        if (Debug.infoOn()) Debug.logInfo("Running Service ECA Service: " + ea.serviceName + ", triggered by rule on Service: " + serviceName, module);
                        if (ea.runAction(serviceName, dctx, context, result)) {
                            actionsRun.add(ea.serviceName);
                        } else {
                            allOkay = false;
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
}
