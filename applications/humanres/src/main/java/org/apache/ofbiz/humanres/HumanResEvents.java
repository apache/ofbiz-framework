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
package org.apache.ofbiz.humanres;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.party.party.PartyHelper;

public class HumanResEvents {
    private static final String MODULE = HumanResEvents.class.getName();
    private static final String RES_ERROR = "HumanResErrorUiLabels";

    // Please note : the structure of map in this function is according to the JSON data map of the jsTree
    public static String getChildHRCategoryTree(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String partyId = request.getParameter("partyId");
        String onclickFunction = request.getParameter("onclickFunction");
        String additionParam = request.getParameter("additionParam");
        String hrefString = request.getParameter("hrefString");
        String hrefString2 = request.getParameter("hrefString2");
        List<Map<String, Object>> categoryList = new ArrayList<>();
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("delegator", delegator);
        paramMap.put("partyId", partyId);
        paramMap.put("onclickFunction", onclickFunction);
        paramMap.put("additionParam", additionParam);
        paramMap.put("hrefString", hrefString);
        paramMap.put("hrefString2", hrefString2);

        //check employee position
        try {
            categoryList.addAll(getCurrentEmployeeDetails(paramMap));
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return "error";
        }
        try {
            GenericValue partyGroup = EntityQuery.use(delegator).from("PartyGroup").where("partyId", partyId).queryOne();
            if (partyGroup != null) {
                paramMap.put("partyGroup", partyGroup);
                /* get the child departments of company or party */
                categoryList.addAll(getChildComps(paramMap));

                /* get employee which are working in company or party */
                categoryList.addAll(getEmployeeInComp(paramMap));
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return "error";
        }
        request.setAttribute("hrTree", categoryList);
        return "success";
    }

    private static List<Map<String, Object>> getCurrentEmployeeDetails(Map<String, Object> params) throws GenericEntityException {
        Delegator delegator = (Delegator) params.get("delegator");
        List<Map<String, Object>> responseList = new ArrayList<>();
        long emplPosCount;
        String partyId = (String) params.get("partyId");
        String onclickFunction = (String) params.get("onclickFunction");
        String additionParam = (String) params.get("additionParam");
        String hrefString = (String) params.get("hrefString");
        String hrefString2 = (String) params.get("hrefString2");
        String title = null;
        try {
            emplPosCount = EntityQuery.use(delegator).from("EmplPosition")
                    .where("emplPositionId", partyId).queryCount();
            if (emplPosCount > 0) {
                String emplId = partyId;
                List<GenericValue> emlpfillCtxs = EntityQuery.use(delegator).from("EmplPositionFulfillment")
                        .where("emplPositionId", emplId)
                        .filterByDate().queryList();
                if (UtilValidate.isNotEmpty(emlpfillCtxs)) {
                    for (GenericValue emlpfillCtx : emlpfillCtxs) {
                        String memberId = emlpfillCtx.getString("partyId");
                        title = PartyHelper.getPartyName(delegator, memberId, false);
                        Map<String, Object> josonMap = new HashMap<>();
                        Map<String, Object> dataMap = new HashMap<>();
                        Map<String, Object> dataAttrMap = new HashMap<>();
                        Map<String, Object> attrMap = new HashMap<>();
                        String hrefStr = hrefString + memberId;
                        if (UtilValidate.isNotEmpty(hrefString2)) {
                            hrefStr = hrefStr + hrefString2;
                        }
                        attrMap.put("rel", "P");
                        attrMap.put("id", memberId);
                        dataAttrMap.put("onClick", onclickFunction + "('" + memberId + additionParam + "')");
                        dataAttrMap.put("href", hrefStr);
                        dataMap.put("title", title);
                        dataMap.put("attr", dataAttrMap);
                        josonMap.put("attr", attrMap);
                        josonMap.put("data", dataMap);
                        responseList.add(josonMap);
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            throw new GenericEntityException(e);
        }
        return responseList;
    }

    private static List<Map<String, Object>> getChildComps(Map<String, Object> params) throws GenericEntityException {
        Delegator delegator = (Delegator) params.get("delegator");
        Map<String, Object> partyGroup = UtilGenerics.cast(params.get("partyGroup"));
        List<Map<String, Object>> resultList = new ArrayList<>();
        List<GenericValue> childOfComs = null;
        String onclickFunction = (String) params.get("onclickFunction");
        String additionParam = (String) params.get("additionParam");
        String hrefString = (String) params.get("hrefString");
        String hrefString2 = (String) params.get("hrefString2");
        try {
            childOfComs = EntityQuery.use(delegator).from("PartyRelationship")
                    .where("partyIdFrom", partyGroup.get("partyId"),
                            "partyRelationshipTypeId", "GROUP_ROLLUP")
                            .filterByDate().queryList();
            if (UtilValidate.isNotEmpty(childOfComs)) {
                for (GenericValue childOfCom : childOfComs) {
                    String catId = null;
                    String childPartyId = null;
                    String catNameField = null;
                    String title = null;
                    Map<String, Object> josonMap = new HashMap<>();
                    Map<String, Object> dataMap = new HashMap<>();
                    Map<String, Object> dataAttrMap = new HashMap<>();
                    Map<String, Object> attrMap = new HashMap<>();
                    catId = childOfCom.getString("partyIdTo");
                    title = PartyHelper.getPartyName(delegator, catId, false);
                    josonMap.put("title", title);
                    //Check child existing
                    List<GenericValue> childOfSubComs = EntityQuery.use(delegator).from("PartyRelationship")
                            .where("partyIdFrom", catId,
                                    "partyRelationshipTypeId", "GROUP_ROLLUP")
                                    .filterByDate().queryList();
                    //check employee position
                    List<GenericValue> isPosition = EntityQuery.use(delegator).from("EmplPosition").where("partyId", catId).queryList();
                    if (UtilValidate.isNotEmpty(childOfSubComs) || UtilValidate.isNotEmpty(isPosition)) {
                        josonMap.put("state", "closed");
                    }
                    dataAttrMap.put("onClick", onclickFunction + "('" + catId + additionParam + "')");
                    String hrefStr = hrefString + catId;
                    if (UtilValidate.isNotEmpty(hrefString2)) {
                        hrefStr = hrefStr + hrefString2;
                    }
                    attrMap.put("rel", "Y");
                    attrMap.put("id", catId);
                    dataAttrMap.put("href", hrefStr);
                    dataMap.put("attr", dataAttrMap);
                    dataMap.put("title", title);
                    josonMap.put("attr", attrMap);
                    josonMap.put("data", dataMap);
                    resultList.add(josonMap);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            throw new GenericEntityException(e);
        }
        return resultList;
    }

    private static List<Map<String, Object>> getEmployeeInComp(Map<String, Object> params) throws GenericEntityException {
        List<GenericValue> isEmpls = null;
        Delegator delegator = (Delegator) params.get("delegator");
        String partyId = (String) params.get("partyId");
        List<Map<String, Object>> resultList = new ArrayList<>();
        try {
            isEmpls = EntityQuery.use(delegator).from("EmplPosition")
                    .where(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId),
                            EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "EMPL_POS_INACTIVE"))
                            .filterByDate("actualFromDate", "actualThruDate")
                            .queryList();
            if (UtilValidate.isNotEmpty(isEmpls)) {
                for (GenericValue childOfEmpl : isEmpls) {
                    Map<String, Object> emplMap = new HashMap<>();
                    Map<String, Object> emplAttrMap = new HashMap<>();
                    Map<String, Object> empldataMap = new HashMap<>();
                    Map<String, Object> emplDataAttrMap = new HashMap<>();
                    String emplId = (String) childOfEmpl.get("emplPositionId");
                    String typeId = (String) childOfEmpl.get("emplPositionTypeId");
                    //check child
                    List<GenericValue> emlpfCtxs = EntityQuery.use(delegator).from("EmplPositionFulfillment")
                            .where("emplPositionId", emplId)
                            .filterByDate().queryList();
                    if (UtilValidate.isNotEmpty(emlpfCtxs)) {
                        emplMap.put("state", "closed");
                    }
                    GenericValue emplContext = EntityQuery.use(delegator).from("EmplPositionType").where("emplPositionTypeId", typeId).queryOne();
                    String title = null;
                    if (UtilValidate.isNotEmpty(emplContext)) {
                        title = (String) emplContext.get("description") + " " + "[" + emplId + "]";
                    }
                    String hrefStr = "emplPositionView?emplPositionId=" + emplId;
                    emplAttrMap.put("href", hrefStr);
                    emplAttrMap.put("title", title);
                    emplAttrMap.put("onClick", "callEmplDocument" + "('" + emplId + "')");
                    empldataMap.put("title", title);
                    empldataMap.put("attr", emplAttrMap);
                    emplDataAttrMap.put("id", emplId);
                    emplDataAttrMap.put("rel", "N");
                    emplMap.put("data", empldataMap);
                    emplMap.put("attr", emplDataAttrMap);
                    emplMap.put("title", title);
                    resultList.add(emplMap);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            throw new GenericEntityException(e);
        }
        return resultList;
    }
}
