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

import java.util.*;
import org.w3c.dom.*;

import org.ofbiz.entity.jdbc.*;
import org.ofbiz.base.util.*;

/**
 * Generic Entity - Field model class
 *
 */
public class ModelField extends ModelChild {

    /** The name of the Field */
    protected String name = "";

    /** The type of the Field */
    protected String type = "";

    /** The col-name of the Field */
    protected String colName = "";

    /** boolean which specifies whether or not the Field is a Primary Key */
    protected boolean isPk = false;
    protected boolean encrypt = false;
    protected boolean isAutoCreatedInternal = false;
    
    /** validators to be called when an update is done */
    protected List validators = new ArrayList();

    /** Default Constructor */
    public ModelField() {}

    /** Fields Constructor */
    public ModelField(String name, String type, String colName, boolean isPk) {
        this(name, type, colName, isPk, false);
    }

    public ModelField(String name, String type, String colName, boolean isPk, boolean encrypt) {
        this.name = name;
        this.type = type;
        this.setColName(colName);
        this.isPk = isPk;
        this.encrypt = encrypt;
    }

    /** XML Constructor */
    public ModelField(Element fieldElement) {
        this.type = UtilXml.checkEmpty(fieldElement.getAttribute("type"));
        this.name = UtilXml.checkEmpty(fieldElement.getAttribute("name"));
        this.setColName(UtilXml.checkEmpty(fieldElement.getAttribute("col-name")));
        this.isPk = false; // is set elsewhere
        this.encrypt = UtilXml.checkBoolean(fieldElement.getAttribute("encrypt"), false);

        NodeList validateList = fieldElement.getElementsByTagName("validate");

        for (int i = 0; i < validateList.getLength(); i++) {
            Element element = (Element) validateList.item(i);

            this.validators.add(UtilXml.checkEmpty(element.getAttribute("name")));
        }
    }

    /** DB Names Constructor */
    public ModelField(DatabaseUtil.ColumnCheckInfo ccInfo, ModelFieldTypeReader modelFieldTypeReader) {
        this.colName = ccInfo.columnName;
        this.name = ModelUtil.dbNameToVarName(this.colName);

        // figure out the type according to the typeName, columnSize and decimalDigits
        this.type = ModelUtil.induceFieldType(ccInfo.typeName, ccInfo.columnSize, ccInfo.decimalDigits, modelFieldTypeReader);

        this.isPk = ccInfo.isPk;
    }

    /** The name of the Field */
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** The type of the Field */
    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /** The col-name of the Field */
    public String getColName() {
        return this.colName;
    }

    public void setColName(String colName) {
        this.colName = colName;
        if (this.colName == null || this.colName.length() == 0) {
            this.colName = ModelUtil.javaNameToDbName(UtilXml.checkEmpty(this.name));
        }
    }

    /** boolean which specifies whether or not the Field is a Primary Key */
    public boolean getIsPk() {
        return this.isPk;
    }

    public void setIsPk(boolean isPk) {
        this.isPk = isPk;
    }

    public boolean getEncrypt() {
        return this.encrypt;
    }

    public void setEncrypt(boolean encrypt) {
        this.encrypt = encrypt;
    }

    public boolean getIsAutoCreatedInternal() {
        return this.isAutoCreatedInternal;
    }

    public void setIsAutoCreatedInternal(boolean isAutoCreatedInternal) {
        this.isAutoCreatedInternal = isAutoCreatedInternal;
    }
    
    /** validators to be called when an update is done */
    public String getValidator(int index) {
        return (String) this.validators.get(index);
    }

    public int getValidatorsSize() {
        return this.validators.size();
    }

    public void addValidator(String validator) {
        this.validators.add(validator);
    }

    public String removeValidator(int index) {
        return (String) this.validators.remove(index);
    }

    public boolean equals(Object obj) {
        if (obj.getClass() != getClass()) return false;
        ModelField other = (ModelField) obj;
        return other.getName().equals(getName()) && other.getModelEntity() == getModelEntity();
    }

    public int hashCode() {
        return getModelEntity().hashCode() ^ getName().hashCode();
    }

    public String toString() {
        return getModelEntity() + "@" + getName();
    }

    public Element toXmlElement(Document document) {
        Element root = document.createElement("field");
        root.setAttribute("name", this.getName());
        if (!this.getColName().equals(ModelUtil.javaNameToDbName(this.getName()))) {
            root.setAttribute("col-name", this.getColName());
        }
        root.setAttribute("type", this.getType());
        if (this.getEncrypt()) {
            root.setAttribute("encrypt", "true");
        }

        Iterator valIter = this.validators.iterator();
        if (valIter != null) {
            while (valIter.hasNext()) {
                String validator = (String) valIter.next();
                Element val = document.createElement("validate");
                val.setAttribute("name", validator);
                root.appendChild(val);
            }
        }

        return root;
    }
}
