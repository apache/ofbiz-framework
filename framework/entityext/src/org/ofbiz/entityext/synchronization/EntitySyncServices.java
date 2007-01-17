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
package org.ofbiz.entityext.synchronization;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.IOException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.serialize.XmlSerializer;
import org.ofbiz.entity.serialize.SerializeException;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entityext.synchronization.EntitySyncContext.SyncAbortException;
import org.ofbiz.entityext.synchronization.EntitySyncContext.SyncErrorException;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Entity Engine Sync Services
 */
public class EntitySyncServices {
    
    public static final String module = EntitySyncServices.class.getName();
    
    /**
     * Run an Entity Sync (checks to see if other already running, etc)
     *@param dctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map runEntitySync(DispatchContext dctx, Map context) {
        EntitySyncContext esc = null;
        try {
            esc = new EntitySyncContext(dctx, context);
            if ("Y".equals(esc.entitySync.get("forPullOnly"))) {
                return ServiceUtil.returnError("Cannot do Entity Sync Push because entitySyncId [] is set for Pull Only.");
            }

            esc.runPushStartRunning();

            // increment starting time to run until now
            esc.setSplitStartTime(); // just run this the first time, will be updated between each loop automatically
            while (esc.hasMoreTimeToSync()) {
                
                // TODO make sure the following message is commented out before commit:
                // Debug.logInfo("Doing runEntitySync split, currentRunStartTime=" + esc.currentRunStartTime + ", currentRunEndTime=" + esc.currentRunEndTime, module);
                
                esc.totalSplits++;
                
                // tx times are indexed
                // keep track of how long these sync runs take and store that info on the history table
                // saves info about removed, all entities that don't have no-auto-stamp set, this will be done in the GenericDAO like the stamp sets
                
                // ===== INSERTS =====
                ArrayList valuesToCreate = esc.assembleValuesToCreate();
                // ===== UPDATES =====
                ArrayList valuesToStore = esc.assembleValuesToStore();
                // ===== DELETES =====
                List keysToRemove = esc.assembleKeysToRemove();
                
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
    public static Map storeEntitySyncData(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        String overrideDelegatorName = (String) context.get("delegatorName");
        if (UtilValidate.isNotEmpty(overrideDelegatorName)) {
            delegator = GenericDelegator.getGenericDelegator(overrideDelegatorName);
            if (delegator == null) {
                return ServiceUtil.returnError("Could not find delegator with specified name " + overrideDelegatorName);
            }
        }
        //LocalDispatcher dispatcher = dctx.getDispatcher();
        
        String entitySyncId = (String) context.get("entitySyncId");
        // incoming lists will already be sorted by lastUpdatedStamp (or lastCreatedStamp)
        List valuesToCreate = (List) context.get("valuesToCreate");
        List valuesToStore = (List) context.get("valuesToStore");
        List keysToRemove = (List) context.get("keysToRemove");

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
            Iterator valueToCreateIter = valuesToCreate.iterator();
            while (valueToCreateIter.hasNext()) {
                GenericValue valueToCreate = (GenericValue) valueToCreateIter.next();
                // to Create check if exists (find by pk), if not insert; if exists check lastUpdatedStamp: if null or before the candidate value insert, otherwise don't insert
                // NOTE: use the delegator from this DispatchContext rather than the one named in the GenericValue
                
                // maintain the original timestamps when doing storage of synced data, by default with will update the timestamps to now
                valueToCreate.setIsFromEntitySync(true);

                // check to make sure all foreign keys are created; if not create dummy values as place holders
                valueToCreate.checkFks(true);

                GenericValue existingValue = delegator.findByPrimaryKey(valueToCreate.getPrimaryKey());
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
            Iterator valueToStoreIter = valuesToStore.iterator();
            while (valueToStoreIter.hasNext()) {
                GenericValue valueToStore = (GenericValue) valueToStoreIter.next();
                // to store check if exists (find by pk), if not insert; if exists check lastUpdatedStamp: if null or before the candidate value insert, otherwise don't insert
                
                // maintain the original timestamps when doing storage of synced data, by default with will update the timestamps to now
                valueToStore.setIsFromEntitySync(true);

                // check to make sure all foreign keys are created; if not create dummy values as place holders
                valueToStore.checkFks(true);

                GenericValue existingValue = delegator.findByPrimaryKey(valueToStore.getPrimaryKey());
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
            Iterator keyToRemoveIter = keysToRemove.iterator();
            while (keyToRemoveIter.hasNext()) {
                GenericEntity pkToRemove = (GenericEntity) keyToRemoveIter.next();
                
                // check to see if it exists, if so remove and count, if not just count already removed
                // always do a removeByAnd, if it was a removeByAnd great, if it was a removeByPrimaryKey, this will also work and save us a query
                pkToRemove.setIsFromEntitySync(true);
                int numRemByAnd = delegator.removeByAnd(pkToRemove.getEntityName(), pkToRemove);
                if (numRemByAnd == 0) {
                    toRemoveAlreadyDeleted++;
                } else {
                    toRemoveDeleted++;
                }
            }
            
            Map result = ServiceUtil.returnSuccess();
            result.put("toCreateInserted", new Long(toCreateInserted));
            result.put("toCreateUpdated", new Long(toCreateUpdated));
            result.put("toCreateNotUpdated", new Long(toCreateNotUpdated));
            result.put("toStoreInserted", new Long(toStoreInserted));
            result.put("toStoreUpdated", new Long(toStoreUpdated));
            result.put("toStoreNotUpdated", new Long(toStoreNotUpdated));
            result.put("toRemoveDeleted", new Long(toRemoveDeleted));
            result.put("toRemoveAlreadyDeleted", new Long(toRemoveAlreadyDeleted));
            return result;
        } catch (GenericEntityException e) {
            String errorMsg = "Exception saving Entity Sync Data for entitySyncId [" + entitySyncId + "]: " + e.toString();
            Debug.logError(e, errorMsg, module);
            return ServiceUtil.returnError(errorMsg);
        } catch (Throwable t) {
            String errorMsg = "Error saving Entity Sync Data for entitySyncId [" + entitySyncId + "]: " + t.toString();
            Debug.logError(t, errorMsg, module);
            return ServiceUtil.returnError(errorMsg);
        }
    }

    /**
     * Run Pull Entity Sync - Pull From Remote
     *@param dctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map runPullEntitySync(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        
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
            Map remoteCallContext = new HashMap();
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
                Map result = dispatcher.runSync(remotePullAndReportEntitySyncDataName, remoteCallContext);
                if (ServiceUtil.isError(result)) {
                    String errMsg = "Error calling remote pull and report EntitySync service with name: " + remotePullAndReportEntitySyncDataName;
                    return ServiceUtil.returnError(errMsg, null, null, result);
                }
                
                startDate = (Timestamp) result.get("startDate");
                
                try {
                    // store data returned, get results (just call storeEntitySyncData locally, get the numbers back and boom shakalaka)
                    
                    // anything to store locally?
                    if (startDate != null && (!UtilValidate.isEmpty((Collection) result.get("valuesToCreate")) || 
                            !UtilValidate.isEmpty((Collection) result.get("valuesToStore")) ||
                            !UtilValidate.isEmpty((Collection) result.get("keysToRemove")))) {
                        
                        // yep, we got more data
                        gotMoreData = true;

                        // at least one of the is not empty, make sure none of them are null now too...
                        List valuesToCreate = (List) result.get("valuesToCreate");
                        if (valuesToCreate == null) valuesToCreate = Collections.EMPTY_LIST;
                        List valuesToStore = (List) result.get("valuesToStore");
                        if (valuesToStore == null) valuesToStore = Collections.EMPTY_LIST;
                        List keysToRemove = (List) result.get("keysToRemove");
                        if (keysToRemove == null) keysToRemove = Collections.EMPTY_LIST;
                        
                        Map callLocalStoreContext = UtilMisc.toMap("entitySyncId", entitySyncId, "delegatorName", context.get("localDelegatorName"),
                                "valuesToCreate", valuesToCreate, "valuesToStore", valuesToStore, 
                                "keysToRemove", keysToRemove);
                        
                        callLocalStoreContext.put("userLogin", context.get("userLogin"));
                        Map storeResult = dispatcher.runSync("storeEntitySyncData", callLocalStoreContext);
                        if (ServiceUtil.isError(storeResult)) {
                            String errMsg = "Error calling service to store data locally";
                            return ServiceUtil.returnError(errMsg, null, null, storeResult);
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
                    String errMsg = "Error calling service to store data locally: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    return ServiceUtil.returnError(errMsg);
                }
            } catch (GenericServiceException e) {
                String errMsg = "Exception calling remote pull and report EntitySync service with name: " + remotePullAndReportEntitySyncDataName + "; " + e.toString();
                Debug.logError(e, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            } catch (Throwable t) {
                String errMsg = "Error calling remote pull and report EntitySync service with name: " + remotePullAndReportEntitySyncDataName + "; " + t.toString();
                Debug.logError(t, errMsg, module);
                return ServiceUtil.returnError(errMsg);
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
    public static Map pullAndReportEntitySyncData(DispatchContext dctx, Map context) {
        EntitySyncContext esc = null;
        try {
            esc = new EntitySyncContext(dctx, context);
            
            Debug.logInfo("Doing pullAndReportEntitySyncData for entitySyncId=" + esc.entitySyncId + ", currentRunStartTime=" + esc.currentRunStartTime + ", currentRunEndTime=" + esc.currentRunEndTime, module);
            
            if ("Y".equals(esc.entitySync.get("forPushOnly"))) {
                return ServiceUtil.returnError("Cannot do Entity Sync Pull because entitySyncId [] is set for Push Only.");
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
                ArrayList valuesToCreate = esc.assembleValuesToCreate();
                // ===== UPDATES =====
                ArrayList valuesToStore = esc.assembleValuesToStore();
                // ===== DELETES =====
                List keysToRemove = esc.assembleKeysToRemove();
                
                esc.setTotalRowCounts(valuesToCreate, valuesToStore, keysToRemove);

                if (Debug.infoOn()) Debug.logInfo("Service pullAndReportEntitySyncData returning - [" + valuesToCreate.size() + "] to create; [" + valuesToStore.size() + "] to store; [" + keysToRemove.size() + "] to remove; [" + esc.totalRowsPerSplit + "] total rows per split.", module); 
                if (esc.totalRowsPerSplit > 0) {
                    // stop if we found some data, otherwise look and try again
                    Map result = ServiceUtil.returnSuccess();
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
            if (!esc.hasMoreTimeToSync() ) {
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

    public static Map runOfflineEntitySync(DispatchContext dctx, Map context) {
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

                ArrayList valuesToCreate = esc.assembleValuesToCreate();
                ArrayList valuesToStore = esc.assembleValuesToStore();
                List keysToRemove = esc.assembleKeysToRemove();

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

    public static Map loadOfflineSyncData(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericDelegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String fileName = (String) context.get("xmlFileName");

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
                return ServiceUtil.returnError("EntitySync XML document (" + fileName + ") is not valid!");
            }

            List syncElements = UtilXml.childElementList(xmlSyncDoc.getDocumentElement());
            if (syncElements != null) {
                Iterator i = syncElements.iterator();
                while (i.hasNext()) {
                    Element entitySync = (Element) i.next();
                    String entitySyncId = entitySync.getAttribute("entitySyncId");
                    String startTime = entitySync.getAttribute("lastSuccessfulSynchTime");

                    String createString = UtilXml.childElementValue(entitySync, "values-to-create");
                    String storeString = UtilXml.childElementValue(entitySync, "values-to-store");
                    String removeString = UtilXml.childElementValue(entitySync, "keys-to-remove");

                    // de-serialize the value lists
                    try {
                        List valuesToCreate = (List) XmlSerializer.deserialize(createString, delegator);
                        List valuesToStore = (List) XmlSerializer.deserialize(storeString, delegator);
                        List keysToRemove = (List) XmlSerializer.deserialize(removeString, delegator);

                        Map storeContext = UtilMisc.toMap("entitySyncId", entitySyncId, "valuesToCreate", valuesToCreate,
                                "valuesToStore", valuesToStore, "keysToRemove", keysToRemove, "userLogin", userLogin);

                        // store the value(s)
                        Map storeResult = dispatcher.runSync("storeEntitySyncData", storeContext);
                        if (ServiceUtil.isError(storeResult)) {
                            throw new Exception(ServiceUtil.getErrorMessage(storeResult));
                        }

                        // TODO create a response document to send back to the initial sync machine
                    } catch (Exception e) {
                        return ServiceUtil.returnError("Unable to load EntitySync XML [" + entitySyncId + "] - Problem at '" +
                                    startTime + "' Error: " + e.getMessage());
                    }
                }
            }
        } else {
            return ServiceUtil.returnError("Offline EntitySync XML file not found (" + fileName + ")");
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map updateOfflineEntitySync(DispatchContext dctx, Map context) {
        return ServiceUtil.returnError("Service not yet implemented.");
    }

    /**
     * Clean EntitySyncRemove Info
     *@param dctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map cleanSyncRemoveInfo(DispatchContext dctx, Map context) {
        Debug.logInfo("Running cleanSyncRemoveInfo", module);
        GenericDelegator delegator = dctx.getDelegator();
        
        try {
            // find the largest keepRemoveInfoHours value on an EntitySyncRemove and kill everything before that, if none found default to 10 days (240 hours)
            double keepRemoveInfoHours = 24;
            
            List entitySyncRemoveList = delegator.findAll("EntitySync");
            Iterator entitySyncRemoveIter = entitySyncRemoveList.iterator();
            while (entitySyncRemoveIter.hasNext()) {
                GenericValue entitySyncRemove = (GenericValue) entitySyncRemoveIter.next();
                Double curKrih = entitySyncRemove.getDouble("keepRemoveInfoHours");
                if (curKrih != null) {
                    double curKrihVal = curKrih.doubleValue();
                    if (curKrihVal > keepRemoveInfoHours) {
                        keepRemoveInfoHours = curKrihVal;
                    }
                }
            }
            
            
            int keepSeconds = (int) Math.floor(keepRemoveInfoHours * 60);
            
            Calendar nowCal = Calendar.getInstance();
            nowCal.setTimeInMillis(System.currentTimeMillis());
            nowCal.add(Calendar.SECOND, -keepSeconds);
            Timestamp keepAfterStamp = new Timestamp(nowCal.getTimeInMillis());
            
            int numRemoved = delegator.removeByCondition("EntitySyncRemove", new EntityExpr(ModelEntity.STAMP_TX_FIELD, EntityOperator.LESS_THAN, keepAfterStamp));
            Debug.logInfo("In cleanSyncRemoveInfo removed [" + numRemoved + "] values with TX timestamp before [" + keepAfterStamp + "]", module);
            
            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException e) {
            String errorMsg = "Error cleaning out EntitySyncRemove info: " + e.toString();
            Debug.logError(e, errorMsg, module);
            return ServiceUtil.returnError(errorMsg);
        }
    }
}
