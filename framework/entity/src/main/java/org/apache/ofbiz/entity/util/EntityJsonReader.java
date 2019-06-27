package org.apache.ofbiz.entity.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.lang.JSON;
import org.apache.ofbiz.base.util.Base64;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericEntityNotFoundException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.datasource.GenericHelper;
import org.apache.ofbiz.entity.eca.EntityEcaHandler;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.json.JSONArray;
import org.json.JSONObject;

public class EntityJsonReader {
    public static final String module = EntitySaxReader.class.getName();
    public static final int DEFAULT_TX_TIMEOUT = 7200;
    private Delegator delegator;
    private EntityEcaHandler<?> ecaHandler = null;
    private long numberRead = 0;
    private long numberCreated = 0;
    private long numberUpdated = 0;
    private long numberReplaced = 0;
    private long numberDeleted = 0;
    private long numberSkipped = 0;

    private int valuesPerWrite = 100;
    private int valuesPerMessage = 1000;
    private int transactionTimeout = 7200;
    private boolean useTryInsertMethod = false;
    private boolean maintainTxStamps = false;
    private boolean createDummyFks = false;
    private boolean checkDataOnly = false;
    private boolean continueOnFail = false;

    private List<Object> messageList = null;

    private List<GenericValue> valuesToCreate = new ArrayList<>(valuesPerWrite);
    private List<GenericValue> valuesToDelete = new ArrayList<>(valuesPerWrite);
    private List<GenericValue> valuesToUpdate = new ArrayList<>(valuesPerWrite);

    /**TODO need to evaluate how placeholders are going to be used in json data*/
    private Map<String, Object> placeholderValues = null; //contains map of values for corresponding placeholders (eg. ${key}) in the entity xml data file.

    protected EntityJsonReader() {
    }

    public EntityJsonReader(Delegator delegator, int transactionTimeout) {
        this.delegator = delegator.cloneDelegator();
        this.transactionTimeout = transactionTimeout;
    }

    public EntityJsonReader(Delegator delegator) {
        this(delegator, DEFAULT_TX_TIMEOUT);
    }

    public int getTransactionTimeout() {
        return this.transactionTimeout;
    }

    public void setUseTryInsertMethod(boolean value) {
        this.useTryInsertMethod = value;
    }

    public void setTransactionTimeout(int transactionTimeout) throws GenericTransactionException {
        if (this.transactionTimeout != transactionTimeout) {
            TransactionUtil.setTransactionTimeout(transactionTimeout);
            this.transactionTimeout = transactionTimeout;
        }

    }

    public void setMaintainTxStamps(boolean maintainTxStamps) {
        this.maintainTxStamps = maintainTxStamps;
    }

    public void setCreateDummyFks(boolean createDummyFks) {
        this.createDummyFks = createDummyFks;
    }

    public void setCheckDataOnly(boolean checkDataOnly) {
        this.checkDataOnly = checkDataOnly;
    }

    public void setContinueOnFail(boolean continueOnFail) {
        this.continueOnFail = continueOnFail;
    }

    public void setPlaceholderValues(Map<String, Object> placeholderValues) {
        this.placeholderValues = placeholderValues;
    }

    public List<Object> getMessageList() {
        if (this.checkDataOnly && this.messageList == null) {
            this.messageList = new LinkedList();
        }

        return this.messageList;
    }

    public void setDisableEeca(boolean disableEeca) {
        if (disableEeca) {
            if (this.ecaHandler == null) {
                this.ecaHandler = this.delegator.getEntityEcaHandler();
            }

            this.delegator.setEntityEcaHandler((EntityEcaHandler) null);
        } else if (this.ecaHandler != null) {
            this.delegator.setEntityEcaHandler(this.ecaHandler);
        }

    }

    public long parse(String content) throws IOException {
        if (content == null) {
            Debug.logWarning("content was null, doing nothing", module);
            return 0L;
        } else {
            return this.convertJsonAndWriteValues(content);
        }
    }

    public long parse(URL location) throws IOException {
        if (location == null) {
            Debug.logWarning("location URL was null, doing nothing", module);
            return 0L;
        } else {
            Debug.logImportant("Beginning import from URL: " + location.toExternalForm(), module);
            long numberRead = 0L;
            InputStream is = location.openStream();
            numberRead = this.parse(is, location.toString());
            return numberRead;
        }
    }

    private long parse(InputStream is, String docDescription) throws IOException {
        JSON json = JSON.from(is);
        return this.convertJsonAndWriteValues(json.toString());
    }

    private long convertJsonAndWriteValues(String jsonString) throws IOException {
        this.numberRead = 0L;
        String _prefix = "";
        JSONArray jsonArray = new JSONArray(jsonString);
        int length = jsonArray.length();

        for (int jsonIndex = 0; jsonIndex < length; ++jsonIndex) {
            JSONObject jsonObject = jsonArray.getJSONObject(jsonIndex);
            Map<String, Map<String, Object>> flatJson = new HashMap<String, Map<String, Object>>();
            Iterator iterator = jsonObject.keySet().iterator();

            while (iterator.hasNext()) {
                String key = iterator.next().toString();
                /**TODO use something else instead of if-else */
                if (key.equals("create")) {
                    action(jsonObject.get(key), "create");
                } else if (key.equals("create-update")) {
                    action(jsonObject.get(key), "createUpdate");
                } else if (key.equals("create-replace")) {
                    action(jsonObject.get(key), "createReplace");
                } else if (key.equals("delete")) {
                    action(jsonObject.get(key), "delete");
                } else {
                    /**TODO replace this block with createUpdate method*/
                    /*Object value = jsonObject.get(key);
                    if (value != null && !value.equals("null") && value instanceof JSONObject) {
                        flatJson.put(_prefix + key, this.iterateJSONObject((JSONObject) value));
                        ModelEntity modelEntity = this.delegator.getModelEntity(key);
                        GenericValue entityVal = GenericValue.create(this.delegator, modelEntity,
                                this.iterateJSONObject((JSONObject) value));
                        if (UtilValidate.isNotEmpty(entityVal)) {
                            this.valuesToCreate.add(entityVal);
                        }
                    }*/
                    createUpdate(jsonObject);
                }
            }
        }

        this.numberRead = this.writeValues();
        return this.numberRead;
    }

    private List<Map<String, Object>> iterateJsonEntityData(Object jsonData) {
        List<Map<String, Object>> genericMapList = new LinkedList<Map<String, Object>>();
        if (jsonData instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) jsonData;
            int length = jsonArray.length();
            for (int jsonIndex = 0; jsonIndex < length; ++jsonIndex) {
                JSONObject jsonObject = jsonArray.getJSONObject(jsonIndex);
                Map<String, Object> genericMap = iterateJSONObject(jsonObject);
                if (UtilValidate.isNotEmpty(genericMap)) {
                    genericMapList.add(genericMap);
                }
            }
        } else if (jsonData instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) jsonData;
            Map<String, Object> genericMap = iterateJSONObject(jsonObject);
            if (UtilValidate.isNotEmpty(genericMap)) {
                genericMapList.add(genericMap);
            }
        }
        return genericMapList;
    }

    private Map<String, Object> iterateJSONObject(JSONObject jsonObj) {
        Map<String, Object> mapObj = new HashMap<String, Object>();
        Iterator iterator = jsonObj.keySet().iterator();
        while (iterator.hasNext()) {
            String keyStr = (String) iterator.next();
            Object keyvalue = jsonObj.get(keyStr);
            if (keyvalue instanceof String) {
                String keyValStr = org.apache.commons.text.StringEscapeUtils.unescapeJson((String) keyvalue);
                mapObj.put(keyStr, keyValStr);
            } else {
                mapObj.put(keyStr, keyvalue);
            }
        }
        return mapObj;
    }

    private long create(JSONObject jsonObject) throws IOException {
        Iterator iterator = jsonObject.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next().toString();
            Object value = jsonObject.get(key);
            if (UtilValidate.isNotEmpty(value)) {
                List<Map<String, Object>> genericMapList = this.iterateJsonEntityData(value);
                for (Map<String, Object> keyValPair : genericMapList) {
                    try {
                        ModelEntity modelEntity = this.delegator.getModelEntity(key);
                        GenericValue currentValue = delegator.makeValue(key, keyValPair);
                        if (this.maintainTxStamps) {
                            currentValue.setIsFromEntitySync(true);
                        }
                        GenericHelper helper = delegator.getEntityHelper(currentValue.getEntityName());
                        if (UtilValidate.isNotEmpty(currentValue)) {
                            boolean exist = true;
                            if (currentValue.containsPrimaryKey()) {
                                try {
                                    helper.findByPrimaryKey(currentValue.getPrimaryKey());
                                } catch (GenericEntityNotFoundException e) {
                                    exist = false;
                                }
                            }
                            if (!exist) {
                                if (this.useTryInsertMethod && !this.checkDataOnly) {
                                    currentValue.create();
                                } else {
                                    this.valuesToCreate.add(currentValue);
                                }
                                this.numberCreated++;
                            }//if pk exist ignore it.
                        }
                    } catch (Exception e) {
                        if (continueOnFail) {
                            Debug.logError(e, module);
                        } else {
                            throw new IOException(e);
                        }
                    }
                }
            }
        }
        return this.numberCreated;
    }

    private long createUpdate(JSONObject jsonObject) throws IOException {
        Iterator iterator = jsonObject.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next().toString();
            Object value = jsonObject.get(key);
            if (UtilValidate.isNotEmpty(value)) {
                List<Map<String, Object>> genericMapList = this.iterateJsonEntityData(value);
                for (Map<String, Object> keyValPair : genericMapList) {
                    try {
                        GenericValue currentValue = delegator.makeValue(key);
                        if (currentValue != null) {
                            ModelEntity modelEntity = currentValue.getModelEntity();
                            List<String> pkFields = modelEntity.getPkFieldNames();
                            for (String pkField : pkFields) {
                                ModelField modelField = modelEntity.getField(pkField);
                                Object pkFieldValue = keyValPair.get(pkField);
                                String type = modelField.getType();
                                if (type != null && "blob".equals(type)) {
                                    byte[] binData = Base64.base64Decode((pkFieldValue.toString()).getBytes());
                                    currentValue.setBytes(pkField, binData);
                                } else {
                                    currentValue.setString(pkField, pkFieldValue.toString());
                                }
                            }

                            GenericHelper helper = delegator.getEntityHelper(currentValue.getEntityName());

                            boolean exist = true;
                            if (currentValue.containsPrimaryKey()) {
                                try {
                                    helper.findByPrimaryKey(currentValue.getPrimaryKey());
                                } catch (GenericEntityNotFoundException e) {
                                    exist = false;
                                }
                            }
                            for (Map.Entry<String, Object> entry : keyValPair.entrySet()) {
                                String currentFieldName = entry.getKey();
                                Object currentFieldValue = entry.getValue();
                                if (currentFieldName != null && !pkFields.contains(currentFieldName)) {
                                    if (currentValue.getModelEntity().isField(currentFieldName)) {
                                        if (UtilValidate.isNotEmpty(currentFieldValue)) {
                                            ModelField modelField = modelEntity.getField(currentFieldName);
                                            String type = modelField.getType();
                                            if (type != null && "blob".equals(type)) {
                                                byte[] binData = Base64.base64Decode(currentFieldValue.toString().getBytes());
                                                currentValue.setBytes(currentFieldName, binData);
                                            } else {
                                                currentValue.setString(currentFieldName, currentFieldValue.toString());
                                            }
                                        }
                                    } else {
                                        Debug.logWarning("Ignoring invalid field name [" + currentFieldName + "] found for the entity: "
                                                + currentValue.getEntityName() + " with value=" + currentFieldValue.toString(), module);
                                    }
                                }
                            }
                            if (exist) {
                                this.valuesToUpdate.add(currentValue);
                            } else {
                                // Not sure about this!
                                //if (this.useTryInsertMethod && !this.checkDataOnly) {
                                //    currentValue.create();
                                //} else {
                                this.valuesToCreate.add(currentValue);
                                //}

                            }
                            if (this.maintainTxStamps) {
                                currentValue.setIsFromEntitySync(true);
                            }
                            this.numberUpdated++;
                        }
                    } catch (Exception e) {
                        if (continueOnFail) {
                            Debug.logError(e, module);
                        } else {
                            throw new IOException(e);
                        }
                    }
                }
            }
        }
        return this.numberUpdated;
    }

    private long createReplace(JSONObject jsonObject) throws IOException {
        Iterator iterator = jsonObject.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next().toString();
            Object value = jsonObject.get(key);
            if (UtilValidate.isNotEmpty(value)) {
                List<Map<String, Object>> genericMapList = this.iterateJsonEntityData(value);
                for (Map<String, Object> keyValPair : genericMapList) {
                    try {
                        GenericValue currentValue = this.delegator.makeValue(key);
                        if (this.maintainTxStamps) {
                            currentValue.setIsFromEntitySync(true);
                        }
                        ModelEntity modelEntity = currentValue.getModelEntity();
                        List<String> pkFields = modelEntity.getPkFieldNames();
                        if (currentValue != null) {
                            for (String pkField : pkFields) {
                                ModelField modelField = modelEntity.getField(pkField);
                                Object pkFieldValue = keyValPair.get(pkField);
                                String type = modelField.getType();
                                if (type != null && "blob".equals(type)) {
                                    byte[] binData = Base64.base64Decode((pkFieldValue.toString()).getBytes());
                                    currentValue.setBytes(pkField, binData);
                                } else {
                                    currentValue.setString(pkField, pkFieldValue.toString());
                                }
                            }

                            GenericHelper helper = delegator.getEntityHelper(currentValue.getEntityName());

                            boolean exist = true;
                            if (currentValue.containsPrimaryKey()) {
                                try {
                                    helper.findByPrimaryKey(currentValue.getPrimaryKey());
                                } catch (GenericEntityNotFoundException e) {
                                    exist = false;
                                }
                            } else {
                                if (modelEntity.getPksSize() == 1) {
                                    ModelField modelField = currentValue.getModelEntity().getOnlyPk();
                                    String newSeq = delegator.getNextSeqId(currentValue.getEntityName());
                                    currentValue.setString(modelField.getName(), newSeq);
                                } else {
                                    throw new IOException("Cannot store value with incomplete primary key with more than 1 primary key field: " + currentValue);
                                }
                            }

                            ModelEntity currentEntity = currentValue.getModelEntity();
                            List<String> absentFields = currentEntity.getNoPkFieldNames();
                            absentFields.removeAll(currentEntity.getAutomaticFieldNames());

                            for (Map.Entry<String, Object> entry : keyValPair.entrySet()) {
                                String currentFieldName = entry.getKey();
                                Object currentFieldValue = entry.getValue();
                                if (UtilValidate.isNotEmpty(currentFieldName) && !pkFields.contains(currentFieldName)) {
                                    if (modelEntity.isField(currentFieldName)) {
                                        if (UtilValidate.isNotEmpty(currentFieldValue)) {
                                            ModelField modelField = modelEntity.getField(currentFieldName);
                                            String type = modelField.getType();
                                            if (type != null && "blob".equals(type)) {
                                                byte[] binData = Base64.base64Decode(((String) currentFieldValue).getBytes());
                                                currentValue.setBytes(currentFieldName, binData);
                                            } else {
                                                currentValue.setString(currentFieldName, currentFieldValue.toString());
                                            }
                                            absentFields.remove(currentFieldName);
                                        }
                                    } else {
                                        Debug.logWarning("Ignoring invalid field name [" + currentFieldName + "] found for the entity: "
                                                + currentValue.getEntityName() + " with value=" + currentFieldValue.toString(), module);
                                    }
                                }
                            }
                            if (absentFields != null) {
                                for (String fieldName : absentFields) {
                                    currentValue.set(fieldName, null);
                                }
                            }
                            if (exist) {
                                this.valuesToUpdate.add(currentValue);
                            } else {
                                // Not sure about this!
                                //if (this.useTryInsertMethod && !this.checkDataOnly) {
                                //    currentValue.create();
                                //} else {
                                this.valuesToCreate.add(currentValue);
                                //}
                            }
                            if (this.maintainTxStamps) {
                                currentValue.setIsFromEntitySync(true);
                            }
                            this.numberReplaced++;
                        }
                    } catch (Exception e) {
                        if (continueOnFail) {
                            Debug.logError(e, module);
                        } else {
                            throw new IOException(e);
                        }
                    }
                }
            }
        }
        return this.numberReplaced;
    }

    private long delete(JSONObject jsonObject) throws IOException {
        Iterator iterator = jsonObject.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next().toString();
            Object value = jsonObject.get(key);
            if (UtilValidate.isNotEmpty(value)) {
                List<Map<String, Object>> genericMapList = this.iterateJsonEntityData(value);
                for (Map<String, Object> keyValPair : genericMapList) {
                    try {
                        ModelEntity modelEntity = this.delegator.getModelEntity(key);
                        GenericValue currentValue = delegator.makeValue(key, keyValPair);
                        if (this.maintainTxStamps) {
                            currentValue.setIsFromEntitySync(true);
                        }
                        GenericHelper helper = delegator.getEntityHelper(key);
                        if (currentValue != null) {
                            boolean exist = true;
                            if (currentValue.containsPrimaryKey()) {
                                try {
                                    helper.findByPrimaryKey(currentValue.getPrimaryKey());
                                } catch (GenericEntityNotFoundException e) {
                                    exist = false;
                                }
                            }
                            if (exist) {
                                if (this.useTryInsertMethod && !this.checkDataOnly) {
                                    currentValue.remove();
                                } else {
                                    this.valuesToDelete.add(currentValue);
                                }
                                this.numberDeleted++;
                            }//if pk exist ignore it.
                        }
                    } catch (Exception e) {
                        if (continueOnFail) {
                            Debug.logError(e, module);
                        } else {
                            throw new IOException(e);
                        }
                    }
                }
            }
        }
        return this.numberDeleted;
    }

    private long action(Object jsonData, String actionName) throws IOException {
        java.lang.reflect.Method method;
        try {
            method = this.getClass().getDeclaredMethod(actionName, JSONObject.class);
            if (jsonData instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) jsonData;
                int length = jsonArray.length();
                for (int jsonIndex = 0; jsonIndex < length; ++jsonIndex) {
                    JSONObject jsonObject = jsonArray.getJSONObject(jsonIndex);
                    method.invoke(this, jsonObject);
                }
            } else if (jsonData instanceof JSONObject) {
                method.invoke(this, (JSONObject) jsonData);
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
        return this.numberCreated;
    }

    private long writeValues() {
        this.numberRead = 0L;

        try {
            boolean beganTransaction = false;
            if (this.transactionTimeout > -1) {
                beganTransaction = TransactionUtil.begin(this.transactionTimeout);
                Debug.logImportant("Transaction Timeout set to " + this.transactionTimeout / 3600 + " hours ("
                        + this.transactionTimeout + " seconds)", module);
            }

            try {
                if (!this.valuesToCreate.isEmpty()) {
                    this.writeValues(this.valuesToCreate);
                    numberRead = numberRead + this.valuesToCreate.size();
                    this.valuesToCreate.clear();
                }

                if (!this.valuesToDelete.isEmpty()) {
                    this.delegator.removeAll(this.valuesToDelete);
                    numberRead = numberRead + this.valuesToDelete.size();
                    this.valuesToDelete.clear();
                }
                if (!this.valuesToUpdate.isEmpty()) {
                    this.writeValues(this.valuesToUpdate);
                    numberRead = numberRead + this.valuesToUpdate.size();
                    this.valuesToUpdate.clear();
                }

                TransactionUtil.commit(beganTransaction);
            } catch (IllegalArgumentException | GenericEntityException e1) {
                String errMsg = "An error occurred saving the data, rolling back transaction (" + beganTransaction
                        + ")";
                Debug.logError(e1, errMsg, module);
                TransactionUtil.rollback(beganTransaction, errMsg, e1);
            }
        } catch (GenericTransactionException e) {
            Debug.logError(e, "A transaction error occurred reading data", module);
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("  Detail created : " + this.numberCreated + ", skipped : " + this.numberSkipped
                    + ", updated : " + this.numberUpdated + ", replaced : " + this.numberReplaced + ", deleted : "
                    + this.numberDeleted, module);
        }

        return this.numberRead;
    }

    private void writeValues(List<GenericValue> valuesToWrite) throws GenericEntityException {
        if (this.checkDataOnly) {
            EntityDataAssert.checkValueList(valuesToWrite, this.delegator, this.getMessageList());
        } else {
            this.delegator.storeAll(valuesToWrite, new EntityStoreOptions(this.createDummyFks));
        }

    }
}
