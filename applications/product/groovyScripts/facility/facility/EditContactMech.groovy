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

import org.apache.ofbiz.party.contact.*

facilityId = parameters.facilityId
context.facilityId = facilityId

facility = from("Facility").where("facilityId", facilityId).queryOne()
context.facility = facility

mechMap = [:]
ContactMechWorker.getFacilityContactMechAndRelated(request, facilityId, mechMap)
context.mechMap = mechMap

contactMechId = mechMap.contactMechId
if (contactMechId) {
    context.contactMechId = contactMechId
}

preContactMechTypeId = request.getParameter("preContactMechTypeId")
if (preContactMechTypeId) {
    context.preContactMechTypeId = preContactMechTypeId
}

paymentMethodId = request.getParameter("paymentMethodId")
if (!paymentMethodId) {
    paymentMethodId = request.getAttribute("paymentMethodId")
}
if (paymentMethodId) {
    context.paymentMethodId = paymentMethodId
}

donePage = request.getParameter("DONE_PAGE")
if (!donePage) {
    donePage = request.getAttribute("DONE_PAGE")
}
if (!donePage || donePage.length() <= 0) {
    donePage = "ViewContactMechs"
}
context.donePage = donePage

cmNewPurposeTypeId = request.getParameter("contactMechPurposeTypeId")
if (!cmNewPurposeTypeId) {
    cmNewPurposeTypeId = mechMap.contactMechPurposeTypeId
}
if (cmNewPurposeTypeId) {
    context.contactMechPurposeTypeId = cmNewPurposeTypeId
    contactMechPurposeType = from("ContactMechPurposeType").where("contactMechPurposeTypeId", cmNewPurposeTypeId).queryOne()
    if (contactMechPurposeType) {
        context.contactMechPurposeType = contactMechPurposeType
    }
}
