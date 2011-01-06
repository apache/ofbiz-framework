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

import org.ofbiz.base.util.*;
import org.ofbiz.product.catalog.*;
import org.ofbiz.product.category.*;
import org.ofbiz.entity.GenericValue;
import javolution.util.FastMap;
import javolution.util.FastList;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import javax.servlet.http.HttpSession;

prodCatalogList = FastList.newInstance();
prodCatalogs = delegator.findByAnd("ProdCatalog");
if (prodCatalogs.size() > 0) {
    for (i = 0; i < prodCatalogs.size(); i++) {
        
        prodCatalogMap = FastMap.newInstance();
        prodCatalog = prodCatalogs[i];
        prodCatalogId = prodCatalog.getString("prodCatalogId");
        prodCatalogMap.put("prodCatalogId", prodCatalogId);
        prodCatalogMap.put("catalogName", prodCatalog.getString("catalogName"));
        prodCatalogMap.put("catalogName", prodCatalog.getString("catalogName"));

        //root category list of the catalog
        prodCategoryList = CatalogWorker.getProdCatalogCategories(request, prodCatalogId, null);
        rootCategoryList = FastList.newInstance();
        if (prodCategoryList.size() > 0) {
            for (j = 0; j < prodCategoryList.size(); j++) {
                prodCategory = prodCategoryList[j];
                rootCategory = delegator.findByPrimaryKey("ProductCategory", ["productCategoryId" : prodCategory.getString("productCategoryId")]);
                rootCategoryList.add(rootCategory);
            }
        }

        if (rootCategoryList) {
            catContentWrappers = [:];
            CategoryWorker.getCategoryContentWrappers(catContentWrappers, rootCategoryList, request);
            prodCatalogMap.put("rootCategoryList", rootCategoryList);
            prodCatalogMap.put("catContentWrappers", catContentWrappers);
            prodCatalogList.add(prodCatalogMap);
        }
    }
}
context.prodCatalogList = prodCatalogList;
