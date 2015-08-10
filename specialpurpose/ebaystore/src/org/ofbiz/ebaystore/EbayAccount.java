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
package org.ofbiz.ebaystore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.entity.Delegator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

import com.ebay.sdk.ApiContext;
import com.ebay.sdk.call.GetUserCall;
import com.ebay.soap.eBLBaseComponents.AddressType;
import com.ebay.soap.eBLBaseComponents.DetailLevelCodeType;
import com.ebay.soap.eBLBaseComponents.UserType;

public class EbayAccount {
    
    public static Map<String, Object> getEbayUser(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object>result = new HashMap<String, Object>();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");

        // Output Variable
        String email = null,
            cityName = null, 
            companyName = null, 
            country = null, 
            firstName = null, 
            lastName = null,
            name = null,
            phone = null, 
            postalCode = null,
            stateOrProvince = null, 
            street = null, 
            street1 = null, 
            street2 = null,
            status = null;
        Map<String, Object>registrationAddress = new HashMap<String, Object>();

        try {
            ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
            GetUserCall getUserCall  = new GetUserCall(apiContext);
            DetailLevelCodeType[] detailLevel = {DetailLevelCodeType.RETURN_ALL};
            getUserCall.setDetailLevel(detailLevel);
            UserType  user = getUserCall.getUser();
            if (user != null) {
                email = user.getEmail();
                AddressType regAddress = user.getRegistrationAddress();
                if (regAddress != null) {
                    cityName = regAddress.getCityName();
                    companyName = regAddress.getCompanyName();
                    country = regAddress.getCountryName();
                    firstName = regAddress.getFirstName();
                    lastName = regAddress.getLastName();
                    name = regAddress.getName();
                    phone = regAddress.getPhone();
                    postalCode = regAddress.getPostalCode();
                    stateOrProvince = regAddress.getStateOrProvince();
                    street = regAddress.getStreet();
                    street1 = regAddress.getStreet1();
                    street2 = regAddress.getStreet2();
                }
                if (firstName == null && lastName == null && name !=null) {
                    String nameArray[] = name.split(" ");
                    firstName = nameArray[0];
                    lastName = nameArray[1];
                }
                registrationAddress.put("cityName", cityName);
                registrationAddress.put("companyName", companyName);
                registrationAddress.put("country", country);
                registrationAddress.put("firstName", firstName);
                registrationAddress.put("lastName", lastName);
                registrationAddress.put("phone", phone);
                registrationAddress.put("postalCode", postalCode);
                registrationAddress.put("stateOrProvince", stateOrProvince);
                registrationAddress.put("street", street);
                registrationAddress.put("street1", street1);
                registrationAddress.put("street2", street2);
                status = user.getStatus().toString();
            }
            result.put("email", email);
            result.put("registrationAddress", registrationAddress);
            result.put("status", status);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            return  ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }
}
