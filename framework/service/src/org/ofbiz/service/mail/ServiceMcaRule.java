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
package org.ofbiz.service.mail;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericValue;

import org.w3c.dom.Element;

public class ServiceMcaRule implements java.io.Serializable {

    public static final String module = ServiceMcaRule.class.getName();

    protected String ruleName = null;
    protected List conditions = new LinkedList();
    protected List actions = new LinkedList();
    protected boolean enabled = true;

    public ServiceMcaRule(Element mca) {
        this.ruleName = mca.getAttribute("mail-rule-name");

        List condFList = UtilXml.childElementList(mca, "condition-field");
        Iterator cfi = condFList.iterator();
        while (cfi.hasNext()) {
            Element condElement = (Element) cfi.next();
            conditions.add(new ServiceMcaCondition(condElement, ServiceMcaCondition.CONDITION_FIELD));
        }

        List condHList = UtilXml.childElementList(mca, "condition-header");
        Iterator chi = condHList.iterator();
        while (chi.hasNext()) {
            Element condElement = (Element) chi.next();
            conditions.add(new ServiceMcaCondition(condElement, ServiceMcaCondition.CONDITION_HEADER));
        }

        List condSList = UtilXml.childElementList(mca, "condition-service");
        Iterator csi = condSList.iterator();
        while (csi.hasNext()) {
            Element condElement = (Element) csi.next();
            conditions.add(new ServiceMcaCondition(condElement, ServiceMcaCondition.CONDITION_SERVICE));
        }

        List actList = UtilXml.childElementList(mca, "action");
        Iterator ai = actList.iterator();
        while (ai.hasNext()) {
            Element actionElement = (Element) ai.next();
            actions.add(new ServiceMcaAction(actionElement));
        }
    }

    public void eval(LocalDispatcher dispatcher, MimeMessageWrapper messageWrapper, Set actionsRun, GenericValue userLogin) throws GenericServiceException {
        if (!enabled) {
            Debug.logInfo("Service MCA [" + ruleName + "] is disabled; not running.", module);
            return;
        }
        
        boolean allCondTrue = true;
        Iterator i = conditions.iterator();
        while (i.hasNext()) {
            ServiceMcaCondition cond = (ServiceMcaCondition) i.next();
            if (!cond.eval(dispatcher, messageWrapper, userLogin)) {
                allCondTrue = false;
                break;
            }
        }

        if (allCondTrue) {
            Iterator a = actions.iterator();
            boolean allOkay = true;
            while (a.hasNext() && allOkay) {
                ServiceMcaAction action = (ServiceMcaAction) a.next();
                if (!actionsRun.contains(action.serviceName)) {
                    if (action.runAction(dispatcher, messageWrapper, userLogin)) {
                        actionsRun.add(action.serviceName);
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
