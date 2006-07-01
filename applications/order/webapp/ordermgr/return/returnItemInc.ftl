<#--
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@version    $Rev$
 *@since      3.5
-->


          <table border="0" width="100%" cellpadding="2" cellspacing="0">
            <tr>
              <td colspan="8"><div class="head3">${uiLabelMap.OrderReturnFromOrder} #<a href="<@ofbizUrl>orderview?orderId=${orderId}</@ofbizUrl>" class="buttontext">${orderId}</div></td>
              <td align="right">
                <span class="tableheadtext">${uiLabelMap.CommonSelectAll}</span>&nbsp;
                <input type="checkbox" name="selectAll" value="Y" onclick="javascript:toggleAll(this, '${selectAllFormName}');"/>
              </td>
            </tr>
            <tr>
              <td><div class="tableheadtext">${uiLabelMap.CommonDescription}</div></td>
              <td><div class="tableheadtext">${uiLabelMap.OrderOrderQty}</div></td>
              <td><div class="tableheadtext">${uiLabelMap.OrderReturnQty}</div></td>
              <td><div class="tableheadtext">${uiLabelMap.OrderUnitPrice}</div></td>
              <td><div class="tableheadtext">${uiLabelMap.OrderReturnPrice}*</div></td>
              <td><div class="tableheadtext">${uiLabelMap.OrderReturnReason}</div></td>
              <td><div class="tableheadtext">${uiLabelMap.OrderReturnType}</div></td>
              <td><div class="tableheadtext">${uiLabelMap.OrderItemStatus}</div></td>
              <td align="right"><div class="tableheadtext">${uiLabelMap.OrderOrderInclude}?</div></td>
            </tr>
            <tr><td colspan="9"><hr class="sepbar"></td></tr>
            <#if returnableItems?has_content>
              <#assign rowCount = 0>
              <#list returnableItems.keySet() as orderItem>
                <#assign returnItemType = returnItemTypeMap.get(returnableItems.get(orderItem).get("itemTypeKey"))/>
                <input type="hidden" name="returnItemTypeId_o_${rowCount}" value="${returnItemType}"/>
                <input type="hidden" name="orderId_o_${rowCount}" value="${orderItem.orderId}"/>
                <input type="hidden" name="orderItemSeqId_o_${rowCount}" value="${orderItem.orderItemSeqId}"/>
                <input type="hidden" name="description_o_${rowCount}" value="${orderItem.itemDescription?if_exists}"/>

                <#-- need some order item information -->
                <#assign orderHeader = orderItem.getRelatedOne("OrderHeader")>
                <#assign itemCount = orderItem.quantity>
                <#assign itemPrice = orderItem.unitPrice>
                <#-- end of order item information -->

                <tr>
                  <td>
                    <div class="tabletext">
                      <#if orderItem.productId?exists>
                      <b>${orderItem.productId}</b>:&nbsp;
                      <input type="hidden" name="productId_o_${rowCount}" value="${orderItem.productId}">
                      </#if>
                      ${orderItem.itemDescription}
                    </div>
                  </td>
                  <td align='center'>
                    <div class="tabletext">${orderItem.quantity?string.number}</div>
                  </td>
                  <td>
                    <input type="text" class="inputBox" size="6" name="returnQuantity_o_${rowCount}" value="${returnableItems.get(orderItem).get("returnableQuantity")}"/>
                  </td>
                  <td align='left'>
                    <div class="tabletext"><@ofbizCurrency amount=orderItem.unitPrice isoCode=orderHeader.currencyUom/></div>
                  </td>
                  <td>
                    <input type="text" class="inputBox" size="8" name="returnPrice_o_${rowCount}" value="${returnableItems.get(orderItem).get("returnablePrice")?string("##0.00")}"/>
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
                  <td>
                    <select name="expectedItemStatus_o_${rowCount}" class="selectBox">
                      <option value="INV_RETURNED">${uiLabelMap.OrderReturned}</option>
                      <option value="INV_RETURNED">---</option>
                      <#list itemStts as status>
                        <option value="${status.statusId}">${status.get("description",locale)}</option>
                      </#list>
                    </select>
                  </td>
                  <td align="right">
                    <input type="checkbox" name="_rowSubmit_o_${rowCount}" value="Y" onclick="javascript:checkToggle(this, '${selectAllFormName}');"/>
                  </td>
                </tr>
                <#assign rowCount = rowCount + 1>
              </#list>
                     

             <tr><td colspan="9"><hr class="sepbar"></td></tr>
            <tr>
              <td colspan="9"><div class="head3">${uiLabelMap.OrderReturnAdjustments} #<a href="<@ofbizUrl>orderview?orderId=${orderId}</@ofbizUrl>" class="buttontext">${orderId}</div></td>
            </tr>
            <tr><td colspan="9"><hr class="sepbar"></td></tr>
            <#if orderHeaderAdjustments?has_content>
              <tr>
                    <td><div class="tableheadtext">${uiLabelMap.CommonDescription}</div></td>
                    <td><div class="tableheadtext">${uiLabelMap.CommonAmount}</div></td>                                                
                    <td><div class="tableheadtext">${uiLabelMap.OrderReturnType}</div></td>

                <td align="right"><div class="tableheadtext">${uiLabelMap.OrderOrderInclude}?</div></td>
              </tr>
              <tr><td colspan="9"><hr class="sepbar"></td></tr>
              <#list orderHeaderAdjustments as adj>
                <#assign returnAdjustmentType = returnItemTypeMap.get(adj.get("orderAdjustmentTypeId"))/>
                <#assign adjustmentType = adj.getRelatedOne("OrderAdjustmentType")/>
                <#assign description = adj.description?default(adjustmentType.get("description",locale))/>

                <input type="hidden" name="returnAdjustmentTypeId_o_${rowCount}" value="${returnAdjustmentType}"/>                
                <input type="hidden" name="orderAdjustmentId_o_${rowCount}" value="${adj.orderAdjustmentId}"/>
                <input type="hidden" name="returnItemSeqId_o_${rowCount}" value="_NA_"/>
                <input type="hidden" name="description_o_${rowCount}" value="${description}"/>
                <tr>
                  <td>
                    <div class="tabletext">
                      ${description?default("N/A")}
                    </div>
                  </td>                                     
                  <td>
                    <input type="text" class="inputBox" size="8" name="amount_o_${rowCount}" value="${adj.amount?string("##0.00")}"/>
                  </td>
                  <td>
                    <select name="returnTypeId_o_${rowCount}" class="selectBox">
                      <#list returnTypes as type>
                      <option value="${type.returnTypeId}">${type.get("description",locale)?default(type.returnTypeId)}</option>
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
              <tr><td colspan="9"><div class="tableheadtext">${uiLabelMap.OrderNoOrderAdjustments}</div></td></tr>
            </#if>

            <#assign manualAdjRowNum = rowCount/>
            <input type="hidden" name="returnItemTypeId_o_${rowCount}" value="RET_MAN_ADJ"/>            
            <tr><td colspan="9"><hr class="sepbar"></td></tr>
            <tr>
              <td colspan="9">
                <div class="head3">${uiLabelMap.OrderReturnManualAdjustment} #<a href="<@ofbizUrl>orderview?orderId=${orderId}</@ofbizUrl>" class="buttontext">${orderId}</div></td></div>
              </td>
            </tr>
            <tr>
              <td>
                <input type="text" class="inputBox" size="30" name="description_o_${rowCount}">
              </td>
              <td>
                <input type="text" class="inputBox" size="8" name="amount_o_${rowCount}" value="${0.00?string("##0.00")}"/>
              </td>
              <td align="right">
                <input type="checkbox" name="_rowSubmit_o_${rowCount}" value="Y" onclick="javascript:checkToggle(this, '${selectAllFormName}');"/>
              </td>
            </tr>
            <#assign rowCount = rowCount + 1>

            <!-- final row count -->
            <input type="hidden" name="_rowCount" value="${rowCount}"/>
        
             <tr>
               <td colspan="9" align="right">
                 <a href="javascript:document.${selectAllFormName}.submit()" class="buttontext">${uiLabelMap.OrderReturnSelectedItems}</a>                 
               </td>
             </tr>
           <#else>
             <tr><td colspan="9"><div class="tabletext">${uiLabelMap.OrderReturnNoReturnableItems} #${orderId}</div></td></tr>
           </#if>
           <tr>
             <td colspan="9"><div class="tabletext">*${uiLabelMap.OrderReturnPriceNotIncludeTax}</div></td>
           </tr>
           </table>

