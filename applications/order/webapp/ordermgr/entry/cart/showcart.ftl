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
    function showQohAtp() {
        document.qohAtpForm.productId.value = document.quickaddform.add_product_id.value;
        document.qohAtpForm.submit();
    }
    function quicklookupGiftCertificate() {
        window.location='AddGiftCertificate';
    }
</script>
<#if shoppingCart.getOrderType() == "PURCHASE_ORDER">
  <#assign target="productAvailabalityByFacility">
<#else>
  <#assign target="getProductInventoryAvailable">
</#if>
<div class="screenlet">
    <div class="screenlet-body">
      <#if shoppingCart.getOrderType() == "SALES_ORDER">
        <div>
          <#if quantityOnHandTotal?exists && availableToPromiseTotal?exists && (productId)?exists>
            <ul>
              <li>
                <label>${uiLabelMap.ProductQuantityOnHand}</label>: ${quantityOnHandTotal}
              </li>
              <li>
                <label>${uiLabelMap.ProductAvailableToPromise}</label>: ${availableToPromiseTotal}
              </li>
            </ul>
          </#if>
        </div>
      <#else>
        <#if parameters.availabalityList?has_content>
          <table>
            <tr>
              <td>${uiLabelMap.Facility}</td>
              <td>${uiLabelMap.ProductQuantityOnHand}</td>
              <td>${uiLabelMap.ProductAvailableToPromise}</td>
            </tr>
            <#list parameters.availabalityList as availabality>
               <tr>
                 <td>${availabality.facilityId}</td>
                 <td>${availabality.quantityOnHandTotal}</td>
                 <td>${availabality.availableToPromiseTotal}</td>
               </tr>
            </#list>
          </table>
        </#if>
      </#if>
      <table border="0" cellspacing="0" cellpadding="0">
        <tr>
          <td>
            <form name="qohAtpForm" method="post" action="<@ofbizUrl>${target}</@ofbizUrl>">
              <fieldset>
                <input type="hidden" name="facilityId" value="${facilityId?if_exists}"/>
                <input type="hidden" name="productId"/>
                <input type="hidden" id="ownerPartyId" name="ownerPartyId" value="${shoppingCart.getBillToCustomerPartyId()?if_exists}" />
              </fieldset>
            </form>
            <form method="post" action="<@ofbizUrl>additem</@ofbizUrl>" name="quickaddform" style="margin: 0;">
              <table border="0">
                <tr>
                  <td align="right"><div>${uiLabelMap.ProductProductId} :</div></td>
                  <td><input type="text" size="25" name="add_product_id" value="${productId?if_exists}"/>
                    <span class='tabletext'>
                      <a href="javascript:quicklookup(document.quickaddform.add_product_id)" class="buttontext">${uiLabelMap.OrderQuickLookup}</a>
                      <a href="javascript:call_fieldlookup2(document.quickaddform.add_product_id,'<@ofbizUrl><#if orderType=="PURCHASE_ORDER">LookupSupplierProduct?partyId=${partyId?if_exists}<#else>LookupProduct</#if></@ofbizUrl>');">
                        <img src="<@ofbizContentUrl>/images/fieldlookup.gif</@ofbizContentUrl>" width="15" height="14" border="0" alt="${uiLabelMap.CommonClickHereForFieldLookup}"/>
                      </a>
                      <#-- FIXME Problem here: the input field is shared -->
                      <#--if orderType=="PURCHASE_ORDER">                        
                        <#if partyId?has_content>                                               
                          <#assign fieldFormName="LookupSupplierProduct?partyId=${partyId}">
                        <#else>
                          <#assign fieldFormName="LookupSupplierProduct">
                        </#if>
                      <#else>
                        <#assign fieldFormName="LookupProduct">
                      </#if>
                      <@htmlTemplate.lookupField formName="quickaddform" name="add_product_id" id="add_product_id" fieldFormName="${fieldFormName}"/-->
                      <a href="javascript:quicklookupGiftCertificate()" class="buttontext">${uiLabelMap.OrderAddGiftCertificate}</a>
                      <#if "PURCHASE_ORDER" == shoppingCart.getOrderType()>
                        <a href="javascript:showQohAtp()" class="buttontext">${uiLabelMap.ProductAtpQoh}</a>
                      </#if>
                    </span>
                  </td>
                </tr>
                <tr>
                  <td align="right"><div>${uiLabelMap.OrderQuantity} :</div></td>
                  <td><input type="text" size="6" name="quantity" value=""/></td>
                </tr>
                <tr>
                  <td align="right"><div>${uiLabelMap.OrderDesiredDeliveryDate} :</div></td>
                  <td>
                    <div>
                      <input type="text" size="25" maxlength="30" name="itemDesiredDeliveryDate"<#if useAsDefaultDesiredDeliveryDate?exists> value="${defaultDesiredDeliveryDate}"</#if>/>
                      <a href="javascript:call_cal(document.quickaddform.itemDesiredDeliveryDate,'${defaultDesiredDeliveryDate} 00:00:00.0');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="${uiLabelMap.OrderCalendarClickHereForCalendar}"/></a>
                      <input type="checkbox" name="useAsDefaultDesiredDeliveryDate" value="true"<#if useAsDefaultDesiredDeliveryDate?exists> checked="checked"</#if>/>
                      ${uiLabelMap.OrderUseDefaultDesiredDeliveryDate}
                    </div>
                  </td>
                </tr>
                <tr>
                  <td align="right"><div>${uiLabelMap.OrderShipAfterDate} :</div></td>
                  <td>
                    <div>
                      <input type="text" size="20" maxlength="30" name="shipAfterDate" value="${shoppingCart.getDefaultShipAfterDate()?default("")}"/>
                      <a href="javascript:call_cal(document.quickaddform.shipAfterDate,'${shoppingCart.getDefaultShipAfterDate()?default("")}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="${uiLabelMap.OrderCalendarClickHereForCalendar}"/></a>
                    </div>
                  </td>
                </tr>
                <tr>
                  <td align="right"><div>${uiLabelMap.OrderShipBeforeDate} :</div></td>
                  <td>
                    <div>
                      <input type="text" size="20" maxlength="30" name="shipBeforeDate" value="${shoppingCart.getDefaultShipBeforeDate()?default("")}"/>
                      <a href="javascript:call_cal(document.quickaddform.shipBeforeDate,'${shoppingCart.getDefaultShipBeforeDate()?default("")}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="${uiLabelMap.OrderCalendarClickHereForCalendar}"/></a>
                    </div>
                  </td>
                </tr>
                <#if shoppingCart.getOrderType() == "PURCHASE_ORDER">
                <tr>
                  <td align="right"><div>${uiLabelMap.OrderOrderItemType} :</div></td>
                  <td>
                    <div>
                      <select name="add_item_type">
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
                  <td align="right"><div>${uiLabelMap.CommonComment} :</div></td>
                  <td>
                    <div>
                      <input type="text" size="25" name="itemComment" value="${defaultComment?if_exists}" />
                      <input type="checkbox" name="useAsDefaultComment" value="true" <#if useAsDefaultComment?exists>checked="checked"</#if> />
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
        <tr><td><hr /></td></tr>
        <tr>
          <td>
            <form method="post" action="<@ofbizUrl>additem</@ofbizUrl>" name="bulkworkaddform" style="margin: 0;">
                <div>
                    ${uiLabelMap.OrderOrderItemType}:&nbsp;<select name="add_item_type"><option value="BULK_ORDER_ITEM">${uiLabelMap.ProductBulkItem}</option><option value="WORK_ORDER_ITEM">${uiLabelMap.ProductWorkItem}</option></select>
                    <br/>${uiLabelMap.ProductProductCategory}:&nbsp;
                    <@htmlTemplate.lookupField formName="bulkworkaddform" value="${requestParameters.add_category_id?if_exists}" name="add_category_id" id="add_category_id" fieldFormName="LookupProductCategory"/>
                </div>
                <div>
                    ${uiLabelMap.CommonDescription}:&nbsp;<input type="text" size="25" name="add_item_description" value=""/>
                    ${uiLabelMap.OrderQuantity}:&nbsp;<input type="text" size="3" name="quantity" value="${requestParameters.quantity?default("1")}"/>
                    ${uiLabelMap.OrderPrice}:&nbsp;<input type="text" size="6" name="price" value="${requestParameters.price?if_exists}"/>
                    <input type="submit" class="smallSubmit" value="${uiLabelMap.OrderAddToOrder}"/>
                </div>
            </form>
          </td>
        </tr>
        </#if>
      </table>
    </div>
</div>

<script language="JavaScript" type="text/javascript">
  document.quickaddform.add_product_id.focus();
</script>

<!-- Internal cart info: productStoreId=${shoppingCart.getProductStoreId()?if_exists} locale=${shoppingCart.getLocale()?if_exists} currencyUom=${shoppingCart.getCurrency()?if_exists} userLoginId=${(shoppingCart.getUserLogin().getString("userLoginId"))?if_exists} autoUserLogin=${(shoppingCart.getAutoUserLogin().getString("userLoginId"))?if_exists} -->
