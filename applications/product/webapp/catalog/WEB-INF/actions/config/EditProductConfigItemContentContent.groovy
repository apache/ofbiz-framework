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
import org.ofbiz.widget.renderer.html.HtmlFormWrapper;

contentId = request.getParameter("contentId") ?: null;

confItemContentTypeId = request.getParameter("confItemContentTypeId");

description = request.getParameter("description") ?: null;

productContent = from("ProdConfItemContent").where("contentId", contentId, "configItemId", configItemId, "confItemContentTypeId", confItemContentTypeId, "fromDate", fromDate).queryOne();
if (!productContent) {
    productContent = [:];
    productContent.configItemId = configItemId;
    productContent.contentId = contentId;
    productContent.confItemContentTypeId = confItemContentTypeId;
    productContent.fromDate = fromDate;
    productContent.thruDate = request.getParameter("thruDate");
}
context.productContent = productContent;

productContentData = [:];
productContentData.putAll(productContent);
Map content = null;

context.contentId = contentId;
if (contentId) {
    content = from("Content").where("contentId", contentId).queryOne();
    context.content = content;
} else {
    content = [:];
    if (description) {
        content.description = description;
    }
}

//Assume it is a generic simple text content
textData = [:];
if (contentId && content) {
    textDr = content.getRelatedOne("DataResource", false);
    if (textDr) {
        text = textDr.getRelatedOne("ElectronicText", false);
        if (text) {
            textData.text = text.textData;
            textData.textDataResourceId = text.dataResourceId;
        }
    }
}
updateProductContentWrapper = new HtmlFormWrapper("component://product/widget/catalog/ConfigForms.xml", "EditProductConfigItemContentSimpleText", request, response);
updateProductContentWrapper.putInContext("textData", textData);

context.updateProductContentWrapper = updateProductContentWrapper;
updateProductContentWrapper.putInContext("productContentData", productContentData);

context.productContentData = productContentData;
updateProductContentWrapper.putInContext("content", content);
updateProductContentWrapper.putInContext("contentId", contentId);
