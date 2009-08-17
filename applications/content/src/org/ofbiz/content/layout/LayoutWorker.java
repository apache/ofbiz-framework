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

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import javolution.util.FastMap;

import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.service.ServiceUtil;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 * LayoutWorker Class
 */
public class LayoutWorker {

    public static final String module = LayoutWorker.class.getName();
    public static final String err_resource = "ContentErrorUiLabels";

    /**
     * Uploads image data from a form and stores it in ImageDataResource.
     * Expects key data in a field identitified by the "idField" value
     * and the binary data to be in a field id'd by uploadField.
     */
    public static Map uploadImageAndParameters(HttpServletRequest request, String uploadField) {

        //Debug.logVerbose("in uploadAndStoreImage", "");
        Locale locale = UtilHttp.getLocale(request);

        Map results = FastMap.newInstance();
        Map formInput = FastMap.newInstance();
        results.put("formInput", formInput);
        ServletFileUpload fu = new ServletFileUpload(new DiskFileItemFactory(10240, new File(new File("runtime"), "tmp")));
        java.util.List lst = null;
        try {
           lst = fu.parseRequest(request);
        } catch (FileUploadException e4) {
            return ServiceUtil.returnError(e4.getMessage());
        }

        if (lst.size() == 0) {
            String errMsg = UtilProperties.getMessage(LayoutWorker.err_resource, "layoutEvents.no_files_uploaded", locale);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            //Debug.logWarning("[DataEvents.uploadImage] No files uploaded", module);
            return ServiceUtil.returnError("No files uploaded.");
        }


        // This code finds the idField and the upload FileItems
        FileItem fi = null;
        FileItem imageFi = null;
        for (int i=0; i < lst.size(); i++) {
            fi = (FileItem)lst.get(i);
            String fn = fi.getName();
            String fieldName = fi.getFieldName();
            String fieldStr = fi.getString();
            if (fi.isFormField()) {
                formInput.put(fieldName, fieldStr);
            //Debug.logVerbose("in uploadAndStoreImage, fieldName:" + fieldName + " fieldStr:" + fieldStr, "");
            }
            if (fieldName.equals(uploadField)) {
                imageFi = fi;
                //MimeType of upload file
                results.put("uploadMimeType", fi.getContentType());
            }
        }

        if (imageFi == null) {
            Map messageMap = UtilMisc.toMap("imageFi", imageFi);
            String errMsg = UtilProperties.getMessage(LayoutWorker.err_resource, "layoutEvents.image_null", messageMap, locale);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            //Debug.logWarning("[DataEvents.uploadImage] imageFi(" + imageFi + ") is null", module);
            return null;
        }

        byte[] imageBytes = imageFi.get();
        ByteBuffer byteWrap = ByteBuffer.wrap(imageBytes);
        results.put("imageData", byteWrap);
        results.put("imageFileName", imageFi.getName());

        //Debug.logVerbose("in uploadAndStoreImage, results:" + results, "");
        return results;
    }

    public static ByteBuffer returnByteBuffer(Map map) {
        ByteBuffer byteBuff = (ByteBuffer)map.get("imageData");
        return byteBuff;
    }
}
