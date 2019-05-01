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
package org.apache.ofbiz.content.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilIO;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * DataServices Class
 */
public class DataServices {

    public static final String module = DataServices.class.getName();
    public static final String resource = "ContentUiLabels";

    public static Map<String, Object> clearAssociatedRenderCache(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        String dataResourceId = (String) context.get("dataResourceId");
        Locale locale = (Locale) context.get("locale");
        try {
            DataResourceWorker.clearAssociatedRenderCache(delegator, dataResourceId);
        } catch (GeneralException e) {
            Debug.logError(e, "Unable to clear associated render cache with dataResourceId=" + dataResourceId, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentClearAssociatedRenderCacheError", UtilMisc.toMap("dataResourceId", dataResourceId), locale));
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * A top-level service for creating a DataResource and ElectronicText together.
     */
    public static Map<String, Object> createDataResourceAndText(DispatchContext dctx, Map<String, ? extends Object> rcontext) {
        Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> thisResult = createDataResourceMethod(dctx, context);
        if (thisResult.get(ModelService.RESPONSE_MESSAGE) != null) {
            return ServiceUtil.returnError((String) thisResult.get(ModelService.ERROR_MESSAGE));
        }

        result.put("dataResourceId", thisResult.get("dataResourceId"));
        context.put("dataResourceId", thisResult.get("dataResourceId"));

        String dataResourceTypeId = (String) context.get("dataResourceTypeId");
        if (dataResourceTypeId != null && "ELECTRONIC_TEXT".equals(dataResourceTypeId)) {
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
    public static Map<String, Object> createDataResource(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = createDataResourceMethod(dctx, context);
        return result;
    }

    public static Map<String, Object> createDataResourceMethod(DispatchContext dctx, Map<String, ? extends Object> rcontext) {
        Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String userLoginId = (String) userLogin.get("userLoginId");
        String createdByUserLogin = userLoginId;
        String lastModifiedByUserLogin = userLoginId;
        Timestamp createdDate = UtilDateTime.nowTimestamp();
        Timestamp lastModifiedDate = UtilDateTime.nowTimestamp();
        String dataTemplateTypeId = (String) context.get("dataTemplateTypeId");
        if (UtilValidate.isEmpty(dataTemplateTypeId)) {
            dataTemplateTypeId = "NONE";
            context.put("dataTemplateTypeId", dataTemplateTypeId);
        }

        // If textData exists, then create DataResource and return dataResourceId
        String dataResourceId = (String) context.get("dataResourceId");
        if (UtilValidate.isEmpty(dataResourceId)) {
            dataResourceId = delegator.getNextSeqId("DataResource");
        }
        if (Debug.infoOn()) {
            Debug.logInfo("in createDataResourceMethod, dataResourceId:" + dataResourceId, module);
        }
        GenericValue dataResource = delegator.makeValue("DataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
        dataResource.setNonPKFields(context);
        dataResource.put("createdByUserLogin", createdByUserLogin);
        dataResource.put("lastModifiedByUserLogin", lastModifiedByUserLogin);
        dataResource.put("createdDate", createdDate);
        dataResource.put("lastModifiedDate", lastModifiedDate);
        // get first statusId  for content out of the statusItem table if not provided
        if (UtilValidate.isEmpty(dataResource.get("statusId"))) {
            try {
                GenericValue statusItem = EntityQuery.use(delegator).from("StatusItem").where("statusTypeId", "CONTENT_STATUS").orderBy("sequenceId").queryFirst();
                if (statusItem != null) {
                    dataResource.put("statusId",  statusItem.get("statusId"));
                }
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        try {
            dataResource.create();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        result.put("dataResourceId", dataResourceId);
        result.put("dataResource", dataResource);
        return result;
    }

    /**
     * A service wrapper for the createElectronicTextMethod method. Forces permissions to be checked.
     */
    public static Map<String, Object> createElectronicText(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = createElectronicTextMethod(dctx, context);
        return result;
    }

    public static Map<String, Object> createElectronicTextMethod(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        String dataResourceId = (String) context.get("dataResourceId");
        String textData = (String) context.get("textData");
        if (UtilValidate.isNotEmpty(textData)) {
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
    public static Map<String, Object> createFile(DispatchContext dctx, Map<String, ? extends Object> context) {
        return createFileMethod(dctx, context);
    }

    public static Map<String, Object> createFileNoPerm(DispatchContext dctx, Map<String, ? extends Object> rcontext) {
        Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);
        context.put("skipPermissionCheck", "true");
        return createFileMethod(dctx, context);
    }

    public static Map<String, Object> createFileMethod(DispatchContext dctx, Map<String, ? extends Object> context) {
        //GenericValue dataResource = (GenericValue) context.get("dataResource");
        String dataResourceTypeId = (String) context.get("dataResourceTypeId");
        String objectInfo = (String) context.get("objectInfo");
        ByteBuffer binData = (ByteBuffer) context.get("binData");
        String textData = (String) context.get("textData");
        Locale locale = (Locale) context.get("locale");

        // a few place holders
        String prefix = "";
        String sep = "";

        // extended validation for binary/character data
        if (UtilValidate.isNotEmpty(textData) && binData != null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentCannotProcessBothCharacterAndBinaryFile", locale));
        }

        // obtain a reference to the file
        File file = null;
        if (UtilValidate.isEmpty(objectInfo)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentUnableObtainReferenceToFile", UtilMisc.toMap("objectInfo", ""), locale));
        }
        if (UtilValidate.isEmpty(dataResourceTypeId) || "LOCAL_FILE".equals(dataResourceTypeId) || "LOCAL_FILE_BIN".equals(dataResourceTypeId)) {
            file = new File(objectInfo);
            if (!file.isAbsolute()) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentLocalFileDoesNotPointToAbsoluteLocation", locale));
            }
        } else if ("OFBIZ_FILE".equals(dataResourceTypeId) || "OFBIZ_FILE_BIN".equals(dataResourceTypeId)) {
            prefix = System.getProperty("ofbiz.home");
            if (objectInfo.indexOf('/') != 0 && prefix.lastIndexOf('/') != (prefix.length() - 1)) {
                sep = "/";
            }
            file = new File(prefix + sep + objectInfo);
        } else if ("CONTEXT_FILE".equals(dataResourceTypeId) || "CONTEXT_FILE_BIN".equals(dataResourceTypeId)) {
            prefix = (String) context.get("rootDir");
            if (UtilValidate.isEmpty(prefix)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentCannotFindContextFileWithEmptyContextRoot", locale));
            }
            if (objectInfo.indexOf('/') != 0 && prefix.lastIndexOf('/') != (prefix.length() - 1)) {
                sep = "/";
            }
            file = new File(prefix + sep + objectInfo);
        }
        if (file == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentUnableObtainReferenceToFile", UtilMisc.toMap("objectInfo", objectInfo), locale));
        }

        // write the data to the file
        if (UtilValidate.isNotEmpty(textData)) {
            try (
                OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file), UtilIO.getUtf8());
            ) {
                out.write(textData);
            } catch (IOException e) {
                Debug.logWarning(e, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentUnableWriteCharacterDataToFile", UtilMisc.toMap("fileName", file.getAbsolutePath()), locale));
            }
        } else if (binData != null) {
            try {
                RandomAccessFile out = new RandomAccessFile(file, "rw");
                out.write(binData.array());
                out.close();
            } catch (FileNotFoundException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentUnableToOpenFileForWriting", UtilMisc.toMap("fileName", file.getAbsolutePath()), locale));
            } catch (IOException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentUnableWriteBinaryDataToFile", UtilMisc.toMap("fileName", file.getAbsolutePath()), locale));
            }
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentNoContentFilePassed", UtilMisc.toMap("fileName", file.getAbsolutePath()), locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        return result;
    }

    /**
     * A top-level service for updating a DataResource and ElectronicText together.
     */
    public static Map<String, Object> updateDataResourceAndText(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> thisResult = updateDataResourceMethod(dctx, context);
        if (thisResult.get(ModelService.RESPONSE_MESSAGE) != null) {
            return ServiceUtil.returnError((String) thisResult.get(ModelService.ERROR_MESSAGE));
        }
        String dataResourceTypeId = (String) context.get("dataResourceTypeId");
        if (dataResourceTypeId != null && "ELECTRONIC_TEXT".equals(dataResourceTypeId)) {
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
    public static Map<String, Object> updateDataResource(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = updateDataResourceMethod(dctx, context);
        return result;
    }

    public static Map<String, Object> updateDataResourceMethod(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        GenericValue dataResource = null;
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String userLoginId = (String) userLogin.get("userLoginId");
        String lastModifiedByUserLogin = userLoginId;
        Timestamp lastModifiedDate = UtilDateTime.nowTimestamp();

        // If textData exists, then create DataResource and return dataResourceId
        String dataResourceId = (String) context.get("dataResourceId");
        try {
            dataResource = EntityQuery.use(delegator).from("DataResource").where("dataResourceId", dataResourceId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentDataResourceNotFound", UtilMisc.toMap("parameters.dataResourceId", dataResourceId), locale));
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
    public static Map<String, Object> updateElectronicText(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = updateElectronicTextMethod(dctx, context);
        return result;
    }

    /**
     * Because sometimes a DataResource will exist, but no ElectronicText has been created,
     * this method will create an ElectronicText if it does not exist.
     * @param dctx the dispatch context
     * @param context the context
     * @return update the ElectronicText
     */
    public static Map<String, Object> updateElectronicTextMethod(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        GenericValue electronicText = null;
        Locale locale = (Locale) context.get("locale");
        String dataResourceId = (String) context.get("dataResourceId");
        result.put("dataResourceId", dataResourceId);
        String contentId = (String) context.get("contentId");
        result.put("contentId", contentId);
        if (UtilValidate.isEmpty(dataResourceId)) {
            Debug.logError("dataResourceId is null.", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentDataResourceIsNull", locale));
        }
        String textData = (String) context.get("textData");
        if (Debug.verboseOn()) {
            Debug.logVerbose("in updateElectronicText, textData:" + textData, module);
        }
        try {
            electronicText = EntityQuery.use(delegator).from("ElectronicText").where("dataResourceId", dataResourceId).queryOne();
            if (electronicText != null) {
                electronicText.put("textData", textData);
                electronicText.store();
            } else {
                electronicText = delegator.makeValue("ElectronicText");
                electronicText.put("dataResourceId", dataResourceId);
                electronicText.put("textData", textData);
                electronicText.create();
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentElectronicTextNotFound", locale) + " " + e.getMessage());
        }

        return result;
    }

    /**
     * A service wrapper for the updateFileMethod method. Forces permissions to be checked.
     */
    public static Map<String, Object> updateFile(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = null;
        try {
            result = updateFileMethod(dctx, context);
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> updateFileMethod(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException {
        Map<String, Object> result = new HashMap<>();
        Locale locale = (Locale) context.get("locale");
        String dataResourceTypeId = (String) context.get("dataResourceTypeId");
        String objectInfo = (String) context.get("objectInfo");
        String textData = (String) context.get("textData");
        ByteBuffer binData = (ByteBuffer) context.get("binData");
        String prefix = "";
        File file = null;
        String fileName = "";
        String sep = "";
        try {
            if (UtilValidate.isEmpty(dataResourceTypeId) || dataResourceTypeId.startsWith("LOCAL_FILE")) {
                fileName = prefix + sep + objectInfo;
                file = new File(fileName);
                if (!file.isAbsolute()) {
                    throw new GenericServiceException("File: " + fileName + " is not absolute.");
                }
            } else if (dataResourceTypeId.startsWith("OFBIZ_FILE")) {
                prefix = System.getProperty("ofbiz.home");
                if (objectInfo.indexOf('/') != 0 && prefix.lastIndexOf('/') != (prefix.length() - 1)) {
                    sep = "/";
                }
                file = new File(prefix + sep + objectInfo);
            } else if (dataResourceTypeId.startsWith("CONTEXT_FILE")) {
                prefix = (String) context.get("rootDir");
                if (objectInfo.indexOf('/') != 0 && prefix.lastIndexOf('/') != (prefix.length() - 1)) {
                    sep = "/";
                }
                file = new File(prefix + sep + objectInfo);
            }
            if (file == null) {
                throw new IOException("File is null");
            }

            // write the data to the file
            if (UtilValidate.isNotEmpty(textData)) {
                try (
                        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file),UtilIO.getUtf8());
                ) {
                    out.write(textData);
                } catch (IOException e) {
                    Debug.logWarning(e, module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentUnableWriteCharacterDataToFile", UtilMisc.toMap("fileName", file.getAbsolutePath()), locale));
                }
            } else if (binData != null) {
                try {
                    RandomAccessFile out = new RandomAccessFile(file, "rw");
                    out.setLength(binData.array().length);
                    out.write(binData.array());
                    out.close();
                } catch (FileNotFoundException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentUnableToOpenFileForWriting", UtilMisc.toMap("fileName", file.getAbsolutePath()), locale));
                } catch (IOException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentUnableWriteBinaryDataToFile", UtilMisc.toMap("fileName", file.getAbsolutePath()), locale));
                }
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentNoContentFilePassed", UtilMisc.toMap("fileName", file.getAbsolutePath()), locale));
            }

        } catch (IOException e) {
            Debug.logWarning(e, module);
            throw new GenericServiceException(e.getMessage());
        }

        return result;
    }

    public static Map<String, Object> renderDataResourceAsText(DispatchContext dctx, Map<String, ? extends Object> context) throws GeneralException, IOException {
        Map<String, Object> results = new HashMap<>();
        //LocalDispatcher dispatcher = dctx.getDispatcher();
        Writer out = (Writer) context.get("outWriter");
        Map<String, Object> templateContext = UtilGenerics.checkMap(context.get("templateContext"));
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
            templateContext = new HashMap<>();
        }

        Writer outWriter = new StringWriter();
        DataResourceWorker.renderDataResourceAsText(dctx.getDispatcher(), dataResourceId, outWriter, templateContext, locale, mimeTypeId, true);
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
    public static Map<String, Object> updateImage(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = updateImageMethod(dctx, context);
        return result;
    }

    public static Map<String, Object> updateImageMethod(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        //Locale locale = (Locale) context.get("locale");
        String dataResourceId = (String) context.get("dataResourceId");
        ByteBuffer byteBuffer = (ByteBuffer)context.get("imageData");
        if (byteBuffer != null) {
            byte[] imageBytes = byteBuffer.array();
            try {
                GenericValue imageDataResource = EntityQuery.use(delegator).from("ImageDataResource").where("dataResourceId", dataResourceId).queryOne();
                if (Debug.infoOn()) {
                    Debug.logInfo("imageDataResource(U):" + imageDataResource, module);
                    Debug.logInfo("imageBytes(U):" + Arrays.toString(imageBytes), module);
                }
                if (imageDataResource == null) {
                    return createImageMethod(dctx, context);
                }
                imageDataResource.setBytes("imageData", imageBytes);
                imageDataResource.store();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.getMessage());
            }
        }
        return result;
    }

    /**
     * A service wrapper for the createImageMethod method. Forces permissions to be checked.
     */
    public static Map<String, Object> createImage(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = createImageMethod(dctx, context);
        return result;
    }

    public static Map<String, Object> createImageMethod(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        String dataResourceId = (String) context.get("dataResourceId");
        ByteBuffer byteBuffer = (ByteBuffer)context.get("imageData");
        if (byteBuffer != null) {
            byte[] imageBytes = byteBuffer.array();
            try {
                GenericValue imageDataResource = delegator.makeValue("ImageDataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
                imageDataResource.setBytes("imageData", imageBytes);
                if (Debug.infoOn()) {
                    Debug.logInfo("imageDataResource(C):" + imageDataResource, module);
                }
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
    public static Map<String, Object> createBinaryFile(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = null;
        try {
            result = createBinaryFileMethod(dctx, context);
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> createBinaryFileMethod(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException {
        Map<String, Object> result = new HashMap<>();
        GenericValue dataResource = (GenericValue) context.get("dataResource");
        String dataResourceTypeId = (String) dataResource.get("dataResourceTypeId");
        String objectInfo = (String) dataResource.get("objectInfo");
        byte [] imageData = (byte []) context.get("imageData");
        String rootDir = (String)context.get("rootDir");
        File file = null;
        if (Debug.infoOn()) {
            Debug.logInfo("in createBinaryFileMethod, dataResourceTypeId:" + dataResourceTypeId, module);
            Debug.logInfo("in createBinaryFileMethod, objectInfo:" + objectInfo, module);
            Debug.logInfo("in createBinaryFileMethod, rootDir:" + rootDir, module);
        }
        try {
            file = DataResourceWorker.getContentFile(dataResourceTypeId, objectInfo, rootDir);
        } catch (FileNotFoundException | GeneralException e) {
            Debug.logWarning(e, module);
            throw new GenericServiceException(e.getMessage());
        }
        if (Debug.infoOn()) {
            Debug.logInfo("in createBinaryFileMethod, file:" + file, module);
            Debug.logInfo("in createBinaryFileMethod, imageData:" + imageData.length, module);
        }
        if (imageData != null && imageData.length > 0) {
            try (
                FileOutputStream out = new FileOutputStream(file);
            ) {
                out.write(imageData);
                if (Debug.infoOn()) {
                    Debug.logInfo("in createBinaryFileMethod, length:" + file.length(), module);
                }
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
    public static Map<String, Object> updateBinaryFile(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = null;
        try {
            result = updateBinaryFileMethod(dctx, context);
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> updateBinaryFileMethod(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException {
        Map<String, Object> result = new HashMap<>();
        GenericValue dataResource = (GenericValue) context.get("dataResource");
        String dataResourceTypeId = (String) dataResource.get("dataResourceTypeId");
        String objectInfo = (String) dataResource.get("objectInfo");
        byte [] imageData = (byte []) context.get("imageData");
        String rootDir = (String)context.get("rootDir");
        File file = null;
        if (Debug.infoOn()) {
            Debug.logInfo("in updateBinaryFileMethod, dataResourceTypeId:" + dataResourceTypeId, module);
            Debug.logInfo("in updateBinaryFileMethod, objectInfo:" + objectInfo, module);
            Debug.logInfo("in updateBinaryFileMethod, rootDir:" + rootDir, module);
        }
        try {
            file = DataResourceWorker.getContentFile(dataResourceTypeId, objectInfo, rootDir);
        } catch (FileNotFoundException e) {
            Debug.logWarning(e, module);
            throw new GenericServiceException(e.getMessage());
        } catch (GeneralException e2) {
            Debug.logWarning(e2, module);
            throw new GenericServiceException(e2.getMessage());
        }
        if (Debug.infoOn()) {
            Debug.logInfo("in updateBinaryFileMethod, file:" + file, module);
            Debug.logInfo("in updateBinaryFileMethod, imageData:" + Arrays.toString(imageData), module);
        }
        if (imageData != null && imageData.length > 0) {
            try (
                    FileOutputStream out = new FileOutputStream(file);
                    ){
                out.write(imageData);
                if (Debug.infoOn()) {
                    Debug.logInfo("in updateBinaryFileMethod, length:" + file.length(), module);
                }
            } catch (IOException e) {
                Debug.logWarning(e, module);
                throw new GenericServiceException(e.getMessage());
            }
        }
        return result;
    }
}
