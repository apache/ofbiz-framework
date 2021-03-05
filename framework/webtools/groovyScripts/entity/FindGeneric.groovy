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

import org.apache.ofbiz.base.util.UtilXml
import org.apache.ofbiz.entity.GenericEntityException
import org.apache.ofbiz.entity.model.ModelEntity
import org.apache.ofbiz.entity.model.ModelFieldType
import org.apache.ofbiz.entity.model.ModelReader
import org.apache.ofbiz.entity.model.ModelViewEntity
import org.apache.ofbiz.entity.model.ModelViewEntity.ModelAlias
import org.apache.ofbiz.widget.model.FormFactory
import org.apache.ofbiz.widget.model.ModelForm
import org.apache.ofbiz.widget.renderer.FormRenderer
import org.apache.ofbiz.widget.renderer.macro.MacroFormRenderer
import org.w3c.dom.Document

ModelEntity modelEntity = null
try {
    modelEntity = delegator.getModelEntity(parameters.entityName)
} catch(GenericEntityException e) {
    logError("The entityName ${parameters.entityName} isn't found", "FindGeneric.groovy")
}

if (modelEntity) {
    List<String> fieldsToSelect = getFieldsToSelect(modelEntity)

    entityName = modelEntity.entityName
    context.entityName = entityName
    ModelReader entityModelReader = delegator.getModelReader()
    //create the search form with auto-fields-entity
    String dynamicAutoEntityFieldSearchForm = """<?xml version="1.0" encoding="UTF-8"?><forms xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://ofbiz.apache.org/Widget-Form" xsi:schemaLocation="http://ofbiz.apache.org/Widget-Form http://ofbiz.apache.org/dtds/widget-form.xsd">
        <form name="FindGeneric" type="single" target="entity/find/${entityName}">
           <auto-fields-entity entity-name="${entityName}" default-field-type="find" include-internal="true"/>
            <field name="_method"><hidden value="GET"/></field>
            <field name="noConditionFind"><hidden value="Y"/></field>
            <field name="searchOptions_collapsed" ><hidden value="true"/></field>
            <field name="searchButton"><submit/></field>"""

    //call modelEntity to complete information on the field type
    modelEntity.getFieldsUnmodifiable().each {
        modelField ->
            if (! modelEntity.getAutomaticFieldNames().contains(modelField.name)) {
                ModelFieldType type = delegator.getEntityFieldType(modelEntity, modelField.getType())
                dynamicAutoEntityFieldSearchForm +=
                        "<field name=\"${modelField.name}\" tooltip=\"${modelField.getName()}" +
                                (modelField.getIsPk() ? '* ': ' ') +
                                " / ${modelField.getType()} (${type.getJavaType()} - ${type.getSqlType()})\">"

                //In general when your research some entity on the pk field, you check on element, so help by set as default equals comparison
                if (modelField.getIsPk() && type.getJavaType() == 'String') {
                    dynamicAutoEntityFieldSearchForm += '<text-find default-option="equals"/>'
                }
                dynamicAutoEntityFieldSearchForm += '</field>'
            }
    }
    dynamicAutoEntityFieldSearchForm = dynamicAutoEntityFieldSearchForm + '</form></forms>'
    logVerbose(dynamicAutoEntityFieldSearchForm)
    Document dynamicAutoEntityFieldSearchFormXml = UtilXml.readXmlDocument(dynamicAutoEntityFieldSearchForm, true, true)
    Map<String, ModelForm> modelFormMap = FormFactory.readFormDocument(dynamicAutoEntityFieldSearchFormXml,
            entityModelReader, context.visualTheme, dispatcher.getDispatchContext(), entityName)
    ModelForm modelForm
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
    String dynamicAutoEntityFieldListForm = """<?xml version="1.0" encoding="UTF-8"?><forms xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://ofbiz.apache.org/Widget-Form" xsi:schemaLocation="http://ofbiz.apache.org/Widget-Form http://ofbiz.apache.org/dtds/widget-form.xsd">
            <form name="ListGeneric" type="list" target="entity/find/${entityName}" list-name="listIt" paginate-target="entity/find/${entityName}"
              odd-row-style="alternate-row" default-table-style="basic-table light-grid hover-bar" header-row-style="header-row-2">
            <actions>
                <service service-name="performFind">
                    <field-map field-name="inputFields" from-field="parameters"/>
                    <field-map field-name="entityName" value="${entityName}"/>"""
    if (fieldsToSelect) {
    dynamicAutoEntityFieldListForm += """
                    <field-map field-name="fieldList" value="${fieldsToSelect}"/>"""
    }
    dynamicAutoEntityFieldListForm += """
                    <field-map field-name="orderBy" from-field="parameters.sortField"/>
                </service>
            </actions>
            <auto-fields-entity entity-name="${entityName}" default-field-type="display" include-internal="true"/>
            <field name="_method"><hidden value="POST"/></field>
            <field name="entityName"><hidden value="${entityName}"/></field>"""
    modelEntity.getFieldsUnmodifiable().each {
        modelField ->
            dynamicAutoEntityFieldListForm +=
                    "<field name=\"${modelField.name}\" sort-field=\"true\"/>"
    }
    dynamicAutoEntityFieldListForm += """
            <field name="viewGeneric" title=" "><hyperlink target="\${groovy: 'entity/find/' + org.apache.ofbiz.entity.util.EntityUtil.entityToPath(delegator, '${entityName}', context)}" description="view"/></field>
            <sort-order><sort-field name="viewGeneric"/></sort-order>
            </form></forms>"""

    Document dynamicAutoEntityFieldListFormXml = UtilXml.readXmlDocument(dynamicAutoEntityFieldListForm, true, true)
    modelFormMap = FormFactory.readFormDocument(dynamicAutoEntityFieldListFormXml, entityModelReader,
            context.visualTheme, dispatcher.getDispatchContext(), entityName)
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

def getFieldsToSelect(ModelEntity modelEntity) {
    groupByFields = []
    functionFields = []

    if (modelEntity instanceof ModelViewEntity) {
        aliases = modelEntity.getAliasesCopy()
        for (ModelAlias alias : aliases) {
            if (alias.getGroupBy()) {
                groupByFields.add(alias.getName())
            } else if (alias.getFunction()) {
                functionFields.add(alias.getName())
            }
        }
    }
    List<String> fieldsToSelect = []

    if (groupByFields || functionFields) {

        for (String groupByField : groupByFields) {
            fieldsToSelect.add(groupByField)
        }

        for (String functionField : functionFields) {
            fieldsToSelect.add(functionField)
        }
    }
    return fieldsToSelect
}
