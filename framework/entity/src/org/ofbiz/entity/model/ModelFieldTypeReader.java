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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.config.MainResourceHandler;
import org.ofbiz.base.config.ResourceHandler;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilTimer;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.ofbiz.entity.config.FieldTypeInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Generic Entity - Field Type Definition Reader
 *
 */
public class ModelFieldTypeReader implements Serializable {

    public static final String module = ModelFieldTypeReader.class.getName();
    public static UtilCache readers = new UtilCache("entity.ModelFieldTypeReader", 0, 0);

    public Map fieldTypeCache = null;

    public int numEntities = 0;
    public int numFields = 0;
    public int numRelations = 0;

    public String modelName;
    public ResourceHandler fieldTypeResourceHandler;
    public String entityFileName;

    public static ModelFieldTypeReader getModelFieldTypeReader(String helperName) {
        DatasourceInfo datasourceInfo = EntityConfigUtil.getDatasourceInfo(helperName);
        if (datasourceInfo == null) {
            throw new IllegalArgumentException("Could not find a datasource/helper with the name " + helperName);
        }

        String tempModelName = datasourceInfo.fieldTypeName;
        ModelFieldTypeReader reader = (ModelFieldTypeReader) readers.get(tempModelName);

        if (reader == null) // don't want to block here
        {
            synchronized (ModelFieldTypeReader.class) {
                // must check if null again as one of the blocked threads can still enter
                reader = (ModelFieldTypeReader) readers.get(tempModelName);
                if (reader == null) {
                    reader = new ModelFieldTypeReader(tempModelName);
                    readers.put(tempModelName, reader);
                }
            }
        }
        return reader;
    }

    public ModelFieldTypeReader(String modelName) {
        this.modelName = modelName;
        FieldTypeInfo fieldTypeInfo = EntityConfigUtil.getFieldTypeInfo(modelName);

        if (fieldTypeInfo == null) {
            throw new IllegalStateException("Could not find a field-type definition with name \"" + modelName + "\"");
        }
        fieldTypeResourceHandler = new MainResourceHandler(EntityConfigUtil.ENTITY_ENGINE_XML_FILENAME, fieldTypeInfo.resourceElement);

        // preload caches...
        getFieldTypeCache();
    }

    public Map getFieldTypeCache() {
        if (fieldTypeCache == null) // don't want to block here
        {
            synchronized (ModelFieldTypeReader.class) {
                // must check if null again as one of the blocked threads can still enter
                if (fieldTypeCache == null) // now it's safe
                {
                    fieldTypeCache = new HashMap();

                    UtilTimer utilTimer = new UtilTimer();
                    // utilTimer.timerString("Before getDocument");

                    Document document = null;

                    try {
                        document = fieldTypeResourceHandler.getDocument();
                    } catch (GenericConfigException e) {
                        Debug.logError(e, "Error loading field type file", module);
                    }
                    if (document == null) {
                        fieldTypeCache = null;
                        return null;
                    }

                    // utilTimer.timerString("Before getDocumentElement");
                    Element docElement = document.getDocumentElement();

                    if (docElement == null) {
                        fieldTypeCache = null;
                        return null;
                    }
                    docElement.normalize();

                    Node curChild = docElement.getFirstChild();

                    int i = 0;

                    if (curChild != null) {
                        utilTimer.timerString("Before start of field type loop");
                        do {
                            if (curChild.getNodeType() == Node.ELEMENT_NODE && "field-type-def".equals(curChild.getNodeName())) {
                                i++;
                                // utilTimer.timerString("Start loop -- " + i + " --");
                                Element curFieldType = (Element) curChild;
                                String fieldTypeName = UtilXml.checkEmpty(curFieldType.getAttribute("type"), "[No type name]");
                                // utilTimer.timerString("  After fieldTypeName -- " + i + " --");
                                ModelFieldType fieldType = createModelFieldType(curFieldType, docElement, null);

                                // utilTimer.timerString("  After createModelFieldType -- " + i + " --");
                                if (fieldType != null) {
                                    fieldTypeCache.put(fieldTypeName, fieldType);
                                    // utilTimer.timerString("  After fieldTypeCache.put -- " + i + " --");
                                    if (Debug.verboseOn()) Debug.logVerbose("-- getModelFieldType: #" + i + " Created fieldType: " + fieldTypeName, module);
                                } else {
                                    Debug.logWarning("-- -- ENTITYGEN ERROR:getModelFieldType: Could not create fieldType for fieldTypeName: " + fieldTypeName, module);
                                }

                            }
                        } while ((curChild = curChild.getNextSibling()) != null);
                    } else
                        Debug.logWarning("No child nodes found.", module);
                    utilTimer.timerString("FINISHED - Total Field Types: " + i + " FINISHED");
                }
            }
        }
        return fieldTypeCache;
    }

    /** Creates a Collection with all of the ModelFieldType names
     * @return A Collection of ModelFieldType names
     */
    public Collection getFieldTypeNames() {
        Map ftc = getFieldTypeCache();

        return ftc.keySet();
    }

    /** Creates a Collection with all of the ModelFieldTypes
     * @return A Collection of ModelFieldTypes
     */
    public Collection getFieldTypes() {
        Map ftc = getFieldTypeCache();

        return ftc.values();
    }

    /** Gets an FieldType object based on a definition from the specified XML FieldType descriptor file.
     * @param fieldTypeName The fieldTypeName of the FieldType definition to use.
     * @return An FieldType object describing the specified fieldType of the specified descriptor file.
     */
    public ModelFieldType getModelFieldType(String fieldTypeName) {
        Map ftc = getFieldTypeCache();

        if (ftc != null)
            return (ModelFieldType) ftc.get(fieldTypeName);
        else
            return null;
    }

    ModelFieldType createModelFieldType(Element fieldTypeElement, Element docElement, UtilTimer utilTimer) {
        if (fieldTypeElement == null) return null;

        ModelFieldType field = new ModelFieldType(fieldTypeElement);

        return field;
    }
}
