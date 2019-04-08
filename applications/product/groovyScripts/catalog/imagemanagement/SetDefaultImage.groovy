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

import java.awt.image.BufferedImage
import java.awt.image.RenderedImage
import java.io.File
import java.util.List

import javax.imageio.ImageIO

import org.apache.ofbiz.entity.*
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.entity.util.EntityUtilProperties
import org.apache.ofbiz.base.util.*
import org.apache.ofbiz.base.util.string.*
import org.apache.ofbiz.product.image.ScaleImage

context.nowTimestampString = UtilDateTime.nowTimestamp().toString()

imageManagementPath = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog", "image.management.path", delegator), context)

String fileType = "original"
String productId = request.getParameter("productId")

productContentList = from("ProductContentAndInfo").where("productId", productId, "productContentTypeId", "DEFAULT_IMAGE").queryList()
if (productContentList) {
    dataResourceName = productContentList.get(0).drDataResourceName
}

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
if (productId) {
    product = from("Product").where("productId", productId).queryOne()
    context.productId = productId
}

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

//UPLOADING STUFF
forLock = new Object()
contentType = null
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

    defaultFileName = "temp_" + dataResourceName
    checkPathFile = imageManagementPath + "/" + productId + "/" + dataResourceName
    if (checkPathFile.equals(productContentList.get(0).drObjectInfo)) {
        BufferedImage bufImg = ImageIO.read(new File(imageManagementPath + "/" + productId + "/" + dataResourceName))
    } else {
        BufferedImage bufImg = ImageIO.read(new File(productContentList.get(0).drObjectInfo))
    }
    ImageIO.write((RenderedImage) bufImg, "jpg", new File(imageManagementPath + "/" + productId + "/" + defaultFileName))

    clientFileName = dataResourceName
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
            file = new File(imageManagementPath + "/" + productId + "/" + defaultFileName)
            file1 = new File(imageServerPath + "/" + filePathPrefix, filenameToUse)
            try {
                // Delete existing image files
                File targetDir = new File(imageServerPath + "/" + filePathPrefix)
                // Images are ordered by productId (${location}/${id}/${viewtype}/${sizetype})
                if (!filenameToUse.startsWith(productId + ".")) {
                    File[] files = targetDir.listFiles()
                    for(File file : files) {
                        if (file.isFile() && !file.getName().equals(defaultFileName)) file.delete()
                    } 
                // Images aren't ordered by productId (${location}/${viewtype}/${sizetype}/${id}) !!! BE CAREFUL !!!
                } else {
                    File[] files = targetDir.listFiles()
                    for(File file : files) {
                        if (file.isFile() && !file.getName().equals(defaultFileName) && file.getName().startsWith(productId + ".")) file.delete()
                    }
                }
            } catch (Exception e) {
                System.out.println("error deleting existing file (not neccessarily a problem)")
            }
            file.renameTo(file1)
        } catch (Exception e) {
            e.printStackTrace()
        }

        if (imageUrl && imageUrl.length() > 0) {
            context.imageUrl = imageUrl
            product.set(fileType + "ImageUrl", imageUrl)

            // call scaleImageInAllSize
            if (fileType.equals("original")) {
                context.delegator = delegator
                result = ScaleImage.scaleImageInAllSize(context, filenameToUse, "main", "0")

                if (result.containsKey("responseMessage") && result.get("responseMessage").equals("success")) {
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
