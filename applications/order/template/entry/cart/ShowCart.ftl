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

<script type="application/javascript">
    function showQohAtp() {
        document.qohAtpForm.productId.value = document.quickaddform.add_product_id.value;
        document.qohAtpForm.submit();
    }
    function quicklookupGiftCertificate() {
        window.location='AddGiftCertificate';
    }
</script>
<#if "PURCHASE_ORDER" == shoppingCart.getOrderType()>
  <#assign target="productAvailabalityByFacility">
<#else>
  <#assign target="getProductInventoryAvailable">
</#if>
<div class="screenlet">
    <div class="screenlet-body">
      <#if "SALES_ORDER" == shoppingCart.getOrderType()>
        <div>
          <#if quantityOnHandTotal?? && availableToPromiseTotal?? && (productId)??>
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
          <table class="basic-table"  cellspacing="0">
            <tr class="header-row">
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
                <input type="hidden" name="facilityId" value="${facilityId!}"/>
                <input type="hidden" name="productId"/>
                <input type="hidden" id="ownerPartyId" name="ownerPartyId" value="${shoppingCart.getBillToCustomerPartyId()!}" />
              </fieldset>
            </form>
            <form method="post" action="<@ofbizUrl>additem</@ofbizUrl>" name="quickaddform" style="margin: 0;">
              <table border="0">
                <tr>
                  <td align="right"><div>${uiLabelMap.ProductProductId} :</div></td>
                  <td>
                    <span class='tabletext'>
                      <#if orderType=="PURCHASE_ORDER">
                        <#if partyId?has_content>
                          <#assign fieldFormName="LookupSupplierProduct?partyId=${partyId}">
                        <#else>
                          <#assign fieldFormName="LookupSupplierProduct">
                        </#if>
                      <#else>
                        <#assign fieldFormName="LookupProduct">
                      </#if>
                      <@htmlTemplate.lookupField formName="quickaddform" name="add_product_id" id="add_product_id" fieldFormName="${fieldFormName}" value="${parameters.productId!}"/>
                      <a href="javascript:quicklookup(document.quickaddform.add_product_id)" class="buttontext">${uiLabelMap.OrderQuickLookup}</a>
                        <#if "SALES_ORDER" == shoppingCart.getOrderType()>
                      <a href="javascript:quicklookupGiftCertificate()" class="buttontext">${uiLabelMap.OrderAddGiftCertificate}</a>
                        </#if>
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
                      <#if useAsDefaultDesiredDeliveryDate??> 
                        <#assign value = defaultDesiredDeliveryDate>
                      </#if>
                      <@htmlTemplate.renderDateTimeField name="itemDesiredDeliveryDate" value="${value!''}" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" size="25" maxlength="30" id="item1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                      <label>
                      <input type="checkbox" name="useAsDefaultDesiredDeliveryDate" value="true"<#if useAsDefaultDesiredDeliveryDate??> checked="checked"</#if>/>
                      ${uiLabelMap.OrderUseDefaultDesiredDeliveryDate}
                      </label>
                    </div>
                  </td>
                </tr>
                <tr>
                  <td align="right"><div>${uiLabelMap.OrderShipAfterDate} :</div></td>
                  <td>
                    <div>
                      <@htmlTemplate.renderDateTimeField name="shipAfterDate" value="${shoppingCart.getDefaultShipAfterDate()!''}" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" size="25" maxlength="30" id="item2" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                    </div>
                  </td>
                </tr>
                <tr>
                  <td align="right"><div>${uiLabelMap.OrderShipBeforeDate} :</div></td>
                  <td>
                    <div>
                      <@htmlTemplate.renderDateTimeField name="shipBeforeDate" value="${shoppingCart.getDefaultShipBeforeDate()!''}" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" size="25" maxlength="30" id="item3" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                    </div>
                  </td>
                </tr>
                <#if "SALES_ORDER" == shoppingCart.getOrderType()>
                  <tr>
                    <td align="right"><div>${uiLabelMap.OrderReserveAfterDate} :</div></td>
                    <td>
                      <div>
                        <@htmlTemplate.renderDateTimeField name="reserveAfterDate" value="${shoppingCart.getDefaultReserveAfterDate()!''}" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" size="25" maxlength="30" id="item4" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                      </div>
                    </td>
                  </tr>
                </#if>
                <#if "PURCHASE_ORDER" == shoppingCart.getOrderType()>
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
                      <input type="text" size="25" name="itemComment" value="${defaultComment!}" />
                      <label>
                      <input type="checkbox" name="useAsDefaultComment" value="true" <#if useAsDefaultComment??>checked="checked"</#if> />
                      ${uiLabelMap.OrderUseDefaultComment}
                      </label>
                    </div>
                  </td>
                </tr>
                <#if "SALES_ORDER" == shoppingCart.getOrderType()>
                  <#assign productStore = Static["org.apache.ofbiz.product.store.ProductStoreWorker"].getProductStore(shoppingCart.getProductStoreId(), delegator) />
                  <#if productStore?has_content && (productStore.allocateInventory)?has_content && (productStore.allocateInventory).equals('Y')>
                  <tr>
                    <td align="right"><div>${uiLabelMap.OrderAutoReserve}</div></td>
                    <td>
                      <div>
                        <label>
                          <input type="checkbox" name="order_item_attr_autoReserve" value="true" <#if autoReserve??>checked="checked"</#if> />
                        </label>
                      </div>
                    </td>
                  </tr>
                  </#if>
                </#if>
                <tr>
                  <td></td>
                  <td><input type="submit" class="smallSubmit" value="${uiLabelMap.OrderAddToOrder}"/></td>
                </tr>
              </table>
            </form>
          </td>
        </tr>
        <#if "PURCHASE_ORDER" == shoppingCart.getOrderType()>
        <tr><td><hr /></td></tr>
        <tr>
          <td>
            <form method="post" action="<@ofbizUrl>additem</@ofbizUrl>" name="bulkworkaddform" style="margin: 0;">
                <div>
                    ${uiLabelMap.OrderOrderItemType}:&nbsp;<select name="add_item_type"><option value="BULK_ORDER_ITEM">${uiLabelMap.ProductBulkItem}</option><option value="WORK_ORDER_ITEM">${uiLabelMap.ProductWorkItem}</option></select>
                    <br/>${uiLabelMap.ProductProductCategory}:&nbsp;
                    <@htmlTemplate.lookupField formName="bulkworkaddform" value="${requestParameters.add_category_id!}" name="add_category_id" id="add_category_id" fieldFormName="LookupProductCategory"/>
                </div>
                <div>
                    ${uiLabelMap.CommonDescription}:&nbsp;<input type="text" size="25" name="add_item_description" value=""/>
                    ${uiLabelMap.OrderQuantity}:&nbsp;<input type="text" size="3" name="quantity" value="${requestParameters.quantity?default("1")}"/>
                    ${uiLabelMap.OrderPrice}:&nbsp;<input type="text" size="6" name="price" value="${requestParameters.price!}"/>
                    <input type="submit" class="smallSubmit" value="${uiLabelMap.OrderAddToOrder}"/>
                </div>
            </form>
          </td>
        </tr>
        </#if>
      </table>
    </div>
</div>

<script type="application/javascript">
  document.quickaddform.add_product_id.focus();
</script>

<!-- Internal cart info: productStoreId=${shoppingCart.getProductStoreId()!} locale=${shoppingCart.getLocale()!} currencyUom=${shoppingCart.getCurrency()!} userLoginId=${(shoppingCart.getUserLogin().getString("userLoginId"))!} autoUserLogin=${(shoppingCart.getAutoUserLogin().getString("userLoginId"))!} -->
