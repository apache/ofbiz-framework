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
package org.apache.ofbiz.product.imagemanagement;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;


public class CropImage {

    private static final String MODULE = CropImage.class.getName();
    private static final String RES_ERROR = "ProductErrorUiLabels";
    private static final String RESOURCE = "ProductUiLabels";

    public static Map<String, Object> imageCrop(DispatchContext dctx, Map<String, ? extends Object> context)
            throws IOException {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dispatcher.getDelegator();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String nameOfThumb = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog",
                "image.management.nameofthumbnail", delegator), context);

        String productId = (String) context.get("productId");
        String imageName = (String) context.get("imageName");
        String imageX = (String) context.get("imageX");
        String imageY = (String) context.get("imageY");
        String imageW = (String) context.get("imageW");
        String imageH = (String) context.get("imageH");

        if (UtilValidate.isNotEmpty(imageName)) {
            Map<String, Object> contentCtx = new HashMap<>();
            contentCtx.put("contentTypeId", "DOCUMENT");
            contentCtx.put("userLogin", userLogin);
            Map<String, Object> contentResult;
            try {
                contentResult = dispatcher.runSync("createContent", contentCtx);
                if (ServiceUtil.isError(contentResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(contentResult));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }

            Map<String, Object> contentThumb = new HashMap<>();
            contentThumb.put("contentTypeId", "DOCUMENT");
            contentThumb.put("userLogin", userLogin);
            Map<String, Object> contentThumbResult;
            try {
                contentThumbResult = dispatcher.runSync("createContent", contentThumb);
                if (ServiceUtil.isError(contentThumbResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(contentThumbResult));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }

            String contentIdThumb = (String) contentThumbResult.get("contentId");
            String contentId = (String) contentResult.get("contentId");
            String filenameToUse = (String) contentResult.get("contentId") + ".jpg";
            String filenameTouseThumb = (String) contentResult.get("contentId") + nameOfThumb + ".jpg";

            String imageServerPath = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog",
                    "image.management.path", delegator), context);
            String imageServerUrl = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog",
                    "image.management.url", delegator), context);
            BufferedImage bufImg = ImageIO.read(new File(imageServerPath + "/" + productId + "/" + imageName));

            int x = Integer.parseInt(imageX);
            int y = Integer.parseInt(imageY);
            int w = Integer.parseInt(imageW);
            int h = Integer.parseInt(imageH);

            BufferedImage bufNewImg = bufImg.getSubimage(x, y, w, h);
            String mimeType = imageName.substring(imageName.lastIndexOf('.') + 1);
            ImageIO.write(bufNewImg, mimeType, new File(imageServerPath + "/" + productId + "/" + filenameToUse));

            double imgHeight = bufNewImg.getHeight();
            double imgWidth = bufNewImg.getWidth();

            Map<String, Object> resultResize = ImageManagementServices.resizeImageThumbnail(bufNewImg, imgHeight, imgWidth);
            ImageIO.write((RenderedImage) resultResize.get("bufferedImage"), mimeType, new File(imageServerPath
                    + "/" + productId + "/" + filenameTouseThumb));

            String imageUrlResource = imageServerUrl + "/" + productId + "/" + filenameToUse;
            String imageUrlThumb = imageServerUrl + "/" + productId + "/" + filenameTouseThumb;

            ImageManagementServices.createContentAndDataResource(dctx, userLogin, filenameToUse, imageUrlResource, contentId, "image/jpeg");
            ImageManagementServices.createContentAndDataResource(dctx, userLogin, filenameTouseThumb, imageUrlThumb, contentIdThumb, "image/jpeg");

            Map<String, Object> createContentAssocMap = new HashMap<>();
            createContentAssocMap.put("contentAssocTypeId", "IMAGE_THUMBNAIL");
            createContentAssocMap.put("contentId", contentId);
            createContentAssocMap.put("contentIdTo", contentIdThumb);
            createContentAssocMap.put("userLogin", userLogin);
            createContentAssocMap.put("mapKey", "100");
            try {
                Map<String, Object> serviceResult = dispatcher.runSync("createContentAssoc", createContentAssocMap);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }

            Map<String, Object> productContentCtx = new HashMap<>();
            productContentCtx.put("productId", productId);
            productContentCtx.put("productContentTypeId", "IMAGE");
            productContentCtx.put("fromDate", UtilDateTime.nowTimestamp());
            productContentCtx.put("userLogin", userLogin);
            productContentCtx.put("contentId", contentId);
            productContentCtx.put("statusId", "IM_PENDING");
            try {
                Map<String, Object> serviceResult = dispatcher.runSync("createProductContent", productContentCtx);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }

            Map<String, Object> contentApprovalCtx = new HashMap<>();
            contentApprovalCtx.put("contentId", contentId);
            contentApprovalCtx.put("userLogin", userLogin);
            try {
                Map<String, Object> serviceResult = dispatcher.runSync("createImageContentApproval", contentApprovalCtx);
                if (ServiceUtil.isError(serviceResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        } else {
            String errMsg = UtilProperties.getMessage(RES_ERROR, "ProductPleaseSelectImage", locale);
            Debug.logFatal(errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        }
        String successMsg = UtilProperties.getMessage(RESOURCE, "ProductCropImageSuccessfully", locale);
        Map<String, Object> result = ServiceUtil.returnSuccess(successMsg);
        return result;
    }
}
