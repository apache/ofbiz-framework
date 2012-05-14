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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.json.JSONObject;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;

public class HumanResEvents {
    public static final String module = HumanResEvents.class.getName();
    public static final String resourceError = "ProductErrorUiLabels";
    
    // Please note : the structure of map in this function is according to the JSON data map of the jsTree
    @SuppressWarnings("unchecked")
    public static void getChildHRCategoryTree(HttpServletRequest request, HttpServletResponse response){
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String partyId = request.getParameter("partyId");
        String onclickFunction = request.getParameter("onclickFunction");
        String additionParam = request.getParameter("additionParam");
        String hrefString = request.getParameter("hrefString");
        String hrefString2 = request.getParameter("hrefString2");
        
        List categoryList = FastList.newInstance();
        List<GenericValue> childOfComs;
        //check employee position
        try {
            List<GenericValue> isEmpl = delegator.findByAnd("EmplPosition", UtilMisc.toMap(
                    "emplPositionId", partyId), null, false);
            if (UtilValidate.isNotEmpty(isEmpl)) {
                String emplId = partyId;
                List<GenericValue> emlpfillCtxs = EntityUtil.filterByDate(delegator.findByAnd("EmplPositionFulfillment", UtilMisc.toMap(
                        "emplPositionId", emplId), null, false));
                if (UtilValidate.isNotEmpty(emlpfillCtxs)) {
                    for (GenericValue emlpfillCtx : emlpfillCtxs ) {
                        String memberId = emlpfillCtx.getString("partyId");
                        GenericValue memCtx = delegator.findOne("Person" ,UtilMisc.toMap("partyId", memberId), false);
                        String title = null;
                        if (UtilValidate.isNotEmpty(memCtx)) {
                            String firstname = (String) memCtx.get("firstName");
                            String lastname = (String) memCtx.get("lastName");
                            if (UtilValidate.isEmpty(lastname)) {
                                lastname = "";
                            }
                            if (UtilValidate.isEmpty(firstname)) {
                                firstname = "";
                            }
                            title = firstname +" "+ lastname;
                        }
                        GenericValue memGroupCtx = delegator.findOne("PartyGroup" ,UtilMisc.toMap("partyId", memberId), false);
                        if (UtilValidate.isNotEmpty(memGroupCtx)) {
                            title = memGroupCtx.getString("groupName");
                        }
                        
                        Map josonMap = FastMap.newInstance();
                        Map dataMap = FastMap.newInstance();
                        Map dataAttrMap = FastMap.newInstance();
                        Map attrMap = FastMap.newInstance();
                        
                        dataAttrMap.put("onClick", onclickFunction + "('" + memberId + additionParam + "')");
                        
                        String hrefStr = hrefString + memberId;
                        if (UtilValidate.isNotEmpty(hrefString2)) {
                            hrefStr = hrefStr + hrefString2;
                        }
                        dataAttrMap.put("href", hrefStr);
                        attrMap.put("rel", "P");
                        dataMap.put("attr", dataAttrMap);
                        attrMap.put("id", memberId);
                        josonMap.put("attr",attrMap);
                        dataMap.put("title", title);
                        josonMap.put("data", dataMap);
                        
                        categoryList.add(josonMap);
                    }
                    toJsonObjectList(categoryList,response);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        try {
            GenericValue partyGroup = delegator.findOne("PartyGroup" ,UtilMisc.toMap("partyId", partyId), false);
            if (UtilValidate.isNotEmpty(partyGroup)) {
                childOfComs = EntityUtil.filterByDate(delegator.findByAnd("PartyRelationship", UtilMisc.toMap(
                        "partyIdFrom", partyGroup.get("partyId"), "partyRelationshipTypeId", "GROUP_ROLLUP"), null, false));
                if (UtilValidate.isNotEmpty(childOfComs)) {
                    
                    for (GenericValue childOfCom : childOfComs ) {
                        Object catId = null;
                        String catNameField = null;
                        String title = null;
                        
                        Map josonMap = FastMap.newInstance();
                        Map dataMap = FastMap.newInstance();
                        Map dataAttrMap = FastMap.newInstance();
                        Map attrMap = FastMap.newInstance();
                        
                        catId = childOfCom.get("partyIdTo");
                        
                        //Department or Sub department
                        GenericValue childContext = delegator.findOne("PartyGroup" ,UtilMisc.toMap("partyId", catId), false);
                        if (UtilValidate.isNotEmpty(childContext)) {
                            catNameField = (String) childContext.get("groupName");
                            title = catNameField;
                            josonMap.put("title",title);
                            
                        }
                        //Check child existing
                        List<GenericValue> childOfSubComs = EntityUtil.filterByDate(delegator.findByAnd("PartyRelationship", UtilMisc.toMap(
                                "partyIdFrom", catId, "partyRelationshipTypeId", "GROUP_ROLLUP"), null, false));
                        //check employee position
                        List<GenericValue> isPosition = delegator.findByAnd("EmplPosition", UtilMisc.toMap(
                                "partyId", catId), null, false);
                        if (UtilValidate.isNotEmpty(childOfSubComs) || UtilValidate.isNotEmpty(isPosition)) {
                            josonMap.put("state", "closed");
                        }
                        
                        //Employee
                        GenericValue emContext = delegator.findOne("Person" ,UtilMisc.toMap("partyId", catId), false);
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
                        
                        attrMap.put("rel", "Y");
                        attrMap.put("id", catId);
                        josonMap.put("attr",attrMap);
                        dataMap.put("title", title);
                        josonMap.put("data", dataMap);
                        
                        categoryList.add(josonMap);
                }
                    
                }
                
                List<EntityExpr> exprs = FastList.newInstance();
                exprs.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
                exprs.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "EMPL_POS_INACTIVE"));
        
                List<GenericValue> isEmpls = null;
                try {
                    isEmpls = delegator.findList("EmplPosition", EntityCondition.makeCondition(exprs, EntityOperator.AND), null, null, null, false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
                
                isEmpls = EntityUtil.filterByDate(isEmpls, UtilDateTime.nowTimestamp(), "actualFromDate", "actualThruDate", true);
                if (UtilValidate.isNotEmpty(isEmpls)) {
                    for (GenericValue childOfEmpl : isEmpls ) {
                        Map emplMap = FastMap.newInstance();
                        Map emplAttrMap = FastMap.newInstance();
                        Map empldataMap = FastMap.newInstance();
                        Map emplDataAttrMap = FastMap.newInstance();
                        
                        String emplId = (String) childOfEmpl.get("emplPositionId");
                        String typeId = (String) childOfEmpl.get("emplPositionTypeId");
                        //check child
                        List<GenericValue> emlpfCtxs = EntityUtil.filterByDate(delegator.findByAnd("EmplPositionFulfillment", UtilMisc.toMap(
                                "emplPositionId", emplId), null, false));
                        if (UtilValidate.isNotEmpty(emlpfCtxs)) {
                            emplMap.put("state", "closed");
                        }
                        
                        GenericValue emplContext = delegator.findOne("EmplPositionType" ,UtilMisc.toMap("emplPositionTypeId", typeId), false);
                        String title = null;
                        if (UtilValidate.isNotEmpty(emplContext)) {
                            title = (String) emplContext.get("description") + " " +"["+ emplId +"]";
                        }
                        String hrefStr = "emplPositionView?emplPositionId=" + emplId;
                        empldataMap.put("title", title);
                        emplAttrMap.put("href", hrefStr);
                        emplAttrMap.put("onClick", "callEmplDocument" + "('" + emplId + "')");
                        empldataMap.put("attr", emplAttrMap);
                        emplMap.put("data", empldataMap);
                        emplDataAttrMap.put("id", emplId);
                        emplDataAttrMap.put("rel", "N");
                        emplMap.put("attr",emplDataAttrMap);
                        emplMap.put("title",title);
                        categoryList.add(emplMap);
                    }
                }
                
                toJsonObjectList(categoryList,response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @SuppressWarnings("unchecked")
    public static void toJsonObjectList(List attrList, HttpServletResponse response){
        String jsonStr = "[";
        for (Object attrMap : attrList) {
            JSONObject json = JSONObject.fromObject(attrMap);
            jsonStr = jsonStr + json.toString() + ',';
        }
        jsonStr = jsonStr + "{ } ]";
        if (UtilValidate.isEmpty(jsonStr)) {
            Debug.logError("JSON Object was empty; fatal error!",module);
        }
        // set the X-JSON content type
        response.setContentType("application/json");
        // jsonStr.length is not reliable for unicode characters
        try {
            response.setContentLength(jsonStr.getBytes("UTF8").length);
        } catch (UnsupportedEncodingException e) {
            Debug.logError("Problems with Json encoding",module);
        }
        // return the JSON String
        Writer out;
        try {
            out = response.getWriter();
            out.write(jsonStr);
            out.flush();
        } catch (IOException e) {
            Debug.logError("Unable to get response writer",module);
        }
    }
}
