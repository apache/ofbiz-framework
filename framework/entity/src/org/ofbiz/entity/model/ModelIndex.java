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

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.IteratorWrapper;

/**
 * Generic Entity - Relation model class
 *
 */
@SuppressWarnings("serial")
public class ModelIndex extends ModelChild {

    /** the index name, used for the database index name */
    protected String name;

    /** specifies whether or not this index should include the unique constraint */
    protected boolean unique;

    /** list of the field names included in this index */
    protected List<Field> fields = new ArrayList<Field>();

    /** Default Constructor */
    public ModelIndex() {
        name = "";
        unique = false;
    }

    /** Direct Create Constructor */
    public ModelIndex(ModelEntity mainEntity, String name, boolean unique) {
        super(mainEntity);
        this.name = name;
        this.unique = unique;
    }

    /** XML Constructor */
    public ModelIndex(ModelEntity mainEntity, Element indexElement) {
        super(mainEntity);

        this.name = UtilXml.checkEmpty(indexElement.getAttribute("name")).intern();
        this.unique = "true".equals(UtilXml.checkEmpty(indexElement.getAttribute("unique")));
        this.description = StringUtil.internString(UtilXml.childElementValue(indexElement, "description"));

        NodeList indexFieldList = indexElement.getElementsByTagName("index-field");
        for (int i = 0; i < indexFieldList.getLength(); i++) {
            Element indexFieldElement = (Element) indexFieldList.item(i);

            if (indexFieldElement.getParentNode() == indexElement) {
                String fieldName = indexFieldElement.getAttribute("name").intern();
                String function = indexFieldElement.getAttribute("function");
                this.fields.add(new Field(fieldName, UtilValidate.isNotEmpty(function) ? Function.valueOf(function.toUpperCase()) : null));
            }
        }
    }

    /** the index name, used for the database index name */
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** specifies whether or not this index should include the unique constraint */
    public boolean getUnique() {
        return this.unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    /** @deprecated use getFieldsIterator() */
    @Deprecated
    public Iterator<String> getIndexFieldsIterator() {
        return new IteratorWrapper<String, Field>(this.fields.iterator()) {
            @Override
            protected void noteRemoval(String dest, Field src) {
            }

            @Override
            protected String convert(Field src) {
                return src.getFieldName();
            }
        };
    }

    public Iterator<Field> getFieldsIterator() {
        return this.fields.iterator();
    }

    public int getIndexFieldsSize() {
        return this.fields.size();
    }

    public String getIndexField(int index) {
        return this.fields.get(index).getFieldName();
    }

    public void addIndexField(String fieldName) {
        this.fields.add(new Field(fieldName, null));
    }

    public void addIndexField(String fieldName, String functionName) {
        this.fields.add(new Field(fieldName, Function.valueOf(functionName)));
    }

    public void addIndexField(String fieldName, Function function) {
        this.fields.add(new Field(fieldName, function));
    }

    public String removeIndexField(int index) {
        return this.fields.remove(index).getFieldName();
    }

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
