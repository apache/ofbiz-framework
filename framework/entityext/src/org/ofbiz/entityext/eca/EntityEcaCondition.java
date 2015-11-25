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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.service.DispatchContext;
import org.w3c.dom.Element;

/**
 * EntityEcaCondition
 */
@SuppressWarnings("serial")
public final class EntityEcaCondition implements java.io.Serializable {

    public static final String module = EntityEcaCondition.class.getName();

    private final String lhsValueName, rhsValueName;
    private final String operator;
    private final String compareType;
    private final String format;
    private final boolean constant;

    public EntityEcaCondition(Element condition, boolean constant) {
        this.lhsValueName = condition.getAttribute("field-name");
        this.constant = constant;
        if (constant) {
            this.rhsValueName = condition.getAttribute("value");
        } else {
            this.rhsValueName = condition.getAttribute("to-field-name");
        }
        this.operator = condition.getAttribute("operator");
        this.compareType = condition.getAttribute("type");
        this.format = condition.getAttribute("format");
    }

    public boolean eval(DispatchContext dctx, GenericEntity value) throws GenericEntityException {
        if (dctx == null || value == null || dctx.getClassLoader() == null) {
            throw new GenericEntityException("Cannot have null Value or DispatchContext!");
        }

        if (Debug.verboseOn()) Debug.logVerbose(this.toString(), module);

        Object lhsValue = value.get(lhsValueName);

        Object rhsValue;
        if (constant) {
            rhsValue = rhsValueName;
        } else {
            rhsValue = value.get(rhsValueName);
        }

        if (Debug.verboseOn()) Debug.logVerbose("Comparing : " + lhsValue + " " + operator + " " + rhsValue, module);

        // evaluate the condition & invoke the action(s)
        List<Object> messages = new LinkedList<Object>();
        Boolean cond = ObjectType.doRealCompare(lhsValue, rhsValue, operator, compareType, format, messages, null, dctx.getClassLoader(), constant);

        // if any messages were returned send them out
        if (messages.size() > 0) {
            for (Object message: messages) {
                Debug.logWarning((String) message, module);
            }
        }
        if (cond != null) {
            return cond.booleanValue();
        } else {
            return false;
        }
    }

    public String getLValue() {
        return this.lhsValueName;
    }

    public String getRValue() {
        if (constant && !rhsValueName.isEmpty()) {
            return "\"".concat(this.rhsValueName).concat("\"");
        }
        return this.rhsValueName;
    }

    public String getOperator() {
        return this.operator;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("[").append(lhsValueName).append("]");
        buf.append("[").append(operator).append("]");
        buf.append("[").append(rhsValueName).append("]");
        buf.append("[").append(constant).append("]");
        buf.append("[").append(compareType).append("]");
        buf.append("[").append(format).append("]");
        return buf.toString();
    }
    
    protected List<String> getFieldNames() {
        List<String> fieldNameList = new ArrayList<String>();
        if( UtilValidate.isNotEmpty(lhsValueName) ) {
            fieldNameList.add(lhsValueName);
        }
        if( !constant && UtilValidate.isNotEmpty(rhsValueName)) {
            fieldNameList.add(rhsValueName);
        }
        return fieldNameList;
    }

}
