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
<table cellspacing="0" class="basic-table">
    <tr>
      <td colspan="7"><h3>${uiLabelMap.OrderReturnFromOrder} ${uiLabelMap.CommonNbr}<a href="<@ofbizUrl>orderview?orderId=${orderId}</@ofbizUrl>" class="buttontext">${orderId}</a></h3></td>
      <td colspan="2" align="right">
        <label><span>${uiLabelMap.CommonSelectAll}</span>&nbsp;
        <input type="checkbox" name="selectAll" value="Y" onclick="javascript:toggleAll(this, '${selectAllFormName}');highlightAllRows(this, 'returnItemId_tableRow_', 'selectAllForm');"/></label>
      </td>
    </tr>

    <#-- information about orders and amount refunded/credited on past returns -->
    <#if orh??>
    <tr><td colspan="10">
        <table cellspacing="0" class="basic-table">
          <tr>
            <td class="label" width="25%">${uiLabelMap.OrderOrderTotal}</td>
            <td><@ofbizCurrency amount=orh.getOrderGrandTotal() isoCode=orh.getCurrency()/></td>
          </tr>
          <tr>
            <td class="label" width="25%">${uiLabelMap.OrderAmountAlreadyCredited}</td>
            <td><@ofbizCurrency amount=orh.getOrderReturnedCreditTotalBd() isoCode=orh.getCurrency()/></td>
          </tr>
          <tr>
            <td class="label" width="25%">${uiLabelMap.OrderAmountAlreadyRefunded}</td>
            <td><@ofbizCurrency amount=orh.getOrderReturnedRefundTotalBd() isoCode=orh.getCurrency()/></td>
          </tr>
        </table>
    </td></tr>
    </#if>
    <tr class="header-row">
      <td>${uiLabelMap.CommonDescription}</td>
      <td>${uiLabelMap.OrderOrderQty}</td>
      <td>${uiLabelMap.OrderReturnQty}</td>
      <td>${uiLabelMap.OrderUnitPrice}</td>
      <td>${uiLabelMap.OrderReturnPrice}*</td>
      <td>${uiLabelMap.OrderReturnReason}</td>
      <td>${uiLabelMap.OrderReturnType}</td>
      <td>${uiLabelMap.OrderItemStatus}</td>
      <td align="right">${uiLabelMap.OrderOrderInclude}?</td>
    </tr>
    <#if returnableItems?has_content>
      <#assign rowCount = 0>
      <#assign alt_row = false>
      <#list returnableItems.keySet() as orderItem>
        <#if orderItem.getEntityName() == "OrderAdjustment">
            <#-- this is an order item adjustment -->
            <#assign returnAdjustmentType = returnItemTypeMap.get(orderItem.get("orderAdjustmentTypeId"))/>
            <#assign adjustmentType = orderItem.getRelatedOne("OrderAdjustmentType", false)/>
            <#assign description = orderItem.description?default(adjustmentType.get("description",locale))/>

            <tr id="returnItemId_tableRow_${rowCount}" valign="middle"<#if alt_row> class="alternate-row"</#if>>
              <td colspan="4">
            <input type="hidden" name="returnAdjustmentTypeId_o_${rowCount}" value="${returnAdjustmentType}"/>
            <input type="hidden" name="orderAdjustmentId_o_${rowCount}" value="${orderItem.orderAdjustmentId}"/>
                ${StringUtil.wrapString(description)?default("N/A")}
              </td>
              <td>
                ${orderItem.amount?string("##0.00")}
                <#--<input type="text" size="8" name="amount_o_${rowCount}" <#if orderItem.amount?has_content>value="${orderItem.amount?string("##0.00")}"</#if>/>-->
              </td>
              <td></td>
              <td>
                <select name="returnTypeId_o_${rowCount}">
                  <#list returnTypes as type>
                  <option value="${type.returnTypeId}" <#if type.returnTypeId == "RTN_REFUND">selected="selected"</#if>>${type.get("description",locale)?default(type.returnTypeId)}</option>
                  </#list>
                </select>
              </td>
              <td></td>
              <td align="right">
                <input type="checkbox" name="_rowSubmit_o_${rowCount}" value="Y" onclick="javascript:checkToggle(this, '${selectAllFormName}');highlightRow(this,'returnItemId_tableRow_${rowCount}');"/>
              </td>
            </tr>
        <#else>
            <#-- this is an order item -->
            <#assign returnItemType = (returnItemTypeMap.get(returnableItems.get(orderItem).get("itemTypeKey")))!/>
            <#-- need some order item information -->
            <#assign orderHeader = orderItem.getRelatedOne("OrderHeader", false)>
            <#assign itemCount = orderItem.quantity>
            <#assign itemPrice = orderItem.unitPrice>
            <#-- end of order item information -->

            <tr id="returnItemId_tableRow_${rowCount}" valign="middle"<#if alt_row> class="alternate-row"</#if>>
              <td>
            <input type="hidden" name="returnItemTypeId_o_${rowCount}" value="${returnItemType}"/>
            <input type="hidden" name="orderId_o_${rowCount}" value="${orderItem.orderId}"/>
            <input type="hidden" name="orderItemSeqId_o_${rowCount}" value="${orderItem.orderItemSeqId}"/>
            <input type="hidden" name="description_o_${rowCount}" value="${orderItem.itemDescription!}"/>

                <div>
                  <#if orderItem.productId??>
                    ${orderItem.productId}&nbsp;
                    <input type="hidden" name="productId_o_${rowCount}" value="${orderItem.productId}"/>
                  </#if>
                  ${orderItem.itemDescription!}
                </div>
              </td>
              <td align='center'>
                <div>${orderItem.quantity?string.number}</div>
              </td>
              <td>
                <input type="text" size="6" name="returnQuantity_o_${rowCount}" value="${returnableItems.get(orderItem).get("returnableQuantity")}"/>
              </td>
              <td align='left'>
                <div><@ofbizCurrency amount=orderItem.unitPrice isoCode=orderHeader.currencyUom/></div>
              </td>
              <td>
                <#if orderItem.productId??>
                  <#assign product = orderItem.getRelatedOne("Product", false)/>
                  <#if product.productTypeId == "ASSET_USAGE_OUT_IN">
                    <input type="text" size="8" name="returnPrice_o_${rowCount}" value="0.00"/>
                  <#else>
                    <input type="text" size="8" name="returnPrice_o_${rowCount}" value="${returnableItems.get(orderItem).get("returnablePrice")?string("##0.00")}"/>
                  </#if>
                </#if>
              </td>
              <td>
                <select name="returnReasonId_o_${rowCount}">
                  <#list returnReasons as reason>
                  <option value="${reason.returnReasonId}">${reason.get("description",locale)?default(reason.returnReasonId)}</option>
                  </#list>
                </select>
              </td>
              <td>
                <select name="returnTypeId_o_${rowCount}">
                  <#list returnTypes as type>
                  <option value="${type.returnTypeId}" <#if type.returnTypeId=="RTN_REFUND">selected="selected"</#if>>${type.get("description",locale)?default(type.returnTypeId)}</option>
                  </#list>
                </select>
              </td>
              <td>
                <select name="expectedItemStatus_o_${rowCount}">
                  <option value="INV_RETURNED">${uiLabelMap.OrderReturned}</option>
                  <option value="INV_RETURNED">---</option>
                  <#list itemStts as status>
                    <option value="${status.statusId}">${status.get("description",locale)}</option>
                  </#list>
                </select>
              </td>
              <td align="right">
                <input type="checkbox" name="_rowSubmit_o_${rowCount}" value="Y" onclick="javascript:checkToggle(this, '${selectAllFormName}');highlightRow(this,'returnItemId_tableRow_${rowCount}');"/>
              </td>
            </tr>
        </#if>
        <#assign rowCount = rowCount + 1>
        <#-- toggle the row color -->
        <#assign alt_row = !alt_row>
      </#list>
    <tr><td colspan="9"><hr/></td></tr>
    <tr>
      <td colspan="9"><h3>${uiLabelMap.OrderReturnAdjustments} ${uiLabelMap.CommonNbr}<a href="<@ofbizUrl>orderview?orderId=${orderId}</@ofbizUrl>" class="buttontext">${orderId}</a></h3></td>
    </tr>
    <tr><td colspan="9"><br /></td></tr>
    <#if orderHeaderAdjustments?has_content>
      <tr class="header-row">
        <td>${uiLabelMap.CommonDescription}</td>
        <td>${uiLabelMap.CommonAmount}</td>
        <td>${uiLabelMap.OrderReturnType}</td>
        <td align="right">${uiLabelMap.OrderOrderInclude}?</td>
      </tr>
      <#list orderHeaderAdjustments as adj>
        <#assign returnAdjustmentType = returnItemTypeMap.get(adj.get("orderAdjustmentTypeId"))/>
        <#assign adjustmentType = adj.getRelatedOne("OrderAdjustmentType", false)/>
        <#assign description = adj.description?default(adjustmentType.get("description",locale))/>

        <tr>
          <td>
        <input type="hidden" name="returnAdjustmentTypeId_o_${rowCount}" value="${returnAdjustmentType}"/>
        <input type="hidden" name="orderAdjustmentId_o_${rowCount}" value="${adj.orderAdjustmentId}"/>
        <input type="hidden" name="returnItemSeqId_o_${rowCount}" value="_NA_"/>
        <input type="hidden" name="description_o_${rowCount}" value="${description}"/>
            <div>
              ${description?default("N/A")}
            </div>
          </td>
          <td>
            <input type="text" size="8" name="amount_o_${rowCount}" <#if adj.amount?has_content>value="${adj.amount?string("##0.00")}"</#if>/>
          </td>
          <td>
            <select name="returnTypeId_o_${rowCount}">
              <#list returnTypes as type>
              <option value="${type.returnTypeId}" <#if type.returnTypeId == "RTN_REFUND">selected="selected"</#if>>${type.get("description",locale)?default(type.returnTypeId)}</option>
              </#list>
            </select>
          </td>
          <td align="right">
            <input type="checkbox" name="_rowSubmit_o_${rowCount}" value="Y" onclick="javascript:checkToggle(this, '${selectAllFormName}');"/>
          </td>
        </tr>
        <#assign rowCount = rowCount + 1>
      </#list>
    <#else>
      <tr><td colspan="9">${uiLabelMap.OrderNoOrderAdjustments}</td></tr>
    </#if>

    <#assign manualAdjRowNum = rowCount/>
    <tr>
        <td colspan="9">
    <input type="hidden" name="returnItemTypeId_o_${rowCount}" value="RET_MAN_ADJ"/>
    <input type="hidden" name="returnItemSeqId_o_${rowCount}" value="_NA_"/>
          <hr/>
        </td>
    </tr>
    <tr>
      <td colspan="9">
        <h3>${uiLabelMap.OrderReturnManualAdjustment} ${uiLabelMap.CommonNbr}<a href="<@ofbizUrl>orderview?orderId=${orderId}</@ofbizUrl>" class="buttontext">${orderId}</a></h3>
      </td>
    </tr>
    <tr>
      <td>
        <input type="text" size="30" name="description_o_${rowCount}" />
      </td>
      <td>
        <input type="text" size="8" name="amount_o_${rowCount}" value="${0.00?string("##0.00")}"/>
      </td>
      <td>
        <select name="returnTypeId_o_${rowCount}">
          <#list returnTypes as type>
          <option value="${type.returnTypeId}" <#if type.returnTypeId == "RTN_REFUND">selected="selected"</#if>>${type.get("description",locale)?default(type.returnTypeId)}</option>
          </#list>
        </select>
      </td>
      <td align="right">
        <input type="checkbox" name="_rowSubmit_o_${rowCount}" value="Y" onclick="javascript:checkToggle(this, '${selectAllFormName}');"/>
      </td>
    </tr>
    <#assign rowCount = rowCount + 1>

    <!-- final row count -->
    <tr>
      <td colspan="9" align="right">
    <input type="hidden" name="_rowCount" value="${rowCount}"/>
        <a href="javascript:document.${selectAllFormName}.submit()" class="buttontext">${uiLabelMap.OrderReturnSelectedItems}</a>
      </td>
    </tr>
    <#else>
    <tr>
      <td colspan="9" class="label">${uiLabelMap.OrderReturnNoReturnableItems} #${orderId}</td>
    </tr>
    </#if>
    <tr>
      <td colspan="3" class="tooltip">*${uiLabelMap.OrderReturnPriceNotIncludeTax}</td>
      <td colspan="6">&nbsp;</td>
    </tr>
</table>
