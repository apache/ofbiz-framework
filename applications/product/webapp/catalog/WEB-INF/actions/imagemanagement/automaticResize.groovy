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

import org.ofbiz.entity.*;
import org.ofbiz.base.util.*;
import org.ofbiz.base.util.string.*;
import org.ofbiz.product.image.ScaleImage;


context.nowTimestampString = UtilDateTime.nowTimestamp().toString();

// make the image file formats
imageFilenameFormat = UtilProperties.getPropertyValue('catalog', 'image.filename.format');
imageServerPath = FlexibleStringExpander.expandString(UtilProperties.getPropertyValue("catalog", "image.management.path"), context);
imageUrlPrefix = UtilProperties.getPropertyValue('catalog', 'image.url.prefix');
context.imageFilenameFormat = imageFilenameFormat;
context.imageServerPath = imageServerPath;
context.imageUrlPrefix = imageUrlPrefix;

filenameExpander = FlexibleStringExpander.getInstance(imageFilenameFormat);
context.imageNameSmall  = imageUrlPrefix + "/" + filenameExpander.expandString([location : 'products', type : 'small' , id : productId]);
context.imageNameMedium = imageUrlPrefix + "/" + filenameExpander.expandString([location : 'products', type : 'medium', id : productId]);
context.imageNameLarge  = imageUrlPrefix + "/" + filenameExpander.expandString([location : 'products', type : 'large' , id : productId]);
context.imageNameDetail = imageUrlPrefix + "/" + filenameExpander.expandString([location : 'products', type : 'detail', id : productId]);
context.imageNameOriginal = imageUrlPrefix + "/" + filenameExpander.expandString([location : 'products', type : 'original', id : productId]);

// Start ProductContent stuff
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

// UPLOADING STUFF
forLock = new Object();
String fileType = request.getParameter("upload_file_type");

if (fileType) {
    if (request.getParameter("contentId")){
        contentId = request.getParameter("contentId");
        content = delegator.findOne("Content",[contentId : contentId], false);

        if (content) {
            clientFileName = content.contentName
            context.fileType = fileType;

            fileLocation = filenameExpander.expandString([location : 'products', type : fileType, id : productId]);
            filePathPrefix = "";
            filenameToUse = fileLocation;
            if (fileLocation.lastIndexOf("/") != -1) {
                filePathPrefix = fileLocation.substring(0, fileLocation.lastIndexOf("/") + 1); // adding 1 to include the trailing slash
                filenameToUse = fileLocation.substring(fileLocation.lastIndexOf("/") + 1);
            }

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
                context.productId = productId;
                context.contentId = contentId;

                characterEncoding = request.getCharacterEncoding();
                imageUrl = imageUrlPrefix + "/" + filePathPrefix + java.net.URLEncoder.encode(filenameToUse, characterEncoding);

                try {
                    f1 = new File(imageServerPath + "/" + productId + "/" + clientFileName);
                    f2 = new File(imageServerPath + "/" + filePathPrefix, filenameToUse);
                    
                    InputStream input = new FileInputStream(f1);
                    OutputStream out = new FileOutputStream(f2);
                    
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = input.read(buf)) > 0){
                      out.write(buf, 0, len);
                    }
                    input.close();
                    out.close();
                    
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
                    } else {
                        result = ScaleImage.scaleImageManageInAllSize(context, filenameToUse, "main", "0", fileType);
                    }

                    product.store();
                }
            }
        } else { 
            context.errorMessage = "ERROR, No existing Content found.";
        }
    } else { 
        context.errorMessage = "ERROR, Please choose image.";
    }
}
