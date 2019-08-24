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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.model.ModelUtil;
import org.w3c.dom.Element;

/**
 * ServiceEcaSetField
 */
public class ServiceEcaSetField {

    public static final String module = ServiceEcaSetField.class.getName();

    protected String fieldName = null;
    protected String mapName = null;
    protected String envName = null;
    protected String value = null;
    protected String format = null;

    public ServiceEcaSetField(Element set) {
        this.fieldName = set.getAttribute("field-name");
        if (UtilValidate.isNotEmpty(this.fieldName) && this.fieldName.indexOf('.') != -1) {
            this.mapName = fieldName.substring(0, this.fieldName.indexOf('.'));
            this.fieldName = this.fieldName.substring(this.fieldName.indexOf('.') +1);
        }
        this.envName = set.getAttribute("env-name");
        this.value = set.getAttribute("value");
        this.format = set.getAttribute("format");
    }

    public void eval(Map<String, Object> context) {
        if (fieldName != null) {
            // try to expand the envName
            if (UtilValidate.isNotEmpty(this.envName) && this.envName.startsWith("${")) {
                FlexibleStringExpander exp = FlexibleStringExpander.getInstance(this.envName);
                String s = exp.expandString(context);
                if (UtilValidate.isNotEmpty(s)) {
                    value = s;
                }
                Debug.logInfo("Expanded String: " + s, module);
            }
            // TODO: rewrite using the ContextAccessor.java see hack below to be able to use maps for email notifications
            // check if target is a map and create/get from contaxt
            Map<String, Object> valueMap = null;
            if (UtilValidate.isNotEmpty(this.mapName) && context.containsKey(this.mapName)) {
                valueMap = UtilGenerics.cast(context.get(mapName));
            } else {
                valueMap = new HashMap<>();
            }
            // process the context changes
            Object newValue = null;
            if (UtilValidate.isNotEmpty(this.value)) {
                newValue = this.format(this.value, context);
            } else if (UtilValidate.isNotEmpty(this.envName) && context.get(this.envName) != null) {
                newValue = this.format((String) context.get(this.envName), context);
            }

            if (newValue != null) {
                if (UtilValidate.isNotEmpty(this.mapName)) {
                    valueMap.put(this.fieldName, newValue);
                    context.put(this.mapName, valueMap);
                } else {
                    context.put(this.fieldName, newValue);
                }
            }
        }
    }

    protected Object format(String s, Map<String, ? extends Object> c) {
        if (UtilValidate.isEmpty(s) || UtilValidate.isEmpty(format)) {
            return s;
        }

        // string formats
        if ("append".equalsIgnoreCase(format) && envName != null) {
            StringBuilder newStr = new StringBuilder();
            if (c.get(envName) != null) {
                newStr.append(c.get(envName));
            }
            newStr.append(s);
            return newStr.toString();
        }
        if ("to-upper".equalsIgnoreCase(format)) {
            return s.toUpperCase(Locale.getDefault());
        }
        if ("to-lower".equalsIgnoreCase(format)) {
            return s.toLowerCase(Locale.getDefault());
        }
        if ("hash-code".equalsIgnoreCase(format)) {
            return s.hashCode();
        }
        if ("long".equalsIgnoreCase(format)) {
            return Long.valueOf(s);
        }
        if ("double".equalsIgnoreCase(format)) {
            return Double.valueOf(s);
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((envName == null) ? 0 : envName.hashCode());
        result = prime * result + ((fieldName == null) ? 0 : fieldName.hashCode());
        result = prime * result + ((format == null) ? 0 : format.hashCode());
        result = prime * result + ((mapName == null) ? 0 : mapName.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ServiceEcaSetField) {
            ServiceEcaSetField other = (ServiceEcaSetField) obj;

            if (!Objects.equals(this.fieldName, other.fieldName)) return false;
            if (!Objects.equals(this.envName, other.envName)) return false;
            if (!Objects.equals(this.value, other.value)) return false;
            if (!Objects.equals(this.format, other.format)) return false;

            return true;
        } else {
            return false;
        }
    }
}
