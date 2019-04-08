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
package org.apache.ofbiz.minilang.operation;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.UtilValidate;
import org.w3c.dom.Element;

/**
 * Convert the current field from the in-map and place it in the out-map
 */
public class Convert extends SimpleMapOperation {

    public static final String module = Convert.class.getName();

    String format;
    boolean replace = true;
    boolean setIfNull = true;
    String toField;
    String type;

    public Convert(Element element, SimpleMapProcess simpleMapProcess) {
        super(element, simpleMapProcess);
        this.toField = element.getAttribute("to-field");
        if (UtilValidate.isEmpty(this.toField)) {
            this.toField = this.fieldName;
        }
        type = element.getAttribute("type");
        // if anything but false it will be true
        replace = !"false".equals(element.getAttribute("replace"));
        // if anything but false it will be true
        setIfNull = !"false".equals(element.getAttribute("set-if-null"));
        format = element.getAttribute("format");
    }

    @Override
    public void exec(Map<String, Object> inMap, Map<String, Object> results, List<Object> messages, Locale locale, ClassLoader loader) {
        Object fieldObject = inMap.get(fieldName);
        if (fieldObject == null) {
            if (setIfNull && (replace || !results.containsKey(toField)))
                results.put(toField, null);
            return;
        }
        // if an incoming string is empty,
        // set to null if setIfNull is true, otherwise do nothing, ie treat as if null
        if (fieldObject instanceof java.lang.String) {
            if (((String) fieldObject).length() == 0) {
                if (setIfNull && (replace || !results.containsKey(toField)))
                    results.put(toField, null);
                return;
            }
        }
        Object convertedObject = null;
        try {
            convertedObject = ObjectType.simpleTypeConvert(fieldObject, type, format, locale);
        } catch (GeneralException e) {
            addMessage(messages, loader, locale);
            Debug.logError(e, "Error in convert simple-map-processor operation: " + e.toString(), module);
            return;
        }
        if (convertedObject == null)
            return;
        if (replace) {
            results.put(toField, convertedObject);
            // if (Debug.infoOn()) Debug.logInfo("[SimpleMapProcessor.Converted.exec] Put converted value \"" + convertedObject + "\" in field \"" + toField + "\"", module);
        } else {
            if (results.containsKey(toField)) {// do nothing
            } else {
                results.put(toField, convertedObject);
                // if (Debug.infoOn()) Debug.logInfo("[SimpleMapProcessor.Converted.exec] Put converted value \"" + convertedObject + "\" in field \"" + toField + "\"", module);
            }
        }
    }
}
