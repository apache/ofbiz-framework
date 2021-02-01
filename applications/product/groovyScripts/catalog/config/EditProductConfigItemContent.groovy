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

import org.apache.ofbiz.base.util.*
import org.apache.ofbiz.base.util.string.*
import org.apache.ofbiz.entity.*
import org.apache.ofbiz.entity.util.EntityUtilProperties

// make the image file formats
context.tenantId = delegator.getDelegatorTenantId()
imageFilenameFormat = "configitems/${configItemId}"
imageServerPath = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog", "image.server.path", delegator), context)
imageUrlPrefix = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog", "image.url.prefix",delegator), context)
imageServerPath = imageServerPath.endsWith("/") ? imageServerPath.substring(0, imageServerPath.length()-1) : imageServerPath
imageUrlPrefix = imageUrlPrefix.endsWith("/") ? imageUrlPrefix.substring(0, imageUrlPrefix.length()-1) : imageUrlPrefix
context.imageFilenameFormat = imageFilenameFormat
context.imageServerPath = imageServerPath
context.imageUrlPrefix = imageUrlPrefix

filenameExpander = FlexibleStringExpander.getInstance(imageFilenameFormat)
context.imageNameSmall = imageUrlPrefix + "/" + filenameExpander.expandString([size : 'small', configItemId : configItemId])

// Start ProdConfItemContent stuff
productContent = null
if (configItem) {
    productContent = configItem.getRelated("ProdConfItemContent", null, ['confItemContentTypeId'], false)
}
context.productContent = productContent

productContentDatas = []
productContent.each { productContent ->
    content = productContent.getRelatedOne("Content", false)
    productContentDatas.add([productContent : productContent, content : content])
}

context.productContentList = productContentDatas
// End ProductContent stuff

tryEntity = true
if (request.getAttribute("_ERROR_MESSAGE_")) {
    tryEntity = false
}
if (!configItem) {
    tryEntity = false
}
if ("true".equalsIgnoreCase(request.getParameter("tryEntity"))) {
    tryEntity = true
}
context.tryEntity = tryEntity

// UPLOADING STUFF

forLock = new Object()
contentType = null
fileType = request.getParameter("upload_file_type")
if (fileType) {
    context.fileType = fileType

    fileNameToUse = "productConfigItem." + configItemId
    fileLocation = filenameExpander.expandString([size : fileType, configItemId : configItemId])
    filePathPrefix = ""
    filenameToUse = fileLocation
    if (fileLocation.lastIndexOf("/") != -1) {
        filePathPrefix = fileLocation.substring(0, fileLocation.lastIndexOf("/") + 1) // adding 1 to include the trailing slash
        filenameToUse = fileLocation.substring(fileLocation.lastIndexOf("/") + 1)
    }

    int i1
    if (contentType && (i1 = contentType.indexOf("boundary=")) != -1) {
        contentType = contentType.substring(i1 + 9)
        contentType = "--" + contentType
    }

    defaultFileName = filenameToUse + "_temp"
    uploadObject = new HttpRequestFileUpload()
    uploadObject.setOverrideFilename(defaultFileName)
    uploadObject.setSavePath(imageServerPath + "/" + filePathPrefix)
    if (!uploadObject.doUpload(request, "Image")) {
        try {
            (new File(imageServerPath + "/" + filePathPrefix, defaultFileName)).delete()
        } catch (Exception e) {
            logError(e, "error deleting existing file (not necessarily a problem, except if it's a webshell!)")
        }
        String errorMessage = UtilProperties.getMessage("SecurityUiLabels","SupportedImageFormats", locale)
        logError(errorMessage)
        return error(errorMessage)
    }

    clientFileName = uploadObject.getFilename()
    if (clientFileName) {
        context.clientFileName = clientFileName
        if (clientFileName.lastIndexOf(".") > 0 && clientFileName.lastIndexOf(".") < clientFileName.length()) {
            filenameToUse += clientFileName.substring(clientFileName.lastIndexOf("."))
        } else {
            filenameToUse += ".jpg"
        }

        context.clientFileName = clientFileName
        context.filenameToUse = filenameToUse

        characterEncoding = request.getCharacterEncoding()
        imageUrl = imageUrlPrefix + "/" + filePathPrefix + java.net.URLEncoder.encode(filenameToUse, characterEncoding)

        try {
            file = new File(imageServerPath + "/" + filePathPrefix, defaultFileName)
            file1 = new File(imageServerPath + "/" + filePathPrefix, filenameToUse)
            try {
                file1.delete()
            } catch (Exception e) {
                logError(e, "error deleting existing file (not necessarily a problem, except if it's a webshell!)")
            }
            file.renameTo(file1)
        } catch (Exception e) {
            logError(e, module)
        }

        if (imageUrl) {
            context.imageUrl = imageUrl
            configItem.set("imageUrl", imageUrl)
            configItem.store()
        }
    }
}

