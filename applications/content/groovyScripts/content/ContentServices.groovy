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

import java.sql.Timestamp

import org.apache.ofbiz.content.content.ContentKeywordIndex
import org.apache.ofbiz.common.UrlServletHelper
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityListIterator
import org.apache.ofbiz.service.GenericServiceException;

import org.apache.ofbiz.service.ModelService
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.base.util.UtilDateTime

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

def createEmailContent() {
    Map result = success()
    Map createContentMap = dispatcher.getDispatchContext()
            .makeValidContext('createContent', ModelService.IN_PARAM, parameters)

    //Create subject
    Map serviceResult = run service: 'createElectronicText', with: [textData: parameters.subject]
    createContentMap.dataResourceId = serviceResult.dataResourceId
    serviceResult = run service: 'createContent', with: createContentMap

    //Create plain body and assoc with subject
    Map createBodyAssoc = [contentId: serviceResult.contentId,
                           contentAssocTypeId: 'TREE_CHILD',
                           mapKey: 'plainBody']

    serviceResult = run service: 'createElectronicText', with: [textData: parameters.plainBody]
    createContentMap.dataResourceId = serviceResult.dataResourceId
    serviceResult = run service: 'createContent', with: createContentMap

    createBodyAssoc.contentIdTo = serviceResult.contentId

    run service: 'createContentAssoc', with: createBodyAssoc
    result.contentId = createBodyAssoc.contentId

    if (parameters.htmlBody) {
        serviceResult = run service: 'createElectronicText', with: [textData: parameters.htmlBody]
        createContentMap.dataResourceId = serviceResult.dataResourceId
        serviceResult = run service: 'createContent', with: createContentMap
        createBodyAssoc.contentIdTo = serviceResult.contentId
        createBodyAssoc.mapKey = 'htmlBody'
        run service: 'createContentAssoc', with: createBodyAssoc
    }

    return result
}

def deactivateAllContentRoles() {
    List contentRoles = from("ContentRole").
            where("contentId", parameters.contentId, "partyId", parameters.partyId, "roleTypeId", parameters.roleTypeId)
            .queryList();
    if (contentRoles) {
        for (GenericValue contentRole : contentRoles) {
            contentRole.put("thruDate", UtilDateTime.nowTimestamp());
            contentRole.store();
        }
    }
    return success()
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
                        logError(e.getMessage())
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
                            logError(e.getMessage())
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
                                logError(e.getMessage())
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
                            logError(e.getMessage())
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
        logInfo("textDataLen: " + textDataLen)
        int descriptLen = 0
        if (parameters.descriptLen) {
            descriptLen = (int) parameters.descriptLen
            logInfo("descriptLen: " + descriptLen)
        }
        int subStringLen = Math.min(descriptLen, textDataLen)
        logInfo("subStringLen: " + subStringLen)
        subDescript = textData.substring(0, subStringLen)
        logInfo("subDescript: " + subDescript)
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
        logInfo("calling createTextContent with map: " + createText)
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
        logInfo("contentAssocMap: " + contentAssocMap)
        run service: "createContentAssoc", with: contentAssocMap
    }
    Map result = success()
    result.contentId = contentId
    return result
}
def setContentStatus() {
    Map resultMap = new HashMap()
    content = from("Content").where("contentId", parameters.contentId).queryOne()
    if (content) {
        oldStatusId = content.statusId
        resultMap.oldStatusId = oldStatusId
        if (!oldStatusId.equals(parameters.statusId)) {
            statusChange = from("StatusValidChange").where("statusId", oldStatusId, "statusIdTo", parameters.statusId).queryOne()
            if (statusChange) {
                content.put("statusId", parameters.statusId)
                content.store()
            } else {
                resultMap.errorMessage = "Cannot change from " + oldStatusId + " to " + parameters.statusId
                logError(resultMap.errorMessage)
            }
        }
    } else {
        return failure("No Content is not available in the system with content ID - " + parameters.contentId)
    }
    return resultMap
}
def createDownloadContent() {
    Map serviceResult = success()
    result = runService("createOtherDataResource", [dataResourceContent : parameters.file])
    if (ServiceUtil.isError(result)) return result
    Map serviceCtx = dispatcher.dispatchContext.makeValidContext("createContent", ModelService.IN_PARAM, parameters)
    serviceCtx.dataResourceId = result.dataResourceId
    result = runService("createContent", serviceCtx)
    if (ServiceUtil.isError(result)) return result
    serviceResult.contentId = result.contentId
    return serviceResult;
}
def updateDownloadContent() {
    Map result = success()
    if(parameters.fileDataResourceId) {
        result = runService("updateOtherDataResource", [dataResourceId: parameters.fileDataResourceId, dataResourceContent: parameters.file])
    }
    return result
}
def getDataResource() {
    Map result = success()
    resultData = [:]

    GenericValue dataResource = from('DataResource').where(parameters).queryOne()
    if (dataResource) {
        resultData.dataResource = dataResource
        if ("ELECTRONIC_TEXT" == dataResource.dataResourceTypeId) {
            resultData.electronicText = dataResource.getRelatedOne('ElectronicText', false)
        }
        if ("IMAGE_OBJECT" == dataResource.dataResourceTypeId) {
            resultData.imageDataResource = dataResource.getRelatedOne('ImageDataResource', false)
        }
    }
    result.resultData = resultData
    return result
}
def getContentAndDataResource () {
    resultMap = [:];
    resultDataContent = [:];
    content = from("Content").where("contentId", parameters.contentId).queryOne();
    resultDataContent.content = content;
    if (content && content.dataResourceId) {
        result = runService("getDataResource", ["dataResourceId": content.dataResourceId, "userLogin": userLogin]);
        if (result) {
            resultData = result.resultData;
            resultDataContent.dataResource = resultData.dataResource;
            resultDataContent.electronicText = resultData.electronicText;
            resultDataContent.imageDataResource = resultData.imageDataResource;
        }
    }
    resultMap.resultData = resultDataContent;
    return resultMap;
}
/* create content from data resource */
/*This method will create a skeleton content record from a data resource */
def createContentFromDataResource() {
    dataResource = from("DataResource").where("dataResourceId", parameters.dataResourceId).queryOne()
    if (dataResource == null) {
            return error(UtilProperties.getMessage("ContentUiLabels", "ContentDataResourceNotFound", UtilMisc.toMap("parameters.dataResourceId", parameters.dataResourceId), parameters.locale))
        }
    Map createContentMap = dispatcher.getDispatchContext().makeValidContext('createContent', ModelService.IN_PARAM, parameters)
    if (!(createContentMap.contentName)) {
        createContentMap.contentName = dataResource.dataResourceName
    }
    if (!(createContentMap.contentTypeId)) {
        createContentMap.contentTypeId = "DOCUMENT"
    }
    if (!(createContentMap.statusId)) {
        createContentMap.statusId = "CTNT_INITIAL_DRAFT"
    }
    if (!(createContentMap.mimeTypeId)) {
        createContentMap.mimeTypeId = dataResource.mimeTypeId
    }
    Map result = run service: "createContent", with: createContentMap
    return result
}
def deleteContentKeywords() {
    content = from('Content').where('contentId', contentId).queryOne()
    content.removeRelated('ContentKeyword')
    return success()
}

// This method first updates Content, DataResource and ElectronicText,
// ImageDataResource, etc. entities (if needed) by calling persistContentAndAssoc.
// It then takes the passed in contentId, communicationEventId and fromDate primary keys
// and calls the "updateCommEventContentAssoc" service to tie the CommunicationEvent and Content entities together.
def updateCommContentDataResource() {
    Map serviceResult = run service: 'persistContentAndAssoc', with: parameters
    run service: 'updateCommEventContentAssoc', with: [contentId           : serviceResult.contentId,
                                                       fromDate            : parameters.fromDate,
                                                       communicationEventId: parameters.communicationEventId,
                                                       sequenceNum         : parameters.sequenceNum,
                                                       userLogin           : userLogin]

    return [*                   : success(),
            contentId           : serviceResult.contentId,
            dataResourceId      : serviceResult.dataResourceId,
            drDataResourceId    : serviceResult.drDataResourceId,
            caContentIdTo       : serviceResult.caContentIdTo,
            caContentId         : serviceResult.caContentId,
            caContentAssocTypeId: serviceResult.caContentAssocTypeId,
            caFromDate          : serviceResult.caFromDate,
            caSequenceNum       : serviceResult.caSequenceNum,
            roleTypeList        : serviceResult.roleTypeList]
}

def indexContentKeywords() {
    // this service is meant to be called from an entity ECA for entities that include a contentId
    // if it is the Content entity itself triggering this action, then a [contentInstance] parameter
    // will be passed and we can save a few cycles looking that up
    contentInstance = parameters.contentInstance
    if (!contentInstance) {
        contentInstance = from("Content").where("contentId", parameters.contentId).queryOne()
    }
    ContentKeywordIndex.indexKeywords(contentInstance)
    return success()
}

def forceIndexContentKeywords() {
    content = from("Content").where("contentId", parameters.contentId).queryOne()
    ContentKeywordIndex.forceIndexKeywords(content)
    return success()
}
