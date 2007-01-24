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
    <table border="0" cellpadding="2" cellspacing="0">
      <tr>
        <td width="20%" align="right"><span class="tableheadtext">${uiLabelMap.ProductShipmentId}</span></td>
        <td><span class="tabletext">&nbsp;</span></td>
        <td width="80%" align="left"><span class="tabletext">${shipment.shipmentId}</span></td>
      </tr>    
      <tr>
        <td width="20%" align="right"><span class="tableheadtext">${uiLabelMap.ProductShipmentType}</span></td>
        <td><span class="tabletext">&nbsp;</span></td>
        <td width="80%" align="left"><span class="tabletext">${(shipmentType.get("description",locale))?default(shipment.shipmentTypeId?if_exists)}</span></td>
      </tr>
      <tr>
        <td width="20%" align="right"><span class="tableheadtext">${uiLabelMap.ProductStatus}</span></td>
        <td><span class="tabletext">&nbsp;</span></td>
        <td width="80%" align="left"><span class="tabletext">${(statusItem.get("description",locale))?default(shipment.statusId?if_exists)}</span></td>
      </tr>
      <tr>
        <td width="20%" align="right"><span class="tableheadtext">${uiLabelMap.ProductPrimaryOrderId}</span></td>
        <td><span class="tabletext">&nbsp;</span></td>
        <td width="80%" align="left"><span class="tabletext"><#if shipment.primaryOrderId?exists><a href="/ordermgr/control/orderview?orderId=${shipment.primaryOrderId}" class="buttontext">${shipment.primaryOrderId}</a></#if></span></td>
      </tr>
      <tr>
        <td width="20%" align="right"><span class="tableheadtext">${uiLabelMap.ProductPrimaryShipGroupSeqId}</span></td>
        <td><span class="tabletext">&nbsp;</span></td>
        <td width="80%" align="left"><span class="tabletext">${shipment.primaryShipGroupSeqId?if_exists}</span></td>
      </tr>
      <tr>
        <td width="20%" align="right"><span class="tableheadtext">${uiLabelMap.ProductEstimatedDates}</span></td>
        <td><span class="tabletext">&nbsp;</span></td>
        <td width="80%" align="left">
          <span class="tabletext">
            <span class="tableheadtext">${uiLabelMap.CommonReady}:&nbsp;</span>${(shipment.estimatedReadyDate.toString())?if_exists}
            <span class="tableheadtext">${uiLabelMap.ProductEstimatedShipDate}:&nbsp;</span>${(shipment.estimatedShipDate.toString())?if_exists}
            <span class="tableheadtext">${uiLabelMap.ProductArrival}:&nbsp;</span>${(shipment.estimatedArrivalDate.toString())?if_exists}
          </span>
        </td>
      </tr>
      <tr>
        <td width="20%" align="right"><span class="tableheadtext">${uiLabelMap.ProductLatestCancelDate}</span></td>
        <td><span class="tabletext">&nbsp;</span></td>
        <td width="80%" align="left"><span class="tabletext">${(shipment.latestCancelDate.toString())?if_exists}</span></td>
      </tr>
      <tr>
        <td width="20%" align="right"><span class="tableheadtext">${uiLabelMap.ProductEstimatedShipCost}</span></td>
        <td><span class="tabletext">&nbsp;</span></td>
        <td width="80%" align="left"><span class="tabletext">${(shipment.estimatedShipCost)?if_exists}</span></td>
      </tr>
      <tr>
        <td width="20%" align="right"><span class="tableheadtext">${uiLabelMap.ProductAdditionalShippingCharge}</span></td>
        <td><span class="tabletext">&nbsp;</span></td>
        <td width="80%" align="left">
            <#if shipment.additionalShippingCharge?exists>
                <span class="tabletext"><@ofbizCurrency amount=shipment.additionalShippingCharge isoCode=shipment.currencyUomId?if_exists /></span>
            </#if>
        </td>
      </tr>
      <tr>
        <td width="20%" align="right"><span class="tableheadtext">${uiLabelMap.ProductHandlingInstructions}</span></td>
        <td><span class="tabletext">&nbsp;</span></td>
        <td width="80%" align="left"><span class="tabletext">${shipment.handlingInstructions?if_exists}</span></td>
      </tr>
      <tr>
        <td width="20%" align="right"><span class="tableheadtext">${uiLabelMap.ProductFacilities}</span></td>
        <td><span class="tabletext">&nbsp;</span></td>
        <td width="80%" align="left">
          <div class="tabletext"><span class="tableheadtext">${uiLabelMap.ProductOrigin}:&nbsp;</span>${(originFacility.facilityName)?if_exists}&nbsp;[${(shipment.originFacilityId?if_exists)}]</div>
          <div class="tabletext"><span class="tableheadtext">${uiLabelMap.ProductDestination}:&nbsp;</span>${(destinationFacility.facilityName)?if_exists}&nbsp;[${(shipment.destinationFacilityId?if_exists)}]</div>
        </td>
      </tr>
      <tr>
        <td width="20%" align="right"><span class="tableheadtext">${uiLabelMap.PartyParties}</span></td>
        <td><span class="tabletext">&nbsp;</span></td>
        <td width="80%" align="left">
          <span class="tabletext">
            <span class="tableheadtext">${uiLabelMap.CommonTo}:&nbsp;</span>${(toPerson.firstName)?if_exists} ${(toPerson.middleName)?if_exists} ${(toPerson.lastName)?if_exists} ${(toPartyGroup.groupName)?if_exists} [${shipment.partyIdTo?if_exists}]
            <span class="tableheadtext">${uiLabelMap.CommonFrom}:&nbsp;</span>${(fromPerson.firstName)?if_exists} ${(fromPerson.middleName)?if_exists} ${(fromPerson.lastName)?if_exists} ${(fromPartyGroup.groupName)?if_exists} [${shipment.partyIdFrom?if_exists}]
          </span>
        </td>
      </tr>
      <tr>
        <td width="20%" align="right"><span class="tableheadtext">${uiLabelMap.ProductAddresses}</span></td>
        <td><span class="tabletext">&nbsp;</span></td>
        <td width="80%" align="left">
          <div class="tabletext"><span class="tableheadtext">${uiLabelMap.ProductOrigin}:&nbsp;</span>${shipment.originContactMechId?if_exists}&nbsp;<#if originPostalAddress?has_content>[${uiLabelMap.CommonTo} : ${originPostalAddress.toName?if_exists}, ${uiLabelMap.CommonAttn} : ${originPostalAddress.attnName?if_exists}, ${originPostalAddress.address1?if_exists}, ${originPostalAddress.address2?if_exists}, ${originPostalAddress.city?if_exists}, ${originPostalAddress.stateProvinceGeoId?if_exists}, ${originPostalAddress.postalCode?if_exists}, ${originPostalAddress.countryGeoId?if_exists}]</#if></div>
          <div class="tabletext"><span class="tableheadtext">${uiLabelMap.ProductDestination}:&nbsp;</span>${shipment.destinationContactMechId?if_exists}&nbsp;<#if destinationPostalAddress?has_content>[${uiLabelMap.CommonTo} : ${destinationPostalAddress.toName?if_exists}, ${uiLabelMap.CommonAttn} : ${destinationPostalAddress.attnName?if_exists}, ${destinationPostalAddress.address1?if_exists}, ${destinationPostalAddress.address2?if_exists}, ${destinationPostalAddress.city?if_exists}, ${destinationPostalAddress.stateProvinceGeoId?if_exists}, ${destinationPostalAddress.postalCode?if_exists}, ${destinationPostalAddress.countryGeoId?if_exists}]</#if></div>
        </td>
      </tr>
      <tr>
        <td width="20%" align="right"><span class="tableheadtext">${uiLabelMap.ProductPhoneNumbers}</span></td>
        <td><span class="tabletext">&nbsp;</span></td>
        <td width="80%" align="left">
          <div class="tabletext"><span class="tableheadtext">${uiLabelMap.ProductOrigin}:&nbsp;</span>${shipment.originTelecomNumberId?if_exists}&nbsp;<#if originTelecomNumber?has_content>[${originTelecomNumber.countryCode?if_exists}  ${originTelecomNumber.areaCode?if_exists} ${originTelecomNumber.contactNumber?if_exists}]</#if></div>
          <div class="tabletext"><span class="tableheadtext">${uiLabelMap.ProductDestination}:&nbsp;</span>${shipment.destinationTelecomNumberId?if_exists}&nbsp;<#if destinationTelecomNumber?has_content>[${destinationTelecomNumber.countryCode?if_exists}  ${destinationTelecomNumber.areaCode?if_exists} ${destinationTelecomNumber.contactNumber?if_exists}]</#if></div>
        </td>
      </tr>
      <tr>
        <td width="20%" align="right"><span class="tableheadtext">${uiLabelMap.CommonCreated}</span></td>
        <td><span class="tabletext">&nbsp;</span></td>
        <td width="80%" align="left">
          <div class="tabletext">${uiLabelMap.CommonBy} [${shipment.createdByUserLogin?if_exists}] ${uiLabelMap.CommonOn} ${(shipment.createdDate.toString())?if_exists}</div>
        </td>
      </tr>
      <tr>
        <td width="20%" align="right"><span class="tableheadtext">${uiLabelMap.CommonLastUpdated}</span></td>
        <td><span class="tabletext">&nbsp;</span></td>
        <td width="80%" align="left">
          <div class="tabletext">${uiLabelMap.CommonBy} [${shipment.lastModifiedByUserLogin?if_exists}] ${uiLabelMap.CommonOn} ${(shipment.lastModifiedDate.toString())?if_exists}</div>
        </td>
      </tr>
    </table>  
  </#if>
