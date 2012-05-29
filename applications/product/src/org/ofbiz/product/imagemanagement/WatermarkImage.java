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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

import watermarker.exception.WatermarkerException;
import watermarker.impl.DefaultWatermarker;
import watermarker.model.WatermarkSettings;
import watermarker.model.WatermarkerSettings;

public class WatermarkImage{
    
    public static final String module = WatermarkImage.class.getName();
    public static final String resource = "ProductErrorUiLabels";
    
    public static String createWatermarkImage(HttpServletRequest request, HttpServletResponse response) throws WatermarkerException, IOException {
        Map<String, ? extends Object> context = UtilGenerics.checkMap(request.getParameterMap());
        String imageServerPath = FlexibleStringExpander.expandString(UtilProperties.getPropertyValue("catalog", "image.management.path"), context);
        String imageServerUrl = FlexibleStringExpander.expandString(UtilProperties.getPropertyValue("catalog", "image.management.url"), context);
        String nameOfThumb = FlexibleStringExpander.expandString(UtilProperties.getPropertyValue("catalog", "image.management.nameofthumbnail"), context);
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        String watermarkText = null;
        URL imageUrl = null;
        String productId = request.getParameter("productId");
        String imageName = request.getParameter("imageName");
        String text = request.getParameter("textWatermark");
        String opacity = request.getParameter("opacity");
        String x = request.getParameter("pointX");
        String y = request.getParameter("pointY");
        String width = request.getParameter("width");
        String count = request.getParameter("count");
        String fontColor = request.getParameter("colorWatermark");
        String fontSize = request.getParameter("sizeWatermark");
        
        File file = new File(imageServerPath + "/preview/" + "/previewImage" + count  + ".jpg");
        file.delete();
        try {
            if (UtilValidate.isNotEmpty(imageName)) {
                imageUrl = new URL("file:" + imageServerPath + "/" + productId + "/" + imageName);
            } else {
                String errMsg = "Please select Image.";
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
            
            if (UtilValidate.isNotEmpty(text)) {
                watermarkText = text;
            } else {
                String errMsg = "Please enter Text.";
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
            
            WatermarkerSettings watermarkerSettings = WatermarkerSettings.DEFAULT;
            
            if (UtilValidate.isNotEmpty(fontColor)) {
                Color graphicsColor = setFontColor(fontColor);
                watermarkerSettings.setGraphicsColor(graphicsColor);
            } else {
                String errMsg = "Please select Text Color.";
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
            
            DecimalFormat decimalFormat = new DecimalFormat();
            decimalFormat.applyPattern("0.00");
            if (UtilValidate.isNotEmpty(fontSize)) {
                BigDecimal widthBase = new BigDecimal(600.00);
                BigDecimal picWidth = new BigDecimal(decimalFormat.format(Float.parseFloat(width)));
                Font graphicsFont = setFontSize(fontSize, picWidth.divide(widthBase, 2));
                watermarkerSettings.setGraphicsFont(graphicsFont);
            } else {
                String errMsg = "Please select Text Size.";
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
            
            WatermarkSettings position = new WatermarkSettings();
            if (UtilValidate.isNotEmpty(x) && UtilValidate.isNotEmpty(y)) {
                BigDecimal positionX = new BigDecimal(decimalFormat.format(Float.parseFloat(x)));
                BigDecimal positionY = new BigDecimal(decimalFormat.format(Float.parseFloat(y)));
                position.setX(positionX.floatValue());
                position.setY(positionY.floatValue());
                watermarkerSettings.setWatermarkSettings(position);
            } else {
                String errMsg = "Please select Text Position.";
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
            
            AlphaComposite alphaComposite = null;
            if (UtilValidate.isNotEmpty(opacity)) {
                BigDecimal opa = new BigDecimal(opacity);
                alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opa.floatValue());
                watermarkerSettings.setAlphaComposite(alphaComposite);
            }
            
            if (UtilValidate.isNotEmpty(imageUrl)) {
                
                Map<String, Object> contentCtx = FastMap.newInstance();
                contentCtx.put("contentTypeId", "DOCUMENT");
                contentCtx.put("userLogin", userLogin);
                Map<String, Object> contentResult = FastMap.newInstance();
                try {
                    contentResult = dispatcher.runSync("createContent", contentCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                return "error";
                }
                
                Map<String, Object> contentThumb = FastMap.newInstance();
                contentThumb.put("contentTypeId", "DOCUMENT");
                contentThumb.put("userLogin", userLogin);
                Map<String, Object> contentThumbResult = FastMap.newInstance();
                try {
                    contentThumbResult = dispatcher.runSync("createContent", contentThumb);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    return e.getMessage();
                }
                
                String contentIdThumb = (String) contentThumbResult.get("contentId");
                String contentId = (String) contentResult.get("contentId");
                String filenameToUse = (String) contentResult.get("contentId") + ".jpg";
                String filenameTouseThumb = (String) contentResult.get("contentId") + nameOfThumb + ".jpg";
                File outputImageFile = new File(imageServerPath + "/" + productId + "/" + filenameToUse);
                OutputStream outputStream = new FileOutputStream(outputImageFile);
                
                // *** Actual call to Watermarker#watermark(...) ***
                new DefaultWatermarker().watermark(imageUrl, watermarkText, outputStream, watermarkerSettings);
                
                String imageUrlResource = imageServerUrl + "/" + productId + "/" + filenameToUse;
                
                BufferedImage bufNewImg = ImageIO.read(new File(imageServerPath + "/" + productId + "/" + filenameToUse));
                
                double imgHeight = bufNewImg.getHeight();
                double imgWidth = bufNewImg.getWidth();
                String mimeType = imageName.substring(imageName.lastIndexOf(".") + 1);
                
                Map<String, Object> resultResize = ImageManagementServices.resizeImageThumbnail(bufNewImg, imgHeight, imgWidth);
                ImageIO.write((RenderedImage) resultResize.get("bufferedImage"), mimeType, new File(imageServerPath + "/" + productId + "/" + filenameTouseThumb));
                
                String imageUrlThumb = imageServerUrl + "/" + productId + "/" + filenameTouseThumb;
                
                createContentAndDataResourceWaterMark(request, userLogin, filenameToUse, imageUrlResource, contentId, "image/jpeg");
                createContentAndDataResourceWaterMark(request, userLogin, filenameTouseThumb, imageUrlThumb, contentIdThumb, "image/jpeg");
                
                Map<String, Object> createContentAssocMap = FastMap.newInstance();
                createContentAssocMap.put("contentAssocTypeId", "IMAGE_THUMBNAIL");
                createContentAssocMap.put("contentId", contentId);
                createContentAssocMap.put("contentIdTo", contentIdThumb);
                createContentAssocMap.put("userLogin", userLogin);
                createContentAssocMap.put("mapKey", "100");
                try {
                    dispatcher.runSync("createContentAssoc", createContentAssocMap);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    return e.getMessage();
                }
                
                Map<String, Object> productContentCtx = FastMap.newInstance();
                productContentCtx.put("productId", productId);
                productContentCtx.put("productContentTypeId", "IMAGE");
                productContentCtx.put("fromDate", UtilDateTime.nowTimestamp());
                productContentCtx.put("userLogin", userLogin);
                productContentCtx.put("contentId", contentId);
                productContentCtx.put("statusId", "IM_PENDING");
                try {
                    dispatcher.runSync("createProductContent", productContentCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    request.setAttribute("_ERROR_MESSAGE_", e.getMessage());return "error";
                }
                
                Map<String, Object> contentApprovalCtx = FastMap.newInstance();
                contentApprovalCtx.put("contentId", contentId);
                contentApprovalCtx.put("userLogin", userLogin);
                try {
                    dispatcher.runSync("createImageContentApproval", contentApprovalCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    request.setAttribute("_ERROR_MESSAGE_", e.getMessage());return "error";
                }
            }
        } catch (WatermarkerException e) {
            String errMsg = "Cannot create watermark.";
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            Debug.logError(e, errMsg, module);
            return "error";
        }
        String eventMsg = "Watermark image successfully.";
        request.setAttribute("_EVENT_MESSAGE_", eventMsg);
        return "success";
    }
    
    public static Map<String, Object> createContentAndDataResourceWaterMark(HttpServletRequest request, GenericValue userLogin, String filenameToUse, String imageUrl, String contentId, String mimeTypeId){
        Map<String, Object> result = FastMap.newInstance();
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        
        Map<String, Object> dataResourceCtx = FastMap.newInstance();
        
        dataResourceCtx.put("objectInfo", imageUrl);
        dataResourceCtx.put("dataResourceName", filenameToUse);
        dataResourceCtx.put("userLogin", userLogin);
        dataResourceCtx.put("dataResourceTypeId", "IMAGE_OBJECT");
        dataResourceCtx.put("mimeTypeId", mimeTypeId);
        dataResourceCtx.put("isPublic", "Y");
        
        Map<String, Object> dataResourceResult = FastMap.newInstance();
        try {
            dataResourceResult = dispatcher.runSync("createDataResource", dataResourceCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        
        Map<String, Object> contentUp = FastMap.newInstance();
        contentUp.put("contentId", contentId);
        contentUp.put("dataResourceId", dataResourceResult.get("dataResourceId"));
        contentUp.put("contentName", filenameToUse);
        contentUp.put("userLogin", userLogin);
        try {
            dispatcher.runSync("updateContent", contentUp);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        
        GenericValue content = null;
        try {
            content = delegator.findOne("Content", UtilMisc.toMap("contentId", contentId), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        
        if (content != null) {
            GenericValue dataResource = null;
            try {
                dataResource = content.getRelatedOne("DataResource", false);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            
            if (dataResource != null) {
                dataResourceCtx.put("dataResourceId", dataResource.getString("dataResourceId"));
                try {
                    dispatcher.runSync("updateDataResource", dataResourceCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }
            }
        }
        return result;
    }
    
    public static String setPreviewWaterMark(HttpServletRequest request, HttpServletResponse response) {
        Map<String, ? extends Object> context = UtilGenerics.checkMap(request.getParameterMap());
        String imageServerPath = FlexibleStringExpander.expandString(UtilProperties.getPropertyValue("catalog", "image.management.path"), context);
        String productId = request.getParameter("productId");
        String imageName = request.getParameter("imageName");
        String text = request.getParameter("text");
        String opacity = request.getParameter("opacity");
        String x = request.getParameter("x");
        String y = request.getParameter("y");
        String width = request.getParameter("width");
        String count = request.getParameter("count");
        String fontColor = request.getParameter("fontColor");
        String fontSize = request.getParameter("fontSize");
        
        String dirPath = "/preview/";
        File dir = new File(imageServerPath + dirPath);
        if (!dir.exists()) {
            boolean createDir = dir.mkdir();
            if (!createDir) {
                request.setAttribute("_ERROR_MESSAGE_", "Cannot create directory.");
                return "error";
            }
        }
        
        BigDecimal opa = new BigDecimal(opacity);
        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.applyPattern("0.00");
        BigDecimal positionX = new BigDecimal(decimalFormat.format(Float.parseFloat(x)));
        BigDecimal positionY = new BigDecimal(decimalFormat.format(Float.parseFloat(y)));
        BigDecimal picWidth = new BigDecimal(decimalFormat.format(Float.parseFloat(width)));
        File file = new File(imageServerPath + "/preview/" + "/previewImage" + count  + ".jpg");
        file.delete();
        BigDecimal widthBase = new BigDecimal(600.00);
        Integer currentPic = Integer.parseInt(count);
        int nextPic = currentPic.intValue() + 1;
        AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opa.floatValue());
        WatermarkSettings position = new WatermarkSettings();
        position.setX(positionX.floatValue());
        position.setY(positionY.floatValue());
        Color graphicsColor = setFontColor(fontColor);
        Font graphicsFont = setFontSize(fontSize, picWidth.divide(widthBase, 2));
        WatermarkerSettings watermarkerSettings = WatermarkerSettings.DEFAULT;
        watermarkerSettings.setGraphicsColor(graphicsColor);
        watermarkerSettings.setGraphicsFont(graphicsFont);
        watermarkerSettings.setWatermarkSettings(position);
        watermarkerSettings.setAlphaComposite(alphaComposite);
        try {
           URL imageUrl = new URL("file:" + imageServerPath + "/" + productId + "/" + imageName);
           File outputImageFile = new File(imageServerPath + "/preview/" + "/previewImage" + nextPic + ".jpg");
           OutputStream outputStream = new FileOutputStream(outputImageFile);
           
           new DefaultWatermarker().watermark(imageUrl, text, outputStream, watermarkerSettings);
           
        } catch (Exception e) {
            String errMsg = "Error from setPreviewWaterMark";
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        return "success";
    }
    
    public static String deletePreviewWatermarkImage(HttpServletRequest request, HttpServletResponse response) {
        Map<String, ? extends Object> context = UtilGenerics.checkMap(request.getParameterMap());
        String imageServerPath = FlexibleStringExpander.expandString(UtilProperties.getPropertyValue("catalog", "image.management.path"), context);
        String count = request.getParameter("count");
        File file = new File(imageServerPath + "/preview/" + "/previewImage" + count  + ".jpg");
        file.delete();
        
        return "success";
    }
    
    private static Color setFontColor(String color) {
        Color graphicsColor = null;
        if (color.equals("TEXT_BLACK")) {
            graphicsColor = Color.BLACK;
        } else if (color.equals("TEXT_WHITE")) {
            graphicsColor = Color.WHITE;
        } else if (color.equals("TEXT_GRAY")) {
            graphicsColor = Color.GRAY;
        } else if(color.equals("TEXT_RED")) {
            graphicsColor = Color.RED;
        } else if(color.equals("TEXT_GREEN")) {
            graphicsColor = Color.GREEN;
        } else if(color.equals("TEXT_BLUE")) {
            graphicsColor = Color.BLUE;
        } else if(color.equals("TEXT_YELLOW")) {
            graphicsColor = Color.YELLOW;
        }
        return graphicsColor;
    }
    
    private static Font setFontSize(String fontSize, BigDecimal multiply) {
        Font graphicsFont = null;
        BigDecimal baseSize = null;
        if (fontSize.equals("TEXT_SMALL")) {
            baseSize = new BigDecimal(24.00);
            graphicsFont = new Font( "Arial", Font.BOLD, baseSize.multiply(multiply).intValue());
        } else if (fontSize.equals("TEXT_MIDDLE")) {
            baseSize = new BigDecimal(36.00);
            graphicsFont = new Font( "Arial", Font.BOLD, baseSize.multiply(multiply).intValue());
        } else if (fontSize.equals("TEXT_LARGE")) {
            baseSize = new BigDecimal(48.00);
            graphicsFont = new Font( "Arial", Font.BOLD, baseSize.multiply(multiply).intValue());
        } else if (fontSize.equals("TEXT_VERYLARGE")) {
            baseSize = new BigDecimal(60.00);
            graphicsFont = new Font( "Arial", Font.BOLD, baseSize.multiply(multiply).intValue());
        }
        return graphicsFont;
    }
}
