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
package org.ofbiz.shark.tool;

import java.util.Map;
import java.util.HashMap;

import org.ofbiz.shark.container.SharkContainer;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.base.util.Debug;

import org.enhydra.shark.toolagent.AbstractToolAgent;
import org.enhydra.shark.api.internal.toolagent.ToolAgentGeneralException;
import org.enhydra.shark.api.internal.toolagent.ApplicationBusy;
import org.enhydra.shark.api.internal.toolagent.ApplicationNotDefined;
import org.enhydra.shark.api.internal.toolagent.ApplicationNotStarted;
import org.enhydra.shark.api.internal.toolagent.AppParameter;
import org.enhydra.shark.api.SharkTransaction;
import org.enhydra.shark.xpdl.XPDLConstants;
import org.enhydra.shark.xpdl.elements.ExtendedAttributes;

/**
 * Shark Service Engine Agent Tool API
 */

public class ServiceEngineAgent extends AbstractToolAgent {

    public static final String module = ServiceEngineAgent.class.getName();

    public void invokeApplication (SharkTransaction trans, long handle, String applicationName, String procInstId, String actInstId,
            AppParameter[] parameters, Integer appMode) throws ApplicationNotStarted,
            ApplicationNotDefined, ApplicationBusy, ToolAgentGeneralException {

        super.invokeApplication(trans, handle, applicationName, procInstId, actInstId, parameters, appMode);

        // set the status
        status = APP_STATUS_RUNNING;

        // prepare the service
        LocalDispatcher dispatcher = SharkContainer.getDispatcher();
        Map serviceContext = new HashMap();
        this.getServiceContext(parameters, serviceContext);

        // invoke the service
        Map serviceResult = null;
        try {
            serviceResult = dispatcher.runSync(appName, serviceContext);
        } catch (GenericServiceException e) {
            status = APP_STATUS_INVALID;
            Debug.logError(e, module);
            throw new ToolAgentGeneralException(e);
        }

        // process the result
        this.getServiceResults(parameters, serviceResult);

        // check for errors
        if (ServiceUtil.isError(serviceResult)) {
            status = APP_STATUS_INVALID;
        } else {
            status = APP_STATUS_FINISHED;
        }
    }

    private void getServiceContext(AppParameter[] params, Map context) {
        if (params != null && context != null) {
            for (int i = 1; i < params.length; i++) {
                if (params[i].the_mode.equals(XPDLConstants.FORMAL_PARAMETER_MODE_IN) || params[i].the_mode.equals(XPDLConstants.FORMAL_PARAMETER_MODE_INOUT)) {
                    context.put(params[i].the_formal_name, params[i].the_value);
                }
            }
        }
    }

    private void getServiceResults(AppParameter[] params, Map result) {
        if (params != null && result != null) {
            for (int i = 1; i < params.length; i++) {
                if (params[i].the_mode.equals(XPDLConstants.FORMAL_PARAMETER_MODE_OUT) || params[i].the_mode.equals(XPDLConstants.FORMAL_PARAMETER_MODE_INOUT)) {
                    params[i].the_value = result.get(params[i].the_formal_name);
                }
            }
        }
    }

    protected ExtendedAttributes readParamsFromExtAttributes (String extAttribs) throws Exception {
        ExtendedAttributes eas = super.readParamsFromExtAttributes(extAttribs);
        return eas;
    }
}
