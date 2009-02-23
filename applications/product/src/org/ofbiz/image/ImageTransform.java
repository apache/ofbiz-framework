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
package org.ofbiz.image;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ImagingOpException;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.io.File;
import java.io.IOException;
import java.lang.IllegalArgumentException;
import java.lang.Double;
import java.lang.Object;
import java.lang.String;
import java.lang.System;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.filter.Filter;
import org.jdom.JDOMException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.service.ServiceUtil;


/**
 * ImageTransform Class
 * <p>
 * Services to apply tranformation for images
 */
public class ImageTransform {

    public static final String module = ImageTransform.class.getName();
//    public static final String err_resource = "ContentErrorUiLabels";

    public ImageTransform() {}
    

    /**
     * getBufferedImage
     * <p>
     * Set a buffered image
     *
     * @param   context
     * @param   fileLocation    Full file Path or Url
     * @return                  Url images for all different size types
     * @throws  IOException Error prevents the document from being fully parsed
     * @throws  JDOMException Errors occur in parsing
     */
    public Map getBufferedImage(String fileLocation)
        throws IllegalArgumentException, IOException {
        
        /* VARIABLES */
        BufferedImage bufImg;
        FastMap result = new FastMap();
        
        /* BUFFERED IMAGE */
        try{
            bufImg = ImageIO.read(new File(fileLocation));
        }catch(IllegalArgumentException e){
            String errMsg = "Input is null : " + fileLocation + " ; " + e.toString();
            Debug.logError(errMsg, module);
            result.put("errorMessage", errMsg);
            return result;
        }catch(IOException e){
            String errMsg = "Error occurs during reading : " + fileLocation + " ; " + e.toString();
            Debug.logError(errMsg, module);
            result.put("errorMessage", errMsg);
            return result;
        }

        result.put("responseMessage", "success");
        result.put("bufferedImage", bufImg); 
        return result;        
    
    } // getBufferedImage
    
    
    /**
     * scaleImageInAllSize
     * <p>
     * Scale the original image into all different size Types (small, medium, large, detail)
     *
     * @param   context                     Context
     * @param   filenameToUse               Filename of future image files
     * @param   viewType                    "Main" view or "additional" view
     * @param   viewNumber                  If it's the main view, viewNumber = "0"
     * @return                              Url images for all different size types
     * @throws  IllegalArgumentException    Any parameter is null
     * @throws  ImagingOpException          The transform is non-invertible
     * @throws  IOException                 Error prevents the document from being fully parsed
     * @throws  JDOMException               Errors occur in parsing
     */
    public Map scaleImageInAllSize(Map<String, ? extends Object> context, String filenameToUse, String viewType, String viewNumber)
        throws IllegalArgumentException, ImagingOpException, IOException, JDOMException {
    
        /* VARIABLES */
        List<String> sizeTypeList = UtilMisc.toList("small", "medium", "large", "detail");
        List<String> extensionList = UtilMisc.toList("jpeg", "jpg", "png");
        int index;
        LinkedHashMap<String, LinkedHashMap<String, String>> imgPropertyMap = new LinkedHashMap<String, LinkedHashMap<String, String>>();
        BufferedImage bufImg, bufNewImg;
        double imgHeight, imgWidth, defaultHeight, defaultWidth, scaleFactor;
        AffineTransformOp op;
        LinkedHashMap<String, String> imgUrlMap = new LinkedHashMap<String, String>(); 
        LinkedHashMap resultXMLMap = new LinkedHashMap(); 
        LinkedHashMap resultBufImgMap = new LinkedHashMap();
        LinkedHashMap resultScaleImgMap = new LinkedHashMap();
        FastMap result = new FastMap();
           
        /* ImageProperties.xml */
        String imgPropertyFullPath = System.getProperty("ofbiz.home") + "/applications/product/config/ImageProperties.xml";
        resultXMLMap.putAll(getXMLValue(imgPropertyFullPath));
        if(resultXMLMap.containsKey("responseMessage") && resultXMLMap.get("responseMessage").equals("success")){
            imgPropertyMap.putAll((LinkedHashMap) resultXMLMap.get("xml"));
        }else{
            String errMsg = "Impossible to parse ImageProperties.xml ";
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
        
        String id = new String();
        String type = new String();
        if(viewType.toLowerCase().contains("main")){
            type = "original";
            id = imgName;
        }else if(viewType.toLowerCase().contains("additional") && viewNumber != null && !viewNumber.equals("0")){
            type = "additional";
            id = imgName + "_View_" + viewNumber;
        }else{
            return ServiceUtil.returnError("View Type : " + type + " is wrong");
        }
        FlexibleStringExpander mainFilenameExpander = FlexibleStringExpander.getInstance(mainFilenameFormat);
        String fileLocation = mainFilenameExpander.expandString(UtilMisc.toMap("location", "products", "type", type, "id", filenameToUse));
        String filePathPrefix = "";
        if (fileLocation.lastIndexOf("/") != -1) {
            filePathPrefix = fileLocation.substring(0, fileLocation.lastIndexOf("/") + 1); // adding 1 to include the trailing slash
        }
        
        
      
        /* get original BUFFERED IMAGE */
        resultBufImgMap.putAll(this.getBufferedImage(imageServerPath + "/" + filePathPrefix + filenameToUse));
        
        if(resultBufImgMap.containsKey("responseMessage") && resultBufImgMap.get("responseMessage").equals("success")){
            bufImg = (BufferedImage) resultBufImgMap.get("bufferedImage");
            
            // get Dimensions    
            imgHeight = (double) bufImg.getHeight();
            imgWidth = (double) bufImg.getWidth();
            if(imgHeight == 0.0 || imgWidth == 0.0){
                String errMsg = "Any current image dimension is null : imgHeight = " + imgHeight + " ; imgWidth = " + imgWidth + ";";
                Debug.logError(errMsg, module);
                result.put("errorMessage", errMsg);
                return result;
            }
            
            // new Filename Format
            FlexibleStringExpander addFilenameExpander = mainFilenameExpander;
            if(viewType.toLowerCase().contains("additional")){
                String addFilenameFormat = UtilProperties.getPropertyValue("catalog", "image.filename.additionalviewsize.format");
                addFilenameExpander = FlexibleStringExpander.getInstance(addFilenameFormat);
            }
        
            /* scale Image for each Size Type */
            Iterator<String> sizeIter = sizeTypeList.iterator();
            while(sizeIter.hasNext()){
                String sizeType = sizeIter.next();
        
                resultScaleImgMap.putAll(this.scaleImage(bufImg, imgHeight, imgWidth, imgPropertyMap, sizeType));
                 
                if(resultScaleImgMap.containsKey("responseMessage") && resultScaleImgMap.get("responseMessage").equals("success")){
                    bufNewImg = (BufferedImage) resultScaleImgMap.get("bufferedImage"); 
                    Double scaleFactorDb = (Double) resultScaleImgMap.get("scaleFactor"); 
                    scaleFactor = scaleFactorDb.doubleValue();
        
                    // define Interpolation
                    LinkedHashMap<RenderingHints.Key, Object> rhMap = new LinkedHashMap<RenderingHints.Key, Object>();
                        rhMap.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                        rhMap.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        rhMap.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
                        rhMap.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
                        rhMap.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                        rhMap.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                        rhMap.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                        //rhMap.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                        rhMap.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    RenderingHints rh = new RenderingHints(rhMap);
                    
                    /* IMAGE TRANFORMATION */ 
                    AffineTransform tx = new AffineTransform();
                    tx.scale(scaleFactor, scaleFactor);
                    
                    
                    try{
                        op = new AffineTransformOp(tx, rh);
                    }catch(ImagingOpException e){
                        String errMsg = "The transform is non-invertible" + e.toString();
                        Debug.logError(errMsg, module);
                        result.put("errorMessage", errMsg);
                        return result;
                    }
        
                    // write the New Scaled Image
                    String newFileLocation = new String();
                    if(viewType.toLowerCase().contains("main")){
                        newFileLocation = mainFilenameExpander.expandString(UtilMisc.toMap("location", "products", "type", sizeType, "id", id));
                    }else if(viewType.toLowerCase().contains("additional")){
                        newFileLocation = addFilenameExpander.expandString(UtilMisc.toMap("location", "products", "viewtype", viewType, "sizetype", sizeType,"id", id));
                    }
                    String newFilePathPrefix = "";
                    if (newFileLocation.lastIndexOf("/") != -1) {
                        newFilePathPrefix = newFileLocation.substring(0, newFileLocation.lastIndexOf("/") + 1); // adding 1 to include the trailing slash
                    }
                    
                    // choose final extension
                    String finalExtension = new String();
                    if(!extensionList.contains(imgExtension.toLowerCase())){
                        finalExtension = imgPropertyMap.get("format").get("extension");
                    }else{
                        finalExtension = imgExtension;
                    }

                    String targetDirectory = imageServerPath + "/" + newFilePathPrefix;
                    File targetDir = new File(targetDirectory);
                    if (!targetDir.exists()) {
                        boolean created = targetDir.mkdirs();
                        if (!created) {
                            Debug.logFatal("Unable to create target directory - " + targetDirectory, module);
                            return ServiceUtil.returnError("Unable to create target directory - " + targetDirectory);
                        }
                    }

                    // write new image
                    try{
                        ImageIO.write(op.filter(bufImg, bufNewImg), imgExtension, new File(imageServerPath + "/" + newFilePathPrefix + filenameToUse));
                    }catch(IllegalArgumentException e){
                        String errMsg = "Any parameter is null" + e.toString();
                        Debug.logError(errMsg, module);
                        result.put("errorMessage", errMsg);
                        return result;
                    }catch(IOException e){
                        String errMsg = "An error occurs during writing" + e.toString();
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
        
        }else{
            String errMsg = "Impossible to scale original image : " + filenameToUse;
            Debug.logError(errMsg, module);
            result.put("errorMessage", errMsg);
            return ServiceUtil.returnError(errMsg);
        }    
        
    } // scaleImageInAllSize 

    
    /**
     * scaleImage
     * <p>
     * scale original image related to the ImageProperties.xml dimensions
     *
     * @param   bufImg          Buffered image to scale
     * @param   imgHeight       Original image height
     * @param   imgwidth        Original image width
     * @param   dimensionMap    Image dimensions by size type
     * @param   sizeType        Size type to scale
     * @return                  New scaled buffered image 
     */
    private Map scaleImage(BufferedImage bufImg, double imgHeight, double imgWidth, LinkedHashMap<String, LinkedHashMap<String, String>> dimensionMap, String sizeType){
    
        /* VARIABLES */
        BufferedImage bufNewImg;
        double defaultHeight, defaultWidth, scaleFactor;
        FastMap result = new FastMap();
       
        /* DIMENSIONS from ImageProperties */     
        defaultHeight = Double.parseDouble(dimensionMap.get(sizeType).get("height").toString());
        defaultWidth = Double.parseDouble(dimensionMap.get(sizeType).get("width").toString());
        if(defaultHeight == 0.0 || defaultWidth == 0.0){
            String errMsg = "Any default dimension is null : defaultHeight = " + defaultHeight + " ; defaultWidth = " + defaultWidth + ";";
            Debug.logError(errMsg, module);
            result.put("errorMessage", errMsg);
            return result;
        }
        
        /* SCALE FACTOR */
        // find the right Scale Factor related to the Image Dimensions
        if(imgHeight > imgWidth){
            scaleFactor = defaultHeight / imgHeight;
            if(scaleFactor == 0.0){
                String errMsg = "Height scaleFactor is null (defaultHeight = " + defaultHeight + "; imgHeight = " + imgHeight + ";";
                Debug.logError(errMsg, module);
                result.put("errorMessage", errMsg);
                return result;
            }
            // get scaleFactor from the smallest width
            if(defaultWidth < (imgWidth * scaleFactor)){
                scaleFactor = defaultWidth / imgWidth;    
            }
        }else{
            scaleFactor = defaultWidth / imgWidth;
            if(scaleFactor == 0.0){
                String errMsg = "Width scaleFactor is null (defaultWidth = " + defaultWidth + "; imgWidth = " + imgWidth + ";";
                Debug.logError(errMsg, module);
                result.put("errorMessage", errMsg);
                return result;
            }
            // get scaleFactor from the smallest height
            if(defaultHeight < (imgHeight * scaleFactor)){
                scaleFactor = defaultHeight / imgHeight;    
            }   
        }
            
        if(scaleFactor == 0.0){
            String errMsg = "Final scaleFactor is null = " + scaleFactor + ";";
            Debug.logError(errMsg, module);
            result.put("errorMessage", errMsg);
            return result;
        }
        
        bufNewImg = new BufferedImage( (int) (imgWidth * scaleFactor), (int) (imgHeight * scaleFactor), bufImg.getType());
 
        result.put("responseMessage", "success");
        result.put("bufferedImage", bufNewImg);
        result.put("scaleFactor", scaleFactor);
        return result;

    } // scaleImage
    
    
    /**
     * getXMLValue
     * <p>
     * From a XML element, get a values map
     *
     * @param fileFullPath      File path to parse
     * @return Map contains asked attribute values by attribute name
     */
    private Map getXMLValue(String fileFullPath)
        throws IllegalStateException, IOException, JDOMException {

        /* VARIABLES */
        Document document;
        Element rootElt;
        List<Element> eltList;
        LinkedHashMap<String, LinkedHashMap> valueMap = new LinkedHashMap<String, LinkedHashMap>();
        FastMap result = new FastMap();
        
        /* PARSING */
        SAXBuilder sxb = new SAXBuilder();
        try{
            // JDOM
            document = sxb.build(new File(fileFullPath));
        }catch(JDOMException e){
            String errMsg = "Errors occur in parsing ImageProperties.xml" + e.toString();
            Debug.logError(errMsg, module);
            result.put("errorMessage", "error");
            return result;
        }catch(IOException e){
            String errMsg = "Error prevents the document from being fully parsed" + e.toString();
            Debug.logError(errMsg, module);
            result.put("errorMessage", "error");
            return result;
        }
        // set Root Element
        try{
            rootElt = document.getRootElement();
        }catch(IllegalStateException e){
            String errMsg = "Root element hasn't been set" + e.toString();
            Debug.logError(errMsg, module);
            result.put("errorMessage", "error");
            return result;
        } 
           
        /* get NAME and VALUE */
        Iterator<Element> eltIter = rootElt.getChildren().iterator();
        while(eltIter.hasNext()){
            Element currentElt = eltIter.next();              
            LinkedHashMap<String, String> eltMap = new LinkedHashMap<String, String>();
            if(currentElt.getContentSize() > 0){
                LinkedHashMap<String, String> childMap = new LinkedHashMap<String, String>();
                // loop over Children 1st level
                Iterator<Element> childrenIter = currentElt.getChildren().iterator();
                while(childrenIter.hasNext()){
                    Element currentChild = childrenIter.next(); 
                    childMap.put(currentChild.getAttributeValue("name"), currentChild.getAttributeValue("value"));  
                }    
                valueMap.put(currentElt.getAttributeValue("name"), childMap);
            }else{
                eltMap.put(currentElt.getAttributeValue("name"), currentElt.getAttributeValue("value"));
                valueMap.put(currentElt.getName(), eltMap);   
            }
        } // eltIter  
        
        result.put("responseMessage", "success");
        result.put("xml", valueMap);
        return result; 
        
    } // getXMLValue
    
} // ImageTransform Class
