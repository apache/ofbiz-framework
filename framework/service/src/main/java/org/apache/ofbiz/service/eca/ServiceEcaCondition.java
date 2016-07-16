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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.w3c.dom.Element;

/**
 * ServiceEcaCondition
 */
@SuppressWarnings("serial")
public class ServiceEcaCondition implements java.io.Serializable {

    public static final String module = ServiceEcaCondition.class.getName();

    protected String conditionService = null;
    protected String lhsValueName = null;
    protected String rhsValueName = null;
    protected String lhsMapName = null;
    protected String rhsMapName = null;
    protected String operator = null;
    protected String compareType = null;
    protected String format = null;
    protected boolean isConstant = false;
    protected boolean isService = false;

    protected ServiceEcaCondition() {}

    public ServiceEcaCondition(Element condition, boolean isConstant, boolean isService) {
        if (isService) {
            this.isService = isService;
            this.conditionService = condition.getAttribute("service-name");
        } else {
            this.lhsValueName = condition.getAttribute("field-name");
            this.lhsMapName = condition.getAttribute("map-name");

            this.isConstant = isConstant;
            if (isConstant) {
                this.rhsValueName = condition.getAttribute("value");
                this.rhsMapName = null;
            } else {
                this.rhsValueName = condition.getAttribute("to-field-name");
                this.rhsMapName = condition.getAttribute("to-map-name");
            }

            this.operator = condition.getAttribute("operator");
            this.compareType = condition.getAttribute("type");
            this.format = condition.getAttribute("format");

            if (lhsValueName == null) {
                lhsValueName = "";
            }
            if (rhsValueName == null) {
                rhsValueName = "";
            }
        }
    }

    public String getShortDisplayDescription(boolean moreDetail) {
        StringBuilder buf = new StringBuilder();
        if (isService) {
            buf.append("[").append(conditionService).append("]");
        } else {
            buf.append("[");
            if (UtilValidate.isNotEmpty(lhsMapName)) buf.append(lhsMapName).append(".");
            buf.append(lhsValueName);
            buf.append(":").append(operator).append(":");
            if (UtilValidate.isNotEmpty(rhsMapName)) buf.append(rhsMapName).append(".");
            buf.append(rhsValueName);

            if (moreDetail) {
                if (UtilValidate.isNotEmpty(compareType)) {
                    buf.append("-");
                    buf.append(compareType);
                }
                if (UtilValidate.isNotEmpty(format)) {
                    buf.append(";");
                    buf.append(format);
                }
            }

            buf.append("]");
        }
        return buf.toString();
    }

    public boolean eval(String serviceName, DispatchContext dctx, Map<String, Object> context) throws GenericServiceException {
        if (serviceName == null || dctx == null || context == null || dctx.getClassLoader() == null) {
            throw new GenericServiceException("Cannot have null Service, Context or DispatchContext!");
        }

        if (Debug.verboseOn()) Debug.logVerbose(this.toString() + ", In the context: " + context, module);

        // condition-service; run the service and return the reply result
        if (isService) {
            LocalDispatcher dispatcher = dctx.getDispatcher();
            Map<String, Object> conditionServiceResult = dispatcher.runSync(conditionService,
                    UtilMisc.<String, Object>toMap("serviceContext", context, "serviceName", serviceName,
                            "userLogin", context.get("userLogin")));

            Boolean conditionReply = Boolean.FALSE;
            if (ServiceUtil.isError(conditionServiceResult)) {
                Debug.logError("Error in condition-service : " +
                        ServiceUtil.getErrorMessage(conditionServiceResult), module);
            } else {
                conditionReply = (Boolean) conditionServiceResult.get("conditionReply");
            }
            return conditionReply.booleanValue();
        }

        Object lhsValue = null;
        Object rhsValue = null;
        if (UtilValidate.isNotEmpty(lhsMapName)) {
            try {
                if (context.containsKey(lhsMapName)) {
                    Map<String, ? extends Object> envMap = UtilGenerics.checkMap(context.get(lhsMapName));
                    lhsValue = envMap.get(lhsValueName);
                } else {
                    Debug.logInfo("From Map (" + lhsMapName + ") not found in context, defaulting to null.", module);
                }
            } catch (ClassCastException e) {
                throw new GenericServiceException("From Map field [" + lhsMapName + "] is not a Map.", e);
            }
        } else {
            if (context.containsKey(lhsValueName)) {
                lhsValue = context.get(lhsValueName);
            } else {
                Debug.logInfo("From Field (" + lhsValueName + ") is not found in context for " + serviceName + ", defaulting to null.", module);
            }
        }

        if (isConstant) {
            rhsValue = rhsValueName;
        } else if (UtilValidate.isNotEmpty(rhsMapName)) {
            try {
                if (context.containsKey(rhsMapName)) {
                    Map<String, ? extends Object> envMap = UtilGenerics.checkMap(context.get(rhsMapName));
                    rhsValue = envMap.get(rhsValueName);
                } else {
                    Debug.logInfo("To Map (" + rhsMapName + ") not found in context for " + serviceName + ", defaulting to null.", module);
                }
            } catch (ClassCastException e) {
                throw new GenericServiceException("To Map field [" + rhsMapName + "] is not a Map.", e);
            }
        } else {
            if (context.containsKey(rhsValueName)) {
                rhsValue = context.get(rhsValueName);
            } else {
                Debug.logInfo("To Field (" + rhsValueName + ") is not found in context for " + serviceName + ", defaulting to null.", module);
            }
        }

        if (Debug.verboseOn()) Debug.logVerbose("Comparing : " + lhsValue + " " + operator + " " + rhsValue, module);

        // evaluate the condition & invoke the action(s)
        List<Object> messages = new LinkedList<Object>();
        Boolean cond = ObjectType.doRealCompare(lhsValue, rhsValue, operator, compareType, format, messages, null, dctx.getClassLoader(), isConstant);

        // if any messages were returned send them out
        if (messages.size() > 0 && Debug.warningOn()) {
            for (Object message: messages) {
                Debug.logWarning(message.toString(), module);
            }
        }
        if (cond != null) {
            return cond.booleanValue();
        } else {
            Debug.logWarning("doRealCompare returned null, returning false", module);
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        if (UtilValidate.isNotEmpty(conditionService)) buf.append("[").append(conditionService).append("]");
        if (UtilValidate.isNotEmpty(lhsMapName)) buf.append("[").append(lhsMapName).append("]");
        if (UtilValidate.isNotEmpty(lhsValueName)) buf.append("[").append(lhsValueName).append("]");
        if (UtilValidate.isNotEmpty(operator)) buf.append("[").append(operator).append("]");
        if (UtilValidate.isNotEmpty(rhsMapName)) buf.append("[").append(rhsMapName).append("]");
        if (UtilValidate.isNotEmpty(rhsValueName)) buf.append("[").append(rhsValueName).append("]");
        if (UtilValidate.isNotEmpty(isConstant)) buf.append("[").append(isConstant).append("]");
        if (UtilValidate.isNotEmpty(compareType)) buf.append("[").append(compareType).append("]");
        if (UtilValidate.isNotEmpty(format)) buf.append("[").append(format).append("]");
        return buf.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ServiceEcaCondition) {
            ServiceEcaCondition other = (ServiceEcaCondition) obj;

            if (!UtilValidate.areEqual(this.conditionService, other.conditionService)) return false;
            if (!UtilValidate.areEqual(this.lhsValueName, other.lhsValueName)) return false;
            if (!UtilValidate.areEqual(this.rhsValueName, other.rhsValueName)) return false;
            if (!UtilValidate.areEqual(this.lhsMapName, other.lhsMapName)) return false;
            if (!UtilValidate.areEqual(this.rhsMapName, other.rhsMapName)) return false;
            if (!UtilValidate.areEqual(this.operator, other.operator)) return false;
            if (!UtilValidate.areEqual(this.compareType, other.compareType)) return false;
            if (!UtilValidate.areEqual(this.format, other.format)) return false;

            if (this.isConstant != other.isConstant) return false;
            if (this.isService != other.isService) return false;

            return true;
        } else {
            return false;
        }
    }
}
