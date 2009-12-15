<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
<#if shipment?exists>
<div class="screenlet">
    <div class="screenlet-title-bar">
        <ul>
            <li class="h3">${uiLabelMap.PageTitleViewShipment}</li>
        </ul>
        <br class="clear"/>
    </div>
    <div class="screenlet-body">
        <table border="0" cellpadding="2" cellspacing="0" class="basic-table">
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.ProductShipmentId}</td>
            <td width="80%">${shipment.shipmentId}</td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.ProductShipmentType}</td>
            <td width="80%">${(shipmentType.get("description",locale))?default(shipment.shipmentTypeId?if_exists)}</td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.CommonStatus}</td>
            <td width="80%">${(statusItem.get("description",locale))?default(shipment.statusId?if_exists)}</td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.ProductPrimaryOrderId}</td>
            <td width="80%"><#if shipment.primaryOrderId?exists><a href="/ordermgr/control/orderview?orderId=${shipment.primaryOrderId}" class="buttontext">${shipment.primaryOrderId}</a></#if></td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.ProductPrimaryReturnId}</td>
            <td width="80%"><#if shipment.primaryReturnId?exists><a href="/ordermgr/control/returnMain?returnId=${shipment.primaryReturnId}" class="buttontext">${shipment.primaryReturnId}</a></#if></td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.ProductPrimaryShipGroupSeqId}</td>
            <td width="80%">${shipment.primaryShipGroupSeqId?if_exists}</td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.ProductEstimatedDates}</td>
            <td width="80%">
              <span>
                <span>${uiLabelMap.CommonReady}:&nbsp;</span>${(shipment.estimatedReadyDate.toString())?if_exists}
                <span>${uiLabelMap.ProductEstimatedShipDate}:&nbsp;</span>${(shipment.estimatedShipDate.toString())?if_exists}
                <span>${uiLabelMap.ProductArrival}:&nbsp;</span>${(shipment.estimatedArrivalDate.toString())?if_exists}
              </span>
            </td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.ProductLatestCancelDate}</td>
            <td width="80%">${(shipment.latestCancelDate.toString())?if_exists}</td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.ProductEstimatedShipCost}</td>
            <td width="80%">${(shipment.estimatedShipCost)?if_exists}</td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.ProductAdditionalShippingCharge}</td>
            <td width="80%">
                <#if shipment.additionalShippingCharge?exists>
                    <@ofbizCurrency amount=shipment.additionalShippingCharge isoCode=shipment.currencyUomId?if_exists />
                </#if>
            </td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.ProductHandlingInstructions}</td>
            <td width="80%">${shipment.handlingInstructions?if_exists}</td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.ProductFacilities}</td>
            <td width="80%">
              <div>${uiLabelMap.ProductOrigin}:&nbsp;${(originFacility.facilityName)?if_exists}&nbsp;[${(shipment.originFacilityId?if_exists)}]</div>
              <div>${uiLabelMap.ProductDestination}:&nbsp;${(destinationFacility.facilityName)?if_exists}&nbsp;[${(shipment.destinationFacilityId?if_exists)}]</div>
            </td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.PartyParties}</td>
            <td width="80%">
              <span>
                <span>${uiLabelMap.CommonTo}:&nbsp;${(toPerson.firstName)?if_exists} ${(toPerson.middleName)?if_exists} ${(toPerson.lastName)?if_exists} ${(toPartyGroup.groupName)?if_exists} [${shipment.partyIdTo?if_exists}]
                <span>${uiLabelMap.CommonFrom}:&nbsp;${(fromPerson.firstName)?if_exists} ${(fromPerson.middleName)?if_exists} ${(fromPerson.lastName)?if_exists} ${(fromPartyGroup.groupName)?if_exists} [${shipment.partyIdFrom?if_exists}]
              </span>
            </td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.ProductAddresses}</td>
            <td width="80%">
              <div>${uiLabelMap.ProductOrigin}:&nbsp;${shipment.originContactMechId?if_exists}&nbsp;<#if originPostalAddress?has_content>[${uiLabelMap.CommonTo} : ${originPostalAddress.toName?if_exists}, ${uiLabelMap.CommonAttn} : ${originPostalAddress.attnName?if_exists}, ${originPostalAddress.address1?if_exists}, ${originPostalAddress.address2?if_exists}, ${originPostalAddress.city?if_exists}, ${originPostalAddress.stateProvinceGeoId?if_exists}, ${originPostalAddress.postalCode?if_exists}, ${originPostalAddress.countryGeoId?if_exists}]</#if></div>
              <div>${uiLabelMap.ProductDestination}:&nbsp;${shipment.destinationContactMechId?if_exists}&nbsp;<#if destinationPostalAddress?has_content>[${uiLabelMap.CommonTo} : ${destinationPostalAddress.toName?if_exists}, ${uiLabelMap.CommonAttn} : ${destinationPostalAddress.attnName?if_exists}, ${destinationPostalAddress.address1?if_exists}, ${destinationPostalAddress.address2?if_exists}, ${destinationPostalAddress.city?if_exists}, ${destinationPostalAddress.stateProvinceGeoId?if_exists}, ${destinationPostalAddress.postalCode?if_exists}, ${destinationPostalAddress.countryGeoId?if_exists}]</#if></div>
            </td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.ProductPhoneNumbers}</td>
            <td width="80%">
              <div>${uiLabelMap.ProductOrigin}:&nbsp;${shipment.originTelecomNumberId?if_exists}&nbsp;<#if originTelecomNumber?has_content>[${originTelecomNumber.countryCode?if_exists}  ${originTelecomNumber.areaCode?if_exists} ${originTelecomNumber.contactNumber?if_exists}]</#if></div>
              <div>${uiLabelMap.ProductDestination}:&nbsp;${shipment.destinationTelecomNumberId?if_exists}&nbsp;<#if destinationTelecomNumber?has_content>[${destinationTelecomNumber.countryCode?if_exists}  ${destinationTelecomNumber.areaCode?if_exists} ${destinationTelecomNumber.contactNumber?if_exists}]</#if></div>
            </td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.CommonCreated}</td>
            <td width="80%">
              <div>${uiLabelMap.CommonBy} [${shipment.createdByUserLogin?if_exists}] ${uiLabelMap.CommonOn} ${(shipment.createdDate.toString())?if_exists}</div>
            </td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.CommonLastUpdated}</td>
            <td width="80%">
              <div>${uiLabelMap.CommonBy} [${shipment.lastModifiedByUserLogin?if_exists}] ${uiLabelMap.CommonOn} ${(shipment.lastModifiedDate.toString())?if_exists}</div>
            </td>
          </tr>
        </table>
    </div>
</div>
</#if>