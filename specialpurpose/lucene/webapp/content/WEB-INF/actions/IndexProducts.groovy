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


import org.ofbiz.content.search.ProductDocument
import org.ofbiz.content.search.DocumentIndexer
import org.ofbiz.entity.transaction.TransactionUtil
import org.ofbiz.entity.util.EntityListIterator

DocumentIndexer pi = DocumentIndexer.getInstance(delegator, 'products')
if (pi) {
    productsCounter = 0
    beganTransaction = TransactionUtil.begin()
    EntityListIterator products
    try {
        products = select("productId").from("Product").queryIterator();
        while (product = products.next()) {
            pi.queue(new ProductDocument(product.productId))
            productsCounter++
        }
    } catch(Exception e) {
        TransactionUtil.rollback(beganTransaction, e.getMessage(), e)
        return error(e.getMessage())
   } finally {
        if (products != null) {
            try {
                products.close()
            } catch (Exception exc) {}
        }
        TransactionUtil.commit(beganTransaction)
    }
    return success("Submitted for indexing $productsCounter products")
} else {
    return error()
}