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

import org.apache.ofbiz.entity.*
import org.apache.ofbiz.entity.util.*
import org.apache.ofbiz.base.util.*
import java.sql.Timestamp
import org.apache.ofbiz.base.util.ObjectType

contentId = parameters.contentId
if (!contentId) {
    contentId = null
}

productContentTypeId = parameters.productContentTypeId

fromDate = parameters.fromDate
if (!fromDate) {
    fromDate = null
} else {
    fromDate = ObjectType.simpleTypeOrObjectConvert(fromDate, "Timestamp", null, null, false)
}


description = parameters.description
if (!description) {
    description = null
}

productContent = from("ProductContent").where("contentId", contentId, "productId", productId, "productContentTypeId", productContentTypeId, "fromDate", fromDate).queryOne()
if (!productContent) {
    productContent = [:]
    productContent.productId = productId
    productContent.contentId = contentId
    productContent.productContentTypeId = productContentTypeId
    productContent.fromDate = fromDate
    productContent.thruDate = parameters.thruDate
    productContent.purchaseFromDate = parameters.purchaseFromDate
    productContent.purchaseThruDate = parameters.purchaseThruDate
    productContent.useCountLimit = parameters.useCountLimit
    productContent.useTime = parameters.useTime
    productContent.useTimeUomId = parameters.useTimeUomId
    productContent.useRoleTypeId = parameters.useRoleTypeId
}
context.productContent = productContent

productContentData = [:]
productContentData.putAll(productContent)

content = [:]
context.contentId = contentId
if (contentId) {
    content = from("Content").where("contentId", contentId).queryOne()
    context.content = content
} else {
    if (description) {
        content.description = description
    }
}

//Email
if ("FULFILLMENT_EMAIL".equals(productContentTypeId)) {
    emailData = [:]
    if (contentId && content) {
        subjectDr = content.getRelatedOne("DataResource", false)
        if (subjectDr) {
            subject = subjectDr.getRelatedOne("ElectronicText", false)
            emailData.subject = subject.textData
            emailData.subjectDataResourceId = subject.dataResourceId
        }
        result = runService('findAssocContent', [userLogin : userLogin, contentId : contentId, mapKeys : ['plainBody', 'htmlBody']])
        contentAssocs = result.get("contentAssocs")
        if (contentAssocs) {
            contentAssocs.each { contentAssoc ->
                bodyContent = contentAssoc.getRelatedOne("ToContent", false)
                bodyDr = bodyContent.getRelatedOne("DataResource", false)
                body = bodyDr.getRelatedOne("ElectronicText", false)
                emailData.put(contentAssoc.mapKey, body.textData)
                emailData.put(contentAssoc.get("mapKey")+"DataResourceId", body.dataResourceId)
            }
        }
    }

    context.contentFormName = "EditProductContentEmail"
    context.emailData = emailData
} else if ("DIGITAL_DOWNLOAD".equals(productContentTypeId)) {
    downloadData = [:]
    if (contentId && content) {
        downloadDr = content.getRelatedOne("DataResource", false)
        if (downloadDr) {
            download = downloadDr.getRelatedOne("OtherDataResource", false)
            if (download) {
                downloadData.file = download.dataResourceContent
                downloadData.fileDataResourceId = download.dataResourceId
            }
        }
    }
    context.contentFormName = "EditProductContentDownload"
    context.downloadData = downloadData
} else if ("FULFILLMENT_EXTERNAL".equals(productContentTypeId)) {
    context.contentFormName = "EditProductContentExternal"
} else if (productContentTypeId && productContentTypeId.indexOf("_IMAGE") > -1) {
    context.contentFormName = "EditProductContentImage"
} else {
    //Assume it is a generic simple text content
    textData = [:]
    if (contentId && content) {
        textDr = content.getRelatedOne("DataResource", false)
        if (textDr) {
            text = textDr.getRelatedOne("ElectronicText", false)
            if (text) {
                textData.text = text.textData
                textData.textDataResourceId = text.dataResourceId
            }
        }
    }
    context.contentFormName = "EditProductContentSimpleText"
    context.textData = textData
}
if (productContentTypeId) {
    productContentType = from("ProductContentType").where("productContentTypeId", productContentTypeId).queryOne()
    if (productContentType && "DIGITAL_DOWNLOAD".equals(productContentType.parentTypeId)) {
        context.contentFormName = "EditProductContentDownload"
    }
}
if (("PAGE_TITLE".equals(productContentTypeId))||("META_KEYWORD".equals(productContentTypeId))||("META_DESCRIPTION".equals(productContentTypeId))) {
    context.contentFormName = "EditProductContentSEO"
}

context.productContentData = productContentData
context.content = content
context.contentId = contentId
