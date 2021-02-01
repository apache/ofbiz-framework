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

import org.apache.ofbiz.entity.*
import org.apache.ofbiz.base.util.*
import org.apache.ofbiz.base.util.string.*
import org.apache.ofbiz.entity.util.EntityUtilProperties
import org.apache.ofbiz.product.image.ScaleImage

import org.apache.commons.io.FileUtils

context.nowTimestampString = UtilDateTime.nowTimestamp().toString()

// make the image file formats
context.tenantId = delegator.getDelegatorTenantId()
imageFilenameFormat = EntityUtilProperties.getPropertyValue('catalog', 'image.filename.format', delegator)
imageServerPath = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog", "image.server.path", delegator), context)
imageUrlPrefix = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog", "image.url.prefix",delegator), context)
imageServerPath = imageServerPath.endsWith("/") ? imageServerPath.substring(0, imageServerPath.length()-1) : imageServerPath
imageUrlPrefix = imageUrlPrefix.endsWith("/") ? imageUrlPrefix.substring(0, imageUrlPrefix.length()-1) : imageUrlPrefix
context.imageFilenameFormat = imageFilenameFormat
context.imageServerPath = imageServerPath
context.imageUrlPrefix = imageUrlPrefix

filenameExpander = FlexibleStringExpander.getInstance(imageFilenameFormat)
context.imageNameSmall  = imageUrlPrefix + "/" + filenameExpander.expandString([location : 'products', id : productId, type : 'small'])
context.imageNameMedium = imageUrlPrefix + "/" + filenameExpander.expandString([location : 'products', id : productId, type : 'medium'])
context.imageNameLarge  = imageUrlPrefix + "/" + filenameExpander.expandString([location : 'products', id : productId, type : 'large'])
context.imageNameDetail = imageUrlPrefix + "/" + filenameExpander.expandString([location : 'products', id : productId, type : 'detail'])
context.imageNameOriginal = imageUrlPrefix + "/" + filenameExpander.expandString([location : 'products', id : productId, type : 'original'])

// Start ProductContent stuff
productContent = null
if (product) {
    productContent = product.getRelated('ProductContent', null, ['productContentTypeId'], false)
}
context.productContent = productContent
// End ProductContent stuff

tryEntity = true
if (request.getAttribute("_ERROR_MESSAGE_")) {
    tryEntity = false
}
if (!product) {
    tryEntity = false
}

if ("true".equalsIgnoreCase((String) request.getParameter("tryEntity"))) {
    tryEntity = true
}
context.tryEntity = tryEntity

// UPLOADING STUFF
forLock = new Object()
contentType = null
String fileType = request.getParameter("upload_file_type")
if (fileType) {

    context.fileType = fileType

    fileLocation = filenameExpander.expandString([location : 'products', id : productId, type : fileType])
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
    }

    if (clientFileName && clientFileName.length() > 0) {
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
                // Delete existing image files
                File targetDir = new File(imageServerPath + "/" + filePathPrefix)
                // Images are ordered by productId (${location}/${id}/${viewtype}/${sizetype})
                if (!filenameToUse.startsWith(productId + ".")) {
                    File[] files = targetDir.listFiles()
                    for(File file : files) {
                        if (file.isFile() && file.getName().contains(filenameToUse.substring(0, filenameToUse.indexOf(".")+1)) && !"original".equals(fileType)) {
                            file.delete()
                        } else if(file.isFile() && "original".equals(fileType) && !file.getName().equals(defaultFileName)) {
                            file.delete()
                        }
                    } 
                // Images aren't ordered by productId (${location}/${viewtype}/${sizetype}/${id}) !!! BE CAREFUL !!!
                } else {
                    File[] files = targetDir.listFiles()
                    for(File file : files) {
                        if (file.isFile() && !file.getName().equals(defaultFileName) && file.getName().startsWith(productId + ".")) {
                            file.delete()
                        }
                    }
                }
            } catch (Exception e) {
                logError(e, "error deleting existing file (not necessarily a problem, except if it's a webshell!)")
            }
            file.renameTo(file1)
        } catch (Exception e) {
            logError(e, module)
        }

        if (imageUrl && imageUrl.length() > 0) {
            context.imageUrl = imageUrl
            product.set(fileType + "ImageUrl", imageUrl)

            // call scaleImageInAllSize
            if ("original".equals(fileType)) {
                context.delegator = delegator
                result = ScaleImage.scaleImageInAllSize(context, filenameToUse, "main", "0")

                if (result.containsKey("responseMessage") && "success".equals(result.get("responseMessage"))) {
                    imgMap = result.get("imageUrlMap")
                    imgMap.each() { key, value ->
                        product.set(key + "ImageUrl", value)
                    }
                }
            }

            product.store()
        }
    }
}
