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
package org.ofbiz.product.imagemanagement;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;

import watermarker.exception.WatermarkerException;
import watermarker.impl.DefaultWatermarker;
import watermarker.model.WatermarkerSettings;

public class ReplaceImage{

    public static final String module = ReplaceImage.class.getName();
    public static final String resource = "ProductErrorUiLabels";

    public static String replaceImageToExistImage(HttpServletRequest request, HttpServletResponse response) throws MalformedURLException, FileNotFoundException, WatermarkerException, GenericEntityException, GenericServiceException {
        Map<String, ? extends Object> context = UtilGenerics.checkMap(request.getParameterMap());
        String imageServerPath = FlexibleStringExpander.expandString(UtilProperties.getPropertyValue("catalog", "image.server.path"), context);
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        String productId = request.getParameter("productId");
        String imageName = request.getParameter("imageName");
        String contentIdExist = request.getParameter("contentIdExist");
        String contentIdReplace = request.getParameter("contentIdReplace");
        if (UtilValidate.isNotEmpty(imageName)) {
            if (UtilValidate.isNotEmpty(contentIdReplace)) {
                if (contentIdExist.equals(contentIdReplace)) {
                    String errMsg = "Cannot replace because both images are the same image.";
                    request.setAttribute("_ERROR_MESSAGE_", errMsg);
                    return "error";
                }
            }
            else{
                String errMsg = "Please choose image to replace.";
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
        }
        else{
            String errMsg = "Please choose replacement image.";
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        
        try {
            File file = new File(imageServerPath + "/products/management/" + productId + "/" + imageName);
            file.delete();
            
            URL imageUrl = new URL("file:" + imageServerPath + "/products/management/" + productId + "/" + contentIdReplace + ".jpg");
            File outputImageFile = new File(imageServerPath + "/products/management/" + productId + "/" + imageName);
            OutputStream outputStream = new FileOutputStream(outputImageFile);
            WatermarkerSettings watermarkerSettings = WatermarkerSettings.DEFAULT;
            new DefaultWatermarker().watermark(imageUrl, " ", outputStream, watermarkerSettings);
            
            List<GenericValue> contentAssocExistList = delegator.findByAnd("ContentAssoc", UtilMisc.toMap("contentId", contentIdExist, "contentAssocTypeId", "IMAGE_THUMBNAIL"));
            GenericValue contentAssocExist = EntityUtil.getFirst(contentAssocExistList);
            
            List<GenericValue> contentAssocReplaceList = delegator.findByAnd("ContentAssoc", UtilMisc.toMap("contentId", contentIdReplace, "contentAssocTypeId", "IMAGE_THUMBNAIL"));
            GenericValue contentAssocReplace = EntityUtil.getFirst(contentAssocReplaceList);
            
            URL imageThumbnailUrl = new URL("file:" + imageServerPath + "/products/management/" + productId + "/" + contentAssocReplace.get("contentIdTo") + ".jpg");
            File outputImageThumbnailFile = new File(imageServerPath + "/products/management/" + productId + "/" + contentAssocExist.get("contentIdTo") + ".jpg");
            OutputStream outputStreamThumbnail = new FileOutputStream(outputImageThumbnailFile);
            new DefaultWatermarker().watermark(imageThumbnailUrl, " ", outputStreamThumbnail, watermarkerSettings);
            
            List<GenericValue> productContentList = delegator.findByAnd("ProductContent", UtilMisc.toMap("productId", productId, "contentId", contentIdReplace, "productContentTypeId", "IMAGE"));
            GenericValue productContent = EntityUtil.getFirst(productContentList);

            if (productContent != null) {
                Map<String, Object> productContentCtx = FastMap.newInstance();
                productContentCtx.put("productId", productId);
                productContentCtx.put("contentId", contentIdReplace);
                productContentCtx.put("productContentTypeId", "IMAGE");
                productContentCtx.put("fromDate", productContent.get("fromDate"));
                productContentCtx.put("userLogin", userLogin);
                dispatcher.runSync("removeProductContentAndImageFile", productContentCtx);
            }
        } catch (WatermarkerException e) {
            String errMsg = "Cannot replace image.";
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            Debug.logError(e, errMsg, module);
            return "error";
        }
        String eventMsg = "Replace image successfully.";
        request.setAttribute("_EVENT_MESSAGE_", eventMsg);
        return "success";
    }

}
