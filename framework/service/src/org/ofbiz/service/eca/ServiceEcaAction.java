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

import java.util.*;
import javax.transaction.xa.XAException;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceXaWrapper;
import org.ofbiz.service.ServiceUtil;
import org.w3c.dom.Element;

/**
 * ServiceEcaAction
 */
public class ServiceEcaAction implements java.io.Serializable {

    public static final String module = ServiceEcaAction.class.getName();

    protected String eventName = null;
    protected String serviceName = null;
    protected String serviceMode = null;
    protected String resultMapName = null;
    protected String runAsUser = null;

    protected boolean newTransaction = false;
    protected boolean resultToContext = true;
    protected boolean ignoreFailure = false;
    protected boolean ignoreError = false;    
    protected boolean persist = false;

    protected ServiceEcaAction() {}

    public ServiceEcaAction(Element action, String event) {
        this.eventName = event;
        this.serviceName = action.getAttribute("service");
        this.serviceMode = action.getAttribute("mode");
        this.runAsUser = action.getAttribute("run-as-user");
        // support the old, inconsistent attribute name
        if (UtilValidate.isEmail(this.runAsUser)) this.runAsUser = action.getAttribute("runAsUser");
        this.resultMapName = action.getAttribute("result-map-name");
        
        // default is true, so anything but false is true
        this.resultToContext = !"false".equals(action.getAttribute("result-to-context"));
        this.newTransaction = !"false".equals(action.getAttribute("new-transaction"));
        this.ignoreFailure = !"false".equals(action.getAttribute("ignore-failure"));
        this.ignoreError = !"false".equals(action.getAttribute("ignore-error"));
        this.persist = "true".equals(action.getAttribute("persist"));
    }

    public boolean runAction(String selfService, DispatchContext dctx, Map context, Map result) throws GenericServiceException {
        if (serviceName.equals(selfService)) {
            throw new GenericServiceException("Cannot invoke self on ECA.");
        }

        // pull out context parameters needed for this service.
        Map actionContext = dctx.getModelService(serviceName).makeValid(context, ModelService.IN_PARAM);

        // set the userLogin object in the context
        actionContext.put("userLogin", ServiceUtil.getUserLogin(dctx, actionContext, runAsUser));
        
        Map actionResult = null;
        LocalDispatcher dispatcher = dctx.getDispatcher();

        if (eventName.startsWith("global-")) {
            // XA resource ECA
            ServiceXaWrapper xaw = new ServiceXaWrapper(dctx);
            if (eventName.equals("global-rollback")) {
                xaw.setRollbackService(serviceName, context, "async".equals(serviceMode), persist); // using the actual context so we get updates
            } else if (eventName.equals("global-commit")) {
                xaw.setCommitService(serviceName, context, "async".equals(serviceMode), persist);   // using the actual context so we get updates
            }
            try {
                xaw.enlist();
            } catch (XAException e) {
                throw new GenericServiceException("Unable to enlist ServiceXaWrapper with transaction", e);
            }
        } else {
            // standard ECA
            if (serviceMode.equals("sync")) {
                if (newTransaction)
                    actionResult = dispatcher.runSync(serviceName, actionContext, -1, true);
                else
                    actionResult = dispatcher.runSync(serviceName, actionContext);
            } else if (serviceMode.equals("async")) {
                dispatcher.runAsync(serviceName, actionContext, persist);
            }
        }

        // put the results in to the defined map
        if (resultMapName != null && resultMapName.length() > 0) {
            Map resultMap = (Map) context.get(resultMapName);
            if (resultMap == null) {
                resultMap = new HashMap();
            }
            resultMap.putAll(dctx.getModelService(this.serviceName).makeValid(actionResult, ModelService.OUT_PARAM, false, null));
            context.put(resultMapName, resultMap);
        }

        // use the result to update the context fields.
        if (resultToContext) {
            context.putAll(dctx.getModelService(this.serviceName).makeValid(actionResult, ModelService.OUT_PARAM, false, null));
        }

        // if we aren't ignoring errors check it here...
        boolean success = true;
        if (actionResult != null) {
            if (!ignoreFailure) {
                if (ModelService.RESPOND_FAIL.equals(actionResult.get(ModelService.RESPONSE_MESSAGE))) {
                    if (result != null) {
                        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_FAIL);
                    }
                    success = false;
                }
            }
            if (!ignoreError) {
                if (ModelService.RESPOND_ERROR.equals(actionResult.get(ModelService.RESPONSE_MESSAGE))) {
                    if (result != null) {
                        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
                    }
                    success = false;
                }
            }
        }

        if (result != null && !success) {
            String errorMessage = (String) actionResult.get(ModelService.ERROR_MESSAGE);
            List errorMessageList = (List) actionResult.get(ModelService.ERROR_MESSAGE_LIST);
            Map errorMessageMap = (Map) actionResult.get(ModelService.ERROR_MESSAGE_MAP);

            // do something with the errorMessage
            if (UtilValidate.isNotEmpty(errorMessage)) {
                if (UtilValidate.isEmpty((String) result.get(ModelService.ERROR_MESSAGE))) {
                    result.put(ModelService.ERROR_MESSAGE, errorMessage);
                } else {
                    if (errorMessageList == null) errorMessageList = new LinkedList();
                    errorMessageList.add(0, errorMessage);
                }
            }
            // do something with the errorMessageList
            if (errorMessageList != null && errorMessageList.size() > 0) {
                List origErrorMessageList = (List) result.get(ModelService.ERROR_MESSAGE_LIST);
                if (origErrorMessageList == null) {
                    result.put(ModelService.ERROR_MESSAGE_LIST, errorMessageList);
                } else {
                    origErrorMessageList.addAll(errorMessageList);
                }
            }
            // do something with the errorMessageMap
            if (errorMessageMap != null && errorMessageMap.size() > 0) {
                Map origErrorMessageMap = (Map) result.get(ModelService.ERROR_MESSAGE_MAP);
                if (origErrorMessageMap == null) {
                    result.put(ModelService.ERROR_MESSAGE_MAP, errorMessageMap);
                } else {
                    origErrorMessageMap.putAll(errorMessageMap);
                }
            }
        }

        return success;
    }
}
