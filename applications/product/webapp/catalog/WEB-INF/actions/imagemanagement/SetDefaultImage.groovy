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

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.util.List;

import javax.imageio.ImageIO;

import org.ofbiz.entity.*;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.base.util.*;
import org.ofbiz.base.util.string.*;
import org.ofbiz.product.image.ScaleImage;

context.nowTimestampString = UtilDateTime.nowTimestamp().toString();

imageManagementPath = FlexibleStringExpander.expandString(UtilProperties.getPropertyValue("catalog", "image.management.path"), context);

String fileType = "original";
String productId = request.getParameter("productId");

productContentList = delegator.findByAnd("ProductContentAndInfo", UtilMisc.toMap("productId", productId, "productContentTypeId", "DEFAULT_IMAGE"));
if (productContentList) {
    dataResourceName = productContentList.get(0).drDataResourceName
}

// make the image file formats
imageFilenameFormat = UtilProperties.getPropertyValue('catalog', 'image.filename.format');
imageServerPath = FlexibleStringExpander.expandString(UtilProperties.getPropertyValue("catalog", "image.server.path"), context);
imageUrlPrefix = UtilProperties.getPropertyValue('catalog', 'image.url.prefix');
context.imageFilenameFormat = imageFilenameFormat;
context.imageServerPath = imageServerPath;
context.imageUrlPrefix = imageUrlPrefix;

filenameExpander = FlexibleStringExpander.getInstance(imageFilenameFormat);
context.imageNameSmall  = imageUrlPrefix + "/" + filenameExpander.expandString([location : 'products', id : productId, type : 'small']);
context.imageNameMedium = imageUrlPrefix + "/" + filenameExpander.expandString([location : 'products', id : productId, type : 'medium']);
context.imageNameLarge  = imageUrlPrefix + "/" + filenameExpander.expandString([location : 'products', id : productId, type : 'large']);
context.imageNameDetail = imageUrlPrefix + "/" + filenameExpander.expandString([location : 'products', id : productId, type : 'detail']);
context.imageNameOriginal = imageUrlPrefix + "/" + filenameExpander.expandString([location : 'products', id : productId, type : 'original']);

// Start ProductContent stuff
if (productId) {
    product = delegator.findByPrimaryKey("Product",["productId" : productId]);
    context.productId = productId;
}

productContent = null;
if (product) {
    productContent = product.getRelated('ProductContent', null, ['productContentTypeId']);
}
context.productContent = productContent;
// End ProductContent stuff

tryEntity = true;
if (request.getAttribute("_ERROR_MESSAGE_")) {
    tryEntity = false;
}
if (!product) {
    tryEntity = false;
}

if ("true".equalsIgnoreCase((String) request.getParameter("tryEntity"))) {
    tryEntity = true;
}
context.tryEntity = tryEntity;

//UPLOADING STUFF
forLock = new Object();
contentType = null;
if (fileType) {

    context.fileType = fileType;

    fileLocation = filenameExpander.expandString([location : 'products', id : productId, type : fileType]);
    filePathPrefix = "";
    filenameToUse = fileLocation;
    if (fileLocation.lastIndexOf("/") != -1) {
        filePathPrefix = fileLocation.substring(0, fileLocation.lastIndexOf("/") + 1); // adding 1 to include the trailing slash
        filenameToUse = fileLocation.substring(fileLocation.lastIndexOf("/") + 1);
    }

    int i1;
    if (contentType && (i1 = contentType.indexOf("boundary=")) != -1) {
        contentType = contentType.substring(i1 + 9);
        contentType = "--" + contentType;
    }

    defaultFileName = "temp_" + dataResourceName;
    BufferedImage bufImg = ImageIO.read(new File(imageManagementPath + "/" + productId + "/" + dataResourceName));
    ImageIO.write((RenderedImage) bufImg, "jpg", new File(imageManagementPath + "/" + productId + "/" + defaultFileName));

    clientFileName = dataResourceName;
    if (clientFileName) {
        context.clientFileName = clientFileName;
    }

    if (clientFileName && clientFileName.length() > 0) {
        if (clientFileName.lastIndexOf(".") > 0 && clientFileName.lastIndexOf(".") < clientFileName.length()) {
            filenameToUse += clientFileName.substring(clientFileName.lastIndexOf("."));
        } else {
            filenameToUse += ".jpg";
        }

        context.clientFileName = clientFileName;
        context.filenameToUse = filenameToUse;

        characterEncoding = request.getCharacterEncoding();
        imageUrl = imageUrlPrefix + "/" + filePathPrefix + java.net.URLEncoder.encode(filenameToUse, characterEncoding);

        try {
            file = new File(imageManagementPath + "/" + productId + "/" + defaultFileName);
            file1 = new File(imageServerPath + "/" + filePathPrefix, filenameToUse);
            try {
                // Delete existing image files
                File targetDir = new File(imageServerPath + "/" + filePathPrefix);
                // Images are ordered by productId (${location}/${id}/${viewtype}/${sizetype})
                if (!filenameToUse.startsWith(productId + ".")) {
                    File[] files = targetDir.listFiles(); 
                    for(File file : files) {
                        if (file.isFile() && !file.getName().equals(defaultFileName)) file.delete();
                    } 
                // Images aren't ordered by productId (${location}/${viewtype}/${sizetype}/${id}) !!! BE CAREFUL !!!
                } else {
                    File[] files = targetDir.listFiles(); 
                    for(File file : files) {
                        if (file.isFile() && !file.getName().equals(defaultFileName) && file.getName().startsWith(productId + ".")) file.delete();
                    }
                }
            } catch (Exception e) {
                System.out.println("error deleting existing file (not neccessarily a problem)");
            }
            file.renameTo(file1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (imageUrl && imageUrl.length() > 0) {
            context.imageUrl = imageUrl;
            product.set(fileType + "ImageUrl", imageUrl);

            // call scaleImageInAllSize
            if (fileType.equals("original")) {
                result = ScaleImage.scaleImageInAllSize(context, filenameToUse, "main", "0");

                if (result.containsKey("responseMessage") && result.get("responseMessage").equals("success")) {
                    imgMap = result.get("imageUrlMap");
                    imgMap.each() { key, value ->
                        product.set(key + "ImageUrl", value);
                    }
                }
            }

            product.store();
        }
    }
}
