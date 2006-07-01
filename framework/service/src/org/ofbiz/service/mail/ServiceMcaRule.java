/*
 * $Id: ServiceMcaRule.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
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

/**
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.3
 */
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
