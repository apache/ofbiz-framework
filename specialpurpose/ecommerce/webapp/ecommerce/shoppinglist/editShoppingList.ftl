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

<script type="text/javascript">
//<![CDATA[
    <!-- function to add extra info for Timestamp format -->
    function TimestampSubmit(obj) {
       reservStartStr = jQuery(obj).find("input[name='reservStartStr']");
       val1 = reservStartStr.val();
       reservStart = jQuery(obj).find("input[name='reservStart']");
       if (reservStartStr.val().length == 10) {
           reservStart.val(reservStartStr.val() + " 00:00:00.000000000");
       } else {
           reservStart.val(reservStartStr.val());
       }
       jQuery(obj).submit();
    }
    
    function callDocumentByPaginate(info) {
        var str = info.split('~');
        var checkUrl = '<@ofbizUrl>showShoppingListAjaxFired</@ofbizUrl>';
        if(checkUrl.search("http"))
            var ajaxUrl = '<@ofbizUrl>showShoppingListAjaxFired</@ofbizUrl>';
        else
            var ajaxUrl = '<@ofbizUrl>showShoppingListAjaxFiredSecure</@ofbizUrl>';
        //jQuerry Ajax Request
        jQuery.ajax({
            url: ajaxUrl,
            type: 'POST',
            data: {"shoppingListId" : str[0], "VIEW_SIZE" : str[1], "VIEW_INDEX" : str[2]},
            error: function(msg) {
                alert("An error occurred loading content! : " + msg);
            },
            success: function(msg) {
                jQuery('#div3').html(msg);
            }
        });
     }
//]]>
</script>
<#macro paginationControls>
  <#assign viewIndexMax = Static["java.lang.Math"].ceil((listSize)?double / viewSize?double)>
  <#if (viewIndexMax?int > 0)>
    <div class="product-prevnext">
        <#-- Start Page Select Drop-Down -->
        <select name="pageSelect" onchange="callDocumentByPaginate(this[this.selectedIndex].value);">
            <option value="#">${uiLabelMap.CommonPage} ${viewIndex?int} ${uiLabelMap.CommonOf} ${viewIndexMax}</option>
            <#if (viewIndex?int > 1)>
                <#list 0..viewIndexMax as curViewNum>
                     <option value="${shoppingListId!}~${viewSize}~${curViewNum?int + 1}">${uiLabelMap.CommonGotoPage} ${curViewNum + 1}</option>
                </#list>
            </#if>
        </select>
        <#-- End Page Select Drop-Down -->
        
        <#if (viewIndex?int > 1)>
            <a href="javascript: void(0);" onclick="callDocumentByPaginate('${shoppingListId!}~${viewSize}~${viewIndex?int - 1}');" class="buttontext">${uiLabelMap.CommonPrevious}</a> |
        </#if>
        <#if ((listSize?int - viewSize?int) > 0)>
            <span>${lowIndex} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}</span>
        </#if>
        <#if highIndex?int < listSize?int>
         | <a href="javascript: void(0);" onclick="callDocumentByPaginate('${shoppingListId!}~${viewSize}~${viewIndex?int + 1}');" class="buttontext">${uiLabelMap.CommonNext}</a>
        </#if>
    </div>
</#if>
</#macro>

<div class="screenlet">
        <div class="boxlink">
            <a href="<@ofbizUrl>createEmptyShoppingList?productStoreId=${productStoreId}</@ofbizUrl>" class="submenutextright">${uiLabelMap.CommonCreateNew}</a>
        </div>
    <h3>${uiLabelMap.EcommerceShoppingLists}</h3>
    <div class="screenlet-body">
        <#if shoppingLists?has_content>
          <form id="selectShoppingList" method="post" action="<@ofbizUrl>editShoppingList</@ofbizUrl>">
            <fieldset>
	            <select name="shoppingListId">
	              <#if shoppingList?has_content>
	                <option value="${shoppingList.shoppingListId}">${shoppingList.listName}</option>
	                <option value="${shoppingList.shoppingListId}">--</option>
	              </#if>
	              <#list shoppingLists as list>
	                <option value="${list.shoppingListId}">${list.listName}</option>
	              </#list>
	            </select>
	            <a href="javascript:$('selectShoppingList').submit();" class="button">${uiLabelMap.CommonEdit}</a>
            </fieldset>
          </form>
        <#else>
          <label>${uiLabelMap.EcommerceNoShoppingListsCreate}.</label>
          <a href="<@ofbizUrl>createEmptyShoppingList?productStoreId=${productStoreId}</@ofbizUrl>" class="submenutextright">${uiLabelMap.CommonCreateNew}</a>
        </#if>
    </div>
</div>

<#if shoppingList?has_content>
    <#if canView>

<div class="screenlet">
    <div class="screenlet-title-bar">
        <div class="boxlink">
          <a class='submenutext' href='javascript:$('createCustRequestFromShoppingList').submit()'>${uiLabelMap.OrderCreateCustRequestFromShoppingList}</a>
          <a class='submenutext' href='javascript:$('createQuoteFromShoppingList').submit()'>${uiLabelMap.OrderCreateQuoteFromShoppingList}</a>
          <a href="javascript:$('updateList').submit();" class="submenutextright">${uiLabelMap.CommonSave}</a>
        </div>
        <h3>${uiLabelMap.EcommerceShoppingListDetail} - ${shoppingList.listName}</h3>
    </div>
    <div class="screenlet-body">
      <form id= "createCustRequestFromShoppingList" method= "post" action= "<@ofbizUrl>createCustRequestFromShoppingList</@ofbizUrl>">
        <fieldset>
          <input type= "hidden" name= "shoppingListId" value= "${shoppingList.shoppingListId}"/>
        </fieldset>
      </form>
      <form name="createQuoteFromShoppingList" method="post" action="<@ofbizUrl>createQuoteFromShoppingList</@ofbizUrl>">
        <fieldset>
          <input type="hidden" name="shoppingListId" value="${shoppingList.shoppingListId}"/>
        </fieldset>
      </form>
      <form id="updateList" method="post" action="<@ofbizUrl>updateShoppingList</@ofbizUrl>">
        <fieldset class="inline">
          <input type="hidden" class="inputBox" name="shoppingListId" value="${shoppingList.shoppingListId}" />
          <input type="hidden" class="inputBox" name="partyId" value="${shoppingList.partyId?if_exists}" />
            <div>
              <label for="listName">${uiLabelMap.EcommerceListName}</label>
              <input type="text" size="25" name="listName" id="listName" value="${shoppingList.listName}" />
            </div>
            <div>
              <label for="description">${uiLabelMap.CommonDescription}</label>
              <input type="text" size="70" name="description" id="description" value="${shoppingList.description?if_exists}" />
            </div>
            <div>
              <label for="shoppingListTypeId">${uiLabelMap.OrderListType}</label>
              <select name="shoppingListTypeId" id="shoppingListTypeId">
                <#if shoppingListType??>
                  <option value="${shoppingListType.shoppingListTypeId}">${shoppingListType.get("description",locale)?default(shoppingListType.shoppingListTypeId)}</option>
                  <option value="${shoppingListType.shoppingListTypeId}">--</option>
                </#if>
                <#list shoppingListTypes as shoppingListType>
                  <option value="${shoppingListType.shoppingListTypeId}">${shoppingListType.get("description",locale)?default(shoppingListType.shoppingListTypeId)}</option>
                </#list>
              </select>
            </div>
            <div>
              <label for="isPublic">${uiLabelMap.EcommercePublic}?</label>
              <select name="isPublic" id="isPublic">
                <#if (((shoppingList.isPublic)!"") == "Y")><option value="Y">${uiLabelMap.CommonY}</option></#if>
                <#if (((shoppingList.isPublic)!"") == "N")><option value="N">${uiLabelMap.CommonN}</option></#if>
                <option></option>
                <option value="Y">${uiLabelMap.CommonY}</option>
                <option value="N">${uiLabelMap.CommonN}</option>
              </select>
            </div>
            <div>
              <label for="isActive">${uiLabelMap.EcommerceActive}?</label>
              <select name="isActive" id="isActive">
                <#if (((shoppingList.isActive)!"") == "Y")><option value="Y">${uiLabelMap.CommonY}</option></#if>
                <#if (((shoppingList.isActive)!"") == "N")><option value="N">${uiLabelMap.CommonN}</option></#if>
                <option></option>
                <option value="Y">${uiLabelMap.CommonY}</option>
                <option value="N">${uiLabelMap.CommonN}</option>
              </select>
            </div>
            <div>
              <label for="parentShoppingListId">${uiLabelMap.EcommerceParentList}</label>
              <select name="parentShoppingListId" id="parentShoppingListId">
                <#if parentShoppingList??>
                  <option value="${parentShoppingList.shoppingListId}">${parentShoppingList.listName?default(parentShoppingList.shoppingListId)}</option>
                </#if>
                <option value="">${uiLabelMap.EcommerceNoParent}</option>
                <#list allShoppingLists as newParShoppingList>
                  <option value="${newParShoppingList.shoppingListId}">${newParShoppingList.listName?default(newParShoppingList.shoppingListId)}</option>
                </#list>
              </select>
              <#if parentShoppingList??>
                <a href="<@ofbizUrl>editShoppingList?shoppingListId=${parentShoppingList.shoppingListId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonGotoParent} (${parentShoppingList.listName?default(parentShoppingList.shoppingListId)})</a>
              </#if>
            </div>
            <div>
              <input type="submit" class="button" name="submit" value="${uiLabelMap.CommonSave}">
            </div>
          </fieldset>
        </form>
    </div>
</div>

<#if shoppingListType?? && shoppingListType.shoppingListTypeId == "SLT_AUTO_REODR">
  <#assign nowTimestamp = Static["org.ofbiz.base.util.UtilDateTime"].monthBegin()>
<div class="screenlet">
    <div class="screenlet-title-bar">
        <div class="boxlink">
            <a href="javascript:document.reorderinfo.submit();" class="submenutextright">${uiLabelMap.CommonSave}</a>
        </div>
        <h3>
            ${uiLabelMap.EcommerceShoppingListReorder} - ${shoppingList.listName}
            <#if shoppingList.isActive?default("N") == "N">
                ${uiLabelMap.EcommerceOrderNotActive}
            </#if>
        </h3>
    </div>
    <div class="screenlet-body">
        <form name="reorderinfo" method="post" action="<@ofbizUrl>updateShoppingList</@ofbizUrl>">
          <fieldset class="inline">
            <input type="hidden" name="shoppingListId" value="${shoppingList.shoppingListId}" />
            <div>
              <span>
                <label>${uiLabelMap.EcommerceRecurrence}</label>
                <#if recurrenceInfo?has_content>
                  <#assign recurrenceRule = recurrenceInfo.getRelatedOne("RecurrenceRule", false)!>
                </#if>
                <select name="intervalNumber" class="selectBox">
                  <option value="">${uiLabelMap.EcommerceSelectInterval}</option>
                  <option value="1" <#if (recurrenceRule.intervalNumber)?default(0) == 1>selected="selected"</#if>>${uiLabelMap.EcommerceEveryDay}</option>
                  <option value="2" <#if (recurrenceRule.intervalNumber)?default(0) == 2>selected="selected"</#if>>${uiLabelMap.EcommerceEveryOther}</option>
                  <option value="3" <#if (recurrenceRule.intervalNumber)?default(0) == 3>selected="selected"</#if>>${uiLabelMap.EcommerceEvery3rd}</option>
                  <option value="6" <#if (recurrenceRule.intervalNumber)?default(0) == 6>selected="selected"</#if>>${uiLabelMap.EcommerceEvery6th}</option>
                  <option value="9" <#if (recurrenceRule.intervalNumber)?default(0) == 9>selected="selected"</#if>>${uiLabelMap.EcommerceEvery9th}</option>
                </select>
                <select name="frequency" class="selectBox">
                  <option value="">${uiLabelMap.EcommerceSelectFrequency}</option>
                  <option value="4" <#if (recurrenceRule.frequency)?default("") == "DAILY">selected="selected"</#if>>${uiLabelMap.CommonDay}</option>
                  <option value="5" <#if (recurrenceRule.frequency)?default("") == "WEEKLY">selected="selected"</#if>>${uiLabelMap.CommonWeek}</option>
                  <option value="6" <#if (recurrenceRule.frequency)?default("") == "MONTHLY">selected="selected"</#if>>${uiLabelMap.CommonMonth}</option>
                  <option value="7" <#if (recurrenceRule.frequency)?default("") == "YEARLY">selected="selected"</#if>>${uiLabelMap.CommonYear}</option>
                </select>
              </span>
              <span>
	              <label>${uiLabelMap.CommonStartDate}</label>
	              <input type="text" class="textBox" name="startDateTime" size="22" value="${(recurrenceInfo.startDateTime)?if_exists}" />
	              <a href="javascript:call_cal(document.reorderinfo.startDateTime, '${nowTimestamp.toString()}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" alt="Calendar" /></a>
              </span>
              <span>
                <label>${uiLabelMap.CommonEndDate}</label>
                <input type="text" class="textBox" name="endDateTime" size="22" value="${(recurrenceRule.untilDateTime)?if_exists}">
                <a href="javascript:call_cal(document.reorderinfo.endDateTime, '${nowTimestamp.toString()}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
              </span>
            </div>
            <div>
              <span><label>${uiLabelMap.OrderShipTo}</label>
                <select name="contactMechId" class="selectBox" onchange="javascript:document.reorderinfo.submit()">
                  <option value="">${uiLabelMap.OrderSelectAShippingAddress}</option>
                  <#if shippingContactMechList?has_content>
                    <#list shippingContactMechList as shippingContactMech>
                      <#assign shippingAddress = shippingContactMech.getRelatedOne("PostalAddress", false)>
                      <option value="${shippingContactMech.contactMechId}"<#if (shoppingList.contactMechId)?default("") == shippingAddress.contactMechId> selected="selected"</#if>>${shippingAddress.address1}</option>
                    </#list>
                  <#else>
                    <option value="">${uiLabelMap.OrderNoAddressesAvailable}</option>
                  </#if>
                </select>
                </span>
                <span><label>${uiLabelMap.OrderShipVia}</label>
                  <select name="shippingMethodString" class="selectBox">
                    <option value="">${uiLabelMap.OrderSelectShippingMethod}</option>
                    <#if carrierShipMethods?has_content>
                      <#list carrierShipMethods as shipMeth>
                        <#assign shippingEst = shippingEstWpr.getShippingEstimate(shipMeth)?default(-1)>
                        <#assign shippingMethod = shipMeth.shipmentMethodTypeId + "@" + shipMeth.partyId>
                        <option value="${shippingMethod}"<#if shippingMethod == chosenShippingMethod> selected="selected"</#if>>
                          <#if shipMeth.partyId != "_NA_">
                            ${shipMeth.partyId!}&nbsp;
                          </#if>
                          ${shipMeth.description!}
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
                </span>
                <span><label>${uiLabelMap.OrderPayBy}</label>
                  <select name="paymentMethodId" class="selectBox">
                    <option value="">${uiLabelMap.OrderSelectPaymentMethod}</option>
                    <#list paymentMethodList as paymentMethod>
                      <#if paymentMethod.paymentMethodTypeId == "CREDIT_CARD">
                        <#assign creditCard = paymentMethod.getRelatedOne("CreditCard", false)>
                        <option value="${paymentMethod.paymentMethodId}" <#if (shoppingList.paymentMethodId)?default("") == paymentMethod.paymentMethodId>selected="selected"</#if>>CC:&nbsp;${Static["org.ofbiz.party.contact.ContactHelper"].formatCreditCard(creditCard)}</option>
                      <#elseif paymentMethod.paymentMethodTypeId == "EFT_ACCOUNT">
                        <#assign eftAccount = paymentMethod.getRelatedOne("EftAccount", false)>
                        <option value="${paymentMethod.paymentMethodId}">EFT:&nbsp;${eftAccount.bankName!}: ${eftAccount.accountNumber!}</option>
                      </#if>
                    </#list>
                  </select>
                </span>
              </div>
              <div>
                <a href="javascript:document.reorderinfo.submit();" class="buttontext">${uiLabelMap.CommonSave}</a>
                <a href="<@ofbizUrl>editcontactmech?preContactMechTypeId=POSTAL_ADDRESS&amp;contactMechPurposeTypeId=SHIPPING_LOCATION&amp;DONE_PAGE=editShoppingList</@ofbizUrl>" class="buttontext">${uiLabelMap.PartyAddNewAddress}</a>
                <a href="<@ofbizUrl>editcreditcard?DONE_PAGE=editShoppingList</@ofbizUrl>" class="buttontext">${uiLabelMap.EcommerceNewCreditCard}</a>
                <a href="<@ofbizUrl>editeftaccount?DONE_PAGE=editShoppingList</@ofbizUrl>" class="buttontext">${uiLabelMap.EcommerceNewEFTAccount}</a>
              </div>
              <#if shoppingList.isActive?default("N") == "Y">
                <div>
                  <#assign nextTime = recInfo.next(lastSlOrderTime)?if_exists />
                  <#if nextTime?has_content>
                    <#assign nextTimeStamp = Static["org.ofbiz.base.util.UtilDateTime"].getTimestamp(nextTime)?if_exists />
                    <#if nextTimeStamp?has_content>
                      <#assign nextTimeString = Static["org.ofbiz.base.util.UtilFormatOut"].formatDate(nextTimeStamp)?if_exists />
                    </#if>
                  </#if>
                  <#if lastSlOrderDate?has_content>
                    <#assign lastOrderedString = Static["org.ofbiz.base.util.UtilFormatOut"].formatDate(lastSlOrderDate)!>
                  </#if>
                    <div class="tabletext">
                      <table>
                        <tr>
                          <td>${uiLabelMap.OrderLastOrderedDate}</div></td>
                          <td>:</td>
                          <td>${lastOrderedString?default("${uiLabelMap.OrderNotYetOrdered}")}</td>
                        </tr>
                        <tr>
                          <td>${uiLabelMap.EcommerceEstimateNextOrderDate}</td>
                          <td>:</td>
                          <td>${nextTimeString?default("${uiLabelMap.EcommerceNotYetKnown}")}</td>
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
    <div class="screenlet-title-bar">
        <div class="boxlink">
            <a href="<@ofbizUrl>addListToCart?shoppingListId=${shoppingList.shoppingListId}&amp;includeChild=yes</@ofbizUrl>" class="submenutextright">${uiLabelMap.EcommerceAddChildListsToCart}</a>
        </div>
        <h3>&nbsp;${uiLabelMap.EcommerceChildShoppingList} - ${shoppingList.listName}</h3>
    </div>
    <div class="screenlet-body">
        <table>
          <thead>
	          <tr>
	            <th>${uiLabelMap.EcommerceListName}</th>
	            <th>${uiLabelMap.EcommerceListName}</th>
	            <th>&nbsp;</th>
	            <th>&nbsp;</th>
	          </tr>
          </thead>
          <tbody>
          <#list childShoppingListDatas as childShoppingListData>
              <#assign childShoppingList = childShoppingListData.childShoppingList/>
              <#assign totalPrice = childShoppingListData.totalPrice/>
              <tr>
                <td>
                  <a href="<@ofbizUrl>editShoppingList?shoppingListId=${childShoppingList.shoppingListId}</@ofbizUrl>" class="button">${childShoppingList.listName?default(childShoppingList.shoppingListId)}</a>
                </td>
                <td>
                  <@ofbizCurrency amount=totalPrice isoCode=currencyUomId/>
                </td>
                <td>
                  <a href="<@ofbizUrl>editShoppingList?shoppingListId=${childShoppingList.shoppingListId}</@ofbizUrl>" class="button">${uiLabelMap.EcommerceGoToList}</a>
                  <a href="<@ofbizUrl>addListToCart?shoppingListId=${childShoppingList.shoppingListId}</@ofbizUrl>" class="button">${uiLabelMap.EcommerceAddListToCart}</a>
                </td>
              </tr>
            </form>
          </#list>
          <tr><td colspan="6"><hr /></td></tr>
          <tr>
            <td>&nbsp;</td>
            <td>
              <@ofbizCurrency amount=shoppingListChildTotal isoCode=currencyUomId/>
            </td>
            <td>&nbsp;</td>
          </tr>
        </table>
    </div>
</div>
</#if>

<div class="screenlet">
    <div class="screenlet-title-bar">
        <div class="boxlink">
            <a href="<@ofbizUrl>addListToCart?shoppingListId=${shoppingList.shoppingListId}</@ofbizUrl>" class="submenutextright">${uiLabelMap.EcommerceAddListToCart}</a>
        </div>
        <h3>${uiLabelMap.EcommerceListItems} - ${shoppingList.listName}</h3>
    </div>
    <div class="screenlet-body">
        <#if shoppingListItemDatas?has_content>
            <#-- Pagination -->
            <@paginationControls/>
            <table width="100%">
              <thead>
	              <tr>
	                <th>${uiLabelMap.OrderProduct}</th>
	                <th><table><tr><th>- ${uiLabelMap.EcommerceStartdate} -</th><th>- ${uiLabelMap.EcommerceNbrOfDays} -</th></tr><tr><th>- ${uiLabelMap.EcommerceNbrOfPersons} -</th><th>- ${uiLabelMap.CommonQuantity} -</th></tr></table></th>
	                <#-- <td nowrap="nowrap" align="center"><div><b>Purchased</b></div></td> -->
	                <th>${uiLabelMap.EcommercePrice}</th>
	                <th>${uiLabelMap.OrderTotal}</th>
	                <th>&nbsp;</th>
	              </tr>
	            </thead>
	            <tbody>
              <#list shoppingListItemDatas[lowIndex-1..highIndex-1] as shoppingListItemData>
                <#assign shoppingListItem = shoppingListItemData.shoppingListItem/>
                <#assign product = shoppingListItemData.product/>
                <#assign productContentWrapper = Static["org.ofbiz.product.product.ProductContentWrapper"].makeProductContentWrapper(product, request)/>
                <#assign unitPrice = shoppingListItemData.unitPrice/>
                <#assign totalPrice = shoppingListItemData.totalPrice/>
                <#assign productVariantAssocs = shoppingListItemData.productVariantAssocs!/>
                <#assign isVirtual = product.isVirtual?? && product.isVirtual.equals("Y")/>
                  <tr>
                    <td>
                      <a href="<@ofbizUrl>product?product_id=${shoppingListItem.productId}</@ofbizUrl>" class="button">${shoppingListItem.productId} -
                      ${productContentWrapper.get("PRODUCT_NAME", "html")?default("No Name")}</a> : ${productContentWrapper.get("DESCRIPTION", "html")!}
                    </td>
                    <td>
                      <form method="post" action="<@ofbizUrl>updateShoppingListItem</@ofbizUrl>" name="listform_${shoppingListItem.shoppingListItemSeqId}">
                        <fieldset>
                          <input type="hidden" name="shoppingListId" value="${shoppingListItem.shoppingListId}" />
                          <input type="hidden" name="shoppingListItemSeqId" value="${shoppingListItem.shoppingListItemSeqId}" />
                          <input type="hidden" name="reservStart" />
                          <#if product.productTypeId == "ASSET_USAGE">
                           <table>
                             <tr>
                               <td>&nbsp;</td>
                               <td>
                                 <input type="text" class="inputBox" size="10" name="reservStartStr" value="${shoppingListItem.reservStart?if_exists}" />
                               </td>
                               <td>
                                 <input type="text" class="inputBox" size="2" name="reservLength" value="${shoppingListItem.reservLength?if_exists}" />
                               </td>
                             </tr>
                             <tr>
                               <td>&nbsp;</td>
                               <td>
                                 <input type="text" class="inputBox" size="3" name="reservPersons" value="${shoppingListItem.reservPersons?if_exists}" />
                               </td>
                               <td>
                          <#else>
                            <table>
                              <tr>
                                <td>--</td>
                                <td>--</td>
                              </tr>
                              <tr>
                                <td>--</td>
                                <td><input type="hidden" name="reservStartStr" value="" />
                          </#if>
                          <input size="6" class="inputBox" type="text" name="quantity" value="${shoppingListItem.quantity?string.number}" />
                          </td></tr></table>
                        </fieldset>
                      </form>
                    </td>
                    <#--
                    <td nowrap="nowrap" align="center">
                      <div>${shoppingListItem.quantityPurchased?default(0)?string.number}</div>
                    </td>
                    -->
                    <td>
                      <@ofbizCurrency amount=unitPrice isoCode=currencyUomId/>
                    </td>
                    <td>
                      <@ofbizCurrency amount=totalPrice isoCode=currencyUomId/>
                    </td>
                    <td>
                        <a href="javascript:TimestampSubmit(listform_${shoppingListItem.shoppingListItemSeqId});" class="button">${uiLabelMap.CommonUpdate}</a>
                        <a href="<@ofbizUrl>removeFromShoppingList?shoppingListId=${shoppingListItem.shoppingListId}&amp;shoppingListItemSeqId=${shoppingListItem.shoppingListItemSeqId}</@ofbizUrl>" class="button">${uiLabelMap.CommonRemove}</a>
                      <#if isVirtual && productVariantAssocs?has_content>
                        <#assign replaceItemAction = "/replaceShoppingListItem/" + requestAttributes._CURRENT_VIEW_?if_exists />
                        <#assign addToCartAction = "/additem/" + requestAttributes._CURRENT_VIEW_?if_exists />
                        <form method="post" action="<@ofbizUrl>${addToCartAction}</@ofbizUrl>" name="listreplform_${shoppingListItem.shoppingListItemSeqId}">
                          <fieldset>
                            <input type="hidden" name="shoppingListId" value="${shoppingListItem.shoppingListId}" />
                            <input type="hidden" name="shoppingListItemSeqId" value="${shoppingListItem.shoppingListItemSeqId}" />
                            <input type="hidden" name="quantity" value="${shoppingListItem.quantity}" />
                            <select name="add_product_id" class="selectBox">
                              <#list productVariantAssocs as productVariantAssoc>
                                <#assign variantProduct = productVariantAssoc.getRelatedOneCache("AssocProduct") />
                                <#if variantProduct??>
                                <#assign variantProductContentWrapper = Static["org.ofbiz.product.product.ProductContentWrapper"].makeProductContentWrapper(variantProduct, request) />
                                  <option value="${variantProduct.productId}">${variantproductContentWrapper.get("PRODUCT_NAME", "html")?default("No Name")} [${variantProduct.productId}]</option>
                                </#if>
                              </#list>
                            </select>
                            <div>
                              <a href="javascript:document.listreplform_${shoppingListItem.shoppingListItemSeqId}.action='<@ofbizUrl>${replaceItemAction}</@ofbizUrl>';document.listreplform_${shoppingListItem.shoppingListItemSeqId}.submit();" class="button">${uiLabelMap.EcommerceReplaceWithVariation}</a>
                            </div>
                            <div>
                              <a href="javascript:document.listreplform_${shoppingListItem.shoppingListItemSeqId}.action='<@ofbizUrl>${addToCartAction}</@ofbizUrl>';document.listreplform_${shoppingListItem.shoppingListItemSeqId}.submit();" class="button">${uiLabelMap.CommonAdd}&nbsp;${shoppingListItem.quantity?string}&nbsp;${uiLabelMap.EcommerceVariationToCart}</a>
                            </div>
                          </fieldset>
                        </form>
                      <#else>
                        <a href="<@ofbizUrl>additem<#if requestAttributes._CURRENT_VIEW_?exists>/${requestAttributes._CURRENT_VIEW_}</#if>?shoppingListId=${shoppingListItem.shoppingListId}&amp;shoppingListItemSeqId=${shoppingListItem.shoppingListItemSeqId}&amp;quantity=${shoppingListItem.quantity}&amp;reservStart=${shoppingListItem.reservStart?if_exists}&amp;reservPersons=${shoppingListItem.reservPersons?if_exists}&amp;reservLength=${shoppingListItem.reservLength?if_exists}&amp;configId=${shoppingListItem.configId?if_exists}&amp;add_product_id=${shoppingListItem.productId}</@ofbizUrl>" class="button">${uiLabelMap.CommonAdd}&nbsp;${shoppingListItem.quantity?string}&nbsp;${uiLabelMap.OrderToCart}</a>
                      </#if>
                    </td>
                  </tr>
              </#list>
              <tr><td><hr /></td></tr>
              <tr>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <#--<td><div>&nbsp;</div></td>-->
                <td>&nbsp;</td>
                <td>
                  <@ofbizCurrency amount=shoppingListItemTotal isoCode=currencyUomId/>
                </td>
                <td>&nbsp;</td>
              </tr>
              </tbody>
            </table>
        <#else>
            <h2>${uiLabelMap.EcommerceShoppingListEmpty}.</h2>
        </#if>
    </div>
</div>

<div class="screenlet">
    <h3>${uiLabelMap.EcommerceShoppingListPriceTotals} - ${shoppingList.listName}</h3>
    <div class="screenlet-body">
      <div>
        <label>${uiLabelMap.EcommerceChildListTotalPrice}</label>
        <@ofbizCurrency amount=shoppingListChildTotal isoCode=currencyUomId/>
      </div>
      <div>
        <label>${uiLabelMap.EcommerceListItemsTotalPrice}</label>
        <@ofbizCurrency amount=shoppingListItemTotal isoCode=currencyUomId/>
      </div>
      <div>
        <label>${uiLabelMap.OrderGrandTotal}</label>
        <@ofbizCurrency amount=shoppingListTotalPrice isoCode=currencyUomId/>
      </div>
    </div>
</div>

<div class="screenlet">
    <h3>${uiLabelMap.CommonQuickAddList}</h3>
    <div class="screenlet-body">
        <form name="addToShoppingList" method="post" action="<@ofbizUrl>addItemToShoppingList</@ofbizUrl>">
          <fieldset class="inline>
            <input type="hidden" name="shoppingListId" value="${shoppingList.shoppingListId}" />
            <input type="text" class="inputBox" name="productId" value="${requestParameters.add_product_id?if_exists}" />
            <#if reservStart?exists><label>${uiLabelMap.EcommerceStartDate}</label><input type="text" class="inputBox" size="10" name="reservStart" value="${requestParameters.reservStart?default("")}" /><label> ${uiLabelMap.EcommerceLength}:</label><input type="text" class="inputBox" size="2" name="reservLength" value="${requestParameters.reservLength?default("")}" ><label>${uiLabelMap.OrderNbrPersons}:</label><input type="text" class="inputBox" size="3" name="reservPersons" value="${requestParameters.reservPersons?default("1")}" /></#if> <label>${uiLabelMap.CommonQuantity} :</label><input type="text" class="inputBox" size="5" name="quantity" value="${requestParameters.quantity?default("1")}" />
            <!-- <input type="text" class="inputBox" size="5" name="quantity" value="${requestParameters.quantity?default("1")}" />-->
            <input type="submit" class="smallSubmit" value="${uiLabelMap.OrderAddToShoppingList}" />
          </fieldset>
        </form>
    </div>
</div>

    <#else>
        <#-- shoppingList was found, but belongs to a different party -->
        <h2>${uiLabelMap.EcommerceShoppingListError} ${uiLabelMap.CommonId} ${shoppingList.shoppingListId}) ${uiLabelMap.EcommerceListDoesNotBelong}.</h2>
    </#if>
</#if>
