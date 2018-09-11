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

<#if security.hasEntityPermission("ORDERMGR", "_CREATE", session) || security.hasEntityPermission("ORDERMGR", "_PURCHASE_CREATE", session)>
<form method="post" action="<@ofbizUrl>finalizeOrder</@ofbizUrl>" name="checkoutsetupform">
<table border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
<tr>
    <td width='100%'>
      <br />
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
        <tr>
          <td>
            <table width="100%" cellpadding="1" border="0" cellpadding="0" cellspacing="0">
              <tr>
                <td colspan="2">
                  <h2>${uiLabelMap.OrderInternalNote}</h2>
                </td>
                <td colspan="2">
                  <h2>${uiLabelMap.OrderShippingNotes}</h2>
                </td>
              </tr>
              <tr>
                <td colspan="2">
                  <textarea cols="30" rows="3" name="internal_order_notes"><#if (cart.getInternalOrderNotes().size()>0)>${(cart.getInternalOrderNotes()[0])!}</#if></textarea>
                </td>
                <td colspan="2">
                  <textarea cols="30" rows="3" name="shippingNotes"><#if (cart.getOrderNotes().size()>0)>${(cart.getOrderNotes()[0])!}</#if></textarea>
                </td>
              </tr>
            </table>
          </td>
        </tr>
        <tr>
          <td>
              <input type="hidden" name="finalizeMode" value="options"/>
<#list 1..cart.getShipGroupSize() as currIndex>
<#assign shipGroupIndex = currIndex - 1>

<#if cart.getShipmentMethodTypeId(shipGroupIndex)?? && cart.getCarrierPartyId(shipGroupIndex)??>
    <#assign chosenShippingMethod = cart.getShipmentMethodTypeId(shipGroupIndex) + '@' + cart.getCarrierPartyId(shipGroupIndex)>
</#if>
<#assign supplierPartyId = cart.getSupplierPartyId(shipGroupIndex)!>
<#assign supplier =  delegator.findOne("PartyGroup", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("partyId", supplierPartyId), false)! />

              <table width="100%" cellpadding="1" border="0" cellpadding="0" cellspacing="0">
              <tr><td colspan="2"><hr /></td></tr>
              <tr>
                <td colspan="2">
                    <h1><b>${uiLabelMap.OrderShipGroup} ${uiLabelMap.CommonNbr} ${currIndex}</b><#if supplier?has_content> - ${supplier.groupName?default(supplier.partyId)}</#if></h1>
                </td>
              </tr>
               <#if cart.getOrderType() != "PURCHASE_ORDER">
                <tr>
                  <td colspan="2">
                    <h2>${uiLabelMap.ProductShipmentMethod}</h2>
                  </td>
                </tr>
                <#assign shipEstimateWrapper = Static["org.apache.ofbiz.order.shoppingcart.shipping.ShippingEstimateWrapper"].getWrapper(dispatcher, cart, 0)>
                <#assign carrierShipmentMethods = shipEstimateWrapper.getShippingMethods()>
                <#list carrierShipmentMethods as carrierShipmentMethod>
                <tr>
                  <td width='1%' valign="top" >
                    <#assign shippingMethod = carrierShipmentMethod.shipmentMethodTypeId + "@" + carrierShipmentMethod.partyId>
                    <input type='radio' name='${shipGroupIndex?default("0")}_shipping_method' value='${shippingMethod}'<#if shippingMethod == chosenShippingMethod?default("N@A")> checked="checked"</#if> id='${shipGroupIndex?default("0")}_shipping_method_${shippingMethod}' />
                    <label for="${shipGroupIndex?default("0")}_shipping_method_${shippingMethod}">
                      <#if carrierShipmentMethod.partyId != "_NA_">${carrierShipmentMethod.partyId!}&nbsp;</#if>${carrierShipmentMethod.description!}
                      <#if cart.getShippingContactMechId(shipGroupIndex)??>
                        <#assign shippingEst = shipEstimateWrapper.getShippingEstimate(carrierShipmentMethod)?default(-1)>
                        <#assign shippingTimeEstimateInDay = shipEstimateWrapper.getShippingTimeEstimateInDay(carrierShipmentMethod)?default(-1)>
                        <#if shippingEst?has_content>
                          &nbsp;-&nbsp;
                          <#if (shippingEst > -1)>
                            <@ofbizCurrency amount=shippingEst isoCode=cart.getCurrency()/>
                          <#else>
                            Calculated Offline
                          </#if>
                          <#if (shippingTimeEstimateInDay > 0)> - ${shippingTimeEstimateInDay} ${uiLabelMap.CommonDays}</#if>
                        </#if>
                      </#if>
                    </label>
                  </td>
                </tr>
                </#list>
                <#if !carrierShipmentMethodList?? || carrierShipmentMethodList?size == 0>
                <tr>
                  <td valign="top">
                  <label>
                    <input type='radio' name='${shipGroupIndex?default("0")}_shipping_method' value="Default" checked="checked" />
                    ${uiLabelMap.FacilityNoOtherShippingMethods}
                  </label>
                  </td>
                </tr>
                </#if>
               <#else>
                   <tr>
                     <td>
                       <h2>${uiLabelMap.OrderOrderShipEstimate}</h2>
                     </td>
                   </tr>
                   <tr>
                     <input type='hidden' name='${shipGroupIndex?default("0")}_shipping_method' value="STANDARD@_NA_" />
                     <td>
                       <input type='text' name='${shipGroupIndex?default("0")}_ship_estimate' value="${cart.getItemShipGroupEstimate(shipGroupIndex?default('0'))!}"/>
                     </td>
                   </tr>
               </#if>
                <tr>
                  <td colspan='2'>
                    <h2>${uiLabelMap.FacilityShipOnceOrAvailable}</h2>
                  </td>
                </tr>
                <tr>
                  <td valign="top">
                  <label>
                    <input type='radio' <#if "N" == cart.getMaySplit(shipGroupIndex)?default("N")>checked="checked"</#if> name='${shipGroupIndex?default("0")}_may_split' value='false' />
                    ${uiLabelMap.FacilityWaitEntireOrderReady}
                  </label>
                  </td>
                </tr>
                <tr>
                  <td valign="top">
                    <label>
                    <input <#if "Y" == cart.getMaySplit(shipGroupIndex)?default("N")>checked="checked"</#if> type='radio' name='${shipGroupIndex?default("0")}_may_split' value='true' />
                    ${uiLabelMap.FacilityShipAvailable}
                    </label>
                  </td>
                </tr>
                <tr>
                  <td colspan="2">
                    <h2>${uiLabelMap.OrderShipBeforeDate}</h2>
                  </td>
                </tr>
                <tr>
                    <td colspan="2">
                    <div>
                      <@htmlTemplate.renderDateTimeField name="sgi${shipGroupIndex?default('0')}_shipBeforeDate" event="" action="" value="${(cart.getShipBeforeDate(shipGroupIndex))!}" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" size="25" maxlength="30" id="sgi${shipGroupIndex?default('0')}_shipBeforeDate" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                    </div>
                    </td>
                </tr>
                <tr>
                  <td colspan="2">
                    <h2>${uiLabelMap.OrderShipAfterDate}</h2>
                  </td>
                </tr>
                <tr>
                    <td colspan="2">
                    <div>
                      <@htmlTemplate.renderDateTimeField name="sgi${shipGroupIndex?default('0')}_shipAfterDate" event="" action="" value="${(cart.getShipAfterDate(shipGroupIndex))!}" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" size="25" maxlength="30" id="sgi${shipGroupIndex?default('0')}_shipAfterDate" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                    </div>
                    </td>
                </tr>
                <tr>
                  <td colspan="2">
                    <h2>${uiLabelMap.FacilitySpecialInstructions}</h2>
                  </td>
                </tr>
                <tr>
                  <td colspan="2">
                    <textarea cols="30" rows="3" name="${shipGroupIndex?default("0")}_shipping_instructions">${cart.getShippingInstructions(shipGroupIndex)!}</textarea>
                  </td>
                </tr>

                <#if cart.getOrderType() == 'PURCHASE_ORDER'>
                    <input type="hidden" name="${shipGroupIndex?default('0')}_is_gift" value="false" />
                <#else>
                    <#if (productStore.showCheckoutGiftOptions)?default('Y') != 'N'>
                        <tr>
                            <td colspan="2">
                                <div>
                                    <span class="h2"><b>${uiLabelMap.OrderIsThisGift}</b></span>
                                    <label><input type="radio" <#if cart.getIsGift(shipGroupIndex)?default('Y') == 'Y'>checked="checked"</#if> name="${shipGroupIndex?default('0')}_is_gift" value="true" /><span>${uiLabelMap.CommonYes}</span></label>
                                    <label><input type="radio" <#if cart.getIsGift(shipGroupIndex)?default('N') == 'N'>checked="checked"</#if> name="${shipGroupIndex?default('0')}_is_gift" value="false" /><span>${uiLabelMap.CommonNo}</span></label>
                                </div>
                            </td>
                        </tr>
                    </#if>
                    <tr>
                        <td colspan="2">
                            <h2>${uiLabelMap.OrderGiftMessage}</h2>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2">
                            <textarea cols="30" rows="3" name="${shipGroupIndex?default('0')}_gift_message">${cart.getGiftMessage(shipGroupIndex)!}</textarea>
                        </td>
                    </tr>
                </#if>

                   <tr>
                      <td colspan="2"></td>
                   </tr>
              </table>
</#list>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>
</form>
<br />
<#else>
  <h3>${uiLabelMap.OrderViewPermissionError}</h3>
</#if>
