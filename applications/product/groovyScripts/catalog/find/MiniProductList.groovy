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

import org.apache.ofbiz.base.util.*

state = request.getParameter("CategoryProductsState")
isOpen = true
if (state) {
    session.setAttribute("CategoryProductsState", state)
    isOpen = "open".equals(state)
} else {
    state = (String) session.getAttribute("CategoryProductsState")
    if (state) {
        isOpen = "open".equals(state)
    }
}
context.isOpen = isOpen

// Get a list of all products in the current category
if (isOpen) {
    paramInMap = [:]
    paramInMap.productCategoryId = UtilFormatOut.checkNull(request.getParameter("productCategoryId"))
    paramInMap.defaultViewSize = 30
    paramInMap.limitView = true
    paramInMap.useCacheForMembers = false
    paramInMap.checkViewAllow = false

    // Returns: viewIndex, viewSize, lowIndex, highIndex, listSize, productCategory, productCategoryMembers
    outMap = runService('getProductCategoryAndLimitedMembers', paramInMap)
    context.viewIndex = outMap.viewIndex
    context.viewSize = outMap.viewSize
    context.lowIndex = outMap.lowIndex
    context.highIndex = outMap.highIndex
    context.listSize = outMap.listSize
    context.productCategory = outMap.productCategory
    context.productCategoryMembers = outMap.productCategoryMembers
}
