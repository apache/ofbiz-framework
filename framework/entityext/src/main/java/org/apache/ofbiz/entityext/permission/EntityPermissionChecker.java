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
package org.apache.ofbiz.entityext.permission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.ServiceUtil;
import org.w3c.dom.Element;


/**
 * EntityPermissionChecker Class
 * Services for granting operation permissions on Content entities in a data-driven manner.
 */
public class EntityPermissionChecker {

    public static final String module = EntityPermissionChecker.class.getName();

    protected FlexibleStringExpander entityIdExdr;
    protected FlexibleStringExpander entityNameExdr;
    protected boolean displayFailCond;
    protected List<String> targetOperationList;
    protected PermissionConditionGetter permissionConditionGetter;
    protected RelatedRoleGetter relatedRoleGetter;
    protected AuxiliaryValueGetter auxiliaryValueGetter;

    public EntityPermissionChecker(Element element) {
        this.entityNameExdr = FlexibleStringExpander.getInstance(element.getAttribute("entity-name"));
        this.entityIdExdr = FlexibleStringExpander.getInstance(element.getAttribute("entity-id"));
        this.displayFailCond = "true".equals(element.getAttribute("display-fail-cond"));
        Element permissionConditionElement = UtilXml.firstChildElement(element, "permission-condition-getter");
        if (permissionConditionElement == null) {
            permissionConditionGetter = new StdPermissionConditionGetter();
        } else {
            permissionConditionGetter = new StdPermissionConditionGetter(permissionConditionElement);
        }
        Element auxiliaryValueElement = UtilXml.firstChildElement(element, "auxiliary-value-getter");
        if (auxiliaryValueElement == null) {
            auxiliaryValueGetter = new StdAuxiliaryValueGetter();
        } else {
            auxiliaryValueGetter = new StdAuxiliaryValueGetter(auxiliaryValueElement);
        }
        Element relatedRoleElement = UtilXml.firstChildElement(element, "related-role-getter");
        if (relatedRoleElement == null) {
            relatedRoleGetter = new StdRelatedRoleGetter();
        } else {
            relatedRoleGetter = new StdRelatedRoleGetter(relatedRoleElement);
        }
        String targetOperationString = element.getAttribute("target-operation");
        if (UtilValidate.isNotEmpty(targetOperationString)) {
            List<String> operationsFromString = StringUtil.split(targetOperationString, "|");
            if (targetOperationList == null) {
                targetOperationList = new ArrayList<>();
            }
            targetOperationList.addAll(operationsFromString);
        }
        permissionConditionGetter.setOperationList(targetOperationList);

    }

    public boolean runPermissionCheck(Map<String, ?> context) {

        boolean passed = false;
        String idString = entityIdExdr.expandString(context);
        List<String> entityIdList = null;
        if (UtilValidate.isNotEmpty(idString)) {
            entityIdList = StringUtil.split(idString, "|");
        } else {
            entityIdList = new LinkedList<>();
        }
        String entityName = entityNameExdr.expandString(context);
        HttpServletRequest request = (HttpServletRequest)context.get("request");
        GenericValue userLogin = null;
        String partyId = null;
        Delegator delegator = null;
        if (request != null) {
            HttpSession session = request.getSession();
            userLogin = (GenericValue)session.getAttribute("userLogin");
            if (userLogin != null) {
                partyId = userLogin.getString("partyId");
            }
           delegator = (Delegator)request.getAttribute("delegator");
        }

        if (auxiliaryValueGetter != null) auxiliaryValueGetter.clearList();
        if (relatedRoleGetter != null) relatedRoleGetter.clearList();
        try {
            permissionConditionGetter.init(delegator);
            passed = checkPermissionMethod(delegator, partyId,  entityName, entityIdList, auxiliaryValueGetter, relatedRoleGetter, permissionConditionGetter);
            if (!passed && displayFailCond) {
                 String errMsg =  "Permission is denied. \nThese are the conditions of which one must be met:\n"
                     + permissionConditionGetter.dumpAsText();
                 List<Object> errorMessageList = UtilGenerics.cast(context.get("errorMessageList"));
                 errorMessageList.add(errMsg);
            }
        } catch (GenericEntityException e) {
            throw new RuntimeException(e.getMessage());
        }
        return passed;
    }

    public static Map<String, Object> checkPermission(GenericValue content, String statusId, GenericValue userLogin, List<String> passedPurposes, List<String> targetOperations, List<String> passedRoles, Delegator delegator , Security security, String entityAction) {
         String privilegeEnumId = null;
         return checkPermission(content, statusId, userLogin, passedPurposes, targetOperations, passedRoles, delegator, security, entityAction, privilegeEnumId, null);
    }

    public static Map<String, Object> checkPermission(GenericValue content, String statusId,
                                  GenericValue userLogin, List<String> passedPurposes,
                                  List<String> targetOperations, List<String> passedRoles,
                                  Delegator delegator ,
                                  Security security, String entityAction,
                                  String privilegeEnumId, String quickCheckContentId) {
         List<String> statusList = null;
         if (statusId != null) {
             statusList = StringUtil.split(statusId, "|");
         }
         return checkPermission(content, statusList, userLogin, passedPurposes, targetOperations, passedRoles, delegator, security, entityAction, privilegeEnumId, quickCheckContentId);
    }

    public static Map<String, Object> checkPermission(GenericValue content, List<String> statusList,
                                  GenericValue userLogin, List<String> passedPurposes,
                                  List<String> targetOperations, List<String> passedRoles,
                                  Delegator delegator ,
                                  Security security, String entityAction,
                                  String privilegeEnumId) {
         return checkPermission(content, statusList, userLogin, passedPurposes, targetOperations, passedRoles, delegator, security, entityAction, privilegeEnumId, null);
    }

    public static Map<String, Object> checkPermission(GenericValue content, List<String> statusList,
                                  GenericValue userLogin, List<String> passedPurposes,
                                  List<String> targetOperations, List<String> passedRoles,
                                  Delegator delegator ,
                                  Security security, String entityAction,
                                  String privilegeEnumId, String quickCheckContentId) {

        List<Object> entityIds = new LinkedList<>();
        if (content != null) entityIds.add(content);
        if (UtilValidate.isNotEmpty(quickCheckContentId)) {
            List<String> quickList = StringUtil.split(quickCheckContentId, "|");
            if (UtilValidate.isNotEmpty(quickList)) entityIds.addAll(quickList);
        }
        Map<String, Object> results  = new HashMap<>();
        boolean passed = false;
        if (userLogin != null && entityAction != null) {
            passed = security.hasEntityPermission("CONTENTMGR", entityAction, userLogin);
        }
        if (passed) {
            results.put("permissionStatus", "granted");
            return results;
        }
        try {
            boolean check  = checkPermissionMethod(delegator, userLogin, targetOperations, "Content", entityIds, passedPurposes, null, privilegeEnumId);
            if (check) {
                results.put("permissionStatus", "granted");
            } else {
                results.put("permissionStatus", "rejected");
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return results;
    }


    public static boolean checkPermissionMethod(Delegator delegator, GenericValue userLogin, List<String> targetOperationList, String entityName, List<? extends Object> entityIdList, List<String> purposeList, List<String> roleList, String privilegeEnumId) throws GenericEntityException {
        boolean passed = false;

        String lcEntityName = entityName.toLowerCase();
        String userLoginId = null;
        String partyId = null;
        if (userLogin != null) {
            userLoginId = userLogin.getString("userLoginId");
            partyId = userLogin.getString("partyId");
        }
        boolean hasRoleOperation = false;
        if (!(targetOperationList == null) && userLoginId != null) {
            hasRoleOperation = checkHasRoleOperations(partyId, targetOperationList, delegator);
        }
        if (hasRoleOperation) {
            return true;
        }
        ModelEntity modelEntity = delegator.getModelEntity(entityName);
        boolean hasStatusField = false;
        if (modelEntity.getField("statusId") != null)
            hasStatusField = true;

        boolean hasPrivilegeField = false;
        if (modelEntity.getField("privilegeEnumId") != null)
            hasPrivilegeField = true;

        
        ModelEntity modelOperationEntity = delegator.getModelEntity(entityName + "PurposeOperation");
        if (modelOperationEntity == null) {
            modelOperationEntity = delegator.getModelEntity(entityName + "Operation");
        }

        if (modelOperationEntity == null) {
            Debug.logError("No operation entity found for " + entityName, module);
            throw new RuntimeException("No operation entity found for " + entityName);
        }

        boolean hasPurposeOp = false;
        if (modelOperationEntity.getField(lcEntityName + "PurposeTypeId") != null)
            hasPurposeOp = true;
        boolean hasStatusOp = false;
        if (modelOperationEntity.getField("statusId") != null)
            hasStatusOp = true;
        boolean hasPrivilegeOp = false;
        if (modelOperationEntity.getField("privilegeEnumId") != null)
             hasPrivilegeOp = true;

        // Get all the condition operations that could apply, rather than having to go thru
        // entire table each time.
        //List condList = new LinkedList();
        //Iterator iterType = targetOperationList.iterator();
        //while (iterType.hasNext()) {
        //    String op = (String)iterType.next();
        //    condList.add(EntityCondition.makeCondition(lcEntityName + "OperationId", op));
        //}
        //EntityCondition opCond = EntityCondition.makeCondition(condList, EntityOperator.OR);


        List<GenericValue> targetOperationEntityList = EntityQuery.use(delegator)
                                                                  .from(modelOperationEntity.getEntityName())
                                                                  .where(EntityCondition.makeCondition(lcEntityName + "OperationId", EntityOperator.IN, targetOperationList))
                                                                  .cache(true)
                                                                  .queryList();
        Map<String, GenericValue> entities = new HashMap<>();
        String pkFieldName = modelEntity.getFirstPkFieldName();

        //TODO: privilegeEnumId test
        /*
        if (hasPrivilegeOp && hasPrivilegeField) {
            int privilegeEnumSeq = -1;

            if (UtilValidate.isNotEmpty(privilegeEnumId)) {
                GenericValue privEnum = EntityQuery.use(delegator).from("Enumeration").where("enumId", privilegeEnumId).cache().queryOne();
                if (privEnum != null) {
                    String sequenceId = privEnum.getString("sequenceId");
                    try {
                        privilegeEnumSeq = Integer.parseInt(sequenceId);
                    } catch (NumberFormatException e) {
                        // just leave it at -1
                    }
                }
            }
            boolean thisPassed = true;
            Iterator iter = entityIdList.iterator();
            while (iter.hasNext()) {
                GenericValue entity = getNextEntity(delegator, entityName, pkFieldName, iter.next(), entities);
                if (entity == null) continue;

                String entityId = entity.getString(pkFieldName);
                String targetPrivilegeEnumId = entity.getString("privilegeEnumId");
                if (UtilValidate.isNotEmpty(targetPrivilegeEnumId)) {
                    int targetPrivilegeEnumSeq = -1;
                    GenericValue privEnum = EntityQuery.use(delegator).from("Enumeration").where("enumId", privilegeEnumId).cache().queryOne();
                    if (privEnum != null) {
                        String sequenceId = privEnum.getString("sequenceId");
                        try {
                            targetPrivilegeEnumSeq = Integer.parseInt(sequenceId);
                        } catch (NumberFormatException e) {
                            // just leave it at -1
                        }
                        if (targetPrivilegeEnumSeq > privilegeEnumSeq) {
                            return false;
                        }
                    }
                }
                entities.put(entityId, entity);
            }
        }
        */

        // check permission for each id in passed list until success.
        // Note that "quickCheck" id come first in the list
        // Check with no roles or purposes on the chance that the permission fields contain _NA_ s.
        
        Map<String, List<String>> purposes = new HashMap<>();
        Map<String, List<String>> roles = new HashMap<>();
        //List purposeList = null;
        //List roleList = null;
        for (Object id: entityIdList) {
               GenericValue entity = getNextEntity(delegator, entityName, pkFieldName, id, entities);
             if (entity == null) continue;

            String statusId = null;
            if (hasStatusOp && hasStatusField) {
                statusId = entity.getString("statusId");
            }

            if (hasPrivilegeOp && hasPrivilegeField) {
                privilegeEnumId = entity.getString("privilegeEnumId");
                getPrivilegeEnumSeq(delegator, privilegeEnumId);
            }

            passed = hasMatch(entityName, targetOperationEntityList, roleList, hasPurposeOp, purposeList, hasStatusOp, statusId);
            if (passed) {
                break;
            }
       }

        if (passed) {
            return true;
        }

        if (hasPurposeOp) {
            // Check with just purposes next.
            for (Object id: entityIdList) {
                GenericValue entity = getNextEntity(delegator, entityName, pkFieldName, id, entities);
                if (entity == null) continue;

                String entityId = entity.getString(pkFieldName);
                purposeList = getRelatedPurposes(entity, null);
                String statusId = null;
                if (hasStatusOp && hasStatusField) {
                    statusId = entity.getString("statusId");
                }

                if (purposeList.size() > 0) {
                    passed = hasMatch(entityName, targetOperationEntityList, roleList, hasPurposeOp, purposeList, hasStatusOp, statusId);
                }
                if (passed) {
                    break;
                }
                purposes.put(entityId, purposeList);
            }
        }

        if (passed) return true;

        if (userLogin == null) return false;

        // Check with roles.
        for (Object id: entityIdList) {
            GenericValue entity = getNextEntity(delegator, entityName, pkFieldName, id, entities);
            if (entity == null) continue;
            String entityId = entity.getString(pkFieldName);
            List<String> tmpPurposeList = purposes.get(entityId);
            if (purposeList != null) {
                if (tmpPurposeList != null) {
                    purposeList.addAll(tmpPurposeList);
                }
            } else {
                purposeList = tmpPurposeList;
            }

            List<String> tmpRoleList = getUserRoles(entity, userLogin, delegator);
            if (roleList != null) {
                if (tmpRoleList != null) {
                    roleList.addAll(tmpRoleList);
                }
            } else {
                roleList = tmpRoleList;
            }

            String statusId = null;
            if (hasStatusOp && hasStatusField) {
                statusId = entity.getString("statusId");
            }

            passed = hasMatch(entityName, targetOperationEntityList, roleList, hasPurposeOp, purposeList, hasStatusOp, statusId);
            if (passed) {
                break;
            }
            roles.put(entityId, roleList);
        }

        if (passed)
            return true;

        // Follow ownedEntityIds
        if (modelEntity.getField("owner" + entityName + "Id") != null) {
            for (Object id: entityIdList) {
                GenericValue entity = getNextEntity(delegator, entityName, pkFieldName, id, entities);
                if (entity == null) continue;

                String entityId = entity.getString(pkFieldName);
                List<String> ownedContentIdList = new LinkedList<>();
                getEntityOwners(delegator, entity, ownedContentIdList, "Content", "ownerContentId");

                List<String> ownedContentRoleIds = getUserRolesFromList(delegator, ownedContentIdList, partyId, "contentId", "partyId", "roleTypeId", "ContentRole");
                String statusId = null;
                if (hasStatusOp && hasStatusField) {
                    statusId = entity.getString("statusId");
                }

                purposeList = purposes.get(entityId);
                passed = hasMatch(entityName, targetOperationEntityList, ownedContentRoleIds, hasPurposeOp, purposeList, hasStatusOp, statusId);
                if (passed) break;

                /*
                   String ownedEntityId = entity.getString("owner" + entityName + "Id");
                   GenericValue ownedEntity = delegator.findOne(entityName,UtilMisc.toMap(pkFieldName, ownedEntityId), true);
                   while (ownedEntity != null) {
                       if (!alreadyCheckedIds.contains(ownedEntityId)) {
                        // Decided to let the original purposes only be used in permission checking
                        //
                        //purposeList = (List)purposes.get(entityId);
                        //purposeList = getRelatedPurposes(ownedEntity, purposeList);
                        roleList = getUserRoles(ownedEntity, userLogin, delegator);

                        String statusId = null;
                        if (hasStatusOp && hasStatusField) {
                            statusId = entity.getString("statusId");
                        }

                        passed = hasMatch(entityName, targetOperationEntityList, roleList, hasPurposeOp, purposeList, hasStatusOp, statusId);
                        if (passed)
                            break;
                        alreadyCheckedIds.add(ownedEntityId);
                       //purposes.put(ownedEntityId, purposeList);
                        //roles.put(ownedEntityId, roleList);
                           ownedEntityId = ownedEntity.getString("owner" + entityName + "Id");
                           ownedEntity = delegator.findOne(entityName,UtilMisc.toMap(pkFieldName, ownedEntityId), true);
                       } else {
                          ownedEntity = null;
                       }
                   }
                   if (passed)
                       break;
                       */
            }
        }


        /* seems like repeat
        // Check parents
        iter = entityIdList.iterator();
        while (iter.hasNext()) {
            String entityId = (String)iter.next();
               GenericValue entity = (GenericValue)entities.get(entityId);
            purposeList = (List)purposes.get(entityId);
            roleList = getUserRoles(entity, userLogin, delegator);

            String statusId = null;
            if (hasStatusOp && hasStatusField) {
                statusId = entity.getString("statusId");
            }
            String targetPrivilegeEnumId = null;
            if (hasPrivilegeOp && hasPrivilegeField) {
                targetPrivilegeEnumId = entity.getString("privilegeEnumId");
            }

            passed = hasMatch(entityName, targetOperationEntityList, roleList, hasPurposeOp, purposeList, hasStatusOp, statusId);
            if (passed)
                break;
            alreadyCheckedIds.add(entityId);
        }
        */

        return passed;
    }
    public static boolean checkPermissionMethod(Delegator delegator, String partyId,  String entityName, List<? extends Object> entityIdList, AuxiliaryValueGetter auxiliaryValueGetter, RelatedRoleGetter relatedRoleGetter, PermissionConditionGetter permissionConditionGetter) throws GenericEntityException {

        permissionConditionGetter.init(delegator);
        if (Debug.verboseOn()) Debug.logVerbose(permissionConditionGetter.dumpAsText(), module);
        boolean passed = false;

        boolean checkAncestors = false;
        boolean hasRoleOperation =  checkHasRoleOperations(partyId, permissionConditionGetter, delegator);
        if (hasRoleOperation) {
            return true;
        }
        ModelEntity modelEntity = delegator.getModelEntity(entityName);

        if (relatedRoleGetter != null) {
            if (UtilValidate.isNotEmpty(partyId)) {
                relatedRoleGetter.setList(UtilMisc.toList("LOGGEDIN"));
            }
        }

        // check permission for each id in passed list until success.
        // Note that "quickCheck" id come first in the list
        // Check with no roles or purposes on the chance that the permission fields contain _NA_ s.
        String pkFieldName = modelEntity.getFirstPkFieldName();
        
        Map<String, GenericValue> entities = new HashMap<>();
        //List purposeList = null;
        //List roleList = null;
        for (Object id: entityIdList) {
            GenericValue entity = getNextEntity(delegator, entityName, pkFieldName, id, entities);
            if (entity == null) continue;
            checkAncestors = false;
            passed = hasMatch(entity, permissionConditionGetter, relatedRoleGetter, null, partyId, checkAncestors);
            if (passed) {
                break;
            }
       }

        if (passed) {
            return true;
        }

        if (auxiliaryValueGetter != null) {
            //if (Debug.infoOn()) Debug.logInfo(auxiliaryValueGetter.dumpAsText(), module);
            // Check with just purposes next.
            for (Object id: entityIdList) {
                GenericValue entity = getNextEntity(delegator, entityName, pkFieldName, id, entities);
                if (entity == null) continue;
                checkAncestors = false;
                passed = hasMatch(entity, permissionConditionGetter, relatedRoleGetter, auxiliaryValueGetter, partyId, checkAncestors);

                if (passed) {
                    break;
                }
            }
        }

        if (passed) return true;

        // TODO: need to return some information here about why it failed
        if (partyId == null) return false;

        // Check with roles.
        if (relatedRoleGetter != null) {
            for (Object id: entityIdList) {
                GenericValue entity = getNextEntity(delegator, entityName, pkFieldName, id, entities);
                if (entity == null) continue;
                checkAncestors = false;
                passed = hasMatch(entity, permissionConditionGetter, relatedRoleGetter, auxiliaryValueGetter, partyId, checkAncestors);

                if (passed) {
                    break;
                }
            }
        }

        if (passed)
            return true;

        if (relatedRoleGetter != null) {
            for (Object id: entityIdList) {
                GenericValue entity = getNextEntity(delegator, entityName, pkFieldName, id, entities);
                if (entity == null) continue;
                checkAncestors = true;
                passed = hasMatch(entity, permissionConditionGetter, relatedRoleGetter, auxiliaryValueGetter, partyId, checkAncestors);

                if (passed) {
                    break;
                }
            }
        }


        return passed;
    }


    public static GenericValue getNextEntity(Delegator delegator, String entityName, String pkFieldName, Object obj, Map<String, GenericValue> entities) throws GenericEntityException {
        GenericValue entity = null;
        if (obj instanceof String) {
            String entityId  = (String)obj;
            if (entities != null) entity = entities.get(entityId);

            if (entity == null) entity = EntityQuery.use(delegator).from(entityName).where(pkFieldName, entityId).cache(true).queryOne();
        } else if (obj instanceof GenericValue) {
            entity = (GenericValue)obj;
        }
        return entity;
    }

    public static boolean checkHasRoleOperations(String partyId,  PermissionConditionGetter permissionConditionGetter , Delegator delegator) {
        List<String> targetOperations = permissionConditionGetter.getOperationList();
        return checkHasRoleOperations(partyId, targetOperations, delegator);
    }

    public static boolean checkHasRoleOperations(String partyId,  List<String> targetOperations, Delegator delegator) {
        //if (Debug.infoOn()) Debug.logInfo("targetOperations:" + targetOperations, module);
        //if (Debug.infoOn()) Debug.logInfo("userLoginId:" + userLoginId, module);
        if (targetOperations == null) return false;

        if (partyId != null && targetOperations.contains("HAS_USER_ROLE")) return true;

        boolean hasRoleOperation = false;
        boolean hasNeed = false;
        List<String> newHasRoleList = new LinkedList<>();
        for (String roleOp: targetOperations) {
            int idx1 = roleOp.indexOf("HAS_");
            if (idx1 == 0) {
                String roleOp1 = roleOp.substring(4); // lop off "HAS_"
                int idx2 = roleOp1.indexOf("_ROLE");
                if (idx2 == roleOp1.length() - 5) {
                    String roleOp2 = roleOp1.substring(0, roleOp1.indexOf("_ROLE") - 1); // lop off "_ROLE"
                    //if (Debug.infoOn()) Debug.logInfo("roleOp2:" + roleOp2, module);
                    newHasRoleList.add(roleOp2);
                    hasNeed = true;
                }
            }
        }

        if (hasNeed) {
            try {
                if (UtilValidate.isNotEmpty(partyId)) {
                    List<GenericValue> partyRoleList = EntityQuery.use(delegator).from("PartyRole").where("partyId", partyId).cache(true).queryList();
                    for (GenericValue partyRole: partyRoleList) {
                        String roleTypeId = partyRole.getString("roleTypeId");
                        for (String thisRole: newHasRoleList) {
                            if (roleTypeId.indexOf(thisRole) >= 0) {
                                hasRoleOperation = true;
                                break;
                            }
                        }
                        if (hasRoleOperation)
                            break;
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return hasRoleOperation;
            }
        }
        return hasRoleOperation;
    }

    public static boolean hasMatch(String entityName, List<GenericValue> targetOperations, List<String> roles, boolean hasPurposeOp, List<String> purposes, boolean hasStatusOp, String targStatusId) {
        boolean isMatch = false;
        
    //    if (UtilValidate.isNotEmpty(targPrivilegeEnumId) && !targPrivilegeEnumId.equals("_NA_") && !targPrivilegeEnumId.equals("_00_")) {
            // need to do a lookup here to find the seq value of targPrivilegeEnumId.
            // The lookup could be a static store or it could be done on Enumeration entity.
    //    }
        String lcEntityName = entityName.toLowerCase();
        for (GenericValue targetOp: targetOperations) {
            String testRoleTypeId = (String)targetOp.get("roleTypeId");
            String testContentPurposeTypeId = null;
            if (hasPurposeOp)
                testContentPurposeTypeId = (String)targetOp.get(lcEntityName + "PurposeTypeId");
            String testStatusId = null;
            if (hasStatusOp)
                testStatusId = (String)targetOp.get("statusId");
            //String testPrivilegeEnumId = null;
            //if (hasPrivilegeOp)
                //testPrivilegeEnumId = (String)targetOp.get("privilegeEnumId");
            //int testPrivilegeSeq = 0;

            boolean purposesCond = (!hasPurposeOp || (purposes != null && purposes.contains(testContentPurposeTypeId)) || testContentPurposeTypeId.equals("_NA_"));
            boolean statusCond = (!hasStatusOp || testStatusId.equals("_NA_") || (targStatusId != null && targStatusId.equals(testStatusId)));
            //boolean privilegeCond = (!hasPrivilegeOp || testPrivilegeEnumId.equals("_NA_") || testPrivilegeSeq <= targPrivilegeSeq || testPrivilegeEnumId.equals(targPrivilegeEnumId));
            boolean roleCond = (testRoleTypeId.equals("_NA_") || (roles != null && roles.contains(testRoleTypeId)));


            if (purposesCond && statusCond && roleCond) {

                    isMatch = true;
                    break;
            }
        }
        return isMatch;
    }

    public static boolean hasMatch(GenericValue entity, PermissionConditionGetter permissionConditionGetter, RelatedRoleGetter relatedRoleGetter, AuxiliaryValueGetter auxiliaryValueGetter, String partyId, boolean checkAncestors) throws GenericEntityException {

        ModelEntity modelEntity = entity.getModelEntity();
        Delegator delegator = entity.getDelegator();
        String pkFieldName = modelEntity.getFirstPkFieldName();
        String entityId = entity.getString(pkFieldName);
        if (Debug.verboseOn()) Debug.logVerbose("\n\nIN hasMatch: entityId:" + entityId + " partyId:" + partyId + " checkAncestors:" + checkAncestors, module);
        boolean isMatch = false;
        permissionConditionGetter.restart();
        List<String> auxiliaryValueList = null;
        if (auxiliaryValueGetter != null) {
           auxiliaryValueGetter.init(delegator, entityId);
           auxiliaryValueList =   auxiliaryValueGetter.getList();
            if (Debug.verboseOn()) Debug.logVerbose(auxiliaryValueGetter.dumpAsText(), module);
        } else {
            if (Debug.verboseOn()) Debug.logVerbose("NO AUX GETTER", module);
        }
        List<String> roleValueList = null;
        if (relatedRoleGetter != null) {
            if (checkAncestors) {
                relatedRoleGetter.initWithAncestors(delegator, entity, partyId);
            } else {
                relatedRoleGetter.init(delegator, entityId, partyId, entity);
            }
            roleValueList =   relatedRoleGetter.getList();
            if (Debug.verboseOn()) Debug.logVerbose(relatedRoleGetter.dumpAsText(), module);
        } else {
            if (Debug.verboseOn()) Debug.logVerbose("NO ROLE GETTER", module);
        }

        String targStatusId = null;
        if (modelEntity.getField("statusId") != null) {
            targStatusId = entity.getString("statusId");
        }
            if (Debug.verboseOn()) Debug.logVerbose("STATUS:" + targStatusId, module);

        while (permissionConditionGetter.getNext()) {
            String roleConditionId = permissionConditionGetter.getRoleValue();
            String auxiliaryConditionId = permissionConditionGetter.getAuxiliaryValue();
            String statusConditionId = permissionConditionGetter.getStatusValue();

            boolean auxiliaryCond = (auxiliaryConditionId == null ||  auxiliaryConditionId.equals("_NA_") || (auxiliaryValueList != null && auxiliaryValueList.contains(auxiliaryConditionId)) );
            boolean statusCond = (statusConditionId == null || statusConditionId.equals("_NA_") || (targStatusId != null && targStatusId.equals(statusConditionId)));
            boolean roleCond = (roleConditionId == null || roleConditionId.equals("_NA_") || (roleValueList != null && roleValueList.contains(roleConditionId)));

            if (auxiliaryCond && statusCond && roleCond) {
                if (Debug.verboseOn()) Debug.logVerbose("MATCHED: role:" + roleConditionId + " status:" + statusConditionId + " aux:" + auxiliaryConditionId, module);
                    isMatch = true;
                    break;
            }
        }
        return isMatch;
    }

    /**
     * getRelatedPurposes
     */
    public static List<String> getRelatedPurposes(GenericValue entity, List<String> passedPurposes) {

        if (entity == null) return passedPurposes;

        List<String> purposeIds = null;
        if (passedPurposes == null) {
            purposeIds = new LinkedList<>();
        } else {
            purposeIds = new LinkedList<>();
            purposeIds.addAll(passedPurposes);
        }

        String entityName = entity.getEntityName();
        String lcEntityName = entityName.toLowerCase();

        List<GenericValue> purposes = null;
        try {
            purposes = entity.getRelated(entityName + "Purpose", null, null, true);
        } catch (GenericEntityException e) {
            Debug.logError(e, "No associated purposes found. ", module);
            return purposeIds;
        }

        for (GenericValue val: purposes) {
            purposeIds.add((String) val.get(lcEntityName + "PurposeTypeId"));
        }


        return purposeIds;
    }


    /**
     * getUserRoles
     * Queries for the ContentRoles associated with a Content entity
     * and returns the ones that match the user.
     * Follows group parties to see if the user is a member.
     */
    public static List<String> getUserRoles(GenericValue entity, GenericValue userLogin, Delegator delegator) throws GenericEntityException {

        List<String> roles = new LinkedList<>();
        if (entity == null) return roles;
        String entityName = entity.getEntityName();
            // TODO: Need to use ContentManagementWorker.getAuthorContent first


        roles.remove("OWNER"); // always test with the owner of the current content
        if (entity.get("createdByUserLogin") != null && userLogin != null) {
            String userLoginId = (String)userLogin.get("userLoginId");
            String userLoginIdCB = (String)entity.get("createdByUserLogin");
            //if (Debug.infoOn()) Debug.logInfo("userLoginId:" + userLoginId + ": userLoginIdCB:" + userLoginIdCB + ":", null);
            if (userLoginIdCB.equals(userLoginId)) {
                roles.add("OWNER");
                //if (Debug.infoOn()) Debug.logInfo("in getUserRoles, passedRoles(0):" + passedRoles, null);
            }
        }

        String partyId = (String)userLogin.get("partyId");
        List<GenericValue> relatedRoles = null;
        List<GenericValue> tmpRelatedRoles = entity.getRelated(entityName + "Role", null, null, true);
        relatedRoles = EntityUtil.filterByDate(tmpRelatedRoles);
        if (relatedRoles != null) {
            for (GenericValue contentRole: relatedRoles) {
                String roleTypeId = (String)contentRole.get("roleTypeId");
                String targPartyId = (String)contentRole.get("partyId");
                if (targPartyId.equals(partyId)) {
                    if (!roles.contains(roleTypeId))
                        roles.add(roleTypeId);
                    if (roleTypeId.equals("AUTHOR") && !roles.contains("OWNER"))
                        roles.add("OWNER");
                } else { // Party may be of "PARTY_GROUP" type, in which case the userLogin may still possess this role
                    GenericValue party = null;
                    String partyTypeId = null;
                    try {
                        party = contentRole.getRelatedOne("Party", false);
                        partyTypeId = (String)party.get("partyTypeId");
                        if (partyTypeId != null && partyTypeId.equals("PARTY_GROUP")) {
                           Map<String, Object> map = new HashMap<>();

                           // At some point from/thru date will need to be added
                           map.put("partyIdFrom", partyId);
                           map.put("partyIdTo", targPartyId);
                           if (isGroupMember(map, delegator)) {
                               if (!roles.contains(roleTypeId))
                                   roles.add(roleTypeId);
                           }
                        }
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Error in finding related party. " + e.getMessage(), module);
                    }
                }
            }
        }
        return roles;
    }


    /**
     * Tests to see if the user belongs to a group
     */
    public static boolean isGroupMember(Map<String, ?> partyRelationshipValues, Delegator delegator) {
        boolean isMember = false;
        String partyIdFrom = (String)partyRelationshipValues.get("partyIdFrom") ;
        String partyIdTo = (String)partyRelationshipValues.get("partyIdTo") ;
        //String roleTypeIdFrom = "PERMISSION_GROUP_MBR";
        //String roleTypeIdTo = "PERMISSION_GROUP";
        //Timestamp fromDate = UtilDateTime.nowTimestamp();
        //Timestamp thruDate = UtilDateTime.getDayStart(UtilDateTime.nowTimestamp(), 1);

        //if (partyRelationshipValues.get("roleTypeIdFrom") != null) {
        //    roleTypeIdFrom = (String)partyRelationshipValues.get("roleTypeIdFrom") ;
        //}
        //if (partyRelationshipValues.get("roleTypeIdTo") != null) {
        //    roleTypeIdTo = (String)partyRelationshipValues.get("roleTypeIdTo") ;
        //}
        //if (partyRelationshipValues.get("fromDate") != null) {
        //    fromDate = (Timestamp)partyRelationshipValues.get("fromDate") ;
        //}
        //if (partyRelationshipValues.get("thruDate") != null) {
        //    thruDate = (Timestamp)partyRelationshipValues.get("thruDate") ;
        //}

        //EntityExpr relationExpr = EntityCondition.makeCondition("partyRelationshipTypeId", "CONTENT_PERMISSION");
        //EntityExpr roleTypeIdFromExpr = EntityCondition.makeCondition("roleTypeIdFrom", "CONTENT_PERMISSION_GROUP_MEMBER");
        //EntityExpr roleTypeIdToExpr = EntityCondition.makeCondition("roleTypeIdTo", "CONTENT_PERMISSION_GROUP");
        //EntityExpr fromExpr = EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, fromDate);
        //EntityCondition thruCond = EntityCondition.makeCondition(UtilMisc.toList(EntityCondition.makeCondition("thruDate", null),
        //                    EntityCondition.makeCondition("thruDate", EntityOperator.GREATER_THAN, thruDate)), EntityOperator.OR);

        // This method is simplified to make it work, these conditions need to be added back in.
        //List joinList = UtilMisc.toList(fromExpr, thruCond, partyFromExpr, partyToExpr, relationExpr);

        List<GenericValue> partyRelationships = null;
        try {
            partyRelationships = EntityQuery.use(delegator).from("PartyRelationship").where("partyIdFrom", partyIdFrom, "partyIdTo", partyIdTo).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem finding PartyRelationships. ", module);
            return false;
        }
        if (partyRelationships.size() > 0) {
           isMember = true;
        }

        return isMember;
    }


    public interface PermissionConditionGetter {

        public boolean getNext();
        public String getRoleValue();
        public String getOperationValue();
        public String getStatusValue();
        public int getPrivilegeValue() throws GenericEntityException;
        public String getAuxiliaryValue();
        public void init(Delegator delegator) throws GenericEntityException;
        public void restart();
        public void setOperationList(String operationIdString);
        public void setOperationList(List<String> opList);
        public List<String> getOperationList();
        public String dumpAsText();
        public void clearList();
    }

    public static class StdPermissionConditionGetter implements PermissionConditionGetter {

        protected List<GenericValue> entityList;
        protected List<String> operationList;
        protected ListIterator<GenericValue> iter;
        protected GenericValue currentValue;
        protected String operationFieldName;
        protected String roleFieldName;
        protected String statusFieldName;
        protected String privilegeFieldName;
        protected String auxiliaryFieldName;
        protected String entityName;

        public StdPermissionConditionGetter () {

            this.operationFieldName = "contentOperationId";
            this.roleFieldName = "roleTypeId";
            this.statusFieldName = "statusId";
            this.privilegeFieldName = "privilegeEnumId";
            this.auxiliaryFieldName = "contentPurposeTypeId";
            this.entityName = "ContentPurposeOperation";
        }

        public StdPermissionConditionGetter (String entityName, String operationFieldName, String roleFieldName, String statusFieldName, String auxiliaryFieldName, String privilegeFieldName) {

            this.operationFieldName = operationFieldName;
            this.roleFieldName = roleFieldName ;
            this.statusFieldName = statusFieldName ;
            this.privilegeFieldName = privilegeFieldName ;
            this.auxiliaryFieldName = auxiliaryFieldName ;
            this.entityName = entityName;
        }

        public StdPermissionConditionGetter (Element getterElement) {
            this.operationFieldName = getterElement.getAttribute("operation-field-name");
            this.roleFieldName = getterElement.getAttribute("role-field-name");
            this.statusFieldName = getterElement.getAttribute("status-field-name");
            this.privilegeFieldName = getterElement.getAttribute("privilege-field-name");
            this.auxiliaryFieldName = getterElement.getAttribute("auxiliary-field-name");
            this.entityName = getterElement.getAttribute("entity-name");
        }

        @Override
        public boolean getNext() {
            boolean hasNext = false;
            if (iter != null && iter.hasNext()) {
                currentValue = iter.next();
                hasNext = true;
            }
            return hasNext;
        }

        @Override
        public String getRoleValue() {
            return this.currentValue.getString(this.roleFieldName);
        }

        @Override
        public String getOperationValue() {
            return this.currentValue.getString(this.operationFieldName);
        }
        @Override
        public String getStatusValue() {
            return this.currentValue.getString(this.statusFieldName);

        }
        @Override
        public int getPrivilegeValue() throws GenericEntityException {
            int privilegeEnumSeq = -1;
            String privilegeEnumId = null;
            Delegator delegator = currentValue.getDelegator();

            if (UtilValidate.isNotEmpty(privilegeFieldName)) {
                privilegeEnumId = currentValue.getString(this.privilegeFieldName);
            }
            if (UtilValidate.isNotEmpty(privilegeEnumId)) {
                GenericValue privEnum = EntityQuery.use(delegator).from("Enumeration").where("enumId", privilegeEnumId).cache().queryOne();
                if (privEnum != null) {
                    String sequenceId = privEnum.getString("sequenceId");
                    try {
                        privilegeEnumSeq = Integer.parseInt(sequenceId);
                    } catch (NumberFormatException e) {
                        // just leave it at -1
                    }
                }
            }
            return privilegeEnumSeq;

        }

        @Override
        public String getAuxiliaryValue() {
            return this.currentValue.getString(this.auxiliaryFieldName);
        }

        @Override
        public void setOperationList(String operationIdString) {

            this.operationList = null;
            if (UtilValidate.isNotEmpty(operationIdString)) {
                this.operationList = StringUtil.split(operationIdString, "|");
            }
        }

        @Override
        public void setOperationList(List<String> operationList) {
            this.operationList = operationList;
        }

        @Override
        public List<String> getOperationList() {
            return this.operationList;
        }

        @Override
        public void clearList() {
            this.entityList = new LinkedList<>();
        }

        @Override
        public void init(Delegator delegator) throws GenericEntityException {
            this.entityList = EntityQuery.use(delegator)
                                         .from(this.entityName)
                                         .where(EntityCondition.makeCondition(operationFieldName, EntityOperator.IN, this.operationList))
                                         .cache(true)
                                         .queryList();
        }

        @Override
        public void restart() {
            this.iter = null;
            if (this.entityList != null) {
                this.iter = this.entityList.listIterator();
            }
        }

        @Override
        public String dumpAsText() {
             List<String> fieldNames = UtilMisc.toList("roleFieldName",  "auxiliaryFieldName",  "statusFieldName");
             Map<String, Integer> widths = UtilMisc.toMap("roleFieldName", 24, "auxiliaryFieldName", 24, "statusFieldName", 24);
             StringBuilder buf = new StringBuilder();
             Integer wid = null;

             buf.append("Dump for ");
             buf.append(this.entityName);
             buf.append(" ops:");
             buf.append(StringUtil.join(this.operationList, ","));
             buf.append("\n");
             for (String fld: fieldNames) {
                 wid = widths.get(fld);
                 buf.append(fld);
                 for (int i = 0; i < (wid - fld.length()); i++) buf.append("^");
                 buf.append("  ");
             }
                     buf.append("\n");
             for (String fld: fieldNames) {
                 wid = widths.get(fld);
                 for (int i = 0; i < wid; i++) buf.append("-");
                 buf.append("  ");
             }
                     buf.append("\n");
             if (entityList != null) {
                 for (GenericValue contentPurposeOperation: this.entityList) {
                     /*
                     String contentOperationId = contentPurposeOperation.getString(this.operationFieldName);
                     if (UtilValidate.isEmpty(contentOperationId)) {
                         contentOperationId = "";
                     }
                     wid = (Integer)widths.get("operationFieldName");
                     buf.append(contentOperationId);
                     for (int i=0; i < (wid.intValue() - contentOperationId.length()); i++) buf.append("^");
                     buf.append("  ");
                     */

                     String roleTypeId = contentPurposeOperation.getString(this.roleFieldName);
                     if (UtilValidate.isEmpty(roleTypeId)) {
                         roleTypeId = "";
                     }
                     wid = widths.get("roleFieldName");
                     buf.append(roleTypeId);
                     for (int i = 0; i < (wid - roleTypeId.length()); i++) buf.append("^");
                     buf.append("  ");

                     String  auxiliaryFieldValue = contentPurposeOperation.getString(this.auxiliaryFieldName);
                     if (UtilValidate.isEmpty(auxiliaryFieldValue)) {
                         auxiliaryFieldValue = "";
                     }
                     wid = widths.get("auxiliaryFieldName");
                     buf.append(auxiliaryFieldValue);
                     for (int i = 0; i < (wid - auxiliaryFieldValue.length()); i++) buf.append("^");
                     buf.append("  ");

                     String statusId = contentPurposeOperation.getString(this.statusFieldName);
                     if (UtilValidate.isEmpty(statusId)) {
                         statusId = "";
                     }
                     buf.append(statusId);
                     /*
                     wid = (Integer)widths.get("statusFieldName");
                     for (int i=0; i < (wid.intValue() - statusId.length()); i++) buf.append(" ");
                     */
                     buf.append("  ");

                     buf.append("\n");
                 }
             }
             return buf.toString();
        }
    }

    public interface AuxiliaryValueGetter {
        public void init(Delegator delegator, String entityId) throws GenericEntityException;
        public List<String> getList();
        public void clearList();
        public String dumpAsText();
    }

    public static class StdAuxiliaryValueGetter implements AuxiliaryValueGetter {

        protected List<String> entityList = new LinkedList<>();
        protected String auxiliaryFieldName;
        protected String entityName;
        protected String entityIdName;

        public StdAuxiliaryValueGetter () {

            this.auxiliaryFieldName = "contentPurposeTypeId";
            this.entityName = "ContentPurpose";
            this.entityIdName = "contentId";
        }

        public StdAuxiliaryValueGetter (String entityName,  String auxiliaryFieldName, String entityIdName) {

            this.auxiliaryFieldName = auxiliaryFieldName ;
            this.entityName = entityName;
            this.entityIdName = entityIdName;
        }

        public StdAuxiliaryValueGetter (Element getterElement) {

            this.auxiliaryFieldName = getterElement.getAttribute("auxiliary-field-name");
            this.entityName = getterElement.getAttribute("entity-name");
            this.entityIdName = getterElement.getAttribute("entity-id-name");
        }

        @Override
        public List<String> getList() {
            return entityList;
        }

        @Override
        public void clearList() {
            this.entityList = new LinkedList<>();
        }

        public void setList(List<String> lst) {
            this.entityList = lst;
        }

        @Override
        public void init(Delegator delegator, String entityId) throws GenericEntityException {

            if (this.entityList == null) {
               this.entityList = new LinkedList<>();
            }
            if (UtilValidate.isEmpty(this.entityName)) {
                return;
            }
            List<GenericValue> values = EntityQuery.use(delegator).from(this.entityName).where(this.entityIdName, entityId).cache(true).queryList();
            for (GenericValue entity: values) {
                this.entityList.add(entity.getString(this.auxiliaryFieldName));
            }
        }

        @Override
        public String dumpAsText() {
             StringBuilder buf = new StringBuilder();
             buf.append("AUXILIARY: ");
             if (entityList != null) {
                 for (String val: entityList) {
                    buf.append(val);
                    buf.append("  ");
                }
             }
             return buf.toString();
        }
    }

    public interface RelatedRoleGetter {
        public void init(Delegator delegator, String entityId, String partyId, GenericValue entity) throws GenericEntityException;
        public void initWithAncestors(Delegator delegator, GenericValue entity, String partyId) throws GenericEntityException;
        public List<String> getList();
        public void clearList();
        public void setList(List<String> lst);
        public String dumpAsText();
        public boolean isOwner(GenericValue entity, String targetPartyId);
    }

    public static class StdRelatedRoleGetter implements RelatedRoleGetter {

        protected List<String> roleIdList = new LinkedList<>();
        protected String roleTypeFieldName;
        protected String partyFieldName;
        protected String entityName;
        protected String roleEntityIdName;
        protected String roleEntityName;
        protected String ownerEntityFieldName;

        public StdRelatedRoleGetter () {

            this.roleTypeFieldName = "roleTypeId";
            this.partyFieldName = "partyId";
            this.ownerEntityFieldName = "ownerContentId";
            this.entityName = "Content";
            this.roleEntityName = "ContentRole";
            this.roleEntityIdName = "contentId";
        }

        public StdRelatedRoleGetter (String entityName,  String roleTypeFieldName, String roleEntityIdName, String partyFieldName, String ownerEntityFieldName, String roleEntityName) {

            this.roleTypeFieldName = roleTypeFieldName ;
            this.partyFieldName = partyFieldName ;
            this.ownerEntityFieldName = ownerEntityFieldName ;
            this.entityName = entityName;
            this.roleEntityName = roleEntityName;
            this.roleEntityIdName = roleEntityIdName;
        }

        public StdRelatedRoleGetter (Element getterElement) {

            this.roleTypeFieldName = getterElement.getAttribute("role-type-field-name");
            this.partyFieldName = getterElement.getAttribute("party-field-name");
            this.ownerEntityFieldName = getterElement.getAttribute("owner-entity-field-name");
            this.entityName = getterElement.getAttribute("entity-name");
            this.roleEntityName = getterElement.getAttribute("role-entity-name");
            this.roleEntityIdName = getterElement.getAttribute("entity-id-name");
        }

        @Override
        public List<String> getList() {
            return this.roleIdList;
        }

        @Override
        public void clearList() {
            this.roleIdList = new LinkedList<>();
        }

        @Override
        public void setList(List<String> lst) {
            this.roleIdList = lst;
        }

        @Override
        public void init(Delegator delegator, String entityId, String partyId, GenericValue entity) throws GenericEntityException {

            List<String> lst = getUserRolesFromList(delegator, UtilMisc.toList(entityId), partyId, this.roleEntityIdName,
                                               this.partyFieldName, this.roleTypeFieldName, this.roleEntityName);
            this.roleIdList.addAll(lst);
            if (isOwner(entity, partyId)) {
                this.roleIdList.add("OWNER");
            }
        }

        @Override
        public void initWithAncestors(Delegator delegator, GenericValue entity, String partyId) throws GenericEntityException {

           List<String> ownedContentIdList = new LinkedList<>();
           getEntityOwners(delegator, entity, ownedContentIdList, this.entityName, this.ownerEntityFieldName);
           if (ownedContentIdList.size() > 0) {
               List<String> lst = getUserRolesFromList(delegator, ownedContentIdList, partyId, this.roleEntityIdName, this.partyFieldName, this.roleTypeFieldName, this.roleEntityName);
               this.roleIdList.addAll(lst);
           }
        }

        @Override
        public boolean isOwner(GenericValue entity, String targetPartyId) {
            boolean isOwner = false;
            if (entity == null || targetPartyId == null) {
                return false;
            }
            Delegator delegator = entity.getDelegator();
            ModelEntity modelEntity = delegator.getModelEntity(entityName);
            if (modelEntity.getField("createdByUserLogin") == null) {
                return false;
            }
            if (entity.get("createdByUserLogin") != null) {
                String userLoginIdCB = (String)entity.get("createdByUserLogin");
                try {
                    GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", userLoginIdCB).cache().queryOne();
                    if (userLogin != null) {
                        String partyIdCB = userLogin.getString("partyId");
                        if (partyIdCB != null) {
                            if (partyIdCB.equals(targetPartyId)) {
                                isOwner = true;
                            }
                        }
                    }
                } catch (GenericEntityException e) {
                    Debug.logInfo(e.getMessage() + " Returning false for 'isOwner'.", module);

                }
            }
            return isOwner;
        }

        @Override
        public String dumpAsText() {
             StringBuilder buf = new StringBuilder();
             buf.append("ROLES: ");
             if (roleIdList != null) {
                for (String val: roleIdList) {
                    buf.append(val);
                    buf.append("  ");
                }
             }
             return buf.toString();
        }
    }

    public static List<String> getUserRolesFromList(Delegator delegator, List<String> idList, String partyId, String entityIdFieldName, String partyIdFieldName, String roleTypeIdFieldName, String entityName) throws GenericEntityException {

        EntityExpr expr = EntityCondition.makeCondition(entityIdFieldName, EntityOperator.IN, idList);
        EntityExpr expr2 = EntityCondition.makeCondition(partyIdFieldName, partyId);
        List<GenericValue> roleList = EntityQuery.use(delegator)
                                                 .from(entityName)
                                                 .where(EntityCondition.makeCondition(UtilMisc.toList(expr, expr2)))
                                                 .cache(true)
                                                 .queryList();
        List<GenericValue> roleListFiltered = EntityUtil.filterByDate(roleList);
        Set<String> distinctSet = new HashSet<>();
        for (GenericValue contentRole: roleListFiltered) {
            String roleTypeId = contentRole.getString(roleTypeIdFieldName);
            distinctSet.add(roleTypeId);
        }
        List<String> distinctList = Arrays.asList(distinctSet.toArray(new String[distinctSet.size()]));
        return distinctList;
    }

    public static void getEntityOwners(Delegator delegator, GenericValue entity,  List<String> contentOwnerList, String entityName, String ownerIdFieldName) throws GenericEntityException {

        String ownerContentId = entity.getString(ownerIdFieldName);
        if (UtilValidate.isNotEmpty(ownerContentId)) {
            contentOwnerList.add(ownerContentId);
            ModelEntity modelEntity = delegator.getModelEntity(entityName);
            String pkFieldName = modelEntity.getFirstPkFieldName();
            GenericValue ownerContent = EntityQuery.use(delegator).from(entityName).where(pkFieldName, ownerContentId).cache(true).queryOne();
            if (ownerContent != null) {
                getEntityOwners(delegator, ownerContent, contentOwnerList, entityName, ownerIdFieldName);
            }
        }

    }

    public static int getPrivilegeEnumSeq(Delegator delegator, String privilegeEnumId) throws GenericEntityException {
        int privilegeEnumSeq = -1;

        if (UtilValidate.isNotEmpty(privilegeEnumId)) {
            GenericValue privEnum = EntityQuery.use(delegator).from("Enumeration").where("enumId", privilegeEnumId).cache().queryOne();
            if (privEnum != null) {
                String sequenceId = privEnum.getString("sequenceId");
                try {
                    privilegeEnumSeq = Integer.parseInt(sequenceId);
                } catch (NumberFormatException e) {
                    // just leave it at -1
                }
            }
        }
        return privilegeEnumSeq;
    }
}
