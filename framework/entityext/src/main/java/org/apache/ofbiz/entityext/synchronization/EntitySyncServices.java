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
package org.apache.ofbiz.entityext.synchronization;

import static org.apache.ofbiz.base.util.UtilGenerics.checkList;

import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilURL;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.serialize.SerializeException;
import org.apache.ofbiz.entity.serialize.XmlSerializer;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entityext.synchronization.EntitySyncContext.SyncAbortException;
import org.apache.ofbiz.entityext.synchronization.EntitySyncContext.SyncErrorException;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.ibm.icu.util.Calendar;

/**
 * Entity Engine Sync Services
 */
public class EntitySyncServices {

    public static final String module = EntitySyncServices.class.getName();
    public static final String resource = "EntityExtUiLabels";

    /**
     * Run an Entity Sync (checks to see if other already running, etc)
     *@param dctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> runEntitySync(DispatchContext dctx, Map<String, ? extends Object> context) {
        Locale locale = (Locale) context.get("locale");
        EntitySyncContext esc = null;
        try {
            esc = new EntitySyncContext(dctx, context);
            if ("Y".equals(esc.entitySync.get("forPullOnly"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtCannotDoEntitySyncPush", locale));
            }

            esc.runPushStartRunning();

            // increment starting time to run until now
            esc.setSplitStartTime(); // just run this the first time, will be updated between each loop automatically
            while (esc.hasMoreTimeToSync()) {

                // this will result in lots of log messages, so leaving commented out unless needed/wanted later
                // Debug.logInfo("Doing runEntitySync split, currentRunStartTime=" + esc.currentRunStartTime + ", currentRunEndTime=" + esc.currentRunEndTime, module);

                esc.totalSplits++;

                // tx times are indexed
                // keep track of how long these sync runs take and store that info on the history table
                // saves info about removed, all entities that don't have no-auto-stamp set, this will be done in the GenericDAO like the stamp sets

                // ===== INSERTS =====
                ArrayList<GenericValue> valuesToCreate = esc.assembleValuesToCreate();
                // ===== UPDATES =====
                ArrayList<GenericValue> valuesToStore = esc.assembleValuesToStore();
                // ===== DELETES =====
                List<GenericEntity> keysToRemove = esc.assembleKeysToRemove();

                esc.runPushSendData(valuesToCreate, valuesToStore, keysToRemove);

                esc.saveResultsReportedFromDataStore();
                esc.advanceRunTimes();
            }

            esc.saveFinalSyncResults();

        } catch (SyncAbortException e) {
            return e.returnError(module);
        } catch (SyncErrorException e) {
            e.saveSyncErrorInfo(esc);
            return e.returnError(module);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Store Entity Sync Data
     *@param dctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> storeEntitySyncData(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        String overrideDelegatorName = (String) context.get("delegatorName");
        Locale locale = (Locale) context.get("locale");
        if (UtilValidate.isNotEmpty(overrideDelegatorName)) {
            delegator = DelegatorFactory.getDelegator(overrideDelegatorName);
            if (delegator == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtCannotFindDelegator", UtilMisc.toMap("overrideDelegatorName", overrideDelegatorName), locale));
            }
        }
        //LocalDispatcher dispatcher = dctx.getDispatcher();

        String entitySyncId = (String) context.get("entitySyncId");
        // incoming lists will already be sorted by lastUpdatedStamp (or lastCreatedStamp)
        List<GenericValue> valuesToCreate = UtilGenerics.cast(context.get("valuesToCreate"));
        List<GenericValue> valuesToStore = UtilGenerics.cast(context.get("valuesToStore"));
        List<GenericEntity> keysToRemove = UtilGenerics.cast(context.get("keysToRemove"));

        if (Debug.infoOn()) Debug.logInfo("Running storeEntitySyncData (" + entitySyncId + ") - [" + valuesToCreate.size() + "] to create; [" + valuesToStore.size() + "] to store; [" + keysToRemove.size() + "] to remove.", module);
        try {
            long toCreateInserted = 0;
            long toCreateUpdated = 0;
            long toCreateNotUpdated = 0;
            long toStoreInserted = 0;
            long toStoreUpdated = 0;
            long toStoreNotUpdated = 0;
            long toRemoveDeleted = 0;
            long toRemoveAlreadyDeleted = 0;

            // create all values in the valuesToCreate List; if the value already exists update it, or if exists and was updated more recently than this one dont update it
            for (GenericValue valueToCreate : valuesToCreate) {
                // to Create check if exists (find by pk), if not insert; if exists check lastUpdatedStamp: if null or before the candidate value insert, otherwise don't insert
                // NOTE: use the delegator from this DispatchContext rather than the one named in the GenericValue

                // maintain the original timestamps when doing storage of synced data, by default with will update the timestamps to now
                valueToCreate.setIsFromEntitySync(true);

                // check to make sure all foreign keys are created; if not create dummy values as place holders
                valueToCreate.checkFks(true);

                GenericValue existingValue = EntityQuery.use(delegator)
                                                        .from(valueToCreate.getEntityName())
                                                        .where(valueToCreate.getPrimaryKey())
                                                        .queryOne();
                if (existingValue == null) {
                    delegator.create(valueToCreate);
                    toCreateInserted++;
                } else {
                    // if the existing value has a stamp field that is AFTER the stamp on the valueToCreate, don't update it
                    if (existingValue.get(ModelEntity.STAMP_FIELD) != null && existingValue.getTimestamp(ModelEntity.STAMP_FIELD).after(valueToCreate.getTimestamp(ModelEntity.STAMP_FIELD))) {
                        toCreateNotUpdated++;
                    } else {
                        delegator.store(valueToCreate);
                        toCreateUpdated++;
                    }
                }
            }

            // iterate through to store list and store each
            for (GenericValue valueToStore  : valuesToStore) {
                // to store check if exists (find by pk), if not insert; if exists check lastUpdatedStamp: if null or before the candidate value insert, otherwise don't insert

                // maintain the original timestamps when doing storage of synced data, by default with will update the timestamps to now
                valueToStore.setIsFromEntitySync(true);

                // check to make sure all foreign keys are created; if not create dummy values as place holders
                valueToStore.checkFks(true);

                GenericValue existingValue = EntityQuery.use(delegator)
                                                        .from(valueToStore.getEntityName())
                                                        .where(valueToStore.getPrimaryKey())
                                                        .queryOne();
                if (existingValue == null) {
                    delegator.create(valueToStore);
                    toStoreInserted++;
                } else {
                    // if the existing value has a stamp field that is AFTER the stamp on the valueToStore, don't update it
                    if (existingValue.get(ModelEntity.STAMP_FIELD) != null && existingValue.getTimestamp(ModelEntity.STAMP_FIELD).after(valueToStore.getTimestamp(ModelEntity.STAMP_FIELD))) {
                        toStoreNotUpdated++;
                    } else {
                        delegator.store(valueToStore);
                        toStoreUpdated++;
                    }
                }
            }

            // iterate through to remove list and remove each
            for (GenericEntity pkToRemove : keysToRemove) {
                // check to see if it exists, if so remove and count, if not just count already removed
                // always do a removeByAnd, if it was a removeByAnd great, if it was a removeByPrimaryKey, this will also work and save us a query
                pkToRemove.setIsFromEntitySync(true);

                // remove the stamp fields inserted by EntitySyncContext.java at or near line 646
                pkToRemove.remove(ModelEntity.STAMP_TX_FIELD);
                pkToRemove.remove(ModelEntity.STAMP_FIELD);
                pkToRemove.remove(ModelEntity.CREATE_STAMP_TX_FIELD);
                pkToRemove.remove(ModelEntity.CREATE_STAMP_FIELD);

                int numRemByAnd = delegator.removeByAnd(pkToRemove.getEntityName(), pkToRemove);
                if (numRemByAnd == 0) {
                    toRemoveAlreadyDeleted++;
                } else {
                    toRemoveDeleted++;
                }
            }

            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("toCreateInserted", toCreateInserted);
            result.put("toCreateUpdated", toCreateUpdated);
            result.put("toCreateNotUpdated", toCreateNotUpdated);
            result.put("toStoreInserted", toStoreInserted);
            result.put("toStoreUpdated", toStoreUpdated);
            result.put("toStoreNotUpdated", toStoreNotUpdated);
            result.put("toRemoveDeleted", toRemoveDeleted);
            result.put("toRemoveAlreadyDeleted", toRemoveAlreadyDeleted);
            if (Debug.infoOn()) Debug.logInfo("Finisching storeEntitySyncData (" + entitySyncId + ") - [" + keysToRemove.size() + "] to remove. Actually removed: " + toRemoveDeleted  + " already removed: " + toRemoveAlreadyDeleted, module);
            return result;
        } catch (GenericEntityException e) {
            Debug.logError(e, "Exception saving Entity Sync Data for entitySyncId [" + entitySyncId + "]: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtExceptionSavingEntitySyncData", UtilMisc.toMap("entitySyncId", entitySyncId, "errorString", e.toString()), locale));
        } catch (Throwable t) {
            Debug.logError(t, "Error saving Entity Sync Data for entitySyncId [" + entitySyncId + "]: " + t.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtErrorSavingEntitySyncData", UtilMisc.toMap("entitySyncId", entitySyncId, "errorString", t.toString()), locale));
        }
    }

    /**
     * Run Pull Entity Sync - Pull From Remote
     *@param dctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> runPullEntitySync(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        String entitySyncId = (String) context.get("entitySyncId");
        String remotePullAndReportEntitySyncDataName = (String) context.get("remotePullAndReportEntitySyncDataName");

        Debug.logInfo("Running runPullEntitySync for entitySyncId=" + context.get("entitySyncId"), module);

        // loop until no data is returned to store
        boolean gotMoreData = true;

        Timestamp startDate = null;
        Long toCreateInserted = null;
        Long toCreateUpdated = null;
        Long toCreateNotUpdated = null;
        Long toStoreInserted = null;
        Long toStoreUpdated = null;
        Long toStoreNotUpdated = null;
        Long toRemoveDeleted = null;
        Long toRemoveAlreadyDeleted = null;

        while (gotMoreData) {
            gotMoreData = false;

            // call pullAndReportEntitySyncData, initially with no results, then with results from last loop
            Map<String, Object> remoteCallContext = new HashMap<>();
            remoteCallContext.put("entitySyncId", entitySyncId);
            remoteCallContext.put("delegatorName", context.get("remoteDelegatorName"));
            remoteCallContext.put("userLogin", context.get("userLogin"));

            remoteCallContext.put("startDate", startDate);
            remoteCallContext.put("toCreateInserted", toCreateInserted);
            remoteCallContext.put("toCreateUpdated", toCreateUpdated);
            remoteCallContext.put("toCreateNotUpdated", toCreateNotUpdated);
            remoteCallContext.put("toStoreInserted", toStoreInserted);
            remoteCallContext.put("toStoreUpdated", toStoreUpdated);
            remoteCallContext.put("toStoreNotUpdated", toStoreNotUpdated);
            remoteCallContext.put("toRemoveDeleted", toRemoveDeleted);
            remoteCallContext.put("toRemoveAlreadyDeleted", toRemoveAlreadyDeleted);

            try {
                Map<String, Object> result = dispatcher.runSync(remotePullAndReportEntitySyncDataName, remoteCallContext);
                if (ServiceUtil.isError(result)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtErrorCallingRemotePull", UtilMisc.toMap("remotePullAndReportEntitySyncDataName", remotePullAndReportEntitySyncDataName), locale), null, null, result);
                }

                startDate = (Timestamp) result.get("startDate");

                try {
                    // store data returned, get results (just call storeEntitySyncData locally, get the numbers back and boom shakalaka)

                    // anything to store locally?
                    if (startDate != null && (UtilValidate.isNotEmpty(result.get("valuesToCreate")) ||
                            UtilValidate.isNotEmpty(result.get("valuesToStore")) ||
                            UtilValidate.isNotEmpty(result.get("keysToRemove")))) {

                        // yep, we got more data
                        gotMoreData = true;

                        // at least one of the is not empty, make sure none of them are null now too...
                        List<GenericValue> valuesToCreate = checkList(result.get("valuesToCreate"), GenericValue.class);
                        if (valuesToCreate == null) valuesToCreate = Collections.emptyList();
                        List<GenericValue> valuesToStore = checkList(result.get("valuesToStore"), GenericValue.class);
                        if (valuesToStore == null) valuesToStore = Collections.emptyList();
                        List<GenericEntity> keysToRemove = checkList(result.get("keysToRemove"), GenericEntity.class);
                        if (keysToRemove == null) keysToRemove = Collections.emptyList();

                        Map<String, Object> callLocalStoreContext = UtilMisc.toMap("entitySyncId", entitySyncId, "delegatorName", context.get("localDelegatorName"),
                                "valuesToCreate", valuesToCreate, "valuesToStore", valuesToStore,
                                "keysToRemove", keysToRemove);

                        callLocalStoreContext.put("userLogin", context.get("userLogin"));
                        Map<String, Object> storeResult = dispatcher.runSync("storeEntitySyncData", callLocalStoreContext);
                        if (ServiceUtil.isError(storeResult)) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtErrorCallingService", locale), null, null, storeResult);
                        }

                        // get results for next pass
                        toCreateInserted = (Long) storeResult.get("toCreateInserted");
                        toCreateUpdated = (Long) storeResult.get("toCreateUpdated");
                        toCreateNotUpdated = (Long) storeResult.get("toCreateNotUpdated");
                        toStoreInserted = (Long) storeResult.get("toStoreInserted");
                        toStoreUpdated = (Long) storeResult.get("toStoreUpdated");
                        toStoreNotUpdated = (Long) storeResult.get("toStoreNotUpdated");
                        toRemoveDeleted = (Long) storeResult.get("toRemoveDeleted");
                        toRemoveAlreadyDeleted = (Long) storeResult.get("toRemoveAlreadyDeleted");
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Error calling service to store data locally: " + e.toString(), module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtErrorCallingService", locale) + e.toString());
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, "Exception calling remote pull and report EntitySync service with name: " + remotePullAndReportEntitySyncDataName + "; " + e.toString(), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtErrorCallingRemotePull", UtilMisc.toMap("remotePullAndReportEntitySyncDataName", remotePullAndReportEntitySyncDataName), locale) + e.toString());
            } catch (Throwable t) {
                Debug.logError(t, "Error calling remote pull and report EntitySync service with name: " + remotePullAndReportEntitySyncDataName + "; " + t.toString(), module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtErrorCallingRemotePull", UtilMisc.toMap("remotePullAndReportEntitySyncDataName", remotePullAndReportEntitySyncDataName), locale) + t.toString());
            }
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Pull and Report Entity Sync Data - Called Remotely to Push Results from last pull, the Pull next set of results.
     *@param dctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> pullAndReportEntitySyncData(DispatchContext dctx, Map<String, ? extends Object> context) {
        EntitySyncContext esc = null;
        Locale locale = (Locale) context.get("locale");
        try {
            esc = new EntitySyncContext(dctx, context);

            Debug.logInfo("Doing pullAndReportEntitySyncData for entitySyncId=" + esc.entitySyncId + ", currentRunStartTime=" + esc.currentRunStartTime + ", currentRunEndTime=" + esc.currentRunEndTime, module);

            if ("Y".equals(esc.entitySync.get("forPushOnly"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtCannotDoEntitySyncPush", locale));
            }

            // Part 1: if any results are passed, store the results for the given startDate, update EntitySync, etc
            // restore info from last pull, or if no results start new run
            esc.runPullStartOrRestoreSavedResults();


            // increment starting time to run until now
            while (esc.hasMoreTimeToSync()) {
                // make sure the following message is commented out before commit:
                // Debug.logInfo("(loop)Doing pullAndReportEntitySyncData split, currentRunStartTime=" + esc.currentRunStartTime + ", currentRunEndTime=" + esc.currentRunEndTime, module);

                esc.totalSplits++;

                // tx times are indexed
                // keep track of how long these sync runs take and store that info on the history table
                // saves info about removed, all entities that don't have no-auto-stamp set, this will be done in the GenericDAO like the stamp sets

                // Part 2: get the next set of data for the given entitySyncId
                // Part 2a: return it back for storage but leave the EntitySyncHistory without results, and don't update the EntitySync last time

                // ===== INSERTS =====
                ArrayList<GenericValue> valuesToCreate = esc.assembleValuesToCreate();
                // ===== UPDATES =====
                ArrayList<GenericValue> valuesToStore = esc.assembleValuesToStore();
                // ===== DELETES =====
                List<GenericEntity> keysToRemove = esc.assembleKeysToRemove();

                esc.setTotalRowCounts(valuesToCreate, valuesToStore, keysToRemove);

                if (Debug.infoOn()) Debug.logInfo("Service pullAndReportEntitySyncData returning - [" + valuesToCreate.size() + "] to create; [" + valuesToStore.size() + "] to store; [" + keysToRemove.size() + "] to remove; [" + esc.totalRowsPerSplit + "] total rows per split.", module);
                if (esc.totalRowsPerSplit > 0) {
                    // stop if we found some data, otherwise look and try again
                    Map<String, Object> result = ServiceUtil.returnSuccess();
                    result.put("startDate", esc.startDate);
                    result.put("valuesToCreate", valuesToCreate);
                    result.put("valuesToStore", valuesToStore);
                    result.put("keysToRemove", keysToRemove);
                    return result;
                } else {
                    // save the progress to EntitySync and EntitySyncHistory, and move on...
                    esc.saveResultsReportedFromDataStore();
                    esc.advanceRunTimes();
                }
            }

            // if no more results from database to return, save final settings
            if (!esc.hasMoreTimeToSync()) {
                esc.saveFinalSyncResults();
            }
        } catch (SyncAbortException e) {
            return e.returnError(module);
        } catch (SyncErrorException e) {
            e.saveSyncErrorInfo(esc);
            return e.returnError(module);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> runOfflineEntitySync(DispatchContext dctx, Map<String, ? extends Object> context) {
        String fileName = (String) context.get("fileName");
        EntitySyncContext esc = null;
        long totalRowsExported = 0;
        try {
            esc = new EntitySyncContext(dctx, context);

            Debug.logInfo("Doing runManualEntitySync for entitySyncId=" + esc.entitySyncId + ", currentRunStartTime=" + esc.currentRunStartTime + ", currentRunEndTime=" + esc.currentRunEndTime, module);
            Document mainDoc = UtilXml.makeEmptyXmlDocument("xml-entity-synchronization");
            Element docElement = mainDoc.getDocumentElement();
            docElement.setAttribute("xml:lang", "en-US");
            esc.runOfflineStartRunning();

            // increment starting time to run until now
            esc.setSplitStartTime(); // just run this the first time, will be updated between each loop automatically

            while (esc.hasMoreTimeToSync()) {
                esc.totalSplits++;

                ArrayList<GenericValue> valuesToCreate = esc.assembleValuesToCreate();
                ArrayList<GenericValue> valuesToStore = esc.assembleValuesToStore();
                List<GenericEntity> keysToRemove = esc.assembleKeysToRemove();

                long currentRows = esc.setTotalRowCounts(valuesToCreate, valuesToStore, keysToRemove);
                totalRowsExported += currentRows;

                if (currentRows > 0) {
                    // create the XML document
                    Element syncElement = UtilXml.addChildElement(docElement, "entity-sync", mainDoc);
                    syncElement.setAttribute("entitySyncId", esc.entitySyncId);
                    syncElement.setAttribute("lastSuccessfulSynchTime", esc.currentRunEndTime.toString());

                    // serialize the list data for XML storage
                    try {
                        UtilXml.addChildElementValue(syncElement, "values-to-create", XmlSerializer.serialize(valuesToCreate), mainDoc);
                        UtilXml.addChildElementValue(syncElement, "values-to-store", XmlSerializer.serialize(valuesToStore), mainDoc);
                        UtilXml.addChildElementValue(syncElement, "keys-to-remove", XmlSerializer.serialize(keysToRemove), mainDoc);
                    } catch (SerializeException e) {
                        throw new EntitySyncContext.SyncOtherErrorException("List serialization problem", e);
                    } catch (IOException e) {
                        throw new EntitySyncContext.SyncOtherErrorException("XML writing problem", e);
                    }
                }

                // update the result info
                esc.runSaveOfflineSyncInfo(currentRows);
                esc.advanceRunTimes();
            }

            if (totalRowsExported > 0) {
                // check the file name; use a default if none is passed in
                if (UtilValidate.isEmpty(fileName)) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                    fileName = "offline_entitySync-" + esc.entitySyncId + "-" + sdf.format(new Date()) + ".xml";
                }

                // write the XML file
                try {
                    UtilXml.writeXmlDocument(fileName, mainDoc);
                } catch (java.io.FileNotFoundException e) {
                    throw new EntitySyncContext.SyncOtherErrorException(e);
                } catch (java.io.IOException e) {
                    throw new EntitySyncContext.SyncOtherErrorException(e);
                }
            } else {
                Debug.logInfo("No rows to write; no data exported.", module);
            }

            // save the final results
            esc.saveFinalSyncResults();
        } catch (SyncAbortException e) {
            return e.returnError(module);
        } catch (SyncErrorException e) {
            e.saveSyncErrorInfo(esc);
            return e.returnError(module);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> loadOfflineSyncData(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String fileName = (String) context.get("xmlFileName");
        Locale locale = (Locale) context.get("locale");
        URL xmlFile = UtilURL.fromResource(fileName);
        if (xmlFile != null) {
            Document xmlSyncDoc = null;
            try {
                xmlSyncDoc = UtilXml.readXmlDocument(xmlFile, false);
            } catch (SAXException e) {
                Debug.logError(e, module);
            } catch (ParserConfigurationException e) {
                Debug.logError(e, module);
            } catch (IOException e) {
                Debug.logError(e, module);
            }
            if (xmlSyncDoc == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtEntitySyncXMLDocumentIsNotValid", UtilMisc.toMap("fileName", fileName), locale));
            }

            List<? extends Element> syncElements = UtilXml.childElementList(xmlSyncDoc.getDocumentElement());
            if (syncElements != null) {
                for (Element entitySync: syncElements) {
                    String entitySyncId = entitySync.getAttribute("entitySyncId");
                    String startTime = entitySync.getAttribute("lastSuccessfulSynchTime");

                    String createString = UtilXml.childElementValue(entitySync, "values-to-create");
                    String storeString = UtilXml.childElementValue(entitySync, "values-to-store");
                    String removeString = UtilXml.childElementValue(entitySync, "keys-to-remove");

                    // de-serialize the value lists
                    try {
                        List<GenericValue> valuesToCreate = checkList(XmlSerializer.deserialize(createString, delegator), GenericValue.class);
                        List<GenericValue> valuesToStore = checkList(XmlSerializer.deserialize(storeString, delegator), GenericValue.class);
                        List<GenericEntity> keysToRemove = checkList(XmlSerializer.deserialize(removeString, delegator), GenericEntity.class);

                        Map<String, Object> storeContext = UtilMisc.toMap("entitySyncId", entitySyncId, "valuesToCreate", valuesToCreate,
                                "valuesToStore", valuesToStore, "keysToRemove", keysToRemove, "userLogin", userLogin);

                        // store the value(s)
                        Map<String, Object> storeResult = dispatcher.runSync("storeEntitySyncData", storeContext);
                        if (ServiceUtil.isError(storeResult)) {
                            throw new Exception(ServiceUtil.getErrorMessage(storeResult));
                        }

                        // TODO create a response document to send back to the initial sync machine
                    } catch (GenericServiceException gse) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtUnableToLoadXMLDocument", UtilMisc.toMap("entitySyncId", entitySyncId, "startTime", startTime, "errorString", gse.getMessage()), locale));
                    } catch (Exception e) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtUnableToLoadXMLDocument", UtilMisc.toMap("entitySyncId", entitySyncId, "startTime", startTime, "errorString", e.getMessage()), locale));
                    }
                }
            }
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtOfflineXMLFileNotFound", UtilMisc.toMap("fileName", fileName), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> updateOfflineEntitySync(DispatchContext dctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtThisServiceIsNotYetImplemented", locale));
    }

    /**
     * Clean EntitySyncRemove Info
     *@param dctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> cleanSyncRemoveInfo(DispatchContext dctx, Map<String, ? extends Object> context) {
        Debug.logInfo("Running cleanSyncRemoveInfo", module);
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        try {
            // find the largest keepRemoveInfoHours value on an EntitySyncRemove and kill everything before that, if none found default to 10 days (240 hours)
            double keepRemoveInfoHours = 24;

            List<GenericValue> entitySyncRemoveList = EntityQuery.use(delegator).from("EntitySync").queryList();
            for (GenericValue entitySyncRemove: entitySyncRemoveList) {
                Double curKrih = entitySyncRemove.getDouble("keepRemoveInfoHours");
                if (curKrih != null) {
                    double curKrihVal = curKrih;
                    if (curKrihVal > keepRemoveInfoHours) {
                        keepRemoveInfoHours = curKrihVal;
                    }
                }
            }


            int keepSeconds = (int) Math.floor(keepRemoveInfoHours * 3600);

            Calendar nowCal = Calendar.getInstance();
            nowCal.setTimeInMillis(System.currentTimeMillis());
            nowCal.add(Calendar.SECOND, -keepSeconds);
            Timestamp keepAfterStamp = new Timestamp(nowCal.getTimeInMillis());

            int numRemoved = delegator.removeByCondition("EntitySyncRemove", EntityCondition.makeCondition(ModelEntity.STAMP_TX_FIELD, EntityOperator.LESS_THAN, keepAfterStamp));
            Debug.logInfo("In cleanSyncRemoveInfo removed [" + numRemoved + "] values with TX timestamp before [" + keepAfterStamp + "]", module);

            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error cleaning out EntitySyncRemove info: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EntityExtErrorCleaningEntitySyncRemove", UtilMisc.toMap("errorString", e.toString()), locale));
        }
    }
}
