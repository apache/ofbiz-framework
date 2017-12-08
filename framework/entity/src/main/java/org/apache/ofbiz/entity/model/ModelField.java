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
package org.apache.ofbiz.entity.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.jdbc.DatabaseUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;field&gt;</code> element.
 *
 */
@ThreadSafe
@SuppressWarnings("serial")
public final class ModelField extends ModelChild {
    public static final String module = ModelField.class.getName();

    public enum EncryptMethod {
        FALSE {
            public boolean isEncrypted() {
                return false;
            }
        },
        TRUE {
            public boolean isEncrypted() {
                return true;
            }
        },
        SALT {
            public boolean isEncrypted() {
                return true;
            }
        };

        public abstract boolean isEncrypted();
    }

    /**
     * Returns a new <code>ModelField</code> instance, initialized with the specified values.
     * 
     * @param modelEntity The <code>ModelEntity</code> this field is a member of.
     * @param name The field name.
     * @param type The field type.
     * @param isPk <code>true</code> if this field is part of the primary key.
     */
    public static ModelField create(ModelEntity modelEntity, String name, String type, boolean isPk) {
        return create(modelEntity, null, name, type, null, null, null, false, isPk, false, false, false, null);
    }

    /**
     * Returns a new <code>ModelField</code> instance, initialized with the specified values.
     * 
     * @param modelEntity The <code>ModelEntity</code> this field is a member of.
     * @param description The field description.
     * @param name The field name.
     * @param type The field type.
     * @param colName The data source column name for this field. Will be generated automatically if left empty.
     * @param colValue
     * @param fieldSet The field set name this field is a member of.
     * @param isNotNull <code>true</code> if this field cannot contain a null value.
     * @param isPk <code>true</code> if this field is part of the primary key.
     * @param encrypt <code>true</code> if this field is encrypted.
     * @param isAutoCreatedInternal <code>true</code> if this field was generated automatically by the entity engine.
     * @param enableAuditLog <code>true</code> if this field is included in the entity audit log.
     * @param validators The validators for this field.
     */
    public static ModelField create(ModelEntity modelEntity, String description, String name, String type, String colName, String colValue, String fieldSet, boolean isNotNull, boolean isPk, boolean encrypt, boolean isAutoCreatedInternal, boolean enableAuditLog, List<String> validators) {
        return create(modelEntity, description, name, type, colName, colValue, fieldSet, isNotNull, isPk, encrypt ? EncryptMethod.TRUE : EncryptMethod.FALSE, isAutoCreatedInternal, enableAuditLog, validators);
    }

    public static ModelField create(ModelEntity modelEntity, String description, String name, String type, String colName, String colValue, String fieldSet, boolean isNotNull, boolean isPk, EncryptMethod encrypt, boolean isAutoCreatedInternal, boolean enableAuditLog, List<String> validators) {
        // TODO: Validate parameters.
        if (description == null) {
            description = "";
        }
        if (name == null) {
            name = "";
        }
        if (type == null) {
            type = "";
        }
        if (colName == null || colName.isEmpty()) {
            colName = ModelUtil.javaNameToDbName(name);
        }
        if (colValue == null) {
            colValue = "";
        }
        if (fieldSet == null) {
            fieldSet = "";
        }
        if (validators == null) {
            validators = Collections.emptyList();
        } else {
            validators = Collections.unmodifiableList(validators);
        }
        if (isPk) {
            isNotNull = true;
        }
        return new ModelField(modelEntity, description, name, type, colName, colValue, fieldSet, isNotNull, isPk, encrypt, isAutoCreatedInternal, enableAuditLog, validators);
    }

    /**
     * Returns a new <code>ModelField</code> instance, initialized with the specified values.
     * 
     * @param modelEntity The <code>ModelEntity</code> this field is a member of.
     * @param fieldElement The <code>&lt;field&gt;</code> element containing the values for this field.
     * @param isPk <code>true</code> if this field is part of the primary key.
     */
    public static ModelField create(ModelEntity modelEntity, Element fieldElement, boolean isPk) {
        String description = UtilXml.childElementValue(fieldElement, "description");
        if (description == null) {
            description = "";
        }
        String name = fieldElement.getAttribute("name").intern();
        String type = fieldElement.getAttribute("type").intern();
        String colName = fieldElement.getAttribute("col-name").intern();
        if (colName.isEmpty()) {
            colName = ModelUtil.javaNameToDbName(name);
        }
        String colValue = "";
        String fieldSet = fieldElement.getAttribute("field-set").intern();
        boolean isNotNull = "true".equals(fieldElement.getAttribute("not-null"));
        if (isPk) {
            isNotNull = true;
        }
        EncryptMethod encrypt = EncryptMethod.valueOf(fieldElement.getAttribute("encrypt").toUpperCase(Locale.getDefault()));
        boolean enableAuditLog = "true".equals(fieldElement.getAttribute("enable-audit-log"));
        List<String>validators = Collections.emptyList();
        List<? extends Element> elementList = UtilXml.childElementList(fieldElement, "validate");
        if (!elementList.isEmpty()) {
            validators = new ArrayList<String>(elementList.size());
            for (Element validateElement : elementList) {
                validators.add(validateElement.getAttribute("name").intern());
            }
            validators = Collections.unmodifiableList(validators);
        }
        return new ModelField(modelEntity, description, name, type, colName, colValue, fieldSet, isNotNull, isPk, encrypt, false, enableAuditLog, validators);
    }

    /**
     * Returns a new <code>ModelField</code> instance, initialized with the specified values.
     * 
     * @param modelEntity The <code>ModelEntity</code> this field is a member of.
     * @param ccInfo The <code>ColumnCheckInfo</code> containing the values for this field.
     * @param modelFieldTypeReader
     */
    public static ModelField create(ModelEntity modelEntity, DatabaseUtil.ColumnCheckInfo ccInfo, ModelFieldTypeReader modelFieldTypeReader) {
        String colName = ccInfo.columnName;
        String name = ModelUtil.dbNameToVarName(colName);
        String type = ModelUtil.induceFieldType(ccInfo.typeName, ccInfo.columnSize, ccInfo.decimalDigits, modelFieldTypeReader);
        boolean isPk = ccInfo.isPk;
        boolean isNotNull = "NO".equals(ccInfo.isNullable.toUpperCase(Locale.getDefault()));
        String description = "";
        String colValue = "";
        String fieldSet = "";
        EncryptMethod encrypt = EncryptMethod.FALSE;
        boolean enableAuditLog = false;
        return new ModelField(modelEntity, description, name, type, colName, colValue, fieldSet, isNotNull, isPk, encrypt, false, enableAuditLog, Collections.<String>emptyList());
    }

    /*
     * Developers - this is an immutable class. Once constructed, the object should not change state.
     * Therefore, 'setter' methods are not allowed. If client code needs to modify the object's
     * state, then it can create a new copy with the changed values.
     */

    /** The name of the Field */
    private final String name;

    /** The type of the Field */
    private final String type;

    /** The col-name of the Field */
    private final String colName;

    private final String colValue;

    /** boolean which specifies whether or not the Field is a Primary Key */
    private final boolean isPk;
    private final EncryptMethod encrypt;
    private final boolean isNotNull;
    private final boolean isAutoCreatedInternal;
    private final boolean enableAuditLog;

    /** when any field in the same set is selected in a query, all fields in that set will be selected */
    private final String fieldSet;

    /** validators to be called when an update is done */
    private final List<String> validators;

    private ModelField(ModelEntity modelEntity, String description, String name, String type, String colName, String colValue, String fieldSet, boolean isNotNull, boolean isPk, EncryptMethod encrypt, boolean isAutoCreatedInternal, boolean enableAuditLog, List<String> validators) {
        super(modelEntity, description);
        this.name = name;
        this.type = type;
        this.colName = colName;
        this.colValue = colValue;
        this.fieldSet = fieldSet;
        this.isPk = isPk;
        this.isNotNull = isNotNull;
        this.encrypt = encrypt;
        this.enableAuditLog = enableAuditLog;
        this.isAutoCreatedInternal = isAutoCreatedInternal;
        this.validators = validators;
    }

    /** Returns the name of this field. */
    public String getName() {
        return this.name;
    }

    /** Returns the type of this field. */
    public String getType() {
        return this.type;
    }

    /** Returns the data source column name of this field. */
    public String getColName() {
        return this.colName;
    }

    public String getColValue() {
        return this.colValue.isEmpty() ? this.colName : this.colValue;
    }

    /** Returns <code>true</code> if this field is part of the primary key. */
    public boolean getIsPk() {
        return this.isPk;
    }

    /** Returns <code>true</code> if this field cannot contain null. */
    public boolean getIsNotNull() {
        return this.isNotNull;
    }

    /** Returns <code>true</code> if this field is encrypted. */
    @Deprecated
    public boolean getEncrypt() {
        return this.encrypt.isEncrypted();
    }

    public EncryptMethod getEncryptMethod() {
        return this.encrypt;
    }

    /** Returns <code>true</code> if this field is included in the entity audit log. */
    public boolean getEnableAuditLog() {
        return this.enableAuditLog;
    }

    /** Returns <code>true</code> if this field was generated automatically by the entity engine. */
    public boolean getIsAutoCreatedInternal() {
        return this.isAutoCreatedInternal;
    }

    /** Returns the field set name this field is a member of. */
    public String getFieldSet() {
        return fieldSet;
    }

    public List<String> getValidators() {
        return this.validators;
    }

    @Override
    public String toString() {
        return getModelEntity() + "@" + getName();
    }

    // TODO: Externalize this.
    public Element toXmlElement(Document document) {
        Element root = document.createElement("field");
        root.setAttribute("name", this.getName());
        if (!this.getColName().equals(ModelUtil.javaNameToDbName(this.getName()))) {
            root.setAttribute("col-name", this.getColName());
        }
        root.setAttribute("type", this.getType());
        if (this.getEncryptMethod().isEncrypted()) {
            root.setAttribute("encrypt", this.getEncryptMethod().toString().toLowerCase(Locale.getDefault()));
        }
        if (this.getIsNotNull()) {
            root.setAttribute("not-null", "true");
        }

        Iterator<String> valIter = this.validators.iterator();
        if (valIter != null) {
            while (valIter.hasNext()) {
                String validator = valIter.next();
                Element val = document.createElement("validate");
                val.setAttribute("name", validator);
                root.appendChild(val);
            }
        }

        return root;
    }
}
