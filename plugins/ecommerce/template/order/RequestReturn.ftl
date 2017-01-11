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
    <div class="screenlet-title-bar">
        <div class="boxlink">
          <#if maySelectItems?default(false)>
              <a href="javascript:document.addCommonToCartForm.add_all.value='true';document.addCommonToCartForm.submit()" class="submenutext">${uiLabelMap.OrderAddAllToCart}</a><a href="javascript:document.addCommonToCartForm.add_all.value='false';document.addCommonToCartForm.submit()" class="submenutextright">${uiLabelMap.OrderAddCheckedToCart}</a>
          </#if>
        </div>
        <div class="h3">${uiLabelMap.OrderReturnItems}</div>
    </div>
    <div class="screenlet-body">
        <form name="selectAllForm" method="post" action="<@ofbizUrl>requestReturn</@ofbizUrl>">
          <input type="hidden" name="_checkGlobalScope" value="Y"/>
          <input type="hidden" name="_useRowSubmit" value="Y"/>
          <input type="hidden" name="returnHeaderTypeId" value="CUSTOMER_RETURN"/>
          <input type="hidden" name="fromPartyId" value="${party.partyId}"/>
          <input type="hidden" name="toPartyId" value="${toPartyId!}"/>
          <input type="hidden" name="orderId" value="${orderId}"/>
          <#if (orderHeader.currencyUom)?has_content>
          <input type="hidden" name="currencyUomId" value="${orderHeader.currencyUom}"/>
          </#if>
          <table border="0" width="100%" cellpadding="2" cellspacing="0">
            <tr>
              <td colspan="5"><h3>${uiLabelMap.OrderReturnItemsFromOrder} ${uiLabelMap.CommonNbr}<a href="<@ofbizUrl>orderstatus?orderId=${orderId}</@ofbizUrl>" class="buttontext">${orderId}</h3></td>
              <td align="right">
                <span class="tableheadtext">${uiLabelMap.CommonSelectAll}</span>&nbsp;
                <input type="checkbox" name="selectAll" value="Y" class="selectAll"/>
              </td>
            </tr>
            <tr>
              <td><div class="tableheadtext">${uiLabelMap.CommonDescription}</div></td>
              <td><div class="tableheadtext">${uiLabelMap.CommonQuantity}</div></td>
              <td><div class="tableheadtext">${uiLabelMap.EcommercePrice}</div></td>
              <td><div class="tableheadtext">${uiLabelMap.OrderReason}</div></td>
              <td><div class="tableheadtext">${uiLabelMap.OrderRequestedResponse}</div></td>
              <td>&nbsp;</td>
            </tr>
            <tr><td colspan="6"><hr /></td></tr>
            <#if returnableItems?has_content>
              <#assign rowCount = 0>
              <#list returnableItems.keySet() as orderItem>
              <#if !orderItem.orderAdjustmentId?has_content>    <#-- filter orderAdjustments -->
                <input type="hidden" name="orderId_o_${rowCount}" value="${orderItem.orderId}"/>
                <input type="hidden" name="orderItemSeqId_o_${rowCount}" value="${orderItem.orderItemSeqId}"/>
                <input type="hidden" name="description_o_${rowCount}" value="${orderItem.itemDescription!}"/>
                <#-- <input type="hidden" name="returnItemType_o_${rowCount}" value="ITEM"/> -->
                <#assign returnItemType = returnItemTypeMap.get(returnableItems.get(orderItem).get("itemTypeKey"))/>
                <input type="hidden" name="returnItemTypeId_o_${rowCount}" value="${returnItemType}"/>
                <input type="hidden" name="returnPrice_o_${rowCount}" value="${returnableItems.get(orderItem).get("returnablePrice")}"/>

                <#-- need some order item information -->
                <#assign orderHeader = orderItem.getRelatedOne("OrderHeader", false)>
                <#assign itemCount = orderItem.quantity>
                <#assign itemPrice = orderItem.unitPrice>
                <#-- end of order item information -->

                <tr>
                  <td>
                    <div>
                      <#if orderItem.productId??>
                        &nbsp;<a href="<@ofbizUrl>product?product_id=${orderItem.productId}</@ofbizUrl>" class="buttontext">${orderItem.productId}</a>
                        <input type="hidden" name="productId_o_${rowCount}" value="${orderItem.productId}"/>
                      </#if>
                      ${orderItem.itemDescription}
                    </div>
                  </td>
                  <td>
                    <input type="text" class="inputBox" size="6" name="returnQuantity_o_${rowCount}" value="${returnableItems.get(orderItem).get("returnableQuantity")}"/>
                  </td>
                  <td>
                    <div><@ofbizCurrency amount=returnableItems.get(orderItem).get("returnablePrice") isoCode=orderHeader.currencyUom/></div>
                  </td>
                  <td>
                    <select name="returnReasonId_o_${rowCount}" class="selectBox">
                      <#list returnReasons as reason>
                        <option value="${reason.returnReasonId}">${reason.get("description",locale)?default(reason.returnReasonId)}</option>
                      </#list>
                    </select>
                  </td>
                  <td>
                    <select name="returnTypeId_o_${rowCount}" class="selectBox">
                      <#list returnTypes as type>
                        <option value="${type.returnTypeId}">${type.get("description",locale)?default(type.returnTypeId)}</option>
                      </#list>
                    </select>
                  </td>
                  <td align="right">
                    <input type="checkbox" name="_rowSubmit_o_${rowCount}" value="Y"/>
                  </td>
                </tr>
                <tr><td colspan="6"><hr /></td></tr>
                <#assign rowCount = rowCount + 1>
              </#if>
              </#list>
              <input type="hidden" name="_rowCount" value="${rowCount}"/>
              <tr>
                <td colspan="6"><div class="tableheadtext">${uiLabelMap.OrderSelectShipFromAddress}:</td>
              </tr>
              <tr><td colspan="6"><hr /></td></tr>
              <tr>
                <td colspan="6">
                  <table cellspacing="1" cellpadding="2" width="100%">
                    <#list shippingContactMechList as shippingContactMech>
                      <#assign shippingAddress = shippingContactMech.getRelatedOne("PostalAddress", false)>
                      <tr>
                        <td align="right" width="1%" valign="top" nowrap="nowrap">
                          <input type="radio" name="originContactMechId" value="${shippingAddress.contactMechId}"/>
                        </td>
                        <td width="99%" valign="top" nowrap="nowrap">
                          <div>
                            <#if shippingAddress.toName?has_content><b>${uiLabelMap.CommonTo}:</b>&nbsp;${shippingAddress.toName}<br /></#if>
                            <#if shippingAddress.attnName?has_content><b>${uiLabelMap.PartyAddrAttnName}:</b>&nbsp;${shippingAddress.attnName}<br /></#if>
                            <#if shippingAddress.address1?has_content>${shippingAddress.address1}<br /></#if>
                            <#if shippingAddress.address2?has_content>${shippingAddress.address2}<br /></#if>
                            <#if shippingAddress.city?has_content>${shippingAddress.city}</#if>
                            <#if shippingAddress.stateProvinceGeoId?has_content><br />${shippingAddress.stateProvinceGeoId}</#if>
                            <#if shippingAddress.postalCode?has_content><br />${shippingAddress.postalCode}</#if>
                            <#if shippingAddress.countryGeoId?has_content><br />${shippingAddress.countryGeoId}</#if>
                            <a href="<@ofbizUrl>editcontactmech?DONE_PAGE=checkoutoptions&amp;contactMechId=${shippingAddress.contactMechId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonUpdate}]</a>
                          </div>
                        </td>
                      </tr>
                    </#list>
                  </table>
                </td>
              </tr>
              <tr><td colspan="6"><hr /></td></tr>
              <tr>
                <td colspan="6" align="right">
                  <a href="javascript:document.selectAllForm.submit();" class="buttontext">${uiLabelMap.OrderReturnSelectedItems}</a>
                </td>
              </tr>
            <#else>
              <tr><td colspan="6"><div>${uiLabelMap.OrderNoReturnableItems} ${uiLabelMap.CommonNbr}${orderId}</div></td></tr>
            </#if>
          </table>
        </form>
    </div>
</div>
