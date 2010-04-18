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
            <li class="h3">${uiLabelMap.PageTitleEditShipmentRouteSegments}</li>
        </ul>
        <br class="clear"/>
    </div>
    <div class="screenlet-body">
        <table cellspacing="0" class="basic-table">
        <tr class="header-row">
            <td valign="top">${uiLabelMap.ProductSegment}</td>
            <td valign="top">
                <div>${uiLabelMap.ProductCarrierShipmentMethod}</div>
                <div>${uiLabelMap.ProductOriginDestinationFacility}</div>
                <div>${uiLabelMap.ProductOriginDestinationAddressId}</div>
                <div>${uiLabelMap.ProductOriginDestinationPhoneId}</div>
                <div>${uiLabelMap.ProductShipmentThirdPartyAccountNumber}</div>
                <div>${uiLabelMap.ProductShipmentThirdPartyPostalCode}</div>
                <div>${uiLabelMap.ProductShipmentThirdPartyCountryCode}</div>
            </td>
            <td valign="top">
                <div>${uiLabelMap.ProductShipmentFedexHomeDeliveryTypeDate}</div>
                <div>${uiLabelMap.ProductCarrierStatus}</div>
                <div>${uiLabelMap.ProductTrackingNumber}</div>
                <div>${uiLabelMap.ProductEstimatedStartArrive}</div>
                <div>${uiLabelMap.ProductActualStartArrive}</div>
            </td>
            <td valign="top">
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
        <form name="duplicateShipmentRouteSegment_${shipmentRouteSegmentData_index}" method="post" action="<@ofbizUrl>duplicateShipmentRouteSegment</@ofbizUrl>">
            <input type="hidden" name="shipmentId" value="${shipmentRouteSegment.shipmentId}"/>
            <input type="hidden" name="shipmentRouteSegmentId" value="${shipmentRouteSegment.shipmentRouteSegmentId}"/>
        </form>
        <form name="deleteShipmentRouteSegment_${shipmentRouteSegmentData_index}" method="post" action="<@ofbizUrl>deleteShipmentRouteSegment</@ofbizUrl>">
            <input type="hidden" name="shipmentId" value="${shipmentRouteSegment.shipmentId}"/>
            <input type="hidden" name="shipmentRouteSegmentId" value="${shipmentRouteSegment.shipmentRouteSegmentId}"/>
        </form>
        <form action="<@ofbizUrl>updateShipmentRouteSegment</@ofbizUrl>" method="post" name="updateShipmentRouteSegmentForm${shipmentRouteSegmentData_index}">
        <input type="hidden" name="shipmentId" value="${shipmentId}"/>
        <input type="hidden" name="shipmentRouteSegmentId" value="${shipmentRouteSegment.shipmentRouteSegmentId}"/>
        <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
            <td valign="top">
                <div>
                    ${shipmentRouteSegment.shipmentRouteSegmentId}
                    <br />
                    <a href="javascript:document.updateShipmentRouteSegmentForm${shipmentRouteSegmentData_index}.submit();" class="buttontext">${uiLabelMap.CommonUpdate}</a>
                    <br />
                    <a href="javascript:document.duplicateShipmentRouteSegment_${shipmentRouteSegmentData_index}.submit();" class="buttontext">${uiLabelMap.CommonDuplicate}</a>
                    <br />
                    <a href="javascript:document.deleteShipmentRouteSegment_${shipmentRouteSegmentData_index}.submit();" class="buttontext">${uiLabelMap.CommonDelete}</a>
                </div>
            </td>
            <td valign="top">
                <div>
                    <select name="carrierPartyId">
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
                    <select name="shipmentMethodTypeId">
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
                    <br />
                    <select name="originFacilityId">
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
                    <select name="destFacilityId">
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
                    <br />
                    <div>
                        <input type="text" size="15" name="originContactMechId" value="${shipmentRouteSegment.originContactMechId?if_exists}"/>
                        <#if originPostalAddress?has_content><span class="tooltip">${uiLabelMap.CommonTo}: ${originPostalAddress.toName?if_exists}, ${uiLabelMap.CommonAttn}: ${originPostalAddress.attnName?if_exists}, ${originPostalAddress.address1?if_exists}, ${originPostalAddress.address2?if_exists}, ${originPostalAddress.city?if_exists}, ${originPostalAddress.stateProvinceGeoId?if_exists}, ${originPostalAddress.postalCode?if_exists}, ${originPostalAddress.countryGeoId?if_exists}</span></#if>
                    </div>
                    <div>
                        <input type="text" size="15" name="destContactMechId" value="${shipmentRouteSegment.destContactMechId?if_exists}"/>
                        <#if destPostalAddress?has_content><span class="tooltip">${uiLabelMap.CommonTo}: ${destPostalAddress.toName?if_exists},${uiLabelMap.CommonAttn}: ${destPostalAddress.attnName?if_exists}, ${destPostalAddress.address1?if_exists}, ${destPostalAddress.address2?if_exists}, ${destPostalAddress.city?if_exists}, ${destPostalAddress.stateProvinceGeoId?if_exists}, ${destPostalAddress.postalCode?if_exists}, ${destPostalAddress.countryGeoId?if_exists}</span></#if>
                    </div>
                    <div>
                        <input type="text" size="15" name="originTelecomNumberId" value="${shipmentRouteSegment.originTelecomNumberId?if_exists}"/>
                        <#if originTelecomNumber?has_content><span class="tooltip">${originTelecomNumber.countryCode?if_exists}  ${originTelecomNumber.areaCode?if_exists} ${originTelecomNumber.contactNumber?if_exists}</span></#if>
                    </div>
                    <div>
                        <input type="text" size="15" name="destTelecomNumberId" value="${shipmentRouteSegment.destTelecomNumberId?if_exists}"/>
                        <#if destTelecomNumber?has_content><span class="tooltip">${destTelecomNumber.countryCode?if_exists}  ${destTelecomNumber.areaCode?if_exists} ${destTelecomNumber.contactNumber?if_exists}</span></#if>
                    </div>
                    <div>
                        <input type="text" size="15" name="thirdPartyAccountNumber" value="${shipmentRouteSegment.thirdPartyAccountNumber?if_exists}"/>
                    </div>
                    <div>
                        <input type="text" size="15" name="thirdPartyPostalCode" value="${shipmentRouteSegment.thirdPartyPostalCode?if_exists}"/>
                    </div>
                    <div>
                        <input type="text" size="15" name="thirdPartyCountryGeoCode" value="${shipmentRouteSegment.thirdPartyCountryGeoCode?if_exists}"/>
                    </div>
                </div>
            </td>
            <td valign="top">
                <div>
                    <#if "UPS" == shipmentRouteSegment.carrierPartyId?if_exists>
                        <#if !shipmentRouteSegment.carrierServiceStatusId?has_content || "SHRSCS_NOT_STARTED" == shipmentRouteSegment.carrierServiceStatusId?if_exists>
                            <a href="javascript:document.upsShipmentConfirm_${shipmentRouteSegmentData_index}.submit()" class="buttontext">${uiLabelMap.ProductConfirmShipmentUps}</a>
                            <br />
                            ${uiLabelMap.ProductShipmentUpsResidential}:
                            <input type="checkbox" name="homeDeliveryType" value="Y" ${(shipmentRouteSegment.homeDeliveryType?has_content)?string("checked=\"checked\"","")} />
                        <#elseif "SHRSCS_CONFIRMED" == shipmentRouteSegment.carrierServiceStatusId?if_exists>
                            <a href="javascript:document.upsShipmentAccept_${shipmentRouteSegmentData_index}.submit()" class="buttontext">${uiLabelMap.ProductAcceptUpsShipmentConfirmation}</a> 
                            <br />
                            <a href="javascript:document.upsVoidShipment_${shipmentRouteSegmentData_index}.submit()" class="buttontext">${uiLabelMap.ProductVoidUpsShipmentConfirmation}</a>
                        <#elseif "SHRSCS_ACCEPTED" == shipmentRouteSegment.carrierServiceStatusId?if_exists>
                            <a href="javascript:document.upsTrackShipment_${shipmentRouteSegmentData_index}.submit()" class="buttontext">${uiLabelMap.ProductTrackUpsShipment}</a>
                            <br />
                            <a href="javascript:document.upsVoidShipment_${shipmentRouteSegmentData_index}.submit()" class="buttontext">${uiLabelMap.ProductVoidUpsShipment}</a>
                        </#if>
                    </#if>
                    <#if "DHL" == shipmentRouteSegment.carrierPartyId?if_exists>
                        <#if !shipmentRouteSegment.carrierServiceStatusId?has_content || "SHRSCS_NOT_STARTED" == shipmentRouteSegment.carrierServiceStatusId?if_exists>
                            <a href="javascript:document.dhlShipmentConfirm_${shipmentRouteSegmentData_index}.submit()" class="buttontext">${uiLabelMap.ProductConfirmShipmentDHL}</a>
                        </#if>
                    </#if>
                    <#if "FEDEX" == shipmentRouteSegment.carrierPartyId?if_exists>
                        <#if !shipmentRouteSegment.carrierServiceStatusId?has_content || "SHRSCS_NOT_STARTED" == shipmentRouteSegment.carrierServiceStatusId?if_exists>
                            <a href="javascript:document.fedexShipmentConfirm_${shipmentRouteSegmentData_index}.submit()" class="buttontext">${uiLabelMap.ProductConfirmShipmentFedex}</a>
                            <br />
                            <#if shipmentMethodType?exists && shipmentMethodType.shipmentMethodTypeId=="GROUND_HOME">
                                <select name="homeDeliveryType">
                                    <option value="">${uiLabelMap.ProductShipmentNone}</option>
                                    <option ${(shipmentRouteSegment.homeDeliveryType?default("")=="DATECERTAIN")?string("selected=\"selected\"","")} value="DATECERTAIN">${uiLabelMap.ProductShipmentFedexHomeDateCertain}</option>
                                    <option ${(shipmentRouteSegment.homeDeliveryType?default("")=="EVENING")?string("selected=\"selected\"","")} value="EVENING">${uiLabelMap.ProductShipmentFedexHomeEvening}</option>
                                    <option ${(shipmentRouteSegment.homeDeliveryType?default("")=="APPOINTMENT")?string("selected=\"selected\"","")} value="APPOINTMENT">${uiLabelMap.ProductShipmentFedexHomeAppointment}</option>
                                </select>
                                <input type="text" size="25" name="homeDeliveryDate" value="${(shipmentRouteSegment.homeDeliveryDate.toString())?if_exists}"/><a href="javascript:call_cal(document.updateShipmentRouteSegmentForm${shipmentRouteSegmentData_index}.homeDeliveryDate, '${(shipmentRouteSegment.homeDeliveryDate.toString())?default(nowTimestampString)}');"><img src='<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>' width='16' height='16' border='0' alt='Calendar' /></a>
                            </#if>
                        <#else>
                            <#-- Todo: implement closeout with Fedex -->
                            <#-- Todo: implement shipment cancellation with Fedex -->
                            <#-- Todo: implement shipment tracking with Fedex -->
                            ${shipmentRouteSegment.homeDeliveryType?default(uiLabelMap.ProductShipmentNone)}
                            <#if shipmentRouteSegment.homeDeliveryDate?exists>
                                &nbsp;(${shipmentRouteSegment.homeDeliveryDate?string("yyyy-MM-dd")})
                            </#if>
                            <br />
                        </#if>
                    </#if>
                    <br />
                    <select name="carrierServiceStatusId">
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
                    <br />
                    <input type="text" size="24" name="trackingIdNumber" value="${shipmentRouteSegment.trackingIdNumber?if_exists}"/>
                    <br />
                    <input type="text" size="25" name="estimatedStartDate" value="${(shipmentRouteSegment.estimatedStartDate.toString())?if_exists}"/><a href="javascript:call_cal(document.updateShipmentRouteSegmentForm${shipmentRouteSegmentData_index}.estimatedStartDate, '${(shipmentRouteSegment.estimatedStartDate.toString())?default(nowTimestampString)}');"><img src='<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>' width='16' height='16' border='0' alt='Calendar' /></a>
                    <input type="text" size="25" name="estimatedArrivalDate" value="${(shipmentRouteSegment.estimatedArrivalDate.toString())?if_exists}"/><a href="javascript:call_cal(document.updateShipmentRouteSegmentForm${shipmentRouteSegmentData_index}.estimatedArrivalDate, '${(shipmentRouteSegment.estimatedArrivalDate.toString())?default(nowTimestampString)}');"><img src='<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>' width='16' height='16' border='0' alt='Calendar' /></a>
                    <br />
                    <input type="text" size="25" name="actualStartDate" value="${(shipmentRouteSegment.actualStartDate.toString())?if_exists}"/><a href="javascript:call_cal(document.updateShipmentRouteSegmentForm${shipmentRouteSegmentData_index}.actualStartDate, '${(shipmentRouteSegment.actualStartDate.toString())?default(nowTimestampString)}');"><img src='<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>' width='16' height='16' border='0' alt='Calendar' /></a>
                    <input type="text" size="25" name="actualArrivalDate" value="${(shipmentRouteSegment.actualArrivalDate.toString())?if_exists}"/><a href="javascript:call_cal(document.updateShipmentRouteSegmentForm${shipmentRouteSegmentData_index}.actualArrivalDate, '${(shipmentRouteSegment.actualArrivalDate.toString())?default(nowTimestampString)}');"><img src='<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>' width='16' height='16' border='0' alt='Calendar' /></a>
                </div>
            </td>
            <td valign="top">
                <input type="text" size="5" name="billingWeight" value="${shipmentRouteSegment.billingWeight?if_exists}"/>
                <select name="billingWeightUomId">
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
                <br />
                <select name="currencyUomId">
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
                <br />
                <input type="text" size="8" name="actualTransportCost" value="${shipmentRouteSegment.actualTransportCost?if_exists}"/>
                <br />
                <input type="text" size="8" name="actualServiceCost" value="${shipmentRouteSegment.actualServiceCost?if_exists}"/>
                <br />
                <input type="text" size="8" name="actualOtherCost" value="${shipmentRouteSegment.actualOtherCost?if_exists}"/>
                <br />
                <input type="text" size="8" name="actualCost" value="${shipmentRouteSegment.actualCost?if_exists}"/>
            </td>
        </tr>
        </form>
        <form name="upsShipmentConfirm_${shipmentRouteSegmentData_index}" method="post" action="<@ofbizUrl>upsShipmentConfirm</@ofbizUrl>">
            <input type="hidden" name="shipmentId" value="${shipmentRouteSegment.shipmentId}"/>
            <input type="hidden" name="shipmentRouteSegmentId" value="${shipmentRouteSegment.shipmentRouteSegmentId}"/>
        </form>
        <form name="upsShipmentAccept_${shipmentRouteSegmentData_index}" method="post" action="<@ofbizUrl>upsShipmentAccept</@ofbizUrl>">
            <input type="hidden" name="shipmentId" value="${shipmentRouteSegment.shipmentId}"/>
            <input type="hidden" name="shipmentRouteSegmentId" value="${shipmentRouteSegment.shipmentRouteSegmentId}"/>
        </form>
        <form name="upsVoidShipment_${shipmentRouteSegmentData_index}" method="post" action="<@ofbizUrl>upsVoidShipment</@ofbizUrl>">
            <input type="hidden" name="shipmentId" value="${shipmentRouteSegment.shipmentId}"/>
            <input type="hidden" name="shipmentRouteSegmentId" value="${shipmentRouteSegment.shipmentRouteSegmentId}"/>
        </form>
        <form name="upsTrackShipment_${shipmentRouteSegmentData_index}" method="post" action="<@ofbizUrl>upsTrackShipment</@ofbizUrl>">
            <input type="hidden" name="shipmentId" value="${shipmentRouteSegment.shipmentId}"/>
            <input type="hidden" name="shipmentRouteSegmentId" value="${shipmentRouteSegment.shipmentRouteSegmentId}"/>
        </form>
        
        <form name="dhlShipmentConfirm_${shipmentRouteSegmentData_index}" method="post" action="<@ofbizUrl>dhlShipmentConfirm</@ofbizUrl>">
            <input type="hidden" name="shipmentId" value="${shipmentRouteSegment.shipmentId}"/>
            <input type="hidden" name="shipmentRouteSegmentId" value="${shipmentRouteSegment.shipmentRouteSegmentId}"/>
        </form>
        
        <form name="fedexShipmentConfirm_${shipmentRouteSegmentData_index}" method="post" action="<@ofbizUrl>fedexShipmentConfirm</@ofbizUrl>">
            <input type="hidden" name="shipmentId" value="${shipmentRouteSegment.shipmentId}"/>
            <input type="hidden" name="shipmentRouteSegmentId" value="${shipmentRouteSegment.shipmentRouteSegmentId}"/>
        </form>
    <#list shipmentPackageRouteSegs as shipmentPackageRouteSeg>
        <form action="<@ofbizUrl>updateRouteSegmentShipmentPackage</@ofbizUrl>" method="post" name="updateShipmentPackageRouteSegForm${shipmentRouteSegmentData_index}${shipmentPackageRouteSeg_index}">
        <input type="hidden" name="shipmentId" value="${shipmentId}"/>
        <input type="hidden" name="shipmentRouteSegmentId" value="${shipmentPackageRouteSeg.shipmentRouteSegmentId}"/>
        <input type="hidden" name="shipmentPackageSeqId" value="${shipmentPackageRouteSeg.shipmentPackageSeqId}"/>
        <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
            <td>&nbsp;</td>
            <td valign="top">
                <div>
                    <span class="label">${uiLabelMap.ProductPackage}</span> ${shipmentPackageRouteSeg.shipmentPackageSeqId}
                    <#if shipmentPackageRouteSeg.labelImage?exists>
                        <a href="javascript:document.viewShipmentPackageRouteSegLabelImage_${shipmentRouteSegmentData_index}_${shipmentPackageRouteSeg_index}.submit();" class="buttontext">${uiLabelMap.ProductViewLabelImage}</a>
                    </#if>
                    <span class="label">${uiLabelMap.ProductTrack} #</span><input type="text" size="22" name="trackingCode" value="${shipmentPackageRouteSeg.trackingCode?if_exists}"/>
                </div>
            </td>
            <td valign="top">
               <div>
                   <span class="label">${uiLabelMap.ProductBox} #</span>
                   <input type="text" size="5" name="boxNumber" value="${shipmentPackageRouteSeg.boxNumber?if_exists}"/>
               </div>
            </td>
            <td valign="top">
                <div>
                    <a href="javascript:document.updateShipmentPackageRouteSegForm${shipmentRouteSegmentData_index}${shipmentPackageRouteSeg_index}.submit();" class="buttontext">${uiLabelMap.CommonUpdate}</a>
                    <a href="javascript:document.deleteRouteSegmentShipmentPackage_${shipmentRouteSegmentData_index}_${shipmentPackageRouteSeg_index}.submit();" class="buttontext">${uiLabelMap.CommonDelete}</a>
                </div>
            </td>
        </tr>
        </form>
        <form name="viewShipmentPackageRouteSegLabelImage_${shipmentRouteSegmentData_index}_${shipmentPackageRouteSeg_index}" method="post" action="<@ofbizUrl>viewShipmentPackageRouteSegLabelImage</@ofbizUrl>">
            <input type="hidden" name="shipmentId" value="${shipmentPackageRouteSeg.shipmentId}"/>
            <input type="hidden" name="shipmentPackageSeqId" value="${shipmentPackageRouteSeg.shipmentPackageSeqId}"/>
            <input type="hidden" name="shipmentRouteSegmentId" value="${shipmentPackageRouteSeg.shipmentRouteSegmentId}"/>
        </form>
        <form name="deleteRouteSegmentShipmentPackage_${shipmentRouteSegmentData_index}_${shipmentPackageRouteSeg_index}" method="post" action="<@ofbizUrl>deleteRouteSegmentShipmentPackage</@ofbizUrl>">
            <input type="hidden" name="shipmentId" value="${shipmentId}"/>
            <input type="hidden" name="shipmentPackageSeqId" value="${shipmentPackageRouteSeg.shipmentPackageSeqId}"/>
            <input type="hidden" name="shipmentRouteSegmentId" value="${shipmentPackageRouteSeg.shipmentRouteSegmentId}"/>
        </form>
    </#list>
        <#--
        <tr>
            <form action="<@ofbizUrl>createRouteSegmentShipmentPackage</@ofbizUrl>" name="createShipmentPackageRouteSegForm${shipmentRouteSegmentData_index}">
            <input type="hidden" name="shipmentId" value="${shipmentId}"/>
            <input type="hidden" name="shipmentRouteSegmentId" value="${shipmentRouteSegment.shipmentRouteSegmentId}"/>
            <td>&nbsp;</td>
            <td>
                <div>${uiLabelMap.ProductAddPackageInfo} :
                <select name="shipmentPackageSeqId">
                    <#list shipmentPackages as shipmentPackage>
                        <option>${shipmentPackage.shipmentPackageSeqId}</option>
                    </#list>
                </select>
                </div>
            </td>
            <td><span class="label">Track#:</span><input type="text" size="22" name="trackingCode"/></td>
            <td><span class="label">Box#:</span><input type="text" size="5" name="boxNumber"/></td>
            <td><a href="javascript:document.createShipmentPackageRouteSegForm${shipmentRouteSegmentData_index}.submit()" class="buttontext">${uiLabelMap.CommonAdd}</a></td>
            <td>&nbsp;</td>
            </form>
        </tr>
        -->
        <#-- toggle the row color -->
        <#assign alt_row = !alt_row>
    </#list>
    </table>
    </div>
</div>
<div class="screenlet">
    <div class="screenlet-title-bar">
        <ul>
            <li class="h3">${uiLabelMap.PageTitleAddShipmentRouteSegment}</li>
        </ul>
        <br class="clear"/>
    </div>
    <div class="screenlet-body">
        <table cellspacing="0" class="basic-table">
            <form action="<@ofbizUrl>createShipmentRouteSegment</@ofbizUrl>" method="post" name="createShipmentRouteSegmentForm">
            <input type="hidden" name="shipmentId" value="${shipmentId}"/>
            <tr>
                <td valign="top">
                    <div>
                        <span class="label">${uiLabelMap.ProductNewSegment}</span>
                        <br />
                        <a href="javascript:document.createShipmentRouteSegmentForm.submit();" class="buttontext">${uiLabelMap.CommonCreate}</a>
                    </div>
                </td>
                <td valign="top">
                    <div>
                        <select name="carrierPartyId">
                                <option value="">&nbsp;</option>
                            <#list carrierPartyDatas as carrierPartyData>
                                <option value="${carrierPartyData.party.partyId}">${(carrierPartyData.person.firstName)?if_exists} ${(carrierPartyData.person.middleName)?if_exists} ${(carrierPartyData.person.lastName)?if_exists} ${(carrierPartyData.partyGroup.groupName)?if_exists} [${carrierPartyData.party.partyId}]</option>
                            </#list>
                        </select>
                        <select name="shipmentMethodTypeId">
                            <#list shipmentMethodTypes as shipmentMethodTypeOption>
                                <option value="${shipmentMethodTypeOption.shipmentMethodTypeId}">${shipmentMethodTypeOption.get("description",locale)}</option>
                            </#list>
                        </select>
                        <br />
                        <select name="originFacilityId">
                                <option value="">&nbsp;</option>
                            <#list facilities as facility>
                                <option value="${facility.facilityId}">${facility.facilityName} [${facility.facilityId}]</option>
                            </#list>
                        </select>
                        <select name="destFacilityId">
                                <option value="">&nbsp;</option>
                            <#list facilities as facility>
                                <option value="${facility.facilityId}">${facility.facilityName} [${facility.facilityId}]</option>
                            </#list>
                        </select>
                        <br />
                        <input type="text" size="15" name="originContactMechId" value=""/>
                        <input type="text" size="15" name="destContactMechId" value=""/>
                        <br />
                        <input type="text" size="15" name="originTelecomNumberId" value=""/>
                        <input type="text" size="15" name="destTelecomNumberId" value=""/>
                    </div>
                </td>
                <td valign="top">
                    <select name="carrierServiceStatusId">
                        <option value="">&nbsp;</option>
                        <#list carrierServiceStatusValidChangeToDetails?if_exists as carrierServiceStatusValidChangeToDetail>
                            <option value="${carrierServiceStatusValidChangeToDetail.statusIdTo}">${carrierServiceStatusValidChangeToDetail.transitionName} [${carrierServiceStatusValidChangeToDetail.description}]</option>
                        </#list>
                    </select>
                    <br />
                    <input type="text" size="24" name="trackingIdNumber" value=""/>
                    <br />
                    <input type="text" size="25" name="estimatedStartDate" value=""/><a href="javascript:call_cal(document.createShipmentRouteSegmentForm.estimatedStartDate, '${nowTimestampString}');"><img src='<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>' width='16' height='16' border='0' alt='Calendar' /></a>
                    <input type="text" size="25" name="estimatedArrivalDate" value=""/><a href="javascript:call_cal(document.createShipmentRouteSegmentForm.estimatedArrivalDate, '${nowTimestampString}');"><img src='<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>' width='16' height='16' border='0' alt='Calendar' /></a>
                    <br />
                    <input type="text" size="25" name="actualStartDate" value=""/><a href="javascript:call_cal(document.createShipmentRouteSegmentForm.actualStartDate, '${nowTimestampString}');"><img src='<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>' width='16' height='16' border='0' alt='Calendar' /></a>
                    <input type="text" size="25" name="actualArrivalDate" value=""/><a href="javascript:call_cal(document.createShipmentRouteSegmentForm.actualArrivalDate, '${nowTimestampString}');"><img src='<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>' width='16' height='16' border='0' alt='Calendar' /></a>
                </td>
                <td valign="top">
                    <input type="text" size="5" name="billingWeight" value="${(shipmentRouteSegment.billingWeight)?if_exists}"/>
                    <select name="billingWeightUomId">
                        <option value="">&nbsp;</option>
                        <#list weightUoms as weightUom>
                            <option value="${weightUom.uomId}">${weightUom.get("description",locale)} [${weightUom.abbreviation}]</option>
                        </#list>
                    </select>
                    <br />
                    <select name="currencyUomId">
                        <option value="">&nbsp;</option>
                        <#list currencyUoms as altCurrencyUom>
                            <option value="${altCurrencyUom.uomId}">${altCurrencyUom.get("description",locale)} [${altCurrencyUom.uomId}]</option>
                        </#list>
                    </select>
                    <br />
                    <input type="text" size="8" name="actualTransportCost"/>
                    <br />
                    <input type="text" size="8" name="actualServiceCost"/>
                    <br />
                    <input type="text" size="8" name="actualOtherCost"/>
                    <br />
                    <input type="text" size="8" name="actualCost"/>
                </td>
            </tr>
            </form>
        </table>
    </div>
</div>
<#else>
<div class="screenlet">
    <div class="screenlet-title-bar">
        <ul>
            <li class="h3">${uiLabelMap.ProductShipmentNotFoundId} : [${shipmentId?if_exists}]</li>
        </ul>
        <br class="clear"/>
    </div>
</div>
</#if>
