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

package org.ofbiz.ebay;

import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

public class EbayHelper { 
    private static final String configFileName = "ebayExport.properties";
    private static final String module = EbayHelper.class.getName();

    public static Map<String, Object> buildEbayConfig(Map<String, Object> context, GenericDelegator delegator) {
        Map<String, Object> buildEbayConfigContext = FastMap.newInstance();
        String productStoreId = (String) context.get("productStoreId");
        if (UtilValidate.isNotEmpty(productStoreId)) {
            GenericValue eBayConfig = null;
            try {
                eBayConfig = delegator.findOne("EbayConfig", false, UtilMisc.toMap("productStoreId", productStoreId));
            } catch (GenericEntityException e) {
                Debug.logError("Unable to find value for EbayConfig", module);
                e.printStackTrace();
            }
            if (UtilValidate.isNotEmpty(eBayConfig)) {
                buildEbayConfigContext.put("devID", eBayConfig.getString("devId"));
                buildEbayConfigContext.put("appID", eBayConfig.getString("appId"));
                buildEbayConfigContext.put("certID", eBayConfig.getString("certId"));
                buildEbayConfigContext.put("token", eBayConfig.getString("token"));
                buildEbayConfigContext.put("compatibilityLevel", eBayConfig.getString("compatibilityLevel"));
                buildEbayConfigContext.put("siteID", eBayConfig.getString("siteId"));
                buildEbayConfigContext.put("xmlGatewayUri", eBayConfig.getString("xmlGatewayUri"));
            }
        } else {
            buildEbayConfigContext.put("devID", UtilProperties.getPropertyValue(configFileName, "eBayExport.devID"));
            buildEbayConfigContext.put("appID", UtilProperties.getPropertyValue(configFileName, "eBayExport.appID"));
            buildEbayConfigContext.put("certID", UtilProperties.getPropertyValue(configFileName, "eBayExport.certID"));
            buildEbayConfigContext.put("token", UtilProperties.getPropertyValue(configFileName, "eBayExport.token"));
            buildEbayConfigContext.put("compatibilityLevel", UtilProperties.getPropertyValue(configFileName, "eBayExport.compatibilityLevel"));
            buildEbayConfigContext.put("siteID", UtilProperties.getPropertyValue(configFileName, "eBayExport.siteID"));
            buildEbayConfigContext.put("xmlGatewayUri", UtilProperties.getPropertyValue(configFileName, "eBayExport.xmlGatewayUri"));
        }    
        return buildEbayConfigContext;
    }    
}