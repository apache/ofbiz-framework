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
package org.ofbiz.content.layout;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.content.ContentManagementWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.ByteWrapper;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.SimpleMapProcessor;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;

/**
 * LayoutEvents Class
 */
public class LayoutEvents {

    public static final String module = LayoutEvents.class.getName();
    public static final String err_resource = "ContentErrorUiLabel";

    public static String createLayoutImage(HttpServletRequest request, HttpServletResponse response) {
        Locale locale = UtilHttp.getLocale(request);

        try {
            GenericDelegator delegator = (GenericDelegator)request.getAttribute("delegator");
            LocalDispatcher dispatcher = (LocalDispatcher)request.getAttribute("dispatcher");
            HttpSession session = request.getSession();
            Map uploadResults = LayoutWorker.uploadImageAndParameters(request, "imageData");
            //Debug.logVerbose("in createLayoutImage(java), uploadResults:" + uploadResults, "");
            Map formInput = (Map)uploadResults.get("formInput");
            Map context = new HashMap();
            ByteWrapper byteWrap = (ByteWrapper)uploadResults.get("imageData");
            if (byteWrap == null) {
                String errMsg = UtilProperties.getMessage(LayoutEvents.err_resource, "layoutEvents.image_data_null", locale);                                                      
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
        //Debug.logVerbose("in createLayoutImage, byteWrap(0):" + byteWrap, module);
            String imageFileName = (String)uploadResults.get("imageFileName");
            //Debug.logVerbose("in createLayoutImage(java), context:" + context, "");
            String imageFileNameExt = null;
            if (UtilValidate.isNotEmpty(imageFileName)) {
                int pos = imageFileName.lastIndexOf(".");
                if (pos >= 0) 
                    imageFileNameExt = imageFileName.substring(pos + 1);
            }
            String mimeTypeId = "image/" + imageFileNameExt;
            List errorMessages = new ArrayList();
            if (locale == null)
                locale = Locale.getDefault();
            context.put("locale", locale);

            try {
                SimpleMapProcessor.runSimpleMapProcessor(
                      "org/ofbiz/content/ContentManagementMapProcessors.xml", "contentIn",
                      formInput, context, errorMessages, locale);
                SimpleMapProcessor.runSimpleMapProcessor(
                      "org/ofbiz/content/ContentManagementMapProcessors.xml", "dataResourceIn",
                      formInput, context, errorMessages, locale);
                SimpleMapProcessor.runSimpleMapProcessor(
                      "org/ofbiz/content/ContentManagementMapProcessors.xml", "contentAssocIn",
                      formInput, context, errorMessages, locale);
            } catch(MiniLangException e) {
                request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                return "error";
            }

            context.put("dataResourceName", context.get("contentName"));
            context.put("userLogin", session.getAttribute("userLogin"));
            context.put("dataResourceTypeId", "IMAGE_OBJECT");
            context.put("contentAssocTypeId", "SUB_CONTENT");
            context.put("contentTypeId", "DOCUMENT");
            context.put("contentIdTo", formInput.get("contentIdTo"));
            context.put("textData", formInput.get("textData"));
            String contentPurposeTypeId = (String)formInput.get("contentPurposeTypeId");
            if (UtilValidate.isNotEmpty(contentPurposeTypeId)){
                context.put("contentPurposeList", UtilMisc.toList(contentPurposeTypeId));
            }
    
            Map result = dispatcher.runSync("persistContentAndAssoc", context);
        //Debug.logVerbose("in createLayoutImage, result:" + result, module);
    
            String dataResourceId = (String)result.get("dataResourceId");
            String activeContentId = (String)result.get("contentId");
            if (UtilValidate.isNotEmpty(activeContentId)) {
                Map context2 = new HashMap();
                context2.put("activeContentId", activeContentId);
                //context2.put("dataResourceId", dataResourceId);
                context2.put("contentAssocTypeId", result.get("contentAssocTypeId"));
                context2.put("fromDate", result.get("fromDate"));
        
                request.setAttribute("contentId", result.get("contentId"));
                request.setAttribute("drDataResourceId", dataResourceId);
                request.setAttribute("currentEntityName", "SubContentDataResourceId");
        
                context2.put("contentIdTo", formInput.get("contentIdTo"));
                context2.put("mapKey", formInput.get("mapKey"));
        
            //Debug.logVerbose("in createLayoutImage, context2:" + context2, module);
                Map result2 = dispatcher.runSync("deactivateAssocs", context2);
            }

            GenericValue dataResource = delegator.findByPrimaryKey("DataResource",
                          UtilMisc.toMap("dataResourceId", dataResourceId));
        //Debug.logVerbose("in createLayoutImage, dataResource:" + dataResource, module);
            // Use objectInfo field to store the name of the file, since there is no
            // place in ImageDataResource for it.
            if (dataResource != null) {
                dataResource.set("objectInfo", imageFileName);
                dataResource.set("mimeTypeId", mimeTypeId);
                dataResource.store();
            }

            // See if this needs to be a create or an update procedure
            GenericValue imageDataResource = delegator.findByPrimaryKey("ImageDataResource",
                          UtilMisc.toMap("dataResourceId", dataResourceId));
        //Debug.logVerbose("in createLayoutImage, imageDataResource(0):" + imageDataResource, module);
            if (imageDataResource == null) {
                imageDataResource = delegator.makeValue("ImageDataResource",
                          UtilMisc.toMap("dataResourceId", dataResourceId));
                imageDataResource.set("imageData", byteWrap.getBytes());
                imageDataResource.create();
            } else {
                imageDataResource.set("imageData", byteWrap.getBytes());
                imageDataResource.store();
            }
        } catch (GenericEntityException e3) {
            request.setAttribute("_ERROR_MESSAGE_", e3.getMessage());
            return "error";
        } catch( GenericServiceException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }
        return "success";
    }

    public static String updateLayoutImage(HttpServletRequest request, HttpServletResponse response) {
        Locale locale = UtilHttp.getLocale(request);
        try {
            GenericDelegator delegator = (GenericDelegator)request.getAttribute("delegator");
            HttpSession session = request.getSession();
            Map uploadResults = LayoutWorker.uploadImageAndParameters(request, "imageData");
            Map context = (Map)uploadResults.get("formInput");
            ByteWrapper byteWrap = (ByteWrapper)uploadResults.get("imageData");
            if (byteWrap == null) {
                String errMsg = UtilProperties.getMessage(LayoutEvents.err_resource, "layoutEvents.image_data_null", locale);
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
            String imageFileName = (String)uploadResults.get("imageFileName");
            Debug.logVerbose("in createLayoutImage(java), context:" + context, "");
            context.put("userLogin", session.getAttribute("userLogin"));
            context.put("dataResourceTypeId", "IMAGE_OBJECT");
            context.put("contentAssocTypeId", "SUB_CONTENT");
            context.put("contentTypeId", "DOCUMENT");
            context.put("mimeType", context.get("drMimeType"));
            context.put("drMimeType", null);
            context.put("objectInfo", context.get("drobjectInfo"));
            context.put("drObjectInfo", null);
            context.put("drDataResourceTypeId", null);
    
            String dataResourceId = (String)context.get("drDataResourceId");
            Debug.logVerbose("in createLayoutImage(java), dataResourceId:" + dataResourceId, "");

            GenericValue dataResource = delegator.findByPrimaryKey("DataResource",
                          UtilMisc.toMap("dataResourceId", dataResourceId));
            Debug.logVerbose("in createLayoutImage(java), dataResource:" + dataResource, "");
            // Use objectInfo field to store the name of the file, since there is no
            // place in ImageDataResource for it.
            Debug.logVerbose("in createLayoutImage(java), imageFileName:" + imageFileName, "");
            if (dataResource != null) {
                //dataResource.set("objectInfo", imageFileName);
                dataResource.setNonPKFields(context);
                dataResource.store();
            }

            // See if this needs to be a create or an update procedure
            GenericValue imageDataResource = delegator.findByPrimaryKey("ImageDataResource",
                          UtilMisc.toMap("dataResourceId", dataResourceId));
            if (imageDataResource == null) {
                imageDataResource = delegator.makeValue("ImageDataResource",
                          UtilMisc.toMap("dataResourceId", dataResourceId));
                imageDataResource.set("imageData", byteWrap.getBytes());
                imageDataResource.create();
            } else {
                imageDataResource.set("imageData", byteWrap.getBytes());
                imageDataResource.store();
            }
        } catch (GenericEntityException e3) {
            request.setAttribute("_ERROR_MESSAGE_", e3.getMessage());
            return "error";
        }
        return "success";
    }

    public static String replaceSubContent(HttpServletRequest request, HttpServletResponse response) {

        LocalDispatcher dispatcher = (LocalDispatcher)request.getAttribute("dispatcher");
        HttpSession session = request.getSession();
        Locale locale = UtilHttp.getLocale(request);
        Map context = new HashMap();
        Map paramMap = UtilHttp.getParameterMap(request);
        Debug.logVerbose("in replaceSubContent, paramMap:" + paramMap, module);
        String dataResourceId = (String)paramMap.get("dataResourceId");
        if (UtilValidate.isEmpty(dataResourceId)) {
            String errMsg = UtilProperties.getMessage(LayoutEvents.err_resource, "layoutEvents.data_ressource_id_null", locale);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        String contentIdTo = (String)paramMap.get("contentIdTo");
        if (UtilValidate.isEmpty(contentIdTo)) {
            String errMsg = UtilProperties.getMessage(LayoutEvents.err_resource, "layoutEvents.content_id_to_null", locale);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        String mapKey = (String)paramMap.get("mapKey");

        context.put("dataResourceId", dataResourceId);
        String contentId = (String)paramMap.get("contentId");
        context.put("userLogin", session.getAttribute("userLogin"));

/*
        // If contentId is missing
        if (UtilValidate.isEmpty(contentId)) {
            // Look for an existing associated Content
            try {
                List lst = delegator.findByAnd(
                                     "DataResourceContentView ", 
                                     UtilMisc.toMap("dataResourceId", dataResourceId)); 
                if (lst.size() > 0) {
                    GenericValue dataResourceContentView  = (GenericValue)lst.get(0);
                    contentId = (String)dataResourceContentView.get("coContentId");
                }
            } catch( GenericEntityException e) {
                request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                return "error";
            }
            // Else, create and associate a Content
        } 
*/
        if (UtilValidate.isNotEmpty(contentId)) {
            context.put("contentId", contentId);
            context.put("contentIdTo", contentIdTo);
            context.put("mapKey", mapKey);
            context.put("contentAssocTypeId", "SUB_CONTENT");
    
            try {
                Map result = dispatcher.runSync("persistContentAndAssoc", context);
        //Debug.logVerbose("in replaceSubContent, result:" + result, module);
                request.setAttribute("contentId", contentIdTo);
                Map context2 = new HashMap();
                context2.put("activeContentId", contentId);
                //context2.put("dataResourceId", dataResourceId);
                context2.put("contentAssocTypeId", "SUB_CONTENT");
                context2.put("fromDate", result.get("fromDate"));
        
                request.setAttribute("drDataResourceId", null);
                request.setAttribute("currentEntityName", "ContentDataResourceView");
        
                context2.put("contentIdTo", contentIdTo);
                context2.put("mapKey", mapKey);
        
                //Debug.logVerbose("in replaceSubContent, context2:" + context2, module);
                Map result2 = dispatcher.runSync("deactivateAssocs", context2);
            } catch( GenericServiceException e) {
                request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                return "error";
            }
        }

        return "success";
    }

    public static String cloneLayout(HttpServletRequest request, HttpServletResponse response) {

        GenericDelegator delegator = (GenericDelegator)request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher)request.getAttribute("dispatcher");
        HttpSession session = request.getSession();
        Locale locale = UtilHttp.getLocale(request);
        Map paramMap = UtilHttp.getParameterMap(request);
        String contentId = (String)paramMap.get("contentId");
        Debug.logVerbose("in cloneLayout, contentId:" + contentId, "");
        if (UtilValidate.isEmpty(contentId)) {
            String errMsg = UtilProperties.getMessage(LayoutEvents.err_resource, "layoutEvents.content_id_empty", locale);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);            
            return "error";
        }
        String contentIdTo = (String)paramMap.get("contentIdTo");
        Debug.logVerbose("in cloneLayout, contentIdTo:" + contentIdTo, "");
        GenericValue content = null;
        GenericValue newContent = null;
        GenericValue userLogin = (GenericValue)request.getSession().getAttribute("userLogin");
        String userLoginId = (String) userLogin.get("userLoginId");
        List entityList = null;
        String newId = null;
        String newDataResourceId = null;
        try {
            content = delegator.findByPrimaryKey("Content", 
                       UtilMisc.toMap("contentId", contentId));
        Debug.logVerbose("in cloneLayout, content:" + content, "");
            if (content == null) {
                String errMsg = UtilProperties.getMessage(LayoutEvents.err_resource, "layoutEvents.content_empty", locale);
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
            newContent = delegator.makeValue("Content", content);
            Debug.logVerbose("in cloneLayout, newContent:" + newContent, "");
            String oldName = (String)content.get("contentName");
            newId = delegator.getNextSeqId("Content");
            newContent.set("contentId", newId);
            String dataResourceId = (String)content.get("dataResourceId");
            GenericValue dataResource = delegator.findByPrimaryKey("DataResource", 
                       UtilMisc.toMap("dataResourceId", dataResourceId));
            if (dataResource != null) {
                GenericValue newDataResource = delegator.makeValue("DataResource", dataResource);
                Debug.logVerbose("in cloneLayout, newDataResource:" + newDataResource, "");
                String dataResourceName = "Copy:" + (String)dataResource.get("dataResourceName");
                newDataResource.set("dataResourceName", dataResourceName);
                newDataResourceId = delegator.getNextSeqId("DataResource");
                newDataResource.set("dataResourceId", newDataResourceId);
                newDataResource.set("createdDate", UtilDateTime.nowTimestamp());
                newDataResource.set("lastModifiedDate", UtilDateTime.nowTimestamp());
                newDataResource.set("createdByUserLogin", userLoginId);
                newDataResource.set("lastModifiedByUserLogin", userLoginId);
                newDataResource.create();
            }
            newContent.set("contentName", "Copy - " + oldName);
            newContent.set("createdDate", UtilDateTime.nowTimestamp());
            newContent.set("lastModifiedDate", UtilDateTime.nowTimestamp());
            newContent.set("createdByUserLogin", userLoginId);
            newContent.set("lastModifiedByUserLogin", userLoginId);
            newContent.create();
            Debug.logVerbose("in cloneLayout, newContent:" + newContent, "");

            GenericValue newContentAssoc = delegator.makeValue("ContentAssoc", null);
            newContentAssoc.set("contentId", newId);
            newContentAssoc.set("contentIdTo", "TEMPLATE_MASTER");
            newContentAssoc.set("contentAssocTypeId", "SUB_CONTENT");
            newContentAssoc.set("fromDate", UtilDateTime.nowTimestamp());
            newContentAssoc.create();
            Debug.logVerbose("in cloneLayout, newContentAssoc:" + newContentAssoc, "");
        } catch(GenericEntityException e) {
                request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                return "error";
        }
        Map serviceIn = new HashMap();
        Map results = null;
        serviceIn.put("fromDate", UtilDateTime.nowTimestamp());
        serviceIn.put("contentId", contentId);
        serviceIn.put("userLogin", session.getAttribute("userLogin"));
        serviceIn.put("direction", "From");
        serviceIn.put("thruDate", null);
        serviceIn.put("assocTypes", UtilMisc.toList("SUB_CONTENT"));
        try {
            results = dispatcher.runSync("getAssocAndContentAndDataResource", serviceIn);
            entityList = (List)results.get("entityList");
            if (entityList == null || entityList.size() == 0) {
                String errMsg = UtilProperties.getMessage(LayoutEvents.err_resource, "layoutEvents.no_subcontent", locale);
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
            }
        } catch(GenericServiceException e) {
                request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                return "error";
        }
        
        serviceIn = new HashMap();
        serviceIn.put("userLogin", session.getAttribute("userLogin"));

        // Can't count on records being unique
        Map beenThere = new HashMap();
        for (int i=0; i<entityList.size(); i++) {
            GenericValue view = (GenericValue)entityList.get(i);
            List errorMessages = new ArrayList();
            if (locale == null)
                locale = Locale.getDefault();
            try {
                SimpleMapProcessor.runSimpleMapProcessor(
                      "org/ofbiz/content/ContentManagementMapProcessors.xml", "contentAssocIn",
                      view, serviceIn, errorMessages, locale);
            } catch(IllegalArgumentException e) {
                request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                return "error";
            } catch(MiniLangException e) {
                request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                return "error";
            }
            String contentIdFrom = (String)view.get("contentId");
            String mapKey = (String)view.get("caMapKey");
            Timestamp fromDate = (Timestamp)view.get("caFromDate");
            Timestamp thruDate = (Timestamp)view.get("caThruDate");
            Debug.logVerbose("in cloneLayout, contentIdFrom:" + contentIdFrom 
                      + " fromDate:" + fromDate
                      + " thruDate:" + thruDate
                      + " mapKey:" + mapKey
                      , "");
            if (beenThere.get(contentIdFrom) == null) {
                serviceIn.put("contentIdFrom", contentIdFrom);
                serviceIn.put("contentIdTo", newId);
                serviceIn.put("fromDate", UtilDateTime.nowTimestamp());
                serviceIn.put("thruDate", null);
                try {
                    results = dispatcher.runSync("persistContentAndAssoc", serviceIn);
                } catch(GenericServiceException e) {
                    request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                    return "error";
                }
                beenThere.put(contentIdFrom, view);
            }
         
        }

        GenericValue view = delegator.makeValue("ContentDataResourceView", null);
        view.set("contentId", newId);
        view.set("drDataResourceId", newDataResourceId);
        Debug.logVerbose("in cloneLayout, view:" + view, "");
        ContentManagementWorker.setCurrentEntityMap(request, view); 
        request.setAttribute("contentId", view.get("contentId"));
        request.setAttribute("drDataResourceId", view.get("drDataResourceId"));
        return "success";
    }

    public static String createLayoutSubContent(HttpServletRequest request, HttpServletResponse response) {

       
        try {
            LocalDispatcher dispatcher = (LocalDispatcher)request.getAttribute("dispatcher");
            HttpSession session = request.getSession();
            Map paramMap = UtilHttp.getParameterMap(request);
            String contentIdTo = (String)paramMap.get("contentIdTo");
            String mapKey = (String)paramMap.get("mapKey");
            if (Debug.verboseOn()) { 
                Debug.logVerbose("in createSubContent, contentIdTo:" + contentIdTo, module);
                Debug.logVerbose("in createSubContent, mapKey:" + mapKey, module);
            }
            Map context = new HashMap();
            List errorMessages = null;
            Locale loc = (Locale)request.getSession().getServletContext().getAttribute("locale");
            if (loc == null) 
                loc = Locale.getDefault();
            GenericValue userLogin = (GenericValue)session.getAttribute("userLogin");
            context.put("userLogin", userLogin);

            String rootDir = request.getSession().getServletContext().getRealPath("/"); 
            context.put("rootDir", rootDir);
            try {
                SimpleMapProcessor.runSimpleMapProcessor(
                      "org/ofbiz/content/ContentManagementMapProcessors.xml", "contentIn",
                      paramMap, context, errorMessages, loc);
                SimpleMapProcessor.runSimpleMapProcessor(
                      "org/ofbiz/content/ContentManagementMapProcessors.xml", "dataResourceIn",
                      paramMap, context, errorMessages, loc);
                SimpleMapProcessor.runSimpleMapProcessor(
                      "org/ofbiz/content/ContentManagementMapProcessors.xml", "contentAssocIn",
                      paramMap, context, errorMessages, loc);
            } catch(MiniLangException e) {
                request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                return "error";
            }

            context.put("dataResourceName", context.get("contentName"));
            String contentPurposeTypeId = (String)paramMap.get("contentPurposeTypeId");
            if (UtilValidate.isNotEmpty(contentPurposeTypeId))
                context.put("contentPurposeList", UtilMisc.toList(contentPurposeTypeId));
            context.put("contentIdTo", paramMap.get("contentIdTo"));
            context.put("mapKey", paramMap.get("mapKey"));
            context.put("textData", paramMap.get("textData"));
            context.put("contentAssocTypeId", "SUB_CONTENT");
            if (Debug.verboseOn()) Debug.logVerbose("in createSubContent, context:" + context, module);
            Map result = dispatcher.runSync("persistContentAndAssoc", context);
            boolean isError = ModelService.RESPOND_ERROR.equals(result.get(ModelService.RESPONSE_MESSAGE));
            if (isError) {
                request.setAttribute("_ERROR_MESSAGE_", result.get(ModelService.ERROR_MESSAGE));
                return "error";
            }

            if (Debug.verboseOn()) Debug.logVerbose("in createLayoutFile, result:" + result, module);
            String contentId = (String)result.get("contentId");
            String dataResourceId = (String)result.get("dataResourceId");
            request.setAttribute("contentId", contentId);
            request.setAttribute("drDataResourceId", dataResourceId);
            request.setAttribute("currentEntityName", "SubContentDataResourceId");
            Map context2 = new HashMap();
            context2.put("activeContentId", contentId);
            //context2.put("dataResourceId", dataResourceId);
            context2.put("contentAssocTypeId", "SUB_CONTENT");
            context2.put("fromDate", result.get("fromDate"));
            context2.put("contentIdTo", contentIdTo);
            context2.put("mapKey", mapKey);
    
            //Debug.logVerbose("in replaceSubContent, context2:" + context2, module);
            Map result2 = dispatcher.runSync("deactivateAssocs", context2);
        } catch( GenericServiceException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }
        return "success";
    }


    public static String updateLayoutSubContent(HttpServletRequest request, HttpServletResponse response) {

       
        try {
            LocalDispatcher dispatcher = (LocalDispatcher)request.getAttribute("dispatcher");
            HttpSession session = request.getSession();
            Map paramMap = UtilHttp.getParameterMap(request);
            String contentIdTo = (String)paramMap.get("contentIdTo");
            String mapKey = (String)paramMap.get("mapKey");
            Map context = new HashMap();
            List errorMessages = null;
            Locale loc = (Locale)request.getSession().getServletContext().getAttribute("locale");
            if (loc == null) 
                loc = Locale.getDefault();
            context.put("locale", loc);
            GenericValue userLogin = (GenericValue)session.getAttribute("userLogin");
            context.put("userLogin", userLogin);

            String rootDir = request.getSession().getServletContext().getRealPath("/"); 
            context.put("rootDir", rootDir);
            try {
                SimpleMapProcessor.runSimpleMapProcessor(
                      "org/ofbiz/content/ContentManagementMapProcessors.xml", "contentIn",
                      paramMap, context, errorMessages, loc);
                SimpleMapProcessor.runSimpleMapProcessor(
                      "org/ofbiz/content/ContentManagementMapProcessors.xml", "dataResourceIn",
                      paramMap, context, errorMessages, loc);
                SimpleMapProcessor.runSimpleMapProcessor(
                      "org/ofbiz/content/ContentManagementMapProcessors.xml", "contentAssocIn",
                      paramMap, context, errorMessages, loc);
            } catch(MiniLangException e) {
                request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                return "error";
            }


            context.put("dataResourceName", context.get("contentName"));
            String contentPurposeTypeId = (String)paramMap.get("contentPurposeTypeId");
            if (UtilValidate.isNotEmpty(contentPurposeTypeId))
                context.put("contentPurposeList", UtilMisc.toList(contentPurposeTypeId));
            context.put("contentIdTo", paramMap.get("contentIdTo"));
            context.put("textData", paramMap.get("textData"));
            context.put("contentAssocTypeId", null);
            Map result = dispatcher.runSync("persistContentAndAssoc", context);
            boolean isError = ModelService.RESPOND_ERROR.equals(result.get(ModelService.RESPONSE_MESSAGE));
            if (isError) {
                request.setAttribute("_ERROR_MESSAGE_", result.get(ModelService.ERROR_MESSAGE));
                return "error";
            }
            String contentId = (String)result.get("contentId");
            String dataResourceId = (String)result.get("dataResourceId");
            request.setAttribute("contentId", contentId);
            request.setAttribute("drDataResourceId", dataResourceId);
            request.setAttribute("currentEntityName", "SubContentDataResourceId");
            /*
            Map context2 = new HashMap();
            context2.put("activeContentId", contentId);
            //context2.put("dataResourceId", dataResourceId);
            context2.put("contentAssocTypeId", "SUB_CONTENT");
            context2.put("fromDate", result.get("fromDate"));
            context2.put("contentIdTo", contentIdTo);
            context2.put("mapKey", mapKey);
    
            //Debug.logVerbose("in replaceSubContent, context2:" + context2, module);
            Map result2 = dispatcher.runSync("deactivateAssocs", context2);
            */
        } catch( GenericServiceException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }
        return "success";
    }

    public static String copyToClip(HttpServletRequest request, HttpServletResponse response) {
        GenericDelegator delegator = (GenericDelegator)request.getAttribute("delegator");
        Map paramMap = UtilHttp.getParameterMap(request);
        String entityName = (String)paramMap.get("entityName");
        Locale locale = UtilHttp.getLocale(request);
                
        if (UtilValidate.isEmpty(entityName) ) {
            String errMsg = UtilProperties.getMessage(LayoutEvents.err_resource, "layoutEvents.entityname_empty", locale);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        } 
        GenericValue v = delegator.makeValue(entityName, null);
        GenericPK passedPK = v.getPrimaryKey();
        Collection keyColl = passedPK.getAllKeys();
        Iterator keyIt = keyColl.iterator();
        while (keyIt.hasNext()) {
            String attrName = (String)keyIt.next();
            String attrVal = (String)request.getAttribute(attrName);
            if (attrVal == null) {
                attrVal = (String)paramMap.get(attrName);
            }
        Debug.logVerbose("in copyToClip, attrName:" + attrName,"");
        Debug.logVerbose("in copyToClip, attrVal:" + attrVal,"");
            if (UtilValidate.isNotEmpty(attrVal)) {
                passedPK.put(attrName,attrVal);
            } else {
                String errMsg = UtilProperties.getMessage(LayoutEvents.err_resource, "layoutEvents.empty", locale);
                request.setAttribute("_ERROR_MESSAGE_", attrName + " " + errMsg);
                return "error";
            }
        }
        ContentManagementWorker.mruAdd(request, passedPK);

        return "success";
    }
}
