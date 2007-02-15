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
function toggleAll() {
    var cform = document.cartform;
    var len = cform.elements.length;
    for (var i = 0; i < len; i++) {
        var e = cform.elements[i];   
        if (e.name == "selectedItem") {
            toggle(e);
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
        alert("${uiLabelMap.OrderSelectedGiftNotAvailableForAll}");
    }
    cform.submit();
}
function quicklookup_popup(element) {
    target = element;  // note: global var target comes from fieldlookup.js
    var searchTerm = element.value;
    var obj_lookupwindow = window.open('LookupProduct?productId_op=like&productId_ic=Y&productId=' + searchTerm,'FieldLookup', 'width=700,height=550,scrollbars=yes,status=no,resizable=yes,top='+my+',left='+mx+',dependent=yes,alwaysRaised=yes');
    obj_lookupwindow.opener = window;
    obj_lookupwindow.focus();
}
function quicklookup(element) {
    <#if shoppingCart.getOrderType() == "PURCHASE_ORDER">
    window.location='LookupBulkAddSupplierProducts?productId='+element.value;
    <#else>
    window.location='LookupBulkAddProducts?productId='+element.value;
    </#if>
}
</script>

<div class="screenlet">
    <div class="screenlet-body">
      <table border="0" cellspacing="0" cellpadding="0">
        <tr>
          <td>           
            <form method="post" action="<@ofbizUrl>additem</@ofbizUrl>" name="quickaddform" style="margin: 0;">
              <table border="0">
                <tr>
                  <td align="right"><div class="tableheadtext">${uiLabelMap.ProductProductId} :</div></td>
                  <td><input type="text" class="inputBox" size="25" name="add_product_id" value=""/>
                    <span class='tabletext'>
                      <a href="javascript:quicklookup(document.quickaddform.add_product_id)" class="buttontext">${uiLabelMap.OrderQuickLookup}</a>
                      <a href="javascript:call_fieldlookup2(document.quickaddform.add_product_id,'LookupProduct');">
                        <img src="<@ofbizContentUrl>/images/fieldlookup.gif"</@ofbizContentUrl>" width="15" height="14" border="0" alt="Click here For Field Lookup"/>
                      </a>
                    </span>
                  </td>
                </tr>
                <tr>
                  <td align="right"><div class="tableheadtext">${uiLabelMap.OrderQuantity} :</div></td>
                  <td><input type="text" class="inputBox" size="6" name="quantity" value=""/></td>
                </tr>
                <tr>
                  <td align="right"><div class="tableheadtext">${uiLabelMap.OrderDesiredDeliveryDate} :</div></td>
                  <td>
                    <div class="tabletext">
                      <input type="text" class="inputBox" size="25" maxlength="30" name="itemDesiredDeliveryDate"<#if useAsDefaultDesiredDeliveryDate?exists> value="${defaultDesiredDeliveryDate}"</#if>/>
                      <a href="javascript:call_cal(document.quickaddform.itemDesiredDeliveryDate,'${defaultDesiredDeliveryDate} 00:00:00.0');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="${uiLabelMap.calendar_click_here_for_calendar}"/></a>
                      <input type="checkbox" class="inputBox" name="useAsDefaultDesiredDeliveryDate" value="true"<#if useAsDefaultDesiredDeliveryDate?exists> checked="checked"</#if>/>
                      ${uiLabelMap.OrderUseDefaultDesiredDeliveryDate}
                    </div>
                  </td>
                </tr>
                <tr>
                  <td align="right"><div class="tableheadtext">${uiLabelMap.OrderShipAfterDate} :</div></td>
                  <td>
                    <div class="tabletext">
                      <input type="text" class="inputBox" size="20" maxlength="30" name="shipAfterDate" value="${shoppingCart.getDefaultShipAfterDate()?default("")}"/>
                      <a href="javascript:call_cal(document.quickaddform.shipAfterDate,'${shoppingCart.getDefaultShipAfterDate()?default("")}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="${uiLabelMap.calendar_click_here_for_calendar}"/></a>
                    </div>
                  </td>
                </tr>
                <tr>
                  <td align="right"><div class="tableheadtext">${uiLabelMap.OrderShipBeforeDate} :</div></td>
                  <td>
                    <div class="tabletext">
                      <input type="text" class="inputBox" size="20" maxlength="30" name="shipBeforeDate" value="${shoppingCart.getDefaultShipBeforeDate()?default("")}"/>
                      <a href="javascript:call_cal(document.quickaddform.shipBeforeDate,'${shoppingCart.getDefaultShipBeforeDate()?default("")}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="${uiLabelMap.calendar_click_here_for_calendar}"/></a>
                    </div>
                  </td>
                </tr>
                <#if shoppingCart.getOrderType() == "PURCHASE_ORDER">
                <tr>
                  <td align="right"><div class="tableheadtext">${uiLabelMap.OrderOrderItemType} :</div></td>
                  <td>
                    <div class="tabletext">
                      <select name="add_item_type" class="selectBox">
                        <option value="">&nbsp;</option>
                        <#list purchaseOrderItemTypeList as orderItemType>
                        <option value="${orderItemType.orderItemTypeId}">${orderItemType.description}</option>
                        </#list>
                      </select>
                    </div>
                  </td>
                </tr>
                </#if>
                <tr>
                  <td align="right"><div class="tableheadtext">${uiLabelMap.CommonComment} :</div></td>
                  <td>
                    <div class="tabletext">
                      <input type="text" class="inputBox" size="25" name="itemComment" value="${defaultComment?if_exists}">
                      <input type="checkbox" class="inputBox" name="useAsDefaultComment" value="true" <#if useAsDefaultComment?exists>checked</#if>>
                      ${uiLabelMap.OrderUseDefaultComment}
                    </div>
                  </td>
                </tr>
                <tr>
                  <td></td>
                  <td><input type="submit" class="smallSubmit" value="${uiLabelMap.OrderAddToOrder}"/></td>
                </tr>
              </table>
            </form>
          </td>
        </tr>
        <#if shoppingCart.getOrderType() == "PURCHASE_ORDER">
        <tr><td><hr class="sepbar"/></td></tr>
        <tr>
          <td>
            <form method="post" action="<@ofbizUrl>additem</@ofbizUrl>" name="bulkworkaddform" style="margin: 0;">
                <div class="tableheadtext">
                    ${uiLabelMap.CommonOrderItemType}:&nbsp;<select name="add_item_type" class="selectBox"><option value="BULK_ORDER_ITEM">${uiLabelMap.ProductBulkItem}</option><option value="WORK_ORDER_ITEM">${uiLabelMap.ProductWorkItem}</option></select>
                    <br>${uiLabelMap.ProductProductCategory}:&nbsp;<input type="text" class="inputBox" name="add_category_id" size="20" maxlength="20" value="${requestParameters.add_category_id?if_exists}"/>
                    <a href="javascript:call_fieldlookup2(document.bulkworkaddform.add_category_id,'LookupProductCategory');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'/></a>
                </div>
                <div class="tableheadtext">
                    ${uiLabelMap.CommonDescription}:&nbsp;<input type="text" class="inputBox" size="25" name="add_item_description" value=""/>
                    ${uiLabelMap.OrderQuantity}:&nbsp;<input type="text" class="inputBox" size="3" name="quantity" value="${requestParameters.quantity?default("1")}"/>
                    ${uiLabelMap.OrderPrice}:&nbsp;<input type="text" class="inputBox" size="6" name="price" value="${requestParameters.price?if_exists}"/>
                    <input type="submit" class="smallSubmit" value="${uiLabelMap.OrderAddToOrder}"/>
                </div>
            </form>
          </td>
        </tr>  
        </#if>      
      </table>
    </div>
</div>

<!-- Screenlet to add cart to shopping list. The shopping lists are presented in a dropdown box. -->

<#if (shoppingLists?exists) && (shoppingCartSize > 0)>
  <div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.OrderAddOrderToShoppingList}</div>
    </div>
    <div class="screenlet-body">
      <table border="0" cellspacing="0" cellpadding="0">
        <tr>
          <td>           
            <form method="post" name="addBulkToShoppingList" action="<@ofbizUrl>addBulkToShoppingList</@ofbizUrl>" style='margin: 0;'>
              <#assign index = 0/>
              <#list shoppingCart.items() as cartLine>
                <#if (cartLine.getProductId()?exists) && !cartLine.getIsPromo()>
                  <input type="hidden" name="selectedItem" value="${index}"/>
                </#if>
                <#assign index = index + 1/>
              </#list>
              <table border="0">
                <tr>
                  <td>
                    <div class="tabletext">
                    <select name='shoppingListId' class='selectBox'>
                      <#list shoppingLists as shoppingList>
                        <option value='${shoppingList.shoppingListId}'>${shoppingList.getString("listName")}</option>
                      </#list>
                    </select>
                    <input type="submit" class="smallSubmit" value="${uiLabelMap.EcommerceAddtoShoppingList}"/>
                    </div>
                  </td>
                </tr>
              </table>
            </form>
          </td>
        </tr>
      </table>
    </div>
  </div>
</#if>

<script language="JavaScript" type="text/javascript">
  document.quickaddform.add_product_id.focus();
</script>

<!-- Internal cart info: productStoreId=${shoppingCart.getProductStoreId()?if_exists} locale=${shoppingCart.getLocale()?if_exists} currencyUom=${shoppingCart.getCurrency()?if_exists} userLoginId=${(shoppingCart.getUserLogin().getString("userLoginId"))?if_exists} autoUserLogin=${(shoppingCart.getAutoUserLogin().getString("userLoginId"))?if_exists} -->
