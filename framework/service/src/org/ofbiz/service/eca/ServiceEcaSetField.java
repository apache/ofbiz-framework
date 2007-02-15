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

package org.ofbiz.service.eca;

import org.w3c.dom.Element;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.model.ModelUtil;

import java.util.Map;

/**
 * ServiceEcaSetField
 */
public class ServiceEcaSetField {

    public static final String module = ServiceEcaSetField.class.getName();

    protected String fieldName = null;
    protected String envName = null;
    protected String value = null;
    protected String format = null;

    public ServiceEcaSetField(Element set) {
        this.fieldName = set.getAttribute("field-name");
        this.envName = set.getAttribute("env-name");
        this.value = set.getAttribute("value");
        this.format = set.getAttribute("format");
    }

    public void eval(Map context) {
        if (fieldName != null) {
            // try to expand the envName
            if (UtilValidate.isEmpty(value)) {
                if (UtilValidate.isNotEmpty(envName) && envName.startsWith("${")) {
                    FlexibleStringExpander exp = new FlexibleStringExpander(envName);
                    String s = exp.expandString(context);
                    if (UtilValidate.isNotEmpty(s)) {
                        value = s;
                    }
                    Debug.log("Expanded String: " + s, module);
                }
            }

            // process the context changes
            if (UtilValidate.isNotEmpty(value)) {
                context.put(fieldName, this.format(value, context));            
            } else if (UtilValidate.isNotEmpty(envName) && context.get(envName) != null) {
                context.put(fieldName, this.format((String) context.get(envName), context));
            }
        }
    }

    protected Object format(String s, Map c) {
        if (UtilValidate.isEmpty(s) || UtilValidate.isEmpty(format)) {            
            return s;
        }

        // string formats
        if ("append".equalsIgnoreCase(format) && envName != null) {
            String newStr = "";
            if (c.get(envName) != null) {
                newStr = newStr + c.get(envName);
            }
            newStr = newStr + s;
            return newStr; 
        }
        if ("to-upper".equalsIgnoreCase(format)) {
            return s.toUpperCase();
        }
        if ("to-lower".equalsIgnoreCase(format)) {
            return s.toLowerCase();
        }
        if ("hash-code".equalsIgnoreCase(format)) {
            return new Integer(s.hashCode());
        }
        if ("long".equalsIgnoreCase(format)) {
            return new Long(s);
        }
        if ("double".equalsIgnoreCase(format)) {
            return new Double(s);
        }

        // entity formats
        if ("upper-first-char".equalsIgnoreCase(format)) {
            return ModelUtil.upperFirstChar(s);
        }
        if ("lower-first-char".equalsIgnoreCase(format)) {
            return ModelUtil.lowerFirstChar(s);
        }
        if ("db-to-java".equalsIgnoreCase(format)) {
            return ModelUtil.dbNameToVarName(s);
        }
        if ("java-to-db".equalsIgnoreCase(format)) {
            return ModelUtil.javaNameToDbName(s);
        }

        Debug.logWarning("Format function not found [" + format + "] return string unchanged - " + s, module);
        return s;
    }
}
