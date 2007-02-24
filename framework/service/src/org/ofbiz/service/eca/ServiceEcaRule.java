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

import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.base.util.Debug;
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
    protected List conditions = new LinkedList();
    protected List actions = new LinkedList();
    protected List sets = new LinkedList();
    protected boolean enabled = true;

    protected ServiceEcaRule() {}

    public ServiceEcaRule(Element eca) {
        this.serviceName = eca.getAttribute("service");
        this.eventName = eca.getAttribute("event");
        this.runOnFailure = "true".equals(eca.getAttribute("run-on-failure"));
        this.runOnError = "true".equals(eca.getAttribute("run-on-error"));

        List condList = UtilXml.childElementList(eca, "condition");
        Iterator ci = condList.iterator();
        while (ci.hasNext()) {
            conditions.add(new ServiceEcaCondition((Element) ci.next(), true, false));
        }

        List condFList = UtilXml.childElementList(eca, "condition-field");
        Iterator cfi = condFList.iterator();
        while (cfi.hasNext()) {
            conditions.add(new ServiceEcaCondition((Element) cfi.next(), false, false));
        }

        List condSList = UtilXml.childElementList(eca, "condition-service");
        Iterator sfi = condSList.iterator();
        while (sfi.hasNext()) {
            conditions.add(new ServiceEcaCondition((Element) sfi.next(), false, true));
        }

        if (Debug.verboseOn()) Debug.logVerbose("Conditions: " + conditions, module);

        List setList = UtilXml.childElementList(eca, "set");
        Iterator si = setList.iterator();
        while (si.hasNext()) {
            Element setElement = (Element) si.next();
            sets.add(new ServiceEcaSetField(setElement));
        }

        List actList = UtilXml.childElementList(eca, "action");
        Iterator ai = actList.iterator();
        while (ai.hasNext()) {
            Element actionElement = (Element) ai.next();
            actions.add(new ServiceEcaAction(actionElement, eventName));
        }

        if (Debug.verboseOn()) Debug.logVerbose("Actions: " + actions, module);
    }

    public void eval(String serviceName, DispatchContext dctx, Map context, Map result, boolean isError, boolean isFailure, Set actionsRun) throws GenericServiceException {
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
        Iterator c = conditions.iterator();

        while (c.hasNext()) {
            ServiceEcaCondition ec = (ServiceEcaCondition) c.next();
            if (!ec.eval(serviceName, dctx, context)) {
                if (Debug.infoOn()) Debug.logInfo("Got false for condition: " + ec, module);
                allCondTrue = false;
                break;
            } else {
                if (Debug.verboseOn()) Debug.logVerbose("Got true for condition: " + ec, module);
            }
        }

        // if all conditions are true
        if (allCondTrue) {
            // prepare the internal field setters
            Iterator i = sets.iterator();
            while (i.hasNext()) {
                ServiceEcaSetField sf = (ServiceEcaSetField) i.next();
                sf.eval(context);
            }

            // eval the actions
            Iterator a = actions.iterator();
            boolean allOkay = true;
            while (a.hasNext() && allOkay) {
                ServiceEcaAction ea = (ServiceEcaAction) a.next();
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
