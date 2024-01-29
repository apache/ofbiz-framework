package com.simbaquartz.xparty.services;


import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PartyEmployeesServices {
    public static final String module = PartyEmployeesServices.class.getName();


    public static Map<String, Object> setEmployeeGroupSequence(DispatchContext dctx, Map<String, Object> context) throws Exception {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        String partyId = (String) context.get("partyId");
        List<String> contentIdsList = (List<String>) context.get("contentIds");

        Map<String, Object> sequenceNumsMap = FastMap.newInstance();
        long sequenceCounter = 1;

        List<EntityExpr> contentIdsCondsList = new LinkedList<EntityExpr>();
        for(String contentId:contentIdsList) {
            contentIdsCondsList.add(EntityCondition.makeCondition("classificationGroupId", EntityOperator.EQUALS, contentId));
            sequenceNumsMap.put(contentId, sequenceCounter ++);
        };
        EntityConditionList<EntityExpr> exprListContentIdsOr = EntityCondition.makeCondition(contentIdsCondsList, EntityOperator.OR);
        EntityConditionList<EntityCondition> mainCond = EntityCondition.makeCondition(
                UtilMisc.toList(exprListContentIdsOr, EntityCondition.makeCondition("supplierPartyId", EntityOperator.EQUALS, partyId)),
                EntityOperator.AND
        );
        List<GenericValue> supplierClassificationGroups = delegator.findList("SuppPartyClassificationGroup", mainCond , null, UtilMisc.toList("sequenceNum"),null, false);

        if(UtilValidate.isNotEmpty(supplierClassificationGroups) && supplierClassificationGroups.size()>0) {
            for(GenericValue supplierClassificationGroup:supplierClassificationGroups) {
                String classificationGroupId = supplierClassificationGroup.getString("classificationGroupId");
                supplierClassificationGroup.set("sequenceNum", new Long((long)sequenceNumsMap.get(classificationGroupId)) );
            }
            delegator.storeAll(supplierClassificationGroups);
        }

        return result;
    }

    public static Map<String, Object> removeStaff(DispatchContext dctx, Map<String, Object> context) throws GenericEntityException {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        String employeePartyId = (String) context.get("partyId");

        //expire any available relationships
        List<GenericValue> availableRelationships = EntityQuery.use(delegator).from("PartyRelationship").where("partyIdTo", employeePartyId).queryList();

        //expire the relationships
        List<GenericValue> recordsToUpdate = FastList.newInstance();
        for(GenericValue availableRelationship : availableRelationships) {

            availableRelationship.set("thruDate", UtilDateTime.nowTimestamp());
            recordsToUpdate.add(availableRelationship);
        }

        delegator.storeAll(recordsToUpdate);

        //expire any available reported relationships
        List<GenericValue> reportingRelations = EntityQuery.use(delegator).from("PartyRelationship").where("partyIdFrom", employeePartyId).queryList();

        //expire the relationships
        List<GenericValue> recordsToDelete = FastList.newInstance();
        for(GenericValue availableRelationship : reportingRelations) {
            availableRelationship.set("thruDate", UtilDateTime.nowTimestamp());
            recordsToDelete.add(availableRelationship);
        }

        delegator.storeAll(recordsToDelete);

        //expire employee to customer relationships
        List<GenericValue> empToCustRelationships = EntityQuery.use(delegator).from("PartyRelationship").where("partyIdFrom", employeePartyId, "partyRelationshipTypeId", "CUSTOMER_REL").queryList();

        //expire the relationships
        List<GenericValue> empToCustRecordsToUpdate = FastList.newInstance();

        for(GenericValue empToCustRelationship : empToCustRelationships) {
            empToCustRelationship.set("thruDate", UtilDateTime.nowTimestamp());
            empToCustRecordsToUpdate.add(empToCustRelationship);
        }

        delegator.storeAll(empToCustRecordsToUpdate);

        List<GenericValue> employeeRolesFromQuote = EntityQuery.use(delegator).from("QuoteRole").where("partyId", employeePartyId).queryList();
        //expire the roles
        if(UtilValidate.isNotEmpty(employeeRolesFromQuote)) {
            List<GenericValue> employeeRecordsToUpdate = FastList.newInstance();

            for(GenericValue employeeRole : employeeRolesFromQuote) {

                employeeRole.set("thruDate", UtilDateTime.nowTimestamp());
                employeeRecordsToUpdate.add(employeeRole);
            }

            delegator.storeAll(employeeRecordsToUpdate);
        }

        return result;
    }

    public static Map<String, Object> setEmployerForParty(DispatchContext dctx, Map<String, Object> context)
            throws GenericEntityException, GenericServiceException {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String employeePartyId = (String) context.get("employerPartyId");
        String employerName = (String) context.get("employerName");
        String role = (String) context.get("role");
        String partyId = (String) context.get("partyId");

        if(UtilValidate.isNotEmpty(role)) {
            //expire any available relationships
            List<GenericValue> availableRelationships = EntityQuery.use(delegator).from("PartyRelationship")
                    .where("partyIdTo", partyId).queryList();

            //expire the relationships
            List<GenericValue> recordsToUpdate = FastList.newInstance();
            for(GenericValue availableRelationship : availableRelationships) {

                availableRelationship.set("thruDate", UtilDateTime.nowTimestamp());
                recordsToUpdate.add(availableRelationship);
            }

            delegator.storeAll(recordsToUpdate);

            if(role.equals("SUPPLIER_POC")) {

                // remove supplier classification record if exists
                List<GenericValue> supplierPartyClassifications = EntityQuery.use(delegator).from("SupplierPartyClassification")
                        .where("partyId", partyId).queryList();
                if(UtilValidate.isNotEmpty(supplierPartyClassifications)) {
                    delegator.removeAll(supplierPartyClassifications);
                }

                //create party role
                GenericValue partyRole = EntityQuery.use(delegator).from("PartyRole")
                        .where("partyId", partyId, "roleTypeId", role).queryOne();
                if (UtilValidate.isEmpty(partyRole)) {
                    Map<String, Object> createPartyRoleResponse = dispatcher.runSync("createPartyRole",
                            UtilMisc.toMap("partyId", partyId, "roleTypeId", role, "userLogin", userLogin));

                    if (!ServiceUtil.isSuccess(createPartyRoleResponse)) {
                        Debug.logError("An Error occurred while createPartyRole service call: " + ServiceUtil.getErrorMessage(createPartyRoleResponse), module);
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createPartyRoleResponse));

                    }
                }

                //create party relationship with supplier
                GenericValue partyRelationWithSupplier = EntityQuery.use(delegator).from("PartyRelationship")
                        .where("partyIdFrom", employeePartyId, "partyIdTo", partyId, "roleTypeIdFrom", "_NA_",
                                "roleTypeIdTo", "SUPPLIER_POC", "partyRelationshipTypeId", "EMPLOYMENT").filterByDate().queryOne();
                if (UtilValidate.isEmpty(partyRelationWithSupplier)) {
                    //build a relationship
                    Map<String, Object> createSalesRepResult = dispatcher.runSync("createPartyRelationship",
                            UtilMisc.toMap("userLogin", userLogin, "partyIdFrom", employeePartyId,
                                    "partyIdTo", partyId, "roleTypeIdFrom", "_NA_",
                                    "roleTypeIdTo", role, "partyRelationshipTypeId", "EMPLOYMENT"));

                    if( !ServiceUtil.isSuccess(createSalesRepResult) ) {
                        Debug.logError("An Error occurred while createPartyRelationship service call: " + ServiceUtil.getErrorMessage(createSalesRepResult), module);
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createSalesRepResult));
                    }
                }

            } else if(role.equals("CONTRACTING_OFFICER")) {

                //create party role
                GenericValue partyRole = EntityQuery.use(delegator).from("PartyRole")
                        .where("partyId", partyId, "roleTypeId", role).queryOne();
                if (UtilValidate.isEmpty(partyRole)) {
                    Map<String, Object> createPartyRoleResponse = dispatcher.runSync("createPartyRole",
                            UtilMisc.toMap("partyId", partyId, "roleTypeId", role, "userLogin", userLogin));

                    if (!ServiceUtil.isSuccess(createPartyRoleResponse)) {
                        Debug.logError("An Error occurred while createPartyRole service call: " + ServiceUtil.getErrorMessage(createPartyRoleResponse), module);
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createPartyRoleResponse));
                    }
                }
                //create party relationship with customer
                GenericValue partyRelation = EntityQuery.use(delegator).from("PartyRelationship")
                        .where("partyIdFrom", employeePartyId, "partyIdTo", partyId,
                                "roleTypeIdFrom", "_NA_", "roleTypeIdTo", role,
                                "partyRelationshipTypeId", "EMPLOYMENT").filterByDate().queryOne();
                if (UtilValidate.isEmpty(partyRelation)) {
                    Map<String, Object> createSalesRepResult = dispatcher.runSync("createPartyRelationship",
                            UtilMisc.toMap("userLogin", userLogin, "partyIdFrom", employeePartyId,
                                    "partyIdTo", partyId, "roleTypeIdFrom", "_NA_",
                                    "roleTypeIdTo", role, "partyRelationshipTypeId", "EMPLOYMENT"));
                    if (!ServiceUtil.isSuccess(createSalesRepResult)) {
                        Debug.logError("An Error occurred while createPartyRole service call: " + ServiceUtil.getErrorMessage(createSalesRepResult), module);
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createSalesRepResult));
                    }
                }

            } else if(role.equals("PERSONAL_POC")) {
                //create party role
                GenericValue partyRole = EntityQuery.use(delegator).from("PartyRole")
                        .where("partyId", partyId, "roleTypeId", role).queryOne();
                if (UtilValidate.isEmpty(partyRole)) {
                    Map<String, Object> createPartyRoleResponse = dispatcher.runSync("createPartyRole",
                            UtilMisc.toMap("partyId", partyId, "roleTypeId", role, "userLogin", userLogin));

                    if (ServiceUtil.isError(createPartyRoleResponse)) {
                        Debug.logError("An Error occurred while createPartyRole service call: " + ServiceUtil.getErrorMessage(createPartyRoleResponse), module);
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createPartyRoleResponse));
                    }
                }

                if (UtilValidate.isEmpty(employeePartyId)) {

                    Map<String, Object> createPartyGroupResponse = dispatcher.runSync("createPartyGroup",
                            UtilMisc.toMap("userLogin", userLogin, "groupName", employerName));
                    if (!ServiceUtil.isSuccess(createPartyGroupResponse)) {
                        Debug.logError("An Error occurred while createPartyRole service call: " + ServiceUtil.getErrorMessage(createPartyGroupResponse), module);
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createPartyGroupResponse));
                    }
                    employeePartyId = (String) createPartyGroupResponse.get("partyId");
                    //create party role
                    GenericValue compPartyRole = EntityQuery.use(delegator).from("PartyRole")
                            .where("partyId", employeePartyId, "roleTypeId", role).queryOne();
                    if (UtilValidate.isEmpty(compPartyRole)) {
                        Map<String, Object> createPartyRoleResponse = dispatcher.runSync("createPartyRole",
                                UtilMisc.toMap("partyId", employeePartyId, "roleTypeId", role, "userLogin", userLogin));

                        if (ServiceUtil.isError(createPartyRoleResponse)) {
                            Debug.logError("An Error occurred while createPartyRoleResponse service call: " + ServiceUtil.getErrorMessage(createPartyRoleResponse), module);
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createPartyRoleResponse));
                        }
                    }
                    //create company relationship with userlogin
                    GenericValue partyRelation = EntityQuery.use(delegator).from("PartyRelationship")
                            .where("partyIdFrom", (String) userLogin.get("partyId"), "partyIdTo", employeePartyId,
                                    "roleTypeIdFrom", "_NA_", "roleTypeIdTo", role).filterByDate().queryOne();
                    if (UtilValidate.isEmpty(partyRelation)) {
                        Map<String, Object> createPartyRelationshipMapResult = dispatcher.runSync("createPartyRelationship",
                                UtilMisc.toMap("userLogin", userLogin, "partyIdFrom", (String) userLogin.get("partyId"),
                                        "partyIdTo", employeePartyId, "roleTypeIdFrom", "_NA_", "roleTypeIdTo", role));

                        if (!ServiceUtil.isSuccess(createPartyRelationshipMapResult)) {
                            Debug.logError("An Error occurred while createPartyRoleResponse service call: " + ServiceUtil.getErrorMessage(createPartyRelationshipMapResult), module);
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createPartyRelationshipMapResult));
                        }
                    }
                }

                //create party relationship with company
                GenericValue partyRelationWithCompany = EntityQuery.use(delegator).from("PartyRelationship")
                        .where("partyIdFrom", employeePartyId, "partyIdTo", partyId, "roleTypeIdFrom", "_NA_",
                                "roleTypeIdTo", role, "partyRelationshipTypeId", "EMPLOYMENT").filterByDate().queryOne();
                if (UtilValidate.isEmpty(partyRelationWithCompany)) {
                    Map<String, Object> createPartyRelationshipMapResult = dispatcher.runSync("createPartyRelationship",
                            UtilMisc.toMap("userLogin", userLogin, "partyIdFrom", employeePartyId,
                                    "partyIdTo", partyId, "roleTypeIdFrom", "_NA_", "roleTypeIdTo", "PERSONAL_POC",
                                    "partyRelationshipTypeId", "EMPLOYMENT"));

                    if (!ServiceUtil.isSuccess(createPartyRelationshipMapResult)) {
                        Debug.logError("An Error occurred while createPartyRoleResponse service call: " + ServiceUtil.getErrorMessage(createPartyRelationshipMapResult), module);
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createPartyRelationshipMapResult));
                    }
                }
            }

            GenericValue partyClassification = EntityQuery.use(delegator).from("PartyClassification")
                    .where("partyId", partyId, "partyClassificationGroupId", "UNCLASSIFIED_CONTACT").queryOne();
            if (UtilValidate.isNotEmpty(partyClassification)) {
                //update unclassified contacts
                Map<String, Object> updatePartyClassificationResp = dispatcher.runSync("updatePartyClassification",
                        UtilMisc.toMap("userLogin", userLogin, "partyId", partyId,
                                "partyClassificationGroupId", "UNCLASSIFIED_CONTACT",
                                "fromDate", (Timestamp)partyClassification.get("fromDate"),
                                "thruDate", UtilDateTime.nowTimestamp()));
                if( !ServiceUtil.isSuccess(updatePartyClassificationResp) ) {
                    Debug.logError("An Error occurred while createPartyRoleResponse service call: " + ServiceUtil.getErrorMessage(updatePartyClassificationResp), module);
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(updatePartyClassificationResp));
                }
            }

            //Index Party Id for all the Party Id in To,CC,BCC Address
            Map<String, Object> inputMap = new HashMap<String, Object>();
            inputMap.put("partyId", partyId);
            inputMap.put("userLogin", userLogin);
            Map<String, Object> servResult = dispatcher.runSync("indexPartyInSolr", inputMap);
            if( !ServiceUtil.isSuccess(servResult) ) {
                Debug.logError("An Error occurred while indexPartyInSolr service call: " + ServiceUtil.getErrorMessage(servResult), module);
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(servResult));
            }
        }

        return result;
    }
}