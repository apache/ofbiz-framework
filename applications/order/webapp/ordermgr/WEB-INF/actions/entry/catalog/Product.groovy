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
import org.ofbiz.entity.util.EntityUtil;

contentPathPrefix = CatalogWorker.getContentPathPrefix(request);
catalogName = CatalogWorker.getCatalogName(request);
currentCatalogId = CatalogWorker.getCurrentCatalogId(request);
requestParams = UtilHttp.getParameterMap(request);

detailScreen = "productdetail";
productId = requestParams.product_id ?: request.getAttribute("product_id");

pageTitle = null;
metaDescription = null;
metaKeywords = null;

// get the product entity
if (productId) {
    product = delegator.findOne("Product", [productId : productId], true);
    if (product) {
        // first make sure this isn't a virtual-variant that has an associated virtual product, if it does show that instead of the variant
        if("Y".equals(product.isVirtual) && "Y".equals(product.isVariant)){
            virtualVariantProductAssocs = delegator.findByAnd("ProductAssoc", ["productId": productId, "productAssocTypeId": "ALTERNATIVE_PACKAGE"], ["-fromDate"], true);
            virtualVariantProductAssocs = EntityUtil.filterByDate(virtualVariantProductAssocs);
            if (virtualVariantProductAssocs) {
                productAssoc = EntityUtil.getFirst(virtualVariantProductAssocs);
                product = productAssoc.getRelatedOne("AssocProduct", true);
            }
        }
    }
    
    // first make sure this isn't a variant that has an associated virtual product, if it does show that instead of the variant
    virtualProductId = ProductWorker.getVariantVirtualId(product);
    if (virtualProductId) {
        productId = virtualProductId;
        product = delegator.findOne("Product", [productId : productId], true);
    }

    productPageTitle = delegator.findByAnd("ProductContentAndInfo", [productId : productId, productContentTypeId : "PAGE_TITLE"], null, true);
    if (productPageTitle) {
        pageTitle = delegator.findOne("ElectronicText", [dataResourceId : productPageTitle.get(0).dataResourceId], true);
    }
    productMetaDescription = delegator.findByAnd("ProductContentAndInfo", [productId : productId, productContentTypeId : "META_DESCRIPTION"], null, true);
    if (productMetaDescription) {
        metaDescription = delegator.findOne("ElectronicText", [dataResourceId : productMetaDescription.get(0).dataResourceId], true);
    }
    productMetaKeywords = delegator.findByAnd("ProductContentAndInfo", [productId : productId, productContentTypeId : "META_KEYWORD"], null, true);
    if (productMetaKeywords) {
        metaKeywords = delegator.findOne("ElectronicText", [dataResourceId : productMetaKeywords.get(0).dataResourceId], true);
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

        if (pageTitle) {
            context.title = pageTitle.textData;
        } else {
            context.put("title", contentWrapper.get("PRODUCT_NAME"));
        }

        if (metaDescription) {
            context.metaDescription = metaDescription.textData;
        } else {
            context.put("metaDescription", contentWrapper.get("DESCRIPTION"));
        }

        if (metaKeywords) {
            context.metaKeywords = metaKeywords.textData;
        } else {
            keywords = [];
            keywords.add(contentWrapper.get("PRODUCT_NAME"));
            keywords.add(catalogName);
            members = delegator.findByAnd("ProductCategoryMember", [productId : productId], null, true);
            members.each { member ->
                category = member.getRelatedOne("ProductCategory", true);
                if (category.description) {
                    categoryContentWrapper = new CategoryContentWrapper(category, request);
                    categoryDescription = categoryContentWrapper.DESCRIPTION;
                    if (categoryDescription) {
                            keywords.add(categoryDescription);
                    }
                }
            }
            context.metaKeywords = StringUtil.join(keywords, ", ");
        }

        // Set the default template for aggregated product (product component configurator ui)
        if (product.productTypeId && ("AGGREGATED".equals(product.productTypeId) || "AGGREGATED_SERVICE".equals(product.productTypeId)) && context.configproductdetailScreen) {
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
