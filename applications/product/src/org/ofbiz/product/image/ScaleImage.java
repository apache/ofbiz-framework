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
package org.ofbiz.product.image;

import java.awt.image.BufferedImage;
import java.awt.image.ImagingOpException;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import javolution.util.FastMap;

import org.jdom.JDOMException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.common.image.ImageTransform;

/**
 * ScaleImage Class
 * <p>
 * Scale the original image into 4 different size Types (small, medium, large, detail)
 */
public class ScaleImage {

    public static final String module = ScaleImage.class.getName();
    public static final String resource = "ProductErrorUiLabels";

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
        List<String> sizeTypeList = UtilMisc.toList("small", "medium", "large", "detail");
        int index;
        Map<String, Map<String, String>> imgPropertyMap = FastMap.newInstance();
        BufferedImage bufImg, bufNewImg;
        double imgHeight, imgWidth;
        Map<String, String> imgUrlMap = FastMap.newInstance();
        Map<String, Object> resultXMLMap = FastMap.newInstance();
        Map<String, Object> resultBufImgMap = FastMap.newInstance();
        Map<String, Object> resultScaleImgMap = FastMap.newInstance();
        Map<String, Object> result = FastMap.newInstance();

        /* ImageProperties.xml */
        String imgPropertyFullPath = System.getProperty("ofbiz.home") + "/applications/product/config/ImageProperties.xml";
        resultXMLMap.putAll((Map<String, Object>) ImageTransform.getXMLValue(imgPropertyFullPath, locale));
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
        String mainFilenameFormat = UtilProperties.getPropertyValue("catalog", "image.filename.format");
        String imageServerPath = FlexibleStringExpander.expandString(UtilProperties.getPropertyValue("catalog", "image.server.path"), context);
        String imageUrlPrefix = UtilProperties.getPropertyValue("catalog", "image.url.prefix");

        String id = null;
        String type = null;
        if (viewType.toLowerCase().contains("main")) {
            type = "original";
            id = imgName;
        } else if (viewType.toLowerCase().contains("additional") && viewNumber != null && !viewNumber.equals("0")) {
            type = "additional";
            id = imgName + "_View_" + viewNumber;
        } else {
            return ServiceUtil.returnError("View Type : " + type + " is wrong");
        }
        FlexibleStringExpander mainFilenameExpander = FlexibleStringExpander.getInstance(mainFilenameFormat);
        String fileLocation = mainFilenameExpander.expandString(UtilMisc.toMap("location", "products", "type", type, "id", filenameToUse));
        String filePathPrefix = "";
        if (fileLocation.lastIndexOf("/") != -1) {
            filePathPrefix = fileLocation.substring(0, fileLocation.lastIndexOf("/") + 1); // adding 1 to include the trailing slash
        }



        /* get original BUFFERED IMAGE */
        resultBufImgMap.putAll(ImageTransform.getBufferedImage(imageServerPath + "/" + filePathPrefix + filenameToUse, locale));

        if (resultBufImgMap.containsKey("responseMessage") && resultBufImgMap.get("responseMessage").equals("success")) {
            bufImg = (BufferedImage) resultBufImgMap.get("bufferedImage");

            // get Dimensions
            imgHeight = (double) bufImg.getHeight();
            imgWidth = (double) bufImg.getWidth();
            if (imgHeight == 0.0 || imgWidth == 0.0) {
                String errMsg = UtilProperties.getMessage(resource, "ScaleImage.one_current_image_dimension_is_null", locale) + " : imgHeight = " + imgHeight + " ; imgWidth = " + imgWidth;
                Debug.logError(errMsg, module);
                result.put("errorMessage", errMsg);
                return result;
            }

            // new Filename Format
            FlexibleStringExpander addFilenameExpander = mainFilenameExpander;
            if (viewType.toLowerCase().contains("additional")) {
                String addFilenameFormat = UtilProperties.getPropertyValue("catalog", "image.filename.additionalviewsize.format");
                addFilenameExpander = FlexibleStringExpander.getInstance(addFilenameFormat);
            }

            /* scale Image for each Size Type */
            Iterator<String> sizeIter = sizeTypeList.iterator();
            while (sizeIter.hasNext()) {
                String sizeType = sizeIter.next();

                resultScaleImgMap.putAll(ImageTransform.scaleImage(bufImg, imgHeight, imgWidth, imgPropertyMap, sizeType, locale));

                if (resultScaleImgMap.containsKey("responseMessage") && resultScaleImgMap.get("responseMessage").equals("success")) {
                    bufNewImg = (BufferedImage) resultScaleImgMap.get("bufferedImage");

                    // write the New Scaled Image
                    String newFileLocation = null;
                    if (viewType.toLowerCase().contains("main")) {
                        newFileLocation = mainFilenameExpander.expandString(UtilMisc.toMap("location", "products", "type", sizeType, "id", id));
                    } else if (viewType.toLowerCase().contains("additional")) {
                        newFileLocation = addFilenameExpander.expandString(UtilMisc.toMap("location", "products", "viewtype", viewType, "sizetype", sizeType,"id", id));
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
                        ImageIO.write((RenderedImage) bufNewImg, imgExtension, new File(imageServerPath + "/" + newFilePathPrefix + filenameToUse));
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
