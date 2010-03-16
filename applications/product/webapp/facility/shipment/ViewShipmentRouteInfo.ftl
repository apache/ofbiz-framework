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
<div class="screenlet">
    <div class="screenlet-body">
        <table cellspacing="0" cellpadding="2"  class="basic-table">
            <tr class="header-row">
                <td>${uiLabelMap.ProductSegment}</td>
                <td>
                    <div>${uiLabelMap.ProductCarrierShipmentMethod}</div>
                    <div>${uiLabelMap.ProductOriginDestinationFacility}</div>
                    <div>${uiLabelMap.ProductOriginDestinationAddressId}</div>
                    <div>${uiLabelMap.ProductOriginDestinationPhoneId}</div>
                </td>
                <td>
                    <div>${uiLabelMap.ProductCarrierStatus}</div>
                    <div>${uiLabelMap.ProductTrackingNumber}</div>
                    <div>${uiLabelMap.ProductEstimatedStartArrive}</div>
                    <div>${uiLabelMap.ProductActualStartArrive}</div>
                </td>
                <td>
                    <div>${uiLabelMap.ProductBillingWeightUom}</div>
                    <div>${uiLabelMap.ProductCurrencyUom}</div>
                    <div>${uiLabelMap.ProductActualTransport}</div>
                    <div>${uiLabelMap.ProductActualServices}</div>
                    <div>${uiLabelMap.ProductActualOther}</div>
                    <div>${uiLabelMap.ProductActualTotal}</div>
                </td>
            </tr>
        <#assign alt_row = false>
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
            <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
                <td>${shipmentRouteSegment.shipmentRouteSegmentId}</td>
                <td>
                    <span>${(carrierPerson.firstName)?if_exists} ${(carrierPerson.middleName)?if_exists} ${(carrierPerson.lastName)?if_exists} ${(carrierPartyGroup.groupName)?if_exists} [${shipmentRouteSegment.carrierPartyId?if_exists}]</span>
                    <span>${shipmentMethodType.description?default(shipmentRouteSegment.shipmentMethodTypeId?if_exists)}</span>
                    <br />
                    <span class="label">${uiLabelMap.ProductOrigin}</span>
                    <span>${(originFacility.facilityName)?if_exists} [${originFacility.facilityId?if_exists}]</span>
                    <br />
                    <span class="label">${uiLabelMap.ProductDest}</span>
                    <span>${(destFacility.facilityName)?if_exists} [${destFacility.facilityId?if_exists}]</span>
                    <br />
                    <span class="label">${uiLabelMap.ProductOrigin}</span>
                    <span><#if originPostalAddress?has_content>${uiLabelMap.CommonTo} : ${originPostalAddress.toName?if_exists}, ${uiLabelMap.CommonAttn} : ${originPostalAddress.attnName?if_exists}, ${originPostalAddress.address1?if_exists}, ${originPostalAddress.address2?if_exists}, ${originPostalAddress.city?if_exists}, ${originPostalAddress.stateProvinceGeoId?if_exists}, ${originPostalAddress.postalCode?if_exists}, ${originPostalAddress.countryGeoId?if_exists}</#if> [${shipmentRouteSegment.originContactMechId?if_exists}]</span>
                    <br />
                    <span class="label">${uiLabelMap.ProductDest}</span>
                    <span><#if destPostalAddress?has_content>${uiLabelMap.CommonTo} : ${destPostalAddress.toName?if_exists}, ${uiLabelMap.CommonAttn} : ${destPostalAddress.attnName?if_exists}, ${destPostalAddress.address1?if_exists}, ${destPostalAddress.address2?if_exists}, ${destPostalAddress.city?if_exists}, ${destPostalAddress.stateProvinceGeoId?if_exists}, ${destPostalAddress.postalCode?if_exists}, ${destPostalAddress.countryGeoId?if_exists}</#if> [${shipmentRouteSegment.destContactMechId?if_exists}]</span>
                    <br />
                    <span class="label">${uiLabelMap.ProductOrigin}</span>
                    <span><#if originTelecomNumber?has_content>${originTelecomNumber.countryCode?if_exists}  ${originTelecomNumber.areaCode?if_exists} ${originTelecomNumber.contactNumber?if_exists}</#if> [${shipmentRouteSegment.originTelecomNumberId?if_exists}]</span>
                    <br />
                    <span class="label">${uiLabelMap.ProductDest}</span>
                    <span><#if destTelecomNumber?has_content>${destTelecomNumber.countryCode?if_exists}  ${destTelecomNumber.areaCode?if_exists} ${destTelecomNumber.contactNumber?if_exists}</#if> [${shipmentRouteSegment.destTelecomNumberId?if_exists}]</span>
                    <br />
                </td>
                <td>
                    <div>${(carrierServiceStatus.description)?default("&nbsp;")}</div>
                    <div>${shipmentRouteSegment.trackingIdNumber?default("&nbsp;")}</div>
                    <div>${(shipmentRouteSegment.estimatedStartDate.toString())?if_exists} - ${(shipmentRouteSegment.estimatedArrivalDate.toString())?if_exists}</div>
                    <div>${(shipmentRouteSegment.actualStartDate.toString())?if_exists} - ${(shipmentRouteSegment.actualArrivalDate.toString())?if_exists}</div>
                </td>
                <td>
                    <div>${shipmentRouteSegment.billingWeight?if_exists} ${(billingWeightUom.get("description",locale))?if_exists} [${(billingWeightUom.abbreviation)?if_exists}]</div>
                    <div>${(currencyUom.get("description",locale))?default("&nbsp;")}</div>
                    <div>${(shipmentRouteSegment.actualTransportCost)?default("&nbsp;")}</div>
                    <div>${(shipmentRouteSegment.actualServiceCost)?default("&nbsp;")}</div>
                    <div>${(shipmentRouteSegment.actualOtherCost)?default("&nbsp;")}</div>
                    <div>${(shipmentRouteSegment.actualCost)?default("&nbsp;")}</div>
                </td>
            </tr>
            <#list shipmentPackageRouteSegs as shipmentPackageRouteSeg>
            <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
                <td>&nbsp;</td>
                <td><span class="label">${uiLabelMap.ProductPackage}</span> ${shipmentPackageRouteSeg.shipmentPackageSeqId}</td>
                <td><span class="label">${uiLabelMap.ProductTracking}</span> ${shipmentPackageRouteSeg.trackingCode?if_exists}</td>
                <td><span class="label">${uiLabelMap.ProductBox}</span> ${shipmentPackageRouteSeg.boxNumber?if_exists}</td>
            </tr>
            </#list>
            <#-- toggle the row color -->
            <#assign alt_row = !alt_row>
        </#list>
        </table>
    </div>
</div>
</#if>