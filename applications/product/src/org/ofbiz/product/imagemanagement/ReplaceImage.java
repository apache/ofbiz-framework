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

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

public class ReplaceImage{

    public static final String module = ReplaceImage.class.getName();
    public static final String resource = "ProductErrorUiLabels";

    public static Map<String, Object> replaceImageToExistImage(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String imageServerPath = FlexibleStringExpander.expandString(UtilProperties.getPropertyValue("catalog", "image.management.path"), context);
        String productId = (String) context.get("productId");
        String contentIdExist = (String) context.get("contentIdExist");
        String contentIdReplace = (String) context.get("contentIdReplace");
        String dataResourceNameExist = (String) context.get("dataResourceNameExist");
        String dataResourceNameReplace = (String) context.get("dataResourceNameReplace");
        
        if (UtilValidate.isNotEmpty(dataResourceNameExist)) {
            if (UtilValidate.isNotEmpty(contentIdReplace)) {
                if (contentIdExist.equals(contentIdReplace)) {
                    String errMsg = "Cannot replace because both images are the same image.";
                    Debug.logError(errMsg, module);
                    return ServiceUtil.returnError(errMsg);
                }
            }
            else{
                String errMsg = "Please choose image to replace.";
                Debug.logError(errMsg, module);
                return ServiceUtil.returnError(errMsg);
            }
        }
        else{
            String errMsg = "Please choose replacement image.";
            Debug.logError(errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        
        try {
            BufferedImage bufImg = ImageIO.read(new File(imageServerPath + "/" + productId + "/" + dataResourceNameReplace));
            ImageIO.write(bufImg, "jpg", new File(imageServerPath + "/" + productId + "/" + dataResourceNameExist));
            
            List<GenericValue> contentAssocReplaceList = delegator.findByAnd("ContentAssoc", UtilMisc.toMap("contentId", contentIdReplace, "contentAssocTypeId", "IMAGE_THUMBNAIL"), null, false);
            if (contentAssocReplaceList.size() > 0) {
                for (int i = 0; i < contentAssocReplaceList.size(); i++) {
                    GenericValue contentAssocReplace = contentAssocReplaceList.get(i);
                    
                    List<GenericValue> dataResourceAssocReplaceList = delegator.findByAnd("ContentDataResourceView", UtilMisc.toMap("contentId", contentAssocReplace.get("contentIdTo")), null, false);
                    GenericValue dataResourceAssocReplace = EntityUtil.getFirst(dataResourceAssocReplaceList);
                    
                    List<GenericValue> contentAssocExistList = delegator.findByAnd("ContentAssoc", UtilMisc.toMap("contentId", contentIdExist, "contentAssocTypeId", "IMAGE_THUMBNAIL", "mapKey", contentAssocReplace.get("mapKey")), null, false);
                    GenericValue contentAssocExist = EntityUtil.getFirst(contentAssocExistList);
                    
                    List<GenericValue> dataResourceAssocExistList = delegator.findByAnd("ContentDataResourceView", UtilMisc.toMap("contentId", contentAssocExist.get("contentIdTo")), null, false);
                    GenericValue dataResourceAssocExist = EntityUtil.getFirst(dataResourceAssocExistList);
                    
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
            
            List<GenericValue> productContentList = delegator.findByAnd("ProductContent", UtilMisc.toMap("productId", productId, "contentId", contentIdReplace, "productContentTypeId", "IMAGE"), null, false);
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
        } catch (Exception e) {
            String errMsg = "Cannot replace image.";
            Debug.logError(errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        String successMsg = "Replace image successfully.";
        return ServiceUtil.returnSuccess(successMsg);
    }

}
