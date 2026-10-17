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
package org.apache.ofbiz.product.catalog.category

import org.apache.ofbiz.base.util.UtilProperties

uiLabelMap = UtilProperties.getResourceBundleMap('ProductUiLabels', locale)

contentId = parameters.contentId ?: null

prodCatContentTypeId = parameters.prodCatContentTypeId
context.contentFormName = 'EditCategoryContentSimpleText'
context.contentFormTitle = "${uiLabelMap.ProductUpdateSimpleTextContentCategory}"

if (['PAGE_TITLE', 'META_KEYWORD', 'META_DESCRIPTION'].contains(prodCatContentTypeId)) {
    context.contentFormName = 'EditCategoryContentSEO'
    context.contentFormTitle = "${uiLabelMap.ProductUpdateSEOContentCategory}"
}
if (prodCatContentTypeId == 'RELATED_URL') {
    contentList = from('ContentDataResourceView').where('contentId', contentId).queryList()
    if (contentList) {
        context.contentId = contentList.get(0).contentId
        context.dataResourceId = contentList.get(0).dataResourceId
        context.title = contentList.get(0).drDataResourceName
        context.description = contentList.get(0).description
        context.url = contentList.get(0).drObjectInfo
        context.localeString = contentList.get(0).localeString
    }
    context.contentFormName = 'EditCategoryContentRelatedUrl'
    context.contentFormTitle = "${uiLabelMap.ProductUpdateRelatedURLContentCategory}"
} else if (prodCatContentTypeId == 'VIDEO' || prodCatContentTypeId == 'CATEGORY_IMAGE') {
    if (content) {
        context.fileDataResourceId = content.dataResourceId
    }
    if (prodCatContentTypeId == 'CATEGORY_IMAGE') {
        context.dataResourceTypeId = 'IMAGE_OBJECT'
    } else {
        context.dataResourceTypeId = 'VIDEO_OBJECT'
    }
    context.contentFormName = 'EditCategoryContentDownload'
    context.contentFormTitle = "${uiLabelMap.ProductUpdateDownloadContentCategory}"
}
