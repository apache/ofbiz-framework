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
 */
package org.apache.ofbiz.common.qrcode;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Events for QRCode.
 */
public class QRCodeEvents {

    public static final String module = QRCodeEvents.class.getName();

    /** Streams QR Code to the output. */
    public static String serveQRCodeImage(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Map<String, Object> parameters = UtilHttp.getParameterMap(request);
        String message = (String) parameters.get("message");
        GenericValue userLogin = (GenericValue) request.getAttribute("userLogin");
        if (userLogin == null) {
            userLogin = (GenericValue) session.getAttribute("userLogin");
        }
        if (userLogin == null) {
            userLogin = (GenericValue) session.getAttribute("autoUserLogin");
        }
        Locale locale = UtilHttp.getLocale(request);

        if (UtilValidate.isEmpty(message)) {
            message = "Error get message parameter.";
        }
        String format = (String) parameters.get("format");
        if (UtilValidate.isEmpty(format)) {
            format = "jpg";
        }
        String mimeType = "image/" + format;
        String width = (String) parameters.get("width");
        String height = (String) parameters.get("height");
        String encoding = (String) parameters.get("encoding");
        Boolean verifyOutput = Boolean.valueOf((String) parameters.get("verifyOutput"));
        String logoImageMaxWidth = (String) parameters.get("logoImageMaxWidth");
        String logoImageMaxHeight = (String) parameters.get("logoImageMaxHeight");

        try {
            response.setContentType(mimeType);
            OutputStream os = response.getOutputStream();
            Map<String, Object> context = UtilMisc.<String, Object>toMap("message", message, "format", format, "userLogin", userLogin, "locale", locale);
            if (UtilValidate.isNotEmpty(width)) {
                try {
                    context.put("width", Integer.parseInt(width));
                } catch (NumberFormatException e) {
                    Debug.logWarning(e, e.getMessage(), module);
                }
                if (UtilValidate.isEmpty(height)) {
                    try {
                        context.put("height", Integer.parseInt(width));
                    } catch (NumberFormatException e) {
                        Debug.logWarning(e, e.getMessage(), module);
                    }
                }
            }
            if (UtilValidate.isNotEmpty(height)) {
                try {
                    context.put("height", Integer.parseInt(height));
                } catch (NumberFormatException e) {
                    Debug.logWarning(e, e.getMessage(), module);
                }
                if (UtilValidate.isEmpty(width)) {
                    try {
                        context.put("width", Integer.parseInt(height));
                    } catch (NumberFormatException e) {
                        Debug.logWarning(e, e.getMessage(), module);
                    }
                }
            }
            if (UtilValidate.isNotEmpty(encoding)) {
                context.put("encoding", encoding);
            }
            if (UtilValidate.isNotEmpty(verifyOutput) && verifyOutput) {
                context.put("verifyOutput", verifyOutput);
            }
            if (UtilValidate.isNotEmpty(logoImageMaxWidth)) {
                try {
                    context.put("logoImageMaxWidth", Integer.parseInt(logoImageMaxWidth));
                } catch (NumberFormatException e) {
                    Debug.logWarning(e, e.getMessage(), module);
                }
            }
            if (UtilValidate.isNotEmpty(logoImageMaxHeight)) {
                try {
                    context.put("logoImageMaxHeight", Integer.parseInt(logoImageMaxHeight));
                } catch (NumberFormatException e) {
                    Debug.logWarning(e, e.getMessage(), module);
                }
            }
            Map<String, Object> results = dispatcher.runSync("generateQRCodeImage", context);
            if (ServiceUtil.isSuccess(results)) {
                BufferedImage bufferedImage = (BufferedImage) results.get("bufferedImage");
                if (!ImageIO.write(bufferedImage, format, os)) {
                    String errMsg = UtilProperties.getMessage("QRCodeUiLabels", "ErrorWriteFormatToFile", new Object[] { format }, locale);
                    request.setAttribute("_ERROR_MESSAGE_", errMsg);
                    return "error";
                }
                os.flush();
            } else {
                String errMsg = ServiceUtil.getErrorMessage(results);
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
        } catch (IOException | GenericServiceException e) {
            String errMsg = UtilProperties.getMessage("QRCodeUiLabels", "ErrorGenerateQRCode", new Object[] { e.getMessage() }, locale);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        return "success";
    }
}
