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
package org.apache.ofbiz.content

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityConditionBuilder

Map createBlogEntry() {
    String ownerContentId = parameters.blogContentId
    String contentIdFrom = parameters.blogContentId
    parameters.statusId = parameters.statusId ?: 'CTNT_INITIAL_DRAFT'
    parameters.templateDataResourceId = parameters.templateDataResourceId ?: 'BLOG_TPL_TOPLEFT'

    if (!parameters.contentName) {
        return error(label('ContentUiLabels', 'ContentArticleNameIsMissing'))
    }
    Map serviceResult = run service: 'createContent',
            with: [dataResourceId: parameters.templateDataResourceId,
                   contentAssocTypeId: 'PUBLISH_LINK',
                   contentName: parameters.contentName,
                   description: parameters.description,
                   statusId: parameters.statusId,
                   contentIdFrom: contentIdFrom,
                   partyId: userLogin.partyId,
                   ownerContentId: ownerContentId,
                   dataTemplateTypeId: 'SCREEN_COMBINED',
                   mapKey: 'MAIN']
    contentIdFrom = serviceResult.contentId

    if (parameters._uploadedFile_fileName) {
        run service: 'createContentFromUploadedFile',
                with: [
                        dataResourceTypeId: 'LOCAL_FILE',
                        dataTemplateTypeId: 'NONE',
                        mapKey: 'IMAGE',
                        ownerContentId: ownerContentId,
                        contentName: parameters.contentName,
                        description: parameters.description,
                        statusId: parameters.statusId,
                        contentAssocTypeId: 'SUB_CONTENT',
                        contentIdFrom: contentIdFrom,
                        partyId: userLogin.partyId,
                        isPublic: 'Y',
                        uploadedFile: parameters.uploadedFile,
                        _uploadedFile_fileName: parameters._uploadedFile_fileName,
                        _uploadedFile_contentType: parameters._uploadedFile_contentType]
    }
    if (parameters.articleData) {
        run service: 'createTextContent',
                with: [
                        dataResourceTypeId: 'ELECTRONIC_TEXT',
                        contentPurposeTypeId: 'ARTICLE',
                        dataTemplateTypeId: 'NONE',
                        ownerContentId: ownerContentId,
                        contentName: parameters.contentName,
                        description: parameters.description,
                        statusId: parameters.statusId,
                        contentAssocTypeId: 'SUB_CONTENT',
                        textData: parameters.articleData,
                        contentIdFrom: contentIdFrom,
                        partyId: userLogin.partyId,
                        mapKey: 'ARTICLE']
    }
    if (parameters.summaryData) {
        run service: 'createTextContent',
                with: [
                        dataResourceTypeId: 'ELECTRONIC_TEXT',
                        dataTemplateTypeId: 'NONE',
                        mapKey: 'SUMMARY',
                        ownerContentId: ownerContentId,
                        contentName: parameters.contentName,
                        description: parameters.description,
                        statusId: parameters.statusId,
                        contentAssocTypeId: 'SUB_CONTENT',
                        textData: parameters.summaryData,
                        contentIdFrom: contentIdFrom,
                        partyId: userLogin.partyId]
    }
    return success([blogContentId: ownerContentId, contentId: contentIdFrom])
}

/**
 * Get all the info for a blog article
 */
Map getBlogEntry() {
    if (!parameters.contentId) {
        return success([blogContentId: parameters.blogContentId])
    }
    GenericValue content = from('Content').where(parameters).cache().queryOne()
    GenericValue mainContent, articleText, dataResource, summaryContent, summaryText, imageContent
    from('ContentAssoc')
        .where(contentId: content.contentId)
        .filterByDate()
        .cache()
        .queryList()
        .each {
            switch (it.mapKey) {
                case 'ARTICLE':
                    mainContent = it.getRelatedOne('ToContent', true)
                    dataResource = mainContent.getRelatedOne('DataResource', true)
                    articleText = dataResource.getRelatedOne('ElectronicText', true)
                    break
                case 'SUMMARY':
                    summaryContent = it.getRelatedOne('ToContent', true)
                    dataResource = summaryContent.getRelatedOne('DataResource', true)
                    summaryText = dataResource.getRelatedOne('ElectronicText', true)
                    break
                case 'IMAGE':
                    imageContent = it.getRelatedOne('ToContent', true)
                    break
            }
        }

    Map resultMap = [blogContentId: parameters.blogContentId,
                     contentId: content.contentId,
                     contentName: content.contentName,
                     description: content.description,
                     statusId: content.statusId]
    if (imageContent) {
        resultMap.templateDataResourceId = content.dataResourceId
        resultMap.imageContentId = imageContent.contentId
        resultMap.imageDataResourceId = imageContent.dataResourceId
    }
    if (mainContent) {
        resultMap.articleData = articleText.textData
        resultMap.articleContentId = mainContent.contentId
        resultMap.articleDataResourceId = mainContent.dataResourceId
    }
    if (summaryContent) {
        resultMap.summaryData = summaryText.textData
        resultMap.summaryContentId = summaryContent.contentId
        resultMap.summaryDataResourceId = summaryContent.dataResourceId
    }
    return success(resultMap)
}

/**
 *  Update a existing Blog Entry
 */
Map updateBlogEntry() {
    Map blog = run service: 'getBlogEntry', with: parameters
    if (['contentName', 'description', 'summaryData', 'templateDataResourceId', 'statusId']
            .stream().anyMatch { blog[ it ] != parameters[ it ] }) {
        run service: 'updateContent', with: [*: parameters,
                                             dataResourceId: parameters.templateDataResourceId]
        if (parameters.statusId != blog.statusId
                && blog.imageContentId) {
            delegator.storeByCondition('Content', [statusId: parameters.statusId],
                    EntityCondition.makeCondition('contentId', blog.imageContentId))
        }
    }

    if (!blog.articleText && parameters.articleData) {
        run service: 'createTextContent',
                with: [
                        dataResourceTypeId: 'ELECTRONIC_TEXT',
                        contentPurposeTypeId: 'ARTICLE',
                        dataTemplateTypeId: 'NONE',
                        ownerContentId: parameters.blogContentId,
                        contentName: parameters.contentName,
                        description: parameters.description,
                        statusId: parameters.statusId,
                        contentAssocTypeId: 'SUB_CONTENT',
                        textData: parameters.articleData,
                        contentIdFrom: blog.contentId,
                        partyId: userLogin.partyId,
                        mapKey: 'ARTICLE']
    }
    if (blog.articleData && parameters.articleData != blog.articleData) {
        run service: 'updateElectronicText', with: [dataResourceId: blog.articleDataResourceId,
                                                    textData: parameters.articleData]
    }
    if (!blog.summaryData && parameters.summaryData) {
        run service: 'createTextContent',
                with: [
                        dataResourceTypeId: 'ELECTRONIC_TEXT',
                        dataTemplateTypeId: 'NONE',
                        mapKey: 'SUMMARY',
                        ownerContentId: parameters.blogContentId,
                        contentName: parameters.contentName,
                        description: parameters.description,
                        statusId: parameters.statusId,
                        contentAssocTypeId: 'SUB_CONTENT',
                        textData: parameters.summaryData,
                        contentIdFrom: blog.contentId,
                        partyId: userLogin.partyId]
    }
    if (blog.summaryData && parameters.summaryData != blog.summaryData) {
        run service: 'updateElectronicText', with: [dataResourceId: blog.summaryDataResourceId,
                                                    textData: parameters.summaryData]
    }

    if (parameters._uploadedFile_fileName) {
        if (blog.imageContentId) {
            EntityCondition condition = new EntityConditionBuilder().AND {
                EQUALS(contentId: blog.contentId)
                EQUALS(contentIdTo: blog.imageContentId)
                EQUALS(mapKey: 'IMAGE')
                EQUALS(thruDate: null)
            }
            delegator.storeByCondition('ContentAssoc', [thruDate: UtilDateTime.nowTimestamp()], condition)
        }
        run service: 'createContentFromUploadedFile',
                with: [
                        isPublic: 'Y',
                        dataTemplateTypeId: 'NONE',
                        mapKey: 'IMAGE',
                        dataResourceTypeId: 'LOCAL_FILE',
                        contentAssocTypeId: 'SUB_CONTENT',
                        ownerContentId: parameters.blogContentId,
                        contentName: parameters.contentName,
                        description: parameters.description,
                        statusId: parameters.statusId,
                        contentIdFrom: blog.contentId,
                        partyId: userLogin.partyId,
                        uploadedFile: parameters.uploadedFile,
                        _uploadedFile_fileName: parameters._uploadedFile_fileName,
                        _uploadedFile_contentType: parameters._uploadedFile_contentType]
    }
    return success([blogContentId: parameters.blogContentId, contentId: blog.contentId])
}

/**
 * Get blog entries that the user owns or are published
 */
Map getOwnedOrPublishedBlogEntries() {
    List blogList = []
    from('ContentAssocViewTo')
        .where(contentIdStart: parameters.contentId,
                caContentAssocTypeId: 'PUBLISH_LINK')
        .orderBy('-caFromDate')
        .filterByDate()
        .cache()
        .queryList()
        .each {
            Map serviceResult = run service: 'genericContentPermission',
                    with: [*: it.getAllFields(),
                           ownerContentId: parameters.contentId,
                           mainAction: 'VIEW']
            if (!serviceResult.hasPermission) {
                serviceResult = run service: 'genericContentPermission',
                        with: [*: it.getAllFields(),
                               ownerContentId: parameters.contentId,
                               mainAction: 'UPDATE']
            }
            if (serviceResult.hasPermission) {
                blogList << it
            }
        }
    return success([blogList: blogList,
                    blogContentId: parameters.blogContentId])
}
