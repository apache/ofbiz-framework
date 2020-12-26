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
package org.apache.ofbiz.content.content;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.SimpleMapProcessor;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceAuthException;
import org.apache.ofbiz.service.ServiceUtil;


/**
 * UploadContentAndImage Class
 *
 * Services for granting operation permissions on Content entities in a data-driven manner.
 */
public class UploadContentAndImage {

    private static final String MODULE = UploadContentAndImage.class.getName();
    private static final String ERR_RESOURCE = "ContentErrorUiLabels";

    public UploadContentAndImage() { }

    public static String uploadContentAndImage(HttpServletRequest request, HttpServletResponse response) {
        try {
            Locale locale = UtilHttp.getLocale(request);
            LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
            Delegator delegator = (Delegator) request.getAttribute("delegator");
            HttpSession session = request.getSession();
            GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");

            long maxUploadSize = UtilHttp.getMaxUploadSize(delegator);
            int sizeThreshold = UtilHttp.getSizeThreshold(delegator);
            File tmpUploadRepository = UtilHttp.getTmpUploadRepository(delegator);

            ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory(sizeThreshold, tmpUploadRepository));
            upload.setSizeMax(maxUploadSize);
            List<FileItem> lst = null;
            try {
                lst = UtilGenerics.cast(upload.parseRequest(request));
            } catch (FileUploadException e4) {
                request.setAttribute("_ERROR_MESSAGE_", e4.getMessage());
                Debug.logError("[UploadContentAndImage.uploadContentAndImage] " + e4.getMessage(), MODULE);
                return "error";
            }

            if (lst.isEmpty()) {
                String errMsg = UtilProperties.getMessage(ERR_RESOURCE, "uploadContentAndImage.no_files_uploaded", locale);
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                Debug.logWarning("[DataEvents.uploadImage] No files uploaded", MODULE);
                return "error";
            }

            Map<String, Object> passedParams = new HashMap<>();
            FileItem fi = null;
            FileItem imageFi = null;
            byte[] imageBytes = {};
            for (FileItem fileItem : lst) {
                fi = fileItem;
                String fieldName = fi.getFieldName();
                if (fi.isFormField()) {
                    String fieldStr = fi.getString();
                    passedParams.put(fieldName, fieldStr);
                } else if ("imageData".equals(fieldName)) {
                    imageFi = fi;
                    imageBytes = imageFi.get();
                }
            }
            if (Debug.infoOn()) {
                Debug.logInfo("[UploadContentAndImage]passedParams: " + passedParams, MODULE);
            }

            TransactionUtil.begin();
            List<String> contentPurposeList = ContentWorker.prepContentPurposeList(passedParams);
            passedParams.put("contentPurposeList", contentPurposeList);
            String entityOperation = (String) passedParams.get("entityOperation");
            String passedContentId = (String) passedParams.get("ftlContentId");
            List<String> targetOperationList = ContentWorker.prepTargetOperationList(passedParams, entityOperation);
            passedParams.put("targetOperationList", targetOperationList);

            // Create or update FTL template
            Map<String, Object> ftlContext = new HashMap<>();
            ftlContext.put("userLogin", userLogin);
            ftlContext.put("contentId", passedParams.get("ftlContentId"));
            ftlContext.put("ownerContentId", passedParams.get("ownerContentId"));
            String contentTypeId = (String) passedParams.get("contentTypeId");
            ftlContext.put("contentTypeId", contentTypeId);
            ftlContext.put("statusId", passedParams.get("statusId"));
            ftlContext.put("contentPurposeList", UtilMisc.toList(passedParams.get("contentPurposeList")));
            ftlContext.put("contentPurposeList", contentPurposeList);
            ftlContext.put("targetOperationList", targetOperationList);
            ftlContext.put("contentName", passedParams.get("contentName"));
            ftlContext.put("dataTemplateTypeId", passedParams.get("dataTemplateTypeId"));
            ftlContext.put("description", passedParams.get("description"));
            ftlContext.put("privilegeEnumId", passedParams.get("privilegeEnumId"));
            String drid = (String) passedParams.get("dataResourceId");
            ftlContext.put("dataResourceId", drid);
            ftlContext.put("dataResourceTypeId", null); // inhibits persistence of DataResource, because it already exists
            String contentIdTo = (String) passedParams.get("contentIdTo");
            ftlContext.put("contentIdTo", contentIdTo);
            String contentAssocTypeId = (String) passedParams.get("contentAssocTypeId");
            ftlContext.put("contentAssocTypeId", null); // Don't post assoc at this time
            Map<String, Object> ftlResults = dispatcher.runSync("persistContentAndAssoc", ftlContext);
            if (ServiceUtil.isError(ftlResults)) {
                String errorMessage = ServiceUtil.getErrorMessage(ftlResults);
                request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                Debug.logError(errorMessage, MODULE);
                TransactionUtil.rollback();
                return "error";
            }
            String ftlContentId = (String) ftlResults.get("contentId");
            if (UtilValidate.isNotEmpty(contentIdTo)) {
                Map<String, Object> map = new HashMap<>();
                map.put("fromDate", UtilDateTime.nowTimestamp());
                map.put("contentId", ftlContentId);
                map.put("contentIdTo", contentIdTo);
                map.put("userLogin", userLogin);
                if (UtilValidate.isEmpty(contentAssocTypeId) && UtilValidate.isEmpty(passedContentId) && UtilValidate.isNotEmpty(contentIdTo)) {
                    // switch the association order because we are really not linking to the forum
                    // but showing that this content is released to that forum.
                    map.put("contentIdTo", ftlContentId);
                    map.put("contentId", contentIdTo);
                    map.put("contentAssocTypeId", "PUBLISH_RELEASE");
                } else if ("PUBLISH_LINK".equals(contentAssocTypeId)) {
                    map.put("contentAssocTypeId", "PUBLISH_LINK");
                    String publishOperation = (String) passedParams.get("publishOperation");
                    if (UtilValidate.isEmpty(publishOperation)) {
                        publishOperation = "CONTENT_PUBLISH";
                    }
                    map.put("targetOperationList", StringUtil.split(publishOperation, "|"));
                    map.put("targetOperationString", null);
                } else {
                    map.put("contentAssocTypeId", contentAssocTypeId);
                }
                if (UtilValidate.isNotEmpty(map.get("contentAssocTypeId"))) {
                    ftlResults = dispatcher.runSync("createContentAssoc", map);
                    if (ServiceUtil.isError(ftlResults)) {
                        String errorMessage = ServiceUtil.getErrorMessage(ftlResults);
                        request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                        Debug.logError(errorMessage, MODULE);
                        TransactionUtil.rollback();
                        return "error";
                    }
                }
            }

            if (UtilValidate.isEmpty(ftlContentId)) {
                ftlContentId = passedContentId;
            }

            String ftlDataResourceId = drid;

            if (Debug.infoOn()) {
                Debug.logInfo("[UploadContentAndImage]ftlContentId:" + ftlContentId, MODULE);
            }
            // Create or update summary text subContent
            if (passedParams.containsKey("summaryData")) {
                Map<String, Object> sumContext = new HashMap<>();
                sumContext.put("userLogin", userLogin);
                sumContext.put("contentId", passedParams.get("sumContentId"));
                sumContext.put("ownerContentId", ftlContentId);
                sumContext.put("contentTypeId", "DOCUMENT");
                sumContext.put("statusId", passedParams.get("statusId"));
                sumContext.put("contentPurposeList", UtilMisc.toList("SUMMARY"));
                sumContext.put("targetOperationList", targetOperationList);
                sumContext.put("contentName", passedParams.get("contentName"));
                sumContext.put("description", passedParams.get("description"));
                sumContext.put("privilegeEnumId", passedParams.get("privilegeEnumId"));
                sumContext.put("dataResourceId", passedParams.get("sumDataResourceId"));
                sumContext.put("dataResourceTypeId", "ELECTRONIC_TEXT");
                sumContext.put("contentIdTo", ftlContentId);
                sumContext.put("contentAssocTypeId", "SUB_CONTENT");
                sumContext.put("textData", passedParams.get("summaryData"));
                sumContext.put("mapKey", "SUMMARY");
                sumContext.put("dataTemplateTypeId", "NONE");
                Map<String, Object> sumResults = dispatcher.runSync("persistContentAndAssoc", sumContext);
                if (ServiceUtil.isError(sumResults)) {
                    String errorMessage = ServiceUtil.getErrorMessage(sumResults);
                    request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                    Debug.logError(errorMessage, MODULE);
                    TransactionUtil.rollback();
                    return "error";
                }
            }

            // Create or update electronic text subContent
            if (passedParams.containsKey("textData")) {
                Map<String, Object> txtContext = new HashMap<>();
                txtContext.put("userLogin", userLogin);
                txtContext.put("contentId", passedParams.get("txtContentId"));
                txtContext.put("ownerContentId", ftlContentId);
                txtContext.put("contentTypeId", "DOCUMENT");
                txtContext.put("statusId", passedParams.get("statusId"));
                txtContext.put("contentPurposeList", UtilMisc.toList("MAIN_ARTICLE"));
                txtContext.put("targetOperationList", targetOperationList);
                txtContext.put("contentName", passedParams.get("contentName"));
                txtContext.put("description", passedParams.get("description"));
                txtContext.put("privilegeEnumId", passedParams.get("privilegeEnumId"));
                txtContext.put("dataResourceId", passedParams.get("txtDataResourceId"));
                txtContext.put("dataResourceTypeId", "ELECTRONIC_TEXT");
                txtContext.put("contentIdTo", ftlContentId);
                txtContext.put("contentAssocTypeId", "SUB_CONTENT");
                txtContext.put("textData", passedParams.get("textData"));
                txtContext.put("mapKey", "ARTICLE");
                txtContext.put("dataTemplateTypeId", "NONE");
                Map<String, Object> txtResults = dispatcher.runSync("persistContentAndAssoc", txtContext);
                if (ServiceUtil.isError(txtResults)) {
                    String errorMessage = ServiceUtil.getErrorMessage(txtResults);
                    request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                    Debug.logError(errorMessage, MODULE);
                    TransactionUtil.rollback();
                    return "error";
                }
            }

            // Create or update image subContent
            Map<String, Object> imgContext = new HashMap<>();
            if (imageBytes.length > 0) {
                imgContext.put("userLogin", userLogin);
                imgContext.put("contentId", passedParams.get("imgContentId"));
                imgContext.put("ownerContentId", ftlContentId);
                imgContext.put("contentTypeId", "DOCUMENT");
                imgContext.put("statusId", passedParams.get("statusId"));
                imgContext.put("contentName", passedParams.get("contentName"));
                imgContext.put("description", passedParams.get("description"));
                imgContext.put("contentPurposeList", contentPurposeList);
                imgContext.put("privilegeEnumId", passedParams.get("privilegeEnumId"));
                imgContext.put("targetOperationList", targetOperationList);
                imgContext.put("dataResourceId", passedParams.get("imgDataResourceId"));
                String dataResourceTypeId = "IMAGE_OBJECT";
                imgContext.put("dataResourceTypeId", dataResourceTypeId);
                imgContext.put("contentIdTo", ftlContentId);
                imgContext.put("contentAssocTypeId", "SUB_CONTENT");
                imgContext.put("imageData", imageBytes);
                imgContext.put("mapKey", "IMAGE");
                imgContext.put("dataTemplateTypeId", "NONE");
                imgContext.put("rootDir", "rootDir");
                if (Debug.infoOn()) {
                    Debug.logInfo("[UploadContentAndImage]imgContext " + imgContext, MODULE);
                }
                Map<String, Object> imgResults = dispatcher.runSync("persistContentAndAssoc", imgContext);
                if (ServiceUtil.isError(imgResults)) {
                    String errorMessage = ServiceUtil.getErrorMessage(imgResults);
                    request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                    Debug.logError(errorMessage, MODULE);
                    TransactionUtil.rollback();
                    return "error";
                }
            }

            // Check for existing AUTHOR link
            String userLoginId = userLogin.getString("userLoginId");
            GenericValue authorContent = EntityQuery.use(delegator).from("Content").where("contentId", userLoginId).cache().queryOne();
            if (authorContent != null) {
                long currentAuthorAssocCount = EntityQuery.use(delegator).from("ContentAssoc")
                        .where("contentId", ftlContentId, "contentIdTo", userLoginId, "contentAssocTypeId", "AUTHOR")
                        .filterByDate().queryCount();
                if (currentAuthorAssocCount == 0) {
                    // Don't want to bother with permission checking on this association
                    GenericValue authorAssoc = delegator.makeValue("ContentAssoc");
                    authorAssoc.set("contentId", ftlContentId);
                    authorAssoc.set("contentIdTo", userLoginId);
                    authorAssoc.set("contentAssocTypeId", "AUTHOR");
                    authorAssoc.set("fromDate", UtilDateTime.nowTimestamp());
                    authorAssoc.set("createdByUserLogin", userLoginId);
                    authorAssoc.set("lastModifiedByUserLogin", userLoginId);
                    authorAssoc.set("createdDate", UtilDateTime.nowTimestamp());
                    authorAssoc.set("lastModifiedDate", UtilDateTime.nowTimestamp());
                    authorAssoc.create();
                }
            }

            request.setAttribute("dataResourceId", ftlDataResourceId);
            request.setAttribute("drDataResourceId", ftlDataResourceId);
            request.setAttribute("contentId", ftlContentId);
            request.setAttribute("masterContentId", ftlContentId);
            request.setAttribute("contentIdTo", contentIdTo);
            String newTrail = passedParams.get("nodeTrailCsv") + "," + ftlContentId;
            request.setAttribute("nodeTrailCsv", newTrail);
            request.setAttribute("passedParams", passedParams);
            TransactionUtil.commit();
        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError(e, "[UploadContentAndImage]", MODULE);
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            try {
                TransactionUtil.rollback();
            } catch (GenericTransactionException e2) {
                request.setAttribute("_ERROR_MESSAGE_", e2.getMessage());
                return "error";
            }
            return "error";
        }
        return "success";
    }

    public static String uploadContentStuff(HttpServletRequest request, HttpServletResponse response) {
        try {
            HttpSession session = request.getSession();
            GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
            Delegator delegator = (Delegator) request.getAttribute("delegator");

            long maxUploadSize = UtilHttp.getMaxUploadSize(delegator);
            int sizeThreshold = UtilHttp.getSizeThreshold(delegator);
            File tmpUploadRepository = UtilHttp.getTmpUploadRepository(delegator);
            ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory(sizeThreshold, tmpUploadRepository));
            upload.setSizeMax(maxUploadSize);

            List<FileItem> lst = null;
            try {
                lst = UtilGenerics.cast(upload.parseRequest(request));
            } catch (FileUploadException e4) {
                request.setAttribute("_ERROR_MESSAGE_", e4.getMessage());
                Debug.logError("[UploadContentAndImage.uploadContentAndImage] " + e4.getMessage(), MODULE);
                return "error";
            }

            if (lst.isEmpty()) {
                request.setAttribute("_ERROR_MESSAGE_", "No files uploaded");
                Debug.logWarning("[DataEvents.uploadImage] No files uploaded", MODULE);
                return "error";
            }

            Map<String, Object> passedParams = new HashMap<>();
            FileItem fi = null;
            FileItem imageFi = null;
            byte[] imageBytes;
            passedParams.put("userLogin", userLogin);
            for (FileItem fileItem : lst) {
                fi = fileItem;
                String fieldName = fi.getFieldName();
                if (fi.isFormField()) {
                    String fieldStr = fi.getString();
                    passedParams.put(fieldName, fieldStr);
                } else if (fieldName.startsWith("imageData")) {
                    imageFi = fi;
                    String fileName = fi.getName();
                    passedParams.put("drObjectInfo", fileName);
                    String contentType = fi.getContentType();
                    passedParams.put("drMimeTypeId", contentType);
                    imageBytes = imageFi.get();
                    passedParams.put(fieldName, imageBytes);
                    if (Debug.infoOn()) {
                        Debug.logInfo("[UploadContentAndImage]imageData: " + imageBytes.length, MODULE);
                    }
                }
            }
            if (Debug.infoOn()) {
                Debug.logInfo("[UploadContentAndImage]passedParams: " + passedParams, MODULE);
            }

            // The number of multi form rows is retrieved
            int rowCount = UtilHttp.getMultiFormRowCount(request);
            if (rowCount < 1) {
                rowCount = 1;
            }
            TransactionUtil.begin();
            for (int i = 0; i < rowCount; i++) {
                String suffix = "_o_" + i;
                if (i == 0) {
                    suffix = "";
                }
                String returnMsg = processContentUpload(passedParams, suffix, request);
                if ("error".equals(returnMsg)) {
                    try {
                        TransactionUtil.rollback();
                    } catch (GenericTransactionException e2) {
                        ServiceUtil.setMessages(request, e2.getMessage(), null, null);
                        return "error";
                    }
                    return "error";
                }
            }
            TransactionUtil.commit();
        } catch (GenericTransactionException | GenericServiceException e) {
            Debug.logError(e, "[UploadContentAndImage]", MODULE);
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            try {
                TransactionUtil.rollback();
            } catch (GenericTransactionException e2) {
                request.setAttribute("_ERROR_MESSAGE_", e2.getMessage());
                return "error";
            }
            return "error";
        }
        return "success";
    }

    public static String processContentUpload(Map<String, Object> passedParams, String suffix, HttpServletRequest request)
            throws GenericServiceException {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        Map<String, Object> ftlContext = new HashMap<>();

        String contentPurposeString = (String) passedParams.get("contentPurposeString" + suffix);
        if (UtilValidate.isEmpty(contentPurposeString)) {
            contentPurposeString = (String) passedParams.get("contentPurposeString");
        }
        List<String> contentPurposeList = StringUtil.split(contentPurposeString, "|");
        ftlContext.put("contentPurposeList", contentPurposeList);

        String targetOperationString = (String) passedParams.get("targetOperationString" + suffix);
        if (UtilValidate.isEmpty(targetOperationString)) {
            targetOperationString = (String) passedParams.get("targetOperationString");
        }
        List<String> targetOperationList = StringUtil.split(targetOperationString, "|");
        ftlContext.put("targetOperationList", targetOperationList);

        ftlContext.put("userLogin", userLogin);
        Object objSequenceNum = passedParams.get("caSequenceNum");
        if (objSequenceNum != null) {
            if (objSequenceNum instanceof String) {
                Long sequenceNum = null;
                try {
                    sequenceNum = Long.valueOf((String) objSequenceNum);
                } catch (NumberFormatException e) {
                    String msg = "Caught an exception : " + e.toString();
                    Debug.logError(e, msg);
                    request.setAttribute("_ERROR_MESSAGE_", msg);
                    List<String> errorMsgList = UtilGenerics.cast(request.getAttribute("_EVENT_MESSAGE_LIST_"));
                    if (errorMsgList == null) {
                        errorMsgList = new LinkedList<>();
                        request.setAttribute("errorMessageList", errorMsgList);
                    }
                    errorMsgList.add(msg);
                    return "error";
                }
                passedParams.put("caSequenceNum", sequenceNum);
            }
        }

        ModelEntity modelEntity = delegator.getModelEntity("ContentAssocDataResourceViewFrom");
        List<String> fieldNames = modelEntity.getAllFieldNames();
        Map<String, Object> ftlContext2 = new HashMap<>();
        Map<String, Object> ftlContext3 = new HashMap<>();
        for (String keyName : fieldNames) {
            Object obj = passedParams.get(keyName + suffix);
            ftlContext2.put(keyName, obj);
        }
        if (Debug.infoOn()) {
            Debug.logInfo("[UploadContentStuff]ftlContext2:" + ftlContext2, MODULE);
        }
        List<Object> errorMessages = new LinkedList<>();
        Locale loc = Locale.getDefault();
        try {
            SimpleMapProcessor.runSimpleMapProcessor("component://content/minilang/ContentManagementMapProcessors.xml",
                    "contentIn", ftlContext2, ftlContext3, errorMessages, loc);
            SimpleMapProcessor.runSimpleMapProcessor("component://content/minilang/ContentManagementMapProcessors.xml",
                    "contentOut", ftlContext3, ftlContext, errorMessages, loc);

            ftlContext3 = new HashMap<>();
            SimpleMapProcessor.runSimpleMapProcessor("component://content/minilang/ContentManagementMapProcessors.xml",
                    "dataResourceIn", ftlContext2, ftlContext3, errorMessages, loc);
            SimpleMapProcessor.runSimpleMapProcessor("component://content/minilang/ContentManagementMapProcessors.xml",
                    "dataResourceOut", ftlContext3, ftlContext, errorMessages, loc);

            ftlContext3 = new HashMap<>();
            SimpleMapProcessor.runSimpleMapProcessor("component://content/minilang/ContentManagementMapProcessors.xml",
                    "contentAssocIn", ftlContext2, ftlContext3, errorMessages, loc);
            SimpleMapProcessor.runSimpleMapProcessor("component://content/minilang/ContentManagementMapProcessors.xml",
                    "contentAssocOut", ftlContext3, ftlContext, errorMessages, loc);
        } catch (MiniLangException e) {
            throw new GenericServiceException(e.getMessage());
        }

        ftlContext.put("textData", passedParams.get("textData" + suffix));
        byte[] bytes = (byte[]) passedParams.get("imageData" + suffix);
        ftlContext.put("imageData", bytes);
        if (Debug.infoOn()) {
            Debug.logInfo("[UploadContentStuff]ftlContext:" + ftlContext, MODULE);
        }
        Map<String, Object> ftlResults = null;
        try {
            ftlResults = dispatcher.runSync("persistContentAndAssoc", ftlContext);
        } catch (ServiceAuthException e) {
            String msg = e.getMessage();
            request.setAttribute("_ERROR_MESSAGE_", msg);
            List<String> errorMsgList = UtilGenerics.cast(request.getAttribute("_EVENT_MESSAGE_LIST_"));
            if (Debug.infoOn()) {
                Debug.logInfo("[UploadContentStuff]errorMsgList:" + errorMsgList, MODULE);
                Debug.logInfo("[UploadContentStuff]msg:" + msg, MODULE);
            }
            if (errorMsgList == null) {
                errorMsgList = new LinkedList<>();
                request.setAttribute("errorMessageList", errorMsgList);
            }
            errorMsgList.add(msg);
            return "error";
        }
        if (ServiceUtil.isError(ftlResults)) {
            String msg = ServiceUtil.getErrorMessage(ftlResults);
            request.setAttribute("_ERROR_MESSAGE_", msg);
            List<String> errorMsgList = UtilGenerics.cast(request.getAttribute("_EVENT_MESSAGE_LIST_"));
            if (errorMsgList == null) {
                errorMsgList = new LinkedList<>();
                request.setAttribute("errorMessageList", errorMsgList);
            }
            errorMsgList.add(msg);
            return "error";
        }
        String returnedContentId = (String) ftlResults.get("contentId");
        if (Debug.infoOn()) {
            Debug.logInfo("returnedContentId:" + returnedContentId, MODULE);
        }
        request.setAttribute("contentId" + suffix, ftlResults.get("contentId"));
        request.setAttribute("caContentIdTo" + suffix, ftlResults.get("contentIdTo"));
        request.setAttribute("caContentIdStart" + suffix, ftlResults.get("contentIdTo"));
        request.setAttribute("caContentAssocTypeId" + suffix, ftlResults.get("contentAssocTypeId"));
        request.setAttribute("caFromDate" + suffix, ftlResults.get("fromDate"));
        request.setAttribute("drDataResourceId" + suffix, ftlResults.get("dataResourceId"));
        request.setAttribute("caContentId" + suffix, ftlResults.get("contentId"));

        String caContentIdTo = (String) passedParams.get("caContentIdTo");
        if (UtilValidate.isNotEmpty(caContentIdTo)) {
            Map<String, Object> resequenceContext = new HashMap<>();
            resequenceContext.put("contentIdTo", caContentIdTo);
            resequenceContext.put("userLogin", userLogin);
            try {
                ftlResults = dispatcher.runSync("resequence", resequenceContext);
                if (ServiceUtil.isError(ftlResults)) {
                    String errorMessage = ServiceUtil.getErrorMessage(ftlResults);
                    request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                    Debug.logError(errorMessage, MODULE);
                    return "error";
                }
            } catch (ServiceAuthException e) {
                String msg = e.getMessage();
                request.setAttribute("_ERROR_MESSAGE_", msg);
                List<String> errorMsgList = UtilGenerics.cast(request.getAttribute("_EVENT_MESSAGE_LIST_"));
                if (Debug.infoOn()) {
                    Debug.logInfo("[UploadContentStuff]errorMsgList:" + errorMsgList, MODULE);
                    Debug.logInfo("[UploadContentStuff]msg:" + msg, MODULE);
                }
                if (errorMsgList == null) {
                    errorMsgList = new LinkedList<>();
                    request.setAttribute("errorMessageList", errorMsgList);
                }
                errorMsgList.add(msg);
                return "error";
            }
        }
        return "success";
    }

} // end of UploadContentAndImage
