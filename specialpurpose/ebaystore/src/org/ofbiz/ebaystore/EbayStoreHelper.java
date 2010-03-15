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

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.serialize.SerializeException;
import org.ofbiz.entity.serialize.XmlSerializer;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.calendar.RecurrenceInfo;
import org.ofbiz.service.calendar.RecurrenceInfoException;
import org.ofbiz.service.config.ServiceConfigUtil;
import org.ofbiz.service.job.JobManager;

import com.ebay.sdk.ApiAccount;
import com.ebay.sdk.ApiContext;
import com.ebay.sdk.ApiCredential;
import com.ebay.sdk.ApiLogging;
import com.ebay.sdk.call.AddItemCall;
import com.ebay.soap.eBLBaseComponents.AddItemRequestType;
import com.ebay.soap.eBLBaseComponents.AddItemResponseType;
import com.ebay.soap.eBLBaseComponents.BuyerPaymentMethodCodeType;
import com.ebay.soap.eBLBaseComponents.GeteBayDetailsResponseType;
import com.ebay.soap.eBLBaseComponents.ItemType;
import com.ebay.soap.eBLBaseComponents.SiteCodeType;
import com.ebay.soap.eBLBaseComponents.ShippingLocationDetailsType;

import org.ofbiz.ebay.EbayHelper;

import sun.net.www.content.text.Generic;

public class EbayStoreHelper {
    private static final String configFileName = "ebayStore.properties";
    private static final String module = EbayStoreHelper.class.getName();
    public static final String resource = "EbayStoreUiLabels";

    public static ApiContext getApiContext(String productStoreId,Locale locale, Delegator delegator) {
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

       if (token != null) {
           apiCredential.seteBayToken(token);
       } else if (devID != null && appID != null && certID != null) {
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

    public static SiteCodeType getSiteCodeType(String productStoreId, Locale locale, Delegator delegator) {
        Map<String, Object> context = FastMap.newInstance();
        context.put("locale", locale);
        context.put("productStoreId", productStoreId);
        Map<String, Object> config = EbayHelper.buildEbayConfig(context, delegator);
        String siteId = (String)config.get("siteID");
        if (siteId != null) {
            if (siteId.equals("0")) return SiteCodeType.US;
            if (siteId.equals("2")) return SiteCodeType.CANADA;
            if (siteId.equals("3")) return SiteCodeType.UK;
            if (siteId.equals("15")) return SiteCodeType.AUSTRALIA;
            if (siteId.equals("16")) return SiteCodeType.AUSTRIA;
            if (siteId.equals("23")) return SiteCodeType.BELGIUM_FRENCH;
            if (siteId.equals("71")) return SiteCodeType.FRANCE;
            if (siteId.equals("77")) return SiteCodeType.GERMANY;
            if (siteId.equals("100")) return SiteCodeType.E_BAY_MOTORS;
            if (siteId.equals("101")) return SiteCodeType.ITALY;
            if (siteId.equals("123")) return SiteCodeType.BELGIUM_DUTCH;
            if (siteId.equals("146")) return SiteCodeType.NETHERLANDS;
            if (siteId.equals("189")) return SiteCodeType.SPAIN;
            if (siteId.equals("193")) return SiteCodeType.SWITZERLAND;
            if (siteId.equals("196")) return SiteCodeType.TAIWAN;
            if (siteId.equals("201")) return SiteCodeType.HONG_KONG;
            if (siteId.equals("203")) return SiteCodeType.INDIA;
            if (siteId.equals("205")) return SiteCodeType.IRELAND;
            if (siteId.equals("207")) return SiteCodeType.MALAYSIA;
            if (siteId.equals("210")) return SiteCodeType.CANADA_FRENCH;
            if (siteId.equals("211")) return SiteCodeType.PHILIPPINES;
            if (siteId.equals("212")) return SiteCodeType.POLAND;
            if (siteId.equals("216")) return SiteCodeType.SINGAPORE;
            if (siteId.equals("218")) return SiteCodeType.SWEDEN;
            if (siteId.equals("223")) return SiteCodeType.CHINA;
        }
        return SiteCodeType.US;
    }

    public static boolean validatePartyAndRoleType(Delegator delegator, String partyId) {
        GenericValue partyRole = null;
        try {
            if (partyId == null) {
                Debug.logError("Require field partyId.",module);
                return false;
            }
            partyRole = delegator.findByPrimaryKey("PartyRole", UtilMisc.toMap("partyId", partyId, "roleTypeId", "EBAY_ACCOUNT"));
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

    public static String retriveEbayCategoryIdByPartyId(Delegator delegator, String productCategoryId, String partyId) {
        String ebayCategoryId = null;
        List<GenericValue> productCategoryRoles = null;
        try {
            if (partyId == null) {
                Debug.logError("Require field partyId.",module);
                return ebayCategoryId;
            }
            productCategoryRoles = delegator.findByAnd("ProductCategoryRole", UtilMisc.toMap("productCategoryId", productCategoryId, "partyId", partyId, "roleTypeId", "EBAY_ACCOUNT"));
            if (productCategoryRoles != null && productCategoryRoles.size()>0) {
                for (GenericValue productCategoryRole : productCategoryRoles) {
                    ebayCategoryId = productCategoryRole.getString("comments");
                }
            } else {
                Debug.logInfo("Party Id ".concat(partyId).concat(" Not found productCategoryRole with productCategoryId "+ productCategoryId),module);
                return ebayCategoryId;
            }
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), module);
        }
        return ebayCategoryId;
    }

    public static boolean createEbayCategoryIdByPartyId(Delegator delegator, String productCategoryId, String partyId, String ebayCategoryId) {
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

    public static boolean veriflyCategoryInCatalog(Delegator delegator, List<GenericValue> catalogCategories, String productCategoryId) {
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

    public static Map<String, Object> startEbayAutoPreference(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object>result = FastMap.newInstance();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        String autoPrefEnumId = (String) context.get("autoPrefEnumId");
        String serviceName = (String) context.get("serviceName");
        try {
            GenericValue ebayProductPref = delegator.findByPrimaryKey("EbayProductStorePref", UtilMisc.toMap("productStoreId", productStoreId, "autoPrefEnumId", autoPrefEnumId));
            String jobId = ebayProductPref.getString("autoPrefJobId");
            if (UtilValidate.isNotEmpty(jobId)) {
                List<GenericValue> jobs = delegator.findByAnd("JobSandbox", UtilMisc.toMap("parentJobId", jobId, "statusId", "SERVICE_PENDING"));
                if (jobs.size() == 0) {
                    Map<String, Object>inMap = FastMap.newInstance();
                    inMap.put("jobId", jobId);
                    inMap.put("userLogin", userLogin);
                    dispatcher.runSync("resetScheduledJob", inMap);
                }
            }
            if (UtilValidate.isEmpty(ebayProductPref.getString("autoPrefJobId"))) {
                if (UtilValidate.isEmpty(serviceName)) return ServiceUtil.returnError("If you add a new job, you have to add serviec name.");
                /*** RuntimeData ***/
                String runtimeDataId = null;
                GenericValue runtimeData = delegator.makeValue("RuntimeData");
                runtimeData = delegator.createSetNextSeqId(runtimeData);
                runtimeDataId = runtimeData.getString("runtimeDataId");

                /*** JobSandbox ***/
                // create the recurrence
                String infoId = null;
                String jobName = null;
                long startTime = UtilDateTime.getNextDayStart(UtilDateTime.nowTimestamp()).getTime();
                RecurrenceInfo info;
                // run every day when day start
                info = RecurrenceInfo.makeInfo(delegator, startTime, 4, 1, -1);
                infoId = info.primaryKey();
                // set the persisted fields
                GenericValue enumeration = delegator.findByPrimaryKey("Enumeration", UtilMisc.toMap("enumId", autoPrefEnumId));
                    jobName = enumeration.getString("description");
                    if (jobName == null) {
                        jobName = Long.toString((new Date().getTime()));
                    }
                    Map<String, Object> jFields = UtilMisc.<String, Object>toMap("jobName", jobName, "runTime", UtilDateTime.nowTimestamp(),
                        "serviceName", serviceName, "statusId", "SERVICE_PENDING", "recurrenceInfoId", infoId, "runtimeDataId", runtimeDataId);

                // set the pool ID
                jFields.put("poolId", ServiceConfigUtil.getSendPool());

                // set the loader name
                jFields.put("loaderName", JobManager.dispatcherName);
                // create the value and store
                GenericValue jobV;
                jobV = delegator.makeValue("JobSandbox", jFields);
                GenericValue jobSandbox = delegator.createSetNextSeqId(jobV);
                
                ebayProductPref.set("autoPrefJobId", jobSandbox.getString("jobId"));
                ebayProductPref.store();
                
                Map<String, Object>infoData = FastMap.newInstance();
                infoData.put("jobId", jobSandbox.getString("jobId"));
                infoData.put("productStoreId", ebayProductPref.getString("productStoreId"));
                runtimeData.set("runtimeInfo", XmlSerializer.serialize(infoData));
                runtimeData.store();
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (SerializeException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (IOException e) {
            return ServiceUtil.returnError(e.getMessage());
        }catch (RecurrenceInfoException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> stopEbayAutoPreference(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object>result = FastMap.newInstance();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        String autoPrefEnumId = (String) context.get("autoPrefEnumId");
        try {
            GenericValue ebayProductPref = delegator.findByPrimaryKey("EbayProductStorePref", UtilMisc.toMap("productStoreId", productStoreId, "autoPrefEnumId", autoPrefEnumId));
            String jobId = ebayProductPref.getString("autoPrefJobId");
            List<GenericValue> jobs = delegator.findByAnd("JobSandbox", UtilMisc.toMap("parentJobId", jobId ,"statusId", "SERVICE_PENDING"));

            Map<String, Object>inMap = FastMap.newInstance();
            inMap.put("userLogin", userLogin);
            for (int index = 0; index < jobs.size(); index++) {
                inMap.put("jobId", jobs.get(index).getString("jobId"));
                dispatcher.runSync("cancelScheduledJob", inMap);
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static void mappedPaymentMethods(Map requestParams, String itemPkCateId, Map<String,Object> addItemObject, ItemType item, HashMap attributeMapList) {
        String refName = "itemCateFacade_"+itemPkCateId;
        if (UtilValidate.isNotEmpty(addItemObject) && UtilValidate.isNotEmpty(requestParams)) {
            EbayStoreCategoryFacade cf = (EbayStoreCategoryFacade) addItemObject.get(refName);
            BuyerPaymentMethodCodeType[] paymentMethods = cf.getPaymentMethods();
            if (UtilValidate.isNotEmpty(paymentMethods)) {
                BuyerPaymentMethodCodeType[] tempPayments = new BuyerPaymentMethodCodeType[paymentMethods.length];
                int i = 0;
                for (BuyerPaymentMethodCodeType paymentMethod : paymentMethods) {
                    String pmName = paymentMethod.value();
                    String payPara = (String) requestParams.get("Payments_".concat(pmName));
                    if ("true".equals(payPara)) {
                        tempPayments[i] = paymentMethod;
                        attributeMapList.put(""+pmName, pmName);
                        if ("PayPal".equals(pmName)) {
                            if (UtilValidate.isNotEmpty(requestParams.get("paymentMethodPaypalEmail"))) {
                                item.setPayPalEmailAddress(requestParams.get("paymentMethodPaypalEmail").toString());
                                attributeMapList.put("PaypalEmail", requestParams.get("paymentMethodPaypalEmail").toString());
                            }
                        }
                        i++;
                    }
                }
                item.setPaymentMethods(tempPayments);
            }
        }
    }

    public static void mappedShippingLocations(Map requestParams, ItemType item, ApiContext apiContext, HttpServletRequest request, HashMap attributeMapList) {
        try {
            if (UtilValidate.isNotEmpty(requestParams)) {
                EbayStoreSiteFacade sf = (EbayStoreSiteFacade) EbayEvents.getSiteFacade(apiContext, request);
                Map<SiteCodeType, GeteBayDetailsResponseType> eBayDetailsMap = sf.getEBayDetailsMap();
                GeteBayDetailsResponseType eBayDetails = eBayDetailsMap.get(apiContext.getSite());
                ShippingLocationDetailsType[] shippingLocationDetails = eBayDetails.getShippingLocationDetails();
                if (UtilValidate.isNotEmpty(shippingLocationDetails)) {
                    int i = 0;
                    String[] tempShipLocation = new String[shippingLocationDetails.length];
                    for (ShippingLocationDetailsType shippingLocationDetail : shippingLocationDetails) {
                        String shippingLocation = (String) shippingLocationDetail.getShippingLocation();
                        String shipParam = (String)requestParams.get("Shipping_".concat(shippingLocation));
                        if ("true".equals(shipParam)) {
                            tempShipLocation[i] = shippingLocation;
                            attributeMapList.put(""+shippingLocation, shippingLocation);
                            i++;
                        }
                    }
                    item.setShipToLocations(tempShipLocation);
                }
            }
        } catch(Exception e) {
            Debug.logError(e.getMessage(), module);
        }
    }

    public static Map<String, Object> exportProductEachItem(DispatchContext dctx, Map<String, Object> context) {
        Map<String,Object> result = FastMap.newInstance();
        LocalDispatcher dispatcher = (LocalDispatcher) dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> itemObject = (Map<String, Object>) context.get("itemObject");
        String productListingId = itemObject.get("productListingId").toString();
        AddItemCall addItemCall = (AddItemCall) itemObject.get("addItemCall");
        AddItemRequestType req = new AddItemRequestType();
        AddItemResponseType resp = null;
        try {
            GenericValue userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system"));
            ItemType item = addItemCall.getItem();
            req.setItem(item);
            resp = (AddItemResponseType) addItemCall.execute(req);
            if (resp != null && "SUCCESS".equals(resp.getAck().toString()) || "WARNING".equals(resp.getAck().toString())) {
                String itemId = resp.getItemID();
                String listingXml = addItemCall.getRequestXml().toString();
                Map<String, Object> updateItemMap = FastMap.newInstance();
                updateItemMap.put("productListingId", productListingId);
                updateItemMap.put("itemId", itemId);
                updateItemMap.put("listingXml", listingXml);
                updateItemMap.put("statusId", "ITEM_APPROVED");
                updateItemMap.put("userLogin", userLogin);
                try {
                    dispatcher.runSync("updateEbayProductListing", updateItemMap);
                } catch (GenericServiceException ex) {
                    Debug.logError(ex.getMessage(), module);
                    return ServiceUtil.returnError(ex.getMessage());
                }
            }
            result = ServiceUtil.returnSuccess();
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> setEbayProductListingAttribute(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object>result = FastMap.newInstance();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        HashMap attributeMapList = (HashMap) context.get("attributeMapList");
        String productListingId = (String) context.get("productListingId");
        try {
           List<GenericValue> attributeToClears = delegator.findByAnd("EbayProductListingAttribute", UtilMisc.toMap("productListingId", productListingId));
           for (int clearCount = 0; clearCount < attributeToClears.size(); clearCount++) {
              GenericValue valueToClear = attributeToClears.get(clearCount);
              if (valueToClear != null) {
                 valueToClear.remove();
              }
           }
           Set attributeSet = attributeMapList.entrySet();
           Iterator itr = attributeSet.iterator();
           while (itr.hasNext()) {
             Map.Entry attrMap = (Map.Entry) itr.next();

             if (UtilValidate.isNotEmpty(attrMap.getKey())) {
                 GenericValue ebayProductListingAttribute = delegator.makeValue("EbayProductListingAttribute");
                  ebayProductListingAttribute.set("productListingId", productListingId);
                  ebayProductListingAttribute.set("attrName", attrMap.getKey().toString());
                  ebayProductListingAttribute.set("attrValue", attrMap.getValue().toString());
                  ebayProductListingAttribute.create();
              }
           }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }


}
