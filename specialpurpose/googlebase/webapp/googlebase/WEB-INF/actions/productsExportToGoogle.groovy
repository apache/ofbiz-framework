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
import org.ofbiz.base.util.*;

uiLabelMap = UtilProperties.getResourceBundleMap("GoogleBaseUiLabels", locale);

webSiteList = [];
webSite = null;
if (parameters.productStoreId) {
    productStoreId = parameters.productStoreId;
    webSiteList = delegator.findList("WebSite", EntityCondition.makeCondition("productStoreId", EntityOperator.EQUALS, productStoreId), null, null, null, false);
    if (parameters.webSiteId) {
        webSite = delegator.findOne("WebSite", ["webSiteId" : parameters.webSiteId], true);
        context.webSiteId = parameters.webSiteId;
    } else if (webSiteList) {
        webSite = EntityUtil.getFirst(webSiteList);
        context.webSiteId = webSite.webSiteId;
    }
    context.productStoreId = productStoreId;
    context.webSiteList = webSiteList;
    context.webSiteUrl = webSite.standardContentPrefix;
    parameters.webSiteUrl = webSite.standardContentPrefix;;
}

if (parameters.productStoreId) {
    productStore = delegator.findByAnd("ProductStore", ["productStoreId":parameters.productStoreId], null, false);
    str = productStore[0].defaultLocaleString.toString().toUpperCase();
    localeString = str.substring(str.length()-2, str.length());
    if(localeString.equals("US")){
        context.showText = uiLabelMap.GoogleBaseExportCountryCodeUS;
    }else if(localeString.equals("GB")){
        context.showText = uiLabelMap.GoogleBaseExportCountryCodeGB;
    }else{ // "DE"
        context.showText = uiLabelMap.GoogleBaseExportCountryCodeDE;
    }
    context.countryCode = localeString;
}
