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

import org.ofbiz.base.util.*
import org.ofbiz.base.util.string.*

productCategoryType = null;
if (productCategory) {
    productCategoryType = productCategory.getRelatedOne("ProductCategoryType");
    context.productCategoryType = productCategoryType;
}

primaryParentCategory = null;
primParentCatIdParam = request.getParameter("primaryParentCategoryId");
if(productCategory) {
    primaryParentCategory = productCategory.getRelatedOne("PrimaryParentProductCategory");
} else if (primParentCatIdParam && primParentCatIdParam.length() > 0) {
    primaryParentCategory = delegator.findOne("ProductCategory", [productCategoryId : primParentCatIdParam], false);
}
context.primaryParentCategory = primaryParentCategory;


// make the image file formats
String imageFilenameFormat = UtilProperties.getPropertyValue("catalog", "image.filename.format");
String imageServerPath = UtilProperties.getPropertyValue("catalog", "image.server.path");
String imageUrlPrefix = UtilProperties.getPropertyValue("catalog", "image.url.prefix");
context.imageFilenameFormat = imageFilenameFormat;
context.imageServerPath = imageServerPath;
context.imageUrlPrefix = imageUrlPrefix;

FlexibleStringExpander filenameExpander = new FlexibleStringExpander(imageFilenameFormat);
context.imageNameCategory = imageUrlPrefix + "/" + filenameExpander.expandString([location : "categories", type : "category", id : productCategoryId]);
context.imageNameLinkOne  = imageUrlPrefix + "/" + filenameExpander.expandString([location : "categories", type : "linkOne", id : productCategoryId]);
context.imageNameLinkTwo  = imageUrlPrefix + "/" + filenameExpander.expandString([location : "categories", type : "linkTwo", id : productCategoryId]);


// UPLOADING STUFF

Object forLock = new Object();
String contentType = null;
String fileType = request.getParameter("upload_file_type");
if (fileType) {
    context.fileType = fileType;

    String fileLocation = filenameExpander.expandString([location : "categories", type : fileType, id : productCategoryId]);
    String filePathPrefix = "";
    String filenameToUse = fileLocation;
    if (fileLocation.lastIndexOf("/") != -1) {
        filePathPrefix = fileLocation.substring(0, fileLocation.lastIndexOf("/") + 1); // adding 1 to include the trailing slash
        filenameToUse = fileLocation.substring(fileLocation.lastIndexOf("/") + 1);
    }

    int i1;
    if (contentType && (i1 = contentType.indexOf("boundary=")) != -1) {
        contentType = contentType.substring(i1 + 9);
        contentType = "--" + contentType;
    }

    String defaultFileName = filenameToUse + "_temp";
    HttpRequestFileUpload uploadObject = new HttpRequestFileUpload();
    uploadObject.setOverrideFilename(defaultFileName);
    uploadObject.setSavePath(imageServerPath + "/" + filePathPrefix);
    uploadObject.doUpload(request);

    String clientFileName = uploadObject.getFilename();
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

        String characterEncoding = request.getCharacterEncoding();
        String imageUrl = imageUrlPrefix + "/" + filePathPrefix + java.net.URLEncoder.encode(filenameToUse, characterEncoding);

        try {
            File file = new File(imageServerPath + "/" + filePathPrefix, defaultFileName);
            File file1 = new File(imageServerPath + "/" + filePathPrefix, filenameToUse);
            try {
                file1.delete();
            } catch(Exception e) {
                System.out.println("error deleting existing file (not neccessarily a problem)");
            }
            file.renameTo(file1);
        } catch(Exception e) {
            e.printStackTrace();
        }

        if (imageUrl && imageUrl.length() > 0) {
            context.imageUrl = imageUrl;
            productCategory.set(fileType + "ImageUrl", imageUrl);
            productCategory.store();
        }
    }
}