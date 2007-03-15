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

<#-- Continuation of showcart.ftl:  List of order items and forms to modify them. -->

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.OrderOrderItems}</div>
    </div>
    <div class="screenlet-body">
  <#if (shoppingCartSize > 0)>
    <form method="post" action="<@ofbizUrl>modifycart</@ofbizUrl>" name="cartform" style="margin: 0;">
      <input type="hidden" name="removeSelected" value="false"/>
      <#if shoppingCart.getOrderType() == "PURCHASE_ORDER">
        <input type="hidden" name="finalizeReqShipInfo" value="false"/>
        <input type="hidden" name="finalizeReqOptions" value="false"/>
        <input type="hidden" name="finalizeReqPayInfo" value="false"/>
        <input type="hidden" name="finalizeReqAdditionalParty" value="false"/>
      </#if>
      <table cellspacing="0" cellpadding="1" border="0">
        <tr>
          <td>&nbsp;</td>
          <td colspan="2">
            <div class="tabletext">
              <b>${uiLabelMap.ProductProduct}</b>
              <#if showOrderGiftWrap?default("true") == "true">
                  <select class="selectBox" name="GWALL" onchange="javascript:gwAll(this);">
                    <option value="">${uiLabelMap.OrderGiftWrapAllItems}</option>
                    <option value="NO^">${uiLabelMap.OrderNoGiftWrap}</option>
                    <#if allgiftWraps?has_content>
                      <#list allgiftWraps as option>
                        <option value="${option.productFeatureId?default("")}">${option.description?default("")} : <@ofbizCurrency amount=option.defaultAmount?default(0) isoCode=currencyUomId/></option>
                      </#list>
                    </#if>
                  </select>
              </#if>
            </div>
          </td>
          <td align="center"><div class="tabletext"><b>${uiLabelMap.OrderQuantity}</b></div></td>
          <td align="right"><div class="tabletext"><b>${uiLabelMap.CommonUnitPrice}</b></div></td>
          <td align="right"><div class="tabletext"><b>${uiLabelMap.OrderAdjustments}</b></div></td>
          <td align="right"><div class="tabletext"><b>${uiLabelMap.OrderItemTotal}</b></div></td>
          <td align="center"><input type="checkbox" name="selectAll" value="0" onclick="javascript:toggleAll(this);"></td>
        </tr>

        <#assign itemsFromList = false>
        <#list shoppingCart.items() as cartLine>
          <#assign cartLineIndex = shoppingCart.getItemIndex(cartLine)>
          <#assign lineOptionalFeatures = cartLine.getOptionalProductFeatures()>
          <tr><td colspan="8"><hr class="sepbar"></td></tr>
          <tr valign="top">
            <td>&nbsp;</td>         
            <td>
          <table border="0">
          <tr><td colspan="2">
                <div class="tabletext">                    
                  <#if cartLine.getProductId()?exists>
                    <#-- product item -->
                    <a href="<@ofbizUrl>product?product_id=${cartLine.getProductId()}</@ofbizUrl>" class="buttontext">${cartLine.getProductId()}</a> -
                    <input size="60" class="inputBox" type="text" name="description_${cartLineIndex}" value="${cartLine.getName()?default("")}"/><br/>
                    <i>${cartLine.getDescription()?if_exists}</i>
                    <#if shoppingCart.getOrderType() != "PURCHASE_ORDER">
                      <#-- only applies to sales orders, not purchase orders -->
                      <#-- if inventory is not required check to see if it is out of stock and needs to have a message shown about that... -->
                      <#assign itemProduct = cartLine.getProduct()>
                      <#assign isStoreInventoryNotRequiredAndNotAvailable = Static["org.ofbiz.product.store.ProductStoreWorker"].isStoreInventoryRequiredAndAvailable(request, itemProduct, cartLine.getQuantity(), false, false)>
                      <#if isStoreInventoryNotRequiredAndNotAvailable && itemProduct.inventoryMessage?has_content>
                          <b>(${itemProduct.inventoryMessage})</b>
                      </#if>                                          
                    </#if>   
                  <#else>
                    <#-- this is a non-product item -->
                    <b>${cartLine.getItemTypeDescription()?if_exists}</b> : ${cartLine.getName()?if_exists}
                  </#if>
                    <#-- display the item's features -->
                   <#assign features = "">
                   <#if cartLine.getFeaturesForSupplier(dispatcher,shoppingCart.getPartyId())?has_content>
                       <#assign features = cartLine.getFeaturesForSupplier(dispatcher, shoppingCart.getPartyId())>
                   <#elseif cartLine.getStandardFeatureList()?has_content>
                       <#assign features = cartLine.getStandardFeatureList()>
                   </#if>
                   <#if features?has_content>
                     <br/><i>${uiLabelMap.ProductFeatures}: <#list features as feature>${feature.description?default("")} </#list></i>
                   </#if>
                    <#-- show links to survey response for this item -->
                    <#if cartLine.getAttribute("surveyResponses")?has_content>
                        <br/>Surveys: 
                       <#list cartLine.getAttribute("surveyResponses") as surveyResponseId>
                        <a href="/content/control/ViewSurveyResponses?surveyResponseId=${surveyResponseId}&externalLoginKey=${externalLoginKey}" class="buttontext" style="font-size: xx-small;">${surveyResponseId}</a> 
                       </#list>
                    </#if>
                </div>
            </td></tr>
            <#if cartLine.getRequirementId()?has_content>
                <tr>
                    <td colspan="2" align="left">
                      <div class="tabletext"><b>${uiLabelMap.OrderRequirementId}</b>: ${cartLine.getRequirementId()?if_exists}</div>
                    </td>
                </tr>
            </#if>
            <#if cartLine.getQuoteId()?has_content>
                <#if cartLine.getQuoteItemSeqId()?has_content>
                  <tr>
                    <td colspan="2" align="left">
                      <div class="tabletext"><b>${uiLabelMap.OrderOrderQuoteId}</b>: ${cartLine.getQuoteId()?if_exists} - ${cartLine.getQuoteItemSeqId()?if_exists}</div>
                    </td>
                  </tr>
                </#if>
            </#if>
            <#if cartLine.getItemComment()?has_content>
              <tr><td align="left"><div class="tableheadtext">${uiLabelMap.CommonComment} : </div></td>
                  <td align="left"><div class="tabletext">${cartLine.getItemComment()?if_exists}</div>
              </td></tr>
            </#if>
            <#if cartLine.getDesiredDeliveryDate()?has_content>
              <tr><td align="left"><div class="tableheadtext">${uiLabelMap.OrderDesiredDeliveryDate}: </div></td>
                  <td align="left"><div class="tabletext">${cartLine.getDesiredDeliveryDate()?if_exists}</div>
              </td></tr>
            </#if>
            <#-- inventory summary -->
            <#if cartLine.getProductId()?exists>
              <#assign productId = cartLine.getProductId()>
              <#assign product = cartLine.getProduct()>
              <tr>
                <td colspan="2" align="left">
                  <div class="tabletext">
                    <a href="/catalog/control/EditProductInventoryItems?productId=${productId}" class="buttontext"><b>${uiLabelMap.ProductInventory}</b></a>: 
                    ${uiLabelMap.ProductAtp} = ${availableToPromiseMap.get(productId)}, ${uiLabelMap.ProductQoh} = ${quantityOnHandMap.get(productId)}
                    <#if product.productTypeId == "MARKETING_PKG_AUTO"> 
                    ${uiLabelMap.ProductMarketingPackageATP} = ${mktgPkgATPMap.get(productId)}, ${uiLabelMap.ProductMarketingPackageQOH} = ${mktgPkgQOHMap.get(productId)}
                    </#if>
                  </div>
                </td>
              </tr>
            </#if>
            <#if shoppingCart.getOrderType() == "PURCHASE_ORDER">
              <#assign currentOrderItemType = cartLine.getItemTypeGenericValue()?if_exists/>
                <tr>
                  <td align="left">
                    <div class="tabletext">
                      ${uiLabelMap.OrderOrderItemType}:
                      <select name="itemType_${cartLineIndex}" class="selectBox">
                        <#if currentOrderItemType?has_content>
                        <option value="${currentOrderItemType.orderItemTypeId}">${currentOrderItemType.get("description",locale)}</option>
                        <option value="${currentOrderItemType.orderItemTypeId}">---</option>
                        </#if>
                        <option value="">&nbsp;</option>
                        <#list purchaseOrderItemTypeList as orderItemType>
                        <option value="${orderItemType.orderItemTypeId}">${orderItemType.get("description",locale)}</option>
                        </#list>
                      </select>
                    </div>
                  </td>
                </tr>
            </#if>
            
            <#-- ship before/after date -->
            <tr>
              <td colspan="2">
               <table border="0" cellpadding="0" cellspacing="0" width="100%">
               <tr>
                <td align="left">
                  <div class="tabletext">${uiLabelMap.OrderShipAfterDate}
                    <input type="text" class="inputBox" size="20" maxlength="30" name="shipAfterDate_${cartLineIndex}" 
                      value="${cartLine.getShipAfterDate()?default("")}"/>
                    <a href="javascript:call_cal(document.cartform.shipAfterDate_${cartLineIndex},'${shoppingCart.getShipAfterDate()?default("")}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="${uiLabelMap.calendar_click_here_for_calendar}"/></a>
                  </div>
                </td>
                <td>&nbsp;</td>
                <td align="left">
                  <div class="tabletext">${uiLabelMap.OrderShipBeforeDate}
                    <input type="text" class="inputBox" size="20" maxlength="30" name="shipBeforeDate_${cartLineIndex}" 
                      value="${cartLine.getShipBeforeDate()?default("")}"/>
                    <a href="javascript:call_cal(document.cartform.shipBeforeDate_${cartLineIndex},'${shoppingCart.getShipBeforeDate()?default("")}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="${uiLabelMap.calendar_click_here_for_calendar}"/></a>
                  </div>
                </td>
               </tr>
               </table>
              </td>
            </tr>
          </table>

                <#if (cartLine.getIsPromo() && cartLine.getAlternativeOptionProductIds()?has_content)>
                  <#-- Show alternate gifts if there are any... -->
                  <div class="tableheadtext">${uiLabelMap.OrderChooseFollowingForGift}:</div>
                  <#list cartLine.getAlternativeOptionProductIds() as alternativeOptionProductId>
                    <#assign alternativeOptionProduct = delegator.findByPrimaryKeyCache("Product", Static["org.ofbiz.base.util.UtilMisc"].toMap("productId", alternativeOptionProductId))>
                    <#assign alternativeOptionName = Static["org.ofbiz.product.product.ProductContentWrapper"].getProductContentAsText(alternativeOptionProduct, "PRODUCT_NAME", locale, dispatcher)?if_exists>
                    <div class="tabletext"><a href="<@ofbizUrl>setDesiredAlternateGwpProductId?alternateGwpProductId=${alternativeOptionProductId}&alternateGwpLine=${cartLineIndex}</@ofbizUrl>" class="buttontext">Select: ${alternativeOptionName?default(alternativeOptionProductId)}</a></div>
                  </#list>
                </#if>
            </td>

            <#-- gift wrap option -->
            <#assign showNoGiftWrapOptions = false>
            <td nowrap align="right">
              <#assign giftWrapOption = lineOptionalFeatures.GIFT_WRAP?if_exists>
              <#assign selectedOption = cartLine.getAdditionalProductFeatureAndAppl("GIFT_WRAP")?if_exists>
              <#if giftWrapOption?has_content>
                <select class="selectBox" name="option^GIFT_WRAP_${cartLineIndex}" onchange="javascript:document.cartform.submit()">
                  <option value="NO^">${uiLabelMap.OrderNoGiftWrap}</option>
                  <#list giftWrapOption as option>
                    <option value="${option.productFeatureId}" <#if ((selectedOption.productFeatureId)?exists && selectedOption.productFeatureId == option.productFeatureId)>SELECTED</#if>>${option.description} : <@ofbizCurrency amount=option.amount?default(0) isoCode=currencyUomId/></option>
                  </#list>
                </select>
              <#elseif showNoGiftWrapOptions>
                <select class="selectBox" name="option^GIFT_WRAP_${cartLineIndex}" onchange="javascript:document.cartform.submit()">
                  <option value="">${uiLabelMap.OrderNoGiftWrap}</option>
                </select>
              <#else>
                &nbsp;
              </#if>
            </td>
            <#-- end gift wrap option -->
            <td nowrap align="center">
              <div class="tabletext">
                <#if cartLine.getIsPromo() || cartLine.getShoppingListId()?exists>
                    ${cartLine.getQuantity()?string.number}
                <#else>
                    <input size="6" class="inputBox" type="text" name="update_${cartLineIndex}" value="${cartLine.getQuantity()?string.number}"/>
                </#if>
              </div>
            </td>
            <td nowrap align="right">
              <div class="tabletext">
                <#if cartLine.getIsPromo() || (shoppingCart.getOrderType() == "SALES_ORDER" && !security.hasEntityPermission("ORDERMGR", "_SALES_PRICEMOD", session))>
                  <@ofbizCurrency amount=cartLine.getDisplayPrice() isoCode=currencyUomId/>
                <#else>
                  <input size="6" class="inputBox" type="text" name="price_${cartLineIndex}" value="${cartLine.getBasePrice()}"/>
                </#if>
              </div>
            </td>
            <td nowrap align="right"><div class="tabletext"><@ofbizCurrency amount=cartLine.getOtherAdjustments() isoCode=currencyUomId/></div></td>
            <td nowrap align="right"><div class="tabletext"><@ofbizCurrency amount=cartLine.getDisplayItemSubTotal() isoCode=currencyUomId/></div></td>
            <td nowrap align="center"><div class="tabletext"><#if !cartLine.getIsPromo()><input type="checkbox" name="selectedItem" value="${cartLineIndex}" onclick="javascript:checkToggle(this);"><#else>&nbsp;</#if></div></td>
          </tr>
        </#list>

        <#if shoppingCart.getAdjustments()?has_content>
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
              <tr>
                <td colspan="4" nowrap align="right"><div class="tabletext">${uiLabelMap.OrderSubTotal}:</div></td>
                <td nowrap align="right"><div class="tabletext"><@ofbizCurrency amount=shoppingCart.getSubTotal() isoCode=currencyUomId/></div></td>
                <td>&nbsp;</td>
              </tr>
            <#list shoppingCart.getAdjustments() as cartAdjustment>
              <#assign adjustmentType = cartAdjustment.getRelatedOneCache("OrderAdjustmentType")>
              <tr>
                <td colspan="4" nowrap align="right">
                  <div class="tabletext">
                    <i>Adjustment</i> - ${adjustmentType.get("description",locale)?if_exists}
                    <#if cartAdjustment.productPromoId?has_content><a href="<@ofbizUrl>showPromotionDetails?productPromoId=${cartAdjustment.productPromoId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonDetails}</a></#if>:
                  </div>
                </td>
                <td nowrap align="right"><div class="tabletext"><@ofbizCurrency amount=Static["org.ofbiz.order.order.OrderReadHelper"].calcOrderAdjustment(cartAdjustment, shoppingCart.getSubTotal()) isoCode=currencyUomId/></div></td>
                <td>&nbsp;</td>
              </tr>
            </#list>
        </#if>
        
        <tr> 
          <td colspan="6" align="right" valign="bottom">
            <div class="tabletext"><b>${uiLabelMap.OrderCartTotal}:</b></div>
          </td>
          <td align="right" valign="bottom">
            <hr class="sepbar"/>
            <div class="tabletext"><b><@ofbizCurrency amount=shoppingCart.getGrandTotal() isoCode=currencyUomId/></b></div>
          </td>
        </tr>       
        <tr>
          <td colspan="8">&nbsp;</td>
        </tr>      
      </table>    
    </form>
  <#else>
    <div class="tabletext">${uiLabelMap.OrderNoOrderItemsToDisplay}</div>
  </#if>
    </div>
</div>

