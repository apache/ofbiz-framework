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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.MiniLangValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.ofbiz.minilang.operation.BaseCompare;
import org.w3c.dom.Element;

/**
 * Iff the comparison between the constant and the specified field is true process sub-operations
 */
public class IfCompare extends MethodOperation {

    public static final String module = IfCompare.class.getName();

    protected List<MethodOperation> elseSubOps = null;
    protected ContextAccessor<Object> fieldAcsr;
    protected String format;
    protected ContextAccessor<Map<String, ? extends Object>> mapAcsr;
    protected String operator;
    protected List<MethodOperation> subOps;
    protected String type;
    protected String value;

    public IfCompare(Element element, SimpleMethod simpleMethod) throws MiniLangException {
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
        this.type = MiniLangValidate.checkAttribute(element.getAttribute("type"), "PlainString");
        this.format = element.getAttribute("format");
        this.subOps = Collections.unmodifiableList(SimpleMethod.readOperations(element, simpleMethod));
        Element elseElement = UtilXml.firstChildElement(element, "else");
        if (elseElement != null) {
            this.elseSubOps = Collections.unmodifiableList(SimpleMethod.readOperations(elseElement, simpleMethod));
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
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
                if (Debug.infoOn())
                    Debug.logInfo("Map not found with name " + mapAcsr + ", using empty string for comparison", module);
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
                for (Object message : messages) {
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

    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }

    public List<MethodOperation> getAllSubOps() {
        List<MethodOperation> allSubOps = FastList.newInstance();
        allSubOps.addAll(this.subOps);
        if (this.elseSubOps != null)
            allSubOps.addAll(this.elseSubOps);
        return allSubOps;
    }

    @Override
    public String rawString() {
        // TODO: add all attributes and other info
        return "<if-compare field-name=\"" + this.fieldAcsr + "\" map-name=\"" + this.mapAcsr + "\" value=\"" + value + "\"/>";
    }

    public static final class IfCompareFactory implements Factory<IfCompare> {
        public IfCompare createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new IfCompare(element, simpleMethod);
        }

        public String getName() {
            return "if-compare";
        }
    }
}
