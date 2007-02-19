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
package org.ofbiz.content.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entityext.permission.EntityPermissionChecker;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;


/**
 * ContentPermissionServices Class
 * 
 * Services for granting operation permissions on Content entities in a data-driven manner.
 */
public class ContentPermissionServices {

    public static final String module = ContentPermissionServices.class.getName();


    public ContentPermissionServices() {}

    /**
     * checkContentPermission
     *
     *@param dctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     *
     * This service goes thru a series of test to determine if the user has
     * authority to performed anyone of the passed in target operations.
     *
     * It expects a Content entity in "currentContent" 
     * It expects a list of contentOperationIds in "targetOperationList" rather
     * than a scalar because it is thought that sometimes more than one operation
     * would fit the situation.
     * Similarly, it expects a list of contentPurposeTypeIds in "contentPurposeList".
     * Again, normally there will just be one, but it is possible that a Content 
     * entity could have multiple purposes associated with it.
     * The userLogin GenericValue is also required.
     * A list of roleTypeIds is also possible.
     *
     * The basic sequence of testing events is:
     * First the ContentPurposeOperation table is checked to see if there are any 
     * entries with matching purposes (and operations) with no roleTypeId (ie. _NA_).
     * This is done because it would be the most common scenario and is quick to check.
     *
     * Secondly, the CONTENTMGR permission is checked.
     *
     * Thirdly, the ContentPurposeOperation table is rechecked to see if there are 
     * any conditions with roleTypeIds that match associated ContentRoles tied to the
     * user. 
     * If a Party of "PARTY_GROUP" type is found, the PartyRelationship table is checked
     * to see if the current user is linked to that group.
     *
     * If no match is found to this point and the current Content entity has a value for
     * ownerContentId, then the last step is recusively applied, using the ContentRoles
     * associated with the ownerContent entity.
     */
    public static Map checkContentPermission(DispatchContext dctx, Map context) {
        Debug.logWarning(new Exception(), "This service has been depricated in favor of [genericContentPermission]", module);
        
        Security security = dctx.getSecurity();
        GenericDelegator delegator = dctx.getDelegator();
        String statusId = (String) context.get("statusId");
        String privilegeEnumId = (String) context.get("privilegeEnumId");
        GenericValue content = (GenericValue) context.get("currentContent"); 
        Boolean bDisplayFailCond = (Boolean)context.get("displayFailCond");
        boolean displayFailCond = false;
        if (bDisplayFailCond != null && bDisplayFailCond.booleanValue()) {
             displayFailCond = true;   
        }
                Debug.logInfo("displayFailCond(0):" + displayFailCond, "");
        Boolean bDisplayPassCond = (Boolean)context.get("displayPassCond");
        boolean displayPassCond = false;
        if (bDisplayPassCond != null && bDisplayPassCond.booleanValue()) {
             displayPassCond = true;   
        }
                Debug.logInfo("displayPassCond(0):" + displayPassCond, "");
        Map results  = new HashMap();
        String contentId = null;
        if (content != null)
            contentId = content.getString("contentId");
        GenericValue userLogin = (GenericValue) context.get("userLogin"); 
        String partyId = (String) context.get("partyId");
        if (UtilValidate.isEmpty(partyId)) {
            String passedUserLoginId = (String)context.get("userLoginId");
            if (UtilValidate.isNotEmpty(passedUserLoginId)) {
                try {
                    userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", passedUserLoginId));
                    if (userLogin != null) {
                        partyId = userLogin.getString("partyId");   
                    }
                } catch(GenericEntityException e) {
                    ServiceUtil.returnError(e.getMessage());
                }
            }
        }
        if (UtilValidate.isEmpty(partyId) && userLogin != null) {
            partyId = userLogin.getString("partyId");
        }

 
        // Do entity permission check. This will pass users with administrative permissions.
        boolean passed = false;
        // I realized, belatedly, that I wanted to be able to pass parameters in as
        // strings so this service could be used in an action event directly,
        // so I had to write this code to handle both list and strings
        List passedPurposes = (List) context.get("contentPurposeList"); 
        String contentPurposeString = (String) context.get("contentPurposeString"); 
        //Debug.logInfo("contentPurposeString(b):" + contentPurposeString, "");
        if (UtilValidate.isNotEmpty(contentPurposeString)) {
            List purposesFromString = StringUtil.split(contentPurposeString, "|");
            if (passedPurposes == null) {
                passedPurposes = new ArrayList();
            }
            passedPurposes.addAll(purposesFromString);
        }
        
        EntityPermissionChecker.StdAuxiliaryValueGetter auxGetter = new EntityPermissionChecker.StdAuxiliaryValueGetter("ContentPurpose",  "contentPurposeTypeId", "contentId");
        // Sometimes permissions need to be checked before an entity is created, so 
        // there needs to be a method for setting a purpose list
        auxGetter.setList(passedPurposes);
        //Debug.logInfo("passedPurposes(b):" + passedPurposes, "");
        List targetOperations = (List) context.get("targetOperationList"); 
        //Debug.logInfo("targetOperations(b):" + targetOperations, "");
        String targetOperationString = (String) context.get("targetOperationString"); 
        //Debug.logInfo("targetOperationString(b):" + targetOperationString, "");
        if (UtilValidate.isNotEmpty(targetOperationString)) {
            List operationsFromString = StringUtil.split(targetOperationString, "|");
            if (targetOperations == null) {
                targetOperations = new ArrayList();
            }
            targetOperations.addAll(operationsFromString);
        }
        //Debug.logInfo("targetOperations(c):" + targetOperations, "");
        EntityPermissionChecker.StdPermissionConditionGetter permCondGetter = new EntityPermissionChecker.StdPermissionConditionGetter("ContentPurposeOperation",  "contentOperationId", "roleTypeId", "statusId", "contentPurposeTypeId", "privilegeEnumId");
        permCondGetter.setOperationList(targetOperations);
        
        EntityPermissionChecker.StdRelatedRoleGetter roleGetter = new EntityPermissionChecker.StdRelatedRoleGetter("Content",  "roleTypeId", "contentId", "partyId", "ownerContentId", "ContentRole");
        //Debug.logInfo("targetOperations(b):" + targetOperations, "");
        List passedRoles = (List) context.get("roleTypeList"); 
        if (passedRoles == null) passedRoles = new ArrayList();
        String roleTypeString = (String) context.get("roleTypeString"); 
        if (UtilValidate.isNotEmpty(roleTypeString)) {
            List rolesFromString = StringUtil.split(roleTypeString, "|");
            passedRoles.addAll(rolesFromString);
        }
        roleGetter.setList(passedRoles);
        
        String entityAction = (String) context.get("entityOperation");
        if (entityAction == null) entityAction = "_ADMIN";
        if (userLogin != null && entityAction != null) {
            passed = security.hasEntityPermission("CONTENTMGR", entityAction, userLogin);
        }
        
        StringBuffer errBuf = new StringBuffer();
        String permissionStatus = null;
        List entityIds = new ArrayList();
        if (passed) {
            results.put("permissionStatus", "granted");   
            permissionStatus = "granted";
            if (displayPassCond) {
                 errBuf.append("\n    hasEntityPermission(" + entityAction + "): PASSED" );
            } 
                
        } else {
            if (displayFailCond) {
                 errBuf.append("\n    hasEntityPermission(" + entityAction + "): FAILED" );
            } 

            if (content != null)
                entityIds.add(content);
            String quickCheckContentId = (String) context.get("quickCheckContentId");
            if (UtilValidate.isNotEmpty(quickCheckContentId)) {
               List quickList = StringUtil.split(quickCheckContentId, "|"); 
               if (UtilValidate.isNotEmpty(quickList)) entityIds.addAll(quickList);
            }
            try {
                boolean check = EntityPermissionChecker.checkPermissionMethod(delegator, partyId,  "Content", entityIds, auxGetter, roleGetter, permCondGetter);
                if (check) {
                    results.put("permissionStatus", "granted");
                } else {
                    results.put("permissionStatus", "rejected");
                }
            } catch (GenericEntityException e) {
                ServiceUtil.returnError(e.getMessage());   
            }
            permissionStatus = (String)results.get("permissionStatus");
            errBuf.append("\n    permissionStatus:" );
            errBuf.append(permissionStatus);
        }
            
        if ((permissionStatus.equals("granted") && displayPassCond)
            || (permissionStatus.equals("rejected") && displayFailCond)) {
            // Don't show this if passed on 'hasEntityPermission'
            if (displayFailCond || displayPassCond) {
              if (!passed) {
                 errBuf.append("\n    targetOperations:" );
                 errBuf.append(targetOperations);

                 String errMsg = permCondGetter.dumpAsText();
                 errBuf.append("\n" );
                 errBuf.append(errMsg);
                 errBuf.append("\n    partyId:" );
                 errBuf.append(partyId);
                 errBuf.append("\n    entityIds:" );
                 errBuf.append(entityIds);
                 
                 if (auxGetter != null) {
                     errBuf.append("\n    auxList:" );
                     errBuf.append(auxGetter.getList());
                 }
                 
                 if (roleGetter != null) {
                     errBuf.append("\n    roleList:" );
                     errBuf.append(roleGetter.getList());
                 }
              }
                 
            }
        }
        Debug.logInfo("displayPass/FailCond(0), errBuf:" + errBuf.toString(), "");
        results.put(ModelService.ERROR_MESSAGE, errBuf.toString());
        return results;
    }
    
    public static Map checkAssocPermission(DispatchContext dctx, Map context) {
    
        Map results = new HashMap();
        Security security = dctx.getSecurity();
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Boolean bDisplayFailCond = (Boolean)context.get("displayFailCond");
        String contentIdFrom = (String) context.get("contentIdFrom");
        String contentIdTo = (String) context.get("contentIdTo");
        GenericValue userLogin = (GenericValue) context.get("userLogin"); 
        String entityAction = (String) context.get("entityOperation");
        if (entityAction == null) entityAction = "_ADMIN";
        List roleIds = null;
        String permissionStatus = null;
    
        GenericValue contentTo = null;
        GenericValue contentFrom = null;
        try {
            contentTo = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", contentIdTo) );
            contentFrom = delegator.findByPrimaryKeyCache("Content",  UtilMisc.toMap("contentId", contentIdFrom) );
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Error in retrieving content To or From. " + e.getMessage());
        }
        if (contentTo == null || contentFrom == null) {
            return ServiceUtil.returnError("contentTo[" + contentTo + "]/From[" + contentFrom + "] is null. ");
        }
        Map resultsMap = null;
        boolean isMatch = false;
    
        boolean isMatchTo = false;
        boolean isMatchFrom = false;
        Map permResults = new HashMap();
        String skipPermissionCheck = null;
    
        if (skipPermissionCheck == null
            || skipPermissionCheck.length() == 0
            || (!skipPermissionCheck.equalsIgnoreCase("true") && !skipPermissionCheck.equalsIgnoreCase("granted"))) {
            // Use the purposes from the from entity for both cases.
            List relatedPurposes = EntityPermissionChecker.getRelatedPurposes(contentFrom, null);
            List relatedPurposesTo = EntityPermissionChecker.getRelatedPurposes(contentTo, relatedPurposes);
            Map serviceInMap = new HashMap();
            serviceInMap.put("userLogin", userLogin);
            serviceInMap.put("targetOperationList", UtilMisc.toList("CONTENT_LINK_TO"));
            serviceInMap.put("contentPurposeList", relatedPurposesTo);
            serviceInMap.put("currentContent", contentTo);
            serviceInMap.put("displayFailCond", bDisplayFailCond);
    
            try {
                permResults = dispatcher.runSync("checkContentPermission", serviceInMap);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem checking permissions", "ContentServices");
            }
            permissionStatus = (String)permResults.get("permissionStatus");
            if(permissionStatus == null || !permissionStatus.equals("granted") ) {
                if (bDisplayFailCond != null && bDisplayFailCond.booleanValue()) {
                     String errMsg = (String)permResults.get(ModelService.ERROR_MESSAGE);
                     results.put(ModelService.ERROR_MESSAGE, errMsg);
                }
                return results;
            }
            serviceInMap.put("currentContent", contentFrom);
            serviceInMap.put("targetOperationList", UtilMisc.toList("CONTENT_LINK_FROM"));
            serviceInMap.put("contentPurposeList", relatedPurposes);
            try {
                permResults = dispatcher.runSync("checkContentPermission", serviceInMap);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem checking permissions", "ContentServices");
            }
            permissionStatus = (String)permResults.get("permissionStatus");
            if(permissionStatus != null && permissionStatus.equals("granted") ) {
                results.put("permissionStatus", "granted");   
            } else {
                if (bDisplayFailCond != null && bDisplayFailCond.booleanValue()) {
                     String errMsg = (String)permResults.get(ModelService.ERROR_MESSAGE);
                     results.put(ModelService.ERROR_MESSAGE, errMsg);
                }
            }
        } else {
            results.put("permissionStatus", "granted");   
        }
        return results;
    }
    
}
