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

package org.apache.ofbiz.ebaystore;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.base.config.GenericConfigException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.ebay.EbayHelper;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.serialize.SerializeException;
import org.apache.ofbiz.entity.serialize.XmlSerializer;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.calendar.RecurrenceInfo;
import org.apache.ofbiz.service.calendar.RecurrenceInfoException;
import org.apache.ofbiz.service.config.ServiceConfigUtil;

import com.ebay.sdk.ApiAccount;
import com.ebay.sdk.ApiContext;
import com.ebay.sdk.ApiCredential;
import com.ebay.sdk.ApiLogging;
import com.ebay.sdk.call.AddItemCall;
import com.ebay.sdk.call.AddOrderCall;
import com.ebay.sdk.call.GetOrdersCall;
import com.ebay.soap.eBLBaseComponents.AddItemRequestType;
import com.ebay.soap.eBLBaseComponents.AddItemResponseType;
import com.ebay.soap.eBLBaseComponents.AddOrderRequestType;
import com.ebay.soap.eBLBaseComponents.AddOrderResponseType;
import com.ebay.soap.eBLBaseComponents.AmountType;
import com.ebay.soap.eBLBaseComponents.BuyerPaymentMethodCodeType;
import com.ebay.soap.eBLBaseComponents.CategoryType;
import com.ebay.soap.eBLBaseComponents.CountryCodeType;
import com.ebay.soap.eBLBaseComponents.CurrencyCodeType;
import com.ebay.soap.eBLBaseComponents.GetOrdersRequestType;
import com.ebay.soap.eBLBaseComponents.GetOrdersResponseType;
import com.ebay.soap.eBLBaseComponents.GeteBayDetailsResponseType;
import com.ebay.soap.eBLBaseComponents.ItemType;
import com.ebay.soap.eBLBaseComponents.ListingDesignerType;
import com.ebay.soap.eBLBaseComponents.ListingTypeCodeType;
import com.ebay.soap.eBLBaseComponents.OrderArrayType;
import com.ebay.soap.eBLBaseComponents.OrderIDArrayType;
import com.ebay.soap.eBLBaseComponents.OrderStatusCodeType;
import com.ebay.soap.eBLBaseComponents.OrderType;
import com.ebay.soap.eBLBaseComponents.PictureDetailsType;
import com.ebay.soap.eBLBaseComponents.ReturnPolicyType;
import com.ebay.soap.eBLBaseComponents.ShipmentTrackingDetailsType;
import com.ebay.soap.eBLBaseComponents.ShippingDetailsType;
import com.ebay.soap.eBLBaseComponents.ShippingLocationDetailsType;
import com.ebay.soap.eBLBaseComponents.ShippingServiceOptionsType;
import com.ebay.soap.eBLBaseComponents.ShippingTypeCodeType;
import com.ebay.soap.eBLBaseComponents.SiteCodeType;
import com.ebay.soap.eBLBaseComponents.TradingRoleCodeType;
import com.ebay.soap.eBLBaseComponents.VATDetailsType;
import com.ibm.icu.text.SimpleDateFormat;

public class EbayStoreHelper {
    private static final String module = EbayStoreHelper.class.getName();
    public static final String resource = "EbayStoreUiLabels";

    public static ApiContext getApiContext(String productStoreId,Locale locale, Delegator delegator) {
       Map<String, Object> context = new HashMap<String, Object>();
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
        Map<String, Object> context = new HashMap<String, Object>();
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
            partyRole = EntityQuery.use(delegator).from("PartyRole").where("partyId", partyId, "roleTypeId", "EBAY_ACCOUNT").queryOne();
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
            productCategoryRoles = EntityQuery.use(delegator).from("ProductCategoryRole").where("productCategoryId", productCategoryId, "partyId", partyId, "roleTypeId", "EBAY_ACCOUNT").queryList();
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
                    List<GenericValue> productCategoryRollupList = EntityQuery.use(delegator).from("ProductCategoryRollup").where("parentProductCategoryId",catalogCategory.getString("productCategoryId")).queryList();
                    for (GenericValue productCategoryRollup : productCategoryRollupList) {
                        if (productCategoryRollup.containsValue(productCategoryId)) {
                            flag = true;
                            break;
                        } else {
                            // check from level 2
                            List<GenericValue> prodCategoryRollupList = EntityQuery.use(delegator).from("ProductCategoryRollup").where("parentProductCategoryId",productCategoryRollup.getString("productCategoryId")).queryList();
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
        Map<String, Object>result = new HashMap<String, Object>();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String productStoreId = (String) context.get("productStoreId");
        String autoPrefEnumId = (String) context.get("autoPrefEnumId");
        String serviceName = (String) context.get("serviceName");
        try {
            GenericValue ebayProductPref = EntityQuery.use(delegator).from("EbayProductStorePref").where("productStoreId", productStoreId, "autoPrefEnumId", autoPrefEnumId).queryOne();
            String jobId = ebayProductPref.getString("autoPrefJobId");
            if (UtilValidate.isNotEmpty(jobId)) {
                List<GenericValue> jobs = EntityQuery.use(delegator).from("JobSandbox").where("parentJobId", jobId, "statusId", "SERVICE_PENDING").queryList();
                if (jobs.size() == 0) {
                    Map<String, Object>inMap = new HashMap<String, Object>();
                    inMap.put("jobId", jobId);
                    inMap.put("userLogin", userLogin);
                    dispatcher.runSync("resetScheduledJob", inMap);
                }
            }
            if (UtilValidate.isEmpty(ebayProductPref.getString("autoPrefJobId"))) {
                if (UtilValidate.isEmpty(serviceName)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "EbayStoreAutoPrefJobEmpty", locale));
                }
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
                GenericValue enumeration = EntityQuery.use(delegator).from("Enumeration").where("enumId", autoPrefEnumId).queryOne();
                    jobName = enumeration.getString("description");
                    if (jobName == null) {
                        jobName = Long.toString((new Date().getTime()));
                    }
                    Map<String, Object> jFields = UtilMisc.<String, Object>toMap("jobName", jobName, "runTime", UtilDateTime.nowTimestamp(),
                        "serviceName", serviceName, "statusId", "SERVICE_PENDING", "recurrenceInfoId", infoId, "runtimeDataId", runtimeDataId);

                // set the pool ID
                jFields.put("poolId", ServiceConfigUtil.getServiceEngine().getThreadPool().getSendToPool());

                // set the loader name
                jFields.put("loaderName", delegator.getDelegatorName());
                // create the value and store
                GenericValue jobV;
                jobV = delegator.makeValue("JobSandbox", jFields);
                GenericValue jobSandbox = delegator.createSetNextSeqId(jobV);
                
                ebayProductPref.set("autoPrefJobId", jobSandbox.getString("jobId"));
                ebayProductPref.store();
                
                Map<String, Object>infoData = new HashMap<String, Object>();
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
        } catch (GenericConfigException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> stopEbayAutoPreference(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object>result = new HashMap<String, Object>();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Delegator delegator = dctx.getDelegator();
        String productStoreId = (String) context.get("productStoreId");
        String autoPrefEnumId = (String) context.get("autoPrefEnumId");
        try {
            GenericValue ebayProductPref = EntityQuery.use(delegator).from("EbayProductStorePref").where("productStoreId", productStoreId, "autoPrefEnumId", autoPrefEnumId).queryOne();
            String jobId = ebayProductPref.getString("autoPrefJobId");
            List<GenericValue> jobs = EntityQuery.use(delegator).from("JobSandbox").where("parentJobId", jobId ,"statusId", "SERVICE_PENDING").queryList();

            Map<String, Object>inMap = new HashMap<String, Object>();
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

    public static void mappedPaymentMethods(Map<String,Object> requestParams, String itemPkCateId, Map<String,Object> addItemObject, ItemType item, HashMap<String, Object> attributeMapList) {
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

    public static void mappedShippingLocations(Map<String, Object> requestParams, ItemType item, ApiContext apiContext, HttpServletRequest request, HashMap<String, Object> attributeMapList) {
        try {
            if (UtilValidate.isNotEmpty(requestParams)) {
                EbayStoreSiteFacade sf = EbayEvents.getSiteFacade(apiContext, request);
                Map<SiteCodeType, GeteBayDetailsResponseType> eBayDetailsMap = sf.getEBayDetailsMap();
                GeteBayDetailsResponseType eBayDetails = eBayDetailsMap.get(apiContext.getSite());
                ShippingLocationDetailsType[] shippingLocationDetails = eBayDetails.getShippingLocationDetails();
                if (UtilValidate.isNotEmpty(shippingLocationDetails)) {
                    int i = 0;
                    String[] tempShipLocation = new String[shippingLocationDetails.length];
                    for (ShippingLocationDetailsType shippingLocationDetail : shippingLocationDetails) {
                        String shippingLocation = shippingLocationDetail.getShippingLocation();
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
        Map<String,Object> result = new HashMap<String, Object>();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> itemObject = UtilGenerics.checkMap(context.get("itemObject"));
        String productListingId = itemObject.get("productListingId").toString();
        AddItemCall addItemCall = (AddItemCall) itemObject.get("addItemCall");
        AddItemRequestType req = new AddItemRequestType();
        AddItemResponseType resp = null;
        try {
            GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
            ItemType item = addItemCall.getItem();
            req.setItem(item);
            resp = (AddItemResponseType) addItemCall.execute(req);
            if (resp != null && "SUCCESS".equals(resp.getAck().toString()) || "WARNING".equals(resp.getAck().toString())) {
                String itemId = resp.getItemID();
                String listingXml = addItemCall.getRequestXml().toString();
                Map<String, Object> updateItemMap = new HashMap<String, Object>();
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
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> setEbayProductListingAttribute(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> attributeMapList = UtilGenerics.cast(context.get("attributeMapList"));
        String productListingId = (String) context.get("productListingId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Map<String, Object> ebayProductListingAttributeMap = new HashMap<String, Object>();
        try {
           List<GenericValue> attributeToClears = EntityQuery.use(delegator).from("EbayProductListingAttribute").where("productListingId", productListingId).queryList();
           for (int clearCount = 0; clearCount < attributeToClears.size(); clearCount++) {
              GenericValue valueToClear = attributeToClears.get(clearCount);
              if (valueToClear != null) {
                 valueToClear.remove();
              }
           }
           for (Map.Entry<String,Object> entry : attributeMapList.entrySet()) {
              if (UtilValidate.isNotEmpty(entry.getKey())) {
            	   ebayProductListingAttributeMap.put("productListingId", productListingId);
            	   ebayProductListingAttributeMap.put("attrName", entry.getKey().toString());
            	   ebayProductListingAttributeMap.put("attrValue", entry.getValue().toString());
            	   ebayProductListingAttributeMap.put("userLogin", userLogin);
            	   dispatcher.runSync("createEbayProductListingAttribute", ebayProductListingAttributeMap);
              }
           }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    public static ItemType prepareAddItem(Delegator delegator, GenericValue attribute) {
        ItemType item = new ItemType();
        try {
            List<GenericValue> attrs = EntityQuery.use(delegator).from("EbayProductListingAttribute").where("productListingId", attribute.getString("productListingId")).queryList();
            AmountType amount = new AmountType();
            AmountType shippingServiceCost = new AmountType();
            PictureDetailsType picture = new PictureDetailsType();
            CategoryType category = new CategoryType();
            ListingDesignerType designer = new ListingDesignerType();
            ShippingDetailsType shippingDetail = new ShippingDetailsType();
            ShippingServiceOptionsType shippingOption = new ShippingServiceOptionsType();
            for (int index = 0; index < attrs.size(); index++) {
                if ("Title".equals(attrs.get(index).getString("attrName"))) {
                    item.setTitle(attrs.get(index).getString("attrValue"));
                } else if ("SKU".equals(attrs.get(index).getString("attrName"))) {
                    item.setSKU(attrs.get(index).getString("attrValue"));
                } else if ("Currency".equals(attrs.get(index).getString("attrName"))) {
                    amount.setCurrencyID(CurrencyCodeType.valueOf(attrs.get(index).getString("attrValue")));
                } else if ("Description".equals(attrs.get(index).getString("attrName"))) {
                    item.setDescription(attrs.get(index).getString("attrValue"));
                } else if ("ApplicationData".equals(attrs.get(index).getString("attrName"))) {
                    item.setApplicationData(attrs.get(index).getString("attrValue"));
                } else if ("Country".equals(attrs.get(index).getString("attrName"))) {
                    item.setCountry(CountryCodeType.valueOf(attrs.get(index).getString("attrValue")));
                } else if ("PictureURL".equals(attrs.get(index).getString("attrName"))) {
                    String[] pictureUrl = {attrs.get(index).getString("attrValue")};
                    picture.setPictureURL(pictureUrl);
                } else if ("Site".equals(attrs.get(index).getString("attrName"))) {
                    item.setSite(SiteCodeType.valueOf(attrs.get(index).getString("attrValue")));
                } else if ("UseTaxTable".equals(attrs.get(index).getString("attrName"))) {
                    item.setUseTaxTable(Boolean.valueOf(attrs.get(index).getString("attrValue")));
                } else if ("BestOfferEnabled".equals(attrs.get(index).getString("attrName"))) {
                    item.setBestOfferEnabled(Boolean.valueOf(attrs.get(index).getString("attrValue")));
                } else if ("AutoPayEnabled".equals(attrs.get(index).getString("attrName"))) {
                    item.setAutoPay(Boolean.valueOf(attrs.get(index).getString("attrValue")));
                } else if ("CategoryID".equals(attrs.get(index).getString("attrName"))) {
                    category.setCategoryID(attrs.get(index).getString("attrValue"));
                } else if ("CategoryLevel".equals(attrs.get(index).getString("attrName"))) {
                    category.setCategoryLevel(Integer.parseInt(attrs.get(index).getString("attrValue")));
                } else if ("CategoryName".equals(attrs.get(index).getString("attrName"))) {
                    category.setCategoryName(attrs.get(index).getString("attrValue"));
                } else if ("CategoryParentID".equals(attrs.get(index).getString("attrName"))) {
                    String[] parent = {attrs.get(index).getString("attrValue")};
                    category.setCategoryParentID(parent );
                } else if ("LeafCategory".equals(attrs.get(index).getString("attrName"))) {
                    category.setLeafCategory(Boolean.valueOf(attrs.get(index).getString("attrValue")));
                } else if ("LSD".equals(attrs.get(index).getString("attrName"))) {
                    category.setLSD(Boolean.valueOf(attrs.get(index).getString("attrValue")));
                } else if ("ReturnsAcceptedOption".equals(attrs.get(index).getString("attrName"))) {
                    ReturnPolicyType policy = new ReturnPolicyType();
                    policy.setReturnsAcceptedOption(attrs.get(index).getString("attrValue"));
                    item.setReturnPolicy(policy);
                } else if ("LayoutID".equals(attrs.get(index).getString("attrName"))) {
                    designer.setLayoutID(Integer.parseInt(attrs.get(index).getString("attrValue")));
                } else if ("ThemeID".equals(attrs.get(index).getString("attrName"))) {
                    designer.setThemeID(Integer.parseInt(attrs.get(index).getString("attrValue")));
                } else if ("BuyItNowPrice".equals(attrs.get(index).getString("attrName"))) {
                    amount = new AmountType();
                    amount.setValue(Double.parseDouble(attrs.get(index).getString("attrValue")));
                    item.setBuyItNowPrice(amount);
                } else if ("ReservePrice".equals(attrs.get(index).getString("attrName"))) {
                    amount = new AmountType();
                    amount.setValue(Double.parseDouble(attrs.get(index).getString("attrValue")));
                    item.setReservePrice(amount);
                } else if ("ListingType".equals(attrs.get(index).getString("attrName"))) {
                    item.setListingType(ListingTypeCodeType.valueOf(attrs.get(index).getString("attrValue")));
                } else if ("StartPrice".equals(attrs.get(index).getString("attrName"))) {
                    amount = new AmountType();
                    amount.setValue(Double.parseDouble(attrs.get(index).getString("attrValue")));
                    item.setStartPrice(amount);
                } else if ("ShippingService".equals(attrs.get(index).getString("attrName"))) {
                    shippingOption.setShippingService(attrs.get(index).getString("attrValue"));
                } else if ("ShippingServiceCost".equals(attrs.get(index).getString("attrName"))) {
                    shippingServiceCost.setValue(Double.parseDouble(attrs.get(index).getString("attrValue")));
                    shippingOption.setShippingServiceCost(shippingServiceCost);
                } else if ("ShippingServiceCostCurrency".equals(attrs.get(index).getString("attrName"))) {
                    shippingServiceCost.setCurrencyID(CurrencyCodeType.valueOf(attrs.get(index).getString("attrValue")));
                    shippingOption.setShippingServiceCost(shippingServiceCost);
                } else if ("ShippingServicePriority".equals(attrs.get(index).getString("attrName"))) {
                    shippingOption.setShippingServicePriority(Integer.parseInt(attrs.get(index).getString("attrValue")));
                } else if ("ShippingType".equals(attrs.get(index).getString("attrName"))) {
                    shippingDetail.setShippingType(ShippingTypeCodeType.valueOf(attrs.get(index).getString("attrValue")));
                } else if ("VATPercent".equals(attrs.get(index).getString("attrName"))) {
                    VATDetailsType vat = new VATDetailsType();
                    vat.setVATPercent(new Float(attrs.get(index).getString("attrValue")));
                    item.setVATDetails(vat);
                } else if ("Location".equals(attrs.get(index).getString("attrName"))) {
                    item.setLocation(attrs.get(index).getString("attrValue"));
                } else if ("Quantity".equals(attrs.get(index).getString("attrName"))) {
                    item.setQuantity(Integer.parseInt(attrs.get(index).getString("attrValue")));
                } else if ("ListingDuration".equals(attrs.get(index).getString("attrName"))) {
                    item.setListingDuration(attrs.get(index).getString("attrValue"));
                } else if ("LotSize".equals(attrs.get(index).getString("attrName"))) {
                    item.setLotSize(Integer.parseInt(attrs.get(index).getString("attrValue")));
                } else if ("PostalCode".equals(attrs.get(index).getString("attrName"))) {
                    item.setPostalCode(attrs.get(index).getString("attrValue"));
                } else if ("Title".equals(attrs.get(index).getString("attrName"))) {
                    item.setTitle(attrs.get(index).getString("attrValue"));
                }
                if (category != null) {
                    item.setPrimaryCategory(category);
                }
                if (shippingOption != null) {
                    ShippingServiceOptionsType[] options = {shippingOption};
                    shippingDetail.setShippingServiceOptions(options);
                }
                if (shippingDetail != null) {
                    item.setShippingDetails(shippingDetail);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), module);
            return null;
        }
        return item;
    }

    public static Map<String, Object> uploadTrackingInfoBackToEbay(DispatchContext dctx, Map<String, Object> context) {
    Delegator delegator = dctx.getDelegator();
    Locale locale = (Locale) context.get("locale");
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String productStoreId = (String) context.get("productStoreId");
    String orderId = (String) context.get("orderId");
    GetOrdersRequestType req = new GetOrdersRequestType();
    GetOrdersResponseType resp = null;
    try {
        GenericValue orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
        if (orderHeader != null) {
            String externalId = orderHeader.getString("externalId").toString();
            List<GenericValue> orderShipment = orderHeader.getRelated("OrderShipment", null, null, false);
            if (orderShipment.size() > 0) {
                List<GenericValue> trackingOrders = orderHeader.getRelated("TrackingCodeOrder", null, null, false);
                ApiContext apiContext = EbayStoreHelper.getApiContext(productStoreId, locale, delegator);
                GetOrdersCall ordersCall = new GetOrdersCall(apiContext);
                OrderIDArrayType orderIdArr = new OrderIDArrayType();
                String[] orderIdStr = {""+externalId};
                orderIdArr.setOrderID(orderIdStr);
                req.setOrderIDArray(orderIdArr);
                Calendar orderFrom = Calendar.getInstance();
                orderFrom.setTime(UtilDateTime.toDate("01/01/2001 00:00:00"));
                req.setCreateTimeFrom(orderFrom);
                Calendar orderTo = Calendar.getInstance();
                orderTo.setTime(UtilDateTime.nowDate());
                req.setCreateTimeTo(orderTo);
                req.setOrderStatus(OrderStatusCodeType.SHIPPED);
                req.setOrderRole(TradingRoleCodeType.SELLER);
                resp = (GetOrdersResponseType) ordersCall.execute(req);
                if (resp != null && "SUCCESS".equals(resp.getAck().toString())) {
                    OrderArrayType orderArr = resp.getOrderArray();
                    OrderType[] orderTypeList = orderArr.getOrder();
                    for (OrderType order : orderTypeList) {
                        String orderID = order.getOrderID();
                        if (orderID.equals(externalId)) {
                            AddOrderCall addOrderCall = new AddOrderCall(apiContext);
                            AddOrderRequestType addReq = new AddOrderRequestType();
                            AddOrderResponseType addResp = null;
                            OrderType newOrder = new OrderType();
                            ShippingDetailsType shippingDetail = order.getShippingDetails();
                            if (trackingOrders.size() > 0) {
                                ShipmentTrackingDetailsType[] trackDetails = new ShipmentTrackingDetailsType[trackingOrders.size()];
                                for (int i = 0; i < trackDetails.length; i++) {
                                    ShipmentTrackingDetailsType track = new ShipmentTrackingDetailsType();
                                    track.setShipmentTrackingNumber(trackingOrders.get(i).get("trackingCodeId").toString());
                                    trackDetails[i] = track;
                                }
                                shippingDetail.setShipmentTrackingDetails(trackDetails);
                                newOrder.setShippingDetails(shippingDetail);
                            }
                            newOrder.setOrderID(order.getOrderID());
                            newOrder.setOrderStatus(order.getOrderStatus());
                            newOrder.setAdjustmentAmount(order.getAdjustmentAmount());
                            newOrder.setAmountSaved(order.getAmountSaved());
                            newOrder.setCheckoutStatus(order.getCheckoutStatus());
                            newOrder.setShippingDetails(order.getShippingDetails());
                            newOrder.setCreatingUserRole(order.getCreatingUserRole());
                            newOrder.setCreatedTime(order.getCreatedTime());
                            newOrder.setPaymentMethods(order.getPaymentMethods());
                            newOrder.setShippingAddress(order.getShippingAddress());
                            newOrder.setSubtotal(order.getSubtotal());
                            newOrder.setTotal(order.getTotal());
                            newOrder.setTransactionArray(order.getTransactionArray());
                            newOrder.setBuyerUserID(order.getBuyerUserID());
                            newOrder.setPaidTime(order.getPaidTime());
                            newOrder.setShippedTime(order.getShippedTime());
                            newOrder.setIntegratedMerchantCreditCardEnabled(order.isIntegratedMerchantCreditCardEnabled());
                            addReq.setOrder(newOrder);
                            addResp = (AddOrderResponseType) addOrderCall.execute(addReq);
                            if (addResp != null && "SUCCESS".equals(addResp.getAck().toString())) {
                                Debug.logInfo("Upload tracking code to eBay success...", module);
                            } else {
                                createErrorLogMessage(userLogin, dctx.getDispatcher(), productStoreId, addResp.getAck().toString(), "Update order : uploadTrackingInfoBackToEbay", addResp.getErrors(0).getLongMessage());
                            }
                        }
                    }
                } else {
                    createErrorLogMessage(userLogin, dctx.getDispatcher(), productStoreId, resp.getAck().toString(), "Get order : uploadTrackingInfoBackToEbay", resp.getErrors(0).getLongMessage());
                }
            }
        }
    } catch (GenericEntityException gee) {
        return ServiceUtil.returnError(gee.getMessage());
    } catch (Exception e) {
        return ServiceUtil.returnError(e.getMessage());
    }
    return ServiceUtil.returnSuccess();
    }

    public static void createErrorLogMessage(GenericValue userLogin, LocalDispatcher dispatcher, String productStoreId, String ack, String fuction, String errorMessage) {
        if (!"".equals(productStoreId) && (!"".equals(errorMessage))) {
            try {
                Map<String, Object> newMap = new HashMap<String, Object>();
                newMap.put("productStoreId", productStoreId);
                newMap.put("logAck", ack.toLowerCase());
                newMap.put("functionName", fuction);
                newMap.put("logMessage", errorMessage);
                newMap.put("createDatetime", UtilDateTime.nowTimestamp());
                newMap.put("userLogin", userLogin);
                dispatcher.runSync("insertErrorMessagesFromEbay", newMap);
            } catch (Exception ex) {
                Debug.logError("Error from create error log messages : "+ex.getMessage(), module);
            }
        }
    }

    public static boolean isReserveInventory(Delegator delegator, String productId, String productStoreId) {
        boolean isReserve = false;
        try {
            GenericValue ebayProductStore = EntityQuery.use(delegator).from("EbayProductStoreInventory").where("productStoreId", productStoreId, "productId", productId).filterByDate().queryFirst();
            if (ebayProductStore != null) {
                BigDecimal atp = ebayProductStore.getBigDecimal("availableToPromiseListing");
                int intAtp = atp.intValue();
                if (intAtp > 0) {
                    isReserve = true;
                }
            }
        } catch (Exception ex) {
            Debug.logError("Error from get eBay Inventory data : "+ ex.getMessage(), module);
        }
        return isReserve;
    }

    public static String convertDate(Date date, Locale locale) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",locale);
        return simpleDateFormat.format(date);
    }
}
