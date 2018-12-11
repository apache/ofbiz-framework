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
package org.apache.ofbiz.product.store;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;

public class ProductStoreEvents {

    public static final String module = ProductStoreWorker.class.getName();

    // Please note : the structure of map in this function is according to the JSON data map of the jsTree
    public static String getChildProductStoreGroupTree(HttpServletRequest request, HttpServletResponse response){
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String parentGroupId = request.getParameter("parentGroupId");
        String onclickFunction = request.getParameter("onclickFunction");

        List<Map<Object, Object>> productStoreGroupList = new LinkedList<>();
        List<GenericValue> children;
        List<String> sortList = org.apache.ofbiz.base.util.UtilMisc.toList("sequenceNum");

        try {
            GenericValue productStoreGroup = EntityQuery.use(delegator).from("ProductStoreGroup").where("productStoreGroupId", parentGroupId).cache(true).queryOne();
            if (productStoreGroup != null) {
                children = EntityQuery.use(delegator).from("ProductStoreGroupRollupAndChild").where("parentGroupId", parentGroupId).cache(true).filterByDate().queryList();
                if (UtilValidate.isNotEmpty(children)) {
                    for (GenericValue child : children ) {
                        String productStoreGroupId = child.getString("productStoreGroupId");
                        Map<Object, Object> josonMap = new HashMap<>();
                        List<GenericValue> childList = null;
                        // Get the child list of chosen category
                        childList = EntityQuery.use(delegator).from("ProductStoreGroupRollupAndChild").where("parentGroupId", productStoreGroupId).cache(true).filterByDate().queryList();

                        if (UtilValidate.isNotEmpty(childList)) {
                            josonMap.put("state", "closed");
                        }
                        Map<String, Object> dataMap = new HashMap<>();
                        Map<String, String> dataAttrMap = new HashMap<>();

                        dataAttrMap.put("onClick", onclickFunction + "('" + productStoreGroupId + "')");
                        String hrefStr = "EditProductStoreGroupAndAssoc"; 
                        dataAttrMap.put("href", hrefStr);

                        dataMap.put("attr", dataAttrMap);
                        dataMap.put("title", child.get("productStoreGroupName") + " [" + child.get("productStoreGroupId") + "]");
                        josonMap.put("data", dataMap);
                        Map<String, String> attrMap = new HashMap<>();
                        attrMap.put("parentGroupId", productStoreGroupId);
                        josonMap.put("attr",attrMap);
                        josonMap.put("sequenceNum",child.get("sequenceNum"));
                        josonMap.put("title", child.get("productStoreGroupName"));

                        productStoreGroupList.add(josonMap);
                    }
                    List<Map<Object, Object>> sortedProductStoreGroupList = UtilMisc.sortMaps(productStoreGroupList, sortList);
                    request.setAttribute("storeGroupTree", sortedProductStoreGroupList);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return "error";
        }
        return "success";
    }
}
