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

import java.lang.*;
import java.util.*;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.service.*;
import org.ofbiz.product.catalog.*;
import org.ofbiz.product.category.*;
import org.ofbiz.product.product.ProductWorker;
import org.ofbiz.product.product.ProductContentWrapper;

contentPathPrefix = CatalogWorker.getContentPathPrefix(request);
catalogName = CatalogWorker.getCatalogName(request);
currentCatalogId = CatalogWorker.getCurrentCatalogId(request);
requestParams = UtilHttp.getParameterMap(request);

detailScreen = "productdetail";
productId = requestParams.product_id ?: request.getAttribute("product_id");

/*
 * NOTE JLR 20070221 this should be done using the same method than in add to cart. I will do it like that and remove all this after.
 *
if (productId) {
    previousParams = session.getAttribute("_PREVIOUS_PARAMS_");
    if (previousParams) {
        previousParams = UtilHttp.stripNamedParamsFromQueryString(previousParams, ["product_id"]);
        previousParams += "&product_id=" + productId;
    } else {
        previousParams = "product_id=" + productId;
    }
    session.setAttribute("_PREVIOUS_PARAMS_", previousParams);    // for login
    context.previousParams = previousParams;
}*/

// get the product entity
if (productId) {
    product = delegator.findByPrimaryKeyCache("Product", [productId : productId]);

    // first make sure this isn't a variant that has an associated virtual product, if it does show that instead of the variant
    virtualProductId = ProductWorker.getVariantVirtualId(product);
    if (virtualProductId) {
        productId = virtualProductId;
        product = delegator.findByPrimaryKeyCache("Product", [productId : productId]);
    }

    context.productId = productId;

    // now check to see if there is a view allow category and if this product is in it...
    if (product) {
        viewProductCategoryId = CatalogWorker.getCatalogViewAllowCategoryId(delegator, currentCatalogId);
        if (viewProductCategoryId) {
            if (!CategoryWorker.isProductInCategory(delegator, productId, viewProductCategoryId)) {
                // a view allow productCategoryId was found, but the product is not in the category, axe it...
                product = null;
            }
        }
    }

    if (product) {
        context.product = product;
        contentWrapper = new ProductContentWrapper(product, request);
        context.put("title", contentWrapper.get("PRODUCT_NAME"));
        context.put("metaDescription", contentWrapper.get("DESCRIPTION"));

        keywords = [];
        keywords.add(product.productName);
        keywords.add(catalogName);
        members = delegator.findByAndCache("ProductCategoryMember", [productId : productId]);
        members.each { member ->
            category = member.getRelatedOneCache("ProductCategory");
            if (category.description) {
                keywords.add(category.description);
            }
        }
        context.metaKeywords = StringUtil.join(keywords, ", ");

        // Set the default template for aggregated product (product component configurator ui)
        if (product.productTypeId && "AGGREGATED".equals(product.productTypeId) && context.configproductdetailScreen) {
            detailScreen = context.configproductdetailScreen;
        }

        productTemplate = product.detailScreen;
        if (productTemplate) {
            detailScreen = productTemplate;
        }
    }
}

//  check the catalog's template path and update
templatePathPrefix = CatalogWorker.getTemplatePathPrefix(request);
if (templatePathPrefix) {
    detailScreen = templatePathPrefix + detailScreen;
}

// set the template for the view
context.detailScreen = detailScreen;
