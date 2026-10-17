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
package org.apache.ofbiz.service.mail;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.w3c.dom.Element;

@SuppressWarnings("serial")
public class ServiceMcaRule implements java.io.Serializable {

    private static final String MODULE = ServiceMcaRule.class.getName();

    private String ruleName = null;
    private List<ServiceMcaCondition> conditions = new LinkedList<>();
    private List<ServiceMcaAction> actions = new LinkedList<>();
    private boolean enabled = true;

    public ServiceMcaRule(Element mca) {
        this.ruleName = mca.getAttribute("mail-rule-name");

        for (Element condElement: UtilXml.childElementList(mca, "condition-field")) {
            conditions.add(new ServiceMcaCondition(condElement, ServiceMcaCondition.CONDITION_FIELD));
        }

        for (Element condElement: UtilXml.childElementList(mca, "condition-header")) {
            conditions.add(new ServiceMcaCondition(condElement, ServiceMcaCondition.CONDITION_HEADER));
        }

        for (Element condElement: UtilXml.childElementList(mca, "condition-service")) {
            conditions.add(new ServiceMcaCondition(condElement, ServiceMcaCondition.CONDITION_SERVICE));
        }

        for (Element actionElement: UtilXml.childElementList(mca, "action")) {
            actions.add(new ServiceMcaAction(actionElement));
        }
    }

    /**
     * Eval.
     * @param dispatcher the dispatcher
     * @param messageWrapper the message wrapper
     * @param actionsRun the actions run
     * @param userLogin the user login
     * @throws GenericServiceException the generic service exception
     */
    public void eval(LocalDispatcher dispatcher, MimeMessageWrapper messageWrapper, Set<String> actionsRun, GenericValue userLogin)
            throws GenericServiceException {
        if (!enabled) {
            Debug.logInfo("Service MCA [" + ruleName + "] is disabled; not running.", MODULE);
            return;
        }

        boolean allCondTrue = true;
        for (ServiceMcaCondition cond: conditions) {
            if (!cond.eval(dispatcher, messageWrapper, userLogin)) {
                allCondTrue = false;
                break;
            }
        }

        if (allCondTrue) {
            for (ServiceMcaAction action: actions) {
                if (!actionsRun.contains(action.getServiceName())) {
                    if (action.runAction(dispatcher, messageWrapper, userLogin)) {
                        actionsRun.add(action.getServiceName());
                    } else {
                        break;
                    }
                }
            }
        }
    }

    /**
     * Sets enabled.
     * @param enabled the enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Is enabled boolean.
     * @return the boolean
     */
    public boolean isEnabled() {
        return this.enabled;
    }
}
