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

import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.common.UrlServletHelper
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.util.EntityListIterator
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ModelService
import org.apache.ofbiz.service.ServiceUtil

MODULE = "ContentServices.groovy"
def createTextAndUploadedContent(){
    Map result = success()

    Map serviceResult = run service: 'createContent', with: parameters
    parameters.parentContentId = serviceResult.contentId

    if (parameters.uploadedFile) {
        logInfo('Uploaded file found; processing sub-content')
        Map uploadContext = dispatcher.getDispatchContext()
                .makeValidContext('createContentFromUploadedFile', ModelService.IN_PARAM, parameters)
        uploadContext.ownerContentId = parameters.parentContentId
        uploadContext.contentIdFrom = parameters.parentContentId
        uploadContext.contentAssocTypeId = 'SUB_CONTENT'
        uploadContext.contentPurposeTypeId = 'SECTION'
        run service: 'createContentFromUploadedFile', with: uploadContext
    }

    result.contentId = parameters.parentContentId
    return result
}

def createContentAlternativeUrl() {
    //create Content Alternative URLs.
    String contentCreated
    Map serviceResult = [:]
    Map serviceContext = [:]
    defaultLocaleString = parameters.locale ?: "en"
    EntityListIterator contents

    if (parameters.contentId) {
        entryExprs = EntityCondition.makeCondition([
                EntityCondition.makeCondition("contentName", EntityOperator.NOT_EQUAL, null),
                EntityCondition.makeCondition("contentId", EntityOperator.EQUALS, parameters.contentId),
                EntityCondition.makeCondition([
                        EntityCondition.makeCondition("contentTypeId", EntityOperator.EQUALS, "DOCUMENT"),
                        EntityCondition.makeCondition("contentTypeId", EntityOperator.EQUALS, "WEB_SITE_PUB_PT")], EntityOperator.OR)
        ], EntityOperator.AND)
    } else {
        entryExprs = EntityCondition.makeCondition([
                EntityCondition.makeCondition("contentName", EntityOperator.NOT_EQUAL, null),
                EntityCondition.makeCondition([
                        EntityCondition.makeCondition("contentTypeId", EntityOperator.EQUALS, "DOCUMENT"),
                        EntityCondition.makeCondition("contentTypeId", EntityOperator.EQUALS, "WEB_SITE_PUB_PT")], EntityOperator.OR)
        ], EntityOperator.AND)
    }

    contents = select("contentId", "contentName", "localeString").from("Content").where(entryExprs).queryIterator()
    List contentAssocDataResources = []
    String localeString

    while (content = contents.next()) {
        localeString = content.localeString ?: defaultLocaleString
        contentAssocDataResources = select("contentIdStart", "dataResourceId", "localeString", "drObjectInfo", "caFromDate", "caThruDate").from("ContentAssocDataResourceViewTo").
                where("caContentAssocTypeId", "ALTERNATIVE_URL", "contentIdStart", content.contentId, "localeString", localeString.toString()).filterByDate("caFromDate", "caThruDate").queryList()
        if (!contentAssocDataResources) {
            if (content.contentName) {
                uri = UrlServletHelper.invalidCharacter(content.contentName)
                if (uri) {
                    serviceContext.dataResourceId = delegator.getNextSeqId("DataResource")
                    serviceContext.dataResourceTypeId = "URL_RESOURCE"
                    serviceContext.localeString = localeString.toString()
                    serviceContext.objectInfo = uri + "-" + content.contentId + "-content"
                    serviceContext.statusId = "CTNT_IN_PROGRESS"
                    serviceContext.userLogin = userLogin
                    try {
                        serviceResult = run service: "createDataResource", with: serviceContext
                        if (ServiceUtil.isSuccess(serviceResult)) {
                            dataResourceId = serviceResult.dataResourceId
                        }
                    } catch (GenericServiceException e) {
                        Debug.logInfo(e, MODULE)
                    }
                    if (dataResourceId) {
                        serviceContext.clear()
                        serviceContext.dataResourceId = dataResourceId
                        serviceContext.statusId = "CTNT_IN_PROGRESS"
                        serviceContext.localeString = localeString.toString()
                        serviceContext.userLogin = userLogin
                        try {
                            serviceResult = run service: "createContent", with: serviceContext
                            if (ServiceUtil.isSuccess(serviceResult)) {
                                contentIdTo = serviceResult.contentId
                            }
                        } catch (GenericServiceException e) {
                            Debug.logInfo(e, MODULE)
                        }
                        if (contentIdTo) {
                            serviceContext.clear()
                            serviceContext.contentId = content.contentId
                            serviceContext.contentIdTo = contentIdTo
                            serviceContext.contentAssocTypeId = "ALTERNATIVE_URL"
                            serviceContext.userLogin = userLogin
                            try {
                                serviceResult = run service: "createContentAssoc", with: serviceContext
                                if (ServiceUtil.isSuccess(serviceResult)) {
                                    contentIdTo = serviceResult.contentId
                                }
                            } catch (GenericServiceException e) {
                                Debug.logInfo(e, MODULE)
                            }
                        }
                    }
                    contentCreated = "Y"
                }
            }
        } else {
            if (contentAssocDataResources && contentAssocDataResources.get(0).drObjectInfo) {
                if (content.contentName) {
                    uri = UrlServletHelper.invalidCharacter(content.contentName)
                    if (uri) {
                        serviceContext.clear()
                        serviceContext.dataResourceId = contentAssocDataResources.get(0).dataResourceId
                        serviceContext.objectInfo = "/" + uri + "-" + content.contentId + "-content"
                        serviceContext.userLogin = userLogin
                        try {
                            serviceResult = run service: "updateDataResource", with: serviceContext
                            if (ServiceUtil.isSuccess(serviceResult)) {
                                contentIdTo = serviceResult.contentId
                            }
                        } catch (GenericServiceException e) {
                            Debug.logInfo(e, MODULE)
                        }
                        contentCreated = "Y"
                    }
                }
            } else {
                contentCreated = "N"
            }
        }
    }

    map = success()
    map.contentCreated = contentCreated
    return map
}