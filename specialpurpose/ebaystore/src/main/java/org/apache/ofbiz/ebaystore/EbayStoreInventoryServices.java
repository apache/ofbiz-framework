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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import com.ebay.sdk.ApiException;
import com.ebay.sdk.SdkException;
import com.ebay.sdk.SdkSoapException;
import com.ebay.sdk.call.AddSellingManagerInventoryFolderCall;
import com.ebay.sdk.call.AddSellingManagerProductCall;
import com.ebay.sdk.call.GetSellingManagerInventoryCall;
import com.ebay.sdk.call.GetSellingManagerInventoryFolderCall;
import com.ebay.sdk.call.ReviseSellingManagerProductCall;
import com.ebay.soap.eBLBaseComponents.AddSellingManagerInventoryFolderRequestType;
import com.ebay.soap.eBLBaseComponents.AddSellingManagerInventoryFolderResponseType;
import com.ebay.soap.eBLBaseComponents.AddSellingManagerProductRequestType;
import com.ebay.soap.eBLBaseComponents.AddSellingManagerProductResponseType;
import com.ebay.soap.eBLBaseComponents.GetSellingManagerInventoryFolderRequestType;
import com.ebay.soap.eBLBaseComponents.GetSellingManagerInventoryFolderResponseType;
import com.ebay.soap.eBLBaseComponents.GetSellingManagerInventoryRequestType;
import com.ebay.soap.eBLBaseComponents.GetSellingManagerInventoryResponseType;
import com.ebay.soap.eBLBaseComponents.ReviseSellingManagerProductRequestType;
import com.ebay.soap.eBLBaseComponents.ReviseSellingManagerProductResponseType;
import com.ebay.soap.eBLBaseComponents.SellingManagerFolderDetailsType;
import com.ebay.soap.eBLBaseComponents.SellingManagerProductDetailsType;
import com.ebay.soap.eBLBaseComponents.SellingManagerProductInventoryStatusType;
import com.ebay.soap.eBLBaseComponents.SellingManagerProductType;

public class EbayStoreInventoryServices {
    private static final String module = EbayStoreInventoryServices.class.getName();
    public static final String resource = "EbayStoreUiLabels";
    private static final String defaultFolderName = "OFBizProducts";
    private static String folderId = null;
    public EbayStoreInventoryServices() {
        // TODO Auto-generated constructor stub
    }
    /*update inventory on ebay site*/
    public static Map<String,Object> updateEbayStoreInventory(DispatchContext dctx, Map<String,Object> context) {
        Map<String,Object> result = new HashMap<String, Object>();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        GetSellingManagerInventoryRequestType invenReq = null;
        GetSellingManagerInventoryResponseType invenResp = null;
        boolean checkProd = false;
        boolean status = false;
        try {
            if (context.get("productStoreId") == null || context.get("productId") == null || context.get("folderId") == null) {
                result = ServiceUtil.returnError(UtilProperties.getMessage(resource, "EbayStoreInventoryFolderIdRequired", locale));
                result.put("productStoreId", context.get("productStoreId"));
                result.put("facilityId", context.get("facilityId"));
                result.put("folderId", context.get("folderId"));
                return result;
            }

            String productId = (String)context.get("productId");
            String folderId = (String)context.get("folderId");
            // start upload/update products which selected  to an ebay inventory
            if (folderId != null) {
                GetSellingManagerInventoryCall invenCall = new GetSellingManagerInventoryCall(EbayStoreHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));
                invenReq = new GetSellingManagerInventoryRequestType();
                invenResp = (GetSellingManagerInventoryResponseType) invenCall.execute(invenReq);
                if (invenResp != null && "SUCCESS".equals(invenResp.getAck().toString())) {
                    GenericValue ebayProductStoreInventory = EntityQuery.use(delegator).from("EbayProductStoreInventory").where("productId", productId, "facilityId", context.get("facilityId"), "productStoreId", context.get("productStoreId")).queryOne();

                    SellingManagerProductType[]  sellingManagerProductTypeList = invenResp.getSellingManagerProduct();
                    for (SellingManagerProductType sellingManagerProductType : sellingManagerProductTypeList) {
                        SellingManagerProductDetailsType sellingManagerProductDetailsType = sellingManagerProductType.getSellingManagerProductDetails();
                        if (String.valueOf(sellingManagerProductDetailsType.getFolderID()).equals(folderId) && String.valueOf(sellingManagerProductDetailsType.getProductID()).equals(String.valueOf(ebayProductStoreInventory.getLong("ebayProductId"))) && String.valueOf(sellingManagerProductDetailsType.getCustomLabel()).equals(productId)) {
                            checkProd = true;
                            break;
                        }
                    }
                } else {
                    EbayStoreHelper.createErrorLogMessage(userLogin, dctx.getDispatcher(), context.get("productStoreId").toString(), invenResp.getAck().toString(), "GetSellingManagerInventoryCall : updateEbayStoreInventory", invenResp.getErrors(0).getLongMessage());
                }

                // checkProduct is true then update detail  but is false do create new one.
                if (checkProd) {
                    status = updateProductInEbayInventoryFolder(dctx,context);
                } else {
                    status = createNewProductInEbayInventoryFolder(dctx,context);
                }
                if (status) {
                    Debug.logInfo("Done to updated product ".concat(context.get("productId").toString()), module);
                    result = ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "EbayStoreInventoryFolderIdUpdated", UtilMisc.toMap("folderId", context.get("folderId")), locale));
                } else {
                    result = ServiceUtil.returnError(UtilProperties.getMessage(resource, "EbayStoreInventoryFolderIdUpdatedFailed", locale));
                }
            }
        }catch (ApiException e) {
            result = ServiceUtil.returnFailure(e.getMessage());
        } catch (SdkSoapException e) {
            result = ServiceUtil.returnFailure(e.getMessage());
        } catch (SdkException e) {
            result = ServiceUtil.returnFailure(e.getMessage());
        } catch (GenericEntityException e) {
            result = ServiceUtil.returnFailure(e.getMessage());
        }
        result.put("productStoreId", context.get("productStoreId"));
        result.put("facilityId", context.get("facilityId"));
        result.put("folderId", context.get("folderId"));
        return result;
    }

    /* add new product and quantity to ebay inventory */
    public static boolean createNewProductInEbayInventoryFolder(DispatchContext dctx, Map<String,Object> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        AddSellingManagerProductRequestType productReq = null;
        AddSellingManagerProductResponseType productResp = null;
        boolean flag = false;

        try {
            if (context.get("productStoreId") != null && context.get("productId") != null && context.get("folderId") != null) {
                String productId = (String)context.get("productId");
                String folderId = (String)context.get("folderId");
                AddSellingManagerProductCall productCall = new AddSellingManagerProductCall(EbayStoreHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));
                productReq = new AddSellingManagerProductRequestType();
                productReq.setFolderID(new Long(folderId));
                SellingManagerProductDetailsType  sellingManagerProductDetailsType = new SellingManagerProductDetailsType();
                GenericValue ebayProductStoreInventory = EntityQuery.use(delegator).from("EbayProductStoreInventory").where("productId", productId, "facilityId", context.get("facilityId"), "productStoreId", context.get("productStoreId")).queryOne();

                sellingManagerProductDetailsType.setProductName((EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne()).getString("internalName"));
                //Must keep productId in SKU NUMBER because ebay allow productId field only long value.
                sellingManagerProductDetailsType.setCustomLabel(productId);
                if (ebayProductStoreInventory!=null) sellingManagerProductDetailsType.setQuantityAvailable(ebayProductStoreInventory.getBigDecimal("availableToPromiseListing").intValue());

                productReq.setSellingManagerProductDetails(sellingManagerProductDetailsType);
                productResp = (AddSellingManagerProductResponseType) productCall.execute(productReq);
                if (productResp != null && "SUCCESS".equals(productResp.getAck().toString())) {
                    flag = true;
                    ebayProductStoreInventory.put("ebayProductId", productResp.getSellingManagerProductDetails().getProductID());
                    ebayProductStoreInventory.put("folderId", folderId);
                    ebayProductStoreInventory.store();
                } else {
                    EbayStoreHelper.createErrorLogMessage(userLogin, dctx.getDispatcher(), context.get("productStoreId").toString(), productResp.getAck().toString(), "AddSellingManagerProductCall : createNewProductInEbayInventoryFolder", productResp.getErrors(0).getLongMessage());
                    Debug.logError("Fail to  create inventory product ".concat(productId).concat("in productStore ").concat(context.get("productStoreId").toString()).concat(" message from ebay : ").concat(productResp.getMessage()), module);
                }
            }
        } catch (ApiException e) {
            Debug.logError(e.getMessage(), module);
        } catch (SdkSoapException e) {
            Debug.logError(e.getMessage(), module);
        } catch (SdkException e) {
            Debug.logError(e.getMessage(), module);
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), module);
        }
        return flag;
    }

    /* update product and quantity to ebay inventory */
    public static boolean updateProductInEbayInventoryFolder(DispatchContext dctx, Map<String,Object> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        ReviseSellingManagerProductRequestType req = null;
        ReviseSellingManagerProductResponseType resp = null;
        boolean flag = false;

        try {
            if (context.get("productStoreId") != null && context.get("productId") != null && context.get("folderId") != null) {
                String productId = (String)context.get("productId");
                String folderId = (String)context.get("folderId");
                ReviseSellingManagerProductCall call = new ReviseSellingManagerProductCall(EbayStoreHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));
                req = new ReviseSellingManagerProductRequestType();
                SellingManagerProductDetailsType  sellingManagerProductDetailsType = new SellingManagerProductDetailsType();
                GenericValue ebayProductStoreInventory = EntityQuery.use(delegator).from("EbayProductStoreInventory").where("productId", productId, "facilityId", context.get("facilityId"), "productStoreId", context.get("productStoreId")).queryOne();
                Long ebayProductId = null;
                if (ebayProductStoreInventory != null && ebayProductStoreInventory.getLong("ebayProductId") == null) {
                    Debug.logError("Can not update product "+productId+" has no ebay product Id in EbayProductStoreInventory. ", module);
                    return flag;
                }
                if (ebayProductStoreInventory != null && ebayProductStoreInventory.getLong("ebayProductId") != null) {
                    ebayProductId = ebayProductStoreInventory.getLong("ebayProductId");
                }
                sellingManagerProductDetailsType.setProductID(ebayProductId);

                sellingManagerProductDetailsType.setProductName((EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne()).getString("internalName"));
                //Must keep productId in SKU NUMBER because ebay allow productId field only long value.
                sellingManagerProductDetailsType.setCustomLabel(productId);
                if (ebayProductStoreInventory!=null) sellingManagerProductDetailsType.setQuantityAvailable(ebayProductStoreInventory.getBigDecimal("availableToPromiseListing").intValue());

                req.setSellingManagerProductDetails(sellingManagerProductDetailsType);
                resp = (ReviseSellingManagerProductResponseType) call.execute(req);
                if (resp != null && "SUCCESS".equals(resp.getAck().toString())) {
                    flag = true;
                    ebayProductStoreInventory.put("ebayProductId", ebayProductId);
                    ebayProductStoreInventory.put("folderId", folderId);
                    ebayProductStoreInventory.store();
                } else {
                    EbayStoreHelper.createErrorLogMessage(userLogin, dctx.getDispatcher(), context.get("productStoreId").toString(), resp.getAck().toString(), "ReviseSellingManagerProductCall : updateProductInEbayInventoryFolder", resp.getErrors(0).getLongMessage());
                    Debug.logError("Fail to  update inventory product ".concat(productId).concat("in productStore ").concat(context.get("productStoreId").toString()).concat(" message from ebay : ").concat(resp.getMessage()), module);
                }
            }
        } catch (ApiException e) {
            Debug.logError(e.getMessage(), module);
        } catch (SdkSoapException e) {
            Debug.logError(e.getMessage(), module);
        } catch (SdkException e) {
            Debug.logError(e.getMessage(), module);
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), module);
        }
        return flag;
    }

    public static Map<String,Object> getFolderInEbayStoreInventory(DispatchContext dctx, Map<String,Object> context) {
        Map<String,Object> result = new HashMap<String, Object>();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        GetSellingManagerInventoryFolderRequestType req = null;
        GetSellingManagerInventoryFolderResponseType resp = null;
        boolean flag = false;

        try {
            if (context.get("productStoreId") != null) {
                GetSellingManagerInventoryFolderCall  call = new GetSellingManagerInventoryFolderCall(EbayStoreHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));
                req = new GetSellingManagerInventoryFolderRequestType();
                resp = (GetSellingManagerInventoryFolderResponseType) call.execute(req);
                if (resp != null && "SUCCESS".equals(resp.getAck().toString())) {
                    SellingManagerFolderDetailsType sellingManagerFolderDetailsType = resp.getFolder();
                    if (sellingManagerFolderDetailsType != null) {
                        SellingManagerFolderDetailsType[] SellingManagerFolderDetailsTypeList = sellingManagerFolderDetailsType.getChildFolder();
                        for (SellingManagerFolderDetailsType sellingManagerFolderDetails : SellingManagerFolderDetailsTypeList) {
                            Debug.logInfo("ebay inventory folders name ".concat(sellingManagerFolderDetails.getFolderName()), module);
                            if (sellingManagerFolderDetails.getFolderName().equals(defaultFolderName)) {
                                folderId = String.valueOf(sellingManagerFolderDetails.getFolderID());
                                flag = true;
                                break;
                            }
                        }
                    }
                    if (!flag) {
                        folderId = createNewFolderInEbayStoreInventory(dctx,context);
                    }
                } else {
                    EbayStoreHelper.createErrorLogMessage(userLogin, dctx.getDispatcher(), context.get("productStoreId").toString(), resp.getAck().toString(), "GetSellingManagerInventoryFolderCall : getFolderInEbayStoreInventory", resp.getErrors(0).getLongMessage());
                }
                result = ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "EbayStoreInventoryFolderIdLoaded", UtilMisc.toMap("folderId", folderId), locale));
            }
        } catch (ApiException e) {
            result = ServiceUtil.returnFailure(e.getMessage());
        } catch (SdkSoapException e) {
            result = ServiceUtil.returnFailure(e.getMessage());
        } catch (SdkException e) {
            result = ServiceUtil.returnFailure(e.getMessage());
        }
        if (result.get("responseMessage") != null && result.get("responseMessage").equals("fail")) folderId = null;
        result.put("folderId", folderId);
        Debug.logInfo("service return result "+ result, module);
        return result;
    }

    /*create new folder for export product into inventory.*/
    public static String createNewFolderInEbayStoreInventory(DispatchContext dctx, Map<String,Object> context) {
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Delegator delegator = dctx.getDelegator();
        AddSellingManagerInventoryFolderRequestType req = null;
        AddSellingManagerInventoryFolderResponseType resp = null;

        try {
            if (context.get("productStoreId") != null) {
                AddSellingManagerInventoryFolderCall call = new AddSellingManagerInventoryFolderCall(EbayStoreHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));
                req = new AddSellingManagerInventoryFolderRequestType();
                req.setFolderName(defaultFolderName);//req.setComment(value);//req.setParentFolderID(value)
                resp = (AddSellingManagerInventoryFolderResponseType) call.execute(req);
                if (resp != null && "SUCCESS".equals(resp.getAck().toString())) {
                    folderId = String.valueOf(resp.getFolderID());
                } else {
                    EbayStoreHelper.createErrorLogMessage(userLogin, dctx.getDispatcher(), context.get("productStoreId").toString(), resp.getAck().toString(), "AddSellingManagerInventoryFolderCall : createNewFolderInEbayStoreInventory", resp.getErrors(0).getLongMessage());
                    Debug.logError("The problem with create new folder on ebay site.", module);
                    return folderId;
                }
            }
        } catch (ApiException e) {
            Debug.logError(e.getMessage(), module);
        } catch (SdkSoapException e) {
            Debug.logError(e.getMessage(), module);
        } catch (SdkException e) {
            Debug.logError(e.getMessage(), module);
        }
        return folderId;
    }

    /* update inventory status from ebay store inventory */
    public static Map<String,Object> updateEbayInventoryStatusByProductId(DispatchContext dctx, Map<String,Object> context) {
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Map<String,Object> result = new HashMap<String, Object>();
        Delegator delegator = dctx.getDelegator();
        String productStoreId = (String)context.get("productStoreId");
        String facilityId = (String)context.get("facilityId");
        String folderId = (String)context.get("folderId");
        String productId = (String)context.get("productId");
        String ebayProductId = null;
        GetSellingManagerInventoryRequestType req = null;
        GetSellingManagerInventoryResponseType resp = null;
        GenericValue ebayProductStoreInventory = null;

        if (context.get("ebayProductId") != null) {
            ebayProductId = String.valueOf(context.get("ebayProductId"));
        }
        try {
            if (productStoreId != null && ebayProductId != null) {
                ebayProductStoreInventory = EntityQuery.use(delegator).from("EbayProductStoreInventory").where("productId", productId, "facilityId", facilityId, "productStoreId", productStoreId).queryOne();
                GetSellingManagerInventoryCall call = new GetSellingManagerInventoryCall(EbayStoreHelper.getApiContext(productStoreId, locale, delegator));
                req = new GetSellingManagerInventoryRequestType();
                resp = (GetSellingManagerInventoryResponseType) call.execute(req);
                if (resp != null && "SUCCESS".equals(resp.getAck().toString())) {
                    SellingManagerProductType[] sellingManagerProductTypeList = resp.getSellingManagerProduct();
                    for (SellingManagerProductType sellingManagerProductType : sellingManagerProductTypeList) {
                        SellingManagerProductDetailsType productDetail = sellingManagerProductType.getSellingManagerProductDetails();
                        if (String.valueOf(productDetail.getFolderID()).equals(folderId) && String.valueOf(productDetail.getProductID()).equals(ebayProductId) && String.valueOf(productDetail.getCustomLabel()).equals(productId)) {
                            SellingManagerProductInventoryStatusType prodInventoryStatus = sellingManagerProductType.getSellingManagerProductInventoryStatus();
                            ebayProductStoreInventory.put("activeListing",new BigDecimal(prodInventoryStatus.getQuantityActive()));
                            ebayProductStoreInventory.put("scheduled",new BigDecimal(prodInventoryStatus.getQuantityScheduled()));
                            ebayProductStoreInventory.put("sold",new BigDecimal(prodInventoryStatus.getQuantitySold()));
                            ebayProductStoreInventory.put("unSold",new BigDecimal(prodInventoryStatus.getQuantityUnsold()));
                            ebayProductStoreInventory.store();
                            result = ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "EbayStoreInventoryStatusUpdated", UtilMisc.toMap("productId", productId), locale));
                            break;
                        }
                    }
                } else {
                    EbayStoreHelper.createErrorLogMessage(userLogin, dctx.getDispatcher(), context.get("productStoreId").toString(), resp.getAck().toString(), "GetSellingManagerInventoryCall : updateEbayInventoryStatusByProductId", resp.getErrors(0).getLongMessage());
                    Debug.logError("The problem with get manage inventory detail from ebay site.", module);
                }
            }
        } catch (ApiException e) {
            result = ServiceUtil.returnFailure(e.getMessage());
        } catch (SdkSoapException e) {
            result = ServiceUtil.returnFailure(e.getMessage());
        } catch (SdkException e) {
            result = ServiceUtil.returnFailure(e.getMessage());
        } catch (GenericEntityException e) {
            result = ServiceUtil.returnFailure(e.getMessage());
        }
        result.put("productStoreId", context.get("productStoreId"));
        result.put("facilityId", context.get("facilityId"));
        result.put("folderId", context.get("folderId"));
        result.put("productId", productId);
        return result;
    }

    public static Map<String,Object> updateEbayInventoryStatus(DispatchContext dctx, Map<String,Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String,Object> result = new HashMap<String, Object>();
        Delegator delegator = dctx.getDelegator();
        List<GenericValue> ebayProductStoreInventoryList = null;

        try {
            if (context.get("productStoreId") != null && context.get("facilityId") != null) {
                ebayProductStoreInventoryList = EntityQuery.use(delegator).from("EbayProductStoreInventory").where("facilityId",(String)context.get("facilityId"),"productStoreId",(String)context.get("productStoreId")).queryList();
                for (GenericValue ebayProductStoreInventory : ebayProductStoreInventoryList) {
                    if (ebayProductStoreInventory.get("ebayProductId") != null) {
                        dispatcher.runSync("updateEbayInventoryStatusByProductId", UtilMisc.toMap("productStoreId", (String)context.get("productStoreId"), "facilityId", (String)context.get("facilityId"), "folderId", ebayProductStoreInventory.get("folderId"), "productId", ebayProductStoreInventory.get("productId"), "ebayProductId", ebayProductStoreInventory.get("ebayProductId"), "userLogin", context.get("userLogin")));
                    }
                }
            }
        } catch (GenericEntityException e) {
            result = ServiceUtil.returnFailure(e.getMessage());
        } catch (GenericServiceException e) {
            result = ServiceUtil.returnFailure(e.getMessage());
        }
        result = ServiceUtil.returnSuccess();
        return result;
    }
}
