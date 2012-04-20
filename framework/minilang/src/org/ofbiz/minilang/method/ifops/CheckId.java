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

import java.util.List;
import java.util.Map;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * If the given ID field is not valid the fail-message or fail-property sub-elements are used to add a message to the error-list.
 */
public class CheckId extends MethodOperation {

    public static final String module = CheckId.class.getName();

    ContextAccessor<List<Object>> errorListAcsr;
    ContextAccessor<Object> fieldAcsr;
    boolean isProperty = false;
    ContextAccessor<Map<String, ? extends Object>> mapAcsr;
    String message = null;
    String propertyResource = null;

    public CheckId(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        // the schema for this element now just has the "field" attribute, though the old "field-name" and "map-name" pair is still supported
        this.fieldAcsr = new ContextAccessor<Object>(element.getAttribute("field"), element.getAttribute("field-name"));
        this.mapAcsr = new ContextAccessor<Map<String, ? extends Object>>(element.getAttribute("map-name"));
        this.errorListAcsr = new ContextAccessor<List<Object>>(element.getAttribute("error-list-name"), "error_list");
        // note: if no fail-message or fail-property then message will be null
        Element failMessage = UtilXml.firstChildElement(element, "fail-message");
        Element failProperty = UtilXml.firstChildElement(element, "fail-property");
        if (failMessage != null) {
            this.message = failMessage.getAttribute("message");
            this.isProperty = false;
        } else if (failProperty != null) {
            this.propertyResource = failProperty.getAttribute("resource");
            this.message = failProperty.getAttribute("property");
            this.isProperty = true;
        }
    }

    public void addMessage(List<Object> messages, MethodContext methodContext, String defaultMessage, String errorDetails) {
        String message = methodContext.expandString(this.message);
        String propertyResource = methodContext.expandString(this.propertyResource);
        if (!isProperty && message != null) {
            messages.add(message + errorDetails);
        } else if (isProperty && propertyResource != null && message != null) {
            // String propMsg = UtilProperties.getPropertyValue(UtilURL.fromResource(propertyResource, loader), message);
            String propMsg = UtilProperties.getMessage(propertyResource, message, methodContext.getEnvMap(), methodContext.getLocale());
            if (UtilValidate.isEmpty(propMsg)) {
                messages.add(defaultMessage + errorDetails);
            } else {
                messages.add(methodContext.expandString(propMsg) + errorDetails);
            }
        } else {
            messages.add(defaultMessage + errorDetails);
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        boolean isValid = true;
        List<Object> messages = errorListAcsr.get(methodContext);
        if (messages == null) {
            messages = FastList.newInstance();
            errorListAcsr.put(methodContext, messages);
        }
        Object fieldVal = null;
        if (!mapAcsr.isEmpty()) {
            Map<String, ? extends Object> fromMap = mapAcsr.get(methodContext);
            if (fromMap == null) {
                if (Debug.infoOn())
                    Debug.logInfo("Map not found with name " + mapAcsr + ", running operations", module);
            } else {
                fieldVal = fieldAcsr.get(fromMap, methodContext);
            }
        } else {
            // no map name, try the env
            fieldVal = fieldAcsr.get(methodContext);
        }
        String fieldStr = fieldVal.toString();
        StringBuilder errorDetails = new StringBuilder();
        // check various illegal characters, etc for ids
        isValid = UtilValidate.isValidDatabaseId(fieldStr, errorDetails);
        if (!isValid) {
            this.addMessage(messages, methodContext, "The ID value in the field [" + fieldAcsr + "] was not valid", ": " + errorDetails.toString());
        }
        return true;
    }

    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }

    @Override
    public String rawString() {
        // TODO: add all attributes and other info
        return "<check-id field-name=\"" + this.fieldAcsr + "\" map-name=\"" + this.mapAcsr + "\"/>";
    }

    public static final class CheckIdFactory implements Factory<CheckId> {
        public CheckId createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new CheckId(element, simpleMethod);
        }

        public String getName() {
            return "check-id";
        }
    }
}
