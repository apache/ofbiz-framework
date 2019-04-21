/*
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
 */
package org.apache.ofbiz.entity.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;relation&gt;</code> element.
 *
 */
@ThreadSafe
@SuppressWarnings("serial")
public final class ModelRelation extends ModelChild {

    /**
     * Returns a new <code>ModelRelation</code> instance, initialized with the specified values.
     * 
     * @param modelEntity The <code>ModelEntity</code> this relation is a member of.
     * @param description The relation description.
     * @param type The relation type.
     * @param title The relation title.
     * @param relEntityName The related entity's name.
     * @param fkName The foreign key name.
     * @param keyMaps The key maps included in this relation.
     * @param isAutoRelation <code>true</code> if this relation was generated automatically by the entity engine.
     */
    public static ModelRelation create(ModelEntity modelEntity, String description, String type, String title, String relEntityName, String fkName, List<ModelKeyMap> keyMaps, boolean isAutoRelation) {
        if (description == null) {
            description = "";
        }
        if (type == null) {
            type = "";
        }
        if (title == null) {
            title = "";
        }
        if (relEntityName == null) {
            relEntityName = "";
        }
        if (fkName == null) {
            fkName = "";
        }
        if (keyMaps == null) {
            keyMaps = Collections.emptyList();
        } else {
            keyMaps = Collections.unmodifiableList(keyMaps);
        }
        return new ModelRelation(modelEntity, description, type, title, relEntityName, fkName, keyMaps, isAutoRelation);
    }

    /**
     * Returns a new <code>ModelRelation</code> instance, initialized with the specified values.
     * 
     * @param modelEntity The <code>ModelEntity</code> this relation is a member of.
     * @param relationElement The <code>&lt;relation&gt;</code> element containing the values for this relation.
     * @param isAutoRelation <code>true</code> if this relation was generated automatically by the entity engine.
     */
    public static ModelRelation create(ModelEntity modelEntity, Element relationElement, boolean isAutoRelation) {
        String type = relationElement.getAttribute("type").intern();
        String title = relationElement.getAttribute("title").intern();
        String relEntityName = relationElement.getAttribute("rel-entity-name").intern();
        String fkName = relationElement.getAttribute("fk-name").intern();
        String description = UtilXml.childElementValue(relationElement, "description");
        List<ModelKeyMap >keyMaps = Collections.emptyList();
        List<? extends Element> elementList = UtilXml.childElementList(relationElement, "key-map");
        if (!elementList.isEmpty()) {
            keyMaps = new ArrayList<>(elementList.size());
            for (Element keyMapElement : elementList) {
                keyMaps.add(new ModelKeyMap(keyMapElement));
            }
            keyMaps = Collections.unmodifiableList(keyMaps);
        }
        return new ModelRelation(modelEntity, description, type, title, relEntityName, fkName, keyMaps, isAutoRelation);
    }

    /*
     * Developers - this is an immutable class. Once constructed, the object should not change state.
     * Therefore, 'setter' methods are not allowed. If client code needs to modify the object's
     * state, then it can create a new copy with the changed values.
     */

    /** the title, gives a name/description to the relation */
    private final String title;

    /** the type: either "one" or "many" or "one-nofk" */
    private final String type;

    /** the name of the related entity */
    private final String relEntityName;

    /** the name to use for a database foreign key, if applies */
    private final String fkName;

    /** keyMaps defining how to lookup the relatedTable using columns from this table */
    private final List<ModelKeyMap> keyMaps;

    private final boolean isAutoRelation;

    /** A String to uniquely identify this relation. */
    private final String fullName;

    private final String combinedName;

    private ModelRelation(ModelEntity modelEntity, String description, String type, String title, String relEntityName, String fkName, List<ModelKeyMap> keyMaps, boolean isAutoRelation) {
        super(modelEntity, description);
        this.title = title;
        this.type = type;
        this.relEntityName = relEntityName;
        this.fkName = fkName;
        this.keyMaps = keyMaps;
        this.isAutoRelation = isAutoRelation;
        StringBuilder sb = new StringBuilder();
        sb.append(modelEntity == null ? "Unknown" : modelEntity.getEntityName()).append("->").append(title).append(relEntityName).append("[");
        Set<ModelKeyMap> keyMapSet = new TreeSet<>(keyMaps);
        Iterator<ModelKeyMap> setIter = keyMapSet.iterator();
        while (setIter.hasNext()) {
            ModelKeyMap keyMap = setIter.next();
            sb.append(keyMap);
            if (setIter.hasNext()) {
                sb.append(",");
            }
        }
        sb.append("]");
        this.fullName = sb.toString();
        this.combinedName = title.concat(relEntityName);
    }

    /** Returns the combined name (title + related entity name). */
    public String getCombinedName() {
        return this.combinedName;
    }

    /** Returns the title. */
    public String getTitle() {
        return this.title;
    }

    /** Returns the type. */
    public String getType() {
        return this.type;
    }

    /** Returns the related entity name. */
    public String getRelEntityName() {
        return this.relEntityName;
    }

    /** Returns the foreign key name. */
    public String getFkName() {
        return this.fkName;
    }

    /** Returns the key maps. */
    public List<ModelKeyMap> getKeyMaps() {
        return this.keyMaps;
    }

    /** Returns <code>true</code> if this relation was generated automatically by the entity engine. */
    public boolean isAutoRelation() {
        return isAutoRelation;
    }

    /** Find a KeyMap with the specified fieldName */
    public ModelKeyMap findKeyMap(String fieldName) {
        for (ModelKeyMap keyMap: keyMaps) {
            if (keyMap.getFieldName().equals(fieldName)) return keyMap;
        }
        return null;
    }

    /** Find a KeyMap with the specified relFieldName */
    public ModelKeyMap findKeyMapByRelated(String relFieldName) {
        for (ModelKeyMap keyMap: keyMaps) {
            if (keyMap.getRelFieldName().equals(relFieldName))
                return keyMap;
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ModelRelation) {
            ModelRelation that = (ModelRelation) obj;
            return this.fullName.equals(that.fullName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.fullName.hashCode();
    }

    @Override
    public String toString() {
        return this.fullName;
    }

    // TODO: Externalize this.
    public String keyMapString(String separator, String afterLast) {
        StringBuilder stringBuilder = new StringBuilder("");

        if (keyMaps.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < keyMaps.size() - 1; i++) {
            stringBuilder.append(keyMaps.get(i).getFieldName());
            stringBuilder.append(separator);
        }
        stringBuilder.append(keyMaps.get(i).getFieldName());
        stringBuilder.append(afterLast);
        return stringBuilder.toString();
    }

    // TODO: Externalize this.
    public String keyMapUpperString(String separator, String afterLast) {
        if (keyMaps.size() < 1)
            return "";

        StringBuilder returnString = new StringBuilder(keyMaps.size() * 10);
        int i=0;
        while (true) {
            ModelKeyMap kmap = keyMaps.get(i);
            returnString.append(ModelUtil.upperFirstChar(kmap.getFieldName()));

            i++;
            if (i >= keyMaps.size()) {
                returnString.append(afterLast);
                break;
            }

            returnString.append(separator);
        }

        return returnString.toString();
    }

    // TODO: Externalize this.
    public String keyMapRelatedUpperString(String separator, String afterLast) {
        if (keyMaps.size() < 1)
            return "";

        StringBuilder returnString = new StringBuilder(keyMaps.size() * 10);
        int i=0;
        while (true) {
            ModelKeyMap kmap = keyMaps.get(i);
            returnString.append(ModelUtil.upperFirstChar(kmap.getRelFieldName()));

            i++;
            if (i >= keyMaps.size()) {
                returnString.append(afterLast);
                break;
            }

            returnString.append(separator);
        }

        return returnString.toString();
    }

    // TODO: Externalize this.
    public Element toXmlElement(Document document) {
        Element root = document.createElement("relation");
        root.setAttribute("type", this.getType());
        if (UtilValidate.isNotEmpty(this.getTitle())) {
            root.setAttribute("title", this.getTitle());
        }
        root.setAttribute("rel-entity-name", this.getRelEntityName());

        if (UtilValidate.isNotEmpty(this.getFkName())) {
            root.setAttribute("fk-name", this.getFkName());
        }

        Iterator<ModelKeyMap> kmIter = this.keyMaps.iterator();
        while (kmIter != null && kmIter.hasNext()) {
            ModelKeyMap km = kmIter.next();
            root.appendChild(km.toXmlElement(document));
        }

        return root;
    }
}
