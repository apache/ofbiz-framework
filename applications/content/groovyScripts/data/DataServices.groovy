import java.sql.Timestamp

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.content.data.DataResourceWorker
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.service.ModelService
import org.apache.ofbiz.service.ServiceUtil

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


//Methods for DataResource
/**
 * Create a Data Resource
 *
 * @return
 */

def createDataResource() {
    Map result = success()

    GenericValue newEntity = makeValue("DataResource", parameters)

    if (!newEntity.dataResourceId) {
        newEntity.dataResourceId = delegator.getNextSeqId("DataResource")
    }

    Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
    GenericValue userLogin = parameters.userLogin
    newEntity.lastModifiedByUserLogin = userLogin.userLoginId
    newEntity.createdByUserLogin = userLogin.userLoginId
    newEntity.lastModifiedDate = nowTimestamp
    newEntity.createdDate = nowTimestamp

    if (!parameters.dataTemplateTypeId) {
        newEntity.dataTemplateTypeId = "NONE"
    }

    if (!parameters.statusId) {
        //get first status item
        GenericValue statusItem = from("StatusItem")
            .where("statusTypeId", "CONTENT_STATUS")
            .orderBy("sequenceId")
            .queryFirst()
        newEntity.statusId = statusItem.statusId
    }

    if (!newEntity.mimeTypeId && parameters.uploadedFile) {
        newEntity.mimeTypeId = DataResourceWorker.getMimeTypeWithByteBuffer(parameters.uploadedFile)
    }

    newEntity.create()
    result.dataResourceId = newEntity.dataResourceId
    result.dataResource = newEntity

    return result
}

/**
 * Create a Data Resource and return the data resource type
 *
 * @return
 */
def createDataResourceAndAssocToContent() {

    GenericValue content = from("Content").where(parameters).queryOne()
    if (!content) {
        return error(UtilProperties.getMessage("ContentErrorUiLabels", "layoutEvents.content_empty", parameters.locale))
    }

    Map serviceResult = run service: "createDataResource", with: parameters
    if (!ServiceUtil.isSuccess(serviceResult)) {
        return serviceResult
    }
    GenericValue dataResource = serviceResult.dataResource

    Map contentCtx = [:]
    if (parameters.templateDataResource && parameters.templateDataResource == "Y") {
        contentCtx.put("templateDataResourceId", parameters.dataResourceId)
    } else {
        contentCtx.put("dataRessourceId", parameters.dataResourceId)
    }
    contentCtx.put("contentId", parameters.contentId)

    Map result = run service: "updateContent", with: contentCtx
    if (!ServiceUtil.isSuccess(result)) {
        return result
    }

    result.contentId = parameters.contentId
    if (dataResource.dataResourceTypeId &&
        (dataResource.dataResourceTypeId == "ELECTRONIC_TEXT" ||
            dataResource.dataResourceTypeId == "IMAGE_OBJECT")) {
        result.put(ModelService.RESPONSE_MESSAGE, "${dataResource.dataResourceTypeId}")
    }
    return result
}

/**
 * Get Electronic Text
 * @return
 */
def getElectronicText() {
    Map result = success()
    GenericValue userLogin = parameters.userLogin
    GenericValue currentContent = parameters.content
    logInfo("GETELECTRONICTEXT, currentContent:${currentContent}")

    if (!currentContent) {
        if (parameters.contentId) {
            currentContent = from("Content").where(parameters).queryOne()
        }
        if (!currentContent) {
            return error(UtilProperties.getMessage("ContentUiLabels", "ContentNeitherContentSupplied", parameters.locale))
        }
    }
    if (!currentContent.dataResourceId) {
        return error(UtilProperties.getMessage("ContentUiLabels", "ContentDataResourceNotFound", parameters.locale))
    }
    result.dataResourceId = currentContent.dataResourceId
    GenericValue eText = from("ElectronicText").where("dataResourceId", currentContent.dataResourceId).queryOne()
    if (!eText) {
        return error(UtilProperties.getMessage("ContentUiLabels", "ContentElectronicTextNotFound", parameters.locale))
    }
    result.textData = eText.textData
    return result
}

/**
 * Attach an uploaded file to a data resource
 *
 * @return
 */
def attachUploadToDataResource() {
    boolean isUpdate = false
    boolean forceLocal = UtilProperties.getPropertyValue("content.properties", "content.upload.always.local.file")
    List validLocalFileTypes = [
        "LOCAL_FILE",
        "OFBIZ_FILE",
        "CONTEXT_FILE",
        "LOCAL_FILE_BIN",
        "OFBIZ_FILE_BIN",
        "CONTEXT_FILE_BIN"
    ]
    boolean isValidLocalType = parameters.dataResourceTypeId in validLocalFileTypes
    if (forceLocal && !isValidLocalType) {
        parameters.dataResourceTypeId = "LOCAL_FILE"
    }

    if (!parameters.dataResourceTypeId) {
        // create default behaviour
        if (parameters._uploadedFile_contentType) {
            switch (parameters._uploadedFile_contentType) {
                case ~/image.*/:
                    parameters.dataResourceTypeId = "IMAGE_OBJECT"
                    break
                case ~/video.*/:
                    parameters.dataResourceTypeId = "VIDEO_OBJECT"
                    break
                case ~/audio.*/:
                    parameters.dataResourceTypeId = "AUDIO_OBJECT"
                    break
                default:
                    parameters.dataResourceTypeId = "OTHER_OBJECT"
            }
        } else {
            parameters.dataResourceTypeId = "OTHER_OBJECT"
        }
    }
    switch (parameters.dataResourceTypeId) {
        case ["LOCAL_FILE", "LOCAL_FILE_BIN", "OFBIZ_FILE", "OFBIZ_FILE_BIN", "CONTEXT_FILE", "CONTEXT_FILE_BIN"]:
            return saveLocalFileDataResource(parameters.dataResourceTypeId)
        case "IMAGE_OBJECT":
            GenericValue dataResObj = from("ImageDataResource")
                .where("dataResourceId", parameters.dataResourceId)
                .queryOne()
            if (dataResObj) {
                isUpdate = true
            }
            break
        case "VIDEO_OBJECT":
            GenericValue dataResObj = from("VideoDataResource")
                .where("dataResourceId", parameters.dataResourceId)
                .queryOne()
            if (dataResObj) {
                isUpdate = true
            }
            break
        case "AUDIO_OBJECT":
            GenericValue dataResObj = from("AudioDataResource")
                .where("dataResourceId", parameters.dataResourceId)
                .queryOne()
            if (dataResObj) {
                isUpdate = true
            }
            break
        case "OTHER_OBJECT":
            GenericValue dataResObj = from("OtherDataResource")
                .where("dataResourceId", parameters.dataResourceId)
                .queryOne()
            if (dataResObj) {
                isUpdate = true
            }
            break
    }

    return saveExtFileDataResource(isUpdate, parameters.dataResourceTypeId)
}

/**
 * Attach an uploaded file to a data resource as a Local File-Type (Local, OfBiz or Context)
 *
 * @param absolute
 * @return
 */
def saveLocalFileDataResource(String mode) {
    Map result = success()
    List errorList = []
    boolean isUpdate = false
    GenericValue dataResource = from("DataResource").where(parameters).queryOne()
    if (!dataResource) {
        errorList.add(UtilProperties.getMessage("ContentUiLabels", "ContentDataResourceNotFound", parameters.locale))
    } else {
        if (dataResource.objectInfo) {
            isUpdate = true
        }
    }
    if (!parameters._uploadedFile_fileName) {
        if (isUpdate) {
            // upload is found on an update; its okay, don't do anything just return
            result.dataResourceId = dataResource.dataResourceId
            result.mimeTypeId = dataResource.mimeTypeId
            return result
        } else {
            errorList.add(UtilProperties.getMessage("ContentUiLabels", "ContentNoUploadedContentFound", parameters.locale))
        }
    }
    String uploadPath = null
    switch (mode) {
        case ["LOCAL_FILE", "LOCAL_FILE_BIN"]:
            uploadPath = DataResourceWorker.getDataResourceContentUploadPath(delegator, true)
			break
        case ["OFBIZ_FILE", "OFBIZ_FILE_BIN"]:
            uploadPath = DataResourceWorker.getDataResourceContentUploadPath(delegator, false)
			break
        case ["CONTEXT_FILE", "CONTEXT_FILE_BIN"]:
            uploadPath = parameters.rootDir
			break
    }
    if (!uploadPath) {
        errorList.add(UtilProperties.getMessage("ContentErrorUiLabels", "uploadContentAndImage.noRootDirProvided", parameters.locale))
    }
    if (errorList) {
        return ServiceUtil.returnError(errorList)
    }
    logInfo("[attachLocalFileToDataResource] - Found Subdir : ${uploadPath}")
    GenericValue extension = from("FileExtension")
        .where("mimeTypeId", parameters._uploadedFile_contentType)
        .queryFirst()
    dataResource.dataResourceName = parameters._uploadedFile_fileName
    dataResource.objectInfo = extension ?
        "${uploadPath}/${dataResource.dataResourceId}.${extension.fileExtensionId}" :
        "${uploadPath}/${dataResource.dataResourceId}"
    dataResource.dataResourceTypeId = parameters.dataResourceTypeId
    dataResource.store()

    ModelService cafService = dispatcher.getDispatchContext().getModelService("createAnonFile")
    Map fileCtx = cafService.makeValid(dataResource, "IN")
    fileCtx.binData = parameters.uploadedFile
    fileCtx.dataResource = dataResource
    result = run service: "createAnonFile", with: fileCtx
    result.dataResourceId = dataResource.dataResourceId
    result.mimeTypeId = dataResource.mimeTypeId

    return result
}

def saveExtFileDataResource(boolean isUpdate, String mode) {
    Map result = success()
    List errorList = []
    GenericValue dataResource = from("DataResource")
        .where("dataResourceId", parameters.dataResourceId)
        .queryOne()
    if (!dataResource) {
        errorList.add(UtilProperties.getMessage("ContentUiLabels", "ContentDataResourceNotFound", parameters.locale))
    }
    if (!parameters._uploadedFile_fileName) {
        if (isUpdate) {
            // upload is found on an update; its okay, don't do anything just return
            result.dataResourceId = dataResource.dataResourceId
            result.mimeTypeId = dataResource.mimeTypeId
            return result
        } else {
            errorList.add(UtilProperties.getMessage("ContentUiLabels", "ContentNoUploadedContentFound", parameters.locale))
        }
    }
    // update the data resource with file data
    dataResource.dataResourceTypeId = parameters.dataResourceTypeId
    dataResource.dataResourceName = parameters._uploadedFile_fileName
    dataResource.mimeTypeId = parameters._uploadedFile_contentType
    dataResource.store()

    Map serviceContext = prepareServiceContext(dataResource, mode)

    if (isUpdate) {
        switch (mode) {
            case "IMAGE_OBJECT":
                result = run service: "updateImageDataResource", with: serviceContext
                break
            case "VIDEO_OBJECT":
                result = run service: "updateVideoDataResource", with: serviceContext
                break
            case "AUDIO_OBJECT":
                result = run service: "updateAudioDataResource", with: serviceContext
                break
            case "OTHER_OBJECT":
                result = run service: "updateOtherDataResource", with: serviceContext
                break
        }

    } else {
        switch (mode) {
            case "IMAGE_OBJECT":
                result = run service: "createImageDataResource", with: serviceContext
                break
            case "VIDEO_OBJECT":
                result = run service: "createVideoDataResource", with: serviceContext
                break
            case "AUDIO_OBJECT":
                result = run service: "createAudioDataResource", with: serviceContext
                break
            case "OTHER_OBJECT":
                result = run service: "createOtherDataResource", with: serviceContext
                break
        }
    }

    result.dataResourceId = dataResource.dataResourceId
    result.mimeTypeId = dataResource.mimeTypeId

    return result
}

Map prepareServiceContext(GenericValue dataResource, String mode) {
    switch (mode) {
        case "IMAGE_OBJECT":
            ModelService service = dispatcher.getDispatchContext().getModelService("createImageDataResource")
            Map serviceContext = service.makeValid(dataResource, "IN")
            serviceContext.imageData = parameters.uploadedFile
            serviceContext.dataResource = dataResource
            return serviceContext
        case "VIDEO_OBJECT":
            ModelService service = dispatcher.getDispatchContext().getModelService("updateVideoDataResource")
            Map serviceContext = service.makeValid(dataResource, "IN")
            serviceContext.videoData = parameters.uploadedFile
            return serviceContext
        case "AUDIO_OBJECT":
            ModelService service = dispatcher.getDispatchContext().getModelService("createAudioDataResource")
            Map serviceContext = service.makeValid(dataResource, "IN")
            serviceContext.audioData
            return serviceContext
        case "OTHER_OBJECT":
            ModelService service = dispatcher.getDispatchContext().getModelService("createOtherDataResource")
            Map serviceContext = service.makeValid(dataResource, "IN")
            serviceContext.dataResourceContent = parameters.uploadedFile
            return serviceContext
    }
    return error
}
