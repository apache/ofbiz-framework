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

<script>
    <!-- function to add extra info for Timestamp format -->
    function TimestampSubmit(obj) {
       if (obj.elements["reservStartStr"].value.length == 10) {
           obj.elements["reservStart"].value = obj.elements["reservStartStr"].value + " 00:00:00.000000000";
       } else {
           obj.elements["reservStart"].value = obj.elements["reservStartStr"].value;
       }
       obj.submit();
    }
</script>


<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxlink">
            <a href="<@ofbizUrl>createEmptyShoppingList?productStoreId=${productStoreId}</@ofbizUrl>" class="submenutextright">${uiLabelMap.CommonCreateNew}</a>
        </div>
        <div class="boxhead">&nbsp;${uiLabelMap.EcommerceShoppingLists}</div>
    </div>
    <div class="screenlet-body">
        <#if shoppingLists?has_content>
          <form name="selectShoppingList" method="post" action="<@ofbizUrl>editShoppingList</@ofbizUrl>">
            <select name="shoppingListId" class="selectBox">
              <#if shoppingList?has_content>
                <option value="${shoppingList.shoppingListId}">${shoppingList.listName}</option>
                <option value="${shoppingList.shoppingListId}">--</option>
              </#if>
              <#list shoppingLists as list>
                <option value="${list.shoppingListId}">${list.listName}</option>
              </#list>
            </select>
            &nbsp;&nbsp;
            <a href="javascript:document.selectShoppingList.submit();" class="buttontext">[${uiLabelMap.CommonEdit}]</a>
          </form>
        <#else>
          <div class="tabletext">${uiLabelMap.EcommerceNoShoppingListsCreate}.</div>
          <a href="<@ofbizUrl>createEmptyShoppingList?productStoreId=${productStoreId}</@ofbizUrl>" class="submenutextright">${uiLabelMap.CommonCreateNew}</a>          
        </#if>
    </div>
</div>

<#if shoppingList?has_content>
    <#if canView>

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxlink">
            <a href="<@ofbizUrl>createCustRequestFromShoppingList?shoppingListId=${shoppingList.shoppingListId}</@ofbizUrl>" class="submenutext">${uiLabelMap.OrderCreateCustRequestFromShoppingList}</a>
            <a href="<@ofbizUrl>createQuoteFromShoppingList?shoppingListId=${shoppingList.shoppingListId}</@ofbizUrl>" class="submenutext">${uiLabelMap.OrderCreateQuoteFromShoppingList}</a>
            <a href="javascript:document.updateList.submit();" class="submenutextright">${uiLabelMap.CommonSave}</a>            
        </div>
        <div class="boxhead">&nbsp;${uiLabelMap.EcommerceShoppingListDetail} - ${shoppingList.listName}</div>
    </div>
    <div class="screenlet-body">
        <form name="updateList" method="post" action="<@ofbizUrl>updateShoppingList</@ofbizUrl>">
            <input type="hidden" class="inputBox" name="shoppingListId" value="${shoppingList.shoppingListId}"/>
            <input type="hidden" class="inputBox" name="partyId" value="${shoppingList.partyId?if_exists}"/>
            <table border="0" width="100%" cellspacing="0" cellpadding="0">
              <tr>
                <td><div class="tableheadtext">${uiLabelMap.EcommerceListName}</div></td>
                <td><input type="text" class="inputBox" size="25" name="listName" value="${shoppingList.listName}">
              </tr>
              <tr>
                <td><div class="tableheadtext">${uiLabelMap.CommonDescription}</div></td>
                <td><input type="text" class="inputBox" size="70" name="description" value="${shoppingList.description?if_exists}">
              </tr>
              <tr>
                <td><div class="tableheadtext">${uiLabelMap.EcommerceListType}</div></td>
                <td>
                  <select name="shoppingListTypeId" class="selectBox">
                      <#if shoppingListType?exists>
                      <option value="${shoppingListType.shoppingListTypeId}">${shoppingListType.get("description",locale)?default(shoppingListType.shoppingListTypeId)}</option>
                      <option value="${shoppingListType.shoppingListTypeId}">--</option>
                    </#if>
                    <#list shoppingListTypes as shoppingListType>
                      <option value="${shoppingListType.shoppingListTypeId}">${shoppingListType.get("description",locale)?default(shoppingListType.shoppingListTypeId)}</option>
                    </#list>
                  </select>
                </td>
              </tr>                           
              <tr>
                <td><div class="tableheadtext">${uiLabelMap.EcommercePublic}?</div></td>
                <td>
                  <select name="isPublic" class="selectBox">
                    <option value="${shoppingList.isPublic}"><#if shoppingList.isPublic == "Y">${uiLabelMap.CommonY}<#else>${uiLabelMap.CommonN}</#if></option>
                    <option value="${shoppingList.isPublic}">--</option>
                    <option value="Y">${uiLabelMap.CommonY}</option>
                    <option value="N">${uiLabelMap.CommonN}</option>
                  </select>
                </td>
              </tr>
              <tr>
                <td><div class="tableheadtext">${uiLabelMap.EcommerceActive}?</div></td>
                <td>
                  <select name="isActive" class="selectBox">
                    <option value="${shoppingList.isActive}"><#if shoppingList.isActive == "Y">${uiLabelMap.CommonY}<#else>${uiLabelMap.CommonN}</#if></option>
                    <option value="${shoppingList.isActive}">--</option>
                    <option value="Y">${uiLabelMap.CommonY}</option>
                    <option value="N">${uiLabelMap.CommonN}</option>
                  </select>
                </td>
              </tr>
              <tr>
                <td><div class="tableheadtext">${uiLabelMap.EcommerceParentList}</div></td>
                <td>
                  <select name="parentShoppingListId" class="selectBox">
                      <#if parentShoppingList?exists>
                      <option value="${parentShoppingList.shoppingListId}">${parentShoppingList.listName?default(parentShoppingList.shoppingListId)}</option>
                    </#if>
                    <option value="">${uiLabelMap.EcommerceNoParent}</option>
                    <#list allShoppingLists as newParShoppingList>
                      <option value="${newParShoppingList.shoppingListId}">${newParShoppingList.listName?default(newParShoppingList.shoppingListId)}</option>
                    </#list>
                  </select>
                  <#if parentShoppingList?exists>
                    <a href="<@ofbizUrl>editShoppingList?shoppingListId=${parentShoppingList.shoppingListId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonGotoParent} (${parentShoppingList.listName?default(parentShoppingList.shoppingListId)})</a>
                  </#if>
                </td>
              </tr>                           
              <tr>
                <td><div class="tableheadtext">&nbsp;</div></td>
                <td align="left">
                  <a href="javascript:document.updateList.submit();" class="buttontext">[${uiLabelMap.CommonSave}]</a>         
                </td>
              </tr>
            </table>
        </form>           
    </div>
</div>

<#if shoppingListType?exists && shoppingListType.shoppingListTypeId == "SLT_AUTO_REODR">
  <#assign nowTimestamp = Static["org.ofbiz.base.util.UtilDateTime"].monthBegin()>
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxlink">
            <a href="javascript:document.reorderinfo.submit();" class="submenutextright">${uiLabelMap.CommonSave}</a>
        </div>
        <div class="boxhead">
            &nbsp;${uiLabelMap.EcommerceShoppingListReorder} - ${shoppingList.listName}
            <#if shoppingList.isActive?default("N") == "N">
                <font color="yellow">[${uiLabelMap.OrderNotActive}]</font>
            </#if>
        </div>
    </div>
    <div class="screenlet-body">
        <form name="reorderinfo" method="post" action="<@ofbizUrl>updateShoppingList</@ofbizUrl>">
            <input type="hidden" name="shoppingListId" value="${shoppingList.shoppingListId}"/>
            <table width="100%" cellspacing="0" cellpadding="1" border="0">
              <tr>
                <td><div class="tableheadtext">${uiLabelMap.EcommerceRecurrence}</div></td>
                <td>
                  <#if recurrenceInfo?has_content>
                    <#assign recurrenceRule = recurrenceInfo.getRelatedOne("RecurrenceRule")?if_exists>
                  </#if>
                  <select name="intervalNumber" class="selectBox">
                    <option value="">${uiLabelMap.EcommerceSelectInterval}</option>
                    <option value="1" <#if (recurrenceRule.intervalNumber)?default(0) == 1>selected</#if>>${uiLabelMap.EcommerceEveryDay}</option>
                    <option value="2" <#if (recurrenceRule.intervalNumber)?default(0) == 2>selected</#if>>${uiLabelMap.EcommerceEveryOther}</option>
                    <option value="3" <#if (recurrenceRule.intervalNumber)?default(0) == 3>selected</#if>>${uiLabelMap.EcommerceEvery3rd}</option>
                    <option value="6" <#if (recurrenceRule.intervalNumber)?default(0) == 6>selected</#if>>${uiLabelMap.EcommerceEvery6th}</option>
                    <option value="9" <#if (recurrenceRule.intervalNumber)?default(0) == 9>selected</#if>>${uiLabelMap.EcommerceEvery9th}</option>
                  </select>
                  &nbsp;
                  <select name="frequency" class="selectBox">
                    <option value="">${uiLabelMap.EcommerceSelectFrequency}</option>
                    <option value="5" <#if (recurrenceRule.frequency)?default("") == "WEEKLY">selected</#if>>${uiLabelMap.CommonWeek}</option>
                    <option value="6" <#if (recurrenceRule.frequency)?default("") == "MONTHLY">selected</#if>>${uiLabelMap.CommonMonth}</option>
                    <option value="7" <#if (recurrenceRule.frequency)?default("") == "YEARLY">selected</#if>>${uiLabelMap.CommonYear}</option>
                  </select>
                </td>
                <td>&nbsp;</td>
                <td><div class="tableheadtext">${uiLabelMap.CommonStartDate}</div></td>
                <td>
                  <input type="text" class="textBox" name="startDateTime" size="22" value="${(recurrenceInfo.startDateTime)?if_exists}">
                  <a href="javascript:call_cal(document.reorderinfo.startDateTime, '${nowTimestamp.toString()}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
                </td>
                <td>&nbsp;</td>
                <td><div class="tableheadtext">${uiLabelMap.CommonEndDate}</div></td>
                <td>
                  <input type="text" class="textBox" name="endDateTime" size="22" value="${(recurrenceRule.untilDateTime)?if_exists}">
                  <a href="javascript:call_cal(document.reorderinfo.endDateTime, '${nowTimestamp.toString()}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
                </td>
                <td>&nbsp;</td>
              </tr>
              <tr><td colspan="9"><hr class="sepbar"/></td></tr>
              <tr>
                <td><div class="tableheadtext">${uiLabelMap.OrderShipTo}</div></td>
                <td>
                  <select name="contactMechId" class="selectBox" onchange="javascript:document.reorderinfo.submit()">
                    <option value="">${uiLabelMap.OrderSelectAShippingAddress}</option>
                    <#if shippingContactMechList?has_content>
                      <#list shippingContactMechList as shippingContactMech>
                        <#assign shippingAddress = shippingContactMech.getRelatedOne("PostalAddress")>
                        <option value="${shippingContactMech.contactMechId}"<#if (shoppingList.contactMechId)?default("") == shippingAddress.contactMechId> selected</#if>>${shippingAddress.address1}</option>
                      </#list>
                    <#else>
                      <option value="">${uiLabelMap.OrderNoAddressesAvailable}</option>
                    </#if>
                  </select>
                </td>
                <td>&nbsp;</td>
                <td><div class="tableheadtext">${uiLabelMap.OrderShipVia}</div></td>
                <td>
                  <select name="shippingMethodString" class="selectBox">
                    <option value="">${uiLabelMap.OrderSelectShippingMethod}</option>
                    <#if carrierShipMethods?has_content>
                      <#list carrierShipMethods as shipMeth>
                        <#assign shippingEst = shippingEstWpr.getShippingEstimate(shipMeth)?default(-1)>
                        <#assign shippingMethod = shipMeth.shipmentMethodTypeId + "@" + shipMeth.partyId>
                        <option value="${shippingMethod}"<#if shippingMethod == chosenShippingMethod> selected</#if>>
                          <#if shipMeth.partyId != "_NA_">
                            ${shipMeth.partyId?if_exists}&nbsp;
                          </#if>
                          ${shipMeth.description?if_exists}
                          <#if shippingEst?has_content>
                            &nbsp;-&nbsp;
                            <#if (shippingEst > -1)>
                              <@ofbizCurrency amount=shippingEst isoCode=listCart.getCurrency()/>
                            <#else>
                              ${uiLabelMap.OrderCalculatedOffline}
                            </#if>
                          </#if>
                        </option>
                      </#list>
                    <#else>
                      <option value="">${uiLabelMap.OrderSelectAddressFirst}</option>
                    </#if>
                  </select>
                </td>
                <td>&nbsp;</td>
                <td><div class="tableheadtext">${uiLabelMap.OrderPayBy}</div></td>
                <td>
                  <select name="paymentMethodId" class="selectBox">
                    <option value="">${uiLabelMap.OrderSelectPaymentMethod}</option>
                    <#list paymentMethodList as paymentMethod>
                      <#if paymentMethod.paymentMethodTypeId == "CREDIT_CARD">
                        <#assign creditCard = paymentMethod.getRelatedOne("CreditCard")>
                        <option value="${paymentMethod.paymentMethodId}" <#if (shoppingList.paymentMethodId)?default("") == paymentMethod.paymentMethodId>selected</#if>>CC:&nbsp;${Static["org.ofbiz.party.contact.ContactHelper"].formatCreditCard(creditCard)}</option>
                      <#elseif paymentMethod.paymentMethodTypeId == "EFT_ACCOUNT">
                        <#assign eftAccount = paymentMethod.getRelatedOne("EftAccount")>
                        <option value="${paymentMethod.paymentMethodId}">EFT:&nbsp;${eftAccount.bankName?if_exists}: ${eftAccount.accountNumber?if_exists}</option>
                      </#if>
                    </#list>
                  </select>
                </td>
                <td>&nbsp;</td>
              </tr>
              <tr><td colspan="9"><hr class="sepbar"/></td></tr>
              <tr>
                <td align="right" colspan="9">
                  <div class="tabletext">
                    <a href="javascript:document.reorderinfo.submit();" class="buttontext">[${uiLabelMap.CommonSave}]</a>
                    <a href="<@ofbizUrl>editcontactmech?preContactMechTypeId=POSTAL_ADDRESS&contactMechPurposeTypeId=SHIPPING_LOCATION&DONE_PAGE=editShoppingList</@ofbizUrl>" class="buttontext">[${uiLabelMap.PartyAddNewAddress}]</a>
                    <a href="<@ofbizUrl>editcreditcard?DONE_PAGE=editShoppingList</@ofbizUrl>" class="buttontext">[${uiLabelMap.EcommerceNewCreditCard}]</a>
                    <a href="<@ofbizUrl>editeftaccount?DONE_PAGE=editShoppingList</@ofbizUrl>" class="buttontext">[${uiLabelMap.EcommerceNewEFTAccount}]</a>
                  </div>
                </td>
              </tr>
              <#if shoppingList.isActive?default("N") == "Y">
                <tr><td colspan="9"><hr class="sepbar"/></td></tr>
                <tr>
                  <td align="left" colspan="9">
                    <#assign nextTime = recInfo.next(lastSlOrderTime)?if_exists>
                    <#if nextTime?has_content>
                      <#assign nextTimeStamp = Static["org.ofbiz.base.util.UtilDateTime"].getTimestamp(nextTime)?if_exists>
                      <#if nextTimeStamp?has_content>
                        <#assign nextTimeString = Static["org.ofbiz.base.util.UtilFormatOut"].formatDate(nextTimeStamp)?if_exists>
                      </#if>
                    </#if>
                    <#if lastSlOrderDate?has_content>
                      <#assign lastOrderedString = Static["org.ofbiz.base.util.UtilFormatOut"].formatDate(lastSlOrderDate)?if_exists>
                    </#if>
                    <div class="tabletext">
                      <table cellspacing="2" cellpadding="2" border="0">
                        <tr>
                          <td><div class="tableheadtext">${uiLabelMap.OrderLastOrderedDate}</div></td>
                          <td><div class="tableheadtext">:</div></td>
                          <td><div class="tabletext">${lastOrderedString?default("${uiLabelMap.OrderNotYetOrdered}")}</div></td>
                        </tr>
                        <tr>
                          <td><div class="tableheadtext">${uiLabelMap.OrderEstimateNextOrderDate}</div></td>
                          <td><div class="tableheadtext">:</div></td>
                          <td><div class="tabletext">${nextTimeString?default("${uiLabelMap.EcommerceNotYetKnown}")}</div></td>
                        </tr>
                      </table>
                    </div>
                  </tr>
                </tr>
              </#if>
            </table>
        </form>
    </div>
</div>
</#if>

<#if childShoppingListDatas?has_content>
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxlink">
            <a href="<@ofbizUrl>addListToCart?shoppingListId=${shoppingList.shoppingListId}&includeChild=yes</@ofbizUrl>" class="submenutextright">${uiLabelMap.EcommerceAddChildListsToCart}</a>
        </div>
        <div class="boxhead">&nbsp;${uiLabelMap.EcommerceChildShoppingList} - ${shoppingList.listName}</div>
    </div>
    <div class="screenlet-body">
        <table width="100%" cellspacing="0" cellpadding="1" border="0">
          <tr> 
            <td><div class="tabletext"><b>${uiLabelMap.EcommerceListName}</b></div></td>
            <td align="right"><div class="tabletext"><b>${uiLabelMap.EcommerceTotalPrice}</b></div></td>
            <td>&nbsp;</td>
            <td>&nbsp;</td>
          </tr>
          <#list childShoppingListDatas as childShoppingListData>
              <#assign childShoppingList = childShoppingListData.childShoppingList/>
              <#assign totalPrice = childShoppingListData.totalPrice/>
              <tr>
                <td nowrap align="left">
                  <a href="<@ofbizUrl>editShoppingList?shoppingListId=${childShoppingList.shoppingListId}</@ofbizUrl>" class="buttontext">${childShoppingList.listName?default(childShoppingList.shoppingListId)}</a>
                </td>                      
                <td nowrap align="right">
                  <div class="tabletext"><@ofbizCurrency amount=totalPrice isoCode=currencyUomId/></div>
                </td>                      
                <td align="right">
                  <a href="<@ofbizUrl>editShoppingList?shoppingListId=${childShoppingList.shoppingListId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.EcommerceGoToList}]</a>
                  <a href="<@ofbizUrl>addListToCart?shoppingListId=${childShoppingList.shoppingListId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.EcommerceAddListToCart}]</a>
                </td>                      
              </tr>
            </form>
          </#list>
          <tr><td colspan="6"><hr class="sepbar"/></td></tr>
          <tr>
            <td><div class="tabletext">&nbsp;</div></td>
            <td nowrap align="right">
              <div class="tableheadtext"><@ofbizCurrency amount=shoppingListChildTotal isoCode=currencyUomId/></div>
            </td>                      
            <td><div class="tabletext">&nbsp;</div></td>
          </tr>
        </table>
    </div>
</div>
</#if>

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxlink">
            <a href="<@ofbizUrl>addListToCart?shoppingListId=${shoppingList.shoppingListId}</@ofbizUrl>" class="submenutextright">${uiLabelMap.EcommerceAddListToCart}</a>
        </div>
        <div class="boxhead">&nbsp;${uiLabelMap.EcommerceListItems} - ${shoppingList.listName}</div>
    </div>
    <div class="screenlet-body">
        <#if shoppingListItemDatas?has_content>
            <table width="100%" cellspacing="0" cellpadding="1" border="0">
              <tr>
                <td><div class="tabletext"><b>${uiLabelMap.EcommerceProduct}</b></div></td>
                <td><table><tr><td class="tabletext" nowrap align="center"><b>- ${uiLabelMap.EcommerceStartdate} -</b></td><td class="tabletext" nowrap><b>- ${uiLabelMap.EcommerceNbrOfDays} -</b></td></tr><tr><td class="tabletext" nowrap><b>- ${uiLabelMap.EcommerceNbrOfPersons} -</b></td><td class="tabletext" nowrap align="center"><b>- ${uiLabelMap.CommonQuantity} -</b></td></tr></table></td>
                <#-- <TD NOWRAP align="center"><div class="tabletext"><b>Purchased</b></div></TD> -->
                <td align="right"><div class="tabletext"><b>${uiLabelMap.EcommercePrice}</b></div></td>
                <td align="right"><div class="tabletext"><b>${uiLabelMap.EcommerceTotal}</b></div></td>
                <td>&nbsp;</td>
              </tr>

              <#list shoppingListItemDatas as shoppingListItemData>
                <#assign shoppingListItem = shoppingListItemData.shoppingListItem/>
                <#assign product = shoppingListItemData.product/>
                <#assign productContentWrapper = Static["org.ofbiz.product.product.ProductContentWrapper"].makeProductContentWrapper(product, request)/>
                <#assign unitPrice = shoppingListItemData.unitPrice/>
                <#assign totalPrice = shoppingListItemData.totalPrice/>
                <#assign productVariantAssocs = shoppingListItemData.productVariantAssocs?if_exists/>
                <#assign isVirtual = product.isVirtual?exists && product.isVirtual.equals("Y")/>
                  <tr>
                    <td>
                      <div class="tabletext">
                         <a href="<@ofbizUrl>product?product_id=${shoppingListItem.productId}</@ofbizUrl>" class="buttontext">${shoppingListItem.productId} - 
                         ${productContentWrapper.get("PRODUCT_NAME")?default("No Name")}</a> : ${productContentWrapper.get("DESCRIPTION")?if_exists}
                      </div>
                    </td>
                    <td nowrap align="center">
                      <form method="post" action="<@ofbizUrl>updateShoppingListItem</@ofbizUrl>" name="listform_${shoppingListItem.shoppingListItemSeqId}" style="margin: 0;">
                        <input type="hidden" name="shoppingListId" value="${shoppingListItem.shoppingListId}"/>
                        <input type="hidden" name="shoppingListItemSeqId" value="${shoppingListItem.shoppingListItemSeqId}"/>
                        <input type="hidden" name="reservStart"/>
                        <div class="tabletext">
                           <#if product.productTypeId == "ASSET_USAGE"><table border="0" width="100%"><tr><td width="1%">&nbsp;</td><td><input type="text" class="inputBox" size="10" name="reservStartStr" value=${shoppingListItem.reservStart?if_exists}/></td><td><input type="text" class="inputBox" size="2" name="reservLength" value=${shoppingListItem.reservLength?if_exists}/></td></tr><tr><td>&nbsp;</td><td><input type="text" class="inputBox" size="3" name="reservPersons" value=${shoppingListItem.reservPersons?if_exists}/></td><td class="tabletext"><#else>
                           <table width="100%"><tr><td width="62%" align="center">--</td><td align="center">--</td></tr><tr><td align="center">--</td><td class="tabletext"><input type="hidden" name="reservStartStr" value=""/>
                           </#if>
                        <input size="6" class="inputBox" type="text" name="quantity" value="${shoppingListItem.quantity?string.number}"/>
                        </td></tr></table>
                        </div>
                      </form>
                    </td>
                    <#--
                    <td nowrap align="center">
                      <div class="tabletext">${shoppingListItem.quantityPurchased?default(0)?string.number}</div>
                    </td>
                    -->
                    <td nowrap align="right">
                      <div class="tabletext"><@ofbizCurrency amount=unitPrice isoCode=currencyUomId/></div>
                    </td>
                    <td nowrap align="right">
                      <div class="tabletext"><@ofbizCurrency amount=totalPrice isoCode=currencyUomId/></div>
                    </td>
                    <td align="right">
                        <a href="javascript:TimestampSubmit(listform_${shoppingListItem.shoppingListItemSeqId});" class="buttontext">[${uiLabelMap.CommonUpdate}]</a>
                        <a href="<@ofbizUrl>removeFromShoppingList?shoppingListId=${shoppingListItem.shoppingListId}&shoppingListItemSeqId=${shoppingListItem.shoppingListItemSeqId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonRemove}]</a>
                      <#if isVirtual && productVariantAssocs?has_content>
                        <#assign replaceItemAction = "/replaceShoppingListItem/" + requestAttributes._CURRENT_VIEW_?if_exists>
                        <#assign addToCartAction = "/additem/" + requestAttributes._CURRENT_VIEW_?if_exists>
                        <br/>
                        <form method="post" action="<@ofbizUrl>${addToCartAction}</@ofbizUrl>" name="listreplform_${shoppingListItem.shoppingListItemSeqId}" style="margin: 0;">
                          <input type="hidden" name="shoppingListId" value="${shoppingListItem.shoppingListId}"/>
                          <input type="hidden" name="shoppingListItemSeqId" value="${shoppingListItem.shoppingListItemSeqId}"/>
                          <input type="hidden" name="quantity" value="${shoppingListItem.quantity}"/>
                          <select name="add_product_id" class="selectBox">
                              <#list productVariantAssocs as productVariantAssoc>
                                <#assign variantProduct = productVariantAssoc.getRelatedOneCache("AssocProduct")>
                                <#if variantProduct?exists>
                                <#assign variantProductContentWrapper = Static["org.ofbiz.product.product.ProductContentWrapper"].makeProductContentWrapper(variantProduct, request)>
                                  <option value="${variantProduct.productId}">${variantProductContentWrapper.get("PRODUCT_NAME")?default("No Name")} [${variantProduct.productId}]</option>
                                </#if>
                              </#list>
                          </select>
                          <br/>
                          <a href="javascript:document.listreplform_${shoppingListItem.shoppingListItemSeqId}.action='<@ofbizUrl>${replaceItemAction}</@ofbizUrl>';document.listreplform_${shoppingListItem.shoppingListItemSeqId}.submit();" class="buttontext">[${uiLabelMap.EcommerceReplaceWithVariation}]</a>
                          <br/>
                          <a href="javascript:document.listreplform_${shoppingListItem.shoppingListItemSeqId}.action='<@ofbizUrl>${addToCartAction}</@ofbizUrl>';document.listreplform_${shoppingListItem.shoppingListItemSeqId}.submit();" class="buttontext">[${uiLabelMap.CommonAdd}&nbsp;${shoppingListItem.quantity?string}&nbsp;${uiLabelMap.EcommerceVariationToCart}]</a>
                        </form>
                      <#else>
                        <a href="<@ofbizUrl>additem<#if requestAttributes._CURRENT_VIEW_?exists>/${requestAttributes._CURRENT_VIEW_}</#if>?shoppingListId=${shoppingListItem.shoppingListId}&shoppingListItemSeqId=${shoppingListItem.shoppingListItemSeqId}&quantity=${shoppingListItem.quantity}&reservStart=${shoppingListItem.reservStart?if_exists}&reservPersons=${shoppingListItem.reservPersons?if_exists}&reservLength=${shoppingListItem.reservLength?if_exists}&add_product_id=${shoppingListItem.productId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonAdd}&nbsp;${shoppingListItem.quantity?string}&nbsp;${uiLabelMap.EcommerceToCart}]</a>
                      </#if>
                    </td>
                  </tr>
              </#list>
              <tr><td colspan="6"><hr class="sepbar"/></td></tr>
              <tr>
                <td><div class="tabletext">&nbsp;</div></td>
                <td><div class="tabletext">&nbsp;</div></td>
                <#--<td><div class="tabletext">&nbsp;</div></td>-->
                <td><div class="tabletext">&nbsp;</div></td>
                <td nowrap align="right">
                  <div class="tableheadtext"><@ofbizCurrency amount=shoppingListItemTotal isoCode=currencyUomId/></div>
                </td>                      
                <td><div class="tabletext">&nbsp;</div></td>
              </tr>
            </table>
        <#else>
            <div class="head2">${uiLabelMap.EcommerceShoppingListEmpty}.</div>
        </#if>
    </div>
</div>

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.EcommerceShoppingListPriceTotals} - ${shoppingList.listName}</div>
    </div>
    <div class="screenlet-body">
      <table width="100%" border="0" cellspacing="1" cellpadding="1">
        <tr>
          <td align="left" width="5%" NOWRAP>
              <div class="tabletext">${uiLabelMap.EcommerceChildListTotalPrice}</div>
          </td>
          <td align="right" width="5%" NOWRAP>
              <div class="tabletext"><@ofbizCurrency amount=shoppingListChildTotal isoCode=currencyUomId/></div>
          </td>
          <td width="90%"><div class="tabletext">&nbsp;</div></td>
        </tr>
        <tr>
          <td align="left" NOWRAP>
              <div class="tabletext">${uiLabelMap.EcommerceListItemsTotalPrice}&nbsp;</div>
          </td>
          <td align="right" NOWRAP>
              <div class="tabletext"><@ofbizCurrency amount=shoppingListItemTotal isoCode=currencyUomId/></div>
          </td>
          <td><div class="tabletext">&nbsp;</div></td>
        </tr>
        <tr>
          <td align="left" NOWRAP>
              <div class="tableheadtext">${uiLabelMap.OrderGrandTotal}</div>
          </td>
          <td align="right" NOWRAP>
              <div class="tableheadtext"><@ofbizCurrency amount=shoppingListTotalPrice isoCode=currencyUomId/></div>
          </td>
          <td><div class="tabletext">&nbsp;</div></td>
        </tr>
      </table>
    </div>
</div>

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.CommonQuickAddList}</div>
    </div>
    <div class="screenlet-body">
        <form name="addToShoppingList" method="post" action="<@ofbizUrl>addItemToShoppingList<#if requestAttributes._CURRENT_VIEW_?exists>/${requestAttributes._CURRENT_VIEW_}</#if></@ofbizUrl>">
          <input type="hidden" name="shoppingListId" value="${shoppingList.shoppingListId}"/>
          <input type="text" class="inputBox" name="productId" value="${requestParameters.add_product_id?if_exists}"/>
          <#if reservStart?exists></td><td class="tabletext">${uiLabelMap.EcommerceStartDate}</td><td><input type="text" class="inputBox" size="10" name="reservStart" value=${requestParameters.reservStart?default("")}></td><td class="tabletext"> ${uiLabelMap.EcommerceLength}:</td><td><input type="text" class="inputBox" size="2" name="reservLength" value=${requestParameters.reservLength?default("")}></td></tr><tr><td>&nbsp;</td><td>&nbsp;</td><td class="tabletext">${uiLabelMap.EcommerceNbrPersons}:</td><td><input type="text" class="inputBox" size="3" name="reservPersons" value=${requestParameters.reservPersons?default("1")}></td><td class="tabletext" nowrap></#if> ${uiLabelMap.CommonQuantity} :</td><td><input type="text" class="inputBox" size="5" name="quantity" value="${requestParameters.quantity?default("1")}"></td><td>
          <!-- <input type="text" class="inputBox" size="5" name="quantity" value="${requestParameters.quantity?default("1")}">-->
          <input type="submit" class="smallSubmit" value="${uiLabelMap.EcommerceAddtoShoppingList}"/>
        </form>
    </div>
</div>

    <#else>
        <#-- shoppingList was found, but belongs to a different party -->
        <div class="head2">${uiLabelMap.EcommerceShoppingListError} ${uiLabelMap.CommonId} ${shoppingList.shoppingListId}) ${uiLabelMap.EcommerceListDoesNotBelong}.</div>
    </#if>
</#if>
