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

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.swing.ImageIcon;

import org.jdom.JDOMException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.content.layout.LayoutWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

public class FrameImage {

    public static final String module = FrameImage.class.getName();
    public static final String resourceError = "ProductErrorUiLabels";
    public static final String resource = "ProductUiLabels";

    public static Map<String, Object> addImageFrame(DispatchContext dctx, Map<String, ? extends Object> context)
    throws IOException, JDOMException {
        Map<String, Object> result = new HashMap<String, Object>();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String imageServerPath = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog", "image.management.path", delegator), context);
        String imageServerUrl = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog", "image.management.url", delegator), context);
        String nameOfThumb = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog", "image.management.nameofthumbnail", delegator), context);
        
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productId = (String) context.get("productId");
        String imageName = (String) context.get("imageName");
        String imageWidth = (String) context.get("imageWidth");
        String imageHeight = (String) context.get("imageHeight");
        Locale locale = (Locale) context.get("locale");
        
        if (UtilValidate.isEmpty(context.get("frameContentId")) || UtilValidate.isEmpty(context.get("frameDataResourceId"))) {
            result = ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "ProductImageFrameContentIdRequired", locale));
            result.putAll(context);
        }
        if (UtilValidate.isEmpty(context.get("imageWidth")) || UtilValidate.isEmpty(context.get("imageHeight"))) {
            result = ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "ProductImageWidthAndHeightRequired", locale));
            result.putAll(context);
        }
        
        String frameContentId = (String) context.get("frameContentId");
        String frameDataResourceId = (String) context.get("frameDataResourceId");
        
        String frameImageName = null;
        try {
            GenericValue contentDataResourceView = EntityQuery.use(delegator).from("ContentDataResourceView").where("contentId", frameContentId, "drDataResourceId", frameDataResourceId).queryOne();
            frameImageName = contentDataResourceView.getString("contentName");
        } catch (GenericEntityException gee) {
            Debug.logError(gee, module);
            result = ServiceUtil.returnError(gee.getMessage());
            result.putAll(context);
        } catch (Exception e) {
            Debug.logError(e, module);
            result = ServiceUtil.returnError(e.getMessage());
            result.putAll(context);
        }
        
        if (UtilValidate.isNotEmpty(imageName)) {
            
            // Image Frame
            BufferedImage bufImg1 = ImageIO.read(new File(imageServerPath + "/" + productId + "/" + imageName));
            BufferedImage bufImg2 = ImageIO.read(new File(imageServerPath + "/frame/"+frameImageName));
            
            int bufImgType;
            if (BufferedImage.TYPE_CUSTOM == bufImg1.getType()) {
                bufImgType = BufferedImage.TYPE_INT_ARGB_PRE;
            } else {
                bufImgType = bufImg1.getType();
            }
            
            int width = Integer.parseInt(imageWidth);
            int height= Integer.parseInt(imageHeight);
            
            Map<String, Object> contentCtx = new HashMap<String, Object>();
            contentCtx.put("contentTypeId", "DOCUMENT");
            contentCtx.put("userLogin", userLogin);
            Map<String, Object> contentResult = new HashMap<String, Object>();
            try {
                contentResult = dispatcher.runSync("createContent", contentCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                result =  ServiceUtil.returnError(e.getMessage());
                result.putAll(context);
            }
            
            Map<String, Object> contentThumb = new HashMap<String, Object>();
            contentThumb.put("contentTypeId", "DOCUMENT");
            contentThumb.put("userLogin", userLogin);
            Map<String, Object> contentThumbResult = new HashMap<String, Object>();
            try {
                contentThumbResult = dispatcher.runSync("createContent", contentThumb);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                result =  ServiceUtil.returnError(e.getMessage());
                result.putAll(context);
            }
            
            String contentIdThumb = (String) contentThumbResult.get("contentId");
            String contentId = (String) contentResult.get("contentId");
            String filenameToUse = (String) contentResult.get("contentId") + ".jpg";
            String filenameTouseThumb = (String) contentResult.get("contentId") + nameOfThumb + ".jpg";
            
            Image newImg1 = bufImg1.getScaledInstance(width, height , Image.SCALE_SMOOTH);
            Image newImg2 = bufImg2.getScaledInstance(width , height , Image.SCALE_SMOOTH);
            BufferedImage bufNewImg = combineBufferedImage(newImg1, newImg2, bufImgType);
            String mimeType = imageName.substring(imageName.lastIndexOf(".") + 1);
            ImageIO.write(bufNewImg, mimeType, new File(imageServerPath + "/" + productId + "/" + filenameToUse));
            
            double imgHeight = bufNewImg.getHeight();
            double imgWidth = bufNewImg.getWidth();
            
            Map<String, Object> resultResize = ImageManagementServices.resizeImageThumbnail(bufNewImg, imgHeight, imgWidth);
            ImageIO.write((RenderedImage) resultResize.get("bufferedImage"), mimeType, new File(imageServerPath + "/" + productId + "/" + filenameTouseThumb));
            
            String imageUrlResource = imageServerUrl + "/" + productId + "/" + filenameToUse;
            String imageUrlThumb = imageServerUrl + "/" + productId + "/" + filenameTouseThumb;
            
            ImageManagementServices.createContentAndDataResource(dctx, userLogin, filenameToUse, imageUrlResource, contentId, "image/jpeg");
            ImageManagementServices.createContentAndDataResource(dctx, userLogin, filenameTouseThumb, imageUrlThumb, contentIdThumb, "image/jpeg");
            
            Map<String, Object> createContentAssocMap = new HashMap<String, Object>();
            createContentAssocMap.put("contentAssocTypeId", "IMAGE_THUMBNAIL");
            createContentAssocMap.put("contentId", contentId);
            createContentAssocMap.put("contentIdTo", contentIdThumb);
            createContentAssocMap.put("userLogin", userLogin);
            createContentAssocMap.put("mapKey", "100");
            try {
                dispatcher.runSync("createContentAssoc", createContentAssocMap);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                result =  ServiceUtil.returnError(e.getMessage());
                result.putAll(context);
            }
            
            Map<String, Object> productContentCtx = new HashMap<String, Object>();
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
                result =  ServiceUtil.returnError(e.getMessage());
                result.putAll(context);
            }
            
            Map<String, Object> contentApprovalCtx = new HashMap<String, Object>();
            contentApprovalCtx.put("contentId", contentId);
            contentApprovalCtx.put("userLogin", userLogin);
            try {
                dispatcher.runSync("createImageContentApproval", contentApprovalCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                result =  ServiceUtil.returnError(e.getMessage());
                result.putAll(context);
            }
        }
         else{
             String errMsg = UtilProperties.getMessage(resourceError, "ProductPleaseSelectImage", locale);
             Debug.logFatal(errMsg, module);
             result =  ServiceUtil.returnError(errMsg);
             result.putAll(context);
        }
        String successMsg = UtilProperties.getMessage(resource, "ProductFrameImageSuccessfully", locale);
        result = ServiceUtil.returnSuccess(successMsg);
        return result;
    }
    
    public static BufferedImage combineBufferedImage(Image image1, Image image2, int bufImgType) {
        // Full image loading 
        image1 = new ImageIcon(image1).getImage();
        image2 = new ImageIcon(image2).getImage();
        
        // New BufferedImage creation 
        BufferedImage bufferedImage = new BufferedImage(image1.getWidth(null), image1.getHeight(null), bufImgType);
        Graphics2D g = bufferedImage.createGraphics( );
        g.drawImage(image1, null, null);
        
        // Draw Image combine
        Point2D center =  new Point2D.Float(bufferedImage.getHeight() / 2, bufferedImage.getWidth() / 2);
        AffineTransform at = AffineTransform.getTranslateInstance(center.getX( ) - (image2.getWidth(null) / 2), center.getY( ) - (image2.getHeight(null) / 2));
        g.transform(at);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(image2, 0, 0, null);
        Composite c = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .35f);
        g.setComposite(c);
        at = AffineTransform.getTranslateInstance(center.getX( ) - (bufferedImage.getWidth(null) / 2), center.getY( ) - (bufferedImage.getHeight(null) / 2));
        g.setTransform(at);
        g.drawImage(bufferedImage, 0, 0, null);
        g.dispose();
        
        return( bufferedImage );
    }
    
    public static String uploadFrame(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Delegator delegator = dispatcher.getDelegator();
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue)session.getAttribute("userLogin");
        
        Map<String, ? extends Object> context = UtilGenerics.checkMap(request.getParameterMap());
        String imageServerPath = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog", "image.management.path", delegator), context);
        String imageServerUrl = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog", "image.management.url", delegator), context);
        Map<String, Object> tempFile = LayoutWorker.uploadImageAndParameters(request, "uploadedFile");
        String imageName = tempFile.get("imageFileName").toString();
        String mimType = tempFile.get("uploadMimeType").toString();
        ByteBuffer imageData = (ByteBuffer) tempFile.get("imageData");
        if (UtilValidate.isEmpty(imageName) || UtilValidate.isEmpty(imageData)) {
            session.setAttribute("frameContentId", request.getParameter("frameExistContentId").toString());
            session.setAttribute("frameDataResourceId", request.getParameter("frameExistDataResourceId").toString());
            request.setAttribute("_ERROR_MESSAGE_", "There is no frame image, please select the image type *.PNG  uploading.");
            return "error";
        }
        if (!"image/png".equals(mimType)) {
            session.setAttribute("frameContentId", request.getParameter("frameExistContentId").toString());
            session.setAttribute("frameDataResourceId", request.getParameter("frameExistDataResourceId").toString());
            request.setAttribute("_ERROR_MESSAGE_", "The selected image type is incorrect, please select the image type *.PNG to upload.");
            return "error";
        }
        
        String contentId = null;
        String dataResourceId = null;
        try {
            String dirPath = "/frame/";
            File dir = new File(imageServerPath + dirPath);
            if (!dir.exists()) {
                boolean createDir = dir.mkdir();
                if (!createDir) {
                    request.setAttribute("_ERROR_MESSAGE_", "Cannot create directory.");
                    return "error";
                }
            }
            String imagePath = "/frame/" + imageName;
            File file = new File(imageServerPath + imagePath);
            if (file.exists()) {
                request.setAttribute("_ERROR_MESSAGE_", "There is an existing frame, please select from the existing frame.");
                return "error";
            }
            RandomAccessFile out = new RandomAccessFile(file, "rw");
            out.write(imageData.array());
            out.close();
            
            //create dataResource
            Map<String, Object> dataResourceCtx = new HashMap<String, Object>();
            dataResourceCtx.put("objectInfo", imageServerUrl + imagePath);
            dataResourceCtx.put("dataResourceName", imageName);
            dataResourceCtx.put("userLogin", userLogin);
            dataResourceCtx.put("dataResourceTypeId", "IMAGE_OBJECT");
            dataResourceCtx.put("mimeTypeId", "image/png");
            dataResourceCtx.put("isPublic", "Y");
            Map<String, Object> dataResourceResult = dispatcher.runSync("createDataResource", dataResourceCtx);
            dataResourceId = dataResourceResult.get("dataResourceId").toString();
            //create content
            Map<String, Object> contentCtx = new HashMap<String, Object>();
            contentCtx.put("dataResourceId", dataResourceResult.get("dataResourceId").toString());
            contentCtx.put("contentTypeId", "IMAGE_FRAME");
            contentCtx.put("contentName", imageName);
            contentCtx.put("fromDate", UtilDateTime.nowTimestamp());
            contentCtx.put("userLogin", userLogin);
            Map<String, Object> contentResult = dispatcher.runSync("createContent", contentCtx);
            contentId = contentResult.get("contentId").toString();
        } catch (GenericServiceException gse) {
            request.setAttribute("_ERROR_MESSAGE_", gse.getMessage());
            return "error";
        } catch (Exception e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }
        session.setAttribute("frameContentId", contentId);
        session.setAttribute("frameDataResourceId", dataResourceId);
        request.setAttribute("_EVENT_MESSAGE_", "Upload frame image successful.");
        return "success";
    }
    
    public static String previewFrameImage(HttpServletRequest request, HttpServletResponse response) throws IOException, JDOMException {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Map<String, ? extends Object> context = UtilGenerics.checkMap(request.getParameterMap());
        HttpSession session = request.getSession();
        String imageServerPath = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog", "image.management.path", delegator), context);
        
        String productId = request.getParameter("productId");
        String imageName = request.getParameter("imageName");
        
        String dirPath = "/preview/";
        File dir = new File(imageServerPath + dirPath);
        if (!dir.exists()) {
            boolean createDir = dir.mkdir();
            if (!createDir) {
                request.setAttribute("_ERROR_MESSAGE_", "Cannot create directory.");
                return "error";
            }
        }
        
        if (UtilValidate.isEmpty(request.getParameter("frameContentId")) || UtilValidate.isEmpty(request.getParameter("frameDataResourceId"))) {
            request.setAttribute("_ERROR_MESSAGE_", "Required frame image content ID or dataResource ID parameters. Please upload new frame image or choose the exist frame.");
            return "error";
        }
        String frameContentId = request.getParameter("frameContentId");
        String frameDataResourceId = request.getParameter("frameDataResourceId");
        
        if (UtilValidate.isEmpty(request.getParameter("imageWidth")) || UtilValidate.isEmpty(request.getParameter("imageHeight"))) {
            String errMsg = "Image Width and Image Height are required to preview the image. Please enter in Image Width and Image Height fields.";
            session.setAttribute("frameContentId", frameContentId);
            session.setAttribute("frameDataResourceId", frameDataResourceId);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        
        String frameImageName = null;
        try {
            GenericValue contentDataResourceView = EntityQuery.use(delegator).from("ContentDataResourceView").where("contentId", frameContentId, "drDataResourceId", frameDataResourceId).queryOne();
            frameImageName = contentDataResourceView.getString("contentName");
        } catch (GenericEntityException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        } catch (Exception e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }
        if (UtilValidate.isNotEmpty(imageName)) {
            File file = new File(imageServerPath + "/preview/" +"/previewImage.jpg");
            file.delete();
            // Image Frame
            BufferedImage bufImg1 = ImageIO.read(new File(imageServerPath + "/" + productId + "/" + imageName));
            BufferedImage bufImg2 = ImageIO.read(new File(imageServerPath + "/frame/" + frameImageName));
            
            int bufImgType;
            if (BufferedImage.TYPE_CUSTOM == bufImg1.getType()) {
                bufImgType = BufferedImage.TYPE_INT_ARGB_PRE;
            } else {
                bufImgType = bufImg1.getType();
            }
            
            int width = Integer.parseInt(request.getParameter("imageWidth"));
            int height= Integer.parseInt(request.getParameter("imageHeight"));
            
            Image newImg1 = bufImg1.getScaledInstance(width, height , Image.SCALE_SMOOTH);
            Image newImg2 = bufImg2.getScaledInstance(width , height , Image.SCALE_SMOOTH);
            BufferedImage bufNewImg = combineBufferedImage(newImg1, newImg2, bufImgType);
            String mimeType = imageName.substring(imageName.lastIndexOf(".") + 1);
            ImageIO.write(bufNewImg, mimeType, new File(imageServerPath + "/preview/" + "/previewImage.jpg"));
            
        }
         else{
             String errMsg = "Please select Image.";
             request.setAttribute("_EVENT_MESSAGE_", errMsg);
            return "error";
        }
        return "success";
    }
    
    public static String chooseFrameImage(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        if(UtilValidate.isEmpty(request.getParameter("frameContentId"))) {
            if (UtilValidate.isNotEmpty(request.getParameter("frameExistContentId")) && UtilValidate.isNotEmpty(request.getParameter("frameExistDataResourceId"))) {
                session.setAttribute("frameExistContentId", request.getParameter("frameExistContentId").toString());
                session.setAttribute("frameDataResourceId", request.getParameter("frameExistDataResourceId").toString());
            }
            request.setAttribute("_ERROR_MESSAGE_", "Required frame image content ID");
            return "error";
        }
        
        String frameContentId = request.getParameter("frameContentId");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        
        String frameDataResourceId = null;
        try {
            GenericValue contentDataResource = EntityQuery.use(delegator).from("ContentDataResourceView").where("contentId", frameContentId).queryFirst();
            frameDataResourceId = contentDataResource.getString("dataResourceId");
        } catch (GenericEntityException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        } catch (Exception e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }
        session.setAttribute("frameContentId", frameContentId);
        session.setAttribute("frameDataResourceId", frameDataResourceId);
        return "success";
    }
    
    public static String deleteFrameImage(HttpServletRequest request, HttpServletResponse response) {
        Map<String, ? extends Object> context = UtilGenerics.checkMap(request.getParameterMap());
        String imageServerPath = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog", "image.management.path", (Delegator) context.get("delegator")), context);
        File file = new File(imageServerPath + "/preview/" + "/previewImage.jpg");
        if (file.exists()) {
            file.delete();
        }
        return "success";
    }
}
