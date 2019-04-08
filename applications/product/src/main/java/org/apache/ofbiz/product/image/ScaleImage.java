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
package org.apache.ofbiz.product.image;

import java.awt.image.BufferedImage;
import java.awt.image.ImagingOpException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import org.jdom.JDOMException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.common.image.ImageTransform;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * ScaleImage Class
 * <p>
 * Scale the original image into 4 different size Types (small, medium, large, detail)
 */
public class ScaleImage {

    public static final String module = ScaleImage.class.getName();
    public static final String resource = "ProductErrorUiLabels";
    /* public so that other code can easily use the imageUrlMap returned by scaleImageInAllSize */
    public static final List<String> sizeTypeList = UtilMisc.toList("small", "medium", "large", "detail");


    public ScaleImage() {
    }

    /**
     * scaleImageInAllSize
     * <p>
     * Scale the original image into all different size Types (small, medium, large, detail)
     *
     * @param   context                     Context
     * @param   filenameToUse               Filename of future image files
     * @param   viewType                    "Main" view or "additional" view
     * @param   viewNumber                  If it's the main view, viewNumber = "0"
     * @return                              URL images for all different size types
     * @throws  IllegalArgumentException    Any parameter is null
     * @throws  ImagingOpException          The transform is non-invertible
     * @throws  IOException                 Error prevents the document from being fully parsed
     * @throws  JDOMException               Errors occur in parsing
     */
    public static Map<String, Object> scaleImageInAllSize(Map<String, ? extends Object> context, String filenameToUse, String viewType, String viewNumber)
        throws IllegalArgumentException, ImagingOpException, IOException, JDOMException {

        /* VARIABLES */
        Locale locale = (Locale) context.get("locale");
        
        int index;
        Map<String, Map<String, String>> imgPropertyMap = new HashMap<String, Map<String,String>>();
        BufferedImage bufImg, bufNewImg;
        double imgHeight, imgWidth;
        Map<String, String> imgUrlMap = new HashMap<String, String>();
        Map<String, Object> resultXMLMap = new HashMap<String, Object>();
        Map<String, Object> resultBufImgMap = new HashMap<String, Object>();
        Map<String, Object> resultScaleImgMap = new HashMap<String, Object>();
        Map<String, Object> result = new HashMap<String, Object>();

        /* ImageProperties.xml */
        String imgPropertyFullPath = System.getProperty("ofbiz.home") + "/applications/product/config/ImageProperties.xml";
        resultXMLMap.putAll(ImageTransform.getXMLValue(imgPropertyFullPath, locale));
        if (resultXMLMap.containsKey("responseMessage") && resultXMLMap.get("responseMessage").equals("success")) {
            imgPropertyMap.putAll(UtilGenerics.<Map<String, Map<String, String>>>cast(resultXMLMap.get("xml")));
        } else {
            String errMsg = UtilProperties.getMessage(resource, "ScaleImage.unable_to_parse", locale) + " : ImageProperties.xml";
            Debug.logError(errMsg, module);
            result.put("errorMessage", errMsg);
            return result;
        }

        /* IMAGE */
        // get Name and Extension
        index = filenameToUse.lastIndexOf(".");
        String imgExtension = filenameToUse.substring(index + 1);
        // paths
        
        Map<String, Object> imageContext = new HashMap<String, Object>();
        imageContext.putAll(context);
        imageContext.put("tenantId",((Delegator)context.get("delegator")).getDelegatorTenantId());
        String imageServerPath = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog", "image.server.path", (Delegator)context.get("delegator")), imageContext);
        String imageUrlPrefix = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog", "image.url.prefix", (Delegator)context.get("delegator")), imageContext);
        imageServerPath = imageServerPath.endsWith("/") ? imageServerPath.substring(0, imageServerPath.length()-1) : imageServerPath;
        imageUrlPrefix = imageUrlPrefix.endsWith("/") ? imageUrlPrefix.substring(0, imageUrlPrefix.length()-1) : imageUrlPrefix;
        FlexibleStringExpander filenameExpander;
        String fileLocation = null;
        String type = null;
        String id = null;
        if (viewType.toLowerCase().contains("main")) {
            String filenameFormat = EntityUtilProperties.getPropertyValue("catalog", "image.filename.format", (Delegator) context.get("delegator"));
            filenameExpander = FlexibleStringExpander.getInstance(filenameFormat);
            id = (String) context.get("productId");
            fileLocation = filenameExpander.expandString(UtilMisc.toMap("location", "products", "id", id, "type", "original"));
        } else if (viewType.toLowerCase().contains("additional") && viewNumber != null && !viewNumber.equals("0")) {
            String filenameFormat = EntityUtilProperties.getPropertyValue("catalog", "image.filename.additionalviewsize.format", (Delegator) context.get("delegator"));
            filenameExpander = FlexibleStringExpander.getInstance(filenameFormat);
            id = (String) context.get("productId");
            if (filenameFormat.endsWith("${id}")) {
                id = id + "_View_" + viewNumber;
            } else {
                viewType = "additional" + viewNumber;
            }    
            fileLocation = filenameExpander.expandString(UtilMisc.toMap("location", "products", "id", id, "viewtype", viewType, "sizetype", "original"));
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ProductImageViewType", UtilMisc.toMap("viewType", type), locale));
        }

        if (fileLocation.lastIndexOf("/") != -1) {
            fileLocation.substring(0, fileLocation.lastIndexOf("/") + 1); // adding 1 to include the trailing slash
        }
        
        /* get original BUFFERED IMAGE */
        resultBufImgMap.putAll(ImageTransform.getBufferedImage(imageServerPath + "/" + fileLocation + "." + imgExtension, locale));

        if (resultBufImgMap.containsKey("responseMessage") && resultBufImgMap.get("responseMessage").equals("success")) {
            bufImg = (BufferedImage) resultBufImgMap.get("bufferedImage");

            // get Dimensions
            imgHeight = bufImg.getHeight();
            imgWidth = bufImg.getWidth();
            if (imgHeight == 0.0 || imgWidth == 0.0) {
                String errMsg = UtilProperties.getMessage(resource, "ScaleImage.one_current_image_dimension_is_null", locale) + " : imgHeight = " + imgHeight + " ; imgWidth = " + imgWidth;
                Debug.logError(errMsg, module);
                result.put("errorMessage", errMsg);
                return result;
            }

            /* Scale image for each size from ImageProperties.xml */
            for (Map.Entry<String, Map<String, String>> entry : imgPropertyMap.entrySet()) {
                String sizeType = entry.getKey();
                
                // Scale
                resultScaleImgMap.putAll(ImageTransform.scaleImage(bufImg, imgHeight, imgWidth, imgPropertyMap, sizeType, locale));

                /* Write the new image file */
                if (resultScaleImgMap.containsKey("responseMessage") && resultScaleImgMap.get("responseMessage").equals("success")) {
                    bufNewImg = (BufferedImage) resultScaleImgMap.get("bufferedImage");

                    // Build full path for the new scaled image
                    String newFileLocation = null;
                    filenameToUse = sizeType + filenameToUse.substring(filenameToUse.lastIndexOf("."));
                    if (viewType.toLowerCase().contains("main")) {
                        newFileLocation = filenameExpander.expandString(UtilMisc.toMap("location", "products", "id", id, "type", sizeType));
                    } else if (viewType.toLowerCase().contains("additional")) {
                        newFileLocation = filenameExpander.expandString(UtilMisc.toMap("location", "products", "id", id, "viewtype", viewType, "sizetype", sizeType));
                    }
                    String newFilePathPrefix = "";
                    if (newFileLocation.lastIndexOf("/") != -1) {
                        newFilePathPrefix = newFileLocation.substring(0, newFileLocation.lastIndexOf("/") + 1); // adding 1 to include the trailing slash
                    }     
                    // Directory
                    String targetDirectory = imageServerPath + "/" + newFilePathPrefix;
                    try {
                        // Create the new directory
                        File targetDir = new File(targetDirectory);
                        if (!targetDir.exists()) {
                            boolean created = targetDir.mkdirs();
                            if (!created) {
                                String errMsg = UtilProperties.getMessage(resource, "ScaleImage.unable_to_create_target_directory", locale) + " - " + targetDirectory;
                                Debug.logFatal(errMsg, module);
                                return ServiceUtil.returnError(errMsg);
                            }
                        // Delete existing image files
                        // Images aren't ordered by productId (${location}/${viewtype}/${sizetype}/${id}) !!! BE CAREFUL !!!
                        } else if (newFileLocation.endsWith("/" + id)) {
                            try {
                                File[] files = targetDir.listFiles(); 
                                for (File file : files) {
                                    if (file.isFile() && file.getName().startsWith(id)) {
                                        file.delete();
                                    }
                                }
                            } catch (SecurityException e) {
                                Debug.logError(e,module);
                            }
                        }
                    } catch (NullPointerException e) {
                        Debug.logError(e,module);
                    }

                    // write new image
                    try {
                        ImageIO.write(bufNewImg, imgExtension, new File(imageServerPath + "/" + newFileLocation + "." + imgExtension));
                    } catch (IllegalArgumentException e) {
                        String errMsg = UtilProperties.getMessage(resource, "ScaleImage.one_parameter_is_null", locale) + e.toString();
                        Debug.logError(errMsg, module);
                        result.put("errorMessage", errMsg);
                        return result;
                    } catch (IOException e) {
                        String errMsg = UtilProperties.getMessage(resource, "ScaleImage.error_occurs_during_writing", locale) + e.toString();
                        Debug.logError(errMsg, module);
                        result.put("errorMessage", errMsg);
                        return result;
                    }

                    // Save each Url
                    if (sizeTypeList.contains(sizeType)) {
                        String imageUrl = imageUrlPrefix + "/" + newFileLocation + "." + imgExtension;
                        imgUrlMap.put(sizeType, imageUrl);
                    }

                } // scaleImgMap
            } // Loop over sizeType

            result.put("responseMessage", "success");
            result.put("imageUrlMap", imgUrlMap);
            result.put("original", resultBufImgMap);
            return result;

        } else {
            String errMsg = UtilProperties.getMessage(resource, "ScaleImage.unable_to_scale_original_image", locale) + " : " + filenameToUse;
            Debug.logError(errMsg, module);
            result.put("errorMessage", errMsg);
            return ServiceUtil.returnError(errMsg);
        }
    }

    public static Map<String, Object> scaleImageManageInAllSize(Map<String, ? extends Object> context, String filenameToUse, String viewType, String viewNumber , String imageType)
        throws IllegalArgumentException, ImagingOpException, IOException, JDOMException {

        /* VARIABLES */
        Locale locale = (Locale) context.get("locale");
        List<String> sizeTypeList = null;
        if (UtilValidate.isNotEmpty(imageType)) {
            sizeTypeList = UtilMisc.toList(imageType);
        } else {
            sizeTypeList = UtilMisc.toList("small", "medium", "large", "detail");
        }
        
        int index;
        Map<String, Map<String, String>> imgPropertyMap = new HashMap<String, Map<String, String>>();
        BufferedImage bufImg, bufNewImg;
        double imgHeight, imgWidth;
        Map<String, String> imgUrlMap = new HashMap<String, String>();
        Map<String, Object> resultXMLMap = new HashMap<String, Object>();
        Map<String, Object> resultBufImgMap = new HashMap<String, Object>();
        Map<String, Object> resultScaleImgMap = new HashMap<String, Object>();
        Map<String, Object> result = new HashMap<String, Object>();

        /* ImageProperties.xml */
        String imgPropertyFullPath = System.getProperty("ofbiz.home") + "/applications/product/config/ImageProperties.xml";
        resultXMLMap.putAll(ImageTransform.getXMLValue(imgPropertyFullPath, locale));
        if (resultXMLMap.containsKey("responseMessage") && resultXMLMap.get("responseMessage").equals("success")) {
            imgPropertyMap.putAll(UtilGenerics.<Map<String, Map<String, String>>>cast(resultXMLMap.get("xml")));
        } else {
            String errMsg = UtilProperties.getMessage(resource, "ScaleImage.unable_to_parse", locale) + " : ImageProperties.xml";
            Debug.logError(errMsg, module);
            result.put("errorMessage", errMsg);
            return result;
        }

        /* IMAGE */
        // get Name and Extension
        index = filenameToUse.lastIndexOf(".");
        String imgName = filenameToUse.substring(0, index - 1);
        String imgExtension = filenameToUse.substring(index + 1);
        // paths
        Map<String, Object> imageContext = new HashMap<String, Object>();
        imageContext.putAll(context);
        imageContext.put("tenantId",((Delegator)context.get("delegator")).getDelegatorTenantId());
        String mainFilenameFormat = EntityUtilProperties.getPropertyValue("catalog", "image.filename.format", (Delegator) context.get("delegator"));

        String imageServerPath = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog", "image.server.path", (Delegator)context.get("delegator")), imageContext);
        String imageUrlPrefix = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog", "image.url.prefix",(Delegator)context.get("delegator")), imageContext);
        imageServerPath = imageServerPath.endsWith("/") ? imageServerPath.substring(0, imageServerPath.length()-1) : imageServerPath;
        imageUrlPrefix = imageUrlPrefix.endsWith("/") ? imageUrlPrefix.substring(0, imageUrlPrefix.length()-1) : imageUrlPrefix;
        String id = null;
        String type = null;
        if (viewType.toLowerCase().contains("main")) {
            type = "original";
            id = imgName;
        } else if (viewType.toLowerCase().contains("additional") && viewNumber != null && !viewNumber.equals("0")) {
            type = "additional";
            id = imgName + "_View_" + viewNumber;
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "ProductImageViewType", UtilMisc.toMap("viewType", type), locale));
        }
        FlexibleStringExpander mainFilenameExpander = FlexibleStringExpander.getInstance(mainFilenameFormat);
        String fileLocation = mainFilenameExpander.expandString(UtilMisc.toMap("location", "products", "id", context.get("productId"), "type", type));
        String filePathPrefix = "";
        if (fileLocation.lastIndexOf("/") != -1) {
            filePathPrefix = fileLocation.substring(0, fileLocation.lastIndexOf("/") + 1); // adding 1 to include the trailing slash
        }
        
        if (context.get("contentId") != null){
            resultBufImgMap.putAll(ImageTransform.getBufferedImage(imageServerPath + "/" + context.get("productId") + "/" + context.get("clientFileName"), locale));
        } else {
            /* get original BUFFERED IMAGE */
            resultBufImgMap.putAll(ImageTransform.getBufferedImage(imageServerPath + "/" + filePathPrefix + filenameToUse, locale));
        }
        
        if (resultBufImgMap.containsKey("responseMessage") && resultBufImgMap.get("responseMessage").equals("success")) {
            bufImg = (BufferedImage) resultBufImgMap.get("bufferedImage");

            // get Dimensions
            imgHeight = bufImg.getHeight();
            imgWidth = bufImg.getWidth();
            if (imgHeight == 0.0 || imgWidth == 0.0) {
                String errMsg = UtilProperties.getMessage(resource, "ScaleImage.one_current_image_dimension_is_null", locale) + " : imgHeight = " + imgHeight + " ; imgWidth = " + imgWidth;
                Debug.logError(errMsg, module);
                result.put("errorMessage", errMsg);
                return result;
            }

            // new Filename Format
            FlexibleStringExpander addFilenameExpander = mainFilenameExpander;
            if (viewType.toLowerCase().contains("additional")) {
                String addFilenameFormat = EntityUtilProperties.getPropertyValue("catalog", "image.filename.additionalviewsize.format", (Delegator) context.get("delegator"));
                addFilenameExpander = FlexibleStringExpander.getInstance(addFilenameFormat);
            }

            /* scale Image for each Size Type */
            for (String sizeType : sizeTypeList) {
                resultScaleImgMap.putAll(ImageTransform.scaleImage(bufImg, imgHeight, imgWidth, imgPropertyMap, sizeType, locale));

                if (resultScaleImgMap.containsKey("responseMessage") && resultScaleImgMap.get("responseMessage").equals("success")) {
                    bufNewImg = (BufferedImage) resultScaleImgMap.get("bufferedImage");

                    // write the New Scaled Image
                    String newFileLocation = null;
                    if (viewType.toLowerCase().contains("main")) {
                        newFileLocation = mainFilenameExpander.expandString(UtilMisc.toMap("location", "products", "id", id, "type", sizeType));
                    } else if (viewType.toLowerCase().contains("additional")) {
                        newFileLocation = addFilenameExpander.expandString(UtilMisc.toMap("location", "products","id", id, "viewtype", viewType, "sizetype", sizeType));
                    }
                    String newFilePathPrefix = "";
                    if (newFileLocation.lastIndexOf("/") != -1) {
                        newFilePathPrefix = newFileLocation.substring(0, newFileLocation.lastIndexOf("/") + 1); // adding 1 to include the trailing slash
                    }

                    String targetDirectory = imageServerPath + "/" + newFilePathPrefix;
                    File targetDir = new File(targetDirectory);
                    if (!targetDir.exists()) {
                        boolean created = targetDir.mkdirs();
                        if (!created) {
                            String errMsg = UtilProperties.getMessage(resource, "ScaleImage.unable_to_create_target_directory", locale) + " - " + targetDirectory;
                            Debug.logFatal(errMsg, module);
                            return ServiceUtil.returnError(errMsg);
                        }
                    }

                    // write new image
                    try {
                        ImageIO.write(bufNewImg, imgExtension, new File(imageServerPath + "/" + newFilePathPrefix + filenameToUse));
                    } catch (IllegalArgumentException e) {
                        String errMsg = UtilProperties.getMessage(resource, "ScaleImage.one_parameter_is_null", locale) + e.toString();
                        Debug.logError(errMsg, module);
                        result.put("errorMessage", errMsg);
                        return result;
                    } catch (IOException e) {
                        String errMsg = UtilProperties.getMessage(resource, "ScaleImage.error_occurs_during_writing", locale) + e.toString();
                        Debug.logError(errMsg, module);
                        result.put("errorMessage", errMsg);
                        return result;
                    }

                    /* write Return Result */
                    String imageUrl = imageUrlPrefix + "/" + newFilePathPrefix + filenameToUse;
                    imgUrlMap.put(sizeType, imageUrl);

                } // scaleImgMap
            } // sizeIter

            result.put("responseMessage", "success");
            result.put("imageUrlMap", imgUrlMap);
            result.put("original", resultBufImgMap);
            return result;

        } else {
            String errMsg = UtilProperties.getMessage(resource, "ScaleImage.unable_to_scale_original_image", locale) + " : " + filenameToUse;
            Debug.logError(errMsg, module);
            result.put("errorMessage", errMsg);
            return ServiceUtil.returnError(errMsg);
        }
    }
}
