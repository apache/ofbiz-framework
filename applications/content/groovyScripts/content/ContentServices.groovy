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
import org.apache.ofbiz.common.UrlServletHelper
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.util.EntityListIterator
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ModelService
import org.apache.ofbiz.service.ServiceUtil

MODULE = "ContentServices.groovy"
def createTextAndUploadedContent(){
    Map result = success()

    Map serviceResult = run service: 'createContent', with: parameters
    parameters.parentContentId = serviceResult.contentId

    if (parameters.uploadedFile) {
        logInfo('Uploaded file found; processing sub-content')
        Map uploadContext = dispatcher.getDispatchContext()
                .makeValidContext('createContentFromUploadedFile', ModelService.IN_PARAM, parameters)
        uploadContext.ownerContentId = parameters.parentContentId
        uploadContext.contentIdFrom = parameters.parentContentId
        uploadContext.contentAssocTypeId = 'SUB_CONTENT'
        uploadContext.contentPurposeTypeId = 'SECTION'
        run service: 'createContentFromUploadedFile', with: uploadContext
    }

    result.contentId = parameters.parentContentId
    return result
}

def createContentAlternativeUrl() {
    //create Content Alternative URLs.
    String contentCreated
    Map serviceResult = [:]
    Map serviceContext = [:]
    defaultLocaleString = parameters.locale ?: "en"
    EntityListIterator contents

    if (parameters.contentId) {
        entryExprs = EntityCondition.makeCondition([
                EntityCondition.makeCondition("contentName", EntityOperator.NOT_EQUAL, null),
                EntityCondition.makeCondition("contentId", EntityOperator.EQUALS, parameters.contentId),
                EntityCondition.makeCondition([
                        EntityCondition.makeCondition("contentTypeId", EntityOperator.EQUALS, "DOCUMENT"),
                        EntityCondition.makeCondition("contentTypeId", EntityOperator.EQUALS, "WEB_SITE_PUB_PT")], EntityOperator.OR)
        ], EntityOperator.AND)
    } else {
        entryExprs = EntityCondition.makeCondition([
                EntityCondition.makeCondition("contentName", EntityOperator.NOT_EQUAL, null),
                EntityCondition.makeCondition([
                        EntityCondition.makeCondition("contentTypeId", EntityOperator.EQUALS, "DOCUMENT"),
                        EntityCondition.makeCondition("contentTypeId", EntityOperator.EQUALS, "WEB_SITE_PUB_PT")], EntityOperator.OR)
        ], EntityOperator.AND)
    }

    contents = select("contentId", "contentName", "localeString").from("Content").where(entryExprs).queryIterator()
    List contentAssocDataResources = []
    String localeString

    while (content = contents.next()) {
        localeString = content.localeString ?: defaultLocaleString
        contentAssocDataResources = select("contentIdStart", "dataResourceId", "localeString", "drObjectInfo", "caFromDate", "caThruDate").from("ContentAssocDataResourceViewTo").
                where("caContentAssocTypeId", "ALTERNATIVE_URL", "contentIdStart", content.contentId, "localeString", localeString.toString()).filterByDate("caFromDate", "caThruDate").queryList()
        if (!contentAssocDataResources) {
            if (content.contentName) {
                uri = UrlServletHelper.invalidCharacter(content.contentName)
                if (uri) {
                    serviceContext.dataResourceId = delegator.getNextSeqId("DataResource")
                    serviceContext.dataResourceTypeId = "URL_RESOURCE"
                    serviceContext.localeString = localeString.toString()
                    serviceContext.objectInfo = uri + "-" + content.contentId + "-content"
                    serviceContext.statusId = "CTNT_IN_PROGRESS"
                    serviceContext.userLogin = userLogin
                    try {
                        serviceResult = run service: "createDataResource", with: serviceContext
                        if (ServiceUtil.isSuccess(serviceResult)) {
                            dataResourceId = serviceResult.dataResourceId
                        }
                    } catch (GenericServiceException e) {
                        Debug.logInfo(e, MODULE)
                    }
                    if (dataResourceId) {
                        serviceContext.clear()
                        serviceContext.dataResourceId = dataResourceId
                        serviceContext.statusId = "CTNT_IN_PROGRESS"
                        serviceContext.localeString = localeString.toString()
                        serviceContext.userLogin = userLogin
                        try {
                            serviceResult = run service: "createContent", with: serviceContext
                            if (ServiceUtil.isSuccess(serviceResult)) {
                                contentIdTo = serviceResult.contentId
                            }
                        } catch (GenericServiceException e) {
                            Debug.logInfo(e, MODULE)
                        }
                        if (contentIdTo) {
                            serviceContext.clear()
                            serviceContext.contentId = content.contentId
                            serviceContext.contentIdTo = contentIdTo
                            serviceContext.contentAssocTypeId = "ALTERNATIVE_URL"
                            serviceContext.userLogin = userLogin
                            try {
                                serviceResult = run service: "createContentAssoc", with: serviceContext
                                if (ServiceUtil.isSuccess(serviceResult)) {
                                    contentIdTo = serviceResult.contentId
                                }
                            } catch (GenericServiceException e) {
                                Debug.logInfo(e, MODULE)
                            }
                        }
                    }
                    contentCreated = "Y"
                }
            }
        } else {
            if (contentAssocDataResources && contentAssocDataResources.get(0).drObjectInfo) {
                if (content.contentName) {
                    uri = UrlServletHelper.invalidCharacter(content.contentName)
                    if (uri) {
                        serviceContext.clear()
                        serviceContext.dataResourceId = contentAssocDataResources.get(0).dataResourceId
                        serviceContext.objectInfo = "/" + uri + "-" + content.contentId + "-content"
                        serviceContext.userLogin = userLogin
                        try {
                            serviceResult = run service: "updateDataResource", with: serviceContext
                            if (ServiceUtil.isSuccess(serviceResult)) {
                                contentIdTo = serviceResult.contentId
                            }
                        } catch (GenericServiceException e) {
                            Debug.logInfo(e, MODULE)
                        }
                        contentCreated = "Y"
                    }
                }
            } else {
                contentCreated = "N"
            }
        }
    }

    map = success()
    map.contentCreated = contentCreated
    return map
}

def updateEmailContent() {
    if (parameters.subjectDataResourceId) {
        run service: "updateElectronicText", with: [dataResourceId: parameters.subjectDataResourceId, textData: parameters.subject]
    }
    if (parameters.plainBodyDataResourceId) {
        run service: "updateElectronicText", with: [dataResourceId: parameters.plainBodyDataResourceId, textData: parameters.plainBody]
    }
    if (parameters.htmlBodyDataResourceId) {
        run service: "updateElectronicText", with: [dataResourceId: parameters.htmlBodyDataResourceId, textData: parameters.htmlBody]
    }
}

def createArticleContent() {
    // Post a new Content article Entry
    String origContentAssocTypeId = parameters.contentAssocTypeId
    String contentAssocTypeId = parameters.contentAssocTypeId
    String ownerContentId = parameters.threadContentId
    if ("PUBLISH_LINK".equals(origContentAssocTypeId)) {
        ownerContentId = parameters.pubPtContentId
    }
    String contentIdFrom = parameters.contentIdFrom
    String pubPtContentId = parameters.pubPtContentId
    String textData = parameters.textData
    String subDescript = null
    String contentId = null
    if (textData) {
        int textDataLen = textData.length()
        Debug.logInfo("textDataLen: " + textDataLen, module)
        int descriptLen = 0
        if (parameters.descriptLen) {
            descriptLen = (int) parameters.descriptLen
            Debug.logInfo("descriptLen: " + descriptLen, module)
        }
        int subStringLen = Math.min(descriptLen, textDataLen)
        Debug.logInfo("subStringLen: " + subStringLen, module)
        subDescript = textData.substring(0, subStringLen)
        Debug.logInfo("subDescript: " + subDescript, module)
    }
    if ("PUBLISH_LINK".equals(contentAssocTypeId)) {
        ownerContentId = pubPtContentId
    }
    //determine of we need to create complex template structure or simple content structure
    if (parameters.uploadedFile && textData) {
        Map createMain = [:]
        createMain.dataResourceId = parameters.dataResourceId
        createMain.contentAssocTypeId = parameters.contentAssocTypeId
        createMain.contentName = parameters.contentName
        createMain.description = subDescript
        createMain.statusId = parameters.statusId
        createMain.contentIdFrom = parameters.contentIdFrom
        createMain.partyId = userLogin.partyId
        createMain.ownerContentId = ownerContentId

        createMain.dataTemplateTypeId = "SCREEN_COMBINED"
        createMain.mapKey = "MAIN"
        Map serviceResult = run service: "createContent", with: createMain
        if (ServiceUtil.isSuccess(serviceResult)) {
            contentId = serviceResult.contentId
        }
        // reset contentIdFrom to new contentId
        contentAssocTypeId = "SUB_CONTENT"
        contentIdFrom = contentId
    }
    if (parameters.uploadedFile) {
        // create image data
        Map createImage = [:]
        createImage.dataResourceTypeId = "LOCAL_FILE"
        createImage.dataTemplateTypeId = "NONE"
        createImage.mapKey = "IMAGE"
        createImage.ownerContentId = ownerContentId
        createImage.contentName = parameters.contentName
        createImage.description = subDescript
        createImage.statusId = parameters.statusId
        createImage.contentAssocTypeId = contentAssocTypeId
        createImage.contentIdFrom = contentIdFrom
        createImage.partyId = userLogin.partyId
        createImage.uploadedFile = parameters.uploadedFile
        createImage._uploadedFile_fileName = parameters._uploadedFile_fileName
        createImage._uploadedFile_contentType = parameters._uploadedFile_contentType
        Map serviceResult = run service: "createContentFromUploadedFile", with: createImage
        String imageContentId = ServiceUtil.isSuccess(serviceResult)? serviceResult.contentId : null
        if (!contentId) {
            contentIdFrom = imageContentId
            contentId = imageContentId
            contentAssocTypeId = "SUB_CONTENT"
        }
    }
    if (textData) {
        Map createText = [:]
        createText.dataResourceTypeId = "ELECTRONIC_TEXT"
        createText.dataTemplateTypeId = "NONE"
        createText.mapKey = "MAIN"
        createText.ownerContentId = ownerContentId
        createText.contentName = parameters.contentName
        createText.description = subDescript
        createText.statusId = parameters.statusId
        createText.contentAssocTypeId = contentAssocTypeId
        createText.textData = textData
        createText.contentIdFrom = contentIdFrom
        createText.partyId = userLogin.partyId
        Debug.logInfo("calling createTextContent with map: " + createText, module)
        Map serviceResult = run service: "createTextContent", with: createText
        String textContentId = ServiceUtil.isSuccess(serviceResult)? serviceResult.contentId : null
        if (!contentId) {
            contentIdFrom = textContentId
            contentId = textContentId
            contentAssocTypeId = "SUB_CONTENT"
        }
    }
    // we should have a primary (at least) contentId
    if (contentId && parameters.summaryData) {
        Map createSummary = [:]
        createSummary.dataResourceTypeId = "ELECTRONIC_TEXT"
        createSummary.dataTemplateTypeId = "NONE"
        createSummary.mapKey = "SUMMARY"
        createSummary.ownerContentId = ownerContentId
        createSummary.contentName = parameters.contentName
        createSummary.description = parameters.description
        createSummary.statusId = parameters.statusId
        createSummary.contentAssocTypeId = contentAssocTypeId
        createSummary.textData = parameters.summaryData
        createSummary.contentIdFrom = contentIdFrom
        createSummary.partyId = userLogin.partyId
        run service: "createTextContent", with:  createSummary
    }
    // If a response, still link it to the publish point
    if ("RESPONSE".equals(origContentAssocTypeId)) {
        Map contentAssocMap = [:]
        contentAssocMap.contentId = pubPtContentId
        contentAssocMap.contentIdTo = contentId
        contentAssocMap.contentAssocTypeId = "RESPONSE"
        Debug.logInfo("contentAssocMap: " + contentAssocMap, module)
        run service: "createContentAssoc", with: contentAssocMap
    }
    Map result = success()
    result.contentId = contentId
    return result
}
