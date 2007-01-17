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
package org.ofbiz.pos.container;

import java.util.Locale;

import org.ofbiz.base.container.ContainerConfig;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.guiapp.xui.XuiContainer;
import org.ofbiz.guiapp.xui.XuiSession;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.product.store.ProductStoreWorker;

public class PosContainer extends XuiContainer {

    public String getContainerConfigName() {
        return "pos-container";
    }

    public void configure(ContainerConfig.Container cc) throws ContainerException {
        XuiSession session = XuiContainer.getSession();
        GenericValue productStore = null;
        GenericValue facility = null;

        // get the facility id
        String facilityId = ContainerConfig.getPropertyValue(cc, "facility-id", null);
        if (UtilValidate.isEmpty(facilityId)) {
            throw new ContainerException("No facility-id value set in pos-container!");
        } else {
            try {
                facility = session.getDelegator().findByPrimaryKey("Facility", UtilMisc.toMap("facilityId", facilityId));
            } catch (GenericEntityException e) {
                throw new ContainerException("Invalid facilityId : " + facilityId);
            }
        }

        // verify the facility exists
        if (facility == null) {
            throw new ContainerException("Invalid facility; facility ID not found [" + facilityId + "]");
        }
        session.setAttribute("facilityId", facilityId);
        session.setAttribute("facility", facility);

        // get the product store id
        String productStoreId = facility.getString("productStoreId");
        if (UtilValidate.isEmpty(productStoreId)) {
            throw new ContainerException("No productStoreId set on facility [" + facilityId + "]!");
        } else {
            productStore = ProductStoreWorker.getProductStore(productStoreId, session.getDelegator());
            if (productStore == null) {
                throw new ContainerException("Invalid productStoreId : " + productStoreId);
            }
        }
        session.setAttribute("productStoreId", productStoreId);
        session.setAttribute("productStore", productStore);

        // get the store locale
        String localeStr = ContainerConfig.getPropertyValue(cc, "locale", null);
        if (UtilValidate.isEmpty(localeStr)) {
            localeStr = productStore.getString("defaultLocaleString");
        }
        if (UtilValidate.isEmpty(localeStr)) {
            throw new ContainerException("Invalid Locale for POS!");
        }
        Locale locale = UtilMisc.parseLocale(localeStr);
        session.setAttribute("locale", locale);

        // get the store currency
        String currencyStr = ContainerConfig.getPropertyValue(cc, "currency", null);
        if (UtilValidate.isEmpty(currencyStr)) {
            currencyStr = productStore.getString("defaultCurrencyUomId");
        }
        if (UtilValidate.isEmpty(currencyStr)) {
            throw new ContainerException("Invalid Currency for POS!");
        }
        session.setAttribute("currency", currencyStr);
    }
}
