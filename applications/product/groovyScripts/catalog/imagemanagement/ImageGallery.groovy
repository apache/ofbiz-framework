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

import org.apache.ofbiz.base.util.*
import org.apache.ofbiz.entity.*
import org.apache.ofbiz.entity.condition.*
import org.apache.ofbiz.entity.util.*

productImageList = []
productContentAndInfoImageManamentList = from("ProductContentAndInfo").where("productId", productId, "productContentTypeId", "IMAGE", "statusId", "IM_APPROVED").orderBy("sequenceNum").queryList()
if(productContentAndInfoImageManamentList) {
    productContentAndInfoImageManamentList.each { productContentAndInfoImageManament ->
        contentAssocThumb = from("ContentAssoc").where("contentId", productContentAndInfoImageManament.contentId, "contentAssocTypeId", "IMAGE_THUMBNAIL").queryFirst()
        if(contentAssocThumb) {
            imageContentThumb = from("Content").where("contentId", contentAssocThumb.contentIdTo).queryOne()
            if(imageContentThumb) {
                productImageThumb = from("ContentDataResourceView").where("contentId", imageContentThumb.contentId, "drDataResourceId", imageContentThumb.dataResourceId).queryOne()
                productImageMap = [:]
                productImageMap.contentId = productContentAndInfoImageManament.contentId
                productImageMap.dataResourceId = productContentAndInfoImageManament.dataResourceId
                productImageMap.productImageThumb = productImageThumb.drObjectInfo
                productImageMap.productImage = productContentAndInfoImageManament.drObjectInfo
                productImageList.add(productImageMap)
            }
        }
    }
    context.productImageList = productImageList
}
