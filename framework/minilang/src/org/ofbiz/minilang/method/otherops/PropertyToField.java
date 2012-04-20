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
package org.ofbiz.minilang.method.otherops;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Copies an properties file property value to a field
 */
public class PropertyToField extends MethodOperation {

    public static final String module = PropertyToField.class.getName();

    ContextAccessor<List<? extends Object>> argListAcsr;
    String defaultVal;
    ContextAccessor<Object> fieldAcsr;
    ContextAccessor<Map<String, Object>> mapAcsr;
    boolean noLocale;
    String property;
    String resource;

    public PropertyToField(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        resource = element.getAttribute("resource");
        property = element.getAttribute("property");
        // the schema for this element now just has the "field" attribute, though the old "field-name" and "map-name" pair is still supported
        this.fieldAcsr = new ContextAccessor<Object>(element.getAttribute("field"), element.getAttribute("field-name"));
        this.mapAcsr = new ContextAccessor<Map<String, Object>>(element.getAttribute("map-name"));
        defaultVal = element.getAttribute("default");
        // defaults to false, ie anything but true is false
        noLocale = "true".equals(element.getAttribute("no-locale"));
        argListAcsr = new ContextAccessor<List<? extends Object>>(element.getAttribute("arg-list-name"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        String resource = methodContext.expandString(this.resource);
        String property = methodContext.expandString(this.property);
        String value = null;
        if (noLocale) {
            value = EntityUtilProperties.getPropertyValue(resource, property, methodContext.getDelegator());
        } else {
            value = EntityUtilProperties.getMessage(resource, property, methodContext.getLocale(), methodContext.getDelegator());
        }
        if (UtilValidate.isEmpty(value)) {
            value = defaultVal;
        }
        // note that expanding the value string here will handle defaultValue and the string from
        // the properties file; if we decide later that we don't want the string from the properties
        // file to be expanded we should just expand the defaultValue at the beginning of this method.
        value = methodContext.expandString(value);
        if (!argListAcsr.isEmpty()) {
            List<? extends Object> argList = argListAcsr.get(methodContext);
            if (UtilValidate.isNotEmpty(argList)) {
                value = MessageFormat.format(value, argList.toArray());
            }
        }
        if (!mapAcsr.isEmpty()) {
            Map<String, Object> toMap = mapAcsr.get(methodContext);
            if (toMap == null) {
                if (Debug.infoOn())
                    Debug.logInfo("Map not found with name " + mapAcsr + ", creating new map", module);
                toMap = FastMap.newInstance();
                mapAcsr.put(methodContext, toMap);
            }
            fieldAcsr.put(toMap, value, methodContext);
        } else {
            fieldAcsr.put(methodContext, value);
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
        return "<property-to-field field-name=\"" + this.fieldAcsr + "\" map-name=\"" + this.mapAcsr + "\"/>";
    }

    public static final class PropertyToFieldFactory implements Factory<PropertyToField> {
        public PropertyToField createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new PropertyToField(element, simpleMethod);
        }

        public String getName() {
            return "property-to-field";
        }
    }
}
