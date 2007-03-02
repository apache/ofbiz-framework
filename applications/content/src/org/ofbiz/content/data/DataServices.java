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
package org.ofbiz.content.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.StringWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.ByteWrapper;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

/**
 * DataServices Class
 */
public class DataServices {

    public static final String module = DataServices.class.getName();

    /**
     * A top-level service for creating a DataResource and ElectronicText together.
     */
    public static Map createDataResourceAndText(DispatchContext dctx, Map context) {
        Map result = new HashMap();

            Map thisResult = createDataResourceMethod(dctx, context);
            if (thisResult.get(ModelService.RESPONSE_MESSAGE) != null) {
                return ServiceUtil.returnError((String) thisResult.get(ModelService.ERROR_MESSAGE));
            }

            result.put("dataResourceId", thisResult.get("dataResourceId"));
            context.put("dataResourceId", thisResult.get("dataResourceId"));

            String dataResourceTypeId = (String) context.get("dataResourceTypeId");
            if (dataResourceTypeId != null && dataResourceTypeId.equals("ELECTRONIC_TEXT")) {
                thisResult = createElectronicText(dctx, context);
                if (thisResult.get(ModelService.RESPONSE_MESSAGE) != null) {
                    return ServiceUtil.returnError((String) thisResult.get(ModelService.ERROR_MESSAGE));
                }
            }

        return result;
    }

    /**
     * A service wrapper for the createDataResourceMethod method. Forces permissions to be checked.
     */
    public static Map createDataResource(DispatchContext dctx, Map context) {
        Map result = createDataResourceMethod(dctx, context);
        return result;
    }

    public static Map createDataResourceMethod(DispatchContext dctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = dctx.getDelegator();
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            String userLoginId = (String) userLogin.get("userLoginId");
            String createdByUserLogin = userLoginId;
            String lastModifiedByUserLogin = userLoginId;
            Timestamp createdDate = UtilDateTime.nowTimestamp();
            Timestamp lastModifiedDate = UtilDateTime.nowTimestamp();
            String dataTemplateTypeId = (String) context.get("dataTemplateTypeId");
            if (UtilValidate.isEmpty(dataTemplateTypeId)) {
                dataTemplateTypeId = "NONE";
                context.put("dataTemplateTypeId", dataTemplateTypeId );
            }

            // If textData exists, then create DataResource and return dataResourceId
            String dataResourceId = (String) context.get("dataResourceId");
            if (UtilValidate.isEmpty(dataResourceId))
                dataResourceId = delegator.getNextSeqId("DataResource");
            if (Debug.infoOn()) Debug.logInfo("in createDataResourceMethod, dataResourceId:" + dataResourceId, module);
            GenericValue dataResource = delegator.makeValue("DataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
            dataResource.setNonPKFields(context);
            dataResource.put("createdByUserLogin", createdByUserLogin);
            dataResource.put("lastModifiedByUserLogin", lastModifiedByUserLogin);
            dataResource.put("createdDate", createdDate);
            dataResource.put("lastModifiedDate", lastModifiedDate);
            // get first statusId  for content out of the statusItem table if not provided 
            if (UtilValidate.isEmpty(dataResource.get("statusId"))) {
                try {
                    List statusItems = delegator.findByAnd("StatusItem",UtilMisc.toMap("statusTypeId", "CONTENT_STATUS"), UtilMisc.toList("sequenceId"));
                    if (!UtilValidate.isEmpty(statusItems)) {
                        dataResource.put("statusId",  ((GenericValue) statusItems.get(0)).getString("statusId")); 
                    }
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(e.getMessage());
                }
            }
            
            try {
                dataResource.create();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.getMessage());
            } catch(Exception e2) {
                return ServiceUtil.returnError(e2.getMessage());
            }
            result.put("dataResourceId", dataResourceId);
            result.put("dataResource", dataResource);
        return result;
    }

    /**
     * A service wrapper for the createElectronicTextMethod method. Forces permissions to be checked.
     */
    public static Map createElectronicText(DispatchContext dctx, Map context) {
        Map result = createElectronicTextMethod(dctx, context);
        return result;
    }

    public static Map createElectronicTextMethod(DispatchContext dctx, Map context) {
        HashMap result = new HashMap();
        GenericDelegator delegator = dctx.getDelegator();
            String dataResourceId = (String) context.get("dataResourceId");
            String textData = (String) context.get("textData");
            if (textData != null && textData.length() > 0) {
                GenericValue electronicText = delegator.makeValue("ElectronicText", UtilMisc.toMap("dataResourceId", dataResourceId, "textData", textData));
                try {
                    electronicText.create();
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(e.getMessage());
                }
            }
        

        return result;
    }

    /**
     * A service wrapper for the createFileMethod method. Forces permissions to be checked.
     */
    public static Map createFile(DispatchContext dctx, Map context) {

        return createFileMethod(dctx, context);
    }

    public static Map createFileNoPerm(DispatchContext dctx, Map context) {
        context.put("skipPermissionCheck", "true");        
        return createFileMethod(dctx, context);
    }

    public static Map createFileMethod(DispatchContext dctx, Map context) {
            //GenericValue dataResource = (GenericValue) context.get("dataResource");
            String dataResourceTypeId = (String) context.get("dataResourceTypeId");
            String objectInfo = (String) context.get("objectInfo");
            ByteWrapper binData = (ByteWrapper) context.get("binData");
            String textData = (String) context.get("textData");

            // a few place holders
            String prefix = "";
            String sep = "";

            // extended validation for binary/character data
            if (UtilValidate.isNotEmpty(textData) && binData != null) {
                return ServiceUtil.returnError("Cannot process both character and binary data in the same file");
            }

            // obtain a reference to the file
            File file = null;
            if (UtilValidate.isEmpty(dataResourceTypeId) || dataResourceTypeId.equals("LOCAL_FILE")) {
                file = new File(objectInfo);
                if (!file.isAbsolute()) {
                    return ServiceUtil.returnError("DataResource LOCAL_FILE does not point to an absolute location");
                }
            } else if (dataResourceTypeId.equals("OFBIZ_FILE")) {
                prefix = System.getProperty("ofbiz.home");
                if (objectInfo.indexOf("/") != 0 && prefix.lastIndexOf("/") != (prefix.length() - 1)) {
                    sep = "/";
                }
                file = new File(prefix + sep + objectInfo);
            } else if (dataResourceTypeId.equals("CONTEXT_FILE")) {
                prefix = (String) context.get("rootDir");
                if (objectInfo.indexOf("/") != 0 && prefix.lastIndexOf("/") != (prefix.length() - 1)) {
                    sep = "/";
                }
                file = new File(prefix + sep + objectInfo);
            }
            if (file == null) {
                return ServiceUtil.returnError("Unable to obtain a reference to file - " + objectInfo);
            }

            // write the data to the file
            if (UtilValidate.isNotEmpty(textData)) {
                try {
                    FileWriter out = new FileWriter(file);
                    out.write(textData);
                    out.close();
                } catch (IOException e) {
                    Debug.logWarning(e, module);
                    return ServiceUtil.returnError("Unable to write character data to: " + file.getAbsolutePath());
                }
            } else if (binData != null) {
                try {
                    RandomAccessFile out = new RandomAccessFile(file, "rw");
                    out.write(binData.getBytes());
                    out.close();
                } catch (FileNotFoundException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError("Unable to open file for writing: " + file.getAbsolutePath());
                } catch (IOException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError("Unable to write binary data to: " + file.getAbsolutePath());
                }
            } else {
                return ServiceUtil.returnError("No file content passed for: " + file.getAbsolutePath());
            }

            Map result = ServiceUtil.returnSuccess();
            return result;
    }

    /**
     * A top-level service for updating a DataResource and ElectronicText together.
     */
    public static Map updateDataResourceAndText(DispatchContext dctx, Map context) {
        Map thisResult = updateDataResourceMethod(dctx, context);
        if (thisResult.get(ModelService.RESPONSE_MESSAGE) != null) {
            return ServiceUtil.returnError((String) thisResult.get(ModelService.ERROR_MESSAGE));
        }
        String dataResourceTypeId = (String) context.get("dataResourceTypeId");
        if (dataResourceTypeId != null && dataResourceTypeId.equals("ELECTRONIC_TEXT")) {
            thisResult = updateElectronicText(dctx, context);
            if (thisResult.get(ModelService.RESPONSE_MESSAGE) != null) {
                return ServiceUtil.returnError((String) thisResult.get(ModelService.ERROR_MESSAGE));
            }
        }
        return ServiceUtil.returnSuccess();
    }



    /**
     * A service wrapper for the updateDataResourceMethod method. Forces permissions to be checked.
     */
    public static Map updateDataResource(DispatchContext dctx, Map context) {
        //context.put("skipPermissionCheck", null);
        Map result = updateDataResourceMethod(dctx, context);
        return result;
    }

    public static Map updateDataResourceMethod(DispatchContext dctx, Map context) {

        Map result = new HashMap();
        GenericDelegator delegator = dctx.getDelegator();
        GenericValue dataResource = null;
        //Locale locale = (Locale) context.get("locale");
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            String userLoginId = (String) userLogin.get("userLoginId");
            String lastModifiedByUserLogin = userLoginId;
            Timestamp lastModifiedDate = UtilDateTime.nowTimestamp();

            // If textData exists, then create DataResource and return dataResourceId
            String dataResourceId = (String) context.get("dataResourceId");
            try {
                dataResource = delegator.findByPrimaryKey("DataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
                return ServiceUtil.returnError("dataResource.update.read_failure" + e.getMessage());
            }

            dataResource.setNonPKFields(context);
            dataResource.put("lastModifiedByUserLogin", lastModifiedByUserLogin);
            dataResource.put("lastModifiedDate", lastModifiedDate);

            try {
                dataResource.store();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        
        result.put("dataResource", dataResource);
        return result;
    }

    /**
     * A service wrapper for the updateElectronicTextMethod method. Forces permissions to be checked.
     */
    public static Map updateElectronicText(DispatchContext dctx, Map context) {
        Map result = updateElectronicTextMethod(dctx, context);
        return result;
    }

    /**
     * Because sometimes a DataResource will exist, but no ElectronicText has been created,
     * this method will create an ElectronicText if it does not exist.
     * @param dctx
     * @param context
     * @return
     */
    public static Map updateElectronicTextMethod(DispatchContext dctx, Map context) {
        HashMap result = new HashMap();
        GenericDelegator delegator = dctx.getDelegator();
        GenericValue electronicText = null;
        //Locale locale = (Locale) context.get("locale");
        String dataResourceId = (String) context.get("dataResourceId");
        result.put("dataResourceId",dataResourceId);
        String contentId = (String) context.get("contentId");
        result.put("contentId",contentId);
            if (UtilValidate.isEmpty(dataResourceId)) {
                    String errMsg = "dataResourceId is null.";
                    Debug.logError(errMsg, module);
                    return ServiceUtil.returnError(errMsg);
                
            }
            String textData = (String) context.get("textData");
            if (Debug.verboseOn()) Debug.logVerbose("in updateElectronicText, textData:" + textData, module);
                try {
                    electronicText = delegator.findByPrimaryKey("ElectronicText", UtilMisc.toMap("dataResourceId", dataResourceId));
                    if (electronicText != null) {
                        electronicText.put("textData", textData);
                        electronicText.store();
                    } else {
                            electronicText = delegator.makeValue("ElectronicText", null);
                            electronicText.put("dataResourceId", dataResourceId);
                            electronicText.put("textData", textData);
                            electronicText.create();
                    }
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, module);
                    return ServiceUtil.returnError("electronicText.update.read_failure" + e.getMessage());
                }

        return result;
    }

    /**
     * A service wrapper for the updateFileMethod method. Forces permissions to be checked.
     */
    public static Map updateFile(DispatchContext dctx, Map context) {
        Map result = null;
        try {
            result = updateFileMethod(dctx, context);
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map updateFileMethod(DispatchContext dctx, Map context) throws GenericServiceException {
        HashMap result = new HashMap();
        //GenericValue fileText = null;
        //Locale locale = (Locale) context.get("locale");
            //String dataResourceId = (String) dataResource.get("dataResourceId");
            String dataResourceTypeId = (String) context.get("dataResourceTypeId");
            String objectInfo = (String) context.get("objectInfo");
            String textData = (String) context.get("textData");
            ByteWrapper binData = (ByteWrapper) context.get("binData");
            String prefix = "";
            File file = null;
            String fileName = "";
            String sep = "";
            try {
                if (UtilValidate.isEmpty(dataResourceTypeId) || dataResourceTypeId.startsWith("LOCAL_FILE")) {
                    fileName = prefix + sep + objectInfo;
                    file = new File(fileName);
                    if (file == null) {
                        throw new GenericServiceException("File: " + fileName + " is null.");
                    }
                    if (!file.isAbsolute()) {
                        throw new GenericServiceException("File: " + fileName + " is not absolute.");
                    }
                } else if (dataResourceTypeId.startsWith("OFBIZ_FILE")) {
                    prefix = System.getProperty("ofbiz.home");
                    if (objectInfo.indexOf("/") != 0 && prefix.lastIndexOf("/") != (prefix.length() - 1)) {
                        sep = "/";
                    }
                    file = new File(prefix + sep + objectInfo);
                } else if (dataResourceTypeId.startsWith("CONTEXT_FILE")) {
                    prefix = (String) context.get("rootDir");
                    if (objectInfo.indexOf("/") != 0 && prefix.lastIndexOf("/") != (prefix.length() - 1)) {
                        sep = "/";
                    }
                    file = new File(prefix + sep + objectInfo);
                }
                if (file == null) {
                    throw new IOException("File: " + file + " is null");
                }
            
            // write the data to the file
            if (UtilValidate.isNotEmpty(textData)) {
                try {
                    FileWriter out = new FileWriter(file);
                    out.write(textData);
                    out.close();
                } catch (IOException e) {
                    Debug.logWarning(e, module);
                    return ServiceUtil.returnError("Unable to write character data to: " + file.getAbsolutePath());
                }
            } else if (binData != null) {
                try {
                    RandomAccessFile out = new RandomAccessFile(file, "rw");
                    out.write(binData.getBytes());
                    out.close();
                } catch (FileNotFoundException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError("Unable to open file for writing: " + file.getAbsolutePath());
                } catch (IOException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError("Unable to write binary data to: " + file.getAbsolutePath());
                }
            } else {
                return ServiceUtil.returnError("No file content passed for: " + file.getAbsolutePath());
            }

            } catch (IOException e) {
                Debug.logWarning(e, module);
                throw new GenericServiceException(e.getMessage());
            }

        return result;
    }

    public static Map renderDataResourceAsText(DispatchContext dctx, Map context) throws GeneralException, IOException {
        Map results = new HashMap();
        GenericDelegator delegator = dctx.getDelegator();
        //LocalDispatcher dispatcher = dctx.getDispatcher();
        Writer out = (Writer) context.get("outWriter");
        Map templateContext = (Map) context.get("templateContext");
        //GenericValue userLogin = (GenericValue) context.get("userLogin");
        String dataResourceId = (String) context.get("dataResourceId");
        if (templateContext != null && UtilValidate.isEmpty(dataResourceId)) {
            dataResourceId = (String) templateContext.get("dataResourceId");
        }
        String mimeTypeId = (String) context.get("mimeTypeId");
        if (templateContext != null && UtilValidate.isEmpty(mimeTypeId)) {
            mimeTypeId = (String) templateContext.get("mimeTypeId");
        }

        Locale locale = (Locale) context.get("locale");

        if (templateContext == null) {
            templateContext = new HashMap();
        }

        GenericValue view = (GenericValue) context.get("subContentDataResourceView");
        Writer outWriter = new StringWriter();
        DataResourceWorker.renderDataResourceAsText(delegator, dataResourceId, outWriter, templateContext, locale, mimeTypeId, true);
        try {
            out.write(outWriter.toString());
            results.put("textData", outWriter.toString());
        } catch (IOException e) {
            Debug.logError(e, "Error rendering sub-content text", module);
            return ServiceUtil.returnError(e.toString());
        }
        return results;
    }

    /**
     * A service wrapper for the updateImageMethod method. Forces permissions to be checked.
     */
    public static Map updateImage(DispatchContext dctx, Map context) {
        Map result = updateImageMethod(dctx, context);
        return result;
    }

    public static Map updateImageMethod(DispatchContext dctx, Map context) {
        HashMap result = new HashMap();
        GenericDelegator delegator = dctx.getDelegator();
        //Locale locale = (Locale) context.get("locale");
            String dataResourceId = (String) context.get("dataResourceId");
            ByteWrapper byteWrapper = (ByteWrapper)context.get("imageData");
            if (byteWrapper != null) {
                byte[] imageBytes = byteWrapper.getBytes();
                try {
                    GenericValue imageDataResource = delegator.findByPrimaryKey("ImageDataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
                    if (Debug.infoOn()) Debug.logInfo("imageDataResource(U):" + imageDataResource, module);
                    if (Debug.infoOn()) Debug.logInfo("imageBytes(U):" + imageBytes, module);
                    if (imageDataResource == null) {
                        return createImageMethod(dctx, context);
                    } else {
                        imageDataResource.setBytes("imageData", imageBytes);
                        imageDataResource.store();
                    }
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(e.getMessage());
                }
            }

        return result;
    }

    /**
     * A service wrapper for the createImageMethod method. Forces permissions to be checked.
     */
    public static Map createImage(DispatchContext dctx, Map context) {
        Map result = createImageMethod(dctx, context);
        return result;
    }

    public static Map createImageMethod(DispatchContext dctx, Map context) {
        HashMap result = new HashMap();
        GenericDelegator delegator = dctx.getDelegator();
            String dataResourceId = (String) context.get("dataResourceId");
            ByteWrapper byteWrapper = (ByteWrapper)context.get("imageData");
            if (byteWrapper != null) {
                byte[] imageBytes = byteWrapper.getBytes();
                try {
                    GenericValue imageDataResource = delegator.makeValue("ImageDataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
                    //imageDataResource.set("imageData", imageBytes);
                    imageDataResource.setBytes("imageData", imageBytes);
                    if (Debug.infoOn()) Debug.logInfo("imageDataResource(C):" + imageDataResource, module);
                    imageDataResource.create();
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(e.getMessage());
                }
            }

        return result;
    }

    /**
     * A service wrapper for the createBinaryFileMethod method. Forces permissions to be checked.
     */
    public static Map createBinaryFile(DispatchContext dctx, Map context) {
        Map result = null;
        try {
            result = createBinaryFileMethod(dctx, context);
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map createBinaryFileMethod(DispatchContext dctx, Map context) throws GenericServiceException {
        HashMap result = new HashMap();
            GenericValue dataResource = (GenericValue) context.get("dataResource");
            //String dataResourceId = (String) dataResource.get("dataResourceId");
            String dataResourceTypeId = (String) dataResource.get("dataResourceTypeId");
            String objectInfo = (String) dataResource.get("objectInfo");
            byte [] imageData = (byte []) context.get("imageData");
            String rootDir = (String)context.get("rootDir");
            File file = null;
            if (Debug.infoOn()) Debug.logInfo("in createBinaryFileMethod, dataResourceTypeId:" + dataResourceTypeId, module);
            if (Debug.infoOn()) Debug.logInfo("in createBinaryFileMethod, objectInfo:" + objectInfo, module);
            if (Debug.infoOn()) Debug.logInfo("in createBinaryFileMethod, rootDir:" + rootDir, module);
            try {
                file = DataResourceWorker.getContentFile(dataResourceTypeId, objectInfo, rootDir);
            } catch (FileNotFoundException e) {
                    Debug.logWarning(e, module);
                    throw new GenericServiceException(e.getMessage());
            } catch (GeneralException e2) {
                    Debug.logWarning(e2, module);
                    throw new GenericServiceException(e2.getMessage());
            }
        if (Debug.infoOn()) Debug.logInfo("in createBinaryFileMethod, file:" + file, module);
        if (Debug.infoOn()) Debug.logInfo("in createBinaryFileMethod, imageData:" + imageData.length, module);
            if (imageData != null && imageData.length > 0) {
                try {
                    FileOutputStream out = new FileOutputStream(file);
                    out.write(imageData);
                    if (Debug.infoOn()) Debug.logInfo("in createBinaryFileMethod, length:" + file.length(), module);
                    out.close();
                } catch (IOException e) {
                    Debug.logWarning(e, module);
                    throw new GenericServiceException(e.getMessage());
                }
            }

        return result;
    }


    /**
     * A service wrapper for the createBinaryFileMethod method. Forces permissions to be checked.
     */
    public static Map updateBinaryFile(DispatchContext dctx, Map context) {
        Map result = null;
        try {
            result = updateBinaryFileMethod(dctx, context);
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map updateBinaryFileMethod(DispatchContext dctx, Map context) throws GenericServiceException {
        HashMap result = new HashMap();
            GenericValue dataResource = (GenericValue) context.get("dataResource");
            //String dataResourceId = (String) dataResource.get("dataResourceId");
            String dataResourceTypeId = (String) dataResource.get("dataResourceTypeId");
            String objectInfo = (String) dataResource.get("objectInfo");
            byte [] imageData = (byte []) context.get("imageData");
            String rootDir = (String)context.get("rootDir");
            File file = null;
            if (Debug.infoOn()) Debug.logInfo("in updateBinaryFileMethod, dataResourceTypeId:" + dataResourceTypeId, module);
            if (Debug.infoOn()) Debug.logInfo("in updateBinaryFileMethod, objectInfo:" + objectInfo, module);
            if (Debug.infoOn()) Debug.logInfo("in updateBinaryFileMethod, rootDir:" + rootDir, module);
            try {
                file = DataResourceWorker.getContentFile(dataResourceTypeId, objectInfo, rootDir);
            } catch (FileNotFoundException e) {
                    Debug.logWarning(e, module);
                    throw new GenericServiceException(e.getMessage());
            } catch (GeneralException e2) {
                    Debug.logWarning(e2, module);
                    throw new GenericServiceException(e2.getMessage());
            }
        if (Debug.infoOn()) Debug.logInfo("in updateBinaryFileMethod, file:" + file, module);
        if (Debug.infoOn()) Debug.logInfo("in updateBinaryFileMethod, imageData:" + imageData, module);
            if (imageData != null && imageData.length > 0) {
                try {
                    FileOutputStream out = new FileOutputStream(file);
                    out.write(imageData);
                    if (Debug.infoOn()) Debug.logInfo("in updateBinaryFileMethod, length:" + file.length(), module);
                    out.close();
                } catch (IOException e) {
                    Debug.logWarning(e, module);
                    throw new GenericServiceException(e.getMessage());
                }
            }

        return result;
    }
}
