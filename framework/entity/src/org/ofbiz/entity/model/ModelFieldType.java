/*
 * $Id: ModelFieldType.java 5720 2005-09-13 03:10:59Z jonesde $
 *
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.entity.model;

import java.io.Serializable;
import java.util.*;
import org.w3c.dom.*;

import org.ofbiz.base.util.*;

/**
 * Generic Entity - FieldType model class
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a> 
 * @version    $Rev$
 * @since      2.0
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
        this.sqlType = UtilXml.checkEmpty(fieldTypeElement.getAttribute("sql-type"));
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
