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

<#if security.hasEntityPermission("FACILITY", "_VIEW", session)>
  <#assign showInput = "Y">
  <div class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <li class="h3">${uiLabelMap.ProductVerify}&nbsp;${uiLabelMap.OrderOrder}&nbsp;${uiLabelMap.CommonIn}&nbsp;${facility.facilityName!} [${facility.facilityId!}]</li>
      </ul>
      <br class="clear"/>
    </div>
    <#if (shipmentId?has_content) || (isOrderStatusApproved == false)>
      <#assign showInput = "N">
    </#if>
    <#if shipmentId?has_content>
      <div>
        <span class="label">${uiLabelMap.ProductShipmentId}</span><a href="<@ofbizUrl>/ViewShipment?shipmentId=${shipmentId}</@ofbizUrl>" class="buttontext">${shipmentId}</a>
      </div>
      <#if invoiceIds?? && invoiceIds?has_content>
        <div>
          <span class="label">${uiLabelMap.AccountingInvoices}:</span>
          <ul>
            <#list invoiceIds as invoiceId>
              <li>
                ${uiLabelMap.CommonNbr}<a href="/accounting/control/invoiceOverview?invoiceId=${invoiceId}${StringUtil.wrapString(externalKeyParam)}" target="_blank" class="buttontext">${invoiceId}</a>
                (<a href="/accounting/control/invoice.pdf?invoiceId=${invoiceId}${StringUtil.wrapString(externalKeyParam)}" target="_blank" class="buttontext">PDF</a>)
              </li>
            </#list>
          </ul>
        </div>
      </#if>
    </#if>
    <br />
    <div class="screenlet-body">
      <form name="selectOrderForm" method="post" action="<@ofbizUrl>VerifyPick</@ofbizUrl>">
        <input type="hidden" name="facilityId" value="${facility.facilityId!}"/>
        <table cellspacing="0" class="basic-table">
          <tr>
            <td width="25%" align="right"><span class="label">${uiLabelMap.ProductOrderId}</span></td>
            <td width="1">&nbsp;</td>
            <td width="25%">
              <#if shipmentId?has_content>
                <input type="text" name="orderId" size="20" maxlength="20" value=""/>
              <#else>
                <input type="text" name="orderId" size="20" maxlength="20" value="${orderId!}"/>
              </#if>
              /
              <input type="text" name="shipGroupSeqId" size="6" maxlength="6" value="${shipGroupSeqId?default("00001")}"/>
            </td>
            <td>&nbsp;</td>
          </tr>
          <tr>
            <td colspan="2">&nbsp;</td>
            <td colspan="2">
              <input type="submit" value="${uiLabelMap.ProductVerify}&nbsp;${uiLabelMap.OrderOrder}"/>
            </td>
          </tr>
        </table>
      </form>
      <br />
      <!-- select picklist bin form -->
      <form name="selectPicklistBinForm" method="post" action="<@ofbizUrl>VerifyPick</@ofbizUrl>" style="margin: 0;">
        <input type="hidden" name="facilityId" value="${facility.facilityId!}"/>
        <table cellspacing="0" class="basic-table">
          <tr>
            <td width="25%" align='right'><span class="label">${uiLabelMap.FormFieldTitle_picklistBinId}</span></td>
            <td width="1">&nbsp;</td>
            <td width="25%">
              <input type="text" name="picklistBinId" size="29" maxlength="60" value="${picklistBinId!}"/>
            </td>
            <td>&nbsp;</td>
          </tr>
          <tr>
            <td colspan="2">&nbsp;</td>
            <td colspan="1">
              <input type="submit" value="${uiLabelMap.ProductVerify}&nbsp;${uiLabelMap.OrderOrder}"/>
            </td>
          </tr>
        </table>
      </form>
      <form name="clearPickForm" method="post" action="<@ofbizUrl>cancelAllRows</@ofbizUrl>">
        <input type="hidden" name="orderId" value="${orderId!}"/>
        <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId!}"/>
        <input type="hidden" name="facilityId" value="${facility.facilityId!}"/>
      </form>
    </div>
  </div>
  <#if showInput != "N" && orderHeader?? && orderHeader?has_content>
    <div class="screenlet">
      <div class="screenlet-title-bar">
        <ul>
          <li class="h3">${uiLabelMap.ProductOrderId} ${uiLabelMap.CommonNbr}<a href="/ordermgr/control/orderview?orderId=${orderId}">${orderId}</a> / ${uiLabelMap.ProductOrderShipGroupId} #${shipGroupSeqId}</li>
        </ul>
        <br class="clear"/>
      </div>
      <div class="screenlet-body">
        <#if orderItemShipGroup?has_content>
          <#assign postalAddress = orderItemShipGroup.getRelatedOne("PostalAddress", false)>
          <#assign carrier = orderItemShipGroup.carrierPartyId?default("N/A")>
          <table cellpadding="4" cellspacing="4" class="basic-table">
            <tr>
              <td valign="top">
                <span class="label">${uiLabelMap.ProductShipToAddress}</span>
                <br />
                ${uiLabelMap.CommonTo}: ${postalAddress.toName?default("")}
                <br />
                <#if postalAddress.attnName?has_content>
                  ${uiLabelMap.CommonAttn}: ${postalAddress.attnName}
                  <br />
                </#if>
                ${postalAddress.address1}
                <br />
                <#if postalAddress.address2?has_content>
                  ${postalAddress.address2}
                  <br />
                </#if>
                ${postalAddress.city!}, ${postalAddress.stateProvinceGeoId!} ${postalAddress.postalCode!}
                <br />
                ${postalAddress.countryGeoId}
                <br />
              </td>
              <td>&nbsp;</td>
              <td valign="top">
                <span class="label">${uiLabelMap.ProductCarrierShipmentMethod}</span>
                <br />
                <#if carrier == "USPS">
                  <#assign color = "red">
                <#elseif carrier == "UPS">
                  <#assign color = "green">
                <#else>
                  <#assign color = "black">
                </#if>
                <#if carrier != "_NA_">
                  <font color="${color}">${carrier}</font>
                  &nbsp;
                </#if>
                ${orderItemShipGroup.shipmentMethodTypeId?default("??")}
              </td>
              <td>&nbsp;</td>
              <td valign="top">
                <span class="label">${uiLabelMap.OrderInstructions}</span>
                <br />
                ${orderItemShipGroup.shippingInstructions?default("(${uiLabelMap.CommonNone})")}
              </td>
            </tr>
          </table>
        </#if>
        <hr />
        <form name="singlePickForm" method="post" action="<@ofbizUrl>processVerifyPick</@ofbizUrl>">
          <input type="hidden" name="orderId" value="${orderId!}"/>
          <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId!}"/>
          <input type="hidden" name="facilityId" value="${facility.facilityId!}"/>
          <table cellpadding="2" cellspacing="0" class="basic-table">
            <tr>
              <td>
                <div>
                  <span class="label">${uiLabelMap.ProductProductNumber}</span>
                  <input type="text" name="productId" size="20" maxlength="20" value=""/>
                  @
                  <input type="text" name="quantity" size="6" maxlength="6" value="1"/>
                  <input type="submit" value="${uiLabelMap.ProductVerify}&nbsp;${uiLabelMap.OrderItem}"/>
                </div>
              </td>
            </tr>
          </table>
        </form>
        <br />
        <#assign orderItems = orderItems!>
        <form name="multiPickForm" method="post" action="<@ofbizUrl>processBulkVerifyPick</@ofbizUrl>">
          <input type="hidden" name="facilityId" value="${facility.facilityId!}"/>
          <input type="hidden" name="userLoginId" value="${userLoginId!}"/>
          <input type="hidden" name="orderId" value="${orderId!}"/>
          <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId!}"/>
          <table class="basic-table" cellspacing='0'>
            <tr class="header-row">
              <td>&nbsp;</td>
              <td>${uiLabelMap.ProductItem} ${uiLabelMap.CommonNbr}</td>
              <td>${uiLabelMap.ProductProductId}</td>
              <td>${uiLabelMap.ProductInternalName}</td>
              <td>${uiLabelMap.ProductCountryOfOrigin}</td>
              <td align="right">${uiLabelMap.ProductOrderedQuantity}</td>
              <td align="right">${uiLabelMap.ProductVerified}&nbsp;${uiLabelMap.CommonQuantity}</td>
              <td align="center">${uiLabelMap.CommonQty}&nbsp;${uiLabelMap.CommonTo}&nbsp;${uiLabelMap.ProductVerify}</td>
            </tr>
            <#if orderItems?has_content>
              <#assign rowKey = 1>
              <#assign counter = 1>
              <#assign isShowVerifyItemButton = "false">
              <#list orderItems as orderItem>
                <#assign orderItemSeqId = orderItem.orderItemSeqId!>
                <#assign readyToVerify = verifyPickSession.getReadyToVerifyQuantity(orderId,orderItemSeqId)>
                <#assign orderItemQuantity = orderItem.getBigDecimal("quantity")>
                <#assign verifiedQuantity = 0.000000>
                <#assign shipments = delegator.findByAnd("Shipment", Static["org.ofbiz.base.util.UtilMisc"].toMap("primaryOrderId", orderItem.getString("orderId"), "statusId", "SHIPMENT_PICKED"), null, false)/>
                <#if (shipments?has_content)>
                  <#list shipments as shipment>
                    <#assign itemIssuances = delegator.findByAnd("ItemIssuance", Static["org.ofbiz.base.util.UtilMisc"].toMap("shipmentId", shipment.getString("shipmentId"), "orderItemSeqId", orderItemSeqId), null, false)/>
                    <#if itemIssuances?has_content>
                      <#list itemIssuances as itemIssuance>
                        <#assign verifiedQuantity = verifiedQuantity + itemIssuance.getBigDecimal("quantity")>
                      </#list>
                    </#if>
                  </#list>
                </#if>
                <#if verifiedQuantity == orderItemQuantity>
                  <#assign counter = counter +1>
                </#if>
                <#assign orderItemQuantity = orderItemQuantity.subtract(verifiedQuantity)>
                <#assign product = orderItem.getRelatedOne("Product", false)!/>
                <tr>
                  <#if (orderItemQuantity.compareTo(readyToVerify) > 0) >
                    <td><input type="checkbox" name="sel_${rowKey}" value="Y" checked=""/></td>
                    <#assign isShowVerifyItemButton = "true">
                  <#else>
                    <td>&nbsp;</td>
                  </#if>
                  <td>${orderItemSeqId!}</td>
                  <td>${product.productId?default("N/A")}</td>
                  <td>
                    <a href="/catalog/control/EditProduct?productId=${product.productId!}${StringUtil.wrapString(externalKeyParam)}" class="buttontext" target="_blank">${(product.internalName)!}</a>
                  </td>
                  <td>
                    <select name="geo_${rowKey}">
                      <#if product.originGeoId?has_content>
                        <#assign originGeoId = product.originGeoId>
                        <#assign geo = delegator.findOne("Geo", Static["org.ofbiz.base.util.UtilMisc"].toMap("geoId", originGeoId), true)>
                        <option value="${originGeoId}">${geo.geoName!}</option>
                        <option value="${originGeoId}">---</option>
                      </#if>
                      <option value=""></option>
                      ${screens.render("component://common/widget/CommonScreens.xml#countries")}
                    </select>
                  </td>
                  <td align="right">${orderItemQuantity!}</td>
                  <td align="right">${readyToVerify!}</td>
                  <td align="center">
                    <#if (orderItemQuantity.compareTo(readyToVerify) > 0)>
                      <#assign qtyToVerify = orderItemQuantity.subtract(readyToVerify) >
                      <input type="text" size="7" name="qty_${rowKey}" value="${qtyToVerify!}"/>
                    <#else>
                      0
                    </#if>
                  </td>
                  <input type="hidden" name="prd_${rowKey}" value="${(orderItem.productId)!}"/>
                  <input type="hidden" name="ite_${rowKey}" value="${(orderItem.orderItemSeqId)!}"/>
                </tr>
                <#assign workOrderItemFulfillments = orderItem.getRelated("WorkOrderItemFulfillment", null, null, false)/>
                <#if workOrderItemFulfillments?has_content>
                  <#assign workOrderItemFulfillment = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(workOrderItemFulfillments)/>
                  <#if workOrderItemFulfillment?has_content>
                    <#assign workEffort = workOrderItemFulfillment.getRelatedOne("WorkEffort", false)/>
                    <#if workEffort?has_content>
                      <#assign workEffortTask = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(delegator.findByAnd("WorkEffort", Static["org.ofbiz.base.util.UtilMisc"].toMap("workEffortParentId", workEffort.workEffortId), null, false))/>
                      <#if workEffortTask?has_content>
                        <#assign workEffortInventoryAssigns = workEffortTask.getRelated("WorkEffortInventoryAssign", null, null, false)/>
                        <#if workEffortInventoryAssigns?has_content>
                          <tr>
                            <th colspan="8">
                              ${uiLabelMap.OrderMarketingPackageComposedBy}
                            </th>
                          </tr>
                          <tr><td colspan="8"><hr /></td></tr>
                          <#list workEffortInventoryAssigns as workEffortInventoryAssign>
                            <#assign inventoryItem = workEffortInventoryAssign.getRelatedOne("InventoryItem", false)/>
                            <#assign product = inventoryItem.getRelatedOne("Product", false)/>
                            <tr>
                              <td colspan="2"></td>
                              <td>${product.productId?default("N/A")}</td>
                              <td>${product.internalName!}</td>
                              <td></td>
                              <td align="right">${workEffortInventoryAssign.quantity!}</td>
                            </tr>
                          </#list>
                          <tr><td colspan="8"><hr /></td></tr>
                        </#if>
                      </#if>
                    </#if>
                  </#if>
                </#if>
                <#assign rowKey = rowKey + 1>
              </#list>
            </#if>
            <tr>
              <td colspan="10">&nbsp;</td>
            </tr>
            <tr>
              <td colspan="12" align="right">
                <#if isShowVerifyItemButton == "true">
                  <input type="submit" value="${uiLabelMap.ProductVerify}&nbsp;${uiLabelMap.OrderItems}"/>
                </#if>
                &nbsp;
                <#if rowKey != counter>
                  <input type="button" value="${uiLabelMap.CommonCancel}" onclick="javascript:document.clearPickForm.submit();"/>
                </#if>
              </td>
            </tr>
          </table>
        </form>
        <br />
      </div>
    </div>
    <#assign orderId = orderId! >
    <#assign pickRows = verifyPickSession.getPickRows(orderId)!>
    <form name="completePickForm" method="post" action="<@ofbizUrl>completeVerifiedPick</@ofbizUrl>">
      <input type="hidden" name="orderId" value="${orderId!}"/>
      <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId!}"/>
      <input type="hidden" name="facilityId" value="${facility.facilityId!}"/>
      <input type="hidden" name="userLoginId" value="${userLoginId!}"/>
      <#if pickRows?has_content>
        <div class="screenlet">
          <div class="screenlet-title-bar">
            <ul>
              <li class="h3">${uiLabelMap.ProductVerified}&nbsp;${uiLabelMap.OrderItems} : ${pickRows.size()!}</li>
            </ul>
            <br class="clear"/>
          </div>
          <div class="screenlet-body">
            <table class="basic-table" cellspacing='0'>
              <tr class="header-row">
                <td>${uiLabelMap.ProductItem} ${uiLabelMap.CommonNbr}</td>
                <td>${uiLabelMap.ProductProductId}</td>
                <td>${uiLabelMap.ProductInventoryItem} ${uiLabelMap.CommonNbr}</td>
                <td align="right">${uiLabelMap.ProductVerified}&nbsp;${uiLabelMap.CommonQuantity}</td>
                <td>&nbsp;</td>
              </tr>
              <#list pickRows as pickRow>
                <#if (pickRow.getOrderId()!).equals(orderId)>
                  <tr>
                    <td>${pickRow.getOrderItemSeqId()!}</td>
                    <td>${pickRow.getProductId()!}</td>
                    <td>${pickRow.getInventoryItemId()!}</td>
                    <td align="right">${pickRow.getReadyToVerifyQty()!}</td>
                  </tr>
                </#if>
              </#list>
            </table>
            <div align="right">
              <a href="javascript:document.completePickForm.submit()" class="buttontext">${uiLabelMap.ProductComplete}</a>
            </div>
          </div>
        </div>
      </#if>
    </form>
  </#if>
  <#if orderId?has_content>
    <script language="javascript" type="text/javascript">
      document.singlePickForm.productId.focus();
    </script>
  <#else>
    <script language="javascript" type="text/javascript">
      document.selectOrderForm.orderId.focus();
    </script>
  </#if>
  <#if shipmentId?has_content>
    <script language="javascript" type="text/javascript">
      document.selectOrderForm.orderId.focus();
    </script>
  </#if>
<#else>
  <h3>${uiLabelMap.ProductFacilityViewPermissionError}</h3>
</#if>
