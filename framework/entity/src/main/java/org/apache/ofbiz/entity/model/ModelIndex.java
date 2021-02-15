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
import java.util.List;
import java.util.Locale;

import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;index&gt;</code> element.
 *
 */
@ThreadSafe
@SuppressWarnings("serial")
public final class ModelIndex extends ModelChild {

    /**
     * Returns a new <code>ModelIndex</code> instance, initialized with the specified values.
     * @param modelEntity The <code>ModelEntity</code> this index is a member of.
     * @param description The index description.
     * @param name The index name.
     * @param fields The fields that are included in this index.
     * @param unique <code>true</code> if this index returns unique values.
     */
    public static ModelIndex create(ModelEntity modelEntity, String description, String name, List<Field> fields, boolean unique) {
        if (description == null) {
            description = "";
        }
        if (name == null) {
            name = "";
        }
        if (fields == null) {
            fields = Collections.emptyList();
        } else {
            fields = Collections.unmodifiableList(fields);
        }
        return new ModelIndex(modelEntity, description, name, fields, unique);
    }

    /**
     * Returns a new <code>ModelIndex</code> instance, initialized with the specified values.
     * @param modelEntity The <code>ModelEntity</code> this index is a member of.
     * @param indexElement The <code>&lt;index&gt;</code> element containing the values for this index.
     */
    public static ModelIndex create(ModelEntity modelEntity, Element indexElement) {
        String name = indexElement.getAttribute("name").intern();
        boolean unique = "true".equals(indexElement.getAttribute("unique"));
        String description = UtilXml.childElementValue(indexElement, "description");
        List<Field> fields = Collections.emptyList();
        List<? extends Element> elementList = UtilXml.childElementList(indexElement, "index-field");
        if (!elementList.isEmpty()) {
            fields = new ArrayList<>(elementList.size());
            for (Element indexFieldElement : elementList) {
                String fieldName = indexFieldElement.getAttribute("name").intern();
                String function = indexFieldElement.getAttribute("function").intern();
                fields.add(new Field(fieldName, UtilValidate.isNotEmpty(function) ? Function.valueOf(function
                        .toUpperCase(Locale.getDefault())) : null));
            }
            fields = Collections.unmodifiableList(fields);
        }
        return new ModelIndex(modelEntity, description, name, fields, unique);
    }

    /*
     * Developers - this is an immutable class. Once constructed, the object should not change state.
     * Therefore, 'setter' methods are not allowed. If client code needs to modify the object's
     * state, then it can create a new copy with the changed values.
     */

    /** the index name, used for the database index name */
    private final String name;

    /** specifies whether or not this index should include the unique constraint */
    private final boolean unique;

    /** list of the field names included in this index */
    private final List<Field> fields;

    private ModelIndex(ModelEntity mainEntity, String description, String name, List<Field> fields, boolean unique) {
        super(mainEntity, description);
        this.name = name;
        this.fields = fields;
        this.unique = unique;
    }

    /** Returns the index name. */
    public String getName() {
        return this.name;
    }

    /** Returns <code>true</code> if this index returns unique values. */
    public boolean getUnique() {
        return this.unique;
    }

    /** Returns the fields included in this index. */
    public List<Field> getFields() {
        return this.fields;
    }

    // TODO: Externalize this.
    public Element toXmlElement(Document document) {
        Element root = document.createElement("index");
        root.setAttribute("name", this.getName());
        if (this.getUnique()) {
            root.setAttribute("unique", "true");
        }

        for (Field field: this.fields) {
            Element fn = document.createElement("index-field");
            fn.setAttribute("name", field.getFieldName());
            if (field.getFunction() != null) {
                fn.setAttribute("function", field.getFunction().toString());
            }
            root.appendChild(fn);
        }

        return root;
    }

    public static final class Field {
        private final String fieldName;
        private final Function function;

        public Field(String fieldName, Function function) {
            this.fieldName = fieldName;
            this.function = function;
        }

        public String getFieldName() {
            return this.fieldName;
        }

        public Function getFunction() {
            return function;
        }

        @Override
        public String toString() {
            if (function == null) {
                return fieldName;
            } else {
                return function.toString() + '(' + fieldName + ')';
            }
        }
    }

    public enum Function { LOWER, UPPER }
}
