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
package org.apache.ofbiz.content.layout;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * LayoutWorker Class
 */
public final class LayoutWorker {

    private static final String MODULE = LayoutWorker.class.getName();
    private static final String ERR_RESOURCE = "ContentErrorUiLabels";

    private LayoutWorker() { }

    /**
     * Uploads image data from a form and stores it in ImageDataResource.
     * Expects key data in a field identitified by the "idField" value
     * and the binary data to be in a field id'd by uploadField.
     */
    public static Map<String, Object> uploadImageAndParameters(HttpServletRequest request, String uploadField) {
        Locale locale = UtilHttp.getLocale(request);

        Map<String, Object> results = new HashMap<>();
        Map<String, String> formInput = new HashMap<>();
        results.put("formInput", formInput);

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
            return ServiceUtil.returnError(e4.getMessage());
        }

        if (lst.isEmpty() && UtilValidate.isNotEmpty(request.getAttribute("fileItems"))) {
            lst = UtilGenerics.cast(request.getAttribute("fileItems"));
        }
        if (lst.isEmpty()) {
            String errMsg = UtilProperties.getMessage(ERR_RESOURCE,
                    "layoutEvents.no_files_uploaded", locale);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return ServiceUtil.returnError(UtilProperties.getMessage(ERR_RESOURCE,
                    "layoutEvents.no_files_uploaded", locale));
        }


        // This code finds the idField and the upload FileItems
        FileItem fi = null;
        FileItem imageFi = null;
        for (FileItem fileItem : lst) {
            fi = fileItem;
            String fieldName = fi.getFieldName();
            String fieldStr = fi.getString();
            if (fi.isFormField()) {
                formInput.put(fieldName, fieldStr);
                request.setAttribute(fieldName, fieldStr);
            }
            if (fieldName.equals(uploadField)) {
                imageFi = fi;
                //MimeType of upload file
                results.put("uploadMimeType", fi.getContentType());
            }
        }

        if (imageFi == null) {
            String errMsg = UtilProperties.getMessage(ERR_RESOURCE,
                    "layoutEvents.image_null", UtilMisc.toMap("imageFi", imageFi), locale);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return null;
        }

        byte[] imageBytes = imageFi.get();
        ByteBuffer byteWrap = ByteBuffer.wrap(imageBytes);
        results.put("imageData", byteWrap);
        results.put("imageFileName", imageFi.getName());
        return results;
    }

    public static ByteBuffer returnByteBuffer(Map<String, ByteBuffer> map) {
        ByteBuffer byteBuff = map.get("imageData");
        return byteBuff;
    }
}
