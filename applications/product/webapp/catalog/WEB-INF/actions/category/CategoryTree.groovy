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
import java.util.List;

// Put the result of CategoryWorker.getRelatedCategories into the separateRootType function as attribute.
// The separateRootType function will return the list of category of given catalog.
// PLEASE NOTE : The structure of the list of separateRootType function is according to the JSON_DATA plugin of the jsTree.

completedTree =  FastList.newInstance();

List separateRootType(roots) {
    if(roots) {
         prodRootTypeTree = FastList.newInstance();
         def i = 0;
        for(root in roots) {
            prodCatalogMap2 = FastMap.newInstance();
             prodCatalogTree2 = FastList.newInstance();
            prodCatalogCategories = FastList.newInstance();
            prodCatalog = root.getRelatedOne("ProductCategory");
            
            productCat = root.getRelatedOne("ProductCategory");
            prodCatalogId = productCat.getString("productCategoryId");
            prodCatalogMap2.put("productCategoryId", prodCatalogId);
            prodCatalogMap2.put("categoryName", productCat.getString("categoryName"));
            prodCatalogMap2.put("isCatalog", false)
            prodCatalogMap.put("isCategoryType", true);
            
            i++;
            
            prodRootTypeTree.add(prodCatalogMap2);
        }
        return prodRootTypeTree;
    }
}

// Get the Catalogs
prodCatalogs = delegator.findByAnd("ProdCatalog");

if (prodCatalogs.size() > 0) {
    for (i = 0; i < prodCatalogs.size(); i++) {
        
        prodCatalogMap = FastMap.newInstance();
        prodCatalog = prodCatalogs[i];
        prodCatalogId = prodCatalog.getString("prodCatalogId");
        prodCatalogMap.put("productCategoryId", prodCatalogId);
        prodCatalogMap.put("categoryName", prodCatalog.getString("catalogName"));
        prodCatalogMap.put("isCatalog", true);
        prodCatalogMap.put("isCategoryType", false);
        
        prodCatalogCategories = EntityUtil.filterByDate(delegator.findByAnd("ProdCatalogCategory", ["prodCatalogId" : prodCatalog.prodCatalogId]));
        
        prodCatalogTree = FastList.newInstance();
        
        if (prodCatalogCategories) {
            prodCatalogTree = separateRootType(prodCatalogCategories);
            prodCatalogMap.put("child", prodCatalogTree);
            completedTree.add(prodCatalogMap);
        }
    }
}
// The complete tree list for the category tree
context.completedTree = completedTree;

stillInCatalogManager = true;
productCategoryId = null;
prodCatalogId = null;
showProductCategoryId = null;

// Reset tree condition check. Are we still in the Catalog Manager ?. If not , then reset the tree.
if ((parameters.productCategoryId != null) || (parameters.showProductCategoryId != null)) {
    stillInCatalogManager = false;
    productCategoryId = parameters.productCategoryId;
    showProductCategoryId = parameters.showProductCategoryId;
} else if (parameters.prodCatalogId != null) {
    stillInCatalogManager = false;
    prodCatalogId = parameters.prodCatalogId;
}
context.stillInCatalogManager = stillInCatalogManager;
context.productCategoryId = productCategoryId;
context.prodCatalogId = prodCatalogId;
context.showProductCategoryId = showProductCategoryId;
