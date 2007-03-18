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
function toggle(e) {
    e.checked = !e.checked;
}
function checkToggle(e) {
    var cform = document.cartform;
    if (e.checked) {
        var len = cform.elements.length;
        var allchecked = true;
        for (var i = 0; i < len; i++) {
            var element = cform.elements[i];
            if (element.name == "selectedItem" && !element.checked) {
                allchecked = false;
            }
            cform.selectAll.checked = allchecked;
        }
    } else {
        cform.selectAll.checked = false;
    }
}
function toggleAll(e) {
    var cform = document.cartform;
    var len = cform.elements.length;
    for (var i = 0; i < len; i++) {
        var element = cform.elements[i];
        if (element.name == "selectedItem" && element.checked != e.checked) {
            toggle(element);
        }
    }
}
function removeSelected() {
    var cform = document.cartform;
    cform.removeSelected.value = true;
    cform.submit();
}
function addToList() {
    var cform = document.cartform;
    cform.action = "<@ofbizUrl>addBulkToShoppingList</@ofbizUrl>";
    cform.submit();
}
function gwAll(e) {
    var cform = document.cartform;
    var len = cform.elements.length;
    var selectedValue = e.value;
    if (selectedValue == "") {
        return;
    }
    
    var cartSize = ${shoppingCartSize};
    var passed = 0;
    for (var i = 0; i < len; i++) {
        var element = cform.elements[i];
        var ename = element.name;
        var sname = ename.substring(0,16);
        if (sname == "option^GIFT_WRAP") {
            var options = element.options;
            var olen = options.length;
            var matching = -1;
            for (var x = 0; x < olen; x++) {
                var thisValue = element.options[x].value;
                if (thisValue == selectedValue) {
                    element.selectedIndex = x;
                    passed++;
                }
            }
        }
    }
    if (cartSize > passed && selectedValue != "NO^") {
        alert(${uiLabelMap.EcommerceSelectedGiftWrap});
    }
    cform.submit();
}
</script>

<script language="JavaScript" type="text/javascript">
function setAlternateGwp(field) {
  window.location=field.value;
};
</script>
<#assign fixedAssetExist = shoppingCart.containAnyWorkEffortCartItems()/> <#-- change display format when rental items exist in the shoppingcart -->

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxlink">
            <#if ((sessionAttributes.lastViewedProducts)?has_content && sessionAttributes.lastViewedProducts?size > 0)>
              <#assign continueLink = "/product?product_id=" + sessionAttributes.lastViewedProducts.get(0)>
            <#else>
              <#assign continueLink = "/main">
            </#if>
            <a href="<@ofbizUrl>${continueLink}</@ofbizUrl>" class="submenutext">${uiLabelMap.EcommerceContinueShopping}</a>
            <#if (shoppingCartSize > 0)><a href="<@ofbizUrl>checkoutoptions</@ofbizUrl>" class="submenutextright">${uiLabelMap.EcommerceCheckout}</a><#else><span class="submenutextrightdisabled">${uiLabelMap.EcommerceCheckout}</span></#if>
        </div>
        <div class="boxhead">&nbsp;${uiLabelMap.CommonQuickAdd}</div>
    </div>
    <div class="screenlet-body">
        <div class="tabletext">
            <form method="post" action="<@ofbizUrl>additem<#if requestAttributes._CURRENT_VIEW_?has_content>/${requestAttributes._CURRENT_VIEW_}</#if></@ofbizUrl>" name="quickaddform" style="margin: 0;">
                ${uiLabelMap.EcommerceProductNumber}<input type="text" class="inputBox" name="add_product_id" value="${requestParameters.add_product_id?if_exists}"/>
                <#-- check if rental data present  insert extra fields in Quick Add-->
                <#if product?exists && product.getString("productTypeId") == "ASSET_USAGE">
                    ${uiLabelMap.StartDate}: <input type="text" class="inputBox" size="10" name="reservStart" value=${requestParameters.reservStart?default("")}/>
                    ${uiLabelMap.EcommerceLength}: <input type="text" class="inputBox" size="2" name="reservLength" value=${requestParameters.reservLength?default("")}/>
                    </div>
                    <div>
                    &nbsp;&nbsp;${uiLabelMap.EcommerceNbrPersons}: <input type="text" class="inputBox" size="3" name="reservPersons" value=${requestParameters.reservPersons?default("1")}/>
                </#if> 
                ${uiLabelMap.CommonQuantity}: <input type="text" class="inputBox" size="5" name="quantity" value="${requestParameters.quantity?default("1")}"/>
                <input type="submit" class="smallSubmit" value="${uiLabelMap.EcommerceAddtoCart}"/>
                <#-- <a href="javascript:document.quickaddform.submit()" class="buttontext"><span style="white-space: nowrap;">[${uiLabelMap.EcommerceAddtoCart}]</span></a> -->
            </form>
        </div>
    </div>
</div>

<script language="JavaScript" type="text/javascript">
  document.quickaddform.add_product_id.focus();
</script>

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxlink">
            <div class="lightbuttontextdisabled">
              <#--<a href="<@ofbizUrl>main</@ofbizUrl>" class="lightbuttontext">[${uiLabelMap.EcommerceContinueShopping}]</a>-->
              <#if (shoppingCartSize > 0)>
                <a href="javascript:document.cartform.submit();" class="submenutext">${uiLabelMap.EcommerceRecalculateCart}</a>
                <a href="<@ofbizUrl>emptycart</@ofbizUrl>" class="submenutext">${uiLabelMap.EcommerceEmptyCart}</a>
                <a href="javascript:removeSelected();" class="submenutext">${uiLabelMap.EcommerceRemoveSelected}</a>
              <#else>
                <span class="submenutextdisabled">${uiLabelMap.EcommerceRecalculateCart}</span>
                <span class="submenutextdisabled">${uiLabelMap.EcommerceEmptyCart}</span>
                <span class="submenutextdisabled">${uiLabelMap.EcommerceRemoveSelected}</span>
              </#if>
              <#if (shoppingCartSize > 0)><a href="<@ofbizUrl>checkoutoptions</@ofbizUrl>" class="submenutextright">${uiLabelMap.EcommerceCheckout}</a><#else><span class="submenutextrightdisabled">${uiLabelMap.EcommerceCheckout}</span></#if>
            </div>
        </div>
        <div class="boxhead">&nbsp;${uiLabelMap.EcommerceShoppingCart}</div>
    </div>
    <div class="screenlet-body">

  <#if (shoppingCartSize > 0)>
    <form method="post" action="<@ofbizUrl>modifycart</@ofbizUrl>" name="cartform" style="margin: 0;">
      <input type="hidden" name="removeSelected" value="false">
      <table width="99%" cellspacing="0" cellpadding="1" border="0">
        <tr>
          <td NOWRAP>&nbsp;</td>
          <td NOWRAP><div class="tabletext"><b>${uiLabelMap.EcommerceProduct}</b></div></td>
          <#if asslGiftWraps?has_content && showOrderGiftWrap?default("true") == "true">
            <td NOWRAP align="right">
              <select class="selectBox" name="GWALL" onchange="javascript:gwAll(this);">
                <option value="">${uiLabelMap.EcommerceGiftWrapAllItems}</option>
                <option value="NO^">${uiLabelMap.EcommerceNoGiftWrap}</option>
                <#list allgiftWraps as option>
                  <option value="${option.productFeatureId}">${option.description} : ${option.defaultAmount?default(0)}</option>
                </#list>
              </select>
          <#else>
            <td NOWRAP>&nbsp;</td>
          </#if>
          <#if fixedAssetExist == true><td NOWRAP align="center"><table><tr><td class="tabletext" nowrap align="center"><b>- ${uiLabelMap.Startdate} -</b></td><td class="tabletext" nowrap><b>- ${uiLabelMap.EcommerceNbrOfDays} -</b></td></tr><tr><td class="tabletext" nowrap><b>- ${uiLabelMap.EcommerceNbrOfPersons} -</b></td><td class="tabletext" nowrap align="center"><b>- ${uiLabelMap.CommonQuantity} -</b></td></tr></table></td>
          <#else><td NOWRAP align="center"><div class="tabletext"><b>${uiLabelMap.CommonQuantity}</b></div></td></#if>
          <td NOWRAP align="right"><div class="tabletext"><b>${uiLabelMap.EcommerceUnitPrice}</b></div></td>
          <td NOWRAP align="right"><div class="tabletext"><b>${uiLabelMap.EcommerceAdjustments}</b></div></td>
          <td NOWRAP align="right"><div class="tabletext"><b>${uiLabelMap.EcommerceItemTotal}</b></div></td>
          <td NOWRAP align="center"><input type="checkbox" name="selectAll" value="0" onclick="javascript:toggleAll(this);"></td>
        </tr>

        <#assign itemsFromList = false>
        <#assign promoItems = false>
        <#list shoppingCart.items() as cartLine>
        
          <#assign cartLineIndex = shoppingCart.getItemIndex(cartLine)>
          <#assign lineOptionalFeatures = cartLine.getOptionalProductFeatures()>
          <#-- show adjustment info -->
          <#list cartLine.getAdjustments() as cartLineAdjustment>
            <!-- cart line ${cartLineIndex} adjustment: ${cartLineAdjustment} -->
          </#list>
          <tr><td>&nbsp;</td><td colspan="6"><hr class="sepbar"/></td></tr>
          <tr>
            <td>
                <#if cartLine.getShoppingListId()?exists>
                  <#assign itemsFromList = true>
                  <a href="<@ofbizUrl>editShoppingList?shoppingListId=${cartLine.getShoppingListId()}</@ofbizUrl>" class="linktext">L</a>&nbsp;&nbsp;
                <#elseif cartLine.getIsPromo()>
                  <#assign promoItems = true>
                  <a href="<@ofbizUrl>view/showcart</@ofbizUrl>" class="buttontext">P</a>&nbsp;&nbsp;
                <#else>
                  &nbsp;
                </#if>
            </td>
            <td>
                <div class="tabletext">
                  <#if cartLine.getProductId()?exists>
                    <#-- product item -->
                    <#-- start code to display a small image of the product -->
                    <#if cartLine.getParentProductId()?exists>
                      <#assign parentProductId = cartLine.getParentProductId()/>
                    <#else>
                      <#assign parentProductId = cartLine.getProductId()/>
                    </#if>
                    <#assign smallImageUrl = Static["org.ofbiz.product.product.ProductContentWrapper"].getProductContentAsText(cartLine.getProduct(), "SMALL_IMAGE_URL", locale, dispatcher)?if_exists>
                    <#if !smallImageUrl?has_content><#assign smallImageUrl = "/images/defaultImage.jpg"></#if>
                    <#if smallImageUrl?has_content>
                      <a href="<@ofbizUrl>product?product_id=${parentProductId}</@ofbizUrl>">
                        <img src="<@ofbizContentUrl>${requestAttributes.contentPathPrefix?if_exists}${smallImageUrl}</@ofbizContentUrl>" align="left" width="50" class="imageborder" border="0"/>
                      </a>
                    </#if>
                    <#-- end code to display a small image of the product -->
                    <#-- <b>${cartLineIndex}</b> - -->
                    <a href="<@ofbizUrl>product?product_id=${parentProductId}</@ofbizUrl>" class="linktext">${cartLine.getProductId()} -
                    ${cartLine.getName()?if_exists}</a> : ${cartLine.getDescription()?if_exists}
                    <#-- For configurable products, the selected options are shown -->
                    <#if cartLine.getConfigWrapper()?exists>
                      <#assign selectedOptions = cartLine.getConfigWrapper().getSelectedOptions()?if_exists>
                      <#if selectedOptions?exists>
                        <div>&nbsp;</div>
                        <#list selectedOptions as option>
                          <div>
                            ${option.getDescription()}
                          </div>
                        </#list>
                      </#if>
                    </#if>

                    <#-- if inventory is not required check to see if it is out of stock and needs to have a message shown about that... -->
                    <#assign itemProduct = cartLine.getProduct()>
                    <#assign isStoreInventoryNotRequiredAndNotAvailable = Static["org.ofbiz.product.store.ProductStoreWorker"].isStoreInventoryRequiredAndAvailable(request, itemProduct, cartLine.getQuantity(), false, false)>
                    <#if isStoreInventoryNotRequiredAndNotAvailable && itemProduct.inventoryMessage?has_content>
                        <b>(${itemProduct.inventoryMessage})</b>
                    </#if>

                  <#else>
                    <#-- this is a non-product item -->
                    <b>${cartLine.getItemTypeDescription()?if_exists}</b> : ${cartLine.getName()?if_exists}
                  </#if>
                </div>
                
                <#if (cartLine.getIsPromo() && cartLine.getAlternativeOptionProductIds()?has_content)>
                  <#-- Show alternate gifts if there are any... -->
                  <div class="tableheadtext">${uiLabelMap.OrderChooseFollowingForGift}:</div>
                  <select name="dummyAlternateGwpSelect${cartLineIndex}" onchange="setAlternateGwp(this);" class="selectBox">
                  <option value="">- ${uiLabelMap.OrderChooseAnotherGift} -</option>
                  <#list cartLine.getAlternativeOptionProductIds() as alternativeOptionProductId>
                    <#assign alternativeOptionName = Static["org.ofbiz.product.product.ProductWorker"].getGwpAlternativeOptionName(dispatcher, delegator, alternativeOptionProductId, requestAttributes.locale)>
                    <option value="<@ofbizUrl>setDesiredAlternateGwpProductId?alternateGwpProductId=${alternativeOptionProductId}&alternateGwpLine=${cartLineIndex}</@ofbizUrl>">${alternativeOptionName?default(alternativeOptionProductId)}</option>
                  </#list>
                  </select>
                  <#-- this is the old way, it lists out the options and is not as nice as the drop-down
                  <#list cartLine.getAlternativeOptionProductIds() as alternativeOptionProductId>
                    <#assign alternativeOptionName = Static["org.ofbiz.product.product.ProductWorker"].getGwpAlternativeOptionName(delegator, alternativeOptionProductId, requestAttributes.locale)>
                    <div class="tabletext"><a href="<@ofbizUrl>setDesiredAlternateGwpProductId?alternateGwpProductId=${alternativeOptionProductId}&alternateGwpLine=${cartLineIndex}</@ofbizUrl>" class="buttontext">Select: ${alternativeOptionName?default(alternativeOptionProductId)}</a></div>
                  </#list>
                  -->
                </#if>
            </td>

            <#-- gift wrap option -->
            <#assign showNoGiftWrapOptions = false>
            <td nowrap align="right">
              <#assign giftWrapOption = lineOptionalFeatures.GIFT_WRAP?if_exists>
              <#assign selectedOption = cartLine.getAdditionalProductFeatureAndAppl("GIFT_WRAP")?if_exists>
              <#if giftWrapOption?has_content>
                <select class="selectBox" name="option^GIFT_WRAP_${cartLineIndex}" onchange="javascript:document.cartform.submit()">
                  <option value="NO^">${uiLabelMap.EcommerceNoGiftWrap}</option>
                  <#list giftWrapOption as option>
                    <option value="${option.productFeatureId}" <#if ((selectedOption.productFeatureId)?exists && selectedOption.productFeatureId == option.productFeatureId)>SELECTED</#if>>${option.description} : ${option.amount?default(0)}</option>
                  </#list>
                </select>
              <#elseif showNoGiftWrapOptions>
                <select class="selectBox" name="option^GIFT_WRAP_${cartLineIndex}" onchange="javascript:document.cartform.submit()">
                  <option value="">${uiLabelMap.EcommerceNoGiftWrap}</option>
                </select>
              <#else>
                &nbsp;
              </#if>
            </td>
            <#-- end gift wrap option -->

            <td nowrap align="center">
              <div class="tabletext">
                <#if cartLine.getIsPromo() || cartLine.getShoppingListId()?exists>
                       <#if fixedAssetExist == true><#if cartLine.getReservStart()?exists><table border="0" width="100%"><tr><td width="1%">&nbsp;</td><td width="50%" class="tabletext">${cartLine.getReservStart()?string("yyyy-mm-dd")}</td><td align="center" class="tabletext">${cartLine.getReservLength()?string.number}</td></tr><tr><td align="center">&nbsp;</td><td align="center" class="tabletext">${cartLine.getReservPersons()?string.number}</td><td class="tabletext" align="center"><#else>
                           <table border="0" width="100%"><tr><td width="52%" align="center">--</td><td align="center">--</td></tr><tr><td align="center">--</td><td align="center" class="tabletext">    </#if>
                        ${cartLine.getQuantity()?string.number}</td></tr></table>
                    <#else><#-- fixedAssetExist -->
                        ${cartLine.getQuantity()?string.number}
                    </#if>
                <#else><#-- Is Promo or Shoppinglist -->
                       <#if fixedAssetExist == true><#if cartLine.getReservStart()?exists><table border="0" width="100%"><tr><td width="1%">&nbsp;</td><td><input type="text" class="inputBox" size="10" name="reservStart_${cartLineIndex}" value=${cartLine.getReservStart()?string}></td><td><input type="text" class="inputBox" size="2" name="reservLength_${cartLineIndex}" value=${cartLine.getReservLength()?string.number}></td></tr><tr><td>&nbsp;</td><td><input type="text" class="inputBox" size="3" name="reservPersons_${cartLineIndex}" value=${cartLine.getReservPersons()?string.number}></td><td class="tabletext"><#else>
                           <table border="0" width="100%"><tr><td width="52%" align="center">--</td><td align="center">--</td></tr><tr><td align="center">--</td><td align="center" class="tabletext"></#if>
                        <input size="6" class="inputBox" type="text" name="update_${cartLineIndex}" value="${cartLine.getQuantity()?string.number}"></td></tr></table>
                    <#else><#-- fixedAssetExist -->
                        <input size="6" class="inputBox" type="text" name="update_${cartLineIndex}" value="${cartLine.getQuantity()?string.number}">
                    </#if>
                </#if>
              </div>
            </td>
            <td nowrap align="right"><div class="tabletext"><@ofbizCurrency amount=cartLine.getDisplayPrice() isoCode=shoppingCart.getCurrency()/></div></td>
            <td nowrap align="right"><div class="tabletext"><@ofbizCurrency amount=cartLine.getOtherAdjustments() isoCode=shoppingCart.getCurrency()/></div></td>
            <td nowrap align="right"><div class="tabletext"><@ofbizCurrency amount=cartLine.getDisplayItemSubTotal() isoCode=shoppingCart.getCurrency()/></div></td>
            <td nowrap align="center"><div class="tabletext"><#if !cartLine.getIsPromo()><input type="checkbox" name="selectedItem" value="${cartLineIndex}" onclick="javascript:checkToggle(this);"><#else>&nbsp;</#if></div></td>
          </tr>
        </#list>

        <#if shoppingCart.getAdjustments()?has_content>
            <tr><td>&nbsp;</td><td colspan="6"><hr class="sepbar"/></td></tr>
              <tr>
                <td colspan="5" nowrap align="right"><div class="tabletext">${uiLabelMap.CommonSubTotal}:</div></td>
                <td nowrap align="right"><div class="tabletext"><@ofbizCurrency amount=shoppingCart.getSubTotal() isoCode=shoppingCart.getCurrency()/></div></td>
                <td>&nbsp;</td>
              </tr>
            <#list shoppingCart.getAdjustments() as cartAdjustment>
              <#assign adjustmentType = cartAdjustment.getRelatedOneCache("OrderAdjustmentType")>
              <!-- adjustment info: ${cartAdjustment.toString()} -->
              <tr>
                <td colspan="5" nowrap align="right">
                    <div class="tabletext">
                        <i>${uiLabelMap.EcommerceAdjustment}</i> - ${adjustmentType.get("description",locale)?if_exists}
                        <#if cartAdjustment.productPromoId?has_content><a href="<@ofbizUrl>showPromotionDetails?productPromoId=${cartAdjustment.productPromoId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonDetails}</a></#if>:
                    </div>
                </td>
                <td nowrap align="right"><div class="tabletext"><@ofbizCurrency amount=Static["org.ofbiz.order.order.OrderReadHelper"].calcOrderAdjustment(cartAdjustment, shoppingCart.getSubTotal()) isoCode=shoppingCart.getCurrency()/></div></td>
                <td>&nbsp;</td>
              </tr>
            </#list>
        </#if>

        <#if (shoppingCart.getTotalSalesTax() > 0.0)>
        <tr>
          <td colspan="5" align="right" valign="bottom">
            <div class="tabletext">${uiLabelMap.OrderSalesTax}:</div>
          </td>
          <td colspan="2" align="right" valign="bottom">
            <div class="tabletext"><@ofbizCurrency amount=shoppingCart.getTotalSalesTax() isoCode=shoppingCart.getCurrency()/></div>
          </td>
        </tr>
        </#if>
        <tr>
          <td colspan="5" align="right" valign="bottom">
            <div class="tabletext"><b>${uiLabelMap.EcommerceCartTotal}:</b></div>
          </td>
          <td colspan="2" align="right" valign="bottom">
            <hr size="1" class="sepbar">
            <div class="tabletext"><b><@ofbizCurrency amount=shoppingCart.getDisplayGrandTotal() isoCode=shoppingCart.getCurrency()/></b></div>
          </td>
        </tr>
        <#if itemsFromList>
        <tr>
          <td valign="bottom" colspan="7"><div class="tabletext">L - ${uiLabelMap.EcommerceItemsfromShopingList}.</td>
        </tr>
        </#if>
        <#if promoItems>
        <tr>
          <td valign="bottom" colspan="7"><div class="tabletext">P - ${uiLabelMap.EcommercePromotionalItems}.</td>
        </tr>
        </#if>
        <#if !itemsFromList && !promoItems>
        <tr>
          <td colspan="7">&nbsp;</td>
        </tr>
        </#if>
        <tr><td>&nbsp;</td><td colspan="6"><hr class="sepbar"/></td></tr>
        <tr>
          <td colspan="7" align="right" valign="bottom">
            <div class="tabletext">
              <#if sessionAttributes.userLogin?has_content && sessionAttributes.userLogin.userLoginId != "anonymous">
              <select name="shoppingListId" class="selectBox">
                <#if shoppingLists?has_content>
                  <#list shoppingLists as shoppingList>
                    <option value="${shoppingList.shoppingListId}">${shoppingList.listName}</option>
                  </#list>
                </#if>
                <option value="">---</option>
                <option value="">${uiLabelMap.EcommerceNewShoppingList}</option>
              </select>
              &nbsp;&nbsp;
              <a href="javascript:addToList();" class="buttontext">${uiLabelMap.EcommerceAddSelectedtoList}</a>&nbsp;&nbsp;
              <#else>
               ${uiLabelMap.EcommerceYouMust} <a href="<@ofbizUrl>checkLogin/showcart</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonLogin}</a>
                ${uiLabelMap.EcommerceToAddSelectedItemsToShoppingList}.&nbsp;
              </#if>
            </div>
          </td>
        </tr>
        <tr><td>&nbsp;</td><td colspan="6"><hr class="sepbar"/></td></tr>
        <tr>
          <td colspan="7" align="right" valign="bottom">
            <div class="tabletext">
              <#if sessionAttributes.userLogin?has_content && sessionAttributes.userLogin.userLoginId != "anonymous">
              &nbsp;&nbsp;
              <a href="<@ofbizUrl>createCustRequestFromCart</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderCreateCustRequestFromCart}</a>&nbsp;&nbsp;
              &nbsp;&nbsp;
              <a href="<@ofbizUrl>createQuoteFromCart</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderCreateQuoteFromCart}</a>&nbsp;&nbsp;
              <#else>
               ${uiLabelMap.EcommerceYouMust} <a href="<@ofbizUrl>checkLogin/showcart</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonLogin}</a>
                ${uiLabelMap.EcommerceToOrderCreateCustRequestFromCart}.&nbsp;
              </#if>
            </div>
          </td>
        </tr>
        <tr><td>&nbsp;</td><td colspan="6"><hr class="sepbar"/></td></tr>
        <tr>
          <td colspan="7" align="center" valign="bottom">
            <div class="tabletext"><input type="checkbox" onClick="javascript:document.cartform.submit()" name="alwaysShowcart" <#if shoppingCart.viewCartOnAdd()>checked</#if>>&nbsp;${uiLabelMap.EcommerceAlwaysViewCartAfterAddingAnItem}.</div>
          </td>
        </tr>
      </table>
    </form>
  <#else>
    <div class="head2">${uiLabelMap.EcommerceYourShoppingCartEmpty}.</div>
  </#if>
<#-- Copy link bar to bottom to include a link bar at the bottom too -->
    </div>
</div>

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.ProductPromoCodes}</div>
    </div>
    <div class="screenlet-body">
        <div class="tabletext">
            <form method="post" action="<@ofbizUrl>addpromocode<#if requestAttributes._CURRENT_VIEW_?has_content>/${requestAttributes._CURRENT_VIEW_}</#if></@ofbizUrl>" name="addpromocodeform" style="margin: 0;">
                <input type="text" class="inputBox" size="15" name="productPromoCodeId" value="">
                <input type="submit" class="smallSubmit" value="${uiLabelMap.OrderAddCode}">
                <#assign productPromoCodeIds = (shoppingCart.getProductPromoCodesEntered())?if_exists>
                <#if productPromoCodeIds?has_content>
                    ${uiLabelMap.ProductPromoCodesEntered}
                    <#list productPromoCodeIds as productPromoCodeId>
                        ${productPromoCodeId}
                    </#list>
                </#if>
            </form>
        </div>
    </div>
</div>

<#if showPromoText?exists && showPromoText>
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.EcommerceSpecialOffers}</div>
    </div>
    <div class="screenlet-body">
        <#-- show promotions text -->
        <#list productPromos as productPromo>
            <div class="tabletext"><a href="<@ofbizUrl>showPromotionDetails?productPromoId=${productPromo.productPromoId}</@ofbizUrl>" class="linktext">[${uiLabelMap.CommonDetails}]</a> ${productPromo.promoText?if_exists}</div>
            <#if productPromo_has_next>
                <div><hr class="sepbar"/></div>
            </#if>
        </#list>
        <div><hr class="sepbar"/></div>
        <div class="tabletext"><a href="<@ofbizUrl>showAllPromotions</@ofbizUrl>" class="buttontext">${uiLabelMap.EcommerceViewAllPromotions}</a></div>
    </div>
</div>
</#if>

<#if associatedProducts?has_content>
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.EcommerceYouMightAlsoIntrested}:</div>
    </div>
    <div class="screenlet-body">
        <#-- random complementary products -->
        <#list associatedProducts as assocProduct>
            <div>
                ${setRequestAttribute("optProduct", assocProduct)}
                ${setRequestAttribute("listIndex", assocProduct_index)}
                ${screens.render("component://ecommerce/widget/CatalogScreens.xml#productsummary")}
            </div>
            <#if assocProduct_has_next>
                <div><hr class="sepbar"/></div>
            </#if>
        </#list>
    </div>
</div>
</#if>

<#if (shoppingCartSize?default(0) > 0)>
  ${screens.render("component://ecommerce/widget/CartScreens.xml#promoUseDetailsInline")}
</#if>

<!-- Internal cart info: productStoreId=${shoppingCart.getProductStoreId()?if_exists} locale=${shoppingCart.getLocale()?if_exists} currencyUom=${shoppingCart.getCurrency()?if_exists} userLoginId=${(shoppingCart.getUserLogin().getString("userLoginId"))?if_exists} autoUserLogin=${(shoppingCart.getAutoUserLogin().getString("userLoginId"))?if_exists} -->
