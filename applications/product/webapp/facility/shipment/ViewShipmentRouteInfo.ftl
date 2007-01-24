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
<#if shipmentRouteSegmentDatas?has_content>
<br/>
<table width="100%" cellspacing="0" cellpadding="2" border="1">
    <tr>
        <td><div class="tableheadtext">${uiLabelMap.ProductSegment}</div></td>
        <td>
            <div class="tableheadtext">${uiLabelMap.ProductCarrierShipmentMethod}</div>
            <div class="tableheadtext">${uiLabelMap.ProductOriginDestinationFacility}</div>
            <div class="tableheadtext">${uiLabelMap.ProductOriginDestinationAddressId}</div>
            <div class="tableheadtext">${uiLabelMap.ProductOriginDestinationPhoneId}</div>
        </td>
        <td>
            <div class="tableheadtext">${uiLabelMap.ProductCarrierStatus}</div>
            <div class="tableheadtext">${uiLabelMap.ProductTrackingNumber}</div>
            <div class="tableheadtext">${uiLabelMap.ProductEstimatedStartArrive}</div>
            <div class="tableheadtext">${uiLabelMap.ProductActualStartArrive}</div>
        </td>
        <td>
            <div class="tableheadtext">${uiLabelMap.ProductBillingWeightUom}</div>
            <div class="tableheadtext">${uiLabelMap.ProductCurrencyUom}</div>
            <div class="tableheadtext">${uiLabelMap.ProductActualTransport}</div>
            <div class="tableheadtext">${uiLabelMap.ProductActualServices}</div>
            <div class="tableheadtext">${uiLabelMap.ProductActualOther}</div>
            <div class="tableheadtext">${uiLabelMap.ProductActualTotal}</div>
        </td>
    </tr>
<#list shipmentRouteSegmentDatas as shipmentRouteSegmentData>
    <#assign shipmentRouteSegment = shipmentRouteSegmentData.shipmentRouteSegment>
    <#assign shipmentPackageRouteSegs = shipmentRouteSegmentData.shipmentPackageRouteSegs?if_exists>
    <#assign originFacility = shipmentRouteSegmentData.originFacility?if_exists>
    <#assign destFacility = shipmentRouteSegmentData.destFacility?if_exists>
    <#assign shipmentMethodType = shipmentRouteSegmentData.shipmentMethodType?if_exists>
    <#assign carrierPerson = shipmentRouteSegmentData.carrierPerson?if_exists>
    <#assign carrierPartyGroup = shipmentRouteSegmentData.carrierPartyGroup?if_exists>
    <#assign originPostalAddress = shipmentRouteSegmentData.originPostalAddress?if_exists>
    <#assign destPostalAddress = shipmentRouteSegmentData.destPostalAddress?if_exists>
    <#assign originTelecomNumber = shipmentRouteSegmentData.originTelecomNumber?if_exists>
    <#assign destTelecomNumber = shipmentRouteSegmentData.destTelecomNumber?if_exists>
    <#assign carrierServiceStatusItem = shipmentRouteSegmentData.carrierServiceStatusItem?if_exists>
    <#assign currencyUom = shipmentRouteSegmentData.currencyUom?if_exists>
    <#assign billingWeightUom = shipmentRouteSegmentData.billingWeightUom?if_exists>
    <#assign carrierServiceStatusValidChangeToDetails = shipmentRouteSegmentData.carrierServiceStatusValidChangeToDetails?if_exists>
    <tr>
        <td><div class="tabletext">${shipmentRouteSegment.shipmentRouteSegmentId}</div></td>
        <td>
            <span class="tabletext">${(carrierPerson.firstName)?if_exists} ${(carrierPerson.middleName)?if_exists} ${(carrierPerson.lastName)?if_exists} ${(carrierPartyGroup.groupName)?if_exists} [${shipmentRouteSegment.carrierPartyId?if_exists}]</span>
            <span class="tabletext">${shipmentMethodType.description?default(shipmentRouteSegment.shipmentMethodTypeId?if_exists)}</span>
            <br/>
            <span class="tabletext">${uiLabelMap.ProductOrigin} : ${(originFacility.facilityName)?if_exists} [${originFacility.facilityId?if_exists}]</span>
            <span class="tabletext">${uiLabelMap.ProductDest} : ${(destFacility.facilityName)?if_exists} [${destFacility.facilityId?if_exists}]</span>
            <br/>
            <div class="tabletext">
                ${uiLabelMap.ProductOrigin} : <b>${shipmentRouteSegment.originContactMechId?if_exists}</b>
                <#if originPostalAddress?has_content>[${uiLabelMap.CommonTo} : ${originPostalAddress.toName?if_exists}, ${uiLabelMap.CommonAttn} : ${originPostalAddress.attnName?if_exists}, ${originPostalAddress.address1?if_exists}, ${originPostalAddress.address2?if_exists}, ${originPostalAddress.city?if_exists}, ${originPostalAddress.stateProvinceGeoId?if_exists}, ${originPostalAddress.postalCode?if_exists}, ${originPostalAddress.countryGeoId?if_exists}]</#if>
            </div>
            <div class="tabletext">
                ${uiLabelMap.ProductDest}: <b>${shipmentRouteSegment.destContactMechId?if_exists}</b>
                <#if destPostalAddress?has_content>[${uiLabelMap.CommonTo} : ${destPostalAddress.toName?if_exists}, ${uiLabelMap.CommonAttn} : ${destPostalAddress.attnName?if_exists}, ${destPostalAddress.address1?if_exists}, ${destPostalAddress.address2?if_exists}, ${destPostalAddress.city?if_exists}, ${destPostalAddress.stateProvinceGeoId?if_exists}, ${destPostalAddress.postalCode?if_exists}, ${destPostalAddress.countryGeoId?if_exists}]</#if>
            </div>
            <div class="tabletext">
                ${uiLabelMap.ProductOrigin} : <b>${shipmentRouteSegment.originTelecomNumberId?if_exists}</b>
                <#if originTelecomNumber?has_content>[${originTelecomNumber.countryCode?if_exists}  ${originTelecomNumber.areaCode?if_exists} ${originTelecomNumber.contactNumber?if_exists}]</#if>
            </div>
            <div class="tabletext">
                ${uiLabelMap.ProductDest} : <b>${shipmentRouteSegment.destTelecomNumberId?if_exists}</b>
                <#if destTelecomNumber?has_content>[${destTelecomNumber.countryCode?if_exists}  ${destTelecomNumber.areaCode?if_exists} ${destTelecomNumber.contactNumber?if_exists}]</#if>
            </div>
        </td>
        <td>
            
            <div class="tabletext">${(carrierServiceStatus.description)?default("&nbsp;")}</div>
            <div class="tabletext">${shipmentRouteSegment.trackingIdNumber?default("&nbsp;")}</div>
            <div class="tabletext">[${(shipmentRouteSegment.estimatedStartDate.toString())?if_exists} - ${(shipmentRouteSegment.estimatedArrivalDate.toString())?if_exists}]</span>
            <div class="tabletext">[${(shipmentRouteSegment.actualStartDate.toString())?if_exists} - ${(shipmentRouteSegment.actualArrivalDate.toString())?if_exists}]</span>
        </td>
        <td>
            <div class="tabletext">${shipmentRouteSegment.billingWeight?if_exists} ${(billingWeightUom.get("description",locale))?if_exists} [${(billingWeightUom.abbreviation)?if_exists}]</div>
            <div class="tabletext">${(currencyUom.get("description",locale))?default("&nbsp;")}</div>
            <div class="tabletext">${(shipmentRouteSegment.actualTransportCost)?default("&nbsp;")}</div>
            <div class="tabletext">${(shipmentRouteSegment.actualServiceCost)?default("&nbsp;")}</div>
            <div class="tabletext">${(shipmentRouteSegment.actualOtherCost)?default("&nbsp;")}</div>
            <div class="tabletext">${(shipmentRouteSegment.actualCost)?default("&nbsp;")}</div>
        </td>
    </tr>
    <#list shipmentPackageRouteSegs as shipmentPackageRouteSeg>
        <tr>
            <td><div class="tabletext">&nbsp;</div></td>
            <td><div class="tabletext">${uiLabelMap.ProductPackage} :${shipmentPackageRouteSeg.shipmentPackageSeqId}</div></td>
            <td><span class="tabletext">${uiLabelMap.ProductTracking} : ${shipmentPackageRouteSeg.trackingCode?if_exists}</span></td>
            <td><span class="tabletext">${uiLabelMap.ProductBox} : ${shipmentPackageRouteSeg.boxNumber?if_exists}</span></td>
        </tr>
    </#list>
</#list>
</table> 
</#if>
