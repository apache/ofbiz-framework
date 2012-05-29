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

/*
 * This script is also referenced by the ecommerce's screens and
 * should not contain order component's specific code.
 */
import org.ofbiz.entity.util.EntityUtil;

import org.ofbiz.base.util.*;
import org.ofbiz.product.catalog.*;
import org.ofbiz.product.category.*;
import javolution.util.FastMap;
import javolution.util.FastList;
import javolution.util.FastList.*;
import org.ofbiz.entity.*;

List fillTree(rootCat ,CatLvl, parentCategoryId) {
    if(rootCat) {
        rootCat.sort{ it.productCategoryId }
        def listTree = FastList.newInstance();
        for(root in rootCat) {
            preCatChilds = delegator.findByAnd("ProductCategoryRollup", ["parentProductCategoryId": root.productCategoryId], null, false);
            catChilds = EntityUtil.getRelated("CurrentProductCategory",null,preCatChilds,false);
            def childList = FastList.newInstance();
            
            // CatLvl uses for identify the Category level for display different css class
            if(catChilds) {
                if(CatLvl==2)
                    childList = fillTree(catChilds,CatLvl+1, parentCategoryId.replaceAll("/", "")+'/'+root.productCategoryId);
                    // replaceAll and '/' uses for fix bug in the breadcrum for href of category
                else if(CatLvl==1)
                    childList = fillTree(catChilds,CatLvl+1, parentCategoryId.replaceAll("/", "")+root.productCategoryId);
                else
                    childList = fillTree(catChilds,CatLvl+1, parentCategoryId+'/'+root.productCategoryId);
            }
            
            productsInCat  = delegator.findByAnd("ProductCategoryAndMember", ["productCategoryId": root.productCategoryId], null, false);
            
            // Display the category if this category containing products or contain the category that's containing products
            if(productsInCat || childList) {
                def rootMap = FastMap.newInstance();
                category = delegator.findOne("ProductCategory", ["productCategoryId": root.productCategoryId], false);
                categoryContentWrapper = new CategoryContentWrapper(category, request);
                context.title = categoryContentWrapper.CATEGORY_NAME;
                categoryDescription = categoryContentWrapper.DESCRIPTION;
                
                if(categoryContentWrapper.CATEGORY_NAME)
                    rootMap["categoryName"] = categoryContentWrapper.CATEGORY_NAME;
                else
                    rootMap["categoryName"] = root.categoryName;
                
                if(categoryContentWrapper.DESCRIPTION)
                    rootMap["categoryDescription"] = categoryContentWrapper.DESCRIPTION;
                else
                    rootMap["categoryDescription"] = root.description;
                
                rootMap["productCategoryId"] = root.productCategoryId;
                rootMap["parentCategoryId"] = parentCategoryId;
                rootMap["child"] = childList;

                listTree.add(rootMap);
            }
        }
        return listTree;
    }
}

CategoryWorker.getRelatedCategories(request, "topLevelList", CatalogWorker.getCatalogTopCategoryId(request, CatalogWorker.getCurrentCatalogId(request)), true);
curCategoryId = parameters.category_id ?: parameters.CATEGORY_ID ?: "";
request.setAttribute("curCategoryId", curCategoryId);
CategoryWorker.setTrail(request, curCategoryId);

categoryList = request.getAttribute("topLevelList");
if (categoryList) {
    catContentWrappers = FastMap.newInstance();
    CategoryWorker.getCategoryContentWrappers(catContentWrappers, categoryList, request);
    context.catContentWrappers = catContentWrappers;
    completedTree = fillTree(categoryList,1,"");
    context.completedTree = completedTree;
}
