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
<#if shipment??>
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
            <td width="80%">${(shipmentType.get("description",locale))?default(shipment.shipmentTypeId!)}</td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.CommonStatus}</td>
            <td width="80%">${(statusItem.get("description",locale))?default(shipment.statusId!)}</td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.ProductPrimaryOrderId}</td>
            <td width="80%"><#if shipment.primaryOrderId??><a href="<@ofbizUrl controlPath="/ordermgr/control">orderview?orderId=${shipment.primaryOrderId}</@ofbizUrl>" class="buttontext">${shipment.primaryOrderId}</a></#if></td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.ProductPrimaryReturnId}</td>
            <td width="80%"><#if shipment.primaryReturnId??><a href="<@ofbizUrl controlPath="/ordermgr/control">returnMain?returnId=${shipment.primaryReturnId}</@ofbizUrl>" class="buttontext">${shipment.primaryReturnId}</a></#if></td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.ProductPrimaryShipGroupSeqId}</td>
            <td width="80%">${shipment.primaryShipGroupSeqId!}</td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.ProductEstimatedDates}</td>
            <td width="80%">
              <span>
                <span>${uiLabelMap.CommonReady}:&nbsp;</span>${(shipment.estimatedReadyDate.toString())!}
                <span>${uiLabelMap.ProductEstimatedShipDate}:&nbsp;</span>${(shipment.estimatedShipDate.toString())!}
                <span>${uiLabelMap.ProductArrival}:&nbsp;</span>${(shipment.estimatedArrivalDate.toString())!}
              </span>
            </td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.ProductLatestCancelDate}</td>
            <td width="80%">${(shipment.latestCancelDate.toString())!}</td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.ProductEstimatedShipCost}</td>
            <td width="80%">${(shipment.estimatedShipCost)!}</td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.ProductAdditionalShippingCharge}</td>
            <td width="80%">
                <#if shipment.additionalShippingCharge??>
                    <@ofbizCurrency amount=shipment.additionalShippingCharge isoCode=shipment.currencyUomId! />
                </#if>
            </td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.ProductHandlingInstructions}</td>
            <td width="80%">${shipment.handlingInstructions!}</td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.ProductFacilities}</td>
            <td width="80%">
              <div>${uiLabelMap.ProductOrigin}:&nbsp;${(originFacility.facilityName)!}&nbsp;[${(shipment.originFacilityId!)}]</div>
              <div>${uiLabelMap.ProductDestination}:&nbsp;${(destinationFacility.facilityName)!}&nbsp;[${(shipment.destinationFacilityId!)}]</div>
            </td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.PartyParties}</td>
            <td width="80%">
              <span>
                <span>${uiLabelMap.CommonTo}:&nbsp;${(toPerson.firstName)!} ${(toPerson.middleName)!} ${(toPerson.lastName)!} ${(toPartyGroup.groupName)!} [${shipment.partyIdTo!}]</span>
                <span>${uiLabelMap.CommonFrom}:&nbsp;${(fromPerson.firstName)!} ${(fromPerson.middleName)!} ${(fromPerson.lastName)!} ${(fromPartyGroup.groupName)!} [${shipment.partyIdFrom!}]</span>
              </span>
            </td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.ProductAddresses}</td>
            <td width="80%">
              <div>${uiLabelMap.ProductOrigin}:&nbsp;${shipment.originContactMechId!}&nbsp;<#if originPostalAddress?has_content>[${uiLabelMap.CommonTo} : ${originPostalAddress.toName!}, ${uiLabelMap.CommonAttn} : ${originPostalAddress.attnName!}, ${originPostalAddress.address1!}, ${originPostalAddress.address2!}, ${originPostalAddress.city!}, ${originPostalAddress.stateProvinceGeoId!}, ${originPostalAddress.postalCode!}, ${originPostalAddress.countryGeoId!}]</#if></div>
              <div>${uiLabelMap.ProductDestination}:&nbsp;${shipment.destinationContactMechId!}&nbsp;<#if destinationPostalAddress?has_content>[${uiLabelMap.CommonTo} : ${destinationPostalAddress.toName!}, ${uiLabelMap.CommonAttn} : ${destinationPostalAddress.attnName!}, ${destinationPostalAddress.address1!}, ${destinationPostalAddress.address2!}, ${destinationPostalAddress.city!}, ${destinationPostalAddress.stateProvinceGeoId!}, ${destinationPostalAddress.postalCode!}, ${destinationPostalAddress.countryGeoId!}]</#if></div>
            </td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.ProductPhoneNumbers}</td>
            <td width="80%">
              <div>${uiLabelMap.ProductOrigin}:&nbsp;${shipment.originTelecomNumberId!}&nbsp;<#if originTelecomNumber?has_content>[${originTelecomNumber.countryCode!}  ${originTelecomNumber.areaCode!} ${originTelecomNumber.contactNumber!}]</#if></div>
              <div>${uiLabelMap.ProductDestination}:&nbsp;${shipment.destinationTelecomNumberId!}&nbsp;<#if destinationTelecomNumber?has_content>[${destinationTelecomNumber.countryCode!}  ${destinationTelecomNumber.areaCode!} ${destinationTelecomNumber.contactNumber!}]</#if></div>
            </td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.CommonCreated}</td>
            <td width="80%">
              <div>${uiLabelMap.CommonBy} [${shipment.createdByUserLogin!}] ${uiLabelMap.CommonOn} ${(shipment.createdDate.toString())!}</div>
            </td>
          </tr>
          <tr>
            <td width="20%" align="right" class="label">${uiLabelMap.CommonLastUpdated}</td>
            <td width="80%">
              <div>${uiLabelMap.CommonBy} [${shipment.lastModifiedByUserLogin!}] ${uiLabelMap.CommonOn} ${(shipment.lastModifiedDate.toString())!}</div>
            </td>
          </tr>
        </table>
    </div>
</div>
</#if>