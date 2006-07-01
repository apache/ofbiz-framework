<#--
 *  Copyright (c) 2003-2005 The Open For Business Project - www.ofbiz.org
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

<#if security.hasEntityPermission("FACILITY", "_VIEW", session)>
    <#assign showInput = requestParameters.showInput?default("Y")>
    <#assign hideGrid = requestParameters.hideGrid?default("N")>    

    <#if (requestParameters.forceComplete?has_content && !shipmentId?has_content)>
        <#assign forceComplete = "true">
        <#assign showInput = "Y">
    </#if>

<div class="screenlet">
    <div class="screenlet-header">
        <div class="simple-right-half">
            <a href="<@ofbizUrl>PacklistManage?facilityId=${facilityId?if_exists}</@ofbizUrl>" class="submenutext">${uiLabelMap.ProductPacklistManage}</a>
        </div>
        <div class="boxhead">${uiLabelMap.ProductPacklistManage}</div>
    </div>
    <div class="head1">${uiLabelMap.ProductPackOrder}<span class='head2'>&nbsp;in&nbsp;${facility.facilityName?if_exists} [${uiLabelMap.CommonId}:${facilityId?if_exists}]</span></div>
    <#if shipmentId?has_content>
      <div class="tabletext">
        ${uiLabelMap.CommonView} <a href="<@ofbizUrl>/PackingSlip.pdf?shipmentId=${shipmentId}</@ofbizUrl>" target="_blank" class="buttontext">${uiLabelMap.ProductPackingSlip}</a> ${uiLabelMap.CommonOr} 
        ${uiLabelMap.CommonView} <a href="<@ofbizUrl>/ShipmentBarCode.pdf?shipmentId=${shipmentId}</@ofbizUrl>" target="_blank" class="buttontext">${uiLabelMap.ProductBarcode}</a> ${uiLabelMap.CommonFor} ${uiLabelMap.ProductShipmentId} <a href="<@ofbizUrl>/ViewShipment?shipmentId=${shipmentId}</@ofbizUrl>" class="buttontext">${shipmentId}</a>
      </div>
    </#if>
    <div>&nbsp;</div>

    <!-- select order form -->
    <form name="selectOrderForm" method="post" action="<@ofbizUrl>PackOrder</@ofbizUrl>" style='margin: 0;'>
      <input type="hidden" name="facilityId" value="${facilityId?if_exists}">
      <table border='0' cellpadding='2' cellspacing='0'>
        <tr>
          <td width="25%" align='right'><div class="tabletext">${uiLabelMap.ProductOrderId} #</div></td>
          <td width="1">&nbsp;</td>
          <td width="25%">
            <input type="text" class="inputBox" name="orderId" size="20" maxlength="20" value="${orderId?if_exists}"/>
            <span class="tabletext">/</span>
            <input type="text" class="inputBox" name="shipGroupSeqId" size="6" maxlength="6" value="${shipGroupSeqId?if_exists}"/
          </td>
          <td><div class="tabletext">${uiLabelMap.ProductHideGrid}:&nbsp;<input type="checkbox" name="hideGrid" value="Y" <#if (hideGrid == "Y")>checked=""</#if>></div></td>
          <td><div class='tabletext'>&nbsp;</div></td>
        </tr>
        <tr>
          <td colspan="2">&nbsp;</td>
          <td colspan="2">
            <input type="image" src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" onClick="javascript:document.selectOrderForm.submit();">
            <a href="javascript:document.selectOrderForm.submit();" class="buttontext">${uiLabelMap.ProductPackOrder}</a>
          </td>
        </tr>
      </table>
    </form>

    <form name="clearPackForm" method="post" action="<@ofbizUrl>ClearPackAll</@ofbizUrl>" style='margin: 0;'>
      <input type="hidden" name="orderId" value="${orderId?if_exists}"/>
      <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId?if_exists}"/>
      <input type="hidden" name="facilityId" value="${facilityId?if_exists}"/>
    </form>
    <form name="incPkgSeq" method="post" action="<@ofbizUrl>SetNextPackageSeq</@ofbizUrl>" style='margin: 0;'>
      <input type="hidden" name="orderId" value="${orderId?if_exists}"/>
      <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId?if_exists}"/>
      <input type="hidden" name="facilityId" value="${facilityId?if_exists}"/>
    </form>

    <#if showInput != "N" && orderHeader?exists && orderHeader?has_content>
      <hr class="sepbar"/>
      <div class='head2'>${uiLabelMap.ProductOrderId} #<a href="/ordermgr/control/orderview?orderId=${orderId}" class="buttontext">${orderId}</a> / ${uiLabelMap.ProductOrderShipGroupId} #${shipGroupSeqId}</div>
      <div>&nbsp;</div>
      <#if orderItemShipGroup?has_content>
        <#assign postalAddress = orderItemShipGroup.getRelatedOne("PostalAddress")>
        <#assign carrier = orderItemShipGroup.carrierPartyId?default("N/A")>
        <table border='0' cellpadding='4' cellspacing='4' width="100%">
          <tr>
            <td valign="top">
              <div class="tableheadtext">${uiLabelMap.ProductShipToAddress}:</div>
              <div class="tabletext">
                <b>${uiLabelMap.CommonTo}: </b>${postalAddress.toName?default("")}<br>
                <#if postalAddress.attnName?has_content>
                  <b>${uiLabelMap.CommonAttn}: </b>${postalAddress.attnName}<br>
                </#if>
                ${postalAddress.address1}<br>
                <#if postalAddress.address2?has_content>
                  ${postalAddress.address2}<br>
                </#if>
                ${postalAddress.city}, ${postalAddress.stateProvinceGeoId?if_exists} ${postalAddress.postalCode}<br>
                ${postalAddress.countryGeoId}
              </div>
            </td>
            <td>&nbsp;&nbsp;</td>
            <td valign="top">
              <div class="tableheadtext">${uiLabelMap.ProductCarrierShipmentMethod}:</div>
              <div class="tabletext">
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
              </div>
            </td>
            <td>&nbsp;&nbsp;</td>
            <td valign="top">
              <div class="tableheadtext">${uiLabelMap.OrderShipping} ${uiLabelMap.ProductInstruction}:</div>
              <div class="tabletext">${orderItemShipGroup.shippingInstructions?default("(none)")}</div>
            </td>
          </tr>
        </table>
        <div>&nbsp;</div>
      </#if>

      <!-- manual per item form -->
      <#if showInput != "N">
        <hr class="sepbar"/>
        <div>&nbsp;</div>
        <form name="singlePackForm" method="post" action="<@ofbizUrl>ProcessPackOrder</@ofbizUrl>" style='margin: 0;'>
          <input type="hidden" name="packageSeq" value="${packingSession.getCurrentPackageSeq()}"/>
          <input type="hidden" name="orderId" value="${orderId}"/>
          <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId}"/>
          <input type="hidden" name="facilityId" value="${facilityId?if_exists}"/>
          <input type="hidden" name="hideGrid" value="${hideGrid}"/>
          <table border='0' cellpadding='2' cellspacing='0' width="100%">
            <tr>
              <td><div class="tabletext">${uiLabelMap.ProductProduct} #</div></td>
              <td width="1">&nbsp;</td>
              <td>
                <input type="text" class="inputBox" name="productId" size="20" maxlength="20" value=""/>
                <span class="tabletext">@</span>
                <input type="text" class="inputBox" name="quantity" size="6" maxlength="6" value="1"/>
              </td>
              <td><div class='tabletext'>&nbsp;</div></td>
              <td align="right">
                <div class="tabletext">
                  ${uiLabelMap.CommonCurrent} ${uiLabelMap.ProductPackage} ${uiLabelMap.CommonSequence}: <b>${packingSession.getCurrentPackageSeq()}</b>
                  <input type="button" value="${uiLabelMap.CommonNext} ${uiLabelMap.ProductPackage}" onclick="javascript:document.incPkgSeq.submit();">
                </div>
              </td>
            </tr>
            <tr>
              <td colspan="2">&nbsp;</td>
              <td valign="top">
                <input type="image" src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" onClick="javascript:document.singlePackForm.submit();">
                <a href="javascript:document.singlePackForm.submit();" class="buttontext">${uiLabelMap.ProductPackItem}</a>
              </td>
              <td>&nbsp;</td>
            </tr>
          </table>
        </form>
        <div>&nbsp;</div>
      </#if>

      <!-- auto grid form -->
      <#if showInput != "N" && hideGrid != "Y" && itemInfos?has_content>
        <hr class="sepbar"/>
        <div>&nbsp;</div>
        <form name="multiPackForm" method="post" action="<@ofbizUrl>ProcessBulkPackOrder</@ofbizUrl>" style='margin: 0;'>
          <input type="hidden" name="facilityId" value="${facilityId?if_exists}">
          <input type="hidden" name="orderId" value="${orderId?if_exists}">
          <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId?if_exists}">
          <input type="hidden" name="originFacilityId" value="${facilityId?if_exists}">
          <input type="hidden" name="hideGrid" value="${hideGrid}"/>

          <table border='0' width="100%" cellpadding='2' cellspacing='0'>
            <tr>
              <td>&nbsp;</td>
              <td><div class="tableheadtext">${uiLabelMap.ProductItem} #</td>
              <td><div class="tableheadtext">${uiLabelMap.ProductProductId}</td>
              <td><div class="tableheadtext">${uiLabelMap.ProductDescription}</td>
              <td align="right"><div class="tableheadtext">${uiLabelMap.ProductOrderedQuantity}</td>
              <td align="right"><div class="tableheadtext">${uiLabelMap.ProductPackedQty}</td>
              <td>&nbsp;</td>
              <td align="center"><div class="tableheadtext">${uiLabelMap.ProductPackQty}</td>
              <td align="center"><div class="tableheadtext">${uiLabelMap.ProductPackage}</td>
            </tr>
            <tr>
              <td colspan="9">
                <hr class="sepbar"/>
              </td>
            </tr>

            <#list itemInfos as orderItem>
              <#if orderItem.cancelQuantity?exists>
                <#assign orderItemQuantity = orderItem.quantity - orderItem.cancelQuantity>
              <#else>
                <#assign orderItemQuantity = orderItem.quantity>
              </#if>
              <tr>
                <td><input type="checkbox" name="sel_${orderItem.orderItemSeqId}" value="Y" checked=""/></td>
                <td><div class="tabletext">${orderItem.orderItemSeqId}</td>
                <td><div class="tabletext">${orderItem.productId?default("N/A")}</td>
                <td><div class="tabletext">${orderItem.itemDescription?if_exists}</td>
                <td align="right"><div class="tabletext">${orderItemQuantity}</td>
                <td align="right"><div class="tabletext">${packingSession.getPackedQuantity(orderId, orderItem.orderItemSeqId, shipGroupSeqId)}</td>
                <td>&nbsp;&nbsp;</td>
                <td align="center">
                  <#assign inputQty = (orderItemQuantity - packingSession.getPackedQuantity(orderId, orderItem.orderItemSeqId, shipGroupSeqId))>
                  <input type="text" class="inputBox" size="7" name="qty_${orderItem.orderItemSeqId}" value="${inputQty}">
                </td>
                <td align="center">
                  <select name="pkg_${orderItem.orderItemSeqId}">
                    <option value="1">${uiLabelMap.ProductPackage} 1</option>
                    <option value="2">${uiLabelMap.ProductPackage} 2</option>
                    <option value="3">${uiLabelMap.ProductPackage} 3</option>
                    <option value="4">${uiLabelMap.ProductPackage} 4</option>
                    <option value="5">${uiLabelMap.ProductPackage} 5</option>
                  </select>
                </td>
                <input type="hidden" name="prd_${orderItem.orderItemSeqId}" value="${orderItem.productId?if_exists}">
              </tr>
            </#list>
            <tr><td colspan="9">&nbsp;</td></tr>
            <tr>
              <td colspan="9" align="right">
                <input type="submit" value="${uiLabelMap.ProductPackItem}">
                &nbsp;
                <input type="button" value="${uiLabelMap.CommonClear}" onclick="javascript:document.clearPackForm.submit();"/>
              </td>
            </tr>
          </table>
        </form>
        <div>&nbsp;</div>
      </#if>

      <!-- complete form -->
      <#if showInput != "N">
        <form name="completePackForm" method="post" action="<@ofbizUrl>CompletePack</@ofbizUrl>" style='margin: 0;'>
          <input type="hidden" name="orderId" value="${orderId?if_exists}"/>
          <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId?if_exists}"/>
          <input type="hidden" name="facilityId" value="${facilityId?if_exists}"/>
          <input type="hidden" name="forceComplete" value="${forceComplete?default('false')}"/>
          <input type="hidden" name="showInput" value="N"/>
          <hr class="sepbar">
          <div>&nbsp;</div>
          <table border='0' cellpadding='2' cellspacing='0' width="100%">
            <tr>
              <td>
                <div class="tableheadtext">${uiLabelMap.ProductHandlingInstructions}:</div>
                <div>
                  <textarea name="handlingInstructions" class="inputBox" rows="2" cols="30">${packingSession.getHandlingInstructions()?if_exists}</textarea>
                </div>
              </td>
              <td align="right">
                <div>
                  <#assign buttonName = "${uiLabelMap.ProductComplete}">
                  <#if forceComplete?default("false") == "true">
                    <#assign buttonName = "${uiLabelMap.ProductCompleteForce}">
                  </#if>
                  <input type="button" value="${buttonName}" onclick="javascript:document.completePackForm.submit();"/>
                </div>
              </td>
            </tr>
          </table>
          <div>&nbsp;</div>
        </form>
      </#if>

      <!-- packed items display -->
      <#assign packedLines = packingSession.getLines()?if_exists>
      <#if packedLines?has_content>
        <hr class="sepbar"/>
        <div>&nbsp;</div>
        <table border='0' width="100%" cellpadding='2' cellspacing='0'>
          <tr>
            <td><div class="tableheadtext">${uiLabelMap.ProductItem} #</td>
            <td><div class="tableheadtext">${uiLabelMap.ProductProductId}</td>
            <td><div class="tableheadtext">${uiLabelMap.ProductDescription}</td>
            <td><div class="tableheadtext">${uiLabelMap.ProductInventoryItem} #</td>
            <td align="right"><div class="tableheadtext">${uiLabelMap.ProductPackedQty}</td>
            <td align="right"><div class="tableheadtext">${uiLabelMap.ProductPackage} #</td>
            <td>&nbsp;</td>
          </tr>
          <tr>
            <td colspan="7">
              <hr class="sepbar"/>
            </td>
          </tr>
          <#list packedLines as line>
            <#assign orderItem = orderReadHelper.getOrderItem(line.getOrderItemSeqId())?if_exists>
            <tr>
              <td><div class="tabletext">${line.getOrderItemSeqId()}</td>
              <td><div class="tabletext">${line.getProductId()?default("N/A")}</td>
              <td><div class="tabletext">${(orderItem.itemDescription)?default("[N/A]")}</td>
              <td><div class="tabletext">${line.getInventoryItemId()}</td>
              <td align="right"><div class="tabletext">${line.getQuantity()}</td>
              <td align="right"><div class="tabletext">${line.getPackageSeq()}</td>
              <td align="right"><a href="<@ofbizUrl>ClearPackLine?facilityId=${facilityId}&orderId=${line.getOrderId()}&orderItemSeqId=${line.getOrderItemSeqId()}&shipGroupSeqId=${line.getShipGroupSeqId()}&inventoryItemId=${line.getInventoryItemId()}&packageSeqId=${line.getPackageSeq()}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonClear}</a></td>
            </tr>
          </#list>
        </table>
      </#if>
    </#if>

    <#if orderId?has_content>
      <script language="javascript">
        document.singlePackForm.productId.focus();
      </script>
    <#else>
      <script language="javascript">
        document.selectOrderForm.orderId.focus();
      </script>
    </#if>
<#else>
  <h3>${uiLabelMap.ProductFacilityViewPermissionError}</h3>
</#if>
</div>
