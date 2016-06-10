/*
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
 */
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;

webSiteList = [];
webSite = null;
if (parameters.productStoreId) {
    productStoreId = parameters.productStoreId;
    webSiteList = from("WebSite").where("productStoreId", productStoreId).queryList();
    if (parameters.webSiteId) {
        webSite = from("WebSite").where("webSiteId", parameters.webSiteId).cache(true).queryOne();
        context.selectedWebSiteId = parameters.webSiteId;
    } else if (webSiteList) {
        webSite = EntityUtil.getFirst(webSiteList);
        context.selectedWebSiteId = webSite.webSiteId;
    }
    context.productStoreId = productStoreId;
    context.webSiteList = webSiteList;
    countryCode = null;
    if (parameters.country) {
        countryCode = parameters.country;
    } else {
        countryCode = "US";
    }
    context.countryCode = countryCode;
    if (webSite) {
        eBayConfig = from("EbayConfig").where("productStoreId", productStoreId).queryOne();
        context.customXml = eBayConfig.customXml;
        context.webSiteUrl = webSite.getString("standardContentPrefix");
        
        categoryCode = parameters.categoryCode;
        context.categoryCode = categoryCode; 
        userLogin = parameters.userLogin;
        
        if (productStoreId) {
            results = runService('getEbayCategories', [categoryCode : categoryCode, userLogin : userLogin, productStoreId : productStoreId]);
        }
        
        if (results.categories) {
            context.categories = results.categories;
        }
        
        if (categoryCode) {
            if (!"Y".equals(categoryCode.substring(0, 1)) && !"".equals(categoryCode)) {
                context.hideExportOptions = "Y";
            } else {
                context.hideExportOptions = "N";
            }
        } else {
            context.hideExportOptions = "N";
        }    
    }
}
