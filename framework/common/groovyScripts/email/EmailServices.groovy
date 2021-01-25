import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ModelService
import org.apache.ofbiz.service.ServiceUtil

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

 /**
  * Send Mail from Email Template Setting
  * @return
  */
def sendMailFromTemplateSetting() {
    Map result = success()
    // if partyIdTo provided but no emailAddress, get it from the partyContactMech
    if (!parameters.sendTo && !parameters.partyIdTo) {
        logError("PartyId or SendTo should be specified!")
        return error(UtilProperties.getMessage("CommonUiLabels", "CommonEmailShouldBeSpecified", parameters.locale))
    }
    if (parameters.partyIdTo && !parameters.sendTo) {
        Map getEmail = [partyId: parameters.partyIdTo]
        Map serviceResult = run service: "getPartyEmail", with: getEmail
        if (!ServiceUtil.isSuccess(serviceResult)) {
            return serviceResult
        }
        parameters.sendTo = serviceResult.emailAddress
        if (!parameters.sendTo) {
            logInfo("PartyId: ${parameters.partyIdTo} has no valid email address, not sending email")
            return result;
        }
    }
    GenericValue emailTemplateSetting = from("EmailTemplateSetting").where(parameters).queryOne()
    if (emailTemplateSetting) {
        Map emailParams = [:]
        emailParams.bodyScreenUri = emailTemplateSetting.bodyScreenLocation
        emailParams.xslfoAttachScreenLocation = emailTemplateSetting.xslfoAttachScreenLocation
        emailParams.partyId = parameters.partyIdTo
        if (emailTemplateSetting.fromAddress) {
            emailParams.sendFrom = emailTemplateSetting.fromAddress
        } else {
            emailParams.sendFrom = UtilProperties.getPropertyValue("general", "defaultFromEmailAddress", "ofbizsupport@example.com")
        }
        emailParams.sendCc = emailTemplateSetting.ccAddress
        emailParams.sendBcc = emailTemplateSetting.bccAddress
        emailParams.subject = emailTemplateSetting.subject
        emailParams.contentType = emailTemplateSetting.contentType ?: "text/html"
        if (parameters.custRequestId) {
            Map bodyParameters = [custRequestId: parameters.custRequestId]
            emailParams.bodyParameters = bodyParameters
        }
        // copy the incoming parameter fields AFTER setting the ones from EmailTemplateSetting so they can override things like subject, sendFrom, etc
        emailParams << parameters
        Map sendMailResult = run service: "sendMailFromScreen", with: emailParams
        if (!ServiceUtil.isSuccess(sendMailResult)) {
            return sendMailResult
        }
        result.messageWrapper = sendMailResult.messageWrapper
        result.body = sendMailResult.body
        result.communicationEventId = sendMailResult.communicationEventId
    } else {
        logError("sendMailFromTemplateSetting service could not find the emailTemplateSettingId: ${parameters.emailTemplateSettingId}")
    }
    return result
}












