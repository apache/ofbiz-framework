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

import java.lang.*;
import java.util.*;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.service.*;
import org.ofbiz.product.catalog.*;

currentCatalogId = CatalogWorker.getCurrentCatalogId(request);
categoryId = parameters.category_id ?: CatalogWorker.getCatalogQuickaddCategoryPrimary(request);

quickAddCategories = CatalogWorker.getCatalogQuickaddCategories(request);
context.quickAddCats = quickAddCategories;
context.categoryId = categoryId;

if (categoryId) {
    fields = [productCategoryId : categoryId, defaultViewSize : 10,
            limitView : false, prodCatalogId : currentCatalogId, checkViewAllow : true];
    result = runService('getProductCategoryAndLimitedMembers', fields);
    if (result) {
        result.each { key, value ->
            context[key] = value;
        }
    }
    productCategory = from("ProductCategory").where("productCategoryId", categoryId).queryOne();
    context.productCategory = productCategory;
}
