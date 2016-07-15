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
package org.ofbiz.entityext.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.shiro.crypto.AesCipherService;
import org.ofbiz.base.crypto.DesCrypt;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.FileUtil;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.datasource.GenericHelperInfo;
import org.ofbiz.entity.jdbc.DatabaseUtil;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

/**
 * Entity Data Import/Export Services
 *
 */
public class EntityDataServices {

    public static final String module = EntityDataServices.class.getName();
    public static final String resource = "EntityExtUiLabels";

    public static Map<String, Object> exportDelimitedToDirectory(DispatchContext dctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtThisServiceIsNotYetImplemented", locale));
    }

    public static Map<String, Object> importDelimitedFromDirectory(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        Locale locale = (Locale) context.get("locale");

        // check permission
         GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (!security.hasPermission("ENTITY_MAINT", userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtServicePermissionNotGranted", locale));
        }

        // get the directory & delimiter
        String rootDirectory = (String) context.get("rootDirectory");
        URL rootDirectoryUrl = UtilURL.fromResource(rootDirectory);
        if (rootDirectoryUrl == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtUnableToLocateRootDirectory", UtilMisc.toMap("rootDirectory", rootDirectory), locale));
        }

        String delimiter = (String) context.get("delimiter");
        if (delimiter == null) {
            // default delimiter is tab
            delimiter = "\t";
        }

        File root = null;
        try {
            root = new File(new URI(rootDirectoryUrl.toExternalForm()));
        } catch (URISyntaxException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtUnableToLocateRootDirectoryURI", locale));
        }

        if (!root.exists() || !root.isDirectory() || !root.canRead()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtRootDirectoryDoesNotExists", locale));
        }

        // get the file list
        List<File> files = getFileList(root);
        if (UtilValidate.isNotEmpty(files)) {
            for (File file: files) {
                try {
                    Map<String, Object> serviceCtx = UtilMisc.toMap("file", file, "delimiter", delimiter, "userLogin", userLogin);
                    dispatcher.runSyncIgnore("importDelimitedEntityFile", serviceCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                }
            }
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtNoFileAvailableInTheRootDirectory", UtilMisc.toMap("rootDirectory", rootDirectory), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> importDelimitedFile(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        Locale locale = (Locale) context.get("locale");

        // check permission
         GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (!security.hasPermission("ENTITY_MAINT", userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtServicePermissionNotGranted", locale));
        }

        String delimiter = (String) context.get("delimiter");
        if (delimiter == null) {
            // default delimiter is tab
            delimiter = "\t";
        }

        long startTime = System.currentTimeMillis();

        File file = (File) context.get("file");
        int records = 0;
        try {
            records = readEntityFile(file, delimiter, delegator);
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (FileNotFoundException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtFileNotFound", UtilMisc.toMap("fileName", file.getName()), locale));
        } catch (IOException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtProblemReadingFile", UtilMisc.toMap("fileName", file.getName()), locale));
        }

        long endTime = System.currentTimeMillis();
        long runTime = endTime - startTime;

        Debug.logInfo("Imported/Updated [" + records + "] from : " + file.getAbsolutePath() + " [" + runTime + "ms]", module);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("records", Integer.valueOf(records));
        return result;
    }

    private static List<File> getFileList(File root) {
        List<File> fileList = new LinkedList<File>();

        // check for a file list file
        File listFile = new File(root, "FILELIST.txt");
        Debug.logInfo("Checking file list - " + listFile.getPath(), module);
        if (listFile.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(listFile));
            } catch (FileNotFoundException e) {
                Debug.logError(e, module);
            }
            if (reader != null) {
                // read each line as a file name to load
                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        File thisFile = new File(root, line);
                        if (thisFile.exists()) {
                            fileList.add(thisFile);
                        }
                    }
                } catch (IOException e) {
                    Debug.logError(e, module);
                }

                // close the reader
                try {
                    reader.close();
                } catch (IOException e) {
                    Debug.logError(e, module);
                }
                Debug.logInfo("Read file list : " + fileList.size() + " entities.", module);
            }
        } else {
            for (File file: root.listFiles()) {
                String fileName = file.getName();
                if (!fileName.startsWith("_") && fileName.endsWith(".txt")) {
                    fileList.add(file);
                }
            }
            Debug.logInfo("No file list found; using directory order : " + fileList.size() + " entities.", module);
        }

        return fileList;
    }

    private static String[] readEntityHeader(File file, String delimiter, BufferedReader dataReader) throws IOException {
        String filePath = file.getPath().replace('\\', '/');

        String[] header = null;
        File headerFile = new File(FileUtil.getFile(filePath.substring(0, filePath.lastIndexOf('/'))), "_" + file.getName());

        boolean uniqueHeaderFile = true;
        BufferedReader reader = null;
        if (headerFile.exists()) {
            reader = new BufferedReader(new FileReader(headerFile));
        } else {
            uniqueHeaderFile = false;
            reader = dataReader;
        }

        // read one line from either the header file or the data file if no header file exists
        String firstLine = reader.readLine();
        if (firstLine != null) {
            header = firstLine.split(delimiter);
        }

        if (uniqueHeaderFile) {
            reader.close();
        }

        return header;
    }

    private static int readEntityFile(File file, String delimiter, Delegator delegator) throws IOException, GeneralException {
        String entityName = file.getName().substring(0, file.getName().lastIndexOf('.'));
        if (entityName == null) {
            throw new GeneralException("Entity name cannot be null : [" + file.getName() + "]");
        }

        BufferedReader reader = new BufferedReader(new FileReader(file));
        String[] header = readEntityHeader(file, delimiter, reader);

        //Debug.logInfo("Opened data file [" + file.getName() + "] now running...", module);
        GeneralException exception = null;
        String line = null;
        int lineNumber = 1;
        while ((line = reader.readLine()) != null) {
            // process the record
            String fields[] = line.split(delimiter);
            //Debug.logInfo("Split record", module);
            if (fields.length < 1) {
                exception = new GeneralException("Illegal number of fields [" + file.getName() + " / " + lineNumber);
                break;
            }

            GenericValue newValue = makeGenericValue(delegator, entityName, header, fields);
            //Debug.logInfo("Made value object", module);
            newValue = delegator.createOrStore(newValue);
            //Debug.logInfo("Stored record", module);

            if (lineNumber % 500 == 0 || lineNumber == 1) {
                Debug.logInfo("Records Stored [" + file.getName() + "]: " + lineNumber, module);
                //Debug.logInfo("Last record : " + newValue, module);
            }

            lineNumber++;
        }
        reader.close();

        // now that we closed the reader; throw the exception
        if (exception != null) {
            throw exception;
        }

        return lineNumber;
    }

    private static GenericValue makeGenericValue(Delegator delegator, String entityName, String[] header, String[] line) {
        GenericValue newValue = delegator.makeValue(entityName);
        for (int i = 0; i < header.length; i++) {
            String name = header[i].trim();

            String value = null;
            if (i < line.length) {
                value = line[i];
            }

            // check for null values
            if (UtilValidate.isNotEmpty(value)) {
                char first = value.charAt(0);
                if (first == 0x00) {
                    value = null;
                }

                // trim non-null values
                if (value != null) {
                    value = value.trim();
                }

                if (value != null && value.length() == 0) {
                    value = null;
                }
            } else {
                value = null;
            }

            // convert and set the fields
            newValue.setString(name, value);
        }
        return newValue;
    }

    public static Map<String, Object> rebuildAllIndexesAndKeys(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        Locale locale = (Locale) context.get("locale");

        // check permission
         GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (!security.hasPermission("ENTITY_MAINT", userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtServicePermissionNotGranted", locale));
        }

        String groupName = (String) context.get("groupName");
        Boolean fixSizes = (Boolean) context.get("fixColSizes");
        if (fixSizes == null) fixSizes = Boolean.FALSE;
        List<String> messages = new LinkedList<String>();

        GenericHelperInfo helperInfo = delegator.getGroupHelperInfo(groupName);
        DatabaseUtil dbUtil = new DatabaseUtil(helperInfo);
        Map<String, ModelEntity> modelEntities;
        try {
            modelEntities = delegator.getModelEntityMapByGroup(groupName);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting list of entities in group: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtErrorGettingListOfEntityInGroup", UtilMisc.toMap("errorString", e.toString()), locale));
        }

        // step 1 - remove FK indices
        Debug.logImportant("Removing all foreign key indices", module);
        for (ModelEntity modelEntity: modelEntities.values()) {
            dbUtil.deleteForeignKeyIndices(modelEntity, messages);
        }

        // step 2 - remove FKs
        Debug.logImportant("Removing all foreign keys", module);
        for (ModelEntity modelEntity: modelEntities.values()) {
            dbUtil.deleteForeignKeys(modelEntity, modelEntities, messages);
        }

        // step 3 - remove PKs
        Debug.logImportant("Removing all primary keys", module);
        for (ModelEntity modelEntity: modelEntities.values()) {
            dbUtil.deletePrimaryKey(modelEntity, messages);
        }

        // step 4 - remove declared indices
        Debug.logImportant("Removing all declared indices", module);
        for (ModelEntity modelEntity: modelEntities.values()) {
            dbUtil.deleteDeclaredIndices(modelEntity, messages);
        }

        // step 5 - repair field sizes
        if (fixSizes.booleanValue()) {
            Debug.logImportant("Updating column field size changes", module);
            List<String> fieldsWrongSize = new LinkedList<String>();
            dbUtil.checkDb(modelEntities, fieldsWrongSize, messages, true, true, true, true);
            if (fieldsWrongSize.size() > 0) {
                dbUtil.repairColumnSizeChanges(modelEntities, fieldsWrongSize, messages);
            } else {
                String thisMsg = "No field sizes to update";
                messages.add(thisMsg);
                Debug.logImportant(thisMsg, module);
            }
        }

        // step 6 - create PKs
        Debug.logImportant("Creating all primary keys", module);
        for (ModelEntity modelEntity: modelEntities.values()) {
            dbUtil.createPrimaryKey(modelEntity, messages);
        }

        // step 7 - create FK indices
        Debug.logImportant("Creating all foreign key indices", module);
        for (ModelEntity modelEntity: modelEntities.values()) {
            dbUtil.createForeignKeyIndices(modelEntity, messages);
        }

        // step 8 - create FKs
        Debug.logImportant("Creating all foreign keys", module);
        for (ModelEntity modelEntity: modelEntities.values()) {
            dbUtil.createForeignKeys(modelEntity, modelEntities, messages);
        }

        // step 8 - create FKs
        Debug.logImportant("Creating all declared indices", module);
        for (ModelEntity modelEntity: modelEntities.values()) {
            dbUtil.createDeclaredIndices(modelEntity, messages);
        }

        // step 8 - checkdb
        Debug.logImportant("Running DB check with add missing enabled", module);
        dbUtil.checkDb(modelEntities, messages, true);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("messages", messages);
        return result;
    }

    public static Map<String, Object> unwrapByteWrappers(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        String entityName = (String) context.get("entityName");
        String fieldName = (String) context.get("fieldName");
        Locale locale = (Locale) context.get("locale");
        EntityListIterator eli = null;
        try {
            eli = EntityQuery.use(delegator).from(entityName).queryIterator();
            GenericValue currentValue;
            while ((currentValue = eli.next()) != null) {
                byte[] bytes = currentValue.getBytes(fieldName);
                if (bytes != null) {
                    currentValue.setBytes(fieldName, bytes);
                    currentValue.store();
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error unwrapping ByteWrapper records: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtErrorUnwrappingRecords", UtilMisc.toMap("errorString", e.toString()), locale));
        } finally {
            if (eli != null) {
                try {
                    eli.close();
                } catch (GenericEntityException e) {
                    String errMsg = "Error closing EntityListIterator: " + e.toString();
                    Debug.logError(e, errMsg, module);
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> reencryptPrivateKeys(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        Locale locale = (Locale) context.get("locale");

        // check permission
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (!security.hasPermission("ENTITY_MAINT", userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtServicePermissionNotGranted", locale));
        }
        String oldKey = (String) context.get("oldKey");
        String newKey = (String) context.get("newKey");
        AesCipherService cipherService = new AesCipherService();
        try {
            List<GenericValue> rows = EntityQuery.use(delegator).from("EntityKeyStore").queryList();
            for (GenericValue row: rows) {
                byte[] keyBytes = Base64.decodeBase64(row.getString("keyText"));
                Debug.logInfo("Processing entry " + row.getString("keyName") + " with key: " + row.getString("keyText"), module);
                if (oldKey != null) {
                    Debug.logInfo("Decrypting with old key: " + oldKey, module);
                    try {
                        keyBytes = cipherService.decrypt(keyBytes, Base64.decodeBase64(oldKey)).getBytes();
                    } catch(Exception e) {
                        Debug.logInfo("Failed to decrypt with Shiro cipher; trying with old cipher", module);
                        try {
                            keyBytes = DesCrypt.decrypt(DesCrypt.getDesKey(Base64.decodeBase64(oldKey)), keyBytes);
                        } catch(Exception e1) {
                            Debug.logError(e1, module);
                            return ServiceUtil.returnError(e1.getMessage());
                        }
                    }
                }
                String newKeyText;
                if (newKey != null) {
                    Debug.logInfo("Encrypting with new key: " + oldKey, module);
                    newKeyText = cipherService.encrypt(keyBytes, Base64.decodeBase64(newKey)).toBase64();
                } else {
                    newKeyText = Base64.encodeBase64String(keyBytes);
                }
                Debug.logInfo("Storing new encrypted value: " + newKeyText, module);
                row.setString("keyText", newKeyText);
                row.store();
            }
        } catch(GenericEntityException gee) {
            Debug.logError(gee, module);
            return ServiceUtil.returnError(gee.getMessage());
        }
        delegator.clearAllCaches();
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> reencryptFields(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        Locale locale = (Locale) context.get("locale");

        // check permission
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (!security.hasPermission("ENTITY_MAINT", userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtServicePermissionNotGranted", locale));
        }

        String groupName = (String) context.get("groupName");

        Map<String, ModelEntity> modelEntities;
        try {
            modelEntities = delegator.getModelEntityMapByGroup(groupName);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting list of entities in group: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtErrorGettingListOfEntityInGroup", UtilMisc.toMap("errorString", e.toString()), locale));
        }

        for (ModelEntity modelEntity: modelEntities.values()) {
            List<ModelField> fields = modelEntity.getFieldsUnmodifiable();
            for (ModelField field: fields) {
                if (field.getEncryptMethod().isEncrypted()) {
                    try {
                        List<GenericValue> rows = EntityQuery.use(delegator).from(modelEntity.getEntityName()).select(field.getName()).queryList();
                        for (GenericValue row: rows) {
                            row.setString(field.getName(), row.getString(field.getName()));
                            row.store();
                        }
                    } catch(GenericEntityException gee) {
                        return ServiceUtil.returnError(gee.getMessage());
                    }
                }
            }
        }
        return ServiceUtil.returnSuccess();
    }
}
