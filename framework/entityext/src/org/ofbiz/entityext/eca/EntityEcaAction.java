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
package org.ofbiz.entityext.eca;

import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.w3c.dom.Element;

/**
 * EntityEcaAction
 */
public class EntityEcaAction implements java.io.Serializable {
    public static final String module = EntityEcaAction.class.getName();

    protected String serviceName = null;
    protected String serviceMode = null;
    protected String runAsUser = null;
    protected String valueAttr = null;
    protected boolean resultToValue = true;
    protected boolean abortOnError = false;
    protected boolean rollbackOnError = false;
    protected boolean persist = false;

    protected EntityEcaAction() {}

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

    public void runAction(DispatchContext dctx, GenericEntity value) throws GenericEntityException {
        Map actionResult = null;
        
        try {
            // pull out context parameters needed for this service.
            Map actionContext = dctx.getModelService(serviceName).makeValid(value, ModelService.IN_PARAM);
            // if value-attr is specified, insert the value object in that attr name
            if (valueAttr != null && valueAttr.length() > 0) {
                actionContext.put(valueAttr, value);
            }
            
            //Debug.logInfo("Running Entity ECA action service " + this.serviceName + " triggered by entity: " + value.getEntityName(), module);
            //Debug.logInfo("Running Entity ECA action service " + this.serviceName + "; value=" + value + "; actionContext=" + actionContext, module);

            // setup the run-as-user
            GenericValue userLoginToRunAs = null;
            if (UtilValidate.isNotEmpty(this.runAsUser)) {
                userLoginToRunAs = dctx.getDelegator().findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", this.runAsUser));
                if (userLoginToRunAs != null) {
                    actionContext.put("userLogin", userLoginToRunAs);
                }
            }

            LocalDispatcher dispatcher = dctx.getDispatcher();
            if ("sync".equals(this.serviceMode)) {
                actionResult = dispatcher.runSync(this.serviceName, actionContext);
                if (ServiceUtil.isError(actionResult)) {
                    throw new GenericServiceException("Error running Entity ECA action service: " + ServiceUtil.getErrorMessage(actionResult));
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

        // use the result to update the context fields.
        if (resultToValue) {
            value.setNonPKFields(actionResult);
        }
    }
}
