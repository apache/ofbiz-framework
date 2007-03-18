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
<table border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
<tr>
    <td width='100%'>
      <br/>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
        <tr>
          <td>          
            <form method="post" action="<@ofbizUrl>finalizeOrder</@ofbizUrl>" name="checkoutsetupform">
              <input type="hidden" name="finalizeMode" value="options"/>
<#list 1..cart.getShipGroupSize() as currIndex>
<#assign shipGroupIndex = currIndex - 1>

<#if cart.getShipmentMethodTypeId(shipGroupIndex)?exists && cart.getCarrierPartyId(shipGroupIndex)?exists>
    <#assign chosenShippingMethod = cart.getShipmentMethodTypeId(shipGroupIndex) + '@' + cart.getCarrierPartyId(shipGroupIndex)>
</#if>
<#assign supplierPartyId = cart.getSupplierPartyId(shipGroupIndex)?if_exists>
<#assign supplier =  delegator.findByPrimaryKey("PartyGroup", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", supplierPartyId))?if_exists />
<hr class="sepbar"/>
              <table width="100%" cellpadding="1" border="0" cellpadding="0" cellspacing="0">
              <tr>
                <td colspan="2">
                    <div class="head1"><b>${uiLabelMap.OrderShipGroup} # ${currIndex}</b><#if supplier?has_content> - ${supplier.groupName?default(supplier.partyId)}</#if></div>
                </td>
              </tr>

               <#if cart.getOrderType() != "PURCHASE_ORDER">
                <#assign shipEstimateWrapper = Static["org.ofbiz.order.shoppingcart.shipping.ShippingEstimateWrapper"].getWrapper(dispatcher, cart, 0)>
                <#assign carrierShipmentMethods = shipEstimateWrapper.getShippingMethods()>
                <#list carrierShipmentMethods as carrierShipmentMethod>
                <tr>
                  <td width='1%' valign="top" >
                    <#assign shippingMethod = carrierShipmentMethod.shipmentMethodTypeId + "@" + carrierShipmentMethod.partyId>
                    <input type='radio' name='${shipGroupIndex?default("0")}_shipping_method' value='${shippingMethod}' <#if shippingMethod == chosenShippingMethod?default("N@A")>checked</#if>>       
                  </td>
                  <td valign="top">                            
                    <div class='tabletext'>                                                 
                      <#if carrierShipmentMethod.partyId != "_NA_">${carrierShipmentMethod.partyId?if_exists}&nbsp;</#if>${carrierShipmentMethod.description?if_exists}
                      <#if cart.getShippingContactMechId(shipGroupIndex)?exists>
                        <#assign shippingEst = shipEstimateWrapper.getShippingEstimate(carrierShipmentMethod)?default(-1)>
                        <#if shippingEst?has_content>
                          &nbsp;-&nbsp;
                          <#if (shippingEst > -1)>
                            <@ofbizCurrency amount=shippingEst isoCode=cart.getCurrency()/>
                          <#else>
                            Calculated Offline
                          </#if>
                        </#if>
                      </#if>
                    </div>                           
                  </td>
                </tr>
                </#list>
                <#if !carrierShipmentMethodList?exists || carrierShipmentMethodList?size == 0>                     
                <tr>
                  <td width='1%' valign="top">
                    <input type='radio' name='${shipGroupIndex?default("0")}_shipping_method' value="Default" checked>
                  </td>
                  <td valign="top">
                    <div class='tabletext'>${uiLabelMap.FacilityNoOtherShippingMethods}</div>
                  </td>
                </tr>
                </#if>
                <tr><td colspan='2'><hr class='sepbar'></td></tr>                      
               <#else>
                   <input type='hidden' name='${shipGroupIndex?default("0")}_shipping_method' value="NO_SHIPPING@_NA_">
               </#if>
                <tr>
                  <td colspan='2'>
                    <div class="head2"><b>${uiLabelMap.FacilityShipOnceOrAvailable}</b></div>
                  </td>
                </tr>
                <tr>
                  <td valign="top">
                    <input type='radio' <#if cart.getMaySplit(shipGroupIndex)?default("N") == "N">checked</#if> name='${shipGroupIndex?default("0")}_may_split' value='false'>
                  </td>
                  <td valign="top">
                    <div class="tabletext">${uiLabelMap.FacilityWaitEntireOrderReady}</div>
                  </td>
                </tr>
                <tr>
                  <td valign="top">
                    <input <#if cart.getMaySplit(shipGroupIndex)?default("N") == "Y">checked</#if> type='radio' name='${shipGroupIndex?default("0")}_may_split' value='true'>
                  </td>
                  <td valign="top">
                    <div class="tabletext">${uiLabelMap.FacilityShipAvailable}</div>
                  </td>
                </tr>
                <tr><td colspan="2"><hr class='sepbar'></td></tr>
                <tr>
                  <td colspan="2">
                    <div class="head2"><b>${uiLabelMap.FacilitySpecialInstructions}</b></div>
                  </td>
                </tr>
                <tr>
                  <td colspan="2">
                    <textarea class='textAreaBox' cols="30" rows="3" name="${shipGroupIndex?default("0")}_shipping_instructions">${cart.getShippingInstructions(shipGroupIndex)?if_exists}</textarea>
                  </td>
                </tr>
                <tr>
                  <td colspan="2">
                    <#if cart.getOrderType() = "PURCHASE_ORDER">
                       <input type='hidden' name='${shipGroupIndex?default("0")}_is_gift' value='false'>
                    <#else>
                    <div>
                      <span class="head2"><b>${uiLabelMap.OrderIsThisGift}</b></span>
                      <input type='radio' <#if cart.getIsGift(shipGroupIndex)?default("Y") == "Y">checked</#if> name='${shipGroupIndex?default("0")}_is_gift' value='true'><span class='tabletext'>${uiLabelMap.CommonYes}</span>
                      <input type='radio' <#if cart.getIsGift(shipGroupIndex)?default("N") == "N">checked</#if> name='${shipGroupIndex?default("0")}_is_gift' value='false'><span class='tabletext'>${uiLabelMap.CommonNo}</span>
                    </div>
                    </#if>
                  </td>
                </tr>
                <#if cart.getOrderType() != "PURCHASE_ORDER">
                <tr><td colspan="2"><hr class='sepbar'></td></tr>
                <tr>
                  <td colspan="2">
                    <div class="head2"><b>${uiLabelMap.OrderGiftMessage}</b></div>
                  </td>
                </tr>
                <tr>
                  <td colspan="2">
                    <textarea class='textAreaBox' cols="30" rows="3" name="${shipGroupIndex?default("0")}_gift_message">${cart.getGiftMessage(shipGroupIndex)?if_exists}</textarea>
                  </td>
                </tr>
                 </#if>
                   <tr>
                      <td colspan="2"></td>
                   </tr>
              </table>
</#list>
            </form>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>

<br/>
<#else>
  <h3>${uiLabelMap.OrderViewPermissionError}</h3>
</#if>
