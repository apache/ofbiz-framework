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

jQuery(document).ready(init);

function init() {
    var optForm = document.quickAnonOptSetupForm;
    document.getElementById("noShippingMethodSelectedError").innerHTML = "";
}

function aroundOptSubmitOrder(invocation) {
    var formToSubmit = document.quickAnonOptSetupForm;
    var shipMethodOption = "none";
    for (var i=0; i < formToSubmit.shipping_method.length; i++) {
        if (formToSubmit.shipping_method[i].checked){
            shipMethodOption = formToSubmit.shipping_method[i].value;
        }
    }
    if (shipMethodOption != "none") {
        jQuery.ajax({
            url: formToSubmit.action,
            type: "POST",
            data: jQuery("#quickAnonOptSetupForm").serialize(),
            success: function(data) {
               document.getElementById("optInfoSection").innerHTML = data;
            }
        });
    } else {
        document.getElementById("noShippingMethodSelectedError").innerHTML = "${uiLabelMap.EcommerceMessagePleaseSelectShippingMethod}";
    }
}

function eventTrigger (e) {
    if (! e)
        e = event;
    return e.target || e.srcElement;
}

function onClickShippingMethod(e) {
    var obj = eventTrigger (e);
    shippingMethodChanged(obj.value);
    return true;
}

</script>

<form id="quickAnonOptSetupForm" method="post" action="<@ofbizUrl>quickAnonProcessShipOptions</@ofbizUrl>" name="quickAnonOptSetupForm">
<div id="optInfoSection">
<table  width="100%" >
    <tr>
    <td><div class="screenlet">
        <table hight="100%" >
              <tr><td><div class="errorMessage" id="noShippingMethodSelectedError"></div></td></tr>
            <tr>
                <td>
                    <h2>${uiLabelMap.OrderMethod}</h2>
                </td>
            </tr>
            <#list carrierShipmentMethodList as carrierShipmentMethod>
            <tr>
                <td>
                    <div>
                         <#assign shippingMethod = carrierShipmentMethod.shipmentMethodTypeId + "@" + carrierShipmentMethod.partyId>
                         <input type="radio" onclick="return onClickShippingMethod(event)" name="shipping_method" value="${shippingMethod}" <#if shippingMethod == chosenShippingMethod?default("N@A")>checked="checked"</#if>/>
                         <#if shoppingCart.getShippingContactMechId()??>
                             <#assign shippingEst = shippingEstWpr.getShippingEstimate(carrierShipmentMethod)?default(-1)>
                         </#if>
                         <#if carrierShipmentMethod.partyId != "_NA_">${carrierShipmentMethod.partyId!}&nbsp;</#if>${carrierShipmentMethod.description!}
                         <#if shippingEst?has_content> - <#if (shippingEst > -1)><@ofbizCurrency amount=shippingEst isoCode=shoppingCart.getCurrency()/><#else>${uiLabelMap.OrderCalculatedOffline}</#if></#if>
                    </div>
                </td>
            </tr>
            </#list>
            <#if !carrierShipmentMethodList?? || carrierShipmentMethodList?size == 0>
            <tr>
              <td width="1%" valign="top">
                <div><input type="radio"  onclick="return onClickShippingMethod(event)" name="shipping_method" value="Default" checked="checked"/>${uiLabelMap.OrderUseDefault}.</div>
              </td>
            </tr>
            </#if>
        </table>
        </div>
    </td>
    <td><div>
        <table hight="100%" >
            <tr>
              <td colspan="2">
                <h2>${uiLabelMap.OrderSpecialInstructions}</h2>
              </td>
            </tr>
            <tr>
              <td colspan="2">
                <textarea class='textAreaBox' cols="30" rows="3" name="shipping_instructions">${shoppingCart.getShippingInstructions()!}</textarea>
              </td>
            </tr>
            <tr><td colspan="2"><hr /></td></tr>
            <tr>
              <td colspan="2">
                <h2>${uiLabelMap.OrderPoNumber}</h2>&nbsp;
                <input type="text" class='inputBox' name="correspondingPoId" size="15" value='${shoppingCart.getPoNumber()!}'/>
              </td>
            </tr>
            <#if productStore.showCheckoutGiftOptions! != "N">
            <tr><td colspan="2"><hr /></td></tr>
            <tr>
              <td colspan="2">
                <div>
                  <h2>${uiLabelMap.OrderIsThisGift}</h2>
                  <input type='radio' <#if shoppingCart.getIsGift()?default("Y") == "Y">checked="checked"</#if> name='is_gift' value='true'/><span class='tabletext'>${uiLabelMap.CommonYes}</span>
                  <input type='radio' <#if shoppingCart.getIsGift()?default("N") == "N">checked="checked"</#if> name='is_gift' value='false'/><span class='tabletext'>${uiLabelMap.CommonNo}</span>
                </div>
              </td>
            </tr>
            <tr><td colspan="2"><hr /></td></tr>
            <tr>
              <td colspan="2">
                <h2>${uiLabelMap.OrderGiftMessage}</h2>
              </td>
            </tr>
            <tr>
              <td colspan="2">
                <textarea class='textAreaBox' cols="30" rows="3" name="gift_message">${shoppingCart.getGiftMessage()!}</textarea>
              </td>
            </tr>
            </#if>
        </table></div>
    </td>
    </tr>
    <tr><td colspan="2"><hr /></td></tr>
    <tr><td colspan="2"><h2>${uiLabelMap.OrderShipAllAtOnce}?</h2></td></tr>
    <tr>
        <td valign="top" colspan="2">
            <div>
                <input type='radio' <#if shoppingCart.getMaySplit()?default("N") == "N">checked="checked"</#if> name='may_split' value='false'/>
                <span>${uiLabelMap.OrderPleaseWaitUntilBeforeShipping}.</span>
            </div>
        </td>
    </tr>
    <tr>
        <td valign="top"  colspan="2">
            <div>
                <input <#if shoppingCart.getMaySplit()?default("N") == "Y">checked="checked"</#if> type='radio' name='may_split' value='true'/>
                <span>${uiLabelMap.OrderPleaseShipItemsBecomeAvailable}.</span>
            </div>
        </td>
    </tr>
    <tr>
    </tr>
</table>
</div>
</form>
