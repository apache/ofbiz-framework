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

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.service.ModelService


/**
 * Create Content For Product Category
 */
def createCategoryContent() {
    GenericValue newEntity = makeValue("ProductCategoryContent")
    newEntity.setPKFields(parameters, true)
    newEntity.setNonPKFields(parameters, true)

    if(!newEntity.fromDate) {
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
        newEntity.fromDate  = nowTimestamp
    }

    newEntity.create()

    Map updateContent = dispatcher.getDispatchContext().makeValidContext("updateContent", ModelService.IN_PARAM, parameters)
    run service: "updateContent", with: updateContent
    Map result = success()
    result.contentId = newEntity.contentId
    result.productCategoryId = newEntity.productCategoryId
    result.prodCatContentTypeId = newEntity.prodCatContentTypeId
    return result
}

/**
 * Update Content For Category
 */
def updateCategoryContent() {
    GenericValue lookupPKMap = makeValue("ProductCategoryContent")
    lookupPKMap.setPKFields(parameters, true)
    Map lookedUpValue = from("ProductCategoryContent").where(lookupPKMap).queryOne()
    lookedUpValue.setNonPKFields(parameters, true)
    lookedUpValue.store()

    Map updateContent = dispatcher.getDispatchContext().makeValidContext("updateContent", ModelService.IN_PARAM, parameters)
    run service: "updateContent", with: updateContent
}

/**
 * Create Simple Text Content For Product Category
 */
def createSimpleTextContentForCategory() {
    Map createCategoryContentMap = dispatcher.getDispatchContext().makeValidContext("createCategoryContent", ModelService.IN_PARAM, parameters)
    Map createSimpleTextMap = dispatcher.getDispatchContext().makeValidContext("createSimpleTextContent", ModelService.IN_PARAM, parameters)
    Map cstcRes = run service: "createSimpleTextContent", with: createSimpleTextMap
    createCategoryContentMap.contentId = cstcRes.contentId
    run service: "createCategoryContent", with: createCategoryContentMap
}

/**
 * Update SEO Content For Product Category
 */
def updateContentSEOForCategory() {
    updateContent("title", "PAGE_TITLE")
    updateContent("metaKeyword", "META_KEYWORD")
    updateContent("metaDiscription", "META_DESCRIPTION")
}

/**
 * This method updates the content for the parameters given
 * @param param
 * @param typeId
 */
def updateContent(param, typeId) {
    if (parameters."${param}") {
        List productCategoryContents = from("ProductCategoryContentAndInfo")
            .where("productCategoryId", parameters.productCategoryId, "prodCatContentTypeId", typeId)
            .queryList()
        if (productCategoryContents) {
            Map productCategoryContent = EntityUtil.getFirst(productCategoryContents)
            Map electronicText = from("ElectronicText").where("dataResourceId", productCategoryContent).queryOne()
            if (electronicText) {
                electronicText.textData = parameters."${param}"
                electronicText.store()
            }
        } else {
            Map createTextContentMap = [
                productCategoryId: parameters.productCategoryId,
                prodCatContentTypeId: typeId,
                text: parameters."${param}"
            ]
            run service: "createSimpleTextContentForCategory", with: createTextContentMap
        }
    }
}

/**
 * Create Related URL Content For Product Category
 */
def createRelatedUrlContentForCategory() {
    String url = parameters.url
    url = url.trim()
    if (url.indexOf("&quot;http://&quot;") != 0) {
        url = "&quot;http://&quot;" + url
    }
    Map dataResource = [
        dataRescourceName: parameters.title,
        dataRescourceTypeId: "URL_RESOURCE",
        mimeTypeId: "text/plain",
        objectInfo: url,
        localeString: parameters.localeString
    ]
    Map rescRes = run service: "createDataResource", with: dataResource
    parameters.dataResourceId = rescRes.dataResourceId
    Map content = [
        contentTypeId:"DOCUMENT",
        dataResourceId: parameters.dataResourceId,
        contentName: parameters.title,
        description: parameters.description,
        localeString: parameters.localeString,
        createdByUserLogin: parameters.userLogin.userLoginId
    ]
    Map contRes = run service: "createContent", with: content
    parameters.contentId = contRes.contentId
    Map createCategoryContentMap = dispatcher.getDispatchContext().makeValidContext("createCategoryContent", ModelService.IN_PARAM, parameters)
    run service: "createContentCategory", with: createCategoryContentMap
}

/**
 * Update Related URL Content For Product Category
 */
def updateRelatedUrlContentForCategory() {
    Map updateCategoryContent = dispatcher.getDispatchContext().makeValidContext("updateCategoryContent", ModelService.IN_PARAM, parameters)
    run service: "updateCategoryContent", with: updateCategoryContent
    Map dataResource = [
        dataResourceId: parameters.dataResourceId,
        dataResourceName: parameters.title,
        objectInfo: parameters.url,
        localeString: parameters.localeString
    ]
    run service: "updateDataResource", with: dataResource
    Map updateContent = [
        contentId: parameters.contentId,
        contentName: parameters.title,
        description: parameters.description,
        localeString: parameters.localeString
    ]
    run service: "updateContent", with: updateContent
}

/**
 * Create Download Content For Category
 */
def createDownloadContentForCategory() {
    Map createCategoryContent = dispatcher.getDispatchContext().makeValidContext("createCategoryContent", ModelService.IN_PARAM, parameters)
    // create data resource
    Map data = [
        dataResourceTypeId: parameters.dataResourceTypeId,
        dataResourceName: parameters._uploadedFile_fileName,
        mimeTypeId: parameters._uploadedFile_contentType,
        uploadedFile: parameters.uploadedFile
    ]
    Map creDatRes = run service: "createDataResource", with: data
    parameters.dataResourceId = creDatRes.dataResourceId
    // create attach upload to data resource
    Map attachMap = dispatcher.getDispatchContext().makeValidContext("attachUploadToDataResource", ModelService.IN_PARAM, parameters)
    attachMap = [
        uploadedFile: parameters.uploadedFile,
        _uploadedFile_fileName: parameters._uploadedFile_fileName,
        _uploadedFile_contentType: parameters._uploadedFile_contentType
    ]
    run service: "attachUploadToDataResource", with: attachMap
    // create content from dataResource
    Map contentMap = dispatcher.getDispatchContext().makeValidContext("createContentFromDataResource", ModelService.IN_PARAM, parameters)
    contentMap.contentTypeId = "DOCUMENT"
    Map creConRes = run service: "createContentFromDataResource", with: contentMap
    createCategoryContent.contentId = creConRes.contentId

    createCategoryContent.contentId = parameters.contentId
    run service: "createCategoryContent", with: createCategoryContent
}

/**
 * Update Download Content For Category
 */
def updateDownloadContentForCategory() {
    Map attachMap = [
        uploadedFile: parameters.uploadedFile,
        _uploadedFile_fileName: parameters._uploadedFile_fileName,
        _uploadedFile_contentType: parameters._uploadedFile_contentType,
        mimeTypeId: parameters._uploadedFile_contentType,
        dataResourceId: parameters.fileDataResourceId
    ]
    run service: "attachUploadToDataResource", with: attachMap

    Map updateCategoryContent = dispatcher.getDispatchContext().makeValidContext("updateCategoryContent", ModelService.IN_PARAM, parameters)
    run sevrice: "updateCategoryContent", with: updateCategoryContent
}