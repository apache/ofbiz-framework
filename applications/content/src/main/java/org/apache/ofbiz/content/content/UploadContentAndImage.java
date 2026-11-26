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
 *******************************************************************************/
package org.apache.ofbiz.content.content;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.ModelEntity;
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

    public UploadContentAndImage() { }

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
                ftlResults = dispatcher.runSync("resequenceContentAssocChildren", resequenceContext);
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
