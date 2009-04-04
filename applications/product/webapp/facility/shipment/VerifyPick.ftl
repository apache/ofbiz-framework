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
        <li class="h3">${uiLabelMap.ProductVerify}&nbsp;${uiLabelMap.OrderOrder}&nbsp;${uiLabelMap.CommonIn}&nbsp;${facility.facilityName?if_exists} [${facility.facilityId?if_exists}]</li>
      </ul>
      <br class="clear"/>
    </div>
    <#if shipmentId?has_content>
      <#assign showInput = "N">
    </#if>
    <#if shipmentId?has_content>
      <div>
        ${uiLabelMap.ProductShipmentId} <a href="<@ofbizUrl>/ViewShipment?shipmentId=${shipmentId}</@ofbizUrl>" class="buttontext">${shipmentId}</a>
      </div>
      <#if invoiceIds?exists && invoiceIds?has_content>
        <div>
          <p>${uiLabelMap.AccountingInvoices}:</p>
          <ul>
            <#list invoiceIds as invoiceId>
              <li>
                #<a href="/accounting/control/invoiceOverview?invoiceId=${invoiceId}&externalLoginKey=${externalLoginKey}" target="_blank" class="buttontext">${invoiceId}</a>
                (<a href="/accounting/control/invoice.pdf?invoiceId=${invoiceId}&externalLoginKey=${externalLoginKey}" target="_blank" class="buttontext">PDF</a>)
              </li>
            </#list>
          </ul>
        </div>
      </#if>
    </#if>
    <br/>
    <div class="screenlet-body">
      <form name="selectOrderForm" method="post" action="<@ofbizUrl>VerifyPick</@ofbizUrl>">
        <input type="hidden" name="facilityId" value="${facility.facilityId?if_exists}"/>
        <table cellspacing="0" class="basic-table">
          <tr>
            <td width="25%" align="right"><span class="label">${uiLabelMap.ProductOrderId}</span></td>
            <td width="1">&nbsp;</td>
            <td width="25%">
              <#if shipmentId?has_content>
                <input type="text" name="orderId" size="20" maxlength="20" value=""/>
              <#else>
                <input type="text" name="orderId" size="20" maxlength="20" value="${orderId?if_exists}"/>
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
      <br/>
      <!-- select picklist bin form -->
      <form name="selectPicklistBinForm" method="post" action="<@ofbizUrl>VerifyPick</@ofbizUrl>" style="margin: 0;">
        <input type="hidden" name="facilityId" value="${facility.facilityId?if_exists}"/>
        <table cellspacing="0" class="basic-table">
          <tr>
            <td width="25%" align='right'><span class="label">${uiLabelMap.FormFieldTitle_picklistBinId}</span></td>
            <td width="1">&nbsp;</td>
            <td width="25%">
              <input type="text" name="picklistBinId" size="29" maxlength="60" value="${picklistBinId?if_exists}"/>
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
        <input type="hidden" name="orderId" value="${orderId?if_exists}"/>
        <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId?if_exists}"/>
        <input type="hidden" name="facilityId" value="${facility.facilityId?if_exists}"/>
      </form>
    </div>
  </div>
  <#if showInput != "N" && orderHeader?exists && orderHeader?has_content>
    <div class="screenlet">
      <div class="screenlet-title-bar">
        <ul>
          <li class="h3">${uiLabelMap.ProductOrderId} #<a href="/ordermgr/control/orderview?orderId=${orderId}">${orderId}</a> / ${uiLabelMap.ProductOrderShipGroupId} #${shipGroupSeqId}</li>
        </ul>
        <br class="clear"/>
      </div>
      <div class="screenlet-body">
        <#if orderItemShipGroup?has_content>
          <#assign postalAddress = orderItemShipGroup.getRelatedOne("PostalAddress")>
          <#assign carrier = orderItemShipGroup.carrierPartyId?default("N/A")>
          <table cellpadding="4" cellspacing="4" class="basic-table">
            <tr>
              <td valign="top">
                <span class="label">${uiLabelMap.ProductShipToAddress}</span>
                <br/>
                ${uiLabelMap.CommonTo}: ${postalAddress.toName?default("")}
                <br/>
                <#if postalAddress.attnName?has_content>
                  ${uiLabelMap.CommonAttn}: ${postalAddress.attnName}
                  <br/>
                </#if>
                ${postalAddress.address1}
                <br/>
                <#if postalAddress.address2?has_content>
                  ${postalAddress.address2}
                  <br/>
                </#if>
                ${postalAddress.city?if_exists}, ${postalAddress.stateProvinceGeoId?if_exists} ${postalAddress.postalCode?if_exists}
                <br/>
                ${postalAddress.countryGeoId}
                <br/>
              </td>
              <td>&nbsp;</td>
              <td valign="top">
                <span class="label">${uiLabelMap.ProductCarrierShipmentMethod}</span>
                <br/>
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
                <br/>
                ${orderItemShipGroup.shippingInstructions?default("(${uiLabelMap.CommonNone})")}
              </td>
            </tr>
          </table>
        </#if>
        <hr/>
        <form name="singlePickForm" method="post" action="<@ofbizUrl>processVerifyPick</@ofbizUrl>">
          <input type="hidden" name="orderId" value="${orderId?if_exists}"/>
          <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId?if_exists}"/>
          <input type="hidden" name="facilityId" value="${facility.facilityId?if_exists}"/>
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
        <br/>
        <#assign orderItems = orderItems?if_exists>
        <form name="multiPickForm" method="post" action="<@ofbizUrl>processBulkVerifyPick</@ofbizUrl>">
          <input type="hidden" name="facilityId" value="${facility.facilityId?if_exists}"/>
          <input type="hidden" name="userLoginId" value="${userLoginId?if_exists}"/>
          <input type="hidden" name="orderId" value="${orderId?if_exists}"/>
          <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId?if_exists}"/>
          <table class="basic-table" cellspacing='0'>
            <tr class="header-row">
              <td>&nbsp;</td>
              <td>${uiLabelMap.ProductItem} #</td>
              <td>${uiLabelMap.ProductProductId}</td>
              <td>${uiLabelMap.ProductInternalName}</td>
              <td align="right">${uiLabelMap.ProductOrderedQuantity}</td>
              <td align="right">${uiLabelMap.ProductVerified}&nbsp;${uiLabelMap.CommonQuantity}</td>
              <td>&nbsp;</td>
              <td align="right">${uiLabelMap.CommonReady}&nbsp;${uiLabelMap.CommonTo}&nbsp;${uiLabelMap.ProductVerify}</td>
              <td align="center">${uiLabelMap.CommonQty}&nbsp;${uiLabelMap.CommonTo}&nbsp;${uiLabelMap.ProductVerify}</td>
            </tr>
            <#if orderItems?has_content>
              <#assign rowKey = 1>
              <#assign counter = 1>
              <#assign isShowVerifyItemButton = "false">
              <#list orderItems as orderItem>
                <#assign orderItemSeqId = orderItem.orderItemSeqId?if_exists>
                <#assign readyToVerify = verifyPickSession.getReadyToVerifyQuantity(orderId,orderItemSeqId)>
                <#assign orderItemQuantity = orderItem.getBigDecimal("quantity")>
                <#assign verifiedQuantity = 0.000000>
                <#assign shipments = delegator.findByAnd("Shipment", Static["org.ofbiz.base.util.UtilMisc"].toMap("primaryOrderId", orderItem.getString("orderId"), "statusId", "SHIPMENT_PICKED"))>
                <#if (shipments?has_content)>
                  <#list shipments as shipment>
                    <#assign orderShipments = shipment.getRelatedByAnd("OrderShipment", Static["org.ofbiz.base.util.UtilMisc"].toMap("orderId", "${orderId}", "orderItemSeqId", orderItemSeqId))>
                    <#if orderShipments?has_content>
                      <#list orderShipments as orderShipment>
                        <#assign verifiedQuantity = verifiedQuantity + orderShipment.getBigDecimal("quantity")>
                      </#list>
                    </#if>
                  </#list>
                </#if>
                <#if verifiedQuantity == orderItemQuantity>
                  <#assign counter = counter +1>
                </#if>
                <#assign orderItemQuantity = orderItemQuantity.subtract(verifiedQuantity)>
                <#assign orderProduct = orderItem.getRelatedOne("Product")?if_exists/>
                <tr>
                  <#if (orderItemQuantity.compareTo(readyToVerify) > 0) >
                    <td><input type="checkbox" name="sel_${rowKey}" value="Y" checked=""/></td>
                    <#assign isShowVerifyItemButton = "true">
                  <#else>
                    <td>&nbsp;</td>
                  </#if>
                  <td>${orderItemSeqId?if_exists}</td>
                  <td>${orderProduct.productId?default("N/A")}</td>
                  <td>
                    <a href="/catalog/control/EditProduct?productId=${orderProduct.productId?if_exists}${externalKeyParam}" class="buttontext" target="_blank">${(orderProduct.internalName)?if_exists}</a>
                  </td>
                  <td align="right">${orderItemQuantity?if_exists}</td>
                  <td align="right">${verifiedQuantity?if_exists}</td>
                  <td>&nbsp;&nbsp;&nbsp;</td>
                  <td align="right">${readyToVerify?if_exists}</td>
                  <td align="center">
                    <#if (orderItemQuantity.compareTo(readyToVerify) > 0)>
                      <#assign qtyToVerify = orderItemQuantity.subtract(readyToVerify) >
                      <input type="text" size="7" name="qty_${rowKey}" value="${qtyToVerify?if_exists}"/>
                    <#else>
                      0
                    </#if>
                  </td>
                  <input type="hidden" name="prd_${rowKey}" value="${(orderItem.productId)?if_exists}"/>
                  <input type="hidden" name="ite_${rowKey}" value="${(orderItem.orderItemSeqId)?if_exists}"/>
                </tr>
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
        <br/>
      </div>
    </div>
    <#assign orderId = orderId?if_exists >
    <#assign pickRows = verifyPickSession.getPickRows(orderId)?if_exists>
    <form name="completePickForm" method="post" action="<@ofbizUrl>completeVerifiedPick</@ofbizUrl>">
      <input type="hidden" name="orderId" value="${orderId?if_exists}"/>
      <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId?if_exists}"/>
      <input type="hidden" name="facilityId" value="${facility.facilityId?if_exists}"/>
      <input type="hidden" name="userLoginId" value="${userLoginId?if_exists}"/>
      <#if pickRows?has_content>
        <div class="screenlet">
          <div class="screenlet-title-bar">
            <ul>
              <li class="h3">${uiLabelMap.ProductVerified}&nbsp;${uiLabelMap.OrderItems} : ${pickRows.size()?if_exists}</li>
            </ul>
            <br class="clear"/>
          </div>
          <div class="screenlet-body">
            <table class="basic-table" cellspacing='0'>
              <tr class="header-row">
                <td>${uiLabelMap.ProductItem} #</td>
                <td>${uiLabelMap.ProductProductId}</td>
                <td align="right">${uiLabelMap.ProductVerify}&nbsp;${uiLabelMap.CommonQty}</td>
                <td>&nbsp;</td>
              </tr>
              <#list pickRows as pickRow>
                <#if (pickRow.getOrderId()?if_exists).equals(orderId)>
                  <tr>
                    <td>${pickRow.getOrderSeqId()?if_exists}</td>
                    <td>${pickRow.getProductId()?if_exists}</td>
                    <td align="right">${pickRow.getReadyToVerifyQty()?if_exists}</td>
                  </tr>
                </#if>
              </#list>
            </table>
            <input type="submit" value="${uiLabelMap.ProductComplete}"/>
          </div>
        </div>
      </#if>
    </form>
  </#if>
  <#if orderId?has_content>
    <script language="javascript">
      document.singlePickForm.productId.focus();
    </script>
  <#else>
    <script language="javascript">
      document.selectOrderForm.orderId.focus();
    </script>
  </#if>
  <#if shipmentId?has_content>
    <script language="javascript">
      document.selectOrderForm.orderId.focus();
    </script>
  </#if>
<#else>
  <h3>${uiLabelMap.ProductFacilityViewPermissionError}</h3>
</#if>