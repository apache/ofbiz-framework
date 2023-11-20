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
package org.apache.ofbiz.commonext.template

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.model.ModelUtil

String getFieldTypeName(String entityName) {
    return ModelUtil.lowerFirstChar(entityName) + 'Id'
}

/**
 * For a entityTypeName and a value return the matching templating document screen
 * If no value found, do a recursive search on parentType
 */
Map getCustomScreenTemplate(String entityTypeName, String fieldTypeValue) {
    if (!fieldTypeValue) {
        return [:]
    }

    GenericValue tplCustScreen = from('PartyPrefDocTypeTplAndCustomScreen')
            .where(getFieldTypeName(entityTypeName), fieldTypeValue)
            .cache()
            .queryFirst()

    if (tplCustScreen) {
        return tplCustScreen
    }

    // No template found for this type, try if the parent had
    String parentTypeValue = from(entityTypeName)
            .where(getFieldTypeName(entityTypeName), fieldTypeValue)
            .cache()
            .queryFirst()
            .parentTypeId
    return getCustomScreenTemplate(entityTypeName, parentTypeValue)
}

// first resolve the document reference passed on the context
String entityName = ''
String entityTypeName = ''
if (parameters.orderId) {
    entityName = 'OrderHeader'
    entityTypeName = 'OrderType'
} else if (parameters.invoiceId) {
    entityName = 'Invoice'
} else if (parameters.quoteId) {
    entityName = 'Quote'
}

// second ask for this entity the custom screen to use
GenericValue tplCustScreen
if (entityName) {
    entityTypeName = entityTypeName ?: entityName + 'Type'
    GenericValue entityValue = from(entityName).where(parameters).queryOne()
    if (entityValue) {
        tplCustScreen = getCustomScreenTemplate(entityTypeName, entityValue[getFieldTypeName(entityTypeName)])
    }
}
context.templateLocation = tplCustScreen ? tplCustScreen.customScreenLocation : context.defaultTemplateLocation
context.templateName = tplCustScreen ? tplCustScreen.customScreenName : context.defaultTemplateName
