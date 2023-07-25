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

package org.apache.ofbiz.product.catalog


import org.apache.ofbiz.base.util.UtilTimer
import org.apache.ofbiz.entity.GenericValue

messageList = []

messageList.add('Loading Categories...')
UtilTimer ctimer = new UtilTimer()
messageList.add(ctimer.timerString('Before category find'))
categories = from('ProductCategory').queryIterator()
messageList.add(ctimer.timerString('Before load all categories into cache'))

category = null
long numCategories = 0
while (categories.hasNext()) {
    category = (GenericValue) categories.next()
    delegator.putInPrimaryKeyCache(category.getPrimaryKey(), category)
    numCategories++
}
categories.close()

messageList.add(ctimer.timerString('Finished Categories'))
messageList.add('Loaded ' + numCategories + ' Categories')

messageList.add('&nbsp;')

messageList.add('Loading Products...')
UtilTimer ptimer = new UtilTimer()
messageList.add(ptimer.timerString('Before product find'))
products = from('Product').queryIterator()
messageList.add(ptimer.timerString('Before load all products into cache'))
product = null
long numProducts = 0
while (products.hasNext()) {
    product = (GenericValue) products.next()
    delegator.putInPrimaryKeyCache(product.getPrimaryKey(), product)
    numProducts++
}
products.close()

messageList.add(ptimer.timerString('Finished Products'))
messageList.add('Loaded ' + numProducts + ' products')

context.messageList = messageList
