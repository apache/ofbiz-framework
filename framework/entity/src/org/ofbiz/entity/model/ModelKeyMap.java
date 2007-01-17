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

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilXml;


/**
 * Generic Entity - KeyMap model class
 *
 */
public class ModelKeyMap implements java.io.Serializable {

    /** name of the field in this entity */
    protected String fieldName = "";

    /** name of the field in related entity */
    protected String relFieldName = "";

    /** Default Constructor */
    public ModelKeyMap() {}

    /** Data Constructor, if relFieldName is null defaults to fieldName */
    public ModelKeyMap(String fieldName, String relFieldName) {
        this.fieldName = fieldName;
        this.relFieldName = UtilXml.checkEmpty(relFieldName, this.fieldName);
    }

    /** XML Constructor */
    public ModelKeyMap(Element keyMapElement) {
        this.fieldName = UtilXml.checkEmpty(keyMapElement.getAttribute("field-name"));
        // if no relFieldName is specified, use the fieldName; this is convenient for when they are named the same, which is often the case
        this.relFieldName = UtilXml.checkEmpty(keyMapElement.getAttribute("rel-field-name"), this.fieldName);
    }

    /** name of the field in this entity */
    public String getFieldName() {
        return this.fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    /** name of the field in related entity */
    public String getRelFieldName() {
        return this.relFieldName;
    }

    public void setRelFieldName(String relFieldName) {
        this.relFieldName = relFieldName;
    }

    // ======= Some Convenience Oriented Factory Methods =======
    public static List makeKeyMapList(String fieldName1) {
        return UtilMisc.toList(new ModelKeyMap(fieldName1, null));
    }
    public static List makeKeyMapList(String fieldName1, String relFieldName1) {
        return UtilMisc.toList(new ModelKeyMap(fieldName1, relFieldName1));
    }
    public static List makeKeyMapList(String fieldName1, String relFieldName1, String fieldName2, String relFieldName2) {
        return UtilMisc.toList(new ModelKeyMap(fieldName1, relFieldName1), new ModelKeyMap(fieldName2, relFieldName2));
    }
    public static List makeKeyMapList(String fieldName1, String relFieldName1, String fieldName2, String relFieldName2, String fieldName3, String relFieldName3) {
        return UtilMisc.toList(new ModelKeyMap(fieldName1, relFieldName1), new ModelKeyMap(fieldName2, relFieldName2), new ModelKeyMap(fieldName3, relFieldName3));
    }

    public int hashCode() {
        return this.fieldName.hashCode() + this.relFieldName.hashCode();
    }

    public boolean equals(Object other) {
        ModelKeyMap otherKeyMap = (ModelKeyMap) other;

        if (!otherKeyMap.fieldName.equals(this.fieldName)) return false;
        if (!otherKeyMap.relFieldName.equals(this.relFieldName)) return false;

        return true;
    }

    public Element toXmlElement(Document document) {
        Element root = document.createElement("key-map");
        root.setAttribute("field-name", this.getFieldName());
        if (!this.getFieldName().equals(this.getRelFieldName())) {
            root.setAttribute("rel-field-name", this.getRelFieldName());
        }

        return root;
    }
}
