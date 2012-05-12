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
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilDateTime;
import java.math.BigDecimal;
import javax.servlet.http.HttpSession;

defaultLocaleString = "";
if (parameters.productStoreId) {
    productStore = delegator.findByAnd("ProductStore", ["productStoreId":parameters.productStoreId]);
    defaultLocaleString = productStore[0].defaultLocaleString.toString()
}
active = parameters.ACTIVE_PRODUCT;
notSynced = parameters.GOOGLE_SYNCED;

productList = FastList.newInstance();
if (UtilValidate.isNotEmpty(productIds) && ("Y".equals(active) || "Y".equals(notSynced))) {
    for (int i = 0; i < productIds.size(); i++) {
        productId = productIds[i];
        productCategoryMembers = delegator.findByAnd("ProductCategoryMember", [productId : productId]);
        productCategoryMember = EntityUtil.getFirst(productCategoryMembers);
        if (UtilValidate.isNotEmpty(productCategoryMember)) {
            if ("Y".equals(active) && "Y".equals(notSynced)) {
                thruDate = productCategoryMember.get("thruDate");
                goodIdentification = delegator.findOne("GoodIdentification", [productId : productId, goodIdentificationTypeId : "GOOGLE_ID_" + defaultLocaleString], false);
                if (UtilValidate.isEmpty(thruDate) && UtilValidate.isEmpty(goodIdentification)) {
                    productList.add(productId);
                }
            } else if ("Y".equals(active)) {
                thruDate = productCategoryMember.get("thruDate");
                if (UtilValidate.isEmpty(thruDate)) {
                    productList.add(productId);
                }
                parameters.GOOGLE_SYNCED = "N"
            } else if ("Y".equals(notSynced)) {
                goodIdentification = delegator.findOne("GoodIdentification", [productId : productId, goodIdentificationTypeId : "GOOGLE_ID_" + defaultLocaleString], false);
                if (UtilValidate.isEmpty(goodIdentification)) {
                    productList.add(productId);
                }
                parameters.ACTIVE_PRODUCT = "N"
            }
        }
    }
    productIds = productList;
} else {
    parameters.ACTIVE_PRODUCT = "N"
    parameters.GOOGLE_SYNCED = "N"
}

//stop sending discontinued product.
def notDiscontProdList = []
if(parameters.DISCONTINUED_PRODUCT == 'Y'){
    productIds.each { value ->
        def stockIsZero = dispatcher.runSync("isStoreInventoryAvailable", ["productId": value, "productStoreId": parameters.productStoreId, "quantity": BigDecimal.valueOf(1.00)]);
        def thisProduct = delegator.findOne("Product", [productId : value], false);
        if (stockIsZero.get("available") == 'Y'){
            notDiscontProdList.add(value);
        }else {
            if (thisProduct.salesDiscontinuationDate == null || thisProduct.salesDiscontinuationDate > UtilDateTime.nowTimestamp()){
                notDiscontProdList.add(value);
            }
        }
    }
} else {
    parameters.DISCONTINUED_PRODUCT = 'N'
}
if (notDiscontProdList){
    context.productIds = notDiscontProdList;
} else {
    context.productIds = productIds;
}
