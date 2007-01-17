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
public class ModelRelation extends ModelChild {

    /** the title, gives a name/description to the relation */
    protected String title;

    /** the type: either "one" or "many" or "one-nofk" */
    protected String type;

    /** the name of the related entity */
    protected String relEntityName;

    /** the name to use for a database foreign key, if applies */
    protected String fkName;

    /** keyMaps defining how to lookup the relatedTable using columns from this table */
    protected List keyMaps = new ArrayList();

    /** the main entity of this relation */
    protected ModelEntity mainEntity = null;
    
    protected boolean isAutoRelation = false;

    /** Default Constructor */
    public ModelRelation() {
        title = "";
        type = "";
        relEntityName = "";
        fkName = "";
    }

    /** Default Constructor */
    public ModelRelation(String type, String title, String relEntityName, String fkName, List keyMaps) {
        this.title = title;
        if (title == null) title = "";
        this.type = type;
        this.relEntityName = relEntityName;
        this.fkName = fkName;
        this.keyMaps.addAll(keyMaps);
    }

    /** XML Constructor */
    public ModelRelation(ModelEntity mainEntity, Element relationElement) {
        this.mainEntity = mainEntity;

        this.type = UtilXml.checkEmpty(relationElement.getAttribute("type"));
        this.title = UtilXml.checkEmpty(relationElement.getAttribute("title"));
        this.relEntityName = UtilXml.checkEmpty(relationElement.getAttribute("rel-entity-name"));
        this.fkName = UtilXml.checkEmpty(relationElement.getAttribute("fk-name"));

        NodeList keyMapList = relationElement.getElementsByTagName("key-map");
        for (int i = 0; i < keyMapList.getLength(); i++) {
            Element keyMapElement = (Element) keyMapList.item(i);

            if (keyMapElement.getParentNode() == relationElement) {
                ModelKeyMap keyMap = new ModelKeyMap(keyMapElement);

                if (keyMap != null) {
                    this.keyMaps.add(keyMap);
                }
            }
        }
    }

    /** the title, gives a name/description to the relation */
    public String getTitle() {
        if (this.title == null) {
            this.title = "";
        }
        return this.title;
    }

    public void setTitle(String title) {
        if (title == null) {
            this.title = "";
        } else {
            this.title = title;
        }
    }

    /** the type: either "one" or "many" or "one-nofk" */
    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /** the name of the related entity */
    public String getRelEntityName() {
        return this.relEntityName;
    }

    public void setRelEntityName(String relEntityName) {
        this.relEntityName = relEntityName;
    }

    public String getFkName() {
        return this.fkName;
    }

    public void setFkName(String fkName) {
        this.fkName = fkName;
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

    /** keyMaps defining how to lookup the relatedTable using columns from this table */
    public Iterator getKeyMapsIterator() {
        return this.keyMaps.iterator();
    }

    public int getKeyMapsSize() {
        return this.keyMaps.size();
    }

    public ModelKeyMap getKeyMap(int index) {
        return (ModelKeyMap) this.keyMaps.get(index);
    }

    public void addKeyMap(ModelKeyMap keyMap) {
        this.keyMaps.add(keyMap);
    }

    public ModelKeyMap removeKeyMap(int index) {
        return (ModelKeyMap) this.keyMaps.remove(index);
    }

    /** Find a KeyMap with the specified fieldName */
    public ModelKeyMap findKeyMap(String fieldName) {
        for (int i = 0; i < keyMaps.size(); i++) {
            ModelKeyMap keyMap = (ModelKeyMap) keyMaps.get(i);

            if (keyMap.fieldName.equals(fieldName)) return keyMap;
        }
        return null;
    }

    /** Find a KeyMap with the specified relFieldName */
    public ModelKeyMap findKeyMapByRelated(String relFieldName) {
        for (int i = 0; i < keyMaps.size(); i++) {
            ModelKeyMap keyMap = (ModelKeyMap) keyMaps.get(i);

            if (keyMap.relFieldName.equals(relFieldName))
                return keyMap;
        }
        return null;
    }

    public String keyMapString(String separator, String afterLast) {
        String returnString = "";

        if (keyMaps.size() < 1) {
            return "";
        }

        int i = 0;

        for (; i < keyMaps.size() - 1; i++) {
            returnString = returnString + ((ModelKeyMap) keyMaps.get(i)).fieldName + separator;
        }
        returnString = returnString + ((ModelKeyMap) keyMaps.get(i)).fieldName + afterLast;
        return returnString;
    }

    public String keyMapUpperString(String separator, String afterLast) {
        if (keyMaps.size() < 1)
            return "";

        StringBuffer returnString = new StringBuffer( keyMaps.size() * 10 );
        int i=0;
        while (true) {
            ModelKeyMap kmap = (ModelKeyMap) keyMaps.get(i);
            returnString.append( ModelUtil.upperFirstChar( kmap.fieldName));

            i++;
            if (i >= keyMaps.size()) {
                returnString.append( afterLast );
                break;
            }

            returnString.append( separator );
        }

        return returnString.toString();
    }

    public String keyMapRelatedUpperString(String separator, String afterLast) {
        if (keyMaps.size() < 1)
            return "";

        StringBuffer returnString = new StringBuffer( keyMaps.size() * 10 );
        int i=0;
        while (true) {
            ModelKeyMap kmap = (ModelKeyMap) keyMaps.get(i);
            returnString.append( ModelUtil.upperFirstChar( kmap.relFieldName ));

            i++;
            if (i >= keyMaps.size()) {
                returnString.append( afterLast );
                break;
            }

            returnString.append( separator );
        }

        return returnString.toString();
    }
    /**
     * @return Returns the isAutoRelation.
     */
    public boolean isAutoRelation() {
        return isAutoRelation;
    }
    /**
     * @param isAutoRelation The isAutoRelation to set.
     */
    public void setAutoRelation(boolean isAutoRelation) {
        this.isAutoRelation = isAutoRelation;
    }
    
    public boolean equals(Object other) {
        ModelRelation otherRel = (ModelRelation) other;
        
        if (!otherRel.type.equals(this.type)) return false;
        if (!otherRel.title.equals(this.title)) return false;
        if (!otherRel.relEntityName.equals(this.relEntityName)) return false;
        
        Set thisKeyNames = new HashSet(this.keyMaps);
        Set otherKeyNames = new HashSet(otherRel.keyMaps);
        if (!thisKeyNames.containsAll(otherKeyNames)) return false;
        if (!otherKeyNames.containsAll(thisKeyNames)) return false;
        
        return true;
    }

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

        Iterator kmIter = this.getKeyMapsIterator();
        while (kmIter != null && kmIter.hasNext()) {
            ModelKeyMap km = (ModelKeyMap) kmIter.next();
            root.appendChild(km.toXmlElement(document));
        }

        return root;
    }
}
