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

productId = parameters.productId;
if (productId) {
    productContents  = delegator.findByAnd("ProductContent", ["productId" : productId], null, false);
    productContents.each{ productContent->
        if (productContent.productContentTypeId == "PAGE_TITLE") {
            contentTitle  = delegator.findOne("Content", ["contentId" : productContent.contentId], false);
            dataTextTitle  = delegator.findOne("ElectronicText", ["dataResourceId" : contentTitle.dataResourceId], false);
            context.title = dataTextTitle.textData;
        }
        if (productContent.productContentTypeId == "META_KEYWORD") {
            contentMetaKeyword  = delegator.findOne("Content", ["contentId" : productContent.contentId], false);
            dataTextMetaKeyword  = delegator.findOne("ElectronicText", ["dataResourceId" : contentMetaKeyword.dataResourceId], false);
            context.metaKeyword = dataTextMetaKeyword.textData;
        }
        if (productContent.productContentTypeId == "META_DESCRIPTION") {
            contentMetaDescription  = delegator.findOne("Content", ["contentId" : productContent.contentId], false);
            dataTextMetaDescription  = delegator.findOne("ElectronicText", ["dataResourceId" : contentMetaDescription.dataResourceId], false);
            context.metaDescription = dataTextMetaDescription.textData;
        }
    }
}
