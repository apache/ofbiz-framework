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

import org.ofbiz.base.util.*;

/**
 * Generic Entity - Relation model class
 *
 */
public class ModelIndex extends ModelChild {

    /** the index name, used for the database index name */
    protected String name;

    /** specifies whether or not this index should include the unique constraint */
    protected boolean unique;

    /** list of the field names included in this index */
    protected List fieldNames = new ArrayList();

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

        this.name = UtilXml.checkEmpty(indexElement.getAttribute("name"));
        this.unique = "true".equals(UtilXml.checkEmpty(indexElement.getAttribute("unique")));

        NodeList indexFieldList = indexElement.getElementsByTagName("index-field");
        for (int i = 0; i < indexFieldList.getLength(); i++) {
            Element indexFieldElement = (Element) indexFieldList.item(i);

            if (indexFieldElement.getParentNode() == indexElement) {
                String fieldName = indexFieldElement.getAttribute("name");
                this.fieldNames.add(fieldName);            }
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

    /** @deprecated
      * the main entity of this relation */
    public ModelEntity getMainEntity() {
        return getModelEntity();
    }

    /** @deprecated */
    public void setMainEntity(ModelEntity mainEntity) {
        setModelEntity(mainEntity);
    }

    public Iterator getIndexFieldsIterator() {
        return this.fieldNames.iterator();
    }

    public int getIndexFieldsSize() {
        return this.fieldNames.size();
    }

    public String getIndexField(int index) {
        return (String) this.fieldNames.get(index);
    }

    public void addIndexField(String fieldName) {
        this.fieldNames.add(fieldName);
    }

    public String removeIndexField(int index) {
        return (String) this.fieldNames.remove(index);
    }

    public Element toXmlElement(Document document) {
        Element root = document.createElement("index");
        root.setAttribute("name", this.getName());
        if (this.getUnique()) {
            root.setAttribute("unique", "true");
        }

        Iterator fnIter = this.fieldNames.iterator();
        while (fnIter != null && fnIter.hasNext()) {
            String fieldName = (String) fnIter.next();
            Element fn = document.createElement("index-field");
            fn.setAttribute("name", fieldName);
            root.appendChild(fn);
        }

        return root;
    }
}
