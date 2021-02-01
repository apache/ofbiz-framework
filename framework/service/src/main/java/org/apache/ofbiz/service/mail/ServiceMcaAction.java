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

import java.util.HashMap;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.w3c.dom.Element;

@SuppressWarnings("serial")
public class ServiceMcaAction implements java.io.Serializable {

    private static final String MODULE = ServiceMcaAction.class.getName();

    private String serviceName = null;
    private String serviceMode = null;
    private String runAsUser = null;
    private boolean persist = false;

    /**
     * Gets service name.
     * @return the service name
     */
    public String getServiceName() {
        return serviceName;
    }

    protected ServiceMcaAction() { }

    protected ServiceMcaAction(Element action) {
        this.serviceName = action.getAttribute("service");
        this.serviceMode = action.getAttribute("mode");
        this.runAsUser = action.getAttribute("run-as-user");
        // support the old, inconsistent attribute name
        if (UtilValidate.isEmail(this.runAsUser)) {
            this.runAsUser = action.getAttribute("runAsUser");
        }
        this.persist = "true".equals(action.getAttribute("persist"));
    }

    /**
     * Run action boolean.
     * @param dispatcher the dispatcher
     * @param messageWrapper the message wrapper
     * @param userLogin the user login
     * @return the boolean
     * @throws GenericServiceException the generic service exception
     */
    public boolean runAction(LocalDispatcher dispatcher, MimeMessageWrapper messageWrapper, GenericValue userLogin) throws GenericServiceException {
        Map<String, Object> serviceContext = new HashMap<>();
        serviceContext.putAll(UtilMisc.toMap("messageWrapper", messageWrapper, "userLogin", userLogin));
        serviceContext.put("userLogin", ServiceUtil.getUserLogin(dispatcher.getDispatchContext(), serviceContext, runAsUser));

        if ("sync".equals(serviceMode)) {
            Map<String, Object> result = dispatcher.runSync(serviceName, serviceContext);
            if (ServiceUtil.isError(result)) {
                Debug.logError(ServiceUtil.getErrorMessage(result), MODULE);
                return false;
            }
            return true;
        } else if ("async".equals(serviceMode)) {
            dispatcher.runAsync(serviceName, serviceContext, persist);
            return true;
        } else {
            Debug.logError("Invalid service mode [" + serviceMode + "] unable to invoke MCA action (" + serviceName + ").", MODULE);
            return false;
        }
    }
}
