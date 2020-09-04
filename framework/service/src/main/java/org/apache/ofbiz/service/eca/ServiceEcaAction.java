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
import java.util.Objects;

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

    private static final String MODULE = ServiceEcaAction.class.getName();

    private String eventName = null;
    private String serviceName = null;
    private String serviceMode = null;
    private String resultMapName = null;
    private String runAsUser = null;

    private boolean newTransaction = false;
    private boolean resultToContext = true;
    private boolean resultToResult = false;
    private boolean ignoreFailure = false;
    private boolean ignoreError = false;
    private boolean persist = false;

    protected ServiceEcaAction() { }

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

    /**
     * Gets service name.
     * @return the service name
     */
    public String getServiceName() {
        return this.serviceName;
    }

    /**
     * Gets service mode.
     * @return the service mode
     */
    public String getServiceMode() {
        return this.serviceMode;
    }

    /**
     * Is persist boolean.
     * @return the boolean
     */
    public boolean isPersist() {
        return this.persist;
    }

    /**
     * Gets short display description.
     * @return the short display description
     */
    public String getShortDisplayDescription() {
        return this.serviceName + "[" + this.serviceMode + (this.persist ? "-persist" : "") + "]";
    }

    /**
     * Run action boolean.
     * @param selfService the self service
     * @param dctx the dctx
     * @param context the context
     * @param result the result
     * @return the boolean
     * @throws GenericServiceException the generic service exception
     */
    public boolean runAction(String selfService, DispatchContext dctx, Map<String, Object> context, Map<String, Object> result)
            throws GenericServiceException {
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
            Debug.logWarning("SECAs have been disabled on purpose and will not be run for [" + serviceName + "]", MODULE);
            return true;
        }

        if (eventName.startsWith("global-")) {
            if ("global-rollback".equals(eventName)) {
                ServiceSynchronization.registerRollbackService(dctx, serviceName, runAsUser, context, "async".equals(serviceMode), persist);
                // using the actual context so we get updates
            } else if ("global-commit".equals(eventName)) {
                ServiceSynchronization.registerCommitService(dctx, serviceName, runAsUser, context, "async".equals(serviceMode), persist);
                // using the actual context so we get updates
            } else if ("global-commit-post-run".equals(eventName)) {
                ServiceSynchronization.registerCommitService(dctx, serviceName, runAsUser, context, "async".equals(serviceMode), persist);
                // using the actual context so we get updates
            }
        } else {
            // standard ECA
            if ("sync".equals(this.serviceMode)) {
                if (newTransaction) {
                    actionResult = dispatcher.runSync(this.serviceName, actionContext, -1, true);
                } else {
                    actionResult = dispatcher.runSync(this.serviceName, actionContext);
                }
            } else if ("async".equals(this.serviceMode)) {
                dispatcher.runAsync(serviceName, actionContext, persist);
            }
        }

        // put the results in to the defined map
        if (UtilValidate.isNotEmpty(resultMapName)) {
            Map<String, Object> resultMap = UtilGenerics.cast(context.get(resultMapName));
            if (resultMap == null) {
                resultMap = new HashMap<>();
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
            Map<String, Object> normalizedActionResult = dctx.getModelService(selfService).makeValid(actionResult, ModelService.OUT_PARAM,
                    false, null);
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

        // copy/combine error messages on error/failure (!success) or on resultToResult to combine
        // any error info coming out, regardless of success status
        if ((!success || resultToResult) && UtilValidate.isNotEmpty(actionResult)) {
            String errorMessage = (String) actionResult.get(ModelService.ERROR_MESSAGE);
            String failMessage = (String) actionResult.get("failMessage");
            List<? extends Object> errorMessageList = UtilGenerics.cast(actionResult.get(ModelService.ERROR_MESSAGE_LIST));
            Map<String, ? extends Object> errorMessageMap = UtilGenerics.cast(actionResult.get(ModelService.ERROR_MESSAGE_MAP));

            // do something with the errorMessage
            if (UtilValidate.isNotEmpty(errorMessage)) {
                if (UtilValidate.isEmpty(result.get(ModelService.ERROR_MESSAGE))) {
                    result.put(ModelService.ERROR_MESSAGE, errorMessage);
                } else {
                    List<Object> origErrorMessageList = UtilGenerics.cast(result.get(ModelService.ERROR_MESSAGE_LIST));
                    if (origErrorMessageList == null) {
                        origErrorMessageList = new LinkedList<>();
                        result.put(ModelService.ERROR_MESSAGE_LIST, origErrorMessageList);
                    }
                    origErrorMessageList.add(0, errorMessage);
                }
            }
            // do something with the errorMessageList
            if (UtilValidate.isNotEmpty(errorMessageList)) {
                List<Object> origErrorMessageList = UtilGenerics.cast(result.get(ModelService.ERROR_MESSAGE_LIST));
                if (origErrorMessageList == null) {
                    result.put(ModelService.ERROR_MESSAGE_LIST, errorMessageList);
                } else {
                    origErrorMessageList.addAll(errorMessageList);
                }
            }
            // do something with the errorMessageMap
            if (UtilValidate.isNotEmpty(errorMessageMap)) {
                Map<String, Object> origErrorMessageMap = UtilGenerics.cast(result.get(ModelService.ERROR_MESSAGE_MAP));
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
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (UtilValidate.isNotEmpty(eventName)) buf.append("[").append(eventName).append("]");
        if (UtilValidate.isNotEmpty(ignoreError)) buf.append("[").append(ignoreError).append("]");
        if (UtilValidate.isNotEmpty(ignoreFailure)) buf.append("[").append(ignoreFailure).append("]");
        if (UtilValidate.isNotEmpty(newTransaction)) buf.append("[").append(newTransaction).append("]");
        if (UtilValidate.isNotEmpty(persist)) buf.append("[").append(persist).append("]");
        if (UtilValidate.isNotEmpty(resultMapName)) buf.append("[").append(resultMapName).append("]");
        if (UtilValidate.isNotEmpty(resultToContext)) buf.append("[").append(resultToContext).append("]");
        if (UtilValidate.isNotEmpty(resultToResult)) buf.append("[").append(resultToResult).append("]");
        if (UtilValidate.isNotEmpty(runAsUser)) buf.append("[").append(runAsUser).append("]");
        if (UtilValidate.isNotEmpty(serviceMode)) buf.append("[").append(serviceMode).append("]");
        if (UtilValidate.isNotEmpty(serviceName)) buf.append("[").append(serviceName).append("]");
        return buf.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((eventName == null) ? 0 : eventName.hashCode());
        result = prime * result + (ignoreError ? 1231 : 1237);
        result = prime * result + (ignoreFailure ? 1231 : 1237);
        result = prime * result + (newTransaction ? 1231 : 1237);
        result = prime * result + (persist ? 1231 : 1237);
        result = prime * result + ((resultMapName == null) ? 0 : resultMapName.hashCode());
        result = prime * result + (resultToContext ? 1231 : 1237);
        result = prime * result + (resultToResult ? 1231 : 1237);
        result = prime * result + ((runAsUser == null) ? 0 : runAsUser.hashCode());
        result = prime * result + ((serviceMode == null) ? 0 : serviceMode.hashCode());
        result = prime * result + ((serviceName == null) ? 0 : serviceName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ServiceEcaAction) {
            ServiceEcaAction other = (ServiceEcaAction) obj;

            if (!Objects.equals(this.eventName, other.eventName)) return false;
            if (!Objects.equals(this.serviceName, other.serviceName)) return false;
            if (!Objects.equals(this.serviceMode, other.serviceMode)) return false;
            if (!Objects.equals(this.resultMapName, other.resultMapName)) return false;
            if (!Objects.equals(this.runAsUser, other.runAsUser)) return false;

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
