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
package org.apache.ofbiz.webtools.service

import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.base.util.UtilXml
import org.apache.ofbiz.entity.Delegator
import org.apache.ofbiz.entity.GenericEntityException
import org.apache.ofbiz.entity.model.ModelEntity
import org.apache.ofbiz.entity.model.ModelField
import org.apache.ofbiz.service.ModelParam
import org.apache.ofbiz.service.ModelService
import org.apache.ofbiz.service.config.ServiceConfigUtil
import org.apache.ofbiz.widget.model.FormFactory
import org.apache.ofbiz.widget.model.ModelForm
import org.apache.ofbiz.widget.model.ModelFormFieldBuilder
import org.apache.ofbiz.widget.renderer.macro.MacroFormRenderer
import org.w3c.dom.Document

savedSyncResult = null
if (session.getAttribute('_SAVED_SYNC_RESULT_') != null) {
    savedSyncResult = session.getAttribute('_SAVED_SYNC_RESULT_')
}

serviceName = parameters.SERVICE_NAME
context.POOL_NAME = ServiceConfigUtil.getServiceEngine().getThreadPool().getSendToPool()

scheduleOptions = []
serviceParameters = []
e = request.getParameterNames()
while (e.hasMoreElements()) {
    paramName = e.nextElement()
    paramValue = parameters[paramName]
    scheduleOptions.add([name: paramName, value: paramValue])
}

context.scheduleOptions = scheduleOptions

if (serviceName) {
    dctx = dispatcher.getDispatchContext()
    ModelService modelService = null
    try {
        modelService = dctx.getModelService(serviceName)
    } catch (Exception exc) {
        context.errorMessageList = [exc.getMessage()]
    }

    context.uiLabelMap = UtilProperties.getResourceBundleMap('CommonUiLabels', locale)
    String formRendererLocationTheme = context.visualTheme.getModelTheme().getFormRendererLocation('screen')
    MacroFormRenderer renderer = new MacroFormRenderer(formRendererLocationTheme, request, response)
    String dynamicServiceForm = '<?xml version="1.0" encoding="UTF-8"?><forms><form name="scheduleForm" type="single"/></forms>'
    Document dynamicServiceFormXml = UtilXml.readXmlDocument(dynamicServiceForm, true, true)
    Map<String, ModelForm> modelFormMap = FormFactory.readFormDocument(dynamicServiceFormXml, null, context.visualTheme,
        dispatcher.getDispatchContext(), null)
    ModelForm form
    if (modelFormMap) {
        Map.Entry<String, ModelForm> entry = modelFormMap.entrySet().iterator().next()
        form = entry.getValue()
    }

    if (modelService != null) {
        modelService.getInParamNames().each { paramName ->
            ModelParam modelParam = modelService.getParam(paramName)
            if (modelParam.internal) {
                return
            }
            serviceParam = null
            if (savedSyncResult?.get(modelParam.name)) {
                serviceParam = [name: modelParam.name, type: modelParam.type, optional: modelParam.optional ? 'Y' : 'N',
                                defaultValue: modelParam.defaultValue, value: savedSyncResult.get(modelParam.name)]
            } else {
                serviceParam = [name: modelParam.name, type: modelParam.type, optional: modelParam.optional ? 'Y' : 'N',
                                defaultValue: modelParam.defaultValue]
            }
            serviceParam.field = prepareServiceParamFieldHtml(delegator, modelParam, form, context, renderer, modelService)

            serviceParameters.add(serviceParam)
        }
    }
}
context.serviceParameters = serviceParameters


private String prepareServiceParamFieldHtml(Delegator delegator, ModelParam modelParam, ModelForm form,
        Map context, MacroFormRenderer renderer, ModelService modelService) {
    Writer writer = new StringWriter()
    ModelFormFieldBuilder builder = new ModelFormFieldBuilder()
    boolean isEntityField = false
    if (modelParam.getEntityName() && modelParam.getFieldName()) {
        try {
            ModelEntity modelEntity = delegator.getModelEntity(modelParam.getEntityName())
            ModelField modelField = modelEntity.getField(modelParam.getFieldName())
            if (modelField != null) {
                isEntityField = true
                prepareEntityFieldBuilder(builder, modelField, modelEntity)
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, 'SetServiceParameters.groovy')
        }
    }
    if (!isEntityField) {
        prepareServiceFieldBuilder(builder, modelParam, modelService)
    }
    builder.setModelForm(form)
    builder.setAttributeName(modelParam.getName())
    builder.setTitle(modelParam.getFormLabel())
    builder.setRequiredField(!modelParam.isOptional())
    builder.build().renderFieldString(writer, context, renderer)
    return writer.toString()
}

private void prepareEntityFieldBuilder(ModelFormFieldBuilder builder, ModelField modelField, ModelEntity modelEntity) {
    builder.setName(modelField.getName())
    builder.setFieldName(modelField.getName())
    builder.setEntityName(modelEntity.getEntityName())
    builder.induceFieldInfoFromEntityField(modelEntity, modelField, 'edit')
}

private void prepareServiceFieldBuilder(ModelFormFieldBuilder builder, ModelParam modelParam, ModelService modelService) {
    builder.setName(modelParam.getName())
    builder.setFieldName(modelParam.getName())
    builder.setServiceName(modelService.getName())
    builder.induceFieldInfoFromServiceParam(modelService, modelParam, 'edit')
}
