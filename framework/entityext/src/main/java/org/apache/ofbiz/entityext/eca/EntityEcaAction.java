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
package org.apache.ofbiz.entityext.eca;

import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.w3c.dom.Element;

/**
 * EntityEcaAction
 */
@SuppressWarnings("serial")
public final class EntityEcaAction implements java.io.Serializable {
    public static final String module = EntityEcaAction.class.getName();

    private final String serviceName;
    private final String serviceMode;
    private final String runAsUser;
    private final String valueAttr;
    private final boolean resultToValue;
    private final boolean abortOnError;
    private final boolean rollbackOnError;
    private final boolean persist;

    public EntityEcaAction(Element action) {
        this.serviceName = action.getAttribute("service");
        this.serviceMode = action.getAttribute("mode");
        // default is true, so anything but false is true
        this.resultToValue = !"false".equals(action.getAttribute("result-to-value"));
        // default is false, so anything but true is false
        this.abortOnError = "true".equals(action.getAttribute("abort-on-error"));
        this.rollbackOnError = "true".equals(action.getAttribute("rollback-on-error"));
        this.persist = "true".equals(action.getAttribute("persist"));
        this.runAsUser = action.getAttribute("run-as-user");
        this.valueAttr = action.getAttribute("value-attr");
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public void runAction(DispatchContext dctx, Map<String, ? extends Object> context, GenericEntity newValue) throws GenericEntityException {
        try {
            // pull out context parameters needed for this service.
            Map<String, Object> actionContext = dctx.getModelService(serviceName).makeValid(context, ModelService.IN_PARAM);
            // if value-attr is specified, insert the value object in that attr name
            if (!valueAttr.isEmpty()) {
                actionContext.put(valueAttr, newValue);
            }

            //Debug.logInfo("Running Entity ECA action service " + this.serviceName + " triggered by entity: " + value.getEntityName(), module);
            //Debug.logInfo("Running Entity ECA action service " + this.serviceName + "; value=" + value + "; actionContext=" + actionContext, module);

            // setup the run-as-user
            GenericValue userLoginToRunAs = null;
            if (!this.runAsUser.isEmpty()) {
                userLoginToRunAs = dctx.getDelegator().findOne("UserLogin", UtilMisc.toMap("userLoginId", this.runAsUser), true);
                if (userLoginToRunAs != null) {
                    actionContext.put("userLogin", userLoginToRunAs);
                }
            }

            LocalDispatcher dispatcher = dctx.getDispatcher();
            if ("sync".equals(this.serviceMode)) {
                Map<String, Object> actionResult = dispatcher.runSync(this.serviceName, actionContext);
                if (ServiceUtil.isError(actionResult)) {
                    throw new GenericServiceException("Error running Entity ECA action service: " + ServiceUtil.getErrorMessage(actionResult));
                }
                // use the result to update the context fields.
                if (resultToValue) {
                    newValue.setNonPKFields(actionResult);
                }
            } else if ("async".equals(this.serviceMode)) {
                dispatcher.runAsync(serviceName, actionContext, persist);
            }
        } catch (GenericServiceException e) {
            // check abortOnError and rollbackOnError
            if (rollbackOnError) {
                String errMsg = "Entity ECA action service failed and rollback-on-error is true, so setting rollback only.";
                Debug.logError(errMsg, module);
                TransactionUtil.setRollbackOnly(errMsg, e);
            }

            if (this.abortOnError) {
                throw new EntityEcaException("Error running Entity ECA action service: " + e.toString(), e);
            } else {
                Debug.logError(e, "Error running Entity ECA action service", module);
            }
        }
    }
}
