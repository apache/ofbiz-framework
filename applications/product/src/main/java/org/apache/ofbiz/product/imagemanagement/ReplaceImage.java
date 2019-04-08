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
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

public class ReplaceImage{

    public static final String module = ReplaceImage.class.getName();
    public static final String resourceError = "ProductErrorUiLabels";
    public static final String resource = "ProductUiLabels";

    public static Map<String, Object> replaceImageToExistImage(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale)context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String imageServerPath = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog", "image.management.path", delegator), context);
        String productId = (String) context.get("productId");
        String contentIdExist = (String) context.get("contentIdExist");
        String contentIdReplace = (String) context.get("contentIdReplace");
        String dataResourceNameExist = (String) context.get("dataResourceNameExist");
        String dataResourceNameReplace = (String) context.get("dataResourceNameReplace");
        
        if (UtilValidate.isNotEmpty(dataResourceNameExist)) {
            if (UtilValidate.isNotEmpty(contentIdReplace)) {
                if (contentIdExist.equals(contentIdReplace)) {
                    String errMsg = UtilProperties.getMessage(resourceError, "ProductCannotReplaceBecauseBothImagesAreTheSameImage", locale);
                    Debug.logError(errMsg, module);
                    return ServiceUtil.returnError(errMsg);
                }
            }
            else{
                 String errMsg = UtilProperties.getMessage(resourceError, "ProductPleaseChooseImageToReplace", locale);
                Debug.logError(errMsg, module);
                return ServiceUtil.returnError(errMsg);
            }
        }
        else{
            String errMsg = UtilProperties.getMessage(resourceError, "ProductPleaseChooseReplacementImage", locale);
            Debug.logError(errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        
        try {
            BufferedImage bufImg = ImageIO.read(new File(imageServerPath + "/" + productId + "/" + dataResourceNameReplace));
            ImageIO.write(bufImg, "jpg", new File(imageServerPath + "/" + productId + "/" + dataResourceNameExist));
            
            List<GenericValue> contentAssocReplaceList = EntityQuery.use(delegator).from("ContentAssoc").where("contentId", contentIdReplace, "contentAssocTypeId", "IMAGE_THUMBNAIL").queryList();
            if (contentAssocReplaceList.size() > 0) {
                for (int i = 0; i < contentAssocReplaceList.size(); i++) {
                    GenericValue contentAssocReplace = contentAssocReplaceList.get(i);
                    
                    GenericValue dataResourceAssocReplace = EntityQuery.use(delegator).from("ContentDataResourceView").where("contentId", contentAssocReplace.get("contentIdTo")).queryFirst();
                    
                    GenericValue contentAssocExist = EntityQuery.use(delegator).from("ContentAssoc").where("contentId", contentIdExist, "contentAssocTypeId", "IMAGE_THUMBNAIL", "mapKey", contentAssocReplace.get("mapKey")).queryFirst();
                    
                    GenericValue dataResourceAssocExist = EntityQuery.use(delegator).from("ContentDataResourceView").where("contentId", contentAssocExist.get("contentIdTo")).queryFirst();
                    
                    if (UtilValidate.isNotEmpty(dataResourceAssocExist)) {
                        BufferedImage bufImgAssocReplace = ImageIO.read(new File(imageServerPath + "/" + productId + "/" + dataResourceAssocReplace.get("drDataResourceName")));
                        ImageIO.write(bufImgAssocReplace, "jpg", new File(imageServerPath + "/" + productId + "/" + dataResourceAssocExist.get("drDataResourceName")));
                    }
                    else{
                        BufferedImage bufImgAssocReplace = ImageIO.read(new File(imageServerPath + "/" + productId + "/" + dataResourceAssocReplace.get("drDataResourceName")));
                        ImageIO.write(bufImgAssocReplace, "jpg", new File(imageServerPath + "/" + productId + "/" + dataResourceNameExist.substring(0, dataResourceNameExist.length() - 4) + "-" + contentAssocReplace.get("mapKey") + ".jpg"));
                    }
                }
            }
            
            GenericValue productContent = EntityQuery.use(delegator).from("ProductContent").where("productId", productId, "contentId", contentIdReplace, "productContentTypeId", "IMAGE").queryFirst();
            
            if (productContent != null) {
                Map<String, Object> productContentCtx = new HashMap<String, Object>();
                productContentCtx.put("productId", productId);
                productContentCtx.put("contentId", contentIdReplace);
                productContentCtx.put("productContentTypeId", "IMAGE");
                productContentCtx.put("fromDate", productContent.get("fromDate"));
                productContentCtx.put("userLogin", userLogin);
                dispatcher.runSync("removeProductContentAndImageFile", productContentCtx);
            }
        } catch (GenericEntityException | GenericServiceException e) {
            String errMsg = UtilProperties.getMessage(resourceError, "ProductCannotReplaceImage", locale);
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (Exception e) {
            String errMsg = UtilProperties.getMessage(resourceError, "ProductCannotReplaceImage", locale);
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        String successMsg = UtilProperties.getMessage(resource, "ProductReplaceImageSuccessfully", locale);
        return ServiceUtil.returnSuccess(successMsg);
    }

}
