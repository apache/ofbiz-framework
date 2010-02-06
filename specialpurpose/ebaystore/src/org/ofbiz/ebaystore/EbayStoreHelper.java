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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.party.contact.ContactHelper;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ebay.sdk.ApiAccount;
import com.ebay.sdk.ApiContext;
import com.ebay.sdk.ApiCredential;
import com.ebay.sdk.ApiLogging;
import com.ebay.soap.eBLBaseComponents.SiteCodeType;

import org.ofbiz.ebay.EbayHelper;

public class EbayStoreHelper {
    private static final String configFileName = "ebayStore.properties";
    private static final String module = EbayStoreHelper.class.getName();
    public static final String resource = "EbayStoreUiLabels";

    public static ApiContext getApiContext(String productStoreId,Locale locale, Delegator delegator){
       Map<String, Object> context = FastMap.newInstance();
       context.put("locale", locale);
       context.put("productStoreId", productStoreId);
       Map<String, Object> config = EbayHelper.buildEbayConfig(context, delegator);
       ApiCredential apiCredential = new ApiCredential();
       ApiLogging apiLogging = new ApiLogging();
       apiLogging.setEnableLogging(false);
       apiLogging.setLogExceptions(false);
       apiLogging.setLogSOAPMessages(false);

       String devID = (String)config.get("devId");
        String appID = (String)config.get("appID");
        String certID = (String)config.get("certID");
        String token = (String)config.get("token");
        String apiServerUrl = (String)config.get("apiServerUrl");

       if(token != null){
           apiCredential.seteBayToken(token);
       }else if(devID != null && appID != null && certID != null){
           ApiAccount apiAccount = new ApiAccount();
           apiAccount.setApplication(appID);
           apiAccount.setCertificate(certID);
           apiAccount.setDeveloper(devID);
           apiCredential.setApiAccount(apiAccount);
       }
       ApiContext apiContext = new ApiContext();
       apiContext.setApiCredential(apiCredential);
       apiContext.setApiServerUrl(apiServerUrl);
       apiContext.setApiLogging(apiLogging); 
       apiContext.setErrorLanguage("en_US");
       return apiContext;
   }
    
   public static SiteCodeType getSiteCodeType(String productStoreId,Locale locale, Delegator delegator){
        Map<String, Object> context = FastMap.newInstance();
        context.put("locale", locale);
        context.put("productStoreId", productStoreId);
        Map<String, Object> config = EbayHelper.buildEbayConfig(context, delegator);
        String siteId = (String)config.get("siteID");
        if(siteId != null){
	        if(siteId.equals("0")) return SiteCodeType.US;
	        if(siteId.equals("2")) return SiteCodeType.CANADA;
	        if(siteId.equals("3")) return SiteCodeType.UK;
	        if(siteId.equals("15")) return SiteCodeType.AUSTRALIA;
	        if(siteId.equals("16")) return SiteCodeType.AUSTRIA;
	        if(siteId.equals("23")) return SiteCodeType.BELGIUM_FRENCH;
	        if(siteId.equals("71")) return SiteCodeType.FRANCE;
	        if(siteId.equals("77")) return SiteCodeType.GERMANY;
	        if(siteId.equals("100")) return SiteCodeType.E_BAY_MOTORS;
	        if(siteId.equals("101")) return SiteCodeType.ITALY;
	        if(siteId.equals("123")) return SiteCodeType.BELGIUM_DUTCH;
	        if(siteId.equals("146")) return SiteCodeType.NETHERLANDS;
	        if(siteId.equals("189")) return SiteCodeType.SPAIN;
	        if(siteId.equals("193")) return SiteCodeType.SWITZERLAND;
	        if(siteId.equals("196")) return SiteCodeType.TAIWAN;
	        if(siteId.equals("201")) return SiteCodeType.HONG_KONG;
	        if(siteId.equals("203")) return SiteCodeType.INDIA;
	        if(siteId.equals("205")) return SiteCodeType.IRELAND;
	        if(siteId.equals("207")) return SiteCodeType.MALAYSIA;
	        if(siteId.equals("210")) return SiteCodeType.CANADA_FRENCH;
	        if(siteId.equals("211")) return SiteCodeType.PHILIPPINES;
	        if(siteId.equals("212")) return SiteCodeType.POLAND;
	        if(siteId.equals("216")) return SiteCodeType.SINGAPORE;
	        if(siteId.equals("218")) return SiteCodeType.SWEDEN;
	        if(siteId.equals("223")) return SiteCodeType.CHINA;
        }
        return SiteCodeType.US;
    }
   
   public static boolean validatePartyAndRoleType(Delegator delegator,String partyId){
	   GenericValue partyRole = null;
	   try {
		   if (partyId == null) {
			   Debug.logError("Require field partyId.",module);
			   return false;
		   }
		   partyRole = delegator.findByPrimaryKey("PartyRole", UtilMisc.toMap("partyId", partyId,"roleTypeId","EBAY_ACCOUNT"));
		   if (partyRole == null) {
			   Debug.logError("Party Id ".concat(partyId).concat("not have roleTypeId EBAY_ACCOUNT"),module);
			   return false;
		   }
	   } catch (GenericEntityException e) {
		   Debug.logError(e.getMessage(), module);
		   return false;
	   }
	   return true;
   }
   public static String retriveEbayCategoryIdByPartyId(Delegator delegator,String productCategoryId,String partyId){
	   String ebayCategoryId = null;
	   List<GenericValue> productCategoryRoles = null;
	   try {
		   if (partyId == null) {
			   Debug.logError("Require field partyId.",module);
			   return ebayCategoryId;
		   }
		   productCategoryRoles = delegator.findByAnd("ProductCategoryRole", UtilMisc.toMap("productCategoryId",productCategoryId,"partyId", partyId,"roleTypeId","EBAY_ACCOUNT"));
		   if (productCategoryRoles != null && productCategoryRoles.size()>0) {
			   for (GenericValue productCategoryRole : productCategoryRoles) {
				   ebayCategoryId = productCategoryRole.getString("comments");
			   }
		   } else {
			   Debug.logInfo("Party Id ".concat(partyId).concat(" Not found productCategoryRole with productCategoryId "+productCategoryId),module);
			   return ebayCategoryId;
		   }
	   } catch (GenericEntityException e) {
		   Debug.logError(e.getMessage(), module);
	   }
	   return ebayCategoryId;
   }
   public static boolean createEbayCategoryIdByPartyId(Delegator delegator,String productCategoryId,String partyId,String ebayCategoryId){
	   
	   try {
		   if (partyId == null && ebayCategoryId != null) {
			   Debug.logError("Require field partyId and ebayCategoryId.",module);
			   return false;
		   }
		   GenericValue productCategoryRole = delegator.makeValue("ProductCategoryRole");
		   productCategoryRole.put("productCategoryId",productCategoryId);
		   productCategoryRole.put("partyId", partyId);
		   productCategoryRole.put("roleTypeId","EBAY_ACCOUNT");
		   productCategoryRole.put("fromDate",UtilDateTime.nowTimestamp());
		   productCategoryRole.put("comments",ebayCategoryId);
		   productCategoryRole.create();

	   } catch (GenericEntityException e) {
		   Debug.logError(e.getMessage(), module);
		   return false;
	   }
	   return true;
   }
   public static boolean veriflyCategoryInCatalog(Delegator delegator,List<GenericValue> catalogCategories,String productCategoryId){
	   boolean flag = false;
	   try {
		   for (GenericValue catalogCategory : catalogCategories) {
			   // check in productCatalogCategory first level 0
			   if (catalogCategory.containsValue(productCategoryId)) {
				   flag = true;
				   break;
			   } else {
				   // check from child category level 1
				   List<GenericValue> productCategoryRollupList = delegator.findByAnd("ProductCategoryRollup",  UtilMisc.toMap("parentProductCategoryId",catalogCategory.getString("productCategoryId")));
				   for (GenericValue productCategoryRollup : productCategoryRollupList) {
					   if (productCategoryRollup.containsValue(productCategoryId)) {
						   flag = true;
						   break;
					   } else {
						   // check from level 2
						   List<GenericValue> prodCategoryRollupList = delegator.findByAnd("ProductCategoryRollup",  UtilMisc.toMap("parentProductCategoryId",productCategoryRollup.getString("productCategoryId")));
						   for (GenericValue prodCategoryRollup : prodCategoryRollupList) {
							   if (prodCategoryRollup.containsValue(productCategoryId)) {
								   flag = true;
								   break;
							   }
						   }
					   }
				   }
			   }
		   }
	   } catch (GenericEntityException e) {
		   Debug.logError(e.getMessage(), module);
		   return false;
	   }
	   return flag;
   }
}
