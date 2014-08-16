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

<#if security.hasEntityPermission("ORDERMGR", "_CREATE", session) || security.hasEntityPermission("ORDERMGR", "_PURCHASE_CREATE", session)>
<form method="post" action="<@ofbizUrl>finalizeOrder</@ofbizUrl>" name="checkoutsetupform">
  <input type="hidden" name="finalizeMode" value="removeEmptyShipGroups"/>
</form>

<table border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
<tr>
    <td width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
        <tr>
          <td>
            <#list 1..shoppingCart.getShipGroupSize() as currIndex>
              <#assign shipGroupIndex = currIndex - 1>
              <#assign supplier =  delegator.findOne("PartyGroup", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", shoppingCart.getSupplierPartyId(shipGroupIndex)), false)! />
              <table width="100%" cellpadding="1" border="0" cellpadding="0" cellspacing="0">
              <tr>
                <td colspan="2">
                    <h1>${uiLabelMap.OrderShipGroup} ${uiLabelMap.CommonNbr} ${currIndex}<#if supplier?has_content> - ${uiLabelMap.OrderDropShipped} - ${supplier.groupName?default(supplier.partyId)}</#if></h1>
                </td>
              </tr>
              <tr>
                <td>
                    <div>${uiLabelMap.ProductProduct}</div>
                </td>
                <td>
                    <div>${uiLabelMap.CommonQuantity}</div>
                </td>
                <td>
                    <div>${uiLabelMap.ProductMoveQuantity}</div>
                </td>
                <td>
                    <div>${uiLabelMap.OrderShipGroupTo}</div>
                </td>
              </tr>

              <#assign shipGroupItems = shoppingCart.getShipGroupItems(shipGroupIndex)>
              <#assign shoppingCartItems = shipGroupItems.keySet().iterator()>
              <form method="post" action="<@ofbizUrl>assignItemToShipGroups</@ofbizUrl>" name="assignitemtoshipgroup${shipGroupIndex}">
              <input type="hidden" name="_useRowSubmit" value="N" />
              <#assign rowCount = 0>
              <#list shoppingCartItems as shoppingCartItem>
                <#assign cartLineIndex = shoppingCart.getItemIndex(shoppingCartItem)>
                <#assign shipGroupItemQuantity = shipGroupItems.get(shoppingCartItem)>
                <input type="hidden" name="itemIndex_o_${rowCount}" value="${cartLineIndex}"/>
                <input type="hidden" name="clearEmptyGroups_o_${rowCount}" value="false"/>
                <input type="hidden" name="fromGroupIndex_o_${rowCount}" value="${shipGroupIndex}"/>
                <tr>
                  <td>
                    <div>[${shoppingCartItem.getProductId()}] ${shoppingCartItem.getName()!}: ${shoppingCartItem.getDescription()!}</div>
                  </td>
                  <td>
                    <div>${shipGroupItemQuantity}</div>
                  </td>
                  <td>
                    <div><input type="text" name="quantity_o_${rowCount}" value="${shipGroupItemQuantity}"/></div>
                  </td>
                  <td>
                    <div>
                    <select name="toGroupIndex_o_${rowCount}">
                      <option value="${shipGroupIndex}">---</option>
                      <#list 0..(shoppingCart.getShipGroupSize() - 1) as groupIdx>
                        <#assign groupNumber = groupIdx + 1>
                        <option value="${groupIdx}">${uiLabelMap.CommonGroup} ${uiLabelMap.CommonNbr} ${groupNumber}</option>
                      </#list>
                    </select>
                    </div>
                  </td>
                </tr>
                <#assign rowCount = rowCount + 1>
              </#list>
              <#if (rowCount > 0)>
              <tr>
                <td colspan="3">&nbsp;</td>
                <td>
                <input type="submit" class="smallSubmit" value="${uiLabelMap.CommonSubmit}"/>
                </td>
              </tr>
              </#if>
              </table>
            <input type="hidden" name="_rowCount" value="${rowCount}" />
            </form>
            </#list>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>

<br />
<#else>
  <h3>${uiLabelMap.OrderViewPermissionError}</h3>
</#if>
