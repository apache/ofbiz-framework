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
package org.ofbiz.product.store;

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
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.util.EntityUtil;

public class ProductStoreEvents {

    public static final String module = ProductStoreWorker.class.getName();

    // Please note : the structure of map in this function is according to the JSON data map of the jsTree
    @SuppressWarnings("unchecked")
    public static void getChildProductStoreGroupTree(HttpServletRequest request, HttpServletResponse response){
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String parentGroupId = request.getParameter("parentGroupId");
        String onclickFunction = request.getParameter("onclickFunction");

        List productStoreGroupList = FastList.newInstance();
        List<GenericValue> children;
        List<String> sortList = org.ofbiz.base.util.UtilMisc.toList("sequenceNum");

        try {
            GenericValue productStoreGroup = delegator.findOne("ProductStoreGroup" ,UtilMisc.toMap("productStoreGroupId", parentGroupId), true);
            if (UtilValidate.isNotEmpty(productStoreGroup)) {
                children = EntityUtil.filterByDate(delegator.findList("ProductStoreGroupRollupAndChild",
                        EntityCondition.makeCondition("parentGroupId", parentGroupId), null, null, null, true));
                if (UtilValidate.isNotEmpty(children)) {
                    for (GenericValue child : children ) {
                        String productStoreGroupId = child.getString("productStoreGroupId");
                        Map josonMap = FastMap.newInstance();
                        List<GenericValue> childList = null;
                        // Get the child list of chosen category
                        childList = EntityUtil.filterByDate(delegator.findList("ProductStoreGroupRollupAndChild",
                                EntityCondition.makeCondition("parentGroupId", productStoreGroupId), null, null, null, true));

                        if (UtilValidate.isNotEmpty(childList)) {
                            josonMap.put("state", "closed");
                        }
                        Map dataMap = FastMap.newInstance();
                        Map dataAttrMap = FastMap.newInstance();

                        dataAttrMap.put("onClick", onclickFunction + "('" + productStoreGroupId + "')");
                        String hrefStr = "EditProductStoreGroupAndAssoc"; 
                        dataAttrMap.put("href", hrefStr);

                        dataMap.put("attr", dataAttrMap);
                        dataMap.put("title", child.get("productStoreGroupName") + " [" + child.get("productStoreGroupId") + "]");
                        josonMap.put("data", dataMap);
                        Map attrMap = FastMap.newInstance();
                        attrMap.put("parentGroupId", productStoreGroupId);
                        josonMap.put("attr",attrMap);
                        josonMap.put("sequenceNum",child.get("sequenceNum"));
                        josonMap.put("title", child.get("productStoreGroupName"));

                        productStoreGroupList.add(josonMap);
                    }
                    List<Map<Object, Object>> sortedProductStoreGroupList = UtilMisc.sortMaps(productStoreGroupList, sortList);
                    toJsonObjectList(sortedProductStoreGroupList,response);
                }
            }
        } catch (GenericEntityException e) {
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
