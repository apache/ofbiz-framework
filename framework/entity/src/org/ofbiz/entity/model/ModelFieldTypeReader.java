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
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.config.MainResourceHandler;
import org.ofbiz.base.config.ResourceHandler;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilTimer;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.ofbiz.entity.config.FieldTypeInfo;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Field Type Definition Reader.
 *
 */
@SuppressWarnings("serial")
public class ModelFieldTypeReader implements Serializable {

    public static final String module = ModelFieldTypeReader.class.getName();
    protected static final UtilCache<String, ModelFieldTypeReader> readers = UtilCache.createUtilCache("entity.ModelFieldTypeReader", 0, 0);

    protected static Map<String, ModelFieldType> createFieldTypeCache(Element docElement, String location) {
        docElement.normalize();
        Map<String, ModelFieldType> fieldTypeMap = FastMap.newInstance();
        List<? extends Element> fieldTypeList = UtilXml.childElementList(docElement, "field-type-def");
        for (Element curFieldType: fieldTypeList) {
            String fieldTypeName = curFieldType.getAttribute("type");
            if (UtilValidate.isEmpty(fieldTypeName)) {
                Debug.logError("Invalid field-type element, type attribute is missing in file " + location, module);
            } else {
                ModelFieldType fieldType = new ModelFieldType(curFieldType);
                fieldTypeMap.put(fieldTypeName.intern(), fieldType);
            }
        }
        return fieldTypeMap;
    }

    public static ModelFieldTypeReader getModelFieldTypeReader(String helperName) {
        DatasourceInfo datasourceInfo = EntityConfigUtil.getDatasourceInfo(helperName);
        if (datasourceInfo == null) {
            throw new IllegalArgumentException("Could not find a datasource/helper with the name " + helperName);
        }
        String tempModelName = datasourceInfo.fieldTypeName;
        ModelFieldTypeReader reader = readers.get(tempModelName);
        if (reader == null) {
            synchronized (readers) {
                FieldTypeInfo fieldTypeInfo = EntityConfigUtil.getFieldTypeInfo(tempModelName);
                if (fieldTypeInfo == null) {
                    throw new IllegalArgumentException("Could not find a field-type definition with name \"" + tempModelName + "\"");
                }
                ResourceHandler fieldTypeResourceHandler = new MainResourceHandler(EntityConfigUtil.ENTITY_ENGINE_XML_FILENAME, fieldTypeInfo.resourceElement);
                UtilTimer utilTimer = new UtilTimer();
                utilTimer.timerString("[ModelFieldTypeReader.getModelFieldTypeReader] Reading field types from " + fieldTypeResourceHandler.getLocation());
                Document document = null;
                try {
                    document = fieldTypeResourceHandler.getDocument();
                } catch (GenericConfigException e) {
                    Debug.logError(e, module);
                    throw new IllegalStateException("Error loading field type file " + fieldTypeResourceHandler.getLocation());
                }
                Map<String, ModelFieldType> fieldTypeMap = createFieldTypeCache(document.getDocumentElement(), fieldTypeResourceHandler.getLocation());
                reader = new ModelFieldTypeReader(fieldTypeMap);
                readers.put(tempModelName, reader);
                utilTimer.timerString("[ModelFieldTypeReader.getModelFieldTypeReader] Read " + fieldTypeMap.size() + " field types");
            }
        }
        return reader;
    }

    protected final Map<String, ModelFieldType> fieldTypeCache;

    public ModelFieldTypeReader(Map<String, ModelFieldType> fieldTypeMap) {
        this.fieldTypeCache = fieldTypeMap;
    }

    /** Creates a Collection with all of the ModelFieldType names
     * @return A Collection of ModelFieldType names
     */
    public Collection<String> getFieldTypeNames() {
        return this.fieldTypeCache.keySet();
    }

    /** Creates a Collection with all of the ModelFieldTypes
     * @return A Collection of ModelFieldTypes
     */
    public Collection<ModelFieldType> getFieldTypes() {
        return this.fieldTypeCache.values();
    }

    /** Gets an FieldType object based on a definition from the specified XML FieldType descriptor file.
     * @param fieldTypeName The fieldTypeName of the FieldType definition to use.
     * @return An FieldType object describing the specified fieldType of the specified descriptor file.
     */
    public ModelFieldType getModelFieldType(String fieldTypeName) {
        return this.fieldTypeCache.get(fieldTypeName);
    }

}
