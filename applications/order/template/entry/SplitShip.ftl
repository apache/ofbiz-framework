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
//<![CDATA[
function submitForm(form, mode, value) {
    if (mode == "DN") {
        // done action; payment info
        form.action="<@ofbizUrl>updateShippingOptions/checkoutpayment</@ofbizUrl>";
        form.submit();
    } else if (mode == "CS") {
        // continue shopping
        form.action="<@ofbizUrl>updateShippingOptions/showcart</@ofbizUrl>";
        form.submit();
    } else if (mode == "NA") {
        // new address
        form.action="<@ofbizUrl>updateCheckoutOptions/editcontactmech?DONE_PAGE=splitship&partyId=${cart.getPartyId()}&preContactMechTypeId=POSTAL_ADDRESS&contactMechPurposeTypeId=SHIPPING_LOCATION</@ofbizUrl>";
        form.submit();
    } else if (mode == "SV") {
        // save option; return to current screen
        form.action="<@ofbizUrl>updateShippingOptions/splitship</@ofbizUrl>";
        form.submit();
    } else if (mode == "SA") {
        // selected shipping address
        form.action="<@ofbizUrl>updateShippingAddress/splitship</@ofbizUrl>";
        form.submit();
    }
}
//]]>
</script>

<div class="screenlet">
    <div class="screenlet-title-bar">
        <div class="h3">${uiLabelMap.OrderItemGroups}</div>
    </div>
    <div class="screenlet-body">
        <table width="100%" cellspacing="0" cellpadding="1" border="0">
          <#assign shipGroups = cart.getShipGroups()>
          <#if (shipGroups.size() > 0)>
            <#assign groupIdx = 0>
            <#list shipGroups as group>
              <#assign shipEstimateWrapper = Static["org.ofbiz.order.shoppingcart.shipping.ShippingEstimateWrapper"].getWrapper(dispatcher, cart, groupIdx)>
              <#assign carrierShipmentMethods = shipEstimateWrapper.getShippingMethods()>
              <#assign groupNumber = groupIdx + 1>
              <form method="post" action="#" name="editgroupform${groupIdx}" style="margin: 0;">
                <input type="hidden" name="groupIndex" value="${groupIdx}"/>
                <tr>
                  <td>
                    <div class="tabletext"><b>${uiLabelMap.CommonGroup} ${groupNumber}:</b></div>
                    <#list group.getShipItems() as item>
                      <#assign groupItem = group.getShipItemInfo(item)>
                      <div class="tabletext">&nbsp;&nbsp;&nbsp;${item.getName()} - (${groupItem.getItemQuantity()})</div>
                    </#list>
                  </td>
                  <td>
                    <div>
                      <span class='tabletext'>${uiLabelMap.CommonAdd}:</span>
                      <a href="javascript:submitForm(document.editgroupform${groupIdx}, 'NA', '');" class="buttontext">${uiLabelMap.PartyAddNewAddress}</a>
                    </div>
                    <div>
                      <#assign selectedContactMechId = cart.getShippingContactMechId(groupIdx)?default("")>
                      <select name="shippingContactMechId" class="selectBox" onchange="javascript:submitForm(document.editgroupform${groupIdx}, 'SA', null);">
                        <option value="">${uiLabelMap.OrderSelectShippingAddress}</option>
                        <#list shippingContactMechList as shippingContactMech>
                          <#assign shippingAddress = shippingContactMech.getRelatedOne("PostalAddress", false)>
                          <option value="${shippingAddress.contactMechId}" <#if (shippingAddress.contactMechId == selectedContactMechId)>selected="selected"</#if>>${shippingAddress.address1}</option>
                        </#list>
                      </select>
                    </div>
                    <#if cart.getShipmentMethodTypeId(groupIdx)??>
                      <#assign selectedShippingMethod = cart.getShipmentMethodTypeId(groupIdx) + "@" + cart.getCarrierPartyId(groupIdx)>
                    <#else>
                      <#assign selectedShippingMethod = "">
                    </#if>
                    <select name="shipmentMethodString" class="selectBox">
                      <option value="">${uiLabelMap.OrderSelectShippingMethod}</option>
                      <#list carrierShipmentMethods as carrierShipmentMethod>
                        <#assign shippingEst = shipEstimateWrapper.getShippingEstimate(carrierShipmentMethod)?default(-1)>
                        <#assign shippingMethod = carrierShipmentMethod.shipmentMethodTypeId + "@" + carrierShipmentMethod.partyId>
                        <option value="${shippingMethod}" <#if (shippingMethod == selectedShippingMethod)>selected="selected"</#if>>
                          <#if carrierShipmentMethod.partyId != "_NA_">
                            ${carrierShipmentMethod.partyId!}&nbsp;
                          </#if>
                          ${carrierShipmentMethod.description!}
                          <#if shippingEst?has_content>
                            &nbsp;-&nbsp;
                            <#if (shippingEst > -1)>
                              <@ofbizCurrency amount=shippingEst isoCode=cart.getCurrency()/>
                            <#else>
                              ${uiLabelMap.OrderCalculatedOffline}
                            </#if>
                          </#if>
                        </option>
                      </#list>
                    </select>

                    <h2>${uiLabelMap.OrderSpecialInstructions}</h2>
                    <textarea class='textAreaBox' cols="35" rows="3" wrap="hard" name="shippingInstructions">${cart.getShippingInstructions(groupIdx)!}</textarea>
                  </td>
                  <td>
                    <div>
                      <select name="maySplit" class="selectBox">
                        <#assign maySplitStr = cart.getMaySplit(groupIdx)?default("")>
                        <option value="">${uiLabelMap.OrderSplittingPreference}</option>
                        <option value="false" <#if maySplitStr == "N">selected="selected"</#if>>${uiLabelMap.OrderShipAllItemsTogether}</option>
                        <option value="true" <#if maySplitStr == "Y">selected="selected"</#if>>${uiLabelMap.OrderShipItemsWhenAvailable}</option>
                      </select>
                    </div>
                    <div>
                      <select name="isGift" class="selectBox">
                        <#assign isGiftStr = cart.getIsGift(groupIdx)?default("")>
                        <option value="">${uiLabelMap.OrderIsGift} ?</option>
                        <option value="false" <#if isGiftStr == "N">selected="selected"</#if>>${uiLabelMap.OrderNotAGift}</option>
                        <option value="true" <#if isGiftStr == "Y">selected="selected"</#if>>${uiLabelMap.OrderYesIsAGift}</option>
                      </select>
                    </div>

                    <h2>${uiLabelMap.OrderGiftMessage}</h2>
                    <textarea class='textAreaBox' cols="30" rows="3" wrap="hard" name="giftMessage">${cart.getGiftMessage(groupIdx)!}</textarea>
                  </td>
                  <td><input type="button" class="smallSubmit" value="${uiLabelMap.CommonSave}" onclick="javascript:submitForm(document.editgroupform${groupIdx}, 'SV', null);"/></td>
                </tr>
                <#assign groupIdx = groupIdx + 1>
                <#if group_has_next>
                  <tr>
                    <td colspan="6"><hr /></td>
                  </tr>
                </#if>
              </form>
            </#list>
          <#else>
            <div class="tabletext">${uiLabelMap.OrderNoShipGroupsDefined}.</div>
          </#if>
        </table>
    </div>
</div>

<div class="screenlet">
    <div class="screenlet-title-bar">
        <div class="h3">${uiLabelMap.OrderAssignItems}</div>
    </div>
    <div class="screenlet-body">
        <table width="100%" cellspacing="0" cellpadding="1" border="0">
          <tr>
            <td><div class="tabletext"><b>${uiLabelMap.OrderProduct}</b></div></td>
            <td align="center"><div class="tabletext"><b>${uiLabelMap.OrderTotalQty}</b></div></td>
            <td>&nbsp;</td>
            <td align="center"><div class="tabletext"><b>${uiLabelMap.OrderMoveQty}</b></div></td>
            <td>&nbsp;</td>
            <td>&nbsp;</td>
          </tr>

          <#list cart.items() as cartLine>
            <#assign cartLineIndex = cart.getItemIndex(cartLine)>
            <tr>
              <form method="post" action="<@ofbizUrl>updatesplit</@ofbizUrl>" name="editgroupform" style="margin: 0;">
                <input type="hidden" name="itemIndex" value="${cartLineIndex}"/>
                <td>
                  <div class="tabletext">
                    <#if cartLine.getProductId()??>
                      <#-- product item -->
                      <#-- start code to display a small image of the product -->
                      <#assign smallImageUrl = Static["org.ofbiz.product.product.ProductContentWrapper"].getProductContentAsText(cartLine.getProduct(), "SMALL_IMAGE_URL", locale, dispatcher, "url")!>
                      <#if !smallImageUrl?string?has_content><#assign smallImageUrl = "/images/defaultImage.jpg"></#if>
                      <#if smallImageUrl?string?has_content>
                        <a href="<@ofbizUrl>product?product_id=${cartLine.getProductId()}</@ofbizUrl>">
                          <img src="<@ofbizContentUrl>${requestAttributes.contentPathPrefix!}${smallImageUrl}</@ofbizContentUrl>" class="cssImgSmall" alt="" />
                        </a>
                      </#if>
                      <#-- end code to display a small image of the product -->
                      <a href="<@ofbizUrl>product?product_id=${cartLine.getProductId()}</@ofbizUrl>" class="buttontext">${cartLine.getProductId()} -
                      ${cartLine.getName()!}</a> : ${cartLine.getDescription()!}

                      <#-- display the registered ship groups and quantity -->
                      <#assign itemShipGroups = cart.getShipGroups(cartLine)>
                      <#list itemShipGroups.entrySet() as group>
                        <div class="tabletext">
                          <#assign groupNumber = group.getKey() + 1>
                          <b>Group - </b>${groupNumber} / <b>${uiLabelMap.CommonQuantity} - </b>${group.getValue()}
                        </div>
                      </#list>

                      <#-- if inventory is not required check to see if it is out of stock and needs to have a message shown about that... -->
                      <#assign itemProduct = cartLine.getProduct()>
                      <#assign isStoreInventoryNotRequiredAndNotAvailable = Static["org.ofbiz.product.store.ProductStoreWorker"].isStoreInventoryRequiredAndAvailable(request, itemProduct, cartLine.getQuantity(), false, false)>
                      <#if isStoreInventoryNotRequiredAndNotAvailable && itemProduct.inventoryMessage?has_content>
                        <b>(${itemProduct.inventoryMessage})</b>
                      </#if>

                    <#else>
                      <#-- this is a non-product item -->
                      <b>${cartLine.getItemTypeDescription()!}</b> : ${cartLine.getName()!}
                    </#if>
                  </div>

                </td>
                <td align="right">
                  <div class="tabletext">${cartLine.getQuantity()?string.number}&nbsp;&nbsp;&nbsp;</div>
                </td>
                <td>
                  <div>&nbsp;</div>
                </td>
                <td align="center">
                  <input size="6" class="inputBox" type="text" name="quantity" value="${cartLine.getQuantity()?string.number}"/>
                </td>
                <td>
                  <div>&nbsp;</div>
                </td>
                <td>
                  <div class="tabletext">${uiLabelMap.CommonFrom}:
                    <select name="fromGroupIndex" class="selectBox">
                      <#list itemShipGroups.entrySet() as group>
                        <#assign groupNumber = group.getKey() + 1>
                        <option value="${group.getKey()}">${uiLabelMap.CommonGroup} ${groupNumber}</option>
                      </#list>
                    </select>
                  </div>
                </td>
                <td>
                  <div class="tabletext">${uiLabelMap.CommonTo}:
                    <select name="toGroupIndex" class="selectBox">
                      <#list 0..(cart.getShipGroupSize() - 1) as groupIdx>
                        <#assign groupNumber = groupIdx + 1>
                        <option value="${groupIdx}">${uiLabelMap.CommonGroup} ${groupNumber}</option>
                      </#list>
                      <option value="-1">${uiLabelMap.CommonNew} ${uiLabelMap.CommonGroup}</option>
                    </select>
                  </div>
                </td>
                <td><input type="submit" class="smallSubmit" value="${uiLabelMap.CommonSubmit}"/></td>
              </form>
            </tr>
          </#list>
        </table>
    </div>
</div>

<table>
  <tr valign="top">
    <td>
      &nbsp;<a href="<@ofbizUrl>updateCheckoutOptions/showcart</@ofbizUrl>" class="buttontextbig">${uiLabelMap.OrderBacktoShoppingCart}</a>
    </td>
    <td align="right">
      <a href="<@ofbizUrl>setBilling</@ofbizUrl>" class="buttontextbig">${uiLabelMap.CommonContinue}</a>
    </td>
  </tr>
</table>
