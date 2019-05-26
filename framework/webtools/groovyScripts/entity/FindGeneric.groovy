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

import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.base.util.UtilXml
import org.apache.ofbiz.entity.GenericEntityException
import org.apache.ofbiz.entity.model.ModelEntity
import org.apache.ofbiz.entity.model.ModelFieldType
import org.apache.ofbiz.entity.model.ModelReader
import org.apache.ofbiz.widget.model.FormFactory
import org.apache.ofbiz.widget.model.ModelForm
import org.apache.ofbiz.widget.renderer.FormRenderer
import org.apache.ofbiz.widget.renderer.VisualTheme
import org.apache.ofbiz.widget.renderer.macro.MacroFormRenderer
import org.w3c.dom.Document

ModelEntity modelEntity = null
try {
    modelEntity = delegator.getModelEntity(parameters.entityName)
} catch(GenericEntityException e) {
    Debug.logError("The entityName " + parameters.entityName + " is not found", "FindGeneric.groovy")
}

if (modelEntity) {
    entityName = modelEntity.entityName
    context.entityName = entityName
    ModelReader entityModelReader = delegator.getModelReader()
    //create the search form with auto-fields-entity
    String dynamicAutoEntityFieldSearchForm = '<?xml version="1.0" encoding="UTF-8"?><forms xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://ofbiz.apache.org/Widget-Form" xsi:schemaLocation="http://ofbiz.apache.org/Widget-Form http://ofbiz.apache.org/dtds/widget-form.xsd">' +
            '<form name="FindGeneric" type="single" target="FindGeneric?entityName='+ entityName +'">' +
            '<auto-fields-entity entity-name="' + entityName + '" default-field-type="find"/>' +
            '<field name="noConditionFind"><hidden value="Y"/></field>' +
            '<field name="searchOptions_collapsed" ><hidden value="true"/></field>' +
            '<field name="searchButton" title="' + UtilProperties.getMessage("CommonUiLabels", "FormFieldTitle_searchButton", locale) + '"><submit/></field>'

    //call modelEntity to complete information on the field type
    modelEntity.getFieldsUnmodifiable().each {
        modelField ->
            if (! modelEntity.getAutomaticFieldNames().contains(modelField.name)) {
                ModelFieldType type = delegator.getEntityFieldType(modelEntity, modelField.getType())
                dynamicAutoEntityFieldSearchForm = dynamicAutoEntityFieldSearchForm +
                        '<field name="' + modelField.name + '" tooltip="' +
                        (modelField.getIsPk() ? "* " : " ") +
                        modelField.getType() + " (${type.getJavaType()} - ${type.getSqlType()})" +
                        '"/>'
            }
    }
    dynamicAutoEntityFieldSearchForm = dynamicAutoEntityFieldSearchForm + '</form></forms>'
    Document dynamicAutoEntityFieldSearchFormXml = UtilXml.readXmlDocument(dynamicAutoEntityFieldSearchForm, true, true)
    Map<String, ModelForm> modelFormMap = FormFactory.readFormDocument(dynamicAutoEntityFieldSearchFormXml, entityModelReader, dispatcher.getDispatchContext(), entityName)
    if (modelFormMap) {
        Map.Entry<String, ModelForm> entry = modelFormMap.entrySet().iterator().next()
        modelForm = entry.getValue()
    }

    String formRendererLocationTheme = context.visualTheme.getModelTheme().getFormRendererLocation("screen")
    MacroFormRenderer renderer = new MacroFormRenderer(formRendererLocationTheme, request, response)
    FormRenderer dynamicAutoEntitySearchFormRenderer = new FormRenderer(modelForm, renderer)
    Writer writer = new StringWriter()
    dynamicAutoEntitySearchFormRenderer.render(writer, context)
    context.dynamicAutoEntitySearchForm = writer

    //prepare the result list from performFind
    String dynamicAutoEntityFieldListForm = '<?xml version="1.0" encoding="UTF-8"?><forms xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://ofbiz.apache.org/Widget-Form" xsi:schemaLocation="http://ofbiz.apache.org/Widget-Form http://ofbiz.apache.org/dtds/widget-form.xsd">' +
            '<form name="ListGeneric" type="list" target="FindGeneric" list-name="listIt" ' +
            '  odd-row-style="alternate-row" default-table-style="basic-table light-grid hover-bar">' +
            '<actions><service service-name="performFind">' +
            '<field-map field-name="inputFields" from-field="parameters"/>' +
            '<field-map field-name="entityName" value="' + entityName + '"/>' +
            '</service></actions>' +
            '<auto-fields-entity entity-name="' + entityName + '" default-field-type="display" include-internal="true"/>' +
            '<field name="entityName"><hidden value="' + entityName + '"/></field>' +
            '<field name="viewGeneric" title=" "><hyperlink target="ViewGeneric" description="view">' +
            '    <auto-parameters-entity entity-name="' + entityName + '"/>' +
            '    <parameter param-name="entityName" value="' + entityName + '"/>' +
            '</hyperlink></field>' +
            '<sort-order><sort-field name="viewGeneric"/></sort-order>' +
            '</form></forms>'
    //Debug.logInfo(dynamicAutoEntityFieldForm, "")
    Document dynamicAutoEntityFieldListFormXml = UtilXml.readXmlDocument(dynamicAutoEntityFieldListForm, true, true)
    modelFormMap = FormFactory.readFormDocument(dynamicAutoEntityFieldListFormXml, entityModelReader, dispatcher.getDispatchContext(), entityName)
    if (modelFormMap) {
        Map.Entry<String, ModelForm> entry = modelFormMap.entrySet().iterator().next()
        modelForm = entry.getValue()
    }
    renderer = new MacroFormRenderer(formRendererLocationTheme, request, response)
    FormRenderer dynamicAutoEntityListFormRenderer = new FormRenderer(modelForm, renderer)
    Writer writerList = new StringWriter()
    dynamicAutoEntityListFormRenderer.render(writerList, context)
    context.dynamicAutoEntityListForm = writerList
}