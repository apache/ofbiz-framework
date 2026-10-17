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
package org.apache.ofbiz.product.facility.facility

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.party.contact.ContactMechWorker

context.nowStr = UtilDateTime.nowTimestamp()

facilityId = parameters.facilityId
facility = from('Facility').where('facilityId', facilityId).queryOne()
facilityType = null
if (facility) {
    facilityType = facility.getRelatedOne('FacilityType', false)
} else {
    context.facility = makeValue('Facility', null)
    context.facilityType = makeValue('FacilityType', null)
}
context.facility = facility
context.facilityType = facilityType
context.facilityId = facilityId

showOld = request.getParameter('SHOW_OLD') == 'true'
context.showOld = Boolean.valueOf(showOld)

context.contactMeches = ContactMechWorker.getFacilityContactMechValueMaps(delegator, facilityId, showOld, null)
