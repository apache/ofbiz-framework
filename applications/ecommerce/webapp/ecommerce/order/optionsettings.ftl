<#--
 *  Copyright (c) 2003-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@version    $Rev$
 *@since      3.0
-->

<div class="screenlet">
    <div class="screenlet-header">
        <div style="float: right;">
            <div class="tabletext">
              ${screens.render(anonymoustrailScreen)}
            </div>
        </div>
        <div class='boxhead'>&nbsp;${uiLabelMap.OrderShippingInformation}</div>
    </div>
    <div class="screenlet-body">
        <form method="post" action="<@ofbizUrl>finalizeOrder</@ofbizUrl>" name="optsetupform">
          <input type="hidden" name="finalizeMode" value="options"/>
          <table width="100%" cellpadding="1" border="0" cellpadding="0" cellspacing="0">
            <#list carrierShipmentMethodList as carrierShipmentMethod>
            <tr>
              <td width='1%' valign="top" >
                <#assign shippingMethod = carrierShipmentMethod.shipmentMethodTypeId + "@" + carrierShipmentMethod.partyId>
                <input type='radio' name='shipping_method' value='${shippingMethod}' <#if shippingMethod == chosenShippingMethod?default("N@A")>checked</#if>>
              </td>
              <td valign="top">
                <div class='tabletext'>
                          <#if shoppingCart.getShippingContactMechId()?exists>
                            <#assign shippingEst = shippingEstWpr.getShippingEstimate(carrierShipmentMethod)?default(-1)>
                          </#if>
                          <#if carrierShipmentMethod.partyId != "_NA_">${carrierShipmentMethod.partyId?if_exists}&nbsp;</#if>${carrierShipmentMethod.description?if_exists}
                          <#if shippingEst?has_content> - <#if (shippingEst > -1)?exists><@ofbizCurrency amount=shippingEst isoCode=shoppingCart.getCurrency()/><#else>${uiLabelMap.OrderCalculatedOffline}</#if></#if>
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
                 <input type='radio' <#if shoppingCart.getMaySplit()?default("N") == "N">checked</#if> name='may_split' value='false'>
              </td>
              <td valign="top">
                <div class="tabletext">${uiLabelMap.OrderPleaseWaitUntilBeforeShipping}.</div>
              </td>
            </tr>
            <tr>
              <td valign="top">
                 <input <#if shoppingCart.getMaySplit()?default("N") == "Y">checked</#if> type='radio' name='may_split' value='true'>
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
                <input type="text" class='inputBox' name="corresponding_po_id" size="15" value='${shoppingCart.getPoNumber()?if_exists}'/>
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
