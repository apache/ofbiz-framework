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

import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.security.Security;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.jdbc.DatabaseUtil;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.UtilMisc;

import java.util.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;

/**
 * Entity Data Import/Export Services
 *
 */
public class EntityDataServices {

    public static final String module = EntityDataServices.class.getName();

    public static Map exportDelimitedToDirectory(DispatchContext dctx, Map context) {
        return ServiceUtil.returnError("This service is not implemented yet.");
    }

    public static Map importDelimitedFromDirectory(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();

        // check permission
         GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (!security.hasPermission("ENTITY_MAINT", userLogin)) {
            return ServiceUtil.returnError("You do not have permission to run this service.");
        }

        // get the directory & delimiter
        String rootDirectory = (String) context.get("rootDirectory");
        URL rootDirectoryUrl = UtilURL.fromResource(rootDirectory);
        if (rootDirectoryUrl == null) {
            return ServiceUtil.returnError("Unable to locate root directory : " + rootDirectory);
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
            return ServiceUtil.returnError("Unable to get root directory URI");
        }

        if (!root.exists() || !root.isDirectory() || !root.canRead()) {
            return ServiceUtil.returnError("Root directory does not exist or is not readable.");
        }

        // get the file list
        List files = getFileList(root);
        if (files != null && files.size() > 0) {
            Iterator i = files.iterator();
            while (i.hasNext()) {
                File file = (File) i.next();
                try {
                    Map serviceCtx = UtilMisc.toMap("file", file, "delimiter", delimiter, "userLogin", userLogin);
                    dispatcher.runSyncIgnore("importDelimitedEntityFile", serviceCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                }
            }
        } else {
            return ServiceUtil.returnError("No files available for reading in this root directory : " + rootDirectory);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map importDelimitedFile(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();

        // check permission
         GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (!security.hasPermission("ENTITY_MAINT", userLogin)) {
            return ServiceUtil.returnError("You do not have permission to run this service.");
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
            return ServiceUtil.returnError("File not found : " + file.getName());
        } catch (IOException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Problem reading file : " + file.getName());
        }

        long endTime = System.currentTimeMillis();
        long runTime = endTime - startTime;

        Debug.logInfo("Imported/Updated [" + records + "] from : " + file.getAbsolutePath() + " [" + runTime + "ms]", module);
        Map result = ServiceUtil.returnSuccess();
        result.put("records", new Integer(records));
        return result;
    }

    private static List getFileList(File root) {
        List fileList = new ArrayList();

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
            File[] files = root.listFiles();
            for (int i = 0; i < files.length; i++) {
                String fileName = files[i].getName();
                if (!fileName.startsWith("_") && fileName.endsWith(".txt")) {
                    fileList.add(files[i]);
                }
            }
            Debug.logInfo("No file list found; using directory order : " + fileList.size() + " entities.", module);
        }

        return fileList;
    }

    private static String[] readEntityHeader(File file, String delimiter, BufferedReader dataReader) throws IOException {
        String filePath = file.getPath().replace('\\', '/');

        String[] header = null;
        File headerFile = new File(filePath.substring(0, filePath.lastIndexOf('/')), "_" + file.getName());

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

    private static int readEntityFile(File file, String delimiter, GenericDelegator delegator) throws IOException, GeneralException {
        String entityName = file.getName().substring(0, file.getName().lastIndexOf('.'));
        if (entityName == null) {
            throw new GeneralException("Entity name cannot be null : [" + file.getName() + "]");
        }

        BufferedReader reader = new BufferedReader(new FileReader(file));
        String[] header = readEntityHeader(file, delimiter, reader);

        //Debug.log("Opened data file [" + file.getName() + "] now running...", module);
        GeneralException exception = null;
        String line = null;
        int lineNumber = 1;
        while ((line = reader.readLine()) != null) {
            // process the record
            String fields[] = line.split(delimiter);
            //Debug.log("Split record", module);
            if (fields.length < 1) {
                exception = new GeneralException("Illegal number of fields [" + file.getName() + " / " + lineNumber);
                break;
            }

            GenericValue newValue = makeGenericValue(delegator, entityName, header, fields);
            //Debug.log("Made value object", module);
            newValue = delegator.createOrStore(newValue);
            //Debug.log("Stored record", module);

            if (lineNumber % 500 == 0 || lineNumber == 1) {
                Debug.log("Records Stored [" + file.getName() + "]: " + lineNumber, module);
                //Debug.log("Last record : " + newValue, module);
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

    private static GenericValue makeGenericValue(GenericDelegator delegator, String entityName, String[] header, String[] line) {
        GenericValue newValue = delegator.makeValue(entityName, null);
        for (int i = 0; i < header.length; i++) {
            String name = header[i].trim();

            String value = null;
            if (i < line.length) {
                value = line[i];
            }

            // check for null values
            if (value != null && value.length() > 0) {
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

    private String[] getEntityFieldNames(GenericDelegator delegator, String entityName) {
        ModelEntity entity = delegator.getModelEntity(entityName);
        if (entity == null) {
            return null;
        }
        List modelFields = entity.getFieldsCopy();
        if (modelFields == null) {
            return null;
        }

        String[] fieldNames = new String[modelFields.size()];
        for (int i = 0; i < modelFields.size(); i++) {
            ModelField field = (ModelField) modelFields.get(i);
            fieldNames[i] = field.getName();
        }

        return fieldNames;
    }

    public static Map rebuildAllIndexesAndKeys(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();

        // check permission
         GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (!security.hasPermission("ENTITY_MAINT", userLogin)) {
            return ServiceUtil.returnError("You do not have permission to run this service.");
        }

        String groupName = (String) context.get("groupName");
        Boolean fixSizes = (Boolean) context.get("fixColSizes");
        if (fixSizes == null) fixSizes = Boolean.FALSE;
        List messages = new ArrayList();

        String helperName = delegator.getGroupHelperName(groupName);
        DatabaseUtil dbUtil = new DatabaseUtil(helperName);
        Map modelEntities = delegator.getModelEntityMapByGroup(groupName);
        Set modelEntityNames = new TreeSet(modelEntities.keySet());

        Iterator modelEntityNameIter = null;

        // step 1 - remove FK indices
        Debug.logImportant("Removing all foreign key indices", module);
        modelEntityNameIter = modelEntityNames.iterator();
        while (modelEntityNameIter.hasNext()) {
      	    String modelEntityName = (String) modelEntityNameIter.next();
      	    ModelEntity modelEntity = (ModelEntity) modelEntities.get(modelEntityName);
            dbUtil.deleteForeignKeyIndices(modelEntity, messages);
        }
        modelEntityNameIter = null;

        // step 2 - remove FKs
        Debug.logImportant("Removing all foreign keys", module);
        modelEntityNameIter = modelEntityNames.iterator();
        while (modelEntityNameIter.hasNext()) {
      	    String modelEntityName = (String) modelEntityNameIter.next();
      	    ModelEntity modelEntity = (ModelEntity) modelEntities.get(modelEntityName);
            dbUtil.deleteForeignKeys(modelEntity, modelEntities, messages);
        }
        modelEntityNameIter = null;

        // step 3 - remove PKs
        Debug.logImportant("Removing all primary keys", module);
        modelEntityNameIter = modelEntityNames.iterator();
        while (modelEntityNameIter.hasNext()) {
            String modelEntityName = (String) modelEntityNameIter.next();
            ModelEntity modelEntity = (ModelEntity) modelEntities.get(modelEntityName);
            dbUtil.deletePrimaryKey(modelEntity, messages);
        }
        modelEntityNameIter = null;

        // step 4 - remove declared indices
        Debug.logImportant("Removing all declared indices", module);
        modelEntityNameIter = modelEntityNames.iterator();
        while (modelEntityNameIter.hasNext()) {
            String modelEntityName = (String) modelEntityNameIter.next();
            ModelEntity modelEntity = (ModelEntity) modelEntities.get(modelEntityName);
            dbUtil.deleteDeclaredIndices(modelEntity, messages);
        }
        modelEntityNameIter = null;

        // step 5 - repair field sizes
        if (fixSizes.booleanValue()) {
            Debug.logImportant("Updating column field size changes", module);
            List fieldsWrongSize = new LinkedList();
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
        modelEntityNameIter = modelEntityNames.iterator();
        while (modelEntityNameIter.hasNext()) {
            String modelEntityName = (String) modelEntityNameIter.next();
            ModelEntity modelEntity = (ModelEntity) modelEntities.get(modelEntityName);
            dbUtil.createPrimaryKey(modelEntity, messages);
        }
        modelEntityNameIter = null;

        // step 7 - create FK indices
        Debug.logImportant("Creating all foreign key indices", module);
        modelEntityNameIter = modelEntityNames.iterator();
        while (modelEntityNameIter.hasNext()) {
      	    String modelEntityName = (String) modelEntityNameIter.next();
      	    ModelEntity modelEntity = (ModelEntity) modelEntities.get(modelEntityName);
            dbUtil.createForeignKeyIndices(modelEntity, messages);
        }
        modelEntityNameIter = null;

        // step 8 - create FKs
        Debug.logImportant("Creating all foreign keys", module);
        modelEntityNameIter = modelEntityNames.iterator();
        while (modelEntityNameIter.hasNext()) {
      	    String modelEntityName = (String) modelEntityNameIter.next();
      	    ModelEntity modelEntity = (ModelEntity) modelEntities.get(modelEntityName);
            dbUtil.createForeignKeys(modelEntity, modelEntities, messages);
        }
        modelEntityNameIter = null;

        // step 8 - create FKs
        Debug.logImportant("Creating all declared indices", module);
        modelEntityNameIter = modelEntityNames.iterator();
        while (modelEntityNameIter.hasNext()) {
      	    String modelEntityName = (String) modelEntityNameIter.next();
      	    ModelEntity modelEntity = (ModelEntity) modelEntities.get(modelEntityName);
            dbUtil.createDeclaredIndices(modelEntity, messages);
        }
        modelEntityNameIter = null;

        // step 8 - checkdb
        Debug.logImportant("Running DB check with add missing enabled", module);
        dbUtil.checkDb(modelEntities, messages, true);
        
        Map result = ServiceUtil.returnSuccess();
        result.put("messages", messages);
        return result;
    }
}
