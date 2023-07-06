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

package org.apache.ofbiz.lookup


import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.util.EntityUtil

productId = request.getParameter('productId')

if (productId != null) {
    product = from('Product').where('productId', productId).queryOne()
    prodAssocs = product.getRelated('MainProductAssoc', null, null, false)
    if (prodAssocs) {
        products = EntityUtil.filterByAnd(prodAssocs,
                [EntityCondition.makeCondition('productAssocTypeId', EntityOperator.NOT_EQUAL, 'PRODUCT_VARIANT')])

        if (products) {
            productList = []
            products.each { product ->
                if (product != null) {
                    String productIdTo = product.getString('productIdTo')
                    productList.add(from('Product').where('productId', productIdTo).queryFirst())
                }
            }
            context.put('productList', productList)
        }
    }
}
