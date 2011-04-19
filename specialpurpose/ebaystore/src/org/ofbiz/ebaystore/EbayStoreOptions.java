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
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.json.JSONObject;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.webapp.event.EventHandlerException;

import com.ebay.sdk.ApiContext;
import com.ebay.sdk.ApiException;
import com.ebay.sdk.SdkException;
import com.ebay.sdk.SdkSoapException;
import com.ebay.sdk.call.GetStoreOptionsCall;
import com.ebay.soap.eBLBaseComponents.CategoryType;
import com.ebay.soap.eBLBaseComponents.GetStoreOptionsRequestType;
import com.ebay.soap.eBLBaseComponents.GetStoreOptionsResponseType;
import com.ebay.soap.eBLBaseComponents.StoreColorSchemeType;
import com.ebay.soap.eBLBaseComponents.StoreColorType;
import com.ebay.soap.eBLBaseComponents.StoreFontType;
import com.ebay.soap.eBLBaseComponents.StoreThemeArrayType;
import com.ebay.soap.eBLBaseComponents.StoreThemeType;
import com.ebay.soap.eBLBaseComponents.StoreCustomCategoryType;

public class EbayStoreOptions {

    private static final String module = EbayStoreOptions.class.getName();
    
    public static String retrieveThemeColorSchemeByThemeId(HttpServletRequest request, HttpServletResponse response) {
        Locale locale = UtilHttp.getLocale(request);
        HttpSession session = request.getSession(true);
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GetStoreOptionsRequestType req = null;
        GetStoreOptionsResponseType resp  = null;
        StoreThemeArrayType returnedBasicThemeArray = null;

        try {
            Map<String, Object> paramMap = UtilHttp.getCombinedMap(request);
            if (paramMap.get("productStoreId") != null) {
                String themeId = (String)paramMap.get("themeId");

                GetStoreOptionsCall  call = new GetStoreOptionsCall(EbayStoreHelper.getApiContext((String)paramMap.get("productStoreId"), locale, delegator));
                req = new GetStoreOptionsRequestType();

                resp = (GetStoreOptionsResponseType) call.execute(req);
                if (resp != null && "SUCCESS".equals(resp.getAck().toString())) {

                    returnedBasicThemeArray = resp.getBasicThemeArray();
                    StoreThemeType[] storeBasicTheme = returnedBasicThemeArray.getTheme();

                    int i=0;
                    String colorSchemeId = themeId.substring(themeId.indexOf("-")+1);
                    themeId = themeId.substring(0,themeId.indexOf("-"));

                    Map<String,Object> storeColorSchemeMap = FastMap.newInstance();
                    while (i < storeBasicTheme.length) {

                        StoreThemeType storeThemeType = storeBasicTheme[i];
                        if (themeId.equals(storeThemeType.getThemeID().toString())) {
                            StoreColorSchemeType colorSchemeType = storeThemeType.getColorScheme();
                            if (colorSchemeType != null) {
                                if (colorSchemeId.equals(colorSchemeType.getColorSchemeID().toString())) {
                                    StoreColorType storeColor = colorSchemeType.getColor();
                                    storeColorSchemeMap.put("storeColorAccent",storeColor.getAccent());
                                    storeColorSchemeMap.put("storeColorPrimary",storeColor.getPrimary());
                                    storeColorSchemeMap.put("storeColorSecondary",storeColor.getSecondary());

                                    // get font,size and color 
                                    StoreFontType storeFontType = colorSchemeType.getFont();
                                    storeColorSchemeMap.put("storeFontTypeNameFaceColor",storeFontType.getNameColor());
                                    storeColorSchemeMap.put("storeFontTypeFontFaceValue",storeFontType.getNameFace().value());
                                    storeColorSchemeMap.put("storeFontTypeSizeFaceValue",storeFontType.getNameSize().value());

                                    storeColorSchemeMap.put("storeFontTypeTitleColor",storeFontType.getTitleColor());
                                    storeColorSchemeMap.put("storeFontTypeFontTitleValue",storeFontType.getTitleFace().value());
                                    storeColorSchemeMap.put("storeFontSizeTitleValue",storeFontType.getTitleSize().value());

                                    storeColorSchemeMap.put("storeFontTypeDescColor",storeFontType.getDescColor());
                                    storeColorSchemeMap.put("storeFontTypeFontDescValue",storeFontType.getDescFace().value());
                                    storeColorSchemeMap.put("storeDescSizeValue",storeFontType.getDescSize().value());
                                    toJsonObject(storeColorSchemeMap,response);

                                    break;
                                }
                            }
                        }
                        i++;
                    }
                } else {
                    EbayStoreHelper.createErrorLogMessage(userLogin, dispatcher, paramMap.get("productStoreId").toString(), resp.getAck().toString(), "GetStoreOptionsCall : retrieveThemeColorSchemeByThemeId", resp.getErrors(0).getLongMessage());
                }
            }
        } catch (ApiException e) {
            e.printStackTrace();
            return "error";
        } catch (SdkSoapException e) {
            e.printStackTrace();
            return "error";
        } catch (SdkException e) {
            e.printStackTrace();
            return "error";
        } catch (EventHandlerException e) {
            e.printStackTrace();
            return "error";
        }
        return "success";
    }

    public static void toJsonObject(Map<String,Object> attrMap, HttpServletResponse response) throws EventHandlerException {
        JSONObject json = JSONObject.fromObject(attrMap);
        String jsonStr = json.toString();
        if (jsonStr == null) {
            throw new EventHandlerException("JSON Object was empty; fatal error!");
        }
        // set the X-JSON content type
        response.setContentType("application/json");
        // jsonStr.length is not reliable for unicode characters
        try {
            response.setContentLength(jsonStr.getBytes("UTF8").length);
        } catch (UnsupportedEncodingException e) {
            throw new EventHandlerException("Problems with Json encoding", e);
        }
        // return the JSON String
        Writer out;
        try {
            out = response.getWriter();
            out.write(jsonStr);
            out.flush();
        } catch (IOException e) {
            throw new EventHandlerException("Unable to get response writer", e);
        }
    }

    public static String retrieveItemTemplateByTemplateGroupId(HttpServletRequest request,HttpServletResponse response) {
        Map<String, Object> paramMap = UtilHttp.getCombinedMap(request);
        try {
            if (paramMap.get("productStoreId") != null) {
                String temGroupId = (String)paramMap.get("templateGroupId");
                Map<String,Object> addItemObj = EbayEvents.getAddItemListingObject(request, EbayEvents.getApiContext(request));
                if (UtilValidate.isNotEmpty(addItemObj)) {
                    String refName = "itemCateFacade_".concat((String) paramMap.get("pkCategoryId"));
                    if (UtilValidate.isNotEmpty(addItemObj.get(refName))) {
                        EbayStoreCategoryFacade cf = (EbayStoreCategoryFacade) addItemObj.get(refName);
                        List<Map<String,Object>> theme = cf.getAdItemTemplates(temGroupId);
                        if (theme.size() > 0) {
                            toJsonObjectList(theme,response);
                        }
                    }
                }
            }
        } catch (EventHandlerException e) {
            Debug.logError(e.getMessage(), module);
        }
        return "success";
    }

    public static String retrieveEbayCategoryByParent(HttpServletRequest request, HttpServletResponse response) {
        List<CategoryType> results = FastList.newInstance();
        try {
            Map<String, Object> paramMap = UtilHttp.getCombinedMap(request);
            if (paramMap.get("productStoreId") != null) {
                String ebayCategoryId = (String)paramMap.get("ebayCategoryId");
                // when change category should be remove old category from session
                if (ebayCategoryId.indexOf("CH_") != -1) {
                    ebayCategoryId = ebayCategoryId.replace("CH_", "");
                    if (UtilValidate.isNotEmpty(ebayCategoryId)) {
                        ApiContext apiContext = EbayEvents.getApiContext(request);
                        Map<String,Object> addItemObject = EbayEvents.getAddItemListingObject(request, apiContext);
                        String refName = "itemCateFacade_".concat(ebayCategoryId);
                        if (UtilValidate.isNotEmpty(addItemObject.get(refName))) {
                            addItemObject.remove(refName);
                        }
                    }
                    ebayCategoryId = "";
                }
                request.setAttribute("productStoreId", paramMap.get("productStoreId"));
                request.setAttribute("categoryId", ebayCategoryId);
                results = EbayEvents.getChildCategories(request);
                if (UtilValidate.isNotEmpty(results)) {
                    List<Map<String,Object>> categories = FastList.newInstance();
                    for (CategoryType category : results) {
                        Map<String,Object> context = FastMap.newInstance();
                        context.put("CategoryCode", category.getCategoryID());
                        context.put("CategoryName", category.getCategoryName());
                        String isLeaf = String.valueOf(category.isLeafCategory()!= null ? category.isLeafCategory() : "false");
                        context.put("IsLeafCategory", isLeaf);
                        categories.add(context);
                    }
                    if (categories.size() > 0) {
                        toJsonObjectList(categories,response);
                    }
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError(e.getMessage(), module);
        } catch (EventHandlerException e) {
            Debug.logError(e.getMessage(), module);
        } catch (ApiException e) {
            Debug.logError(e.getMessage(), module);
        } catch (SdkException e) {
            Debug.logError(e.getMessage(), module);
        } catch (Exception e) {
            Debug.logError(e.getMessage(), module);
        }
        return "success";
    }

    public static void toJsonObjectList(List<Map<String,Object>> list, HttpServletResponse response) throws EventHandlerException {
        JSONObject json = null;
        List<JSONObject> jsonList = new ArrayList<JSONObject>();
        if (list != null) {
            for (Map<String,Object> val : list) {
                json = new JSONObject();
                for (String rowKey: val.keySet()) {
                    json.put(rowKey, val.get(rowKey));
                }
                jsonList.add(json);
            }
            String jsonStr = jsonList.toString();
            if (jsonStr == null) {
                throw new EventHandlerException("JSON Object was empty; fatal error!");
            }
            // set the X-JSON content type
            response.setContentType("application/json");
            // jsonStr.length is not reliable for unicode characters
            try {
                response.setContentLength(jsonStr.getBytes("UTF8").length);
            } catch (UnsupportedEncodingException e) {
                throw new EventHandlerException("Problems with Json encoding", e);
            }
            // return the JSON String
            Writer out;
            try {
                out = response.getWriter();
                out.write(jsonStr);
                out.flush();
            } catch (IOException e) {
                throw new EventHandlerException("Unable to get response writer", e);
            } 
        }
    }
    
    public static String retrieveEbayStoreCategoryByParent(HttpServletRequest request, HttpServletResponse response) {
        List<StoreCustomCategoryType> results = FastList.newInstance();
        try {
            Map paramMap = UtilHttp.getCombinedMap(request);
            if (paramMap.get("productStoreId") != null) {
                String ebayStoreCategory = (String)paramMap.get("ebayCategoryId");
                // when change category should be remove old category from session
                if (ebayStoreCategory.indexOf("CH_") != -1) {
                    ebayStoreCategory = ebayStoreCategory.replace("CH_", "");
                    if (UtilValidate.isNotEmpty(ebayStoreCategory)) {
                        ApiContext apiContext = EbayEvents.getApiContext(request);
                        Map<String,Object> addItemObject = EbayEvents.getAddItemListingObject(request, apiContext);
                        String refName = "itemCateFacade_".concat(ebayStoreCategory);
                        if (UtilValidate.isNotEmpty(addItemObject.get(refName))) {
                            addItemObject.remove(refName);
                        }
                    }
                    ebayStoreCategory = "";
                }
                request.setAttribute("productStoreId", paramMap.get("productStoreId"));
                request.setAttribute("categoryId", ebayStoreCategory);
                ApiContext apiContext = EbayEvents.getApiContext(request);
                EbayStoreSiteFacade sf = EbayEvents.getSiteFacade(apiContext,request);
                results = EbayEvents.getStoreChildCategories(request);
                if (UtilValidate.isNotEmpty(results)) {
                    List<Map<String,Object>> categories = FastList.newInstance();
                    for (StoreCustomCategoryType category : results) {
                        Map<String,Object> context = FastMap.newInstance();
                        context.put("CategoryCode", category.getCategoryID());
                        context.put("CategoryName", category.getName());
                        String isLeaf = "false";
                        if (category.getChildCategory().length == 0) {
                            isLeaf = "true";
                        } else {
                            isLeaf = "false";
                        }
                        //String isLeaf = String.valueOf(category.getChildCategory().!= null ? "true" : "false");
                        context.put("IsLeafCategory", isLeaf);
                        categories.add(context);
                    }
                    if (categories.size() > 0) {
                        toJsonObjectList(categories,response);
                    }
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError(e.getMessage(), module);
        } catch (EventHandlerException e) {
            Debug.logError(e.getMessage(), module);
        } catch (ApiException e) {
            Debug.logError(e.getMessage(), module);
        } catch (SdkException e) {
            Debug.logError(e.getMessage(), module);
        } catch (Exception e) {
            Debug.logError(e.getMessage(), module);
        }
        return "success";
    }

}
