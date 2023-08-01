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
package org.apache.ofbiz.content.content

import org.apache.ofbiz.base.util.UtilProperties

import org.apache.ofbiz.content.content.ContentKeywordIndex
import org.apache.ofbiz.common.UrlServletHelper
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityConditionBuilder
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.util.EntityListIterator
import org.apache.ofbiz.service.GenericServiceException

import org.apache.ofbiz.service.ModelService
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.base.util.UtilDateTime

Map createTextAndUploadedContent() {
    Map result = success()

    Map serviceResult = run service: 'createContent', with: parameters
    parameters.parentContentId = serviceResult.contentId

    if (parameters.uploadedFile) {
        logInfo('Uploaded file found; processing sub-content')
        Map uploadContext = [*: parameters,
                             ownerContentId: parentContentId,
                             contentIdFrom: parameters.parentContentId,
                             contentAssocTypeId: 'SUB_CONTENT',
                             contentPurposeTypeId: 'SECTION']
        run service: 'createContentFromUploadedFile', with: uploadContext
    }

    result.contentId = parameters.parentContentId
    return result
}

Map findAssocContent() {
    EntityCondition condition = new EntityConditionBuilder().AND {
        EQUALS(contentId: parameters.contentId)
        IN(mapKey: parameters.mapKeys)
    }
    List contentAssocs = from('ContentAssoc')
            .where(condition)
            .filterByDate()
            .cache()
            .queryList()

    Map result = success()
    if (contentAssocs) {
        result.contentAssocs = contentAssocs
    }
    return result
}

Map updateSingleContentPurpose() {
    delegator.removeByAnd('ContentPurpose', [contentId: parameters.contentId])
    run service: 'createContentPurpose', with: parameters
}

Map createEmailContent() {
    Map result = success()
    Map createContentMap = [*: parameters]

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

Map deactivateAllContentRoles() {
    List contentRoles = from('ContentRole')
            .where(contentId: parameters.contentId,
                    partyId: parameters.partyId,
                    roleTypeId: parameters.roleTypeId)
            .queryList()
    contentRoles.each {
        it.thruDate = UtilDateTime.nowTimestamp()
        it.store()
    }
    return success()
}

Map createContentAlternativeUrl() {
    //create Content Alternative URLs.
    String contentCreated = 'N'
    defaultLocaleString = parameters.locale ?: 'en'
    EntityListIterator contents

    EntityCondition entryExprs
    EntityCondition contentTypeExprs = EntityCondition.makeCondition(EntityOperator.OR,
        'contentTypeId', 'DOCUMENT',
        'contentTypeId', 'WEB_SITE_PUB_PT')
    if (parameters.contentId) {
        entryExprs = new EntityConditionBuilder().AND(contentTypeExprs) {
            NOT_EQUAL(contentName: null)
            EQUALS(contentId: parameters.contentId)
        }
    } else {
        entryExprs = new EntityConditionBuilder().AND(contentTypeExprs) {
            NOT_EQUAL(contentName: null)
        }
    }

    contents = select('contentId', 'contentName', 'localeString')
            .from('Content')
            .where(entryExprs)
            .queryIterator()

    GenericValue content
    while (content = contents.next()) {
        String localeString = content.localeString ?: defaultLocaleString
        List contentAssocDataResources = select('contentIdStart', 'dataResourceId', 'localeString', 'drObjectInfo', 'caFromDate', 'caThruDate')
                .from('ContentAssocDataResourceViewTo')
                .where(caContentAssocTypeId: 'ALTERNATIVE_URL',
                        contentIdStart: content.contentId,
                        localeString: localeString.toString())
                .filterByDate('caFromDate', 'caThruDate')
                .queryList()
        if (contentAssocDataResources) {
            if (contentAssocDataResources
                    && contentAssocDataResources[0].drObjectInfo
                    && content.contentName) {
                String uri = UrlServletHelper.invalidCharacter(content.contentName)
                if (uri) {
                    try {
                        serviceResult = run service: 'updateDataResource', with: [dataResourceId: contentAssocDataResources[0].dataResourceId,
                                      objectInfo: "/${uri}'${content.contentId}-content"]
                        if (ServiceUtil.isSuccess(serviceResult)) {
                            contentIdTo = serviceResult.contentId
                        }
                    } catch (GenericServiceException e) {
                        logError(e.getMessage())
                    }
                    contentCreated = 'Y'
                }
            }
        } else {
            if (content.contentName) {
                String uri = UrlServletHelper.invalidCharacter(content.contentName)
                if (uri) {
                    try {
                        Map serviceResult = run service: 'createDataResource', with: [dataResourceId: delegator.getNextSeqId('DataResource'),
                                                                                      dataResourceTypeId: 'URL_RESOURCE',
                                                                                      localeString: localeString.toString(),
                                                                                      objectInfo: "${uri}-${content.contentId}-content",
                                                                                      statusId: 'CTNT_IN_PROGRESS']
                        if (ServiceUtil.isSuccess(serviceResult)) {
                            dataResourceId = serviceResult.dataResourceId
                        }
                    } catch (GenericServiceException e) {
                        logError(e.getMessage())
                    }
                    if (dataResourceId) {
                        try {
                            serviceResult = run service: 'createContent', with: [dataResourceId: dataResourceId,
                                                                                 localeString: localeString.toString(),
                                                                                 statusId: 'CTNT_IN_PROGRESS']
                            if (ServiceUtil.isSuccess(serviceResult)) {
                                contentIdTo = serviceResult.contentId
                            }
                        } catch (GenericServiceException e) {
                            logError(e.getMessage())
                        }
                        if (contentIdTo) {
                            try {
                                serviceResult = run service: 'createContentAssoc', with: [contentId: content.contentId,
                                                                                          contentIdTo: contentIdTo,
                                                                                          contentAssocTypeId: 'ALTERNATIVE_URL']
                                if (ServiceUtil.isSuccess(serviceResult)) {
                                    contentIdTo = serviceResult.contentId
                                }
                            } catch (GenericServiceException e) {
                                logError(e.getMessage())
                            }
                        }
                    }
                    contentCreated = 'Y'
                }
            }
        }
    }
    return [*: success(),
            contentCreated: contentCreated]
}

Map updateEmailContent() {
    if (parameters.subjectDataResourceId) {
        run service: 'updateElectronicText', with: [dataResourceId: parameters.subjectDataResourceId,
                                                    textData: parameters.subject]
    }
    if (parameters.plainBodyDataResourceId) {
        run service: 'updateElectronicText', with: [dataResourceId: parameters.plainBodyDataResourceId,
                                                    textData: parameters.plainBody]
    }
    if (parameters.htmlBodyDataResourceId) {
        run service: 'updateElectronicText', with: [dataResourceId: parameters.htmlBodyDataResourceId,
                                                    textData: parameters.htmlBody]
    }
}

Map createArticleContent() {
    // Post a new Content article Entry
    String origContentAssocTypeId = parameters.contentAssocTypeId
    String contentAssocTypeId = parameters.contentAssocTypeId
    String ownerContentId = parameters.threadContentId
    if (origContentAssocTypeId == 'PUBLISH_LINK') {
        ownerContentId = parameters.pubPtContentId
    }
    String contentIdFrom = parameters.contentIdFrom
    String pubPtContentId = parameters.pubPtContentId
    String textData = parameters.textData
    String subDescript = null
    String contentId = null
    if (textData) {
        int textDataLen = textData.length()
        logInfo('textDataLen: ' + textDataLen)
        int descriptLen = 0
        if (parameters.descriptLen) {
            descriptLen = (int) parameters.descriptLen
            logInfo('descriptLen: ' + descriptLen)
        }
        int subStringLen = Math.min(descriptLen, textDataLen)
        logInfo('subStringLen: ' + subStringLen)
        subDescript = textData.substring(0, subStringLen)
        logInfo('subDescript: ' + subDescript)
    }
    if (contentAssocTypeId == 'PUBLISH_LINK') {
        ownerContentId = pubPtContentId
    }
    //determine of we need to create complex template structure or simple content structure
    if (parameters.uploadedFile && textData) {
        Map createMain = [dataResourceId: parameters.dataResourceId,
                          contentAssocTypeId: parameters.contentAssocTypeId,
                          contentName: parameters.contentName,
                          description: subDescript,
                          statusId: parameters.statusId,
                          contentIdFrom: parameters.contentIdFrom,
                          partyId: userLogin.partyId,
                          ownerContentId: ownerContentId,
                          dataTemplateTypeId: 'SCREEN_COMBINED',
                          mapKey: 'MAIN']
        Map serviceResult = run service: 'createContent', with: createMain
        if (ServiceUtil.isSuccess(serviceResult)) {
            contentId = serviceResult.contentId
        }
        // reset contentIdFrom to new contentId
        contentAssocTypeId = 'SUB_CONTENT'
        contentIdFrom = contentId
    }
    if (parameters.uploadedFile) {
        // create image data
        Map createImage = [dataResourceTypeId: 'LOCAL_FILE',
                           dataTemplateTypeId: 'NONE',
                           mapKey: 'IMAGE',
                           ownerContentId: ownerContentId,
                           contentName: parameters.contentName,
                           description: subDescript,
                           statusId: parameters.statusId,
                           contentAssocTypeId: contentAssocTypeId,
                           contentIdFrom: contentIdFrom,
                           partyId: userLogin.partyId,
                           uploadedFile: parameters.uploadedFile,
                           _uploadedFile_fileName: parameters._uploadedFile_fileName,
                           _uploadedFile_contentType: parameters._uploadedFile_contentType]
        Map serviceResult = run service: 'createContentFromUploadedFile', with: createImage
        String imageContentId = ServiceUtil.isSuccess(serviceResult) ? serviceResult.contentId : null
        if (!contentId) {
            contentIdFrom = imageContentId
            contentId = imageContentId
            contentAssocTypeId = 'SUB_CONTENT'
        }
    }
    if (textData) {
        Map serviceResult = run service: 'createTextContent', with: [dataResourceTypeId: 'ELECTRONIC_TEXT',
                                                                     dataTemplateTypeId: 'NONE',
                                                                     mapKey: 'MAIN',
                                                                     ownerContentId: ownerContentId,
                                                                     contentName: parameters.contentName,
                                                                     description: subDescript,
                                                                     statusId: parameters.statusId,
                                                                     contentAssocTypeId: contentAssocTypeId,
                                                                     textData: textData,
                                                                     contentIdFrom: contentIdFrom,
                                                                     partyId: userLogin.partyId]
        String textContentId = ServiceUtil.isSuccess(serviceResult) ? serviceResult.contentId : null
        if (!contentId) {
            contentIdFrom = textContentId
            contentId = textContentId
            contentAssocTypeId = 'SUB_CONTENT'
        }
    }
    // we should have a primary (at least) contentId
    if (contentId && parameters.summaryData) {
        run service: 'createTextContent', with: [dataResourceTypeId: 'ELECTRONIC_TEXT',
                                                 dataTemplateTypeId: 'NONE',
                                                 mapKey: 'SUMMARY',
                                                 ownerContentId: ownerContentId,
                                                 contentName: parameters.contentName,
                                                 description: parameters.description,
                                                 statusId: parameters.statusId,
                                                 contentAssocTypeId: contentAssocTypeId,
                                                 textData: parameters.summaryData,
                                                 contentIdFrom: contentIdFrom,
                                                 partyId: userLogin.partyId]
    }
    // If a response, still link it to the publish point
    if (origContentAssocTypeId == 'RESPONSE') {
        run service: 'createContentAssoc', with: [contentId: pubPtContentId,
                                                  contentIdTo: contentId,
                                                  contentAssocTypeId: 'RESPONSE']
    }
    Map result = success()
    result.contentId = contentId
    return result
}

Map setContentStatus() {
    Map result = success()
    GenericValue content = from('Content').where(parameters).queryOne()
    if (content) {
        String oldStatusId = content.statusId
        result.oldStatusId = oldStatusId
        if (oldStatusId != parameters.statusId) {
            GenericValue statusChange = from('StatusValidChange')
                    .where(statusId: oldStatusId,
                            statusIdTo: parameters.statusId)
                    .cache()
                    .queryOne()
            if (statusChange) {
                content.statusId = parameters.statusId
                content.store()
            } else {
                result.errorMessage = "Cannot change from ${oldStatusId} to ${parameters.statusId}"
                logError(result.errorMessage)
            }
        }
    } else {
        return failure("No Content is not available in the system with content ID - ${parameters.contentId}")
    }
    return result
}

Map createDownloadContent() {
    Map serviceResult = success()
    Map result = runService('createOtherDataResource', [dataResourceContent: parameters.file])
    if (ServiceUtil.isError(result)) {
        return result
    }
    Map serviceCtx = dispatcher.dispatchContext.makeValidContext('createContent', ModelService.IN_PARAM, parameters)
    serviceCtx.dataResourceId = result.dataResourceId
    result = runService('createContent', serviceCtx)
    if (ServiceUtil.isError(result)) {
        return result
    }
    serviceResult.contentId = result.contentId
    return serviceResult
}

Map updateDownloadContent() {
    if (parameters.fileDataResourceId) {
        return runService('updateOtherDataResource', [dataResourceId: parameters.fileDataResourceId,
                                                                  dataResourceContent: parameters.file])
    }
    return success()
}

Map getDataResource() {
    Map result = success()
    Map resultData = [:]

    GenericValue dataResource = from('DataResource').where(parameters).queryOne()
    if (dataResource) {
        resultData.dataResource = dataResource
        if (dataResource.dataResourceTypeId == 'ELECTRONIC_TEXT') {
            resultData.electronicText = dataResource.getRelatedOne('ElectronicText', false)
        }
        if (dataResource.dataResourceTypeId == 'IMAGE_OBJECT') {
            resultData.imageDataResource = dataResource.getRelatedOne('ImageDataResource', false)
        }
    }
    result.resultData = resultData
    return result
}

Map getContentAndDataResource () {
    Map result = success()
    Map resultDataContent = [:]
    GenericValue content = from('Content').where('contentId', parameters.contentId).queryOne()
    resultDataContent.content = content
    if (content && content.dataResourceId) {
        Map serviceResult = run service: 'getDataResource', with: [dataResourceId: content.dataResourceId]
        if (serviceResult) {
            Map resultData = serviceResult.resultData
            resultDataContent.dataResource = resultData.dataResource
            resultDataContent.electronicText = resultData.electronicText
            resultDataContent.imageDataResource = resultData.imageDataResource
        }
    }
    result.resultData = resultDataContent
    return result
}

/* create content from data resource
   This method will create a skeleton content record from a data resource */
Map createContentFromDataResource() {
    GenericValue dataResource = from('DataResource').where(parameters).queryOne()
    if (! dataResource) {
        return error(UtilProperties.getMessage('ContentUiLabels', 'ContentDataResourceNotFound',
                [dataResourceId: parameters.dataResourceId], parameters.locale))
    }
    parameters.contentName = parameters.contentName ?: dataResource.dataResourceName
    parameters.contentTypeId = parameters.contentTypeId ?: 'DOCUMENT'
    parameters.statusId = parameters.statusId ?: 'CTNT_INITIAL_DRAFT'
    parameters.mimeTypeId = parameters.mimeTypeId ?: dataResource.mimeTypeId
    Map result = run service: 'createContent', with: parameters
    return result
}
Map deleteContentKeywords() {
    GenericValue content = from('Content').where(parameters).queryOne()
    if (content) {
        content.removeRelated('ContentKeyword')
    }
    return success()
}

// This method first updates Content, DataResource and ElectronicText,
// ImageDataResource, etc. entities (if needed) by calling persistContentAndAssoc.
// It then takes the passed in contentId, communicationEventId and fromDate primary keys
// and calls the "updateCommEventContentAssoc" service to tie the CommunicationEvent and Content entities together.
Map updateCommContentDataResource() {
    Map serviceResult = run service: 'persistContentAndAssoc', with: parameters
    run service: 'updateCommEventContentAssoc', with: [contentId: serviceResult.contentId,
                                                       fromDate: parameters.fromDate,
                                                       communicationEventId: parameters.communicationEventId,
                                                       sequenceNum: parameters.sequenceNum,
                                                       userLogin: userLogin]

    return [*: success(),
            contentId: serviceResult.contentId,
            dataResourceId: serviceResult.dataResourceId,
            drDataResourceId: serviceResult.drDataResourceId,
            caContentIdTo: serviceResult.caContentIdTo,
            caContentId: serviceResult.caContentId,
            caContentAssocTypeId: serviceResult.caContentAssocTypeId,
            caFromDate: serviceResult.caFromDate,
            caSequenceNum: serviceResult.caSequenceNum,
            roleTypeList: serviceResult.roleTypeList]
}

Map indexContentKeywords() {
    // this service is meant to be called from an entity ECA for entities that include a contentId
    // if it is the Content entity itself triggering this action, then a [contentInstance] parameter
    // will be passed and we can save a few cycles looking that up
    GenericValue contentInstance = parameters.contentInstance ?: from('Content').where('contentId', parameters.contentId).queryOne()
    ContentKeywordIndex.indexKeywords(contentInstance)
    return success()
}

Map forceIndexContentKeywords() {
    content = from('Content').where('contentId', parameters.contentId).queryOne()
    ContentKeywordIndex.forceIndexKeywords(content)
    return success()
}

Map createSimpleTextContent() {
    Map serviceResult = run service: 'createDataResource', with: [*: parameters,
                                                                  dataResourceTypeId: 'ELECTRONIC_TEXT',
                                                                  dataTemplateTypeId: 'FTL']

    run service: 'createElectronicText', with: [*: parameters,
                                                textData: parameters.text,
                                                dataResourceId: serviceResult.dataResourceId]

    serviceResult = run service: 'createContent', with: [*: parameters,
                                                         contentTypeId: 'DOCUMENT',
                                                         dataResourceId: serviceResult.dataResourceId]

    return serviceResult
}

Map updateSimpleTextContent() {
    Map result = success()

    if (parameters.textDataResourceId) {
        run service: 'updateElectronicText', with: [dataResourceId: parameters.textDataResourceId,
                                                    textData: parameters.text]
        result.dataResourceId = parameters.textdataResourceId
        result.textData = parameters.text
    }
    return result
}

/**
 * create missing content alternative urls.
 */
Map createMissingContentAltUrls() {
    List contentCreatedList = []
    if (parameters.prodCatalogId) {
        List productCategorieIds = []
        // Get all categories
        from('ProdCatalogCategory')
                .where(prodCatalogId: parameters.prodCatalogId)
                .getFieldList('productCategoryId')
                .each { String productCategoryId ->
                    productCategorieIds.addAll(createMissingCategoryContentAltUrlInline(productCategoryId))
                }
        // Create Content Alternative URLs for Product Category Content
        EntityCondition condition = new EntityConditionBuilder().AND {
            NOT_EQUAL(prodCatContentTypeId: 'ALTERNATIVE_URL')
            IN(productCategoryId: productCategorieIds)
        }
        from('ProductCategoryContentAndInfo')
                .where(condition)
                .filterByDate()
                .orderBy('-fromDate')
                .getFieldList('contentId')
                .each { String contentId ->
                    Map serviceResult = run service: 'createContentAlternativeUrl', with: [contentId: contentId]
                    contentCreatedList << serviceResult.contentCreated
                }

        //Create Content Alternative URLs for Product Content
        condition = new EntityConditionBuilder().AND {
            IN(productCategoryId: productCategorieIds)
        }
        List productIds = from('ProductCategoryMember')
                .where(condition)
                .filterByDate()
                .orderBy('-fromDate')
                .getFieldList('productId')

        condition = new EntityConditionBuilder().AND {
            NOT_EQUAL(productContentTypeId: 'ALTERNATIVE_URL')
            IN(productId: productIds)
        }
        from('ProductContentAndInfo')
                .where(condition)
                .filterByDate()
                .orderBy('-fromDate')
                .getFieldList('contentId')
                .each { String contentId ->
                    Map serviceResult = run service: 'createContentAlternativeUrl', with: [contentId: contentId]
                    contentCreatedList << serviceResult.contentCreated
                }
    }

    //Create Content Alternative URLs for WebSite Content and children
    if (parameters.webSiteId) {
        from('WebSiteContent')
                .where(webSiteId: parameters.webSiteId)
                .getFieldList('contentId')
                .each { String contentId ->
                    contentCreatedList.addAll(createMissingContentAltUrlInline(contentId))
                }
    }
    long totalParse = contentCreatedList.size()
    long contentsUpdated = contentCreatedList.stream().filter {it == 'Y'}.count()
    return success([contentsUpdated: contentsUpdated, contentsNotUpdated: totalParse - contentsUpdated])
}

/**
 * recursive call to get all productCategoryId from children
 * @param parentProductCategoryId
 * @return
 */
List createMissingCategoryContentAltUrlInline(String parentProductCategoryId) {
    List productCategoryIds = []
    from('ProductCategoryRollup')
            .where(parentProductCategoryId: parentProductCategoryId)
            .filterByDate()
            .getFieldList('productCategoryId')
            .each { String productCategoryId ->
                productCategoryIds << productCategoryId
                productCategoryIds.addAll(createMissingCategoryContentAltUrlInline(productCategoryId))
            }
    return productCategoryIds
}

/**
 * call createContentAlternativeUrl on all subContent and return the result for analyse
 * @param contentId
 * @return
 */
List createMissingContentAltUrlInline(String contentId) {
    List contentCreatedList = []
    from('ContentAssoc')
            .where(contentId: contentId)
            .filterByDate()
            .getFieldList('contentIdTo')
            .each { String contentIdTo ->
                Map serviceResult = run service: 'createContentAlternativeUrl', with: [contentId: contentIdTo]
                contentCreatedList << serviceResult.contentCreated
                contentCreatedList.addAll(createMissingContentAltUrlInline(contentIdTo))
            }
    return contentCreatedList
}

/**
 * This method first creates Content, DataResource and ElectronicText, ImageDataResource, etc. entities (if needed)
 * by calling persistContentAndAssoc.
 *
 * It then takes the contentId coming out of that service, the passed in communicationEventId
 * and calls the "createCommEventContentAssoc" service to tie the CommunicationEvent and Content entities together.
 */
Map createCommContentDataResource() {

    // let's take a guess at what the dataResourceTypeId should be if it is empty
    if (!parameters.dataResourceTypeId && parameters.drMimeTypeId) {
        parameters.dataResourceTypeId = parameters.drMimeTypeId.startsWith('text') ?
                'ELECTRONIC_TEXT' : 'IMAGE_OBJECT'
    }
    Map persistServiceResult = run service: 'persistContentAndAssoc', with: parameters
    Map serviceResult = run service: 'createCommEventContentAssoc', with: [contentId: persistServiceResult.contentId,
                                                       communicationEventId: parameters.communicationEventId,
                                                       sequenceNum: parameters.sequenceNum]
    return success([contentId: persistServiceResult.contentId,
                    dataResourceId: persistServiceResult.dataResourceId,
                    drDataResourceId: persistServiceResult.drDataResourceId,
                    caContentIdTo: persistServiceResult.caContentIdTo,
                    caContentId: persistServiceResult.caContentId,
                    caContentAssocTypeId: persistServiceResult.caContentAssocTypeId,
                    caFromDate: persistServiceResult.caFromDate,
                    caSequenceNum: persistServiceResult.caSequenceNum,
                    roleTypeList: persistServiceResult.roleTypeList,
                    fromDate: serviceResult.fromDate])
}

/**
 * Remove a Content Record, related resource(s) and assocs.
 * @return
 */
Map removeContentAndRelated() {
    GenericValue content = from('Content').where(parameters).queryOne()
    if (content) {
        content.removeRelated('ContentAttribute')
        content.removeRelated('ContentRole')
        content.removeRelated('ContentKeyword')
        content.removeRelated('FromContentAssoc')
        content.removeRelated('ToContentAssoc')
        content.remove()
        GenericValue dataResource = content.getRelatedOne('DataResource', false)
        if (dataResource) {
            dataResource.removeRelated("ElectronicText")
            dataResource.removeRelated("ImageDataResource")
            dataResource.removeRelated("OtherDataResource")
            dataResource.removeRelated("VideoDataResource")
            dataResource.removeRelated("DataResourceRole")
            dataResource.remove()
        }
    }
    return success()
}

/**
 * copy a content, electronic text and assocs and set status in progress
 * @return
 */
Map copyContentAndElectronicTextandAssoc() {
    Map getContentResult = run service: 'getContent', with: parameters
    GenericValue content = getContentResult.view
    if (content.dataResourceId) {
        Map getElectronicTextResult = run service: 'getElectronicText', with: content.getAllFields()
        Map dataResourceResult = run service: 'createDataResource', with: [dataResourceTypeId: 'ELECTRONIC_TEXT']
        run service: 'createElectronicText', with: [dataResourceId: dataResourceResult.dataResourceId,
                                                    textData      : getElectronicTextResult.textData]
        content.dataResourceId = dataResourceResult.dataResourceId
    }
    Map contentResult = run service: 'createContent', with: [*: content.getAllFields(),
                                                             contentId: null,
                                                             statusId: null]
    from('ContentAssoc')
            .where(contentId: content.contentId)
            .queryList()
            .each {
                run service: 'createContentAssoc', with: [*: it.getAllFields(),
                                                          contentId: contentResult.contentId]
            }
    from('ContentAssoc')
            .where(contentIdTo: content.contentId)
            .queryList()
            .each {
                run service: 'createContentAssoc', with: [*: it.getAllFields(),
                                                          contentIdTo: contentResult.contentId]
            }
    return success([contentId: contentResult.contentId])
}

/**
 * @deprecated use createContentAssoc instead
 * Associate Content
 * @return
 */
Map assocContent() {
    GenericValue currentContent = from('Content').where(contentId: parameters.contentIdTo).cache().queryOne()
    GenericValue fromContent = from('Content').where(contentId: parameters.contentIdFrom).cache().queryOne()

    if (currentContent && fromContent) {
        Map serviceResult = run service: 'createContentAssoc', with: [*: parameters,
                                                                      contentId: fromContent.contentId]
        return serviceResult
    }
    return error('Contents to assoc not exist')
}
