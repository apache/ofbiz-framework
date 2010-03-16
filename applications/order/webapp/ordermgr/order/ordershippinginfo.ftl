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

<script language="JavaScript" type="text/javascript">
    function editInstruction() {
        $('shippingInstructions').style.display="block";
        $('saveInstruction').style.display="inline";
        $('editInstruction').style.display="none";
        $('instruction').style.display="none";
    }
    function addInstruction() {
        $('shippingInstructions').style.display="block";
        $('saveInstruction').style.display="inline";
        $('addInstruction').style.display="none";
    }
    function saveInstruction() {
        document.updateShippingInstructionsForm.submit();
    }
    function editGiftMessage() {
        $('giftMessage').style.display="block";
        $('saveGiftMessage').style.display="inline";
        $('editGiftMessage').style.display="none";
        $('message').style.display="none";
    }
    function addGiftMessage() {
        $('giftMessage').style.display="block";
        $('saveGiftMessage').style.display="inline";
        $('addGiftMessage').style.display="none";
    }
    function saveGiftMessage() {
        document.setGiftMessageForm.submit();
    }
</script>

<#if (security.hasEntityPermission("ORDERMGR", "_UPDATE", session) || security.hasRolePermission("ORDERMGR", "_UPDATE", "", "", session)) && orderHeader.salesChannelEnumId != "POS_SALES_CHANNEL">
  <div class="screenlet">
    <div class="screenlet-title-bar">
      <ul><li class="h3">&nbsp;${uiLabelMap.OrderActions}</li></ul>
      <br class="clear"/>
    </div>
    <div class="screenlet-body">
      <ul>
        <#if security.hasEntityPermission("FACILITY", "_CREATE", session) && ((orderHeader.statusId == "ORDER_APPROVED") || (orderHeader.statusId == "ORDER_SENT"))>
          <#-- Special shipment options -->
          <#if orderHeader.orderTypeId == "SALES_ORDER">
            <form name="quickShipOrder" method="post" action="<@ofbizUrl>quickShipOrder</@ofbizUrl>">
              <input type="hidden" name="orderId" value="${orderId}"/>
            </form>
            <li><a href="javascript:document.quickShipOrder.submit()" class="buttontext">${uiLabelMap.OrderQuickShipEntireOrder}</a></li>
          <#else> <#-- PURCHASE_ORDER -->
            <span class="label">&nbsp;<#if orderHeader.orderTypeId == "PURCHASE_ORDER">${uiLabelMap.ProductDestinationFacility}</#if></span>
            <#if ownedFacilities?has_content>
              <#if !allShipments?has_content>
                <form action="/facility/control/quickShipPurchaseOrder?externalLoginKey=${externalLoginKey}" method="post">
                  <input type="hidden" name="initialSelected" value="Y"/>
                  <input type="hidden" name="orderId" value="${orderId}"/>
                  <#-- destination form (/facility/control/ReceiveInventory) wants purchaseOrderId instead of orderId, so we set it here as a workaround -->
                  <input type="hidden" name="purchaseOrderId" value="${orderId}"/>
                  <li>
                    <select name="facilityId">
                      <#list ownedFacilities as facility>
                        <option value="${facility.facilityId}">${facility.facilityName}</option>
                      </#list>
                    </select>
                    <input type="submit" class="smallSubmit" value="${uiLabelMap.OrderQuickReceivePurchaseOrder}"/>
                  </li>
                </form>
                <form name="receivePurchaseOrderForm" action="/facility/control/quickShipPurchaseOrder?externalLoginKey=${externalLoginKey}" method="post">
                  <input type="hidden" name="initialSelected" value="Y"/>
                  <input type="hidden" name="orderId" value="${orderId}"/>
                  <input type="hidden" name="purchaseOrderId" value="${orderId}"/>
                  <input type="hidden" name="partialReceive" value="Y"/>
                  <li>
                    <select name="facilityId">
                      <#list ownedFacilities as facility>
                        <option value="${facility.facilityId}">${facility.facilityName}</option>
                      </#list>
                    </select>
                    <a href="javascript:document.receivePurchaseOrderForm.submit()" class="buttontext">${uiLabelMap.CommonReceive}</a>
                  </li>
                </form>
              <#else>
                <form name="receiveInventoryForm" action="/facility/control/ReceiveInventory" method="post">
                  <input type="hidden" name="initialSelected" value="Y"/>
                  <input type="hidden" name="purchaseOrderId" value="${orderId?if_exists}"/>
                  <li>
                    <select name="facilityId">
                      <#list ownedFacilities as facility>
                        <option value="${facility.facilityId}">${facility.facilityName}</option>
                      </#list>
                    </select>
                    <a href="javascript:document.receiveInventoryForm.submit()" class="buttontext">${uiLabelMap.OrderQuickReceivePurchaseOrder}</a>
                  </li>
                </form>
                <form name="partialReceiveInventoryForm" action="/facility/control/ReceiveInventory" method="post">
                  <input type="hidden" name="initialSelected" value="Y"/>
                  <input type="hidden" name="purchaseOrderId" value="${orderId?if_exists}"/>
                  <input type="hidden" name="partialReceive" value="Y"/>
                  <li>
                    <select name="facilityId">
                      <#list ownedFacilities as facility>
                        <option value="${facility.facilityId}">${facility.facilityName}</option>
                      </#list>
                    </select>
                    <a href="javascript:document.partialReceiveInventoryForm.submit()" class="buttontext">${uiLabelMap.CommonReceive}</a>
                  </li>
                </form>
              </#if>
              <#if orderHeader.statusId != "ORDER_COMPLETED">
                <form action="<@ofbizUrl>completePurchaseOrder?externalLoginKey=${externalLoginKey}</@ofbizUrl>" method="post">
                  <input type="hidden" name="orderId" value="${orderId}"/>
                  <li>
                    <select name="facilityId">
                      <#list ownedFacilities as facility>
                        <option value="${facility.facilityId}">${facility.facilityName}</option>
                      </#list>
                    </select>
                    <input type="submit" class="smallSubmit" value="${uiLabelMap.OrderForceCompletePurchaseOrder}"/>
                  </li>
                </form>
              </#if>
            </#if>
          </#if>
        </#if>
        <#-- Refunds/Returns for Sales Orders and Delivery Schedules -->
        <#if orderHeader.statusId != "ORDER_COMPLETED" && orderHeader.statusId != "ORDER_CANCELLED">
          <li><a href="<@ofbizUrl>OrderDeliveryScheduleInfo?orderId=${orderId}</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderViewEditDeliveryScheduleInfo}</a></li>
        </#if>
        <#if security.hasEntityPermission("ORDERMGR", "_RETURN", session) && orderHeader.statusId == "ORDER_COMPLETED">
          <#if returnableItems?has_content>
            <form name="quickRefundOrder" method="post" action="<@ofbizUrl>quickRefundOrder</@ofbizUrl>">
              <input type="hidden" name="orderId" value="${orderId}"/>
              <input type="hidden" name="receiveReturn" value="true"/>
              <input type="hidden" name="returnHeaderTypeId" value="${returnHeaderTypeId}"/>
            </form>
            <li><a href="javascript:document.quickRefundOrder.submit()" class="buttontext">${uiLabelMap.OrderQuickRefundEntireOrder}</a></li>

            <form name="quickreturn" method="post" action="<@ofbizUrl>quickreturn</@ofbizUrl>">
              <input type="hidden" name="orderId" value="${orderId}"/>
              <input type="hidden" name="party_id" value="${partyId?if_exists}"/>
              <input type="hidden" name="returnHeaderTypeId" value="${returnHeaderTypeId}"/>
              <input type="hidden" name="needsInventoryReceive" value="${needsInventoryReceive?default("N")}"/>
            </form>
            <li><a href="javascript:document.quickreturn.submit()" class="buttontext">${uiLabelMap.OrderCreateReturn}</a></li>
          </#if>
        </#if>

        <#if orderHeader?has_content && orderHeader.statusId != "ORDER_CANCELLED">
          <#if orderHeader.statusId != "ORDER_COMPLETED">
            <#--
              <li><a href="<@ofbizUrl>cancelOrderItem?${paramString}</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderCancelAllItems}</a></li>
            -->
            <li><a href="<@ofbizUrl>editOrderItems?${paramString}</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderEditItems}</a></li>
          </#if>
          <li><a href="<@ofbizUrl>loadCartFromOrder?${paramString}&amp;finalizeMode=init</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderCreateAsNewOrder}</a></li>
          <#if orderHeader.statusId == "ORDER_COMPLETED">
            <li><a href="<@ofbizUrl>loadCartForReplacementOrder?${paramString}</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderCreateReplacementOrder}</a></li>
          </#if>
        </#if>
        <li><a href="<@ofbizUrl>OrderHistory?orderId=${orderId}</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderViewOrderHistory}</a></li>
      </ul>
    </div>
  </div>
</#if>

<#if shipGroups?has_content && orderHeader.salesChannelEnumId != "POS_SALES_CHANNEL">
<#list shipGroups as shipGroup>
  <#assign shipmentMethodType = shipGroup.getRelatedOne("ShipmentMethodType")?if_exists>
  <#assign shipGroupAddress = shipGroup.getRelatedOne("PostalAddress")?if_exists>
  <div class="screenlet">
    <div class="screenlet-title-bar">
       <ul>
         <li class="h3">&nbsp;${uiLabelMap.OrderShipmentInformation} - ${shipGroup.shipGroupSeqId}</li>
         <li class="expanded"><a onclick="javascript:toggleScreenlet(this, 'ShipGroupScreenletBody_${shipGroup.shipGroupSeqId}', 'true', '${uiLabelMap.CommonExpand}', '${uiLabelMap.CommonCollapse}');" title="Collapse">&nbsp</a></li>
         <li><a href="<@ofbizUrl>shipGroups.pdf?orderId=${orderId}&amp;shipGroupSeqId=${shipGroup.shipGroupSeqId}</@ofbizUrl>">${uiLabelMap.OrderShipGroup} PDF</a></li>
       </ul>
       <br class="clear"/>
    </div>
    <div class="screenlet-body" id="ShipGroupScreenletBody_${shipGroup.shipGroupSeqId}">
        <form name="updateOrderItemShipGroup" method="post" action="<@ofbizUrl>updateOrderItemShipGroup</@ofbizUrl>">
        <input type="hidden" name="orderId" value="${orderId?if_exists}"/>
        <input type="hidden" name="shipGroupSeqId" value="${shipGroup.shipGroupSeqId?if_exists}"/>
        <input type="hidden" name="contactMechPurposeTypeId" value="SHIPPING_LOCATION"/>
        <input type="hidden" name="oldContactMechId" value="${shipGroup.contactMechId?if_exists}"/>
        <table class="basic-table" cellspacing='0'>
            <#if shipGroup.contactMechId?has_content>
                <tr>
                    <td align="right" valign="top" width="15%">
                        <span class="label">&nbsp;${uiLabelMap.OrderAddress}</span>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td valign="top" width="80%">
                        <div>
                            <#if orderHeader?has_content && orderHeader.statusId != "ORDER_CANCELLED" && orderHeader.statusId != "ORDER_COMPLETED" && orderHeader.statusId != "ORDER_REJECTED">
                            <select name="contactMechId">
                                <option selected="selected" value="${shipGroup.contactMechId?if_exists}">${(shipGroupAddress.address1)?default("")} - ${shipGroupAddress.city?default("")}</option>
                                <#if shippingContactMechList?has_content>
                                <option disabled="disabled" value=""></option>
                                <#list shippingContactMechList as shippingContactMech>
                                <#assign shippingPostalAddress = shippingContactMech.getRelatedOne("PostalAddress")?if_exists>
                                <#if shippingContactMech.contactMechId?has_content>
                                <option value="${shippingContactMech.contactMechId?if_exists}">${(shippingPostalAddress.address1)?default("")} - ${shippingPostalAddress.city?default("")}</option>
                                </#if>
                                </#list>
                                </#if>
                            </select>
                            <#else>
                            ${(shipGroupAddress.address1)?default("")}
                            </#if>
                        </div>
                    </td>
                </tr>
                </#if>

                <#-- the setting of shipping method is only supported for sales orders at this time -->
                <#if orderHeader.orderTypeId == "SALES_ORDER" && shipGroup.shipmentMethodTypeId?has_content>
                  <tr>
                    <td align="right" valign="top" width="15%">
                        <span class="label">&nbsp;<b>${uiLabelMap.CommonMethod}</span>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td valign="top" width="80%">
                        <#if shipGroup.carrierPartyId?has_content || shipmentMethodType?has_content>
                        <div>
                            <#if orderHeader?has_content && orderHeader.statusId != "ORDER_CANCELLED" && orderHeader.statusId != "ORDER_COMPLETED" && orderHeader.statusId != "ORDER_REJECTED">
                            <#-- passing the shipmentMethod value as the combination of two fields value
                            i.e shipmentMethodTypeId & carrierPartyId and this two field values are separated bye
                            "@" symbol.
                            -->
                            <select name="shipmentMethod">
                                <option value="${shipGroup.shipmentMethodTypeId}@${shipGroup.carrierPartyId?if_exists}"><#if shipGroup.carrierPartyId != "_NA_">${shipGroup.carrierPartyId?if_exists}</#if>&nbsp;${shipmentMethodType.get("description",locale)?default("")}</option>
                                <#list productStoreShipmentMethList as productStoreShipmentMethod>
                                <#assign shipmentMethodTypeAndParty = productStoreShipmentMethod.shipmentMethodTypeId + "@" + productStoreShipmentMethod.partyId>
                                <#if productStoreShipmentMethod.partyId?has_content || productStoreShipmentMethod?has_content>
                                <option value="${shipmentMethodTypeAndParty?if_exists}"><#if productStoreShipmentMethod.partyId != "_NA_">${productStoreShipmentMethod.partyId?if_exists}</#if>&nbsp;${productStoreShipmentMethod.get("description",locale)?default("")}</option>
                                </#if>
                                </#list>
                            </select>
                            <#else>
                            <#if shipGroup.carrierPartyId != "_NA_">
                            ${shipGroup.carrierPartyId?if_exists}
                            </#if>
                            ${shipmentMethodType.get("description",locale)?default("")}
                            </#if>
                        </div>
                        </#if>
                    </td>
                </tr>
                </#if>
                <#if orderHeader?has_content && orderHeader.statusId != "ORDER_CANCELLED" && orderHeader.statusId != "ORDER_COMPLETED" && orderHeader.statusId != "ORDER_REJECTED">
                <tr>
                    <td align="right" valign="top" width="15%">&nbsp;</td>
                    <td width="5">&nbsp;</td>
                    <td valign="top" width="80%">
                        <input type="submit" value="${uiLabelMap.CommonUpdate}" class="smallSubmit"/>
                        <a class="buttontext" id="newShippingAddress" href="javascript:void(0);">${uiLabelMap.OrderNewShippingAddress}</a>
                    </td>
                </tr>
                </#if>
            <#if !shipGroup.contactMechId?has_content && !shipGroup.shipmentMethodTypeId?has_content>
            <#assign noShipment = "true">
            <tr>
                <td colspan="3" align="center">${uiLabelMap.OrderNotShipped}</td>
            </tr>
            </#if>
      </table>
      </form>
      <div id="newShippingAddressForm" class="popup" style="display: none;">
        <form id="addShippingAddress" name="addShippingAddress" method="post" action="addShippingAddress">
          <input type="hidden" name="orderId" value="${orderId?if_exists}"/>
          <input type="hidden" name="partyId" value="${partyId?if_exists}"/>
          <input type="hidden" name="oldContactMechId" value="${shipGroup.contactMechId?if_exists}"/>
          <input type="hidden" name="shipGroupSeqId" value="${shipGroup.shipGroupSeqId?if_exists}"/>
          <input type="hidden" name="contactMechPurposeTypeId" value="SHIPPING_LOCATION"/>
          <div class="form-row">
            <label for="address1">${uiLabelMap.PartyAddressLine1}* <span id="advice-required-address1" style="display: none" class="custom-advice">(required)</span></label>
            <div class="form-field"><input type="text" class="required" name="shipToAddress1" id="address1" value="" size="30" maxlength="30"></div>
          </div>
          <div class="form-row">
            <label for="address2">${uiLabelMap.PartyAddressLine2}</label>
            <div class="form-field"><input type="text" name="shipToAddress2" value="" size="30" maxlength="30"></div>
          </div>
          <div class="form-row">
            <label for="city">${uiLabelMap.PartyCity}* <span id="advice-required-city" style="display: none" class="custom-advice">(required)</span></label>
            <div class="form-field"><input type="text" class="required" name="shipToCity" id="city" value="" size="30" maxlength="30"></div>
          </div>
          <div class="form-row">
            <label for="postalCode">${uiLabelMap.PartyZipCode}* <span id="advice-required-postalCode" style="display: none" class="custom-advice">(required)</span></label>
            <div class="form-field"><input type="text" class="required" name="shipToPostalCode" id="postalCode" value="" size="30" maxlength="10"></div>
          </div>
          <div class="form-row">
            <label for="country">${uiLabelMap.PartyCountry}* <span id="advice-required-countryGeoId" style="display: none" class="custom-advice">(required)</span></label>
            <div class="form-field">
              <select name="shipToCountryGeoId" id="countryGeoId" class="required" style="width: 70%">
                <#if countryGeoId?exists>
                  <option value="${countryGeoId}">${countryGeoId}</option>
                </#if>
                ${screens.render("component://common/widget/CommonScreens.xml#countries")}
              </select>
            </div>
          </div>
          <div id="states" class="form-row">
            <label for="state">${uiLabelMap.PartyState}* <span id="advice-required-stateProvinceGeoId" style="display: none" class="custom-advice">(required)</span></label>
            <div class="form-field">
              <select name="shipToStateProvinceGeoId" id="stateProvinceGeoId" style="width: 70%">
                <#if stateProvinceGeoId?has_content>
                  <option value="${stateProvinceGeoId}">${stateProvinceGeoId}</option>
                <#else>
                  <option value="_NA_">${uiLabelMap.PartyNoState}</option>
                </#if>
              </select>
            </div>
          </div>
          <div class="form-row">
            <input id="submitAddShippingAddress" type="button" value="${uiLabelMap.CommonSubmit}"/>
            <form action="">
              <input class="popup_closebox buttontext" type="button" value="${uiLabelMap.CommonClose}"/>
            </form>
          </div>
        </form>
      </div>
      <script language="JavaScript" type="text/javascript">
        new Popup('newShippingAddressForm', 'newShippingAddress', {modal: true, position: 'center', trigger: 'click'})
      </script>
      <table width="100%" border="0" cellpadding="1" cellspacing="0">
        <#if shipGroup.supplierPartyId?has_content>
          <#assign supplier =  delegator.findByPrimaryKey("PartyGroup", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", shipGroup.supplierPartyId))?if_exists />
          <tr><td colspan="3"><hr /></td></tr>
          <tr>
            <td align="right" valign="top" width="15%">
              <span class="label">&nbsp;${uiLabelMap.ProductDropShipment} - ${uiLabelMap.PartySupplier}</span>
            </td>
            <td width="5">&nbsp;</td>
            <td valign="top" width="80%">
              <#if supplier?has_content> - ${supplier.description?default(shipGroup.supplierPartyId)}</#if>
            </td>
          </tr>
        </#if>
        
        <#-- This section appears when Shipment of order is in picked status and its items are packed,this case comes when new shipping estimates based on weight of packages are more than or less than default percentage (defined in shipment.properties) of original shipping estimate-->
        <#-- getShipGroupEstimate method of ShippingEvents class can be used for get shipping estimate from system, on the basis of new package's weight -->
        <#if shippingRateList?has_content>
          <#if orderReadHelper.getOrderTypeId() != "PURCHASE_ORDER">
            <tr><td colspan="3"><hr /></td></tr>
            <tr> 
              <td colspan="3">
                <table>
                  <tr>
                    <td>
                      <span class="label">&nbsp;${uiLabelMap.OrderOnlineUPSShippingEstimates}</span>
                    </td>
                  </tr>
                  <form name="UpdateShippingMethod" method="post" action="<@ofbizUrl>updateShippingMethodAndCharges</@ofbizUrl>">
                    <#list shippingRateList as shippingRate>
                      <tr>
                        <td>
                          <#assign shipmentMethodAndAmount = shippingRate.shipmentMethodTypeId + "@" + "UPS" + "*" + shippingRate.rate>
                          <input type='radio' name='shipmentMethodAndAmount' value='${shipmentMethodAndAmount?if_exists}'>
                          UPS&nbsp;${shippingRate.shipmentMethodDescription?if_exists}
                          <#if (shippingRate.rate > -1)>
                            <@ofbizCurrency amount=shippingRate.rate isoCode=orderReadHelper.getCurrency()/>
                          <#else>
                            ${uiLabelMap.OrderCalculatedOffline} 
                          </#if>
                        </td>
                      </tr>
                    </#list>
                    <input type="hidden" name="shipmentRouteSegmentId" value="${shipmentRouteSegmentId?if_exists}"/>
                    <input type="hidden" name="shipmentId" value="${pickedShipmentId?if_exists}"/>
                    <input type="hidden" name="orderAdjustmentId" value="${orderAdjustmentId?if_exists}"/>
                    <input type="hidden" name="orderId" value="${orderId?if_exists}"/>
                    <input type="hidden" name="shipGroupSeqId" value="${shipGroup.shipGroupSeqId?if_exists}"/>
                    <input type="hidden" name="contactMechPurposeTypeId" value="SHIPPING_LOCATION"/>
                    <input type="hidden" name="oldContactMechId" value="${shipGroup.contactMechId?if_exists}"/>
                    <input type="hidden" name="shippingAmount" value="${shippingAmount?if_exists}"/>
                    <tr>
                      <td valign="top" width="80%">
                        <input type="submit" value="${uiLabelMap.CommonUpdate}" class="smallSubmit"/>
                      </td>
                    </tr>
                  </form>
                </table>
              </td>
            </tr>
          </#if>
        </#if>

        <#-- tracking number -->
        <#if shipGroup.trackingNumber?has_content || orderShipmentInfoSummaryList?has_content>
          <tr><td colspan="3"><hr /></td></tr>
          <tr>
            <td align="right" valign="top" width="15%">
              <span class="label">&nbsp;${uiLabelMap.OrderTrackingNumber}</span>
            </td>
            <td width="5">&nbsp;</td>
            <td valign="top" width="80%">
              <#-- TODO: add links to UPS/FEDEX/etc based on carrier partyId  -->
              <#if shipGroup.trackingNumber?has_content>
                ${shipGroup.trackingNumber}
              </#if>
              <#if orderShipmentInfoSummaryList?has_content>
                <#list orderShipmentInfoSummaryList as orderShipmentInfoSummary>
                  <#if orderShipmentInfoSummary.shipGroupSeqId?if_exists == shipGroup.shipGroupSeqId?if_exists>
                    <div>
                      <#if (orderShipmentInfoSummaryList?size > 1)>${orderShipmentInfoSummary.shipmentPackageSeqId}: </#if>
                      ${uiLabelMap.CommonIdCode}: ${orderShipmentInfoSummary.trackingCode?default("[${uiLabelMap.OrderNotYetKnown}]")}
                      <#if orderShipmentInfoSummary.boxNumber?has_content> ${uiLabelMap.ProductBox} #${orderShipmentInfoSummary.boxNumber}</#if>
                      <#if orderShipmentInfoSummary.carrierPartyId?has_content>(${uiLabelMap.ProductCarrier}: ${orderShipmentInfoSummary.carrierPartyId})</#if>
                    </div>
                  </#if>
                </#list>
              </#if>
            </td>
          </tr>
        </#if>
        <#if shipGroup.maySplit?has_content && noShipment?default("false") != "true">
          <tr><td colspan="3"><hr /></td></tr>
          <tr>
            <td align="right" valign="top" width="15%">
              <span class="label">&nbsp;${uiLabelMap.OrderSplittingPreference}</span>
            </td>
            <td width="5">&nbsp;</td>
            <td valign="top" width="80%">
              <div>
                <#if shipGroup.maySplit?upper_case == "N">
                    ${uiLabelMap.FacilityWaitEntireOrderReady}
                    <#if security.hasEntityPermission("ORDERMGR", "_UPDATE", session)>
                      <#if orderHeader.statusId != "ORDER_COMPLETED" && orderHeader.statusId != "ORDER_CANCELLED">
                        <form name="allowordersplit_${shipGroup.shipGroupSeqId}" method="post" action="<@ofbizUrl>allowordersplit</@ofbizUrl>">
                          <input type="hidden" name="orderId" value="${orderId}"/>
                          <input type="hidden" name="shipGroupSeqId" value="${shipGroup.shipGroupSeqId}"/>
                        </form>
                        <a href="javascript:document.allowordersplit_${shipGroup.shipGroupSeqId}.submit()" class="buttontext">${uiLabelMap.OrderAllowSplit}</a>
                      </#if>
                    </#if>
                <#else>
                    ${uiLabelMap.FacilityShipAvailable}
                </#if>
              </div>
            </td>
          </tr>
        </#if>

        <tr><td colspan="7"><hr class="sepbar"></td></tr>
        <tr>
          <td align="right" valign="top" width="15%">
            <span class="label">&nbsp;${uiLabelMap.OrderInstructions}</span>
          </td>
          <td width="5">&nbsp;</td>
          <td align="left" valign="top" width="80%">
            <#if (!orderHeader.statusId.equals("ORDER_COMPLETED")) && !(orderHeader.statusId.equals("ORDER_REJECTED")) && !(orderHeader.statusId.equals("ORDER_CANCELLED"))>
              <form name="updateShippingInstructionsForm" method="post" action="<@ofbizUrl>setShippingInstructions</@ofbizUrl>">
                <input type="hidden" name="orderId" value="${orderHeader.orderId}"/>
                <input type="hidden" name="shipGroupSeqId" value="${shipGroup.shipGroupSeqId}"/>
                <#if shipGroup.shippingInstructions?has_content>
                  <table>
                    <tr>
                      <td id="instruction">
                        <label>${shipGroup.shippingInstructions}</label>
                      </td>
                      <td>  
                        <a href="javascript:editInstruction();" class="buttontext" id="editInstruction">${uiLabelMap.CommonEdit}</a>
                      </td>
                    </tr>
                  </table>
                <#else>
                  <a href="javascript:addInstruction();" class="buttontext" id="addInstruction">${uiLabelMap.CommonAdd}</a>
                </#if>
                <a href="javascript:saveInstruction();" class="buttontext" id="saveInstruction" style="display:none">${uiLabelMap.CommonSave}</a>
                <textarea name="shippingInstructions" id="shippingInstructions" style="display:none">${shipGroup.shippingInstructions?if_exists}</textarea>
              </form>
            <#else>
              <#if shipGroup.shippingInstructions?has_content>
                <span>${shipGroup.shippingInstructions}</span>
              <#else>
                <span>${uiLabelMap.OrderThisOrderDoesNotHaveShippingInstructions}</span>
              </#if>
            </#if>
          </td>
        </tr>

        <#if shipGroup.isGift?has_content && noShipment?default("false") != "true">
          <form name="setGiftMessageForm" method="post" action="<@ofbizUrl>setGiftMessage</@ofbizUrl>">
            <input type="hidden" name="orderId" value="${orderHeader.orderId}"/>
            <input type="hidden" name="shipGroupSeqId" value="${shipGroup.shipGroupSeqId}"/>
            <tr><td colspan="3"><hr /></td></tr>
            <td align="right" valign="top" width="15%">
              <span class="label">&nbsp;${uiLabelMap.OrderGiftMessage}</span>
            </td>
            <#if shipGroup.giftMessage?has_content>
              <td>&nbsp;</td>
              <td id="message" colspan="3" >
                <label>${shipGroup.giftMessage}</label>
                <a href="javascript:editGiftMessage();" class="buttontext" id="editGiftMessage">${uiLabelMap.CommonEdit}</a>
              </td>
            <#else>
              <td align="right"><a href="javascript:addGiftMessage();" class="buttontext" id="addGiftMessage">${uiLabelMap.CommonAdd}</a></td>
            </#if>
            <td>
              <textarea name="giftMessage" id="giftMessage" style="display:none">${shipGroup.giftMessage?if_exists}</textarea>
              <a href="javascript:saveGiftMessage();" class="buttontext" id="saveGiftMessage" style="display:none">${uiLabelMap.CommonSave}</a>
            </td>
          </form>
        </#if>
        <form name="setShipGroupDates_${shipGroup.shipGroupSeqId}" method="post" action="<@ofbizUrl>updateOrderItemShipGroup</@ofbizUrl>">
        <input type="hidden" name="orderId" value="${orderHeader.orderId}"/>
        <input type="hidden" name="shipGroupSeqId" value="${shipGroup.shipGroupSeqId}"/>
         <tr><td colspan="3"><hr /></td></tr>
         <tr>
            <td align="right" valign="top" width="15%">
              <span class="label">&nbsp;${uiLabelMap.OrderShipAfterDate}<span>
            </td>
            <td width="5">&nbsp;</td>
            <td valign="top" width="80%">
              <input type="text" size="23" name="shipAfterDate" value="${shipGroup.shipAfterDate?if_exists}"/>
              <a href="javascript:call_cal(document.setShipGroupDates_${shipGroup.shipGroupSeqId}.shipAfterDate,'');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="${uiLabelMap.OrderCalendarClickHereForCalendar}"/></a>
            </td>
         </tr>
         <tr>
            <td align="right" valign="top" width="15%">
              <span class="label">&nbsp;${uiLabelMap.OrderShipBeforeDate}<span>
            </td>
            <td width="5">&nbsp;</td>
            <td valign="top" width="80%">
              <input type="text" size="23" name="shipByDate" value="${shipGroup.shipByDate?if_exists}"/>
              <a href="javascript:call_cal(document.setShipGroupDates_${shipGroup.shipGroupSeqId}.shipByDate,'');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="${uiLabelMap.OrderCalendarClickHereForCalendar}"/></a>
              <input type="submit" value="${uiLabelMap.CommonUpdate}"/>
            </td>
         </tr>
        </form>
       <#assign shipGroupShipments = shipGroup.getRelated("PrimaryShipment")>
       <#if shipGroupShipments?has_content>
          <tr><td colspan="3"><hr /></td></tr>
          <tr>
            <td align="right" valign="top" width="15%">
              <span class="label">&nbsp;${uiLabelMap.FacilityShipments}<span>
            </td>
            <td width="5">&nbsp;</td>
            <td valign="top" width="80%">
                <#list shipGroupShipments as shipment>
                    <div>
                      ${uiLabelMap.CommonNbr}<a href="/facility/control/ViewShipment?shipmentId=${shipment.shipmentId}&amp;externalLoginKey=${externalLoginKey}" class="buttontext">${shipment.shipmentId}</a>&nbsp;&nbsp;
                      <a href="/facility/control/PackingSlip.pdf?shipmentId=${shipment.shipmentId}&amp;externalLoginKey=${externalLoginKey}" class="buttontext">${uiLabelMap.ProductPackingSlip}</a>
                      <#if "SALES_ORDER" == orderHeader.orderTypeId && "ORDER_COMPLETED" == orderHeader.statusId>
                        <#assign shipmentRouteSegments = delegator.findByAnd("ShipmentRouteSegment", {"shipmentId" : shipment.shipmentId})>
                        <#if shipmentRouteSegments?has_content>
                          <#assign shipmentRouteSegment = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(shipmentRouteSegments)>
                          <#if "UPS" == (shipmentRouteSegment.carrierPartyId)?if_exists>
                            <a href="javascript:document.upsEmailReturnLabel${shipment_index}.submit();" class="buttontext">${uiLabelMap.ProductEmailReturnShippingLabelUPS}</a>
                          </#if>
                          <form name="upsEmailReturnLabel${shipment_index}" method="post" action="<@ofbizUrl>upsEmailReturnLabelOrder</@ofbizUrl>">
                            <input type="hidden" name="orderId" value="${orderId}"/>
                            <input type="hidden" name="shipmentId" value="${shipment.shipmentId}"/>
                            <input type="hidden" name="shipmentRouteSegmentId" value=${shipmentRouteSegment.shipmentRouteSegmentId}>
                          </form>
                        </#if>
                      </#if>
                    </div>
                </#list>
            </td>
          </tr>
       </#if>

       <#-- shipment actions -->
       <#if security.hasEntityPermission("ORDERMGR", "_UPDATE", session) && ((orderHeader.statusId == "ORDER_CREATED") || (orderHeader.statusId == "ORDER_APPROVED") || (orderHeader.statusId == "ORDER_SENT"))>


         <#-- Manual shipment options -->
         <tr><td colspan="3"><hr /></td></tr>
         <tr>
            <td colspan="3" valign="top" width="100%" align="center">
             <#if orderHeader.orderTypeId == "SALES_ORDER">
               <#if !shipGroup.supplierPartyId?has_content>
                 <#if orderHeader.statusId == "ORDER_APPROVED">
                 <a href="/facility/control/PackOrder?facilityId=${storeFacilityId?if_exists}&amp;orderId=${orderId}&amp;shipGroupSeqId=${shipGroup.shipGroupSeqId}&amp;externalLoginKey=${externalLoginKey}" class="buttontext">${uiLabelMap.OrderPackShipmentForShipGroup}</a>
                 <br />
                 </#if>
                 <a href="javascript:document.createShipment_${shipGroup.shipGroupSeqId}.submit()" class="buttontext">${uiLabelMap.OrderNewShipmentForShipGroup}</a>
                 <form name="createShipment_${shipGroup.shipGroupSeqId}" method="post" action="/facility/control/createShipment">
                   <input type="hidden" name="primaryOrderId" value="${orderId}"/>
                   <input type="hidden" name="primaryShipGroupSeqId" value="${shipGroup.shipGroupSeqId}"/>
                   <input type="hidden" name="statusId" value="SHIPMENT_INPUT">
                   <input type="hidden" name="facilityId" value=${storeFacilityId?if_exists}>
                   <input type="hidden" name="estimatedShipDate" value="${shipGroup.shipByDate?if_exists}"/>
                 </form>
               </#if>
             <#else>
               <#assign facilities = facilitiesForShipGroup.get(shipGroup.shipGroupSeqId)>
               <#if facilities?has_content>
                   <div>
                    <form name="createShipment2_${shipGroup.shipGroupSeqId}" method="post" action="/facility/control/createShipment">
                       <input type="hidden" name="primaryOrderId" value="${orderId}"/>
                       <input type="hidden" name="primaryShipGroupSeqId" value="${shipGroup.shipGroupSeqId}"/>
                       <input type="hidden" name="shipmentTypeId" value="PURCHASE_SHIPMENT"/>
                       <input type="hidden" name="statusId" value="PURCH_SHIP_CREATED"/>
                       <input type="hidden" name="externalLoginKey" value="${externalLoginKey}"/>
                       <input type="hidden" name="estimatedShipDate" value="${shipGroup.estimatedShipDate?if_exists}"/>
                       <input type="hidden" name="estimatedArrivalDate" value="${shipGroup.estimatedDeliveryDate?if_exists}"/>
                       <select name="destinationFacilityId">
                         <#list facilities as facility>
                           <option value="${facility.facilityId}">${facility.facilityName}</option>
                         </#list>
                       </select>
                       <input type="submit" class="smallSubmit" value="${uiLabelMap.OrderNewShipmentForShipGroup} [${shipGroup.shipGroupSeqId}]"/>
                    </form>
                    </div>
               <#else>
                   <a href="javascript:document.quickDropShipOrder_${shipGroup_index}.submit();" class="buttontext">${uiLabelMap.ProductShipmentQuickComplete}</a>
                   <a href="javascript:document.createShipment3_${shipGroup.shipGroupSeqId}.submit();" class="buttontext">${uiLabelMap.OrderNewDropShipmentForShipGroup} [${shipGroup.shipGroupSeqId}]</a>
                   <form name="quickDropShipOrder_${shipGroup_index}" method="post" action="<@ofbizUrl>quickDropShipOrder</@ofbizUrl>">
                        <input type="hidden" name="orderId" value="${orderId}"/>
                        <input type="hidden" name="shipGroupSeqId" value="${shipGroup.shipGroupSeqId}"/>
                        <input type="hidden" name="externalLoginKey" value="${externalLoginKey}">
                    </form>
                    <form name="createShipment3_${shipGroup.shipGroupSeqId}" method="post" action="/facility/control/createShipment">
                        <input type="hidden" name="primaryOrderId" value="${orderId}"/>
                        <input type="hidden" name="primaryShipGroupSeqId" value="${shipGroup.shipGroupSeqId}"/>
                        <input type="hidden" name="shipmentTypeId" value="DROP_SHIPMENT">
                        <input type="hidden" name="statusId" value="PURCH_SHIP_CREATED">
                        <input type="hidden" name="externalLoginKey" value="${externalLoginKey}">
                    </form>
               </#if>
             </#if>
            </td>
         </tr>

       </#if>

      </table>
    </div>
</div>
</#list>
</#if>
