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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilGenerics;

import com.ebay.sdk.ApiContext;
import com.ebay.sdk.ApiException;
import com.ebay.sdk.SdkException;
import com.ebay.sdk.SdkSoapException;
import com.ebay.sdk.attributes.model.AttributeSet;
import com.ebay.sdk.attributes.model.IAttributesMaster;
import com.ebay.sdk.call.GetCategorySpecificsCall;
import com.ebay.sdk.call.GetDescriptionTemplatesCall;
import com.ebay.soap.eBLBaseComponents.BestOfferEnabledDefinitionType;
import com.ebay.soap.eBLBaseComponents.BuyerPaymentMethodCodeType;
import com.ebay.soap.eBLBaseComponents.CategoryFeatureType;
import com.ebay.soap.eBLBaseComponents.CategoryType;
import com.ebay.soap.eBLBaseComponents.DescriptionTemplateType;
import com.ebay.soap.eBLBaseComponents.DetailLevelCodeType;
import com.ebay.soap.eBLBaseComponents.FeatureDefinitionsType;
import com.ebay.soap.eBLBaseComponents.GetDescriptionTemplatesRequestType;
import com.ebay.soap.eBLBaseComponents.GetDescriptionTemplatesResponseType;
import com.ebay.soap.eBLBaseComponents.ItemSpecificsEnabledCodeType;
import com.ebay.soap.eBLBaseComponents.ListingDurationDefinitionType;
import com.ebay.soap.eBLBaseComponents.ListingDurationDefinitionsType;
import com.ebay.soap.eBLBaseComponents.ListingDurationReferenceType;
import com.ebay.soap.eBLBaseComponents.NameRecommendationType;
import com.ebay.soap.eBLBaseComponents.RecommendationsType;
import com.ebay.soap.eBLBaseComponents.SiteDefaultsType;
import com.ebay.soap.eBLBaseComponents.StoreOwnerExtendedListingDurationsType;
import com.ebay.soap.eBLBaseComponents.ThemeGroupType;

public class EbayStoreCategoryFacade {
    public static final String module = EbayStoreCategoryFacade.class.getName();
    private ApiContext apiContext = null;
    private String catId = null;
    private IAttributesMaster attrMaster = null;
    private EbayStoreSiteFacade siteFacade = null;

    private AttributeSet[] joinedAttrSets = null;
    private ItemSpecificsEnabledCodeType itemSpecificEnabled = null;
    private Boolean retPolicyEnabled = null;
    private Map<Integer,String[]> listingDurationMap = null;
    private Map<String,Integer> listingDurationReferenceMap = null;
    private BuyerPaymentMethodCodeType[] paymentMethods = null;
    private NameRecommendationType[] nameRecommendationTypes = null;
    private StoreOwnerExtendedListingDurationsType storeOwnerExtendedListingDuration = null;
    private BestOfferEnabledDefinitionType bestOfferEnabled = null;
    private List<Map<String,Object>> adItemTemplates = null;

    public EbayStoreCategoryFacade(String catId, ApiContext apiContext, IAttributesMaster attrMaster, EbayStoreSiteFacade siteFacade) throws SdkException, Exception {
        this.catId = catId;
        this.apiContext = apiContext;
        this.attrMaster = attrMaster;
        this.siteFacade = siteFacade;
        this.syncCategoryMetaData();
    }

    private void syncCategoryMetaData() throws SdkException, Exception {
        syncJoinedAttrSets();
        syncCategoryFeatures();
        syncNameRecommendationTypes();
        syncAdItemTemplates();
    }

    private void syncJoinedAttrSets() throws SdkException, Exception {
        int[] ids = new int[1];
        ids[0] = Integer.parseInt(this.getCatId());
        AttributeSet[] itemSpecAttrSets = attrMaster.getItemSpecificAttributeSetsForCategories(ids);
        AttributeSet[] siteWideAttrSets = attrMaster.getSiteWideAttributeSetsForCategories(ids);
        AttributeSet[] joinedAttrSets = attrMaster.joinItemSpecificAndSiteWideAttributeSets(itemSpecAttrSets, siteWideAttrSets);
        this.joinedAttrSets = joinedAttrSets;
    }

    private void syncNameRecommendationTypes() throws ApiException, SdkException, Exception {
        GetCategorySpecificsCall getCatSpe = new GetCategorySpecificsCall(apiContext);
        getCatSpe.setCategoryID(new String[]{this.catId});
        DetailLevelCodeType[] detailLevels = new DetailLevelCodeType[] {DetailLevelCodeType.RETURN_ALL};
        getCatSpe.setDetailLevel(detailLevels);
        RecommendationsType[] recommendationsArray = getCatSpe.getCategorySpecifics();
        if (recommendationsArray == null || recommendationsArray.length == 0)
            return;
        RecommendationsType recommendations = recommendationsArray[0];
        this.nameRecommendationTypes = recommendations.getNameRecommendation();
    }

    public void syncCategoryFeatures() throws Exception {
        Map<String, CategoryType> categoriesCacheMap = this.siteFacade.getSiteCategoriesMap().get(apiContext.getSite());

        Map<String, CategoryFeatureType> cfsMap = this.siteFacade.getSiteCategoriesFeaturesMap().get(apiContext.getSite());
        SiteDefaultsType siteDefaults = this.siteFacade.getSiteFeatureDefaultMap().get(apiContext.getSite());
        FeatureDefinitionsType featureDefinition = this.siteFacade.getSiteFeatureDefinitionsMap().get(apiContext.getSite());

        //get itemSpecificsEnabled feature
        itemSpecificEnabled = (ItemSpecificsEnabledCodeType)getInheritProperty(catId, "getItemSpecificsEnabled", categoriesCacheMap, cfsMap);
        if (itemSpecificEnabled == null) {
            itemSpecificEnabled = siteDefaults.getItemSpecificsEnabled();
        }
        //get returnPolicyEnabled feature
        retPolicyEnabled = (Boolean)getInheritProperty(catId, "isReturnPolicyEnabled", categoriesCacheMap, cfsMap);
        if (retPolicyEnabled == null) {
            retPolicyEnabled = siteDefaults.isReturnPolicyEnabled();
        }

        //get listing durations
        ListingDurationDefinitionsType listDuration = featureDefinition.getListingDurations();
        ListingDurationDefinitionType[] durationArray = listDuration.getListingDuration();
        listingDurationMap = new HashMap<Integer, String[]>();
        for (int i = 0; i < durationArray.length; i++) {
            listingDurationMap.put(durationArray[i].getDurationSetID(), durationArray[i].getDuration());
        }

        //get listing types
        ListingDurationReferenceType[] listingDuration = (ListingDurationReferenceType[])getInheritProperty(catId, "getListingDuration", categoriesCacheMap, cfsMap);
        if (listingDuration == null || listingDuration.length == 0) {
            listingDuration = siteDefaults.getListingDuration();
        }
        listingDurationReferenceMap = new HashMap<String, Integer>();
        for (int i = 0; i < listingDuration.length; i++) {
            listingDurationReferenceMap.put(listingDuration[i].getType().value(),listingDuration[i].getValue());
        }

        //get payment methods
        paymentMethods = (BuyerPaymentMethodCodeType[])getInheritProperty(catId, "getPaymentMethod", categoriesCacheMap, cfsMap);
        if (paymentMethods == null || paymentMethods.length == 0) {
            paymentMethods = siteDefaults.getPaymentMethod();
        }

        //fix 'invalid enum' issue
        paymentMethods = fiterPaymentMethod(paymentMethods);

        storeOwnerExtendedListingDuration = siteDefaults.getStoreOwnerExtendedListingDurations();

        bestOfferEnabled = featureDefinition.getBestOfferEnabled();
    }

    //remove all 'null' code type
    private static BuyerPaymentMethodCodeType[] fiterPaymentMethod(BuyerPaymentMethodCodeType[] paymentMethods) {
        ArrayList<BuyerPaymentMethodCodeType> al = new ArrayList<BuyerPaymentMethodCodeType>();
        for (BuyerPaymentMethodCodeType pm : paymentMethods) {
            if (pm != null) {
                al.add(pm);
            }
        }
        return al.toArray(new BuyerPaymentMethodCodeType[0]);
    }

    /**
     * recursively check the parent category to find out category feature 
     * @param catId categoryID to be retrieved
     * @param methodName method name to be invoked
     * @param categoriesCacheMap cache of all the categories
     * @param cfsMap category features map
     * @return generic Object
     * @throws Exception
     */
    private Object getInheritProperty(String catId,String methodName,
            Map<String, CategoryType> categoriesCacheMap, Map<String, CategoryFeatureType> cfsMap) throws Exception {
        if (cfsMap.containsKey(catId)) {
            CategoryFeatureType cf = cfsMap.get(catId);
            // invoke the method indicated by methodName
            Object returnValue = invokeMethodByName(cf, methodName);
            if (returnValue != null) {
                return returnValue;
            }
        }

        CategoryType cat = categoriesCacheMap.get(catId);
        //if we reach top level, return null
        if (cat.getCategoryLevel() == 1) {
            return null;
        }

        //check parent category
        return getInheritProperty(cat.getCategoryParentID(0), methodName, categoriesCacheMap, cfsMap);
    }

    /**
     * invoke the method specified by methodName and return the corresponding return value
     * @param cf CategoryFeatureType
     * @param methodName String
     * @return generic object
     */
    private Object invokeMethodByName(CategoryFeatureType cf, String methodName) {
        java.lang.reflect.Method m = null;
        try {
            m = cf.getClass().getMethod(methodName);
            if (m != null) {
                return m.invoke(cf);
            } 
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Map<String,Object>> syncAdItemTemplates() throws ApiException, SdkSoapException, SdkException {
        GetDescriptionTemplatesRequestType req = null;
        GetDescriptionTemplatesResponseType resp = null;
        List<Map<String,Object>> temGroupList = new LinkedList<Map<String,Object>>();

        GetDescriptionTemplatesCall call = new GetDescriptionTemplatesCall(this.apiContext);
        req = new GetDescriptionTemplatesRequestType();
        req.setCategoryID(this.catId);
        resp = (GetDescriptionTemplatesResponseType) call.execute(req);
        if (resp != null && "SUCCESS".equals(resp.getAck().toString())) {
            DescriptionTemplateType[] descriptionTemplateTypeList = resp.getDescriptionTemplate();
            Debug.logInfo("layout of category "+ this.catId +":"+ resp.getLayoutTotal(), module);
            for (DescriptionTemplateType descTemplateType : descriptionTemplateTypeList) {
                List<Map<String,Object>> templateList = null;
                Map<String,Object> templateGroup = null;
                if ("THEME".equals(String.valueOf(descTemplateType.getType()))) {
                    Map<String,Object> template = new HashMap<String, Object>();
                    template.put("TemplateId", String.valueOf(descTemplateType.getID()));
                    template.put("TemplateImageURL", descTemplateType.getImageURL());
                    template.put("TemplateName", descTemplateType.getName());
                    template.put("TemplateType", descTemplateType.getType());

                    // check group template by groupId
                    for (Map<String,Object> temGroup : temGroupList) {
                        if (temGroup.get("TemplateGroupId").equals(descTemplateType.getGroupID().toString())) {
                            templateGroup = temGroup;
                            break;
                        }
                    }
                    if (templateGroup == null) {
                        templateGroup = new HashMap<String, Object>();
                        templateList = new LinkedList<Map<String,Object>>();
                        templateGroup.put("TemplateGroupId", descTemplateType.getGroupID().toString());
                        templateList.add(template);
                        templateGroup.put("Templates", templateList);
                        temGroupList.add(templateGroup);
                    } else {
                        if (templateGroup.get("Templates") != null) {
                            templateList = UtilGenerics.checkList(templateGroup.get("Templates"));
                            templateList.add(template);
                        }
                    }
                } else if ("Layout".equals(String.valueOf(descTemplateType.getType()))) {
                }
            }
            ThemeGroupType[] themes = resp.getThemeGroup();
            if (themes != null && temGroupList != null) {
                for (Map<String,Object> temGroup : temGroupList) {
                    for (ThemeGroupType theme : themes) {
                        if (theme.getGroupID() == Integer.parseInt(temGroup.get("TemplateGroupId").toString())) {
                            if (theme != null) temGroup.put("TemplateGroupName", theme.getGroupName());
                            break;
                        } else {
                            if (theme != null) temGroup.put("TemplateGroupName", "_NA_");
                        }
                    }
                }
            }
        }
        return adItemTemplates = temGroupList; 
    }

    public List<Map<String,Object>> getAdItemTemplates(String temGroupId) {
        List<Map<String,Object>> themes = new LinkedList<Map<String,Object>>();
        for (Map<String,Object> temp : this.adItemTemplates) {
            if (temp.get("TemplateGroupId").equals(temGroupId)) {
                themes = UtilGenerics.checkList(temp.get("Templates"));
                break;
            }
        }
        return themes;
    }

    public String getCatId() {
        return catId;
    }

    public void setCatId(String catId) {
        this.catId = catId;
    }

    public AttributeSet[] getJoinedAttrSets() {
        return joinedAttrSets;
    }

    public ItemSpecificsEnabledCodeType getItemSpecificEnabled() {
        return itemSpecificEnabled;
    }

    public Boolean getRetPolicyEnabled() {
        return retPolicyEnabled;
    }

    public Map<Integer, String[]> getListingDurationMap() {
        return listingDurationMap;
    }

    public Map<String, Integer> getListingDurationReferenceMap() {
        return listingDurationReferenceMap;
    }

    public BuyerPaymentMethodCodeType[] getPaymentMethods() {
        return paymentMethods;
    }

    public NameRecommendationType[] getNameRecommendationTypes() {
        return nameRecommendationTypes;
    }

    public boolean AttributesEnabled() {
        return this.joinedAttrSets != null && this.joinedAttrSets.length > 0;
    }

    public StoreOwnerExtendedListingDurationsType getStoreOwnerExtendedListingDuration() {
        return this.storeOwnerExtendedListingDuration;
    }

    public BestOfferEnabledDefinitionType getbestOfferEnabled() {
        return this.bestOfferEnabled;
    }

    public List<Map<String,Object>> getAdItemTemplates() {
        return this.adItemTemplates;
    }
}
