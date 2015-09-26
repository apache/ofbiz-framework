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
import org.ofbiz.product.product.ProductContentWrapper;
import org.ofbiz.product.category.*;
import org.ofbiz.base.util.UtilValidate;

parentCategoryStr = parameters.parentCategoryStr;
if(!UtilValidate.isEmpty(parentCategoryStr)) {
    pathList = parentCategoryStr.split('/');
    cateList = [];
    pathTemp = '';
    for(path in pathList) {
        cateMap = [:];
        category = from("ProductCategory").where("productCategoryId", path).queryOne();
        categoryContentWrapper = new CategoryContentWrapper(category, request);
        
        pathTemp = pathTemp + path;
        cateMap.title = categoryContentWrapper.get("DESCRIPTION", "html");
        cateMap.productCategoryId = category.productCategoryId;
        cateMap.parentCategory = pathTemp;
        
        cateList.add(cateMap);
        
        pathTemp = pathTemp + '/';
    }

    context.productCategoryTrail = cateList;
}
currentCategory = from("ProductCategory").where("productCategoryId", productCategoryId).queryOne();
currentCategoryContentWrapper = new CategoryContentWrapper(currentCategory, request);
context.currentCategoryName = currentCategoryContentWrapper.get("CATEGORY_NAME", "html");
context.currentCategoryDescription = currentCategoryContentWrapper.get("DESCRIPTION", "html");
context.currentCategoryId = productCategoryId;
