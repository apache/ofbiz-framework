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

<TABLE border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
  <TR>
    <TD width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxtop'>
        <tr>
          <td valign="middle" align="left">
            <div class="boxhead">&nbsp;${uiLabelMap.PartyShoppingLists}</div>
          </td>
          <td valign="middle" align="right">
            <a href="<@ofbizUrl>createEmptyShoppingList</@ofbizUrl>?partyId=${partyId?if_exists}" class="submenutextright">${uiLabelMap.CommonCreateNew}</a>
            <#-- <a href="<@ofbizUrl>viewprofile?partyId=${partyId?if_exists}</@ofbizUrl>" class="submenutextright">${uiLabelMap.PartyProfile}</a> -->
          </td>
        </tr>
      </table>
    </TD>
  </TR>
  <TR>
    <TD width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
        <tr>
          <td>
            <#if shoppingLists?has_content>
              <form name="selectShoppingList" method="post" action="<@ofbizUrl>editShoppingList</@ofbizUrl>">
                <select name="shoppingListId" class="selectBox">
                    <#if shoppingList?has_content>
                      <option value="${shoppingList.shoppingListId}">${shoppingList.listName}</option>
                      <option value="${shoppingList.shoppingListId}">--</option>
                    </#if>
                  <#list allShoppingLists as list>
                    <option value="${list.shoppingListId}">${list.listName}</option>
                  </#list>
                </select>
                &nbsp;&nbsp;
                <a href="javascript:document.selectShoppingList.submit();" class="buttontext">${uiLabelMap.CommonEdit}</a>
              </form>
            <#else>
              <div class="tabletext">${uiLabelMap.PartyNoShoppingListsParty}.</div>
            </#if>
          </td>
        </tr>
      </table>
    </TD>
  </TR>
</TABLE>
<br/>

<#if shoppingList?has_content>
<TABLE border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
  <TR>
    <TD width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxtop'>
        <tr>
          <td valign="middle" align="left">
            <div class="boxhead">&nbsp;${uiLabelMap.PartyShoppingListDetail} - ${shoppingList.listName}</div>
          </td>  
          <td valign="middle" align="right">
            <a href="/ordermgr/control/loadCartFromShoppingList?shoppingListId=${shoppingList.shoppingListId?if_exists}" class="submenutext">${uiLabelMap.OrderNewOrder}</a>
            <a href="/ordermgr/control/createCustRequestFromShoppingList?shoppingListId=${shoppingList.shoppingListId?if_exists}" class="submenutext">${uiLabelMap.PartyCreateNewCustRequest}</a>
            <a href="/ordermgr/control/createQuoteFromShoppingList?shoppingListId=${shoppingList.shoppingListId?if_exists}&applyStorePromotions=N" class="submenutext">${uiLabelMap.PartyCreateNewQuote}</a>
            <a href="javascript:document.updateList.submit();" class="submenutextright">${uiLabelMap.CommonSave}</a>
          </td>
        </tr>
      </table>
    </TD>
  </TR>
  <TR>
    <TD width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
        <tr>
          <td>
              <form name="updateList" method="post" action="<@ofbizUrl>updateShoppingList</@ofbizUrl>">
                <input type="hidden" class="inputBox" name="shoppingListId" value="${shoppingList.shoppingListId}">
                <input type="hidden" class="inputBox" name="partyId" value="${shoppingList.partyId?if_exists}">
                <table border='0' width='100%' cellspacing='0' cellpadding='0'>
                  <tr>
                    <td><div class="tableheadtext">${uiLabelMap.PartyListName}</div></td>
                    <td><input type="text" class="inputBox" size="25" name="listName" value="${shoppingList.listName}" <#if shoppingList.listName?default("") == "auto-save">disabled</#if>>
                  </tr>
                  <tr>
                    <td><div class="tableheadtext">${uiLabelMap.CommonDescription}</div></td>
                    <td><input type="text" class="inputBox" size="70" name="description" value="${shoppingList.description?if_exists}" <#if shoppingList.listName?default("") == "auto-save">disabled</#if>>
                  </tr>
                  <tr>
                    <td><div class="tableheadtext">${uiLabelMap.PartyListType}</div></td>
                    <td>
                      <select name="shoppingListTypeId" class="selectBox" <#if shoppingList.listName?default("") == "auto-save">disabled</#if>>
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
                    <td><div class="tableheadtext">${uiLabelMap.PartyPublic}?</div></td>
                    <td>
                      <select name="isPublic" class="selectBox" <#if shoppingList.listName?default("") == "auto-save">disabled</#if>>
                        <option>${shoppingList.isPublic}</option>
                        <option value="${shoppingList.isPublic}">--</option>
                        <option>Y</option>
                        <option>N</option>
                      </select>
                    </td>
                  </tr>                           
                  <tr>
                    <td><div class="tableheadtext">${uiLabelMap.PartyParentList}</div></td>
                    <td>
                      <select name="parentShoppingListId" class="selectBox" <#if shoppingList.listName?default("") == "auto-save">disabled</#if>>
                      	<#if parentShoppingList?exists>
                          <option value="${parentShoppingList.shoppingListId}">${parentShoppingList.listName?default(parentShoppingList.shoppingListId)}</option>
                        </#if>
                        <option value="">${uiLabelMap.PartyNoParent}</option>
                        <#list allShoppingLists as newParShoppingList>
                          <option value="${newParShoppingList.shoppingListId}">${newParShoppingList.listName?default(newParShoppingList.shoppingListId)}</option>
                        </#list>
                      </select>
                      <#if parentShoppingList?exists>
                        <a href="<@ofbizUrl>editShoppingList?shoppingListId=${parentShoppingList.shoppingListId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonGotoParent} (${parentShoppingList.listName?default(parentShoppingList.shoppingListId)})</a>
                      </#if>
                    </td>
                  </tr>
                  <#if shoppingList.listName?default("") != "auto-save">
                  <tr>
                    <td><div class="tableheadtext">&nbsp;</div></td>
                    <td align="left">
                      <a href="javascript:document.updateList.submit();" class="buttontext">${uiLabelMap.CommonSave}</a>         
                    </td>
                  </tr>
                  </#if>
                </table>
              </form>           
          </td>
        </tr>
      </table>
    </TD>
  </TR>
</TABLE>

<#if childShoppingListDatas?has_content>
<br/>
<TABLE border="0" width='100%' cellspacing='0' cellpadding='0' <#--class='boxoutside'-->>
  <TR>
    <TD width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxtop'>
        <tr>
          <td valign="middle" align="left">
            <div class="boxhead">&nbsp;${uiLabelMap.PartyChildShoppingList} - ${shoppingList.listName}</div>
          </td>  
          <td valign="middle" align="right">
              <a href="<@ofbizUrl>addListToCart?shoppingListId=${shoppingList.shoppingListId}&includeChild=yes</@ofbizUrl>" class="submenutextright">${uiLabelMap.PartyAddChildListsToCart}</a>
          </td>
        </tr>
      </table>
    </TD>
  </TR>
  <TR>
    <TD width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
        <tr>
          <td>
            <table width='100%' cellspacing="0" cellpadding="1" border="0">
	      <TR> 
	        <TD NOWRAP><div class='tabletext'><b>${uiLabelMap.PartyListName}</b></div></TD>
		<td>&nbsp;</td>
		<td>&nbsp;</td>
	      </TR>
	      <#list childShoppingListDatas as childShoppingListData>
                <#assign childShoppingList = childShoppingListData.childShoppingList>
		<tr>
		  <td nowrap align="left">
                  <a href="<@ofbizUrl>editShoppingList?shoppingListId=${childShoppingList.shoppingListId}</@ofbizUrl>" class="buttontext">${childShoppingList.listName?default(childShoppingList.shoppingListId)}</a>
		  </td>                                        
		  <td align="right">
                    <a href="<@ofbizUrl>editShoppingList?shoppingListId=${childShoppingList.shoppingListId}</@ofbizUrl>" class="buttontext">${uiLabelMap.PartyGotoList}</a>
                    <a href="<@ofbizUrl>addListToCart?shoppingListId=${childShoppingList.shoppingListId}</@ofbizUrl>" class="buttontext">${uiLabelMap.PartyAddListToCart}</a>
	          </td>                      
		</tr>
	      </#list>
	    </table>
          </td>
        </tr>
      </table>
    </TD>
  </TR>
</TABLE>
</#if>

<br/>
<TABLE border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
  <TR>
    <TD width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxtop'>
        <tr>
          <td valign="middle" align="left">
            <div class="boxhead">&nbsp;${uiLabelMap.PartyListItems} - ${shoppingList.listName}</div>
          </td>
          <#--
          <td valign="middle" align="right">
            <a href="<@ofbizUrl>addListToCart?shoppingListId=${shoppingList.shoppingListId}</@ofbizUrl>" class="submenutextright">${uiLabelMap.PartyAddListToCart}</a>
          </td>
          -->
        </tr>
      </table>
    </TD>
  </TR>
  <TR>
    <TD width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
        <tr>
          <td>
            <#if shoppingListItemDatas?has_content>
                <table width='100%' cellspacing="0" cellpadding="1" border="0">
                  <TR>
                    <TD NOWRAP><div class='tabletext'><b>${uiLabelMap.PartyProduct}</b></div></TD>
                    <TD NOWRAP align="center"><div class='tabletext'><b>${uiLabelMap.PartyQuantity}</b></div></TD>
                    <TD NOWRAP align="center"><div class='tabletext'><b>${uiLabelMap.PartyQuantityPurchased}</b></div></TD>
                    <TD NOWRAP align="right"><div class='tabletext'><b>${uiLabelMap.PartyPrice}</b></div></TD>
                    <TD NOWRAP align="right"><div class='tabletext'><b>${uiLabelMap.PartyTotal}</b></div></TD>
                    <td>&nbsp;</td>
                  </TR>
                  <TR>
                    <td colspan="7"><hr class="sepbar"></td>
                  </TR>
                  <#list shoppingListItemDatas as shoppingListItemData>
                    <#assign shoppingListItem = shoppingListItemData.shoppingListItem>
                    <#assign product = shoppingListItemData.product>
                    <#assign productContentWrapper = Static["org.ofbiz.product.product.ProductContentWrapper"].makeProductContentWrapper(product, request)>
                    <#assign unitPrice = shoppingListItemData.unitPrice>
                    <#assign totalPrice = shoppingListItemData.totalPrice>
                    <#assign productVariantAssocs = shoppingListItemData.productVariantAssocs?if_exists>
                    <#assign isVirtual = product.isVirtual?exists && product.isVirtual.equals("Y")>

                      <tr>
                        <td>
                          <div class='tabletext'>
                             <a href="/catalog/control/EditProduct?productId=${shoppingListItem.productId}&externalLoginKey=${requestAttributes.externalLoginKey}" class='buttontext'>${shoppingListItem.productId} -
                             ${productContentWrapper.get("PRODUCT_NAME")?default("No Name")}</a> : ${productContentWrapper.get("DESCRIPTION")?if_exists}
                          </div>
                        </td>
						  <form method="post" action="<@ofbizUrl>updateShoppingListItem</@ofbizUrl>" name='listform_${shoppingListItem.shoppingListItemSeqId}' style='margin: 0;'>
						    <input type="hidden" name="shoppingListId" value="${shoppingListItem.shoppingListId}">
						    <input type="hidden" name="shoppingListItemSeqId" value="${shoppingListItem.shoppingListItemSeqId}">
                         <td nowrap align="center">
                              <div class='tabletext'>
                                <input size="6" class='inputBox' type="text" name="quantity" value="${shoppingListItem.quantity?string.number}">
                              </div>
                         </td>
                         <td nowrap align="center">
                           <div class='tabletext'>
                             <input size="6" class='inputBox' type="text" name="quantityPurchased" 
                             <#if shoppingListItem.quantityPurchased?has_content>
                               value="${shoppingListItem.quantityPurchased?if_exists?string.number}"
                             </#if>
                             >
                            </div>
                         </td>
		                  </form>
                        <td nowrap align="right">
                          <div class="tabletext"><@ofbizCurrency amount=unitPrice isoCode=currencyUomId/></div>
                        </td>
                        <td nowrap align="right">
                          <div class="tabletext"><@ofbizCurrency amount=totalPrice isoCode=currencyUomId/></div>
                        </td>
                        <td align="right" nowrap>
                        	<a href="javascript:document.listform_${shoppingListItem.shoppingListItemSeqId}.submit();" class="buttontext">${uiLabelMap.CommonUpdate}</a>
                        	<a href="<@ofbizUrl>removeFromShoppingList?shoppingListId=${shoppingListItem.shoppingListId}&shoppingListItemSeqId=${shoppingListItem.shoppingListItemSeqId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonRemove}</a>
                        </td>
                      </tr>
                  </#list>
                  <tr><td colspan="7"><hr class='sepbar'></td></tr>
                  <tr>
                    <td><div class="tabletext">&nbsp;</div></td>
                    <td><div class="tabletext">&nbsp;</div></td>
                    <td><div class="tabletext">&nbsp;</div></td>
                    <td><div class="tabletext">&nbsp;</div></td>
                    <td><div class="tabletext">&nbsp;</div></td>
                    <td><div class="tabletext">&nbsp;</div></td>
                  </tr>
                </table>
            <#else>
                <div class='head2'>${uiLabelMap.PartyShoppingListEmpty}.</div>
            </#if>
          </td>
        </tr>
      </table>
    </TD>
  </TR>
</TABLE>

<br/>
<table border="0" width="100%" cellspacing="0" cellpadding="0" class="boxoutside">
  <tr>
    <td width="100%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxtop">
        <tr>
          <td valign="middle" align="left">
            <div class="boxhead">&nbsp;${uiLabelMap.PartyQuickAddList}</div>
          </td>
        </tr>
      </table>
    </td>
  </tr>
  <tr>
    <td width="100%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxbottom">
        <tr>
          <td>
            <form name="addToShoppingList" method="post" action="<@ofbizUrl>addItemToShoppingList<#if requestAttributes._CURRENT_VIEW_?exists>/${requestAttributes._CURRENT_VIEW_}</#if></@ofbizUrl>">
              <input type="hidden" name="shoppingListId" value="${shoppingList.shoppingListId}">
              <input type="hidden" name="partyId" value="${shoppingList.partyId?if_exists}">
              <input type="text" class="inputBox" name="productId" value="">
              <input type="text" class="inputBox" size="5" name="quantity" value="${requestParameters.quantity?default("1")}">
              <input type="submit" class="smallSubmit" value="${uiLabelMap.PartyAddToShoppingList}">
            </form>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>

</#if>
