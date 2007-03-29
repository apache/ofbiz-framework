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

<div class="screenlet">
    <div class="screenlet-header">
        <div class='boxhead'>&nbsp;${uiLabelMap.OrderShippingInformation}</div>
    </div>
    <div class="screenlet-body">
        <form method="post" action="<@ofbizUrl>processShipOptions</@ofbizUrl>" name="${parameters.formNameValue}">
          <input type="hidden" name="finalizeMode" value="options"/>
          <table width="100%" cellpadding="1" border="0" cellpadding="0" cellspacing="0">
            <#list carrierShipmentMethodList as carrierShipmentMethod>
            <tr>
              <td width='1%' valign="top" >
                <#assign shippingMethod = carrierShipmentMethod.shipmentMethodTypeId + "@" + carrierShipmentMethod.partyId>
                <input type='radio' name='shipping_method' value='${shippingMethod}' <#if shippingMethod == chosenShippingMethod?default("N@A")>checked</#if>/>
              </td>
              <td valign="top">
                <div class='tabletext'>
                    <#if shoppingCart.getShippingContactMechId()?exists>
                        <#assign shippingEst = shippingEstWpr.getShippingEstimate(carrierShipmentMethod)?default(-1)>
                    </#if>
                    <#if carrierShipmentMethod.partyId != "_NA_">${carrierShipmentMethod.partyId?if_exists}&nbsp;</#if>${carrierShipmentMethod.description?if_exists}
                    <#if shippingEst?has_content> - <#if (shippingEst > -1)><@ofbizCurrency amount=shippingEst isoCode=shoppingCart.getCurrency()/><#else>${uiLabelMap.OrderCalculatedOffline}</#if></#if>
                </div>
              </td>
            </tr>
            </#list>
            <#if !carrierShipmentMethodList?exists || carrierShipmentMethodList?size == 0>
            <tr>
              <td width='1%' valign="top">
                <input type='radio' name='shipping_method' value="Default" checked="checked"/>
              </td>
              <td valign="top">
                <div class='tabletext'>${uiLabelMap.OrderUseDefault}.</div>
              </td>
            </tr>
            </#if>
            <tr><td colspan='2'><hr class="sepbar"/></td></tr>
            <tr>
              <td colspan='2'>
                <div class="head2"><b>${uiLabelMap.OrderShipAllAtOnce}?</b></div>
              </td>
            </tr>
            <tr>
              <td valign="top">
                 <input type='radio' <#if shoppingCart.getMaySplit()?default("N") == "N">checked</#if> name='may_split' value='false'/>
              </td>
              <td valign="top">
                <div class="tabletext">${uiLabelMap.OrderPleaseWaitUntilBeforeShipping}.</div>
              </td>
            </tr>
            <tr>
              <td valign="top">
                 <input <#if shoppingCart.getMaySplit()?default("N") == "Y">checked</#if> type='radio' name='may_split' value='true'/>
              </td>
              <td valign="top">
                <div class="tabletext">${uiLabelMap.OrderPleaseShipItemsBecomeAvailable}.</div>
              </td>
            </tr>
            <tr><td colspan="2"><hr class="sepbar"/></td></tr>
            <tr>
              <td colspan="2">
                <div class="head2"><b>${uiLabelMap.OrderSpecialInstructions}</b></div>
              </td>
            </tr>
            <tr>
              <td colspan="2">
                <textarea class='textAreaBox' cols="30" rows="3" name="shipping_instructions">${shoppingCart.getShippingInstructions()?if_exists}</textarea>
              </td>
            </tr>
            <tr><td colspan="2"><hr class="sepbar"/></td></tr>
            <tr>
              <td colspan="2">
                <span class="head2"><b>${uiLabelMap.OrderPoNumber}</b></span>&nbsp;
                <input type="text" class='inputBox' name="correspondingPoId" size="15" value='${shoppingCart.getPoNumber()?if_exists}'/>
              </td>
            </tr>
            <#if productStore.showCheckoutGiftOptions?if_exists != "N">
            <tr><td colspan="2"><hr class="sepbar"/></td></tr>
            <tr>
              <td colspan="2">
                <div>
                  <span class="head2"><b>${uiLabelMap.OrderIsThisGift}?</b></span>
                  <input type='radio' <#if shoppingCart.getIsGift()?default("Y") == "Y">checked</#if> name='is_gift' value='true'/><span class='tabletext'>${uiLabelMap.CommonYes}</span>
                  <input type='radio' <#if shoppingCart.getIsGift()?default("N") == "N">checked</#if> name='is_gift' value='false'/><span class='tabletext'>${uiLabelMap.CommonNo}</span>
                </div>
              </td>
            </tr>
            <tr><td colspan="2"><hr class="sepbar"/></td></tr>
            <tr>
              <td colspan="2">
                <div class="head2"><b>${uiLabelMap.OrderGiftMessage}</b></div>
              </td>
            </tr>
            <tr>
              <td colspan="2">
                <textarea class='textAreaBox' cols="30" rows="3" name="gift_message">${shoppingCart.getGiftMessage()?if_exists}</textarea>
              </td>
            </tr>
            </#if>
            <tr>
              <td align="center" colspan="2">
                <input type="submit" class="smallsubmit" value="${uiLabelMap.CommonContinue}"/>
              </td>
            </tr>
          </table>
        </form>
    </div>
</div>
