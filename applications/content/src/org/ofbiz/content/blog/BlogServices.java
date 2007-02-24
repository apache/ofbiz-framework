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
package org.ofbiz.content.blog;

    
import java.util.HashMap;
import java.util.Map;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

public class BlogServices {

    /**
     * This service persists a blog article.
     * 
     * It can persist text only, image only or a combination of content types.
     * If text or image only, that content is attached directly to the main
     * content as an ElectronicText or ImageDataResource entity.
     * If a combination is desired, the two content pieces (image and text) are associated
     * through a predefined screen widget template (ie. drDataTemplateTypeId="SCREEN_COMBINED").
     */
    
    public static Map persistBlogAll(DispatchContext dctx, Map context) throws GenericServiceException {

        Map result = new HashMap();
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue)context.get("userLogin");
        ModelService modelPersistContent = dispatcher.getDispatchContext().getModelService("persistContentAndAssoc");
        Map ctx = modelPersistContent.makeValid(context, "IN");
        ctx.put("userLogin", userLogin);
        ctx.put("textData", context.get("mainData"));
        ctx.put("deactivateExisting", "false");
        String drMimeTypeId_TEXT = (String)context.get("drMimeTypeId_TEXT");
        boolean bTextMimeType = "Y".equals(drMimeTypeId_TEXT);
        String drMimeTypeId_IMAGE = (String)context.get("drMimeTypeId_IMAGE");
        boolean bImageMimeType = "Y".equals(drMimeTypeId_IMAGE);
        String mainData = (String)context.get("mainData");
        String imageMimeTypeId =  (String)context.get("_imageData_contentType");
        if (UtilValidate.isEmpty(imageMimeTypeId)) imageMimeTypeId = "image/jpeg";
        String contentId = (String)context.get("contentId");
        if (UtilValidate.isEmpty(contentId)) ctx.put("contentId", context.get("contentIdTo"));
        
        // if the content already exists, these will identify it.
        // They are stored in the form
        String textContentId = (String)context.get("textContentId");
        String imageContentId = (String)context.get("imageContentId");
        String textDataResourceId = (String)context.get("textDataResourceId");
        String imageDataResourceId = (String)context.get("imageDataResourceId");
        
        String drDataResourceId = (String)context.get("drDataResourceId");
        String drDataTemplateTypeId = (String)context.get("drDataTemplateTypeId");
        
        String templateId = (String)context.get("templateId");
        
        if ("SCREEN_COMBINED".equals(drDataTemplateTypeId) && !bImageMimeType) {
            if (bTextMimeType) {
                ctx.put("dataResourceId", textDataResourceId);                
                ctx.put("drDataResourceId", textDataResourceId);                
                ctx.put("drDataResourceTypeId", "ELECTRONIC_TEXT");
                ctx.put("drDataTemplateTypeId", "NONE");
                ctx.put("drMimeTypeId", "text/html");
            }
        } else if ("SCREEN_COMBINED".equals(drDataTemplateTypeId) && !bTextMimeType) {
            if (bImageMimeType) {
                ctx.put("dataResourceId", imageDataResourceId);                
                ctx.put("drDataResourceId", imageDataResourceId);                
                ctx.put("drDataResourceTypeId", "IMAGE_OBJECT");
                ctx.put("drDataTemplateTypeId", "NONE");
                ctx.put("drMimeTypeId", imageMimeTypeId);
                ctx.put("drIsPublic", "Y");
            }
        } else if (!"SCREEN_COMBINED".equals(drDataTemplateTypeId) ) {
        
            // Store the main record
            // either text or image or both
            if (bTextMimeType && !bImageMimeType) {
                ctx.put("dataResourceId", drDataResourceId);                
                ctx.put("drDataResourceId", drDataResourceId);                
                ctx.put("drDataResourceTypeId", "ELECTRONIC_TEXT");
                ctx.put("drDataTemplateTypeId", "NONE");
                ctx.put("drMimeTypeId", "text/html");
            } else if (!bTextMimeType && bImageMimeType) {
                ctx.put("dataResourceId", drDataResourceId);                
                ctx.put("drDataResourceId", drDataResourceId);                
                ctx.put("drDataResourceTypeId", "IMAGE_OBJECT");
                ctx.put("drDataTemplateTypeId", "NONE");
                ctx.put("drMimeTypeId", imageMimeTypeId);
                ctx.put("drIsPublic", "Y");
            } else if (bTextMimeType && bImageMimeType) {
                // if both then set up for and choose a template
                ctx.put("drDataResourceTypeId", null);
                ctx.put("dataResourceTypeId", null);
                ctx.put("textData", null);
                ctx.put("dataResourceId", templateId);                
                ctx.put("drDataResourceId", templateId);                
                ctx.put("drIsPublic", "Y");
            }
        } else if ("SCREEN_COMBINED".equals(drDataTemplateTypeId) ) {
            if (bTextMimeType && bImageMimeType) {
                // if both then set up for and choose a template
                ctx.put("drDataResourceTypeId", "ELECTRONIC_TEXT");
                ctx.put("drDataTemplateTypeId", "SCREEN_COMBINED");
                ctx.put("drMimeTypeId", "text/html");
                ctx.put("textData", null);
                ctx.put("dataResourceId", templateId);                
                ctx.put("drDataResourceId", templateId);                
                ctx.put("drIsPublic", "Y");
            }
        }
        // store the main content entity
        result = dispatcher.runSync("persistContentAndAssoc", ctx);
        if (ServiceUtil.isError(result)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));   
        }
        String mainContentId = (String)result.get("contentId");
        
        // If both, then that content needs to be persisted
        if (bTextMimeType && bImageMimeType) {
        
            ctx = modelPersistContent.makeValid(context, "IN");
            // first store the text
            ctx.put("caContentId", mainContentId);
            ctx.put("contentId", textContentId);
            ctx.put("dataResourceId", textDataResourceId);
            ctx.put("drDataResourceId", textDataResourceId);
            ctx.put("caContentId", mainContentId);
            ctx.put("drDataTemplateTypeId", "NONE");
            ctx.put("caContentAssocTypeId", "SUB_CONTENT");
            ctx.put("drDataResourceTypeId", "ELECTRONIC_TEXT");
            ctx.put("drMimeTypeId", "text/html");
            ctx.put("caMapKey", "MAIN");
            ctx.put("textData", mainData);
            Map textResult = dispatcher.runSync("persistContentAndAssoc", ctx);
            if (ServiceUtil.isError(textResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(textResult));   
            }
            textContentId = (String)textResult.get("contentId");
            textDataResourceId = (String)textResult.get("drDataResourceId");
            
            
            // persist the image
            String imageData_fileName = (String)context.get("_imageData_fileName");
            // In the update mode, if no upload file is given, do not upload
            if (UtilValidate.isNotEmpty(imageData_fileName)) {
                ctx = modelPersistContent.makeValid(context, "IN");
                ctx.put("caContentId", mainContentId);
                ctx.put("contentId", imageContentId);
                ctx.put("dataResourceId", imageDataResourceId);
                ctx.put("drDataResourceId", imageDataResourceId);
                ctx.put("drDataTemplateTypeId", "NONE");
                ctx.put("drDataResourceTypeId", "IMAGE_OBJECT");
                ctx.put("caContentAssocTypeId", "SUB_CONTENT");
                ctx.put("drMimeTypeId", imageMimeTypeId);
                ctx.put("caMapKey", "IMAGE");
                ctx.put("textData", null);
                ctx.put("drIsPublic", "Y");
                Map imageResult = dispatcher.runSync("persistContentAndAssoc", ctx);
                if (ServiceUtil.isError(imageResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(imageResult));   
                }
                imageContentId = (String)imageResult.get("contentId");
                imageDataResourceId = (String)imageResult.get("drDataResourceId");
            }
        }
        
        // persist the summary info. 
        String summaryData = (String)context.get("summaryData");
        String summaryParentContentId = mainContentId;
        if (UtilValidate.isNotEmpty(summaryData) && UtilValidate.isNotEmpty(summaryParentContentId) ) {
            Map subContentIn = new HashMap();
            subContentIn.put("contentId", summaryParentContentId);
            subContentIn.put("mapKey", "SUMMARY");
            Map thisResult = dispatcher.runSync("getSubContent", subContentIn);
            GenericValue view = (GenericValue)thisResult.get("view");
            Map summaryContext = null;
            if (view != null) {
                summaryContext = modelPersistContent.makeValid(view, "IN");
                summaryContext.put("textData", summaryData);
                summaryContext.put("contentTypeId", null);
                summaryContext.put("caContentAssocTypeId", null);
            } else {
                summaryContext = modelPersistContent.makeValid(result, "IN");
                summaryContext.put("textData", summaryData);
                summaryContext.put("caContentId", summaryParentContentId);
                summaryContext.put("caMapKey", "SUMMARY");
                summaryContext.put("caContentAssocTypeId", "SUB_CONTENT");
                summaryContext.put("contentTypeId", "DOCUMENT");
                summaryContext.put("dataResourceTypeId", "ELECTRONIC_TEXT");
                summaryContext.put("contentId", null);
                summaryContext.put("caFromDate", null);
                summaryContext.put("drDataResourceId", null);
                summaryContext.put("dataResourceId", null);
                summaryContext.put("statusId", context.get("statusId"));
                summaryContext.put("ownerContentId", context.get("ownerContentId"));
                summaryContext.put("contentPurposeString", "ARTICLE");
            }
            summaryContext.put("userLogin", userLogin);
            Map summaryResult = dispatcher.runSync("persistContentAndAssoc", summaryContext);
            if (ServiceUtil.isError(summaryResult)) {
                String errMsg =  ServiceUtil.getErrorMessage(summaryResult);
                return ServiceUtil.returnError(errMsg);
            }
        }
        
        return result;
    }
}
