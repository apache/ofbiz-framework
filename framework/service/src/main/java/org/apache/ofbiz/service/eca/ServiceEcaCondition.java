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
import java.util.Objects;

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

    private static final String MODULE = ServiceEcaCondition.class.getName();

    private String conditionService = null;
    private String lhsValueName = null;
    private String rhsValueName = null;
    private String lhsMapName = null;
    private String rhsMapName = null;
    private String operator = null;
    private String compareType = null;
    private String format = null;
    private boolean isConstant = false;
    private boolean isService = false;

    protected ServiceEcaCondition() { }

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

        }
    }

    /**
     * Gets short display description.
     * @param moreDetail the more detail
     * @return the short display description
     */
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

    /**
     * Eval boolean.
     * @param serviceName the service name
     * @param dctx the dctx
     * @param context the context
     * @return the boolean
     * @throws GenericServiceException the generic service exception
     */
    public boolean eval(String serviceName, DispatchContext dctx, Map<String, Object> context) throws GenericServiceException {
        if (serviceName == null || dctx == null || context == null || dctx.getClassLoader() == null) {
            throw new GenericServiceException("Cannot have null Service, Context or DispatchContext!");
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose(this.toString() + ", In the context: " + context, MODULE);
        }

        // condition-service; run the service and return the reply result
        if (isService) {
            LocalDispatcher dispatcher = dctx.getDispatcher();
            Map<String, Object> conditionServiceResult = dispatcher.runSync(conditionService,
                    UtilMisc.<String, Object>toMap("serviceContext", context, "serviceName", serviceName,
                            "userLogin", context.get("userLogin")));

            Boolean conditionReply = Boolean.FALSE;
            if (ServiceUtil.isError(conditionServiceResult)) {
                Debug.logError("Error in condition-service : "
                        + ServiceUtil.getErrorMessage(conditionServiceResult), MODULE);
            } else {
                conditionReply = (Boolean) conditionServiceResult.get("conditionReply");
            }
            return conditionReply;
        }

        Object lhsValue = null;
        Object rhsValue = null;
        if (UtilValidate.isNotEmpty(lhsMapName)) {
            try {
                if (context.containsKey(lhsMapName)) {
                    Map<String, ? extends Object> envMap = UtilGenerics.cast(context.get(lhsMapName));
                    lhsValue = envMap.get(lhsValueName);
                } else {
                    Debug.logInfo("From Map (" + lhsMapName + ") not found in context, defaulting to null.", MODULE);
                }
            } catch (ClassCastException e) {
                throw new GenericServiceException("From Map field [" + lhsMapName + "] is not a Map.", e);
            }
        } else {
            if (context.containsKey(lhsValueName)) {
                lhsValue = context.get(lhsValueName);
            } else {
                Debug.logInfo("From Field (" + lhsValueName + ") is not found in context for " + serviceName + ", defaulting to null.", MODULE);
            }
        }

        if (isConstant) {
            rhsValue = rhsValueName;
        } else if (UtilValidate.isNotEmpty(rhsMapName)) {
            try {
                if (context.containsKey(rhsMapName)) {
                    Map<String, ? extends Object> envMap = UtilGenerics.cast(context.get(rhsMapName));
                    rhsValue = envMap.get(rhsValueName);
                } else {
                    Debug.logInfo("To Map (" + rhsMapName + ") not found in context for " + serviceName + ", defaulting to null.", MODULE);
                }
            } catch (ClassCastException e) {
                throw new GenericServiceException("To Map field [" + rhsMapName + "] is not a Map.", e);
            }
        } else {
            if (context.containsKey(rhsValueName)) {
                rhsValue = context.get(rhsValueName);
            } else {
                Debug.logInfo("To Field (" + rhsValueName + ") is not found in context for " + serviceName + ", defaulting to null.", MODULE);
            }
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("Comparing : " + lhsValue + " " + operator + " " + rhsValue, MODULE);
        }

        // evaluate the condition & invoke the action(s)
        List<Object> messages = new LinkedList<>();
        Boolean cond = ObjectType.doRealCompare(lhsValue, rhsValue, operator, compareType, format, messages, null, dctx.getClassLoader(), isConstant);

        // if any messages were returned send them out
        if (!messages.isEmpty() && Debug.warningOn()) {
            for (Object message: messages) {
                Debug.logWarning(message.toString(), MODULE);
            }
        }
        if (!cond) {
            Debug.logWarning("doRealCompare returned null, returning false", MODULE);
        }
        return cond;
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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((compareType == null) ? 0 : compareType.hashCode());
        result = prime * result + ((conditionService == null) ? 0 : conditionService.hashCode());
        result = prime * result + ((format == null) ? 0 : format.hashCode());
        result = prime * result + (isConstant ? 1231 : 1237);
        result = prime * result + (isService ? 1231 : 1237);
        result = prime * result + ((lhsMapName == null) ? 0 : lhsMapName.hashCode());
        result = prime * result + ((lhsValueName == null) ? 0 : lhsValueName.hashCode());
        result = prime * result + ((operator == null) ? 0 : operator.hashCode());
        result = prime * result + ((rhsMapName == null) ? 0 : rhsMapName.hashCode());
        result = prime * result + ((rhsValueName == null) ? 0 : rhsValueName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ServiceEcaCondition) {
            ServiceEcaCondition other = (ServiceEcaCondition) obj;

            if (!Objects.equals(this.conditionService, other.conditionService)) return false;
            if (!Objects.equals(this.lhsValueName, other.lhsValueName)) return false;
            if (!Objects.equals(this.rhsValueName, other.rhsValueName)) return false;
            if (!Objects.equals(this.lhsMapName, other.lhsMapName)) return false;
            if (!Objects.equals(this.rhsMapName, other.rhsMapName)) return false;
            if (!Objects.equals(this.operator, other.operator)) return false;
            if (!Objects.equals(this.compareType, other.compareType)) return false;
            if (!Objects.equals(this.format, other.format)) return false;

            if (this.isConstant != other.isConstant) return false;
            if (this.isService != other.isService) return false;

            return true;
        } else {
            return false;
        }
    }
}
