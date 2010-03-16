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
<div class="screenlet">
    <div class="screenlet-title-bar">
        <h3>${uiLabelMap.ProductReceiveReturn} ${uiLabelMap.CommonInto} <#if facility?has_content>"${facility.facilityName?default("Not Defined")}"</#if> [${uiLabelMap.CommonId}:${facility.facilityId?if_exists}]</h3>
    </div>
    <div class="screenlet-body">
        <a href="<@ofbizUrl>EditFacility</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductNewFacility}</a>
        <#-- Receiving Results -->
        <#if receivedItems?has_content>
          <h3>${uiLabelMap.ProductReceiptForReturn}# <a href="/ordermgr/control/returnMain?returnId=${returnHeader.returnId}${externalKeyParam?if_exists}" class="buttontext">${returnHeader.returnId}</a></h3>
          <#if "RETURN_RECEIVED" == returnHeader.getString("statusId")>
            <h3>${uiLabelMap.ProductReturnCompletelyReceived}</h3>
          </#if>
          <br />
          <table cellspacing="0" class="basic-table">
            <tr class="header-row">
              <td>${uiLabelMap.ProductReceipt}</td>
              <td>${uiLabelMap.CommonDate}</td>
              <td>${uiLabelMap.CommonReturn}</td>
              <td>${uiLabelMap.ProductLine}</td>
              <td>${uiLabelMap.ProductProductId}</td>
              <td>${uiLabelMap.ProductPerUnitPrice}</td>
              <td>${uiLabelMap.ProductReceived}</td>
            </tr>
            <#list receivedItems as item>
              <tr>
                <td>${item.receiptId}</td>
                <td>${item.getString("datetimeReceived").toString()}</td>
                <td>${item.returnId}</td>
                <td>${item.returnItemSeqId}</td>
                <td>${item.productId?default("Not Found")}</td>
                <td>${item.unitCost?default(0)?string("##0.00")}</td>
                <td>${item.quantityAccepted?string.number}</td>
              </tr>
            </#list>
          </table>
          <br />
        </#if>

        <#-- Multi-Item Return Receiving -->
        <#if returnHeader?has_content>
          <form method="post" action="<@ofbizUrl>receiveReturnedProduct</@ofbizUrl>" name='selectAllForm'>
            <#-- general request fields -->
            <input type="hidden" name="facilityId" value="${requestParameters.facilityId?if_exists}">
            <input type="hidden" name="returnId" value="${requestParameters.returnId?if_exists}">
            <input type="hidden" name="_useRowSubmit" value="Y">
            <#assign now = Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().toString()>
            <#assign rowCount = 0>
            <table cellspacing="0" class="basic-table">
              <#if !returnItems?exists || returnItems?size == 0>
                <tr>
                  <td colspan="2" class="label">${uiLabelMap.ProductNoItemsToReceive}</td>
                </tr>
              <#else>
                <tr>
                  <td>
                    <h3>
                      ${uiLabelMap.ProductReceiveReturn} <a href="/ordermgr/control/returnMain?returnId=${returnHeader.returnId}${externalKeyParam?if_exists}" class="buttontext">#${returnHeader.returnId}</a>
                      <#if parameters.shipmentId?has_content>${uiLabelMap.ProductShipmentId} <a href="<@ofbizUrl>ViewShipment?shipmentId=${parameters.shipmentId}</@ofbizUrl>" class="buttontext">${parameters.shipmentId}</a></#if>
                    </h3>
                  </td>
                  <td align="right">
                    ${uiLabelMap.ProductSelectAll}&nbsp;
                    <input type="checkbox" name="selectAll" value="Y" onclick="javascript:toggleAll(this, 'selectAllForm');">
                  </td>
                </tr>

                <#list returnItems as returnItem>
                  <#assign defaultQuantity = returnItem.returnQuantity - receivedQuantities[returnItem.returnItemSeqId]?double>
                  <#assign orderItem = returnItem.getRelatedOne("OrderItem")?if_exists>
                  <#if (orderItem?has_content && 0 < defaultQuantity)>
                  <#assign orderItemType = (orderItem.getRelatedOne("OrderItemType"))?if_exists>
                  <input type="hidden" name="returnId_o_${rowCount}" value="${returnItem.returnId}">
                  <input type="hidden" name="returnItemSeqId_o_${rowCount}" value="${returnItem.returnItemSeqId}">
                  <input type="hidden" name="shipmentId_o_${rowCount}" value="${parameters.shipmentId?if_exists}">
                  <input type="hidden" name="facilityId_o_${rowCount}" value="${requestParameters.facilityId?if_exists}">
                  <input type="hidden" name="datetimeReceived_o_${rowCount}" value="${now}">
                  <input type="hidden" name="quantityRejected_o_${rowCount}" value="0">
                  <input type="hidden" name="comments_o_${rowCount}" value="Returned Item RA# ${returnItem.returnId}">

                  <#assign unitCost = Static["org.ofbiz.order.order.OrderReturnServices"].getReturnItemInitialCost(delegator, returnItem.returnId, returnItem.returnItemSeqId)/>
                  <tr>
                    <td colspan="2"><hr></td>
                  </tr>
                  <tr>
                    <td>
                      <table cellspacing="0" class="basic-table">
                        <tr>
                          <#assign productId = "">
                          <#if orderItem.productId?exists>
                            <#assign product = orderItem.getRelatedOne("Product")>
                            <#assign productId = product.productId>
                            <#assign serializedInv = product.getRelatedByAnd("InventoryItem", Static["org.ofbiz.base.util.UtilMisc"].toMap("inventoryItemTypeId", "SERIALIZED_INV_ITEM"))>
                            <input type="hidden" name="productId_o_${rowCount}" value="${product.productId}">
                            <td width="45%">
                              <div>
                                ${returnItem.returnItemSeqId}:&nbsp;<a href="/catalog/control/EditProduct?productId=${product.productId}${externalKeyParam?if_exists}" target="catalog" class="buttontext">${product.productId}&nbsp;-&nbsp;${product.internalName?if_exists}</a> : ${product.description?if_exists}
                                <#if serializedInv?has_content><font color='red'>**${uiLabelMap.ProductSerializedInventoryFound}**</font></#if>
                              </div>
                            </td>
                          <#elseif orderItem?has_content>
                            <td width="45%">
                              <div>
                                ${returnItem.returnItemSeqId}:&nbsp;<b>${orderItemType.get("description",locale)}</b> : ${orderItem.itemDescription?if_exists}&nbsp;&nbsp;
                                <input type="text" size="12" name="productId_o_${rowCount}">
                                <a href="/catalog/control/EditProduct?externalLoginKey=${externalLoginKey}" target="catalog" class="buttontext">${uiLabelMap.ProductCreateProduct}</a>
                              </div>
                            </td>
                          <#else>
                            <td width="45%">
                              <div>
                                ${returnItem.returnItemSeqId}:&nbsp;${returnItem.get("description",locale)?if_exists}
                              </div>
                            </td>
                          </#if>
                          <td>&nbsp;</td>

                          <#-- location(s) -->
                          <td align="right">
                            <div class="label">${uiLabelMap.ProductLocation}</div>
                          </td>
                          <td align="right">
                            <#assign facilityLocations = (product.getRelatedByAnd("ProductFacilityLocation", Static["org.ofbiz.base.util.UtilMisc"].toMap("facilityId", facilityId)))?if_exists>
                            <#if facilityLocations?has_content>
                              <select name="locationSeqId_o_${rowCount}">
                                <#list facilityLocations as productFacilityLocation>
                                  <#assign facility = productFacilityLocation.getRelatedOneCache("Facility")>
                                  <#assign facilityLocation = productFacilityLocation.getRelatedOne("FacilityLocation")?if_exists>
                                  <#assign facilityLocationTypeEnum = (facilityLocation.getRelatedOneCache("TypeEnumeration"))?if_exists>
                                  <option value="${productFacilityLocation.locationSeqId}"><#if facilityLocation?exists>${facilityLocation.areaId?if_exists}:${facilityLocation.aisleId?if_exists}:${facilityLocation.sectionId?if_exists}:${facilityLocation.levelId?if_exists}:${facilityLocation.positionId?if_exists}</#if><#if facilityLocationTypeEnum?exists>(${facilityLocationTypeEnum.get("description",locale)})</#if>[${productFacilityLocation.locationSeqId}]</option>
                                </#list>
                                <option value="">${uiLabelMap.ProductNoLocation}</option>
                              </select>
                            <#else>
                              <input type="text" name="locationSeqId_o_${rowCount}" size="12"/>
                              <span>
                                  <a href="javascript:call_fieldlookup2(document.selectAllForm.locationSeqId_o_${rowCount},'LookupFacilityLocation<#if parameters.facilityId?exists>?facilityId=${facilityId}</#if>');">
                                      <img src="<@ofbizContentUrl>/images/fieldlookup.gif</@ofbizContentUrl>" width="15" height="14" border="0" alt="${uiLabelMap.CommonClickHereForFieldLookup}"/>
                                  </a>
                              </span>
                            </#if>
                          </td>

                          <td align="right" nowrap class="label">${uiLabelMap.ProductQtyReceived}</td>
                          <td align="right">
                            <input type="text" name="quantityAccepted_o_${rowCount}" size="6" value="${defaultQuantity?string.number}">
                          </td>
                        </tr>
                        <tr>
                           <td width='10%'>
                              <select name="inventoryItemTypeId_o_${rowCount}" size="1">
                                 <#list inventoryItemTypes as nextInventoryItemType>
                                    <option value='${nextInventoryItemType.inventoryItemTypeId}'
                                 <#if (facility.defaultInventoryItemTypeId?has_content) && (nextInventoryItemType.inventoryItemTypeId == facility.defaultInventoryItemTypeId)>
                                    SELECTED
                                  </#if>
                                 >${nextInventoryItemType.get("description",locale)?default(nextInventoryItemType.inventoryItemTypeId)}</option>
                                 </#list>
                              </select>
                          </td>
                          <td width="35%">
                            <span class="label">${uiLabelMap.ProductInitialInventoryItemStatus}:</span>&nbsp;&nbsp;
                            <select name="statusId_o_${rowCount}" size='1'>
                              <option value="INV_RETURNED">${uiLabelMap.ProductReturned}</option>
                              <option value="INV_AVAILABLE">${uiLabelMap.ProductAvailable}</option>
                              <option value="INV_DEFECTIVE" <#if returnItem.returnReasonId?default("") == "RTN_DEFECTIVE_ITEM">Selected</#if>>${uiLabelMap.ProductDefective}</option>
                            </select>
                          </td>
                          <#if serializedInv?has_content>
                            <td align="right" class="label">${uiLabelMap.ProductExistingInventoryItem}</td>
                            <td align="right">
                              <select name="inventoryItemId_o_${rowCount}">
                                <#list serializedInv as inventoryItem>
                                  <option>${inventoryItem.inventoryItemId}</option>
                                </#list>
                              </select>
                            </td>
                          <#else>
                            <td colspan="2">&nbsp;</td>
                          </#if>
                          <td align="right" nowrap class="label">${uiLabelMap.ProductPerUnitPrice}</td>
                          <td align="right">
                            <input type='text' name='unitCost_o_${rowCount}' size='6' value='${unitCost?default(0)?string("##0.00")}'>
                          </td>
                        </tr>
                      </table>
                    </td>
                    <td align="right">
                      <input type="checkbox" name="_rowSubmit_o_${rowCount}" value="Y" onclick="javascript:checkToggle(this, 'selectAllForm');">
                    </td>
                  </tr>
                  <#assign rowCount = rowCount + 1>
                  </#if>
                </#list>
                <tr>
                  <td colspan="2">
                    <hr>
                  </td>
                </tr>
                <#if rowCount == 0>
                  <tr>
                    <td colspan="2" class="label">${uiLabelMap.ProductNoItemsReturn} #${returnHeader.returnId} ${uiLabelMap.ProductToReceive}.</td>
                  </tr>
                  <tr>
                    <td colspan="2" align="right">
                      <a href="<@ofbizUrl>ReceiveReturn?facilityId=${requestParameters.facilityId?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductReturnToReceiving}</a>
                    </td>
                  </tr>
                <#else>
                  <tr>
                    <td colspan="2" align="right">
                      <a href="javascript:document.selectAllForm.submit();" class="buttontext">${uiLabelMap.ProductReceiveSelectedProduct}</a>
                    </td>
                  </tr>
                </#if>
              </#if>
            </table>
            <input type="hidden" name="_rowCount" value="${rowCount}">
          </form>
          <script language="JavaScript" type="text/javascript">selectAll('selectAllForm');</script>

          <#-- Initial Screen -->
        <#else>
          <form name="selectAllForm" method="post" action="<@ofbizUrl>ReceiveReturn</@ofbizUrl>">
            <input type="hidden" name="facilityId" value="${requestParameters.facilityId?if_exists}">
            <input type="hidden" name="initialSelected" value="Y">
            <table cellspacing="0" class="basic-table">
              <tr><td colspan="4"><h3>${uiLabelMap.ProductReceiveReturn}</h3></td></tr>
              <tr>
                <td width="15%" align='right' class="label">${uiLabelMap.ProductReturnNumber}</td>
                <td>&nbsp;</td>
                <td width="90%">
                  <input type="text" name="returnId" size="20" maxlength="20" value="${requestParameters.returnId?if_exists}">
                </td>
                <td>&nbsp;</td>
              </tr>
              <tr>
                <td colspan="2">&nbsp;</td>
                <td colspan="2">
                  <a href="javascript:document.selectAllForm.submit();" class="buttontext">${uiLabelMap.ProductReceiveProduct}</a>
                </td>
              </tr>
            </table>
          </form>
        </#if>
    </div>
</div>