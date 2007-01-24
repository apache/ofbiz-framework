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

<script language="javascript" type="text/javascript">
<!--
function submitForm(form, mode, value) {
    if (mode == "DN") {
        // done action; checkout
        form.action="<@ofbizUrl>checkoutoptions</@ofbizUrl>";
        form.submit();
    } else if (mode == "CS") {
        // continue shopping
        form.action="<@ofbizUrl>updateCheckoutOptions/showcart</@ofbizUrl>";
        form.submit();
    } else if (mode == "NA") {
        // new address
        form.action="<@ofbizUrl>updateCheckoutOptions/editcontactmech?preContactMechTypeId=POSTAL_ADDRESS&contactMechPurposeTypeId=SHIPPING_LOCATION&DONE_PAGE=checkoutshippingaddress</@ofbizUrl>";
        form.submit();
    } else if (mode == "EA") {
        // edit address
        form.action="<@ofbizUrl>updateCheckoutOptions/editcontactmech?DONE_PAGE=checkoutshippingaddress&contactMechId="+value+"</@ofbizUrl>";
        form.submit();
    }
}

function toggleBillingAccount(box) {
    var amountName = box.value + "_amount";
    box.checked = true;
    box.form.elements[amountName].disabled = false;

    for (var i = 0; i < box.form.elements[box.name].length; i++) {
        if (!box.form.elements[box.name][i].checked) {
            box.form.elements[box.form.elements[box.name][i].value + "_amount"].disabled = true;
        }
    }
}

// -->
</script>
<#assign cart = shoppingCart?if_exists/>

<form method="post" name="checkoutInfoForm" style="margin:0;">
    <input type="hidden" name="checkoutpage" value="shippingaddress"/>
    <div class="screenlet" style="height: 100%;">
        <div class="screenlet-header">
            <div class="boxhead">1)&nbsp;${uiLabelMap.OrderWhereShallWeShipIt}?</div>
        </div>
        <div class="screenlet-body" style="height: 100%;">
            <table width="100%" border="0" cellpadding="1" cellspacing="0">
              <tr>
                <td colspan="2">
                  <a href="<@ofbizUrl>splitship</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderSplitShipment}</a>
                  <a href="javascript:submitForm(document.checkoutInfoForm, 'NA', '');" class="buttontext">${uiLabelMap.PartyAddNewAddress}</a>
                  <#if (cart.getShipGroupSize() > 1)>
                    <div class="tabletext" style="color: red;">${uiLabelMap.OrderNOTEMultipleShipmentsExis}</div>
                  </#if>
                </td>
              </tr>
               <#if shippingContactMechList?has_content>
                 <tr><td colspan="2"><hr class="sepbar"/></td></tr>
                 <#list shippingContactMechList as shippingContactMech>
                   <#assign shippingAddress = shippingContactMech.getRelatedOne("PostalAddress")>
                   <#assign checkThisAddress = (shippingContactMech_index == 0 && !cart.getShippingContactMechId()?has_content) || (cart.getShippingContactMechId()?default("") == shippingAddress.contactMechId)/>
                   <tr>
                     <td align="left" valign="top" width="1%" nowrap>
                       <input type="radio" name="shipping_contact_mech_id" value="${shippingAddress.contactMechId}"<#if checkThisAddress> checked</#if>>
                     </td>
                     <td align="left" valign="top" width="99%" nowrap>
                       <div class="tabletext">
                         <#if shippingAddress.toName?has_content><b>${uiLabelMap.CommonTo}:</b>&nbsp;${shippingAddress.toName}<br/></#if>
                         <#if shippingAddress.attnName?has_content><b>${uiLabelMap.PartyAddrAttnName}:</b>&nbsp;${shippingAddress.attnName}<br/></#if>
                         <#if shippingAddress.address1?has_content>${shippingAddress.address1}<br/></#if>
                         <#if shippingAddress.address2?has_content>${shippingAddress.address2}<br/></#if>
                         <#if shippingAddress.city?has_content>${shippingAddress.city}</#if>
                         <#if shippingAddress.stateProvinceGeoId?has_content><br/>${shippingAddress.stateProvinceGeoId}</#if>
                         <#if shippingAddress.postalCode?has_content><br/>${shippingAddress.postalCode}</#if>
                         <#if shippingAddress.countryGeoId?has_content><br/>${shippingAddress.countryGeoId}</#if>
                         <a href="javascript:submitForm(document.checkoutInfoForm, 'EA', '${shippingAddress.contactMechId}');" class="buttontext">${uiLabelMap.CommonUpdate}</a>
                       </div>
                     </td>
                   </tr>
                   <tr><td colspan="2"><hr class="sepbar"/></td></tr>
                 </#list>
               </#if>
             </table>
            <#-- Party Tax Info -->
            <div class="tableheadtext">&nbsp;${uiLabelMap.PartyTaxIdentification}</div>
            ${screens.render("component://order/widget/ordermgr/OrderEntryOrderScreens.xml#customertaxinfo")}
        </div>
    </div>
</form>

<table width="100%">
  <tr valign="top">
    <td align="left">
      &nbsp;<a href="javascript:submitForm(document.checkoutInfoForm, 'CS', '');" class="buttontextbig">${uiLabelMap.OrderBacktoShoppingCart}</a>
    </td>
    <td align="right">
      <a href="javascript:submitForm(document.checkoutInfoForm, 'DN', '');" class="buttontextbig">${uiLabelMap.CommonNext}</a>
    </td>
  </tr>
</table>
