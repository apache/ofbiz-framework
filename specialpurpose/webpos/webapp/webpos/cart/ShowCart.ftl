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
</script>
<div class="pos-cart-scroll">
    <table class="basic-table" cellspacing="0">
        <tr class="header-row">
            <td nowrap="nowrap">&nbsp;</td>
            <td nowrap="nowrap"><b>${uiLabelMap.OrderProduct}</b></td>
            <td nowrap="nowrap">&nbsp;</td>
            <td nowrap="nowrap" align="center"><b>${uiLabelMap.CommonQuantity}</b></td>
            <td nowrap="nowrap" align="right"><b>${uiLabelMap.WebPosUnitPrice}</b></td>
            <td nowrap="nowrap" align="right"><b>${uiLabelMap.WebPosAdjustments}</b></td>
            <td nowrap="nowrap" align="right"><b>${uiLabelMap.WebPosItemTotal}</b></td>
            <td nowrap="nowrap" align="center"><input type="checkbox" name="selectAll" value="0" onclick="javascript:toggleAll(this);" /></td>
        </tr>
        <#if (shoppingCartSize > 0)>
            <form method="post" action="<@ofbizUrl>ModifyCart</@ofbizUrl>" name="cartform">
              <input type="hidden" name="removeSelected" value="false" />
                <#assign itemsFromList = false>
                <#-- set initial row color -->
                <#assign alt_row = false>
                <#list shoppingCart.items() as cartLine>
                  <#assign cartLineIndex = shoppingCart.getItemIndex(cartLine)>
                  <#-- show adjustment info -->
                  <#list cartLine.getAdjustments() as cartLineAdjustment>
                    <!-- cart line ${cartLineIndex} adjustment: ${cartLineAdjustment} -->
                  </#list>
                  <tr <#if alt_row>class="alternate-row pos-cart-hover-bar"<#else>class="pos-cart-hover-bar"</#if>>
                    <td>&nbsp;</td>
                    <td>
                        <div>
                          <#if cartLine.getProductId()?exists>
                            <#-- product item -->
                            <#-- start code to display a small image of the product -->
                            <#if cartLine.getParentProductId()?exists>
                              <#assign parentProductId = cartLine.getParentProductId()/>
                            <#else>
                              <#assign parentProductId = cartLine.getProductId()/>
                            </#if>
                            <#assign smallImageUrl = Static["org.ofbiz.product.product.ProductContentWrapper"].getProductContentAsText(cartLine.getProduct(), "SMALL_IMAGE_URL", locale, dispatcher)?if_exists>
                            <#if !smallImageUrl?string?has_content><#assign smallImageUrl = "/images/defaultImage.jpg"></#if>
                            <#if smallImageUrl?string?has_content>
                                <img src="<@ofbizContentUrl>${requestAttributes.contentPathPrefix?if_exists}${smallImageUrl}</@ofbizContentUrl>" align="left" width="50" class="imageborder" border="0" alt="" />
                            </#if>
                            <#-- end code to display a small image of the product -->
                            ${cartLine.getProductId()} - ${cartLine.getName()?if_exists} : ${cartLine.getDescription()?if_exists}
                          <#else>
                            <#-- this is a non-product item -->
                            <b>${cartLine.getItemTypeDescription()?if_exists}</b> : ${cartLine.getName()?if_exists}
                          </#if>
                        </div>
                    </td>
                    <td nowrap="nowrap" align="right">
                        &nbsp;
                    </td>
                    <td nowrap="nowrap" align="center">
                      <div>
                        <input size="6" type="text" name="update_${cartLineIndex}" value="${cartLine.getQuantity()?string.number}" />
                      </div>
                    </td>
                    <td nowrap="nowrap" align="right"><div><@ofbizCurrency amount=cartLine.getDisplayPrice() isoCode=shoppingCart.getCurrency()/></div></td>
                    <td nowrap="nowrap" align="right"><div><@ofbizCurrency amount=cartLine.getOtherAdjustments() isoCode=shoppingCart.getCurrency()/></div></td>
                    <td nowrap="nowrap" align="right"><div><@ofbizCurrency amount=cartLine.getDisplayItemSubTotal() isoCode=shoppingCart.getCurrency()/></div></td>
                    <td nowrap="nowrap" align="center"><div><#if !cartLine.getIsPromo()><input type="checkbox" name="selectedItem" value="${cartLineIndex}" onclick="javascript:checkToggle(this);" /><#else>&nbsp;</#if></div></td>
                  </tr>
                  <#-- toggle the row color -->
                  <#assign alt_row = !alt_row>
                </#list>
            </form>
        </#if>
    </table>
</div>