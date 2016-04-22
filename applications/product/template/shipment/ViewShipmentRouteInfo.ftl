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
            <#assign shipmentPackageRouteSegs = shipmentRouteSegmentData.shipmentPackageRouteSegs!>
            <#assign originFacility = shipmentRouteSegmentData.originFacility!>
            <#assign destFacility = shipmentRouteSegmentData.destFacility!>
            <#assign shipmentMethodType = shipmentRouteSegmentData.shipmentMethodType!>
            <#assign carrierPerson = shipmentRouteSegmentData.carrierPerson!>
            <#assign carrierPartyGroup = shipmentRouteSegmentData.carrierPartyGroup!>
            <#assign originPostalAddress = shipmentRouteSegmentData.originPostalAddress!>
            <#assign destPostalAddress = shipmentRouteSegmentData.destPostalAddress!>
            <#assign originTelecomNumber = shipmentRouteSegmentData.originTelecomNumber!>
            <#assign destTelecomNumber = shipmentRouteSegmentData.destTelecomNumber!>
            <#assign carrierServiceStatusItem = shipmentRouteSegmentData.carrierServiceStatusItem!>
            <#assign currencyUom = shipmentRouteSegmentData.currencyUom!>
            <#assign billingWeightUom = shipmentRouteSegmentData.billingWeightUom!>
            <#assign carrierServiceStatusValidChangeToDetails = shipmentRouteSegmentData.carrierServiceStatusValidChangeToDetails!>
            <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
                <td>${shipmentRouteSegment.shipmentRouteSegmentId}</td>
                <td>
                    <span>${(carrierPerson.firstName)!} ${(carrierPerson.middleName)!} ${(carrierPerson.lastName)!} ${(carrierPartyGroup.groupName)!} [${shipmentRouteSegment.carrierPartyId!}]</span>
                    <span>${shipmentMethodType.description?default(shipmentRouteSegment.shipmentMethodTypeId!)}</span>
                    <br />
                    <span class="label">${uiLabelMap.ProductOrigin}</span>
                    <span>${(originFacility.facilityName)!} [${originFacility.facilityId!}]</span>
                    <br />
                    <span class="label">${uiLabelMap.ProductDest}</span>
                    <span>${(destFacility.facilityName)!} [${destFacility.facilityId!}]</span>
                    <br />
                    <span class="label">${uiLabelMap.ProductOrigin}</span>
                    <span><#if originPostalAddress?has_content>${uiLabelMap.CommonTo} : ${originPostalAddress.toName!}, ${uiLabelMap.CommonAttn} : ${originPostalAddress.attnName!}, ${originPostalAddress.address1!}, ${originPostalAddress.address2!}, ${originPostalAddress.city!}, ${originPostalAddress.stateProvinceGeoId!}, ${originPostalAddress.postalCode!}, ${originPostalAddress.countryGeoId!}</#if> [${shipmentRouteSegment.originContactMechId!}]</span>
                    <br />
                    <span class="label">${uiLabelMap.ProductDest}</span>
                    <span><#if destPostalAddress?has_content>${uiLabelMap.CommonTo} : ${destPostalAddress.toName!}, ${uiLabelMap.CommonAttn} : ${destPostalAddress.attnName!}, ${destPostalAddress.address1!}, ${destPostalAddress.address2!}, ${destPostalAddress.city!}, ${destPostalAddress.stateProvinceGeoId!}, ${destPostalAddress.postalCode!}, ${destPostalAddress.countryGeoId!}</#if> [${shipmentRouteSegment.destContactMechId!}]</span>
                    <br />
                    <span class="label">${uiLabelMap.ProductOrigin}</span>
                    <span><#if originTelecomNumber?has_content>${originTelecomNumber.countryCode!}  ${originTelecomNumber.areaCode!} ${originTelecomNumber.contactNumber!}</#if> [${shipmentRouteSegment.originTelecomNumberId!}]</span>
                    <br />
                    <span class="label">${uiLabelMap.ProductDest}</span>
                    <span><#if destTelecomNumber?has_content>${destTelecomNumber.countryCode!}  ${destTelecomNumber.areaCode!} ${destTelecomNumber.contactNumber!}</#if> [${shipmentRouteSegment.destTelecomNumberId!}]</span>
                    <br />
                </td>
                <td>
                    <div>${(carrierServiceStatus.description)?default("&nbsp;")}</div>
                    <div>${shipmentRouteSegment.trackingIdNumber?default("&nbsp;")}</div>
                    <div>${(shipmentRouteSegment.estimatedStartDate.toString())!} - ${(shipmentRouteSegment.estimatedArrivalDate.toString())!}</div>
                    <div>${(shipmentRouteSegment.actualStartDate.toString())!} - ${(shipmentRouteSegment.actualArrivalDate.toString())!}</div>
                </td>
                <td>
                    <div>${shipmentRouteSegment.billingWeight!} ${(billingWeightUom.get("description",locale))!} [${(billingWeightUom.abbreviation)!}]</div>
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
                <td><span class="label">${uiLabelMap.ProductTracking}</span> ${shipmentPackageRouteSeg.trackingCode!}</td>
                <td><span class="label">${uiLabelMap.ProductBox}</span> ${shipmentPackageRouteSeg.boxNumber!}</td>
            </tr>
            </#list>
            <#-- toggle the row color -->
            <#assign alt_row = !alt_row>
        </#list>
        </table>
    </div>
</div>
</#if>