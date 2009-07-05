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
package org.ofbiz.minilang.method.ifops;

import java.util.*;

import javolution.util.FastList;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

import org.ofbiz.minilang.operation.*;

/**
 * Iff the comparison between the constant and the specified field is true process sub-operations
 */
public class IfCompare extends MethodOperation {
    public static final class IfCompareFactory implements Factory<IfCompare> {
        public IfCompare createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new IfCompare(element, simpleMethod);
        }

        public String getName() {
            return "if-compare";
        }
    }

    public static final String module = IfCompare.class.getName();

    protected List<MethodOperation> subOps = FastList.newInstance();
    protected List<MethodOperation> elseSubOps = null;

    protected ContextAccessor<Map<String, ? extends Object>> mapAcsr;
    protected ContextAccessor<Object> fieldAcsr;
    protected String value;

    protected String operator;
    protected String type;
    protected String format;

    public IfCompare(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        // NOTE: this is still supported, but is deprecated
        this.mapAcsr = new ContextAccessor<Map<String, ? extends Object>>(element.getAttribute("map-name"));
        this.fieldAcsr = new ContextAccessor<Object>(element.getAttribute("field"));
        if (this.fieldAcsr.isEmpty()) {
            // NOTE: this is still supported, but is deprecated
            this.fieldAcsr = new ContextAccessor<Object>(element.getAttribute("field-name"));
        }
        this.value = element.getAttribute("value");

        this.operator = element.getAttribute("operator");
        this.type = element.getAttribute("type");
        this.format = element.getAttribute("format");

        SimpleMethod.readOperations(element, subOps, simpleMethod);
        Element elseElement = UtilXml.firstChildElement(element, "else");
        if (elseElement != null) {
            elseSubOps = FastList.newInstance();
            SimpleMethod.readOperations(elseElement, elseSubOps, simpleMethod);
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) {
        // if conditions fails, always return true; if a sub-op returns false
        // return false and stop, otherwise return true

        String value = methodContext.expandString(this.value);
        String operator = methodContext.expandString(this.operator);
        String type = methodContext.expandString(this.type);
        String format = methodContext.expandString(this.format);

        Object fieldVal = null;
        if (!mapAcsr.isEmpty()) {
            Map<String, ? extends Object> fromMap = mapAcsr.get(methodContext);
            if (fromMap == null) {
                if (Debug.infoOn()) Debug.logInfo("Map not found with name " + mapAcsr + ", using empty string for comparison", module);
            } else {
                fieldVal = fieldAcsr.get(fromMap, methodContext);
            }
        } else {
            // no map name, try the env
            fieldVal = fieldAcsr.get(methodContext);
        }

        // always use an empty string by default
        if (fieldVal == null) {
            fieldVal = "";
        }

        List<Object> messages = FastList.newInstance();
        Boolean resultBool = BaseCompare.doRealCompare(fieldVal, value, operator, type, format, messages, null, methodContext.getLoader(), true);
        if (messages.size() > 0) {
            messages.add(0, "Error with comparison in if-compare between field [" + mapAcsr.toString() + "." + fieldAcsr.toString() + "] with value [" + fieldVal + "] and value [" + value + "] with operator [" + operator + "] and type [" + type + "]: ");
            if (methodContext.getMethodType() == MethodContext.EVENT) {
                StringBuilder fullString = new StringBuilder();

                for (Object message: messages) {
                    fullString.append(message);
                }
                Debug.logWarning(fullString.toString(), module);

                methodContext.putEnv(simpleMethod.getEventErrorMessageName(), fullString.toString());
                methodContext.putEnv(simpleMethod.getEventResponseCodeName(), simpleMethod.getDefaultErrorCode());
            } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
                methodContext.putEnv(simpleMethod.getServiceErrorMessageListName(), messages);
                methodContext.putEnv(simpleMethod.getServiceResponseMessageName(), simpleMethod.getDefaultErrorCode());
            }
            return false;
        }

        if (resultBool != null && resultBool.booleanValue()) {
            return SimpleMethod.runSubOps(subOps, methodContext);
        } else {
            if (elseSubOps != null) {
                return SimpleMethod.runSubOps(elseSubOps, methodContext);
            } else {
                return true;
            }
        }
    }

    public List<MethodOperation> getAllSubOps() {
        List<MethodOperation> allSubOps = FastList.newInstance();
        allSubOps.addAll(this.subOps);
        if (this.elseSubOps != null) allSubOps.addAll(this.elseSubOps);
        return allSubOps;
    }

    @Override
    public String rawString() {
        // TODO: add all attributes and other info
        return "<if-compare field-name=\"" + this.fieldAcsr + "\" map-name=\"" + this.mapAcsr + "\" value=\"" + value + "\"/>";
    }
    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
