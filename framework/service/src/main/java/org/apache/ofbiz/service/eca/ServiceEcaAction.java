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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceSynchronization;
import org.apache.ofbiz.service.ServiceUtil;
import org.w3c.dom.Element;

/**
 * ServiceEcaAction
 */
@SuppressWarnings("serial")
public class ServiceEcaAction implements java.io.Serializable {

    public static final String module = ServiceEcaAction.class.getName();

    protected String eventName = null;
    protected String serviceName = null;
    protected String serviceMode = null;
    protected String resultMapName = null;
    protected String runAsUser = null;

    protected boolean newTransaction = false;
    protected boolean resultToContext = true;
    protected boolean resultToResult = false;
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
        if (UtilValidate.isEmpty(this.runAsUser)) this.runAsUser = action.getAttribute("runAsUser");
        this.resultMapName = action.getAttribute("result-map-name");

        // default is true, so anything but false is true
        this.resultToContext = !"false".equals(action.getAttribute("result-to-context"));
        // default is false, so anything but true is false
        this.resultToResult = "true".equals(action.getAttribute("result-to-result"));
        this.newTransaction = !"false".equals(action.getAttribute("new-transaction"));
        this.ignoreFailure = !"false".equals(action.getAttribute("ignore-failure"));
        this.ignoreError = !"false".equals(action.getAttribute("ignore-error"));
        this.persist = "true".equals(action.getAttribute("persist"));
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public String getServiceMode() {
        return this.serviceMode;
    }

    public boolean isPersist() {
        return this.persist;
    }

    public String getShortDisplayDescription() {
        return this.serviceName + "[" + this.serviceMode + (this.persist ? "-persist" : "") + "]";
    }

    public boolean runAction(String selfService, DispatchContext dctx, Map<String, Object> context, Map<String, Object> result) throws GenericServiceException {
        if (serviceName.equals(selfService)) {
            throw new GenericServiceException("Cannot invoke self on ECA.");
        }

        // pull out context parameters needed for this service.
        Map<String, Object> actionContext = dctx.getModelService(serviceName).makeValid(context, ModelService.IN_PARAM);

        // set the userLogin object in the context
        actionContext.put("userLogin", ServiceUtil.getUserLogin(dctx, actionContext, runAsUser));

        Map<String, Object> actionResult = null;
        LocalDispatcher dispatcher = dctx.getDispatcher();
        // if SECAs have been turned off, then just return true which has same effect as if secas ran successfully
        if (dispatcher.isEcasDisabled()) {
            Debug.logWarning("SECAs have been disabled on purpose and will not be run for [" + serviceName + "]", module);
            return true;
        }

        if (eventName.startsWith("global-")) {
            if (eventName.equals("global-rollback")) {
                ServiceSynchronization.registerRollbackService(dctx, serviceName, runAsUser, context, "async".equals(serviceMode), persist); // using the actual context so we get updates
            } else if (eventName.equals("global-commit")) {
                ServiceSynchronization.registerCommitService(dctx, serviceName, runAsUser, context, "async".equals(serviceMode), persist); // using the actual context so we get updates
            } else if (eventName.equals("global-commit-post-run")) {
                ServiceSynchronization.registerCommitService(dctx, serviceName, runAsUser, context, "async".equals(serviceMode), persist); // using the actual context so we get updates
            }
        } else {
            // standard ECA
            if (this.serviceMode.equals("sync")) {
                if (newTransaction) {
                    actionResult = dispatcher.runSync(this.serviceName, actionContext, -1, true);
                } else {
                    actionResult = dispatcher.runSync(this.serviceName, actionContext);
                }
            } else if (this.serviceMode.equals("async")) {
                dispatcher.runAsync(serviceName, actionContext, persist);
            }
        }

        // put the results in to the defined map
        if (UtilValidate.isNotEmpty(resultMapName)) {
            Map<String, Object> resultMap = UtilGenerics.checkMap(context.get(resultMapName));
            if (resultMap == null) {
                resultMap = new HashMap<String, Object>();
            }
            resultMap.putAll(dctx.getModelService(this.serviceName).makeValid(actionResult, ModelService.OUT_PARAM, false, null));
            context.put(resultMapName, resultMap);
        }

        // use the result to update the context fields.
        if (resultToContext) {
            context.putAll(dctx.getModelService(this.serviceName).makeValid(actionResult, ModelService.OUT_PARAM, false, null));
        }

        // use the result to update the result fields
        if (resultToResult) {
            Map<String, Object> normalizedActionResult = dctx.getModelService(selfService).makeValid(actionResult, ModelService.OUT_PARAM, false, null);
            // don't copy over the error messages, use the combining code to do that later
            normalizedActionResult.remove(ModelService.ERROR_MESSAGE);
            normalizedActionResult.remove(ModelService.ERROR_MESSAGE_LIST);
            normalizedActionResult.remove(ModelService.ERROR_MESSAGE_MAP);
            normalizedActionResult.remove("failMessage");
            result.putAll(normalizedActionResult);
        }

        // if we aren't ignoring errors check it here...
        boolean success = true;
        // don't do this if resultToResult, will already be copied over
        if (actionResult != null && !resultToResult) {
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

        // copy/combine error messages on error/failure (!success) or on resultToResult to combine any error info coming out, regardless of success status
        if ((!success || resultToResult) && UtilValidate.isNotEmpty(actionResult)) {
            String errorMessage = (String) actionResult.get(ModelService.ERROR_MESSAGE);
            String failMessage = (String) actionResult.get("failMessage");
            List<? extends Object> errorMessageList = UtilGenerics.checkList(actionResult.get(ModelService.ERROR_MESSAGE_LIST));
            Map<String, ? extends Object> errorMessageMap = UtilGenerics.checkMap(actionResult.get(ModelService.ERROR_MESSAGE_MAP));

            // do something with the errorMessage
            if (UtilValidate.isNotEmpty(errorMessage)) {
                if (UtilValidate.isEmpty(result.get(ModelService.ERROR_MESSAGE))) {
                    result.put(ModelService.ERROR_MESSAGE, errorMessage);
                } else {
                    List<Object> origErrorMessageList = UtilGenerics.checkList(result.get(ModelService.ERROR_MESSAGE_LIST));
                    if (origErrorMessageList == null) {
                        origErrorMessageList = new LinkedList<Object>();
                        result.put(ModelService.ERROR_MESSAGE_LIST, origErrorMessageList);
                    }
                    origErrorMessageList.add(0, errorMessage);
                }
            }
            // do something with the errorMessageList
            if (UtilValidate.isNotEmpty(errorMessageList)) {
                List<Object> origErrorMessageList = UtilGenerics.checkList(result.get(ModelService.ERROR_MESSAGE_LIST));
                if (origErrorMessageList == null) {
                    result.put(ModelService.ERROR_MESSAGE_LIST, errorMessageList);
                } else {
                    origErrorMessageList.addAll(errorMessageList);
                }
            }
            // do something with the errorMessageMap
            if (UtilValidate.isNotEmpty(errorMessageMap)) {
                Map<String, Object> origErrorMessageMap = UtilGenerics.checkMap(result.get(ModelService.ERROR_MESSAGE_MAP));
                if (origErrorMessageMap == null) {
                    result.put(ModelService.ERROR_MESSAGE_MAP, errorMessageMap);
                } else {
                    origErrorMessageMap.putAll(errorMessageMap);
                }
            }
            // do something with the fail message
            if (UtilValidate.isNotEmpty(failMessage)) {
                String origFailMessage = (String) result.get("failMessage");
                if (UtilValidate.isEmpty(origFailMessage)) {
                    result.put("failMessage", failMessage);
                } else {
                    result.put("failMessage", origFailMessage + ", " + failMessage);
                }
            }
        }

        return success;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ServiceEcaAction) {
            ServiceEcaAction other = (ServiceEcaAction) obj;

            if (!UtilValidate.areEqual(this.eventName, other.eventName)) return false;
            if (!UtilValidate.areEqual(this.serviceName, other.serviceName)) return false;
            if (!UtilValidate.areEqual(this.serviceMode, other.serviceMode)) return false;
            if (!UtilValidate.areEqual(this.resultMapName, other.resultMapName)) return false;
            if (!UtilValidate.areEqual(this.runAsUser, other.runAsUser)) return false;

            if (this.newTransaction != other.newTransaction) return false;
            if (this.resultToContext != other.resultToContext) return false;
            if (this.resultToResult != other.resultToResult) return false;
            if (this.ignoreFailure != other.ignoreFailure) return false;
            if (this.ignoreError != other.ignoreError) return false;
            if (this.persist != other.persist) return false;

            return true;
        } else {
            return false;
        }
    }
}
