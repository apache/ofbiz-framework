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
import javolution.util.FastList;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.product.store.ProductStoreWorker;

cart = session.getAttribute("shoppingCart");
productStoreId = ProductStoreWorker.getProductStoreId(request);
if (productStoreId == null) {
    productStoreId = cart.getProductStoreId();
}

// Get Gift cards availbale in data

giftCardCategories = delegator.findList("ProductCategory", EntityCondition.makeCondition("productCategoryTypeId", EntityOperator.EQUALS, "GIFT_CARD_CATEGORY"), null, null, null, false);
giftCardProductList = FastList.newInstance();
if (UtilValidate.isNotEmpty(giftCardCategories)) {
    giftCardCategories.each { giftCardCategory -> 
        giftCardCategoryMembers = delegator.findList("ProductCategoryMember", EntityCondition.makeCondition("productCategoryId", EntityOperator.EQUALS, giftCardCategory.productCategoryId), null, null, null, false);
        if (UtilValidate.isNotEmpty(giftCardCategoryMembers)) {
            giftCardCategoryMembers.each { giftCardCategoryMember -> 
                giftCardProducts = delegator.findList("ProductAndPriceView", EntityCondition.makeCondition("productId", EntityOperator.EQUALS, giftCardCategoryMember.productId), null, null, null, false);
                if (UtilValidate.isNotEmpty(giftCardProducts)) {
                    giftCardProducts.each { giftCardProduct ->
                        giftCardProductList.add(giftCardProduct);
                    }
                }
            }
        }
    }
}
context.giftCardProductList = giftCardProductList;

// Get Survey Id for Gift Certificates

productStoreFinActSetting = delegator.findOne("ProductStoreFinActSetting", [productStoreId : productStoreId, finAccountTypeId : "GIFTCERT_ACCOUNT"], false);
context.surveyId = productStoreFinActSetting.purchaseSurveyId;

