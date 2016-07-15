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
package org.ofbiz.humanres;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityQuery;

public class HumanResEvents {
    public static final String module = HumanResEvents.class.getName();
    public static final String resourceError = "HumanResErrorUiLabels";

    // Please note : the structure of map in this function is according to the JSON data map of the jsTree
    public static String getChildHRCategoryTree(HttpServletRequest request, HttpServletResponse response){
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String partyId = request.getParameter("partyId");
        String onclickFunction = request.getParameter("onclickFunction");
        String additionParam = request.getParameter("additionParam");
        String hrefString = request.getParameter("hrefString");
        String hrefString2 = request.getParameter("hrefString2");

        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("delegator", delegator);
        paramMap.put("partyId", partyId);
        paramMap.put("onclickFunction", onclickFunction);
        paramMap.put("additionParam", additionParam);
        paramMap.put("hrefString", hrefString);
        paramMap.put("hrefString2", hrefString2);

        List<Map<String,Object>> categoryList = new ArrayList<Map<String,Object>>();

        //check employee position
        try {
            categoryList.addAll(getCurrentEmployeeDetails(paramMap));
        } catch (GenericEntityException e) {
            e.printStackTrace();
            return "error";
        }

        try {
            GenericValue partyGroup = EntityQuery.use(delegator).from("PartyGroup").where("partyId", partyId).queryOne();
            if (UtilValidate.isNotEmpty(partyGroup)) {	
                paramMap.put("partyGroup", partyGroup);
                /* get the child departments of company or party */
                categoryList.addAll(getChildComps(paramMap));

                /* get employee which are working in company or party */
                categoryList.addAll(getEmployeeInComp(paramMap));
            }
        } catch (GenericEntityException e) {
            e.printStackTrace();
            return "error";
        }
        request.setAttribute("hrTree", categoryList);
        return "success";
    }

    private static List<Map<String, Object>> getCurrentEmployeeDetails(Map<String, Object> params) throws GenericEntityException{

        Delegator delegator = (Delegator) params.get("delegator");
        String partyId = (String) params.get("partyId");
        String onclickFunction = (String) params.get("onclickFunction");
        String additionParam = (String) params.get("additionParam");
        String hrefString = (String) params.get("hrefString");
        String hrefString2 = (String) params.get("hrefString2");

        List<Map<String, Object>> responseList = new ArrayList<>();

        long emplPosCount;
        try {
            emplPosCount = EntityQuery.use(delegator).from("EmplPosition")
                    .where("emplPositionId", partyId).queryCount();
            if (emplPosCount > 0) {
                String emplId = partyId;
                List<GenericValue> emlpfillCtxs = EntityQuery.use(delegator).from("EmplPositionFulfillment")
                        .where("emplPositionId", emplId)
                        .filterByDate().queryList();
                if (UtilValidate.isNotEmpty(emlpfillCtxs)) {
                    for (GenericValue emlpfillCtx : emlpfillCtxs ) {
                        String memberId = emlpfillCtx.getString("partyId");
                        GenericValue memCtx = EntityQuery.use(delegator).from("Person").where("partyId", partyId).queryOne();
                        String title = null;
                        if (UtilValidate.isNotEmpty(memCtx)) {
                            String firstname = memCtx.getString("firstName");
                            String lastname = memCtx.getString("lastName");
                            if (UtilValidate.isEmpty(lastname)) {
                                lastname = "";
                            }
                            if (UtilValidate.isEmpty(firstname)) {
                                firstname = "";
                            }
                            title = firstname +" "+ lastname;
                        }
                        GenericValue memGroupCtx = EntityQuery.use(delegator).from("PartyGroup").where("partyId", partyId).queryOne();
                        if (UtilValidate.isNotEmpty(memGroupCtx)) {
                            title = memGroupCtx.getString("groupName");
                        }

                        Map<String,Object> josonMap = new HashMap<String, Object>();
                        Map<String,Object> dataMap = new HashMap<String, Object>();
                        Map<String,Object> dataAttrMap = new HashMap<String, Object>();
                        Map<String,Object> attrMap = new HashMap<String, Object>();

                        dataAttrMap.put("onClick", onclickFunction + "('" + memberId + additionParam + "')");

                        String hrefStr = hrefString + memberId;
                        if (UtilValidate.isNotEmpty(hrefString2)) {
                            hrefStr = hrefStr + hrefString2;
                        }
                        dataAttrMap.put("href", hrefStr);

                        attrMap.put("rel", "P");
                        attrMap.put("id", memberId);

                        dataMap.put("title", title);
                        dataMap.put("attr", dataAttrMap);

                        josonMap.put("attr",attrMap);
                        josonMap.put("data", dataMap);

                        responseList.add(josonMap) ;
                    }
                }
            }
        } catch (GenericEntityException e) {
            e.printStackTrace();
            throw new GenericEntityException(e);
        }

        return responseList;
    }

    private static List<Map<String, Object>> getChildComps(Map<String, Object> params) throws GenericEntityException{

        Delegator delegator = (Delegator) params.get("delegator");
        String onclickFunction = (String) params.get("onclickFunction");
        String additionParam = (String) params.get("additionParam");
        String hrefString = (String) params.get("hrefString");
        String hrefString2 = (String) params.get("hrefString2");

        Map<String , Object> partyGroup = (Map<String, Object>) params.get("partyGroup");
        List<Map<String, Object>> resultList = new ArrayList<Map<String,Object>>();
        List<GenericValue> childOfComs = null;

        try {
            childOfComs = EntityQuery.use(delegator).from("PartyRelationship")
                    .where("partyIdFrom", partyGroup.get("partyId"), 
                            "partyRelationshipTypeId", "GROUP_ROLLUP")
                            .filterByDate().queryList();
            if (UtilValidate.isNotEmpty(childOfComs)) {

                for (GenericValue childOfCom : childOfComs ) {
                    Object catId = null;
                    String catNameField = null;
                    String title = null;

                    Map<String, Object> josonMap = new HashMap<String, Object>();
                    Map<String, Object> dataMap = new HashMap<String, Object>();
                    Map<String, Object> dataAttrMap = new HashMap<String, Object>();
                    Map<String, Object> attrMap = new HashMap<String, Object>();

                    catId = childOfCom.get("partyIdTo");

                    //Department or Sub department
                    GenericValue childContext = EntityQuery.use(delegator).from("PartyGroup").where("partyId", catId).queryOne();
                    if (UtilValidate.isNotEmpty(childContext)) {
                        catNameField = (String) childContext.get("groupName");
                        title = catNameField;
                        josonMap.put("title",title);

                    }
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

                    //Employee
                    GenericValue emContext = EntityQuery.use(delegator).from("Person").where("partyId", catId).queryOne();
                    if (UtilValidate.isNotEmpty(emContext)) {
                        String firstname = (String) emContext.get("firstName");
                        String lastname = (String) emContext.get("lastName");
                        if (UtilValidate.isEmpty(lastname)) {
                            lastname = "";
                        }
                        if (UtilValidate.isEmpty(firstname)) {
                            firstname = "";
                        }
                        title = firstname +" "+ lastname;
                    }

                    dataAttrMap.put("onClick", onclickFunction + "('" + catId + additionParam + "')");

                    String hrefStr = hrefString + catId;
                    if (UtilValidate.isNotEmpty(hrefString2)) {
                        hrefStr = hrefStr + hrefString2;
                    }
                    dataAttrMap.put("href", hrefStr);

                    dataMap.put("attr", dataAttrMap);
                    dataMap.put("title", title);

                    attrMap.put("rel", "Y");
                    attrMap.put("id", catId);


                    josonMap.put("attr",attrMap);
                    josonMap.put("data", dataMap);

                    resultList.add(josonMap);
                }  
            }
        } catch (GenericEntityException e) {
            e.printStackTrace();
            throw new GenericEntityException(e);
        }

        return resultList;

    }

    private static List<Map<String, Object>> getEmployeeInComp(Map<String, Object> params) throws GenericEntityException{
        List<GenericValue> isEmpls = null;
        Delegator delegator = (Delegator) params.get("delegator");
        String partyId = (String) params.get("partyId");

        List<Map<String, Object>> resultList = new ArrayList<Map<String,Object>>();

        try {
            isEmpls = EntityQuery.use(delegator).from("EmplPosition")
                    .where(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId),
                            EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "EMPL_POS_INACTIVE"))
                            .filterByDate("actualFromDate", "actualThruDate")
                            .queryList();

            if (UtilValidate.isNotEmpty(isEmpls)) {
                for (GenericValue childOfEmpl : isEmpls ) {
                    Map<String, Object> emplMap = new HashMap<String, Object>();
                    Map<String, Object> emplAttrMap = new HashMap<String, Object>();
                    Map<String, Object> empldataMap = new HashMap<String, Object>();
                    Map<String, Object> emplDataAttrMap = new HashMap<String, Object>();

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
                        title = (String) emplContext.get("description") + " " +"["+ emplId +"]";
                    }

                    String hrefStr = "emplPositionView?emplPositionId=" + emplId;
                    emplAttrMap.put("href", hrefStr);
                    emplAttrMap.put("onClick", "callEmplDocument" + "('" + emplId + "')");

                    empldataMap.put("title", title);
                    empldataMap.put("attr", emplAttrMap);

                    emplDataAttrMap.put("id", emplId);
                    emplDataAttrMap.put("rel", "N");

                    emplMap.put("data", empldataMap);
                    emplMap.put("attr",emplDataAttrMap);
                    emplMap.put("title",title);

                    resultList.add(emplMap);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new GenericEntityException(e);
        }

        return resultList;
    }
}
