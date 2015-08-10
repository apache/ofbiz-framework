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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ebay.sdk.ApiContext;
import com.ebay.sdk.ApiException;
import com.ebay.sdk.SdkException;
import com.ebay.sdk.attributes.AttributesMaster;
import com.ebay.sdk.attributes.AttributesXmlDownloader;
import com.ebay.sdk.attributes.AttributesXslDownloader;
import com.ebay.sdk.attributes.CategoryCSDownloader;
import com.ebay.sdk.attributes.model.IAttributesMaster;
import com.ebay.sdk.attributes.model.IAttributesXmlProvider;
import com.ebay.sdk.attributes.model.IAttributesXslProvider;
import com.ebay.sdk.attributes.model.ICategoryCSProvider;
import com.ebay.sdk.call.GetStoreCall;
import com.ebay.sdk.helper.cache.CategoriesDownloader;
import com.ebay.sdk.helper.cache.DetailsDownloader;
import com.ebay.sdk.helper.cache.FeaturesDownloader;
import com.ebay.soap.eBLBaseComponents.CategoryFeatureType;
import com.ebay.soap.eBLBaseComponents.CategoryType;
import com.ebay.soap.eBLBaseComponents.FeatureDefinitionsType;
import com.ebay.soap.eBLBaseComponents.GetCategoryFeaturesResponseType;
import com.ebay.soap.eBLBaseComponents.GetStoreRequestType;
import com.ebay.soap.eBLBaseComponents.GetStoreResponseType;
import com.ebay.soap.eBLBaseComponents.GeteBayDetailsResponseType;
import com.ebay.soap.eBLBaseComponents.SiteCodeType;
import com.ebay.soap.eBLBaseComponents.SiteDefaultsType;
import com.ebay.soap.eBLBaseComponents.StoreCustomCategoryArrayType;
import com.ebay.soap.eBLBaseComponents.StoreCustomCategoryType;
import com.ebay.soap.eBLBaseComponents.StoreType;

public class EbayStoreSiteFacade {
    public static final String module = EbayStoreSiteFacade.class.getName();
    private ApiContext apiContext = null;
    private IAttributesMaster attrMaster = null;
    private static final int MAP_SIZE = 30000;
    private Map<SiteCodeType, Map<String, CategoryType>> siteCategoriesMap = new HashMap<SiteCodeType, Map<String, CategoryType>>();
    private Map<SiteCodeType, List<StoreCustomCategoryType>> siteStoreCategoriesMap = new HashMap<SiteCodeType, List<StoreCustomCategoryType>>();
    private Map<SiteCodeType, List<CategoryType>> siteCategoriesCSMap = new HashMap<SiteCodeType,List<CategoryType>>();
    private Map<SiteCodeType, Map<String, CategoryFeatureType>> siteCategoriesFeaturesMap = new HashMap<SiteCodeType, Map<String, CategoryFeatureType>>();
    private Map<SiteCodeType, SiteDefaultsType> siteFeatureDefaultMap = new HashMap<SiteCodeType, SiteDefaultsType>();
    private Map<SiteCodeType, FeatureDefinitionsType> siteFeatureDefinitionsMap = new HashMap<SiteCodeType, FeatureDefinitionsType>();
    private Map<SiteCodeType, GeteBayDetailsResponseType> eBayDetailsMap = new HashMap<SiteCodeType, GeteBayDetailsResponseType>();

    public EbayStoreSiteFacade(ApiContext ctx) throws ApiException, SdkException, Exception {
        this.apiContext = ctx;
        initAttributeMaster();
        syncAllCategoriesFeatures();
        syncEBayDetails();
        getAllMergedCategories();
        getEbayStoreCategories();
    }

    public static IAttributesXslProvider getDefaultStyleXsl() throws java.io.IOException {
        IAttributesXslProvider iAttr = IAttributesXslProvider.class.cast(AttributesXslDownloader.class.getResourceAsStream("Attributes_Style.xsl"));
        return iAttr;
    }

    private void initAttributeMaster() throws ApiException, SdkException, Exception, java.io.IOException {
        //java.io.InputStream strm = IAttributesXmlProvider.class.getResourceAsStream("Attributes_Style.xsl");
        IAttributesMaster amst = new AttributesMaster();
        IAttributesXmlProvider axd = new AttributesXmlDownloader(this.apiContext);
        amst.setXmlProvider(axd);
        IAttributesXslProvider asd = new AttributesXslDownloader(this.apiContext);
        //IAttributesXslProvider asd = getDefaultStyleXsl();
        //asd.downloadXsl();
        amst.setXslProvider(asd);
        this.attrMaster = amst;
    }

    private void syncEBayDetails() throws Exception {
        if (!eBayDetailsMap.containsKey(this.apiContext.getSite())) {
            DetailsDownloader downloader = new DetailsDownloader(this.apiContext);
            GeteBayDetailsResponseType resp = downloader.geteBayDetails();
            eBayDetailsMap.put(this.apiContext.getSite(), resp);
        }
    }

    //sync and cache all categories features in memory
    private void syncAllCategoriesFeatures() throws Exception {
        if (!siteCategoriesFeaturesMap.containsKey(this.apiContext.getSite())) { 
            FeaturesDownloader fd = new FeaturesDownloader(this.apiContext);
            GetCategoryFeaturesResponseType cfrt = fd.getAllCategoryFeatures();
            CategoryFeatureType[] categoryFeatures = cfrt.getCategory();
            Map<String, CategoryFeatureType> cfsMap = new HashMap<String, CategoryFeatureType>(MAP_SIZE);
            for (CategoryFeatureType cf: categoryFeatures) {
                cfsMap.put(cf.getCategoryID(), cf);
            }
            siteCategoriesFeaturesMap.put(this.apiContext.getSite(), cfsMap);
            siteFeatureDefaultMap.put(this.apiContext.getSite(), cfrt.getSiteDefaults());
            siteFeatureDefinitionsMap.put(this.apiContext.getSite(), cfrt.getFeatureDefinitions());
        }
    }

    /**
     * Get categories using GetCategory2CS and GetCategories calls,
     * and merge the categories
     * 
     */
    public List<CategoryType> getAllMergedCategories() throws ApiException, SdkException, Exception {
        //Get all categories that are mapped to characteristics sets
        IAttributesMaster amst = this.attrMaster;
        if (!siteCategoriesCSMap.containsKey(this.apiContext.getSite())) {
            ICategoryCSProvider catCSProvider = new CategoryCSDownloader(this.apiContext);
            amst.setCategoryCSProvider(catCSProvider);
            CategoryType[] csCats = catCSProvider.getCategoriesCS();
            Map<String, CategoryType> csCatsMap = new HashMap<String, CategoryType>(MAP_SIZE);
            for (CategoryType cat : csCats) {
                csCatsMap.put(cat.getCategoryID(), cat);
            }

            //Get all categories
            Map<String, CategoryType> allCatsMap = this.getAllCategories();
            for (CategoryType cat : allCatsMap.values()) {
                CategoryType csCat = csCatsMap.get(cat.getCategoryID());
                if (csCat != null) {
                    //copy category name and leaf category fields, since these
                    //fields are not set when using GetCategoryCS call.
                    csCat.setCategoryName(cat.getCategoryName());
                    csCat.setLeafCategory(cat.isLeafCategory());
                } else {
                    //some category has no characteristic sets, 
                    //but it may has custom item specifics
                    csCatsMap.put(cat.getCategoryID(), cat);
                }
            }

            //convert the map to list
            List<CategoryType> catsList = new LinkedList<CategoryType>();
            for (CategoryType cat : csCatsMap.values()) {
                catsList.add(cat);
            }
            siteCategoriesCSMap.put(this.apiContext.getSite(), catsList);
            return catsList;
        } else {
            return siteCategoriesCSMap.get(this.apiContext.getSite());
        }
    }

    //get all categories map
    private Map<String, CategoryType> getAllCategories() throws Exception {
        if (!siteCategoriesMap.containsKey(this.apiContext.getSite())) {
            Map<String, CategoryType> catsMap = new HashMap<String, CategoryType>(30000);
            CategoriesDownloader cd = new CategoriesDownloader(this.apiContext);
            CategoryType[] cats = cd.getAllCategories();

            for (CategoryType cat : cats) {
                catsMap.put(cat.getCategoryID(), cat);
            }
            siteCategoriesMap.put(this.apiContext.getSite(), catsMap);
            return catsMap;
        } else {
            return siteCategoriesMap.get(this.apiContext.getSite());
        }
    }

    //get all categories from ebay store depend on siteId
    private List<StoreCustomCategoryType> getEbayStoreCategories() {
        Map<String, StoreCustomCategoryType> catsMap = new HashMap<String, StoreCustomCategoryType>(30000);
        List<StoreCustomCategoryType> catsList = new LinkedList<StoreCustomCategoryType>();
        try {
            GetStoreCall call = new GetStoreCall(this.apiContext);
            GetStoreRequestType req = new GetStoreRequestType();
            GetStoreResponseType resp = null;
            resp = (GetStoreResponseType) call.execute(req);
            if (resp != null && "SUCCESS".equals(resp.getAck().toString())) {
                StoreType store = resp.getStore();
                StoreCustomCategoryArrayType categoriesArr = store.getCustomCategories();
                StoreCustomCategoryType[] cateogries = categoriesArr.getCustomCategory();
                for (StoreCustomCategoryType cat : cateogries) {
                    String categoryId = Long.toString(cat.getCategoryID());
                    catsMap.put(categoryId, cat);
                }
                for (StoreCustomCategoryType cat : catsMap.values()) {
                    catsList.add(cat);
                }
                siteStoreCategoriesMap.put(this.apiContext.getSite(), catsList);
            }
        } catch (Exception e) {
            return siteStoreCategoriesMap.get(this.apiContext.getSite());
        }
        return catsList;
    }

    public IAttributesMaster getAttrMaster() {
        return attrMaster;
    }

    public Map<SiteCodeType, Map<String, CategoryFeatureType>> getSiteCategoriesFeaturesMap() {
        return siteCategoriesFeaturesMap;
    }

    public Map<SiteCodeType, SiteDefaultsType> getSiteFeatureDefaultMap() {
        return siteFeatureDefaultMap;
    }

    public Map<SiteCodeType, FeatureDefinitionsType> getSiteFeatureDefinitionsMap() {
        return siteFeatureDefinitionsMap;
    }

    public Map<SiteCodeType, Map<String, CategoryType>> getSiteCategoriesMap() {
        return siteCategoriesMap;
    }

    public Map<SiteCodeType, List<CategoryType>> getSiteCategoriesCSMap() {
        return siteCategoriesCSMap;
    }

    public Map<SiteCodeType, List<StoreCustomCategoryType>> getSiteStoreCategoriesMap() {
        return siteStoreCategoriesMap;
    }

    public Map<SiteCodeType, GeteBayDetailsResponseType> getEBayDetailsMap() {
        return eBayDetailsMap;
    }
}
