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
        form.action="<@ofbizUrl>updateCheckoutOptions/editcontactmech?preContactMechTypeId=POSTAL_ADDRESS&contactMechPurposeTypeId=SHIPPING_LOCATION&DONE_PAGE=checkoutoptions</@ofbizUrl>";
        form.submit();
    } else if (mode == "EA") {
        // edit address
        form.action="<@ofbizUrl>updateCheckoutOptions/editcontactmech?DONE_PAGE=checkoutshippingaddress&contactMechId="+value+"</@ofbizUrl>";
        form.submit();
    } else if (mode == "NC") {
        // new credit card
        form.action="<@ofbizUrl>updateCheckoutOptions/editcreditcard?DONE_PAGE=checkoutoptions</@ofbizUrl>";
        form.submit();
    } else if (mode == "EC") {
        // edit credit card
        form.action="<@ofbizUrl>updateCheckoutOptions/editcreditcard?DONE_PAGE=checkoutoptions&paymentMethodId="+value+"</@ofbizUrl>";
        form.submit();
    } else if (mode == "NE") {
        // new eft account
        form.action="<@ofbizUrl>updateCheckoutOptions/editeftaccount?DONE_PAGE=checkoutoptions</@ofbizUrl>";
        form.submit();
    } else if (mode == "EE") {
        // edit eft account
        form.action="<@ofbizUrl>updateCheckoutOptions/editeftaccount?DONE_PAGE=checkoutoptions&paymentMethodId="+value+"</@ofbizUrl>";
        form.submit();
    }
}

// -->
</script>

<form method="post" name="checkoutInfoForm" style="margin:0;">
    <input type="hidden" name="checkoutpage" value="shippingoptions"/>

    <div class="screenlet" style="height: 100%;">
        <div class="screenlet-header">
            <div class="boxhead">2)&nbsp;${uiLabelMap.OrderHowShallWeShipIt}?</div>
        </div>
        <div class="screenlet-body" style="height: 100%;">
            <table width="100%" cellpadding="1" border="0" cellpadding="0" cellspacing="0">
              <#list carrierShipmentMethodList as carrierShipmentMethod>
                <#assign shippingMethod = carrierShipmentMethod.shipmentMethodTypeId + "@" + carrierShipmentMethod.partyId>
                <tr>
                  <td width="1%" valign="top" >
                    <input type="radio" name="shipping_method" value="${shippingMethod}" <#if shippingMethod == chosenShippingMethod?default("N@A")>checked</#if>/>
                  </td>
                  <td valign="top">
                    <div class="tabletext">
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
                  <td width="1%" valign="top">
                    <input type="radio" name="shipping_method" value="Default" checked>
                  </td>
                  <td valign="top">
                    <div class="tabletext">${uiLabelMap.OrderUseDefault}.</div>
                  </td>
                </tr>
              </#if>
              <tr><td colspan="2"><hr class="sepbar"/></td></tr>
              <tr>
                <td colspan="2">
                  <div class="head2"><b>${uiLabelMap.OrderShipAllAtOnce}?</b></div>
                </td>
              </tr>
              <tr>
                <td valign="top">
                  <input type="radio" <#if "Y" != shoppingCart.getMaySplit()?default("N")>checked</#if> name="may_split" value="false"/>
                </td>
                <td valign="top">
                  <div class="tabletext">${uiLabelMap.OrderPleaseWaitUntilBeforeShipping}.</div>
                </td>
              </tr>
              <tr>
                <td valign="top">
                  <input <#if "Y" == shoppingCart.getMaySplit()?default("N")>checked</#if> type="radio" name="may_split" value="true"/>
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
                  <textarea class="textAreaBox" cols="30" rows="3" wrap="hard" name="shipping_instructions">${shoppingCart.getShippingInstructions()?if_exists}</textarea>
                </td>
              </tr>
              <tr><td colspan="2"><hr class="sepbar"/></td></tr>
              <tr>
                <td colspan="2">
                  <span class="head2"><b>${uiLabelMap.OrderPoNumber}</b></span>&nbsp;
                  <#if shoppingCart.getPoNumber()?exists && shoppingCart.getPoNumber() != "(none)">
                    <#assign currentPoNumber = shoppingCart.getPoNumber()>
                  </#if>
                  <input type="text" class="inputBox" name="correspondingPoId" size="15" value="${currentPoNumber?if_exists}"/>
                </td>
              </tr>
              <#if productStore.showCheckoutGiftOptions?if_exists != "N">
              <tr><td colspan="2"><hr class="sepbar"/></td></tr>
              <tr>
                <td colspan="2">
                  <div>
                    <span class="head2"><b>${uiLabelMap.OrderIsThisGift}?</b></span>
                    <input type="radio" <#if "Y" == shoppingCart.getIsGift()?default("N")>checked</#if> name="is_gift" value="true"/><span class="tabletext">${uiLabelMap.CommonYes}</span>
                    <input type="radio" <#if "Y" != shoppingCart.getIsGift()?default("N")>checked</#if> name="is_gift" value="false"/><span class="tabletext">${uiLabelMap.CommonNo}</span>
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
                  <textarea class="textAreaBox" cols="30" rows="3" wrap="hard" name="gift_message">${shoppingCart.getGiftMessage()?if_exists}</textarea>
                </td>
              </tr>
              <#else/>
              <input type="hidden" name="is_gift" value="false"/>
              </#if>
              <tr><td colspan="2"><hr class="sepbar"/></td></tr>
              <tr>
                <td colspan="2">
                  <div class="head2"><b>${uiLabelMap.PartyEmailAddresses}</b></div>
                </td>
              </tr>
              <tr>
                <td colspan="2">
                  <div class="tabletext">${uiLabelMap. OrderEmailSentToFollowingAddresses}:</div>
                  <div class="tabletext">
                    <b>
                      <#list emailList as email>
                        ${email.infoString?if_exists}<#if email_has_next>,</#if>
                      </#list>
                    </b>
                  </div>
                  <div class="tabletext">${uiLabelMap.OrderUpdateEmailAddress} <a href="<@ofbizUrl>viewprofile?DONE_PAGE=checkoutoptions</@ofbizUrl>" class="buttontext">${uiLabelMap.PartyProfile}</a>.</div>
                  <br/>
                  <div class="tabletext">${uiLabelMap.OrderCommaSeperatedEmailAddresses}:</div>
                  <input type="text" class="inputBox" size="30" name="order_additional_emails" value="${shoppingCart.getOrderAdditionalEmails()?if_exists}"/>
                </td>
              </tr>
            </table>
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
