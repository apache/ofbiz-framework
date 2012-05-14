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
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.MiniLangRuntimeException;
import org.ofbiz.minilang.MiniLangUtil;
import org.ofbiz.minilang.MiniLangValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.ofbiz.minilang.method.conditional.Compare;
import org.w3c.dom.Element;

/**
 * If the comparison between the specified field and the other field is true process sub-operations
 */
public class IfCompareField extends MethodOperation {

    public static final String module = IfCompareField.class.getName();

    private final Compare compare;
    protected List<MethodOperation> elseSubOps = null;
    protected ContextAccessor<Object> fieldAcsr;
    protected String format;
    protected ContextAccessor<Map<String, ? extends Object>> mapAcsr;
    protected String operator;
    protected List<MethodOperation> subOps;
    protected ContextAccessor<Object> toFieldAcsr;
    protected ContextAccessor<Map<String, ? extends Object>> toMapAcsr;
    private final Class<?> targetClass;
    protected String type;

    public IfCompareField(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        // NOTE: this is still supported, but is deprecated
        this.mapAcsr = new ContextAccessor<Map<String, ? extends Object>>(element.getAttribute("map-name"));
        this.fieldAcsr = new ContextAccessor<Object>(element.getAttribute("field"));
        if (this.fieldAcsr.isEmpty()) {
            // NOTE: this is still supported, but is deprecated
            this.fieldAcsr = new ContextAccessor<Object>(element.getAttribute("field-name"));
        }
        // NOTE: this is still supported, but is deprecated
        this.toMapAcsr = new ContextAccessor<Map<String, ? extends Object>>(element.getAttribute("to-map-name"));
        // set fieldAcsr to their defualt value of fieldAcsr if empty
        this.toFieldAcsr = new ContextAccessor<Object>(element.getAttribute("to-field"), element.getAttribute("field"));
        if (this.toFieldAcsr.isEmpty()) {
            // NOTE: this is still supported, but is deprecated
            this.toFieldAcsr = new ContextAccessor<Object>(element.getAttribute("to-field-name"), element.getAttribute("field-name"));
        }
        // do NOT default the to-map-name to the map-name because that
        // would make it impossible to compare from a map field to an
        // environment field
        this.operator = element.getAttribute("operator");
        this.compare = Compare.getInstance(this.operator);
        if (this.compare == null) {
            MiniLangValidate.handleError("Invalid operator " + this.operator, simpleMethod, element);
        }
        this.type = element.getAttribute("type");
        Class<?> targetClass = null;
        if (!this.type.isEmpty()) {
            try {
                targetClass = ObjectType.loadClass(this.type);
            } catch (ClassNotFoundException e) {
                MiniLangValidate.handleError("Invalid type " + this.type, simpleMethod, element);
            }
        }
        this.targetClass = targetClass;
        this.format = element.getAttribute("format");
        this.subOps = Collections.unmodifiableList(SimpleMethod.readOperations(element, simpleMethod));
        Element elseElement = UtilXml.firstChildElement(element, "else");
        if (elseElement != null) {
            this.elseSubOps = Collections.unmodifiableList(SimpleMethod.readOperations(elseElement, simpleMethod));
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        if (this.compare == null) {
            throw new MiniLangRuntimeException("Invalid operator " + this.operator, this);
        }
        // if conditions fails, always return true; if a sub-op returns false
        // return false and stop, otherwise return true
        String format = methodContext.expandString(this.format);
        Object fieldVal1 = null;
        Object fieldVal2 = null;
        if (!mapAcsr.isEmpty()) {
            Map<String, ? extends Object> fromMap = mapAcsr.get(methodContext);
            if (fromMap == null) {
                if (Debug.infoOn())
                    Debug.logInfo("Map not found with name " + mapAcsr + ", using null for comparison", module);
            } else {
                fieldVal1 = fieldAcsr.get(fromMap, methodContext);
            }
        } else {
            // no map name, try the env
            fieldVal1 = fieldAcsr.get(methodContext);
        }
        if (!toMapAcsr.isEmpty()) {
            Map<String, ? extends Object> toMap = toMapAcsr.get(methodContext);
            if (toMap == null) {
                if (Debug.infoOn())
                    Debug.logInfo("To Map not found with name " + toMapAcsr + ", using null for comparison", module);
            } else {
                fieldVal2 = toFieldAcsr.get(toMap, methodContext);
            }
        } else {
            // no map name, try the env
            fieldVal2 = toFieldAcsr.get(methodContext);
        }
        Class<?> targetClass = this.targetClass;
        if (targetClass == null) {
            targetClass = MiniLangUtil.getObjectClassForConversion(fieldVal1);
        }
        boolean result = false;
        try {
            result = this.compare.doCompare(fieldVal1, fieldVal2, targetClass, methodContext.getLocale(), methodContext.getTimeZone(), format);
        } catch (Exception e) {
            if (methodContext.getMethodType() == MethodContext.EVENT) {
                methodContext.putEnv(simpleMethod.getEventErrorMessageName(), e.getMessage());
                methodContext.putEnv(simpleMethod.getEventResponseCodeName(), simpleMethod.getDefaultErrorCode());
            } else {
                methodContext.putEnv(simpleMethod.getServiceErrorMessageListName(), e.getMessage());
                methodContext.putEnv(simpleMethod.getServiceResponseMessageName(), simpleMethod.getDefaultErrorCode());
            }
            return false;
        }
        if (result) {
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
        return "<if-compare-field field-name=\"" + this.fieldAcsr + "\" map-name=\"" + this.mapAcsr + "\"/>";
    }

    public static final class IfCompareFieldFactory implements Factory<IfCompareField> {
        public IfCompareField createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new IfCompareField(element, simpleMethod);
        }

        public String getName() {
            return "if-compare-field";
        }
    }
}
