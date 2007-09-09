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
package org.ofbiz.entity.model;

import java.io.Serializable;
import java.util.*;
import org.w3c.dom.*;

import org.ofbiz.base.util.*;

/**
 * Generic Entity - FieldType model class
 *
 */
public class ModelFieldType implements Serializable {

    /** The type of the Field */
    protected String type = null;

    /** The java-type of the Field */
    protected String javaType = null;

    /** The sql-type of the Field */
    protected String sqlType = null;

    /** The sql-type-alias of the Field, this is optional */
    protected String sqlTypeAlias = null;

    /** validators to be called when an update is done */
    protected List validators = new ArrayList();

    /** Default Constructor */
    public ModelFieldType() {}

    /** XML Constructor */
    public ModelFieldType(Element fieldTypeElement) {
        this.type = UtilXml.checkEmpty(fieldTypeElement.getAttribute("type"));
        this.javaType = UtilXml.checkEmpty(fieldTypeElement.getAttribute("java-type"));
        this.sqlType = UtilXml.checkEmpty(fieldTypeElement.getAttribute("sql-type")).toUpperCase();
        this.sqlTypeAlias = UtilXml.checkEmpty(fieldTypeElement.getAttribute("sql-type-alias"));

        NodeList validateList = fieldTypeElement.getElementsByTagName("validate");
        for (int i = 0; i < validateList.getLength(); i++) {
            Element element = (Element) validateList.item(i);
            String methodName = element.getAttribute("method");
            String className = element.getAttribute("class");
            if (methodName != null) {
                this.validators.add(new ModelFieldValidator(className, methodName));
            }            
        }
    }

    /** The type of the Field */
    public String getType() {
        return this.type;
    }

    /** The java-type of the Field */
    public String getJavaType() {
        return this.javaType;
    }

    /** The sql-type of the Field */
    public String getSqlType() {
        return this.sqlType;
    }

    /** The sql-type-alias of the Field */
    public String getSqlTypeAlias() {
        return this.sqlTypeAlias;
    }

    /** validators to be called when an update is done */
    public List getValidators() {
        return this.validators;
    }

    /** A simple function to derive the max length of a String created from the field value, based on the sql-type
     * @return max length of a String representing the Field value
     */
    public int stringLength() {
        if (sqlType.indexOf("VARCHAR") >= 0) {
            if (sqlType.indexOf("(") > 0 && sqlType.indexOf(")") > 0) {
                String length = sqlType.substring(sqlType.indexOf("(") + 1, sqlType.indexOf(")"));

                return Integer.parseInt(length);
            } else {
                return 255;
            }
        } else if (sqlType.indexOf("CHAR") >= 0) {
            if (sqlType.indexOf("(") > 0 && sqlType.indexOf(")") > 0) {
                String length = sqlType.substring(sqlType.indexOf("(") + 1, sqlType.indexOf(")"));

                return Integer.parseInt(length);
            } else {
                return 255;
            }
        } else if (sqlType.indexOf("TEXT") >= 0 || sqlType.indexOf("LONG") >= 0) {
            return 5000;
        }
        return 20;
    }

    class ModelFieldValidator implements Serializable {

        protected String validatorClass = null;
        protected String validatorMethod = null;

        public ModelFieldValidator(String className, String methodName) {
            this.validatorClass = className;
            this.validatorMethod = methodName;
        }

        public String getClassName() {
            if (UtilValidate.isNotEmpty(validatorClass) && UtilValidate.isNotEmpty(validatorMethod)) {
                return validatorClass;
            }
            return null;
        }

        public String getMethodName() {
            if (UtilValidate.isNotEmpty(validatorClass) && UtilValidate.isNotEmpty(validatorMethod)) {
                return validatorMethod;
            }
            return null;
        }
    }
}
