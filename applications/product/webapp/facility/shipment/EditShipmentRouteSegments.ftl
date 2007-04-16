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
<table width="100%" cellspacing="0" cellpadding="2" border="1">
    <tr>
        <td><div class="tableheadtext">${uiLabelMap.ProductSegment}</div></td>
        <td>
            <div class="tableheadtext">${uiLabelMap.ProductCarrierShipmentMethod}</div>
            <div class="tableheadtext">${uiLabelMap.ProductOriginDestinationFacility}</div>
            <div class="tableheadtext">${uiLabelMap.ProductOriginDestinationAddressId}</div>
            <div class="tableheadtext">${uiLabelMap.ProductOriginDestinationPhoneId}</div>
            <div class="tableheadtext">${uiLabelMap.ProductShipmentThirdPartyAccountNumber}</div>
            <div class="tableheadtext">${uiLabelMap.ProductShipmentThirdPartyAddress}</div>
        </td>
        <td>
            <div class="tableheadtext">${uiLabelMap.ProductShipmentFedexHomeDeliveryTypeDate}</div>
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
        <td><div class="tableheadtext">&nbsp;</div></td>
        <td><div class="tableheadtext">&nbsp;</div></td>
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
    <#assign thirdPartyPostalAddress = shipmentRouteSegmentData.thirdPartyPostalAddress?if_exists>
    <form action="<@ofbizUrl>updateShipmentRouteSegment</@ofbizUrl>" name="updateShipmentRouteSegmentForm${shipmentRouteSegmentData_index}">
    <input type="hidden" name="shipmentId" value="${shipmentId}"/>
    <input type="hidden" name="shipmentRouteSegmentId" value="${shipmentRouteSegment.shipmentRouteSegmentId}"/>
    <tr>
        <td><div class="tabletext">${shipmentRouteSegment.shipmentRouteSegmentId}</div></td>
        <td><span class="tabletext"></span>
            <select name="carrierPartyId" class="selectBox">
                <#if shipmentRouteSegment.carrierPartyId?has_content>
                    <option value="${shipmentRouteSegment.carrierPartyId}">${(carrierPerson.firstName)?if_exists} ${(carrierPerson.middleName)?if_exists} ${(carrierPerson.lastName)?if_exists} ${(carrierPartyGroup.groupName)?if_exists} [${shipmentRouteSegment.carrierPartyId}]</option>
                    <option value="${shipmentRouteSegment.carrierPartyId}">---</option>
                <#else>
                    <option value="">&nbsp;</option>
                </#if>
                <#list carrierPartyDatas as carrierPartyData>
                    <option value="${carrierPartyData.party.partyId}">${(carrierPartyData.person.firstName)?if_exists} ${(carrierPartyData.person.middleName)?if_exists} ${(carrierPartyData.person.lastName)?if_exists} ${(carrierPartyData.partyGroup.groupName)?if_exists} [${carrierPartyData.party.partyId}]</option>
                </#list>
            </select>
            <select name="shipmentMethodTypeId" class="selectBox">
                <#if shipmentMethodType?has_content>
                    <option value="${shipmentMethodType.shipmentMethodTypeId}">${shipmentMethodType.get("description",locale)}</option>
                    <option value="${shipmentMethodType.shipmentMethodTypeId}">---</option>
                <#else>
                    <option value="">&nbsp;</option>
                </#if>
                <#list shipmentMethodTypes as shipmentMethodTypeOption>
                    <option value="${shipmentMethodTypeOption.shipmentMethodTypeId}">${shipmentMethodTypeOption.get("description",locale)}</option>
                </#list>
            </select>
            <br/>
            <select name="originFacilityId" class="selectBox">
                <#if originFacility?has_content>
                    <option value="${originFacility.facilityId}">${originFacility.facilityName} [${originFacility.facilityId}]</option>
                    <option value="${originFacility.facilityId}">---</option>
                <#else>
                    <option value="">&nbsp;</option>
                </#if>
                <#list facilities as facility>
                    <option value="${facility.facilityId}">${facility.facilityName} [${facility.facilityId}]</option>
                </#list>
            </select>
            <select name="destFacilityId" class="selectBox">
                <#if destFacility?has_content>
                    <option value="${destFacility.facilityId}">${destFacility.facilityName} [${destFacility.facilityId}]</option>
                    <option value="${destFacility.facilityId}">---</option>
                <#else>
                    <option value="">&nbsp;</option>
                </#if>
                <#list facilities as facility>
                    <option value="${facility.facilityId}">${facility.facilityName} [${facility.facilityId}]</option>
                </#list>
            </select>
            <br/>
            <div class="tabletext">
                <input type="text" size="15" name="originContactMechId" value="${shipmentRouteSegment.originContactMechId?if_exists}" class="inputBox"/>
                <#if originPostalAddress?has_content>[${uiLabelMap.CommonTo}: ${originPostalAddress.toName?if_exists}, ${uiLabelMap.CommonAttn}: ${originPostalAddress.attnName?if_exists}, ${originPostalAddress.address1?if_exists}, ${originPostalAddress.address2?if_exists}, ${originPostalAddress.city?if_exists}, ${originPostalAddress.stateProvinceGeoId?if_exists}, ${originPostalAddress.postalCode?if_exists}, ${originPostalAddress.countryGeoId?if_exists}]</#if>
            </div>
            <div class="tabletext">
                <input type="text" size="15" name="destContactMechId" value="${shipmentRouteSegment.destContactMechId?if_exists}" class="inputBox"/>
                <#if destPostalAddress?has_content>[${uiLabelMap.CommonTo}: ${destPostalAddress.toName?if_exists},${uiLabelMap.CommonAttn}: ${destPostalAddress.attnName?if_exists}, ${destPostalAddress.address1?if_exists}, ${destPostalAddress.address2?if_exists}, ${destPostalAddress.city?if_exists}, ${destPostalAddress.stateProvinceGeoId?if_exists}, ${destPostalAddress.postalCode?if_exists}, ${destPostalAddress.countryGeoId?if_exists}]</#if>
            </div>
            <div class="tabletext">
                <input type="text" size="15" name="originTelecomNumberId" value="${shipmentRouteSegment.originTelecomNumberId?if_exists}" class="inputBox"/>
                <#if originTelecomNumber?has_content>[${originTelecomNumber.countryCode?if_exists}  ${originTelecomNumber.areaCode?if_exists} ${originTelecomNumber.contactNumber?if_exists}]</#if>
            </div>
            <div class="tabletext">
                <input type="text" size="15" name="destTelecomNumberId" value="${shipmentRouteSegment.destTelecomNumberId?if_exists}" class="inputBox"/>
                <#if destTelecomNumber?has_content>[${destTelecomNumber.countryCode?if_exists}  ${destTelecomNumber.areaCode?if_exists} ${destTelecomNumber.contactNumber?if_exists}]</#if>
            </div>
            <div class="tabletext">
                <input type="text" size="15" name="thirdPartyAccountNumber" value="${shipmentRouteSegment.thirdPartyAccountNumber?if_exists}" class="inputBox"/>
            </div>
            <div class="tabletext">
                <input type="text" size="15" name="thirdPartyContactMechId" value="${shipmentRouteSegment.thirdPartyContactMechId?if_exists}" class="inputBox"/>
                <#if thirdPartyPostalAddress?has_content>[${uiLabelMap.CommonTo}: ${thirdPartyPostalAddress.toName?if_exists}, ${uiLabelMap.CommonAttn}: ${thirdPartyPostalAddress.attnName?if_exists}, ${thirdPartyPostalAddress.address1?if_exists}, ${thirdPartyPostalAddress.address2?if_exists}, ${thirdPartyPostalAddress.city?if_exists}, ${thirdPartyPostalAddress.stateProvinceGeoId?if_exists}, ${thirdPartyPostalAddress.postalCode?if_exists}, ${thirdPartyPostalAddress.countryGeoId?if_exists}]</#if>
            </div>
        </td>
        <td>
            <#if "UPS" == shipmentRouteSegment.carrierPartyId?if_exists>
                <#if !shipmentRouteSegment.carrierServiceStatusId?has_content || "SHRSCS_NOT_STARTED" == shipmentRouteSegment.carrierServiceStatusId?if_exists>
                    <a href="<@ofbizUrl>upsShipmentConfirm?shipmentId=${shipmentRouteSegment.shipmentId}&shipmentRouteSegmentId=${shipmentRouteSegment.shipmentRouteSegmentId}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductConfirmShipmentUps}</a>
                    <br/>
                    ${uiLabelMap.ProductShipmentUpsResidential}:
                    <input type="checkbox" name="homeDeliveryType" class="checkBox" value="Y" ${(shipmentRouteSegment.homeDeliveryType?has_content)?string("checked=\"checked\"","")}>
                <#elseif "SHRSCS_CONFIRMED" == shipmentRouteSegment.carrierServiceStatusId?if_exists>
                    <a href="<@ofbizUrl>upsShipmentAccept?shipmentId=${shipmentRouteSegment.shipmentId}&shipmentRouteSegmentId=${shipmentRouteSegment.shipmentRouteSegmentId}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductAcceptUpsShipmentConfirmation}</a>
                    <br/>
                    <a href="<@ofbizUrl>upsVoidShipment?shipmentId=${shipmentRouteSegment.shipmentId}&shipmentRouteSegmentId=${shipmentRouteSegment.shipmentRouteSegmentId}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductVoidUpsShipmentConfirmation}</a>
                <#elseif "SHRSCS_ACCEPTED" == shipmentRouteSegment.carrierServiceStatusId?if_exists>
                    <a href="<@ofbizUrl>upsTrackShipment?shipmentId=${shipmentRouteSegment.shipmentId}&shipmentRouteSegmentId=${shipmentRouteSegment.shipmentRouteSegmentId}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductTrackUpsShipment}</a>
                    <br/>
                    <a href="<@ofbizUrl>upsVoidShipment?shipmentId=${shipmentRouteSegment.shipmentId}&shipmentRouteSegmentId=${shipmentRouteSegment.shipmentRouteSegmentId}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductVoidUpsShipment}</a>
                </#if>
            </#if>
            <#if "DHL" == shipmentRouteSegment.carrierPartyId?if_exists>
                <#if !shipmentRouteSegment.carrierServiceStatusId?has_content || "SHRSCS_NOT_STARTED" == shipmentRouteSegment.carrierServiceStatusId?if_exists>
                    <a href="<@ofbizUrl>dhlShipmentConfirm?shipmentId=${shipmentRouteSegment.shipmentId}&shipmentRouteSegmentId=${shipmentRouteSegment.shipmentRouteSegmentId}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductConfirmShipmentDHL}</a>
                </#if>
            </#if>
            <#if "FEDEX" == shipmentRouteSegment.carrierPartyId?if_exists>
                <#if !shipmentRouteSegment.carrierServiceStatusId?has_content || "SHRSCS_NOT_STARTED" == shipmentRouteSegment.carrierServiceStatusId?if_exists>
                    <a href="<@ofbizUrl>fedexShipmentConfirm?shipmentId=${shipmentRouteSegment.shipmentId}&shipmentRouteSegmentId=${shipmentRouteSegment.shipmentRouteSegmentId}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductConfirmShipmentFedex}</a>
                    <br/>
                    <#if shipmentMethodType?exists && shipmentMethodType.shipmentMethodTypeId=="GROUND_HOME">
                        <select name="homeDeliveryType" class="selectBox">
                            <option value="">${uiLabelMap.ProductShipmentNone}</option>
                            <option ${(shipmentRouteSegment.homeDeliveryType?default("")=="DATECERTAIN")?string("selected=\"selected\"","")} value="DATECERTAIN">${uiLabelMap.ProductShipmentFedexHomeDateCertain}</option>
                            <option ${(shipmentRouteSegment.homeDeliveryType?default("")=="EVENING")?string("selected=\"selected\"","")} value="EVENING">${uiLabelMap.ProductShipmentFedexHomeEvening}</option>
                            <option ${(shipmentRouteSegment.homeDeliveryType?default("")=="APPOINTMENT")?string("selected=\"selected\"","")} value="APPOINTMENT">${uiLabelMap.ProductShipmentFedexHomeAppointment}</option>
                        </select>
                        <input type="text" size="25" name="homeDeliveryDate" value="${(shipmentRouteSegment.homeDeliveryDate.toString())?if_exists}" class="inputBox"/><a href="javascript:call_cal(document.updateShipmentRouteSegmentForm${shipmentRouteSegmentData_index}.homeDeliveryDate, '${(shipmentRouteSegment.homeDeliveryDate.toString())?default(nowTimestampString)}');"><img src='<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>' width='16' height='16' border='0' alt='Calendar'></a>
                    </#if>
                <#else>
                    <#-- Todo: implement closeout with Fedex -->
                    <#-- Todo: implement shipment cancellation with Fedex -->
                    <#-- Todo: implement shipment tracking with Fedex -->
                    ${shipmentRouteSegment.homeDeliveryType?default(uiLabelMap.ProductShipmentNone)}
                    <#if shipmentRouteSegment.homeDeliveryDate?exists>
                        &nbsp;(${shipmentRouteSegment.homeDeliveryDate?string("yyyy-MM-dd")})
                    </#if>
                    <br/>
                </#if>
            </#if>
           <br/>
           <select name="carrierServiceStatusId" class="selectBox">
                <#if carrierServiceStatusItem?has_content>
                    <option value="${carrierServiceStatusItem.statusId}">${carrierServiceStatusItem.description}</option>
                    <option value="${carrierServiceStatusItem.statusId}">---</option>
                <#else>
                    <option value="">&nbsp;</option>
                </#if>
                <#list carrierServiceStatusValidChangeToDetails as carrierServiceStatusValidChangeToDetail>
                    <option value="${carrierServiceStatusValidChangeToDetail.statusIdTo}">${carrierServiceStatusValidChangeToDetail.transitionName} [${carrierServiceStatusValidChangeToDetail.description}]</option>
                </#list>
            </select>
            <br/>
            <input type="text" size="24" name="trackingIdNumber" value="${shipmentRouteSegment.trackingIdNumber?if_exists}" class="inputBox"/>
            <br/>
            <input type="text" size="25" name="estimatedStartDate" value="${(shipmentRouteSegment.estimatedStartDate.toString())?if_exists}" class="inputBox"/><a href="javascript:call_cal(document.updateShipmentRouteSegmentForm${shipmentRouteSegmentData_index}.estimatedStartDate, '${(shipmentRouteSegment.estimatedStartDate.toString())?default(nowTimestampString)}');"><img src='<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>' width='16' height='16' border='0' alt='Calendar'></a>
            <input type="text" size="25" name="estimatedArrivalDate" value="${(shipmentRouteSegment.estimatedArrivalDate.toString())?if_exists}" class="inputBox"/><a href="javascript:call_cal(document.updateShipmentRouteSegmentForm${shipmentRouteSegmentData_index}.estimatedArrivalDate, '${(shipmentRouteSegment.estimatedArrivalDate.toString())?default(nowTimestampString)}');"><img src='<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>' width='16' height='16' border='0' alt='Calendar'></a>
            <br/>
            <input type="text" size="25" name="actualStartDate" value="${(shipmentRouteSegment.actualStartDate.toString())?if_exists}" class="inputBox"/><a href="javascript:call_cal(document.updateShipmentRouteSegmentForm${shipmentRouteSegmentData_index}.actualStartDate, '${(shipmentRouteSegment.actualStartDate.toString())?default(nowTimestampString)}');"><img src='<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>' width='16' height='16' border='0' alt='Calendar'></a>
            <input type="text" size="25" name="actualArrivalDate" value="${(shipmentRouteSegment.actualArrivalDate.toString())?if_exists}" class="inputBox"/><a href="javascript:call_cal(document.updateShipmentRouteSegmentForm${shipmentRouteSegmentData_index}.actualArrivalDate, '${(shipmentRouteSegment.actualArrivalDate.toString())?default(nowTimestampString)}');"><img src='<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>' width='16' height='16' border='0' alt='Calendar'></a>
        </td>
        <td>
            <input type="text" size="5" name="billingWeight" value="${shipmentRouteSegment.billingWeight?if_exists}" class="inputBox"/>
            <select name="billingWeightUomId" class="selectBox">
                <#if billingWeightUom?has_content>
                    <option value="${billingWeightUom.uomId}">${billingWeightUom.get("description",locale)} [${billingWeightUom.abbreviation}]</option>
                    <option value="${billingWeightUom.uomId}">---</option>
                <#else>
                    <option value="">&nbsp;</option>
                </#if>
                <#list weightUoms as weightUom>
                    <option value="${weightUom.uomId}">${weightUom.get("description",locale)} [${weightUom.abbreviation}]</option>
                </#list>
            </select>
            <br/>
            <select name="currencyUomId" class="selectBox">
                <#if currencyUom?has_content>
                    <option value="${currencyUom.uomId}">${currencyUom.get("description",locale)} [${currencyUom.uomId}]</option>
                    <option value="${currencyUom.uomId}">---</option>
                <#else>
                    <option value="">&nbsp;</option>
                </#if>
                <#list currencyUoms as altCurrencyUom>
                    <option value="${altCurrencyUom.uomId}">${altCurrencyUom.get("description",locale)} [${altCurrencyUom.uomId}]</option>
                </#list>
            </select>
            <br/>
            <input type="text" size="8" name="actualTransportCost" value="${shipmentRouteSegment.actualTransportCost?if_exists}" class="inputBox"/>
            <br/>
            <input type="text" size="8" name="actualServiceCost" value="${shipmentRouteSegment.actualServiceCost?if_exists}" class="inputBox"/>
            <br/>
            <input type="text" size="8" name="actualOtherCost" value="${shipmentRouteSegment.actualOtherCost?if_exists}" class="inputBox"/>
            <br/>
            <input type="text" size="8" name="actualCost" value="${shipmentRouteSegment.actualCost?if_exists}" class="inputBox"/>
        </td>
        <td><a href="javascript:document.updateShipmentRouteSegmentForm${shipmentRouteSegmentData_index}.submit();" class="buttontext">${uiLabelMap.CommonUpdate}</a>
        <br/><div class="tabletext"><a href="<@ofbizUrl>duplicateShipmentRouteSegment?shipmentId=${shipmentId}&shipmentRouteSegmentId=${shipmentRouteSegment.shipmentRouteSegmentId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonDuplicate}</a></div></td>
        <td><div class="tabletext"><a href="<@ofbizUrl>deleteShipmentRouteSegment?shipmentId=${shipmentId}&shipmentRouteSegmentId=${shipmentRouteSegment.shipmentRouteSegmentId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonDelete}</a></div></td>
    </tr>
    </form>
    <#list shipmentPackageRouteSegs as shipmentPackageRouteSeg>
        <form action="<@ofbizUrl>updateRouteSegmentShipmentPackage</@ofbizUrl>" name="updateShipmentPackageRouteSegForm${shipmentRouteSegmentData_index}${shipmentPackageRouteSeg_index}">
        <input type="hidden" name="shipmentId" value="${shipmentId}"/>
        <input type="hidden" name="shipmentRouteSegmentId" value="${shipmentPackageRouteSeg.shipmentRouteSegmentId}"/>
        <input type="hidden" name="shipmentPackageSeqId" value="${shipmentPackageRouteSeg.shipmentPackageSeqId}"/>
        <tr>
            <td><div class="tabletext">&nbsp;</div></td>
            <td>
                <div class="tabletext">
                    ${uiLabelMap.ProductPackage} :${shipmentPackageRouteSeg.shipmentPackageSeqId}
                    <#if shipmentPackageRouteSeg.labelImage?exists>
                        <a href="<@ofbizUrl>viewShipmentPackageRouteSegLabelImage?shipmentId=${shipmentPackageRouteSeg.shipmentId}&shipmentRouteSegmentId=${shipmentPackageRouteSeg.shipmentRouteSegmentId}&shipmentPackageSeqId=${shipmentPackageRouteSeg.shipmentPackageSeqId}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductViewLabelImage}</a>
                    </#if>
                </div>
            </td>
            <td><span class="tabletext">${uiLabelMap.ProductTrack} #:</span><input type="text" size="22" name="trackingCode" value="${shipmentPackageRouteSeg.trackingCode?if_exists}" class="inputBox"/></td>
            <td><span class="tabletext">${uiLabelMap.ProductBox} #:</span><input type="text" size="5" name="boxNumber" value="${shipmentPackageRouteSeg.boxNumber?if_exists}" class="inputBox"/></td>
            <td><a href="javascript:document.updateShipmentPackageRouteSegForm${shipmentRouteSegmentData_index}${shipmentPackageRouteSeg_index}.submit();" class="buttontext">${uiLabelMap.CommonUpdate}</a></td>
            <td><div class="tabletext"><a href="<@ofbizUrl>deleteRouteSegmentShipmentPackage?shipmentId=${shipmentId}&shipmentPackageSeqId=${shipmentPackageRouteSeg.shipmentPackageSeqId}&shipmentRouteSegmentId=${shipmentPackageRouteSeg.shipmentRouteSegmentId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonDelete}</a></div></td>
        </tr>
        </form>
    </#list>
    <#--
    <tr>
        <form action="<@ofbizUrl>createRouteSegmentShipmentPackage</@ofbizUrl>" name="createShipmentPackageRouteSegForm${shipmentRouteSegmentData_index}">
        <input type="hidden" name="shipmentId" value="${shipmentId}"/>
        <input type="hidden" name="shipmentRouteSegmentId" value="${shipmentRouteSegment.shipmentRouteSegmentId}"/>
        <td><div class="tabletext">&nbsp;</div></td>
        <td>
            <div class="tabletext">${uiLabelMap.ProductAddPackageInfo} :
            <select name="shipmentPackageSeqId" class="selectBox">
                <#list shipmentPackages as shipmentPackage>
                    <option>${shipmentPackage.shipmentPackageSeqId}</option>
                </#list>
            </select>
            </div>
        </td>
        <td><span class="tabletext">Track#:</span><input type="text" size="22" name="trackingCode" class="inputBox"/></td>
        <td><span class="tabletext">Box#:</span><input type="text" size="5" name="boxNumber" class="inputBox"/></td>
        <td><a href="javascript:document.createShipmentPackageRouteSegForm${shipmentRouteSegmentData_index}.submit()" class="buttontext">${uiLabelMap.CommonAdd}</a></td>
        <td><div class="tabletext">&nbsp;</div></td>
        </form>
    </tr>
    -->
</#list>
<form action="<@ofbizUrl>createShipmentRouteSegment</@ofbizUrl>" name="createShipmentRouteSegmentForm">
    <input type="hidden" name="shipmentId" value="${shipmentId}"/>
    <tr>
        <td><div class="tabletext">${uiLabelMap.ProductNewSegment} :</div></td>
        <td><span class="tabletext"></span>
            <select name="carrierPartyId" class="selectBox">
                    <option value="">&nbsp;</option>
                <#list carrierPartyDatas as carrierPartyData>
                    <option value="${carrierPartyData.party.partyId}">${(carrierPartyData.person.firstName)?if_exists} ${(carrierPartyData.person.middleName)?if_exists} ${(carrierPartyData.person.lastName)?if_exists} ${(carrierPartyData.partyGroup.groupName)?if_exists} [${carrierPartyData.party.partyId}]</option>
                </#list>
            </select>
            <select name="shipmentMethodTypeId" class="selectBox">
                <#list shipmentMethodTypes as shipmentMethodTypeOption>
                    <option value="${shipmentMethodTypeOption.shipmentMethodTypeId}">${shipmentMethodTypeOption.get("description",locale)}</option>
                </#list>
            </select>
            <br/>
            <select name="originFacilityId" class="selectBox">
                    <option value="">&nbsp;</option>
                <#list facilities as facility>
                    <option value="${facility.facilityId}">${facility.facilityName} [${facility.facilityId}]</option>
                </#list>
            </select>
            <select name="destFacilityId" class="selectBox">
                    <option value="">&nbsp;</option>
                <#list facilities as facility>
                    <option value="${facility.facilityId}">${facility.facilityName} [${facility.facilityId}]</option>
                </#list>
            </select>
            <br/>
            <input type="text" size="15" name="originContactMechId" value="" class="inputBox"/>
            <input type="text" size="15" name="destContactMechId" value="" class="inputBox"/>
            <br/>
            <input type="text" size="15" name="originTelecomNumberId" value="" class="inputBox"/>
            <input type="text" size="15" name="destTelecomNumberId" value="" class="inputBox"/>
        </td>
        <td>
            <select name="carrierServiceStatusId" class="selectBox">
                <option value="">&nbsp;</option>
                <#list carrierServiceStatusValidChangeToDetails?if_exists as carrierServiceStatusValidChangeToDetail>
                    <option value="${carrierServiceStatusValidChangeToDetail.statusIdTo}">${carrierServiceStatusValidChangeToDetail.transitionName} [${carrierServiceStatusValidChangeToDetail.description}]</option>
                </#list>
            </select>
            <br/>
            <input type="text" size="24" name="trackingIdNumber" value="" class="inputBox"/>
            <br/>
            <input type="text" size="25" name="estimatedStartDate" value="" class="inputBox"/><a href="javascript:call_cal(document.createShipmentRouteSegmentForm.estimatedStartDate, '${nowTimestampString}');"><img src='<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>' width='16' height='16' border='0' alt='Calendar'></a>
            <input type="text" size="25" name="estimatedArrivalDate" value="" class="inputBox"/><a href="javascript:call_cal(document.createShipmentRouteSegmentForm.estimatedArrivalDate, '${nowTimestampString}');"><img src='<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>' width='16' height='16' border='0' alt='Calendar'></a>
            <br/>
            <input type="text" size="25" name="actualStartDate" value="" class="inputBox"/><a href="javascript:call_cal(document.createShipmentRouteSegmentForm.actualStartDate, '${nowTimestampString}');"><img src='<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>' width='16' height='16' border='0' alt='Calendar'></a>
            <input type="text" size="25" name="actualArrivalDate" value="" class="inputBox"/><a href="javascript:call_cal(document.createShipmentRouteSegmentForm.actualArrivalDate, '${nowTimestampString}');"><img src='<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>' width='16' height='16' border='0' alt='Calendar'></a>
        </td>
        <td>
            <input type="text" size="5" name="billingWeight" value="${(shipmentRouteSegment.billingWeight)?if_exists}" class="inputBox"/>
            <select name="billingWeightUomId" class="selectBox">
                <option value="">&nbsp;</option>
                <#list weightUoms as weightUom>
                    <option value="${weightUom.uomId}">${weightUom.get("description",locale)} [${weightUom.abbreviation}]</option>
                </#list>
            </select>
            <br/>
            <select name="currencyUomId" class="selectBox">
                <option value="">&nbsp;</option>
                <#list currencyUoms as altCurrencyUom>
                    <option value="${altCurrencyUom.uomId}">${altCurrencyUom.get("description",locale)} [${altCurrencyUom.uomId}]</option>
                </#list>
            </select>
            <br/>
            <input type="text" size="8" name="actualTransportCost" class="inputBox"/>
            <br/>
            <input type="text" size="8" name="actualServiceCost" class="inputBox"/>
            <br/>
            <input type="text" size="8" name="actualOtherCost" class="inputBox"/>
            <br/>
            <input type="text" size="8" name="actualCost" class="inputBox"/>
        </td>
        <td><a href="javascript:document.createShipmentRouteSegmentForm.submit();" class="buttontext">${uiLabelMap.CommonCreate}</a></td>
        <td>&nbsp;</td>
    </tr>
</form>
</table>
<#else>
  <h3>${uiLabelMap.ProductShipmentNotFoundId} : [${shipmentId?if_exists}]</h3>
</#if>
