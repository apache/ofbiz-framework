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
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
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
        
        try {
            GenericValue partyGroup = delegator.findByPrimaryKey("PartyGroup" ,UtilMisc.toMap("partyId", partyId));
            if (UtilValidate.isNotEmpty(partyGroup)) {
                childOfComs = EntityUtil.filterByDate(delegator.findByAnd("PartyRelationship", UtilMisc.toMap(
                        "partyIdFrom", partyGroup.get("partyId"), "partyRelationshipTypeId", "GROUP_ROLLUP")));
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
                        GenericValue childContext = delegator.findByPrimaryKey("PartyGroup" ,UtilMisc.toMap("partyId", catId));
                        if (UtilValidate.isNotEmpty(childContext)) {
                            catNameField = (String) childContext.get("groupName");
                            title = catNameField;
                            josonMap.put("title",title);
                            
                        }
                        //Check child existing
                        List<GenericValue> childOfSubComs = EntityUtil.filterByDate(delegator.findByAnd("PartyRelationship", UtilMisc.toMap(
                                "partyIdFrom", catId, "partyRelationshipTypeId", "GROUP_ROLLUP")));
                        if (UtilValidate.isNotEmpty(childOfSubComs)) {
                            josonMap.put("state", "closed");
                        }
                        
                        //Employee
                        GenericValue emContext = delegator.findByPrimaryKey("Person" ,UtilMisc.toMap("partyId", catId));
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
                        
                        attrMap.put("id", catId);
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
