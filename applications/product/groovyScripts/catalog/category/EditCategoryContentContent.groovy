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

import org.ofbiz.entity.*;
import org.ofbiz.entity.util.*;
import org.ofbiz.base.util.*;
import java.sql.Timestamp;

uiLabelMap = UtilProperties.getResourceBundleMap("ProductUiLabels", locale);

contentId = parameters.contentId;
if (!contentId) {
    contentId = null;
}

prodCatContentTypeId = parameters.prodCatContentTypeId;
context.contentFormName = "EditCategoryContentSimpleText";
context.contentFormTitle = "${uiLabelMap.ProductUpdateSimpleTextContentCategory}";

if (("PAGE_TITLE".equals(prodCatContentTypeId))||("META_KEYWORD".equals(prodCatContentTypeId))||("META_DESCRIPTION".equals(prodCatContentTypeId))) {
    context.contentFormName = "EditCategoryContentSEO";
    context.contentFormTitle = "${uiLabelMap.ProductUpdateSEOContentCategory}";
}
if ("RELATED_URL".equals(prodCatContentTypeId)) {
    contentList = from("ContentDataResourceView").where("contentId", contentId).queryList();
    if (contentList) {
        context.contentId = contentList.get(0).contentId;
        context.dataResourceId = contentList.get(0).dataResourceId;
        context.title = contentList.get(0).drDataResourceName;
        context.description = contentList.get(0).description;
        context.url = contentList.get(0).drObjectInfo;
        context.localeString = contentList.get(0).localeString;
    }
    context.contentFormName = "EditCategoryContentRelatedUrl";
    context.contentFormTitle = "${uiLabelMap.ProductUpdateRelatedURLContentCategory}";
}else if ("VIDEO".equals(prodCatContentTypeId) || "CATEGORY_IMAGE".equals(prodCatContentTypeId)) {
    if (UtilValidate.isNotEmpty(content)) {
        context.fileDataResourceId = content.dataResourceId;
    }
    if("CATEGORY_IMAGE".equals(prodCatContentTypeId)){
        context.dataResourceTypeId = "IMAGE_OBJECT";
    }else{
        context.dataResourceTypeId = "VIDEO_OBJECT";
    }
    context.contentFormName = "EditCategoryContentDownload";
    context.contentFormTitle = "${uiLabelMap.ProductUpdateDownloadContentCategory}";
    
}
