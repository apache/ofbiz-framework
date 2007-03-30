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

<div class="head1">${uiLabelMap.ProductReceiveReturn} <span class='head2'>${uiLabelMap.CommonInto}&nbsp;<#if facility?has_content>"${facility.facilityName?default("Not Defined")}"</#if> [${uiLabelMap.CommonId}:${facility.facilityId?if_exists}]</span></div>
<a href="<@ofbizUrl>EditFacility</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductNewFacility}]</a>

<div>&nbsp;</div>

<#-- Receiving Results -->
<#if receivedItems?has_content>
  <table width="100%" border='0' cellpadding='2' cellspacing='0'>
    <tr><td colspan="7"><div class="head3">${uiLabelMap.ProductReceiptForReturn} <a href="/ordermgr/control/returnMain?returnId=${returnHeader.returnId}${externalKeyParam?if_exists}" class="buttontext">#${returnHeader.returnId}</a></div></td></tr>
    <tr><td colspan="7"><hr class="sepbar"></td></tr>
    <tr>
      <td><div class="tableheadtext">${uiLabelMap.ProductReceipt}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.CommonDate}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.CommonReturn}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.ProductLine}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.ProductProductId}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.ProductPerUnitPrice}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.ProductReceived}</div></td>
    </tr>
    <tr><td colspan="7"><hr class="sepbar"></td></tr>
    <#list receivedItems as item>
      <tr>
        <td><div class="tabletext">${item.receiptId}</div></td>
        <td><div class="tabletext">${item.getString("datetimeReceived").toString()}</div></td>
        <td><div class="tabletext">${item.returnId}</div></td>
        <td><div class="tabletext">${item.returnItemSeqId}</div></td>
        <td><div class="tabletext">${item.productId?default("Not Found")}</div></td>
        <td><div class="tabletext">${item.unitCost?default(0)?string("##0.00")}</div></td>
        <td><div class="tabletext">${item.quantityAccepted?string.number}</div></td>
      </tr>
    </#list>
    <tr><td colspan="7"><hr class="sepbar"></td></tr>
  </table>
  <br/>
</#if>

<#-- Multi-Item Return Receiving -->
<#if returnHeader?has_content>
  <form method="post" action="<@ofbizUrl>receiveReturnedProduct</@ofbizUrl>" name='selectAllForm' style='margin: 0;'>
    <#-- general request fields -->
    <input type="hidden" name="facilityId" value="${requestParameters.facilityId?if_exists}">   
    <input type="hidden" name="returnId" value="${requestParameters.returnId?if_exists}">   
    <input type="hidden" name="_useRowSubmit" value="Y">
    <#assign now = Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().toString()>
    <#assign rowCount = 0>     
    <table width="100%" border='0' cellpadding='2' cellspacing='0'>
      <#if !returnItems?exists || returnItems?size == 0>
        <tr>
          <td colspan="2"><div class="tableheadtext">${uiLabelMap.ProductNoItemsToReceive}.</div></td>
        </tr>
      <#else>
        <tr>
          <td>
            <div class="head3">${uiLabelMap.ProductReceiveReturn} <a href="/ordermgr/control/returnMain?returnId=${returnHeader.returnId}${externalKeyParam?if_exists}" class="buttontext">#${returnHeader.returnId}</a></div>
          </td>
          <td align="right">
            <span class="tableheadtext">${uiLabelMap.ProductSelectAll}</span>&nbsp;
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
          <input type="hidden" name="facilityId_o_${rowCount}" value="${requestParameters.facilityId?if_exists}">       
          <input type="hidden" name="datetimeReceived_o_${rowCount}" value="${now}">
          <input type="hidden" name="quantityRejected_o_${rowCount}" value="0">         
          <input type="hidden" name="comments_o_${rowCount}" value="Returned Item RA# ${returnItem.returnId}">

          <#assign unitCost = Static["org.ofbiz.order.order.OrderReturnServices"].getReturnItemInitialCost(delegator, returnItem.returnId, returnItem.returnItemSeqId)/>
          <tr>
            <td colspan="2"><hr class="sepbar"></td>
          </tr>                 
          <tr>
            <td>
              <table width="100%" border='0' cellpadding='2' cellspacing='0'>
                <tr>
                  <#assign productId = "">
                  <#if orderItem.productId?exists>
                    <#assign product = orderItem.getRelatedOne("Product")>
                    <#assign productId = product.productId>
                    <#assign serializedInv = product.getRelatedByAnd("InventoryItem", Static["org.ofbiz.base.util.UtilMisc"].toMap("inventoryItemTypeId", "SERIALIZED_INV_ITEM"))>
                    <input type="hidden" name="productId_o_${rowCount}" value="${product.productId}">                      
                    <td width="45%">
                      <div class="tabletext">
                        ${returnItem.returnItemSeqId}:&nbsp;<a href="/catalog/control/EditProduct?productId=${product.productId}${externalKeyParam?if_exists}" target="catalog" class="buttontext">${product.productId}&nbsp;-&nbsp;${product.internalName?if_exists}</a> : ${product.description?if_exists}
                        <#if serializedInv?has_content><font color='red'>**${uiLabelMap.ProductSerializedInventoryFound}**</font></#if>
                      </div>                       
                    </td>
                  <#elseif orderItem?has_content>
                    <td width="45%">
                      <div class="tabletext">
                        ${returnItem.returnItemSeqId}:&nbsp;<b>${orderItemType.get("description",locale)}</b> : ${orderItem.itemDescription?if_exists}&nbsp;&nbsp;
                        <input type="text" class="inputBox" size="12" name="productId_o_${rowCount}">
                        <a href="/catalog/control/EditProduct?externalLoginKey=${externalLoginKey}" target="catalog" class="buttontext">${uiLabelMap.ProductCreateProduct}</a>
                      </div>
                    </td>
                  <#else>
                    <td width="45%">
                      <div class="tabletext">
                        ${returnItem.returnItemSeqId}:&nbsp;${returnItem.get("description",locale)?if_exists}
                      </div>
                    </td>
                  </#if>
                  <td>&nbsp;</td>

                  <#-- location(s) -->
                  <td align="right">
                    <div class="tableheadtext">${uiLabelMap.ProductLocation}:</div>
                  </td>                  
                  <td align="right">
                    <#assign facilityLocations = (product.getRelatedByAnd("ProductFacilityLocation", Static["org.ofbiz.base.util.UtilMisc"].toMap("facilityId", facilityId)))?if_exists>
                    <#if facilityLocations?has_content>
                      <select name="locationSeqId_o_${rowCount}" class="selectbox">
                        <#list facilityLocations as productFacilityLocation>
                          <#assign facility = productFacilityLocation.getRelatedOneCache("Facility")>
                          <#assign facilityLocation = productFacilityLocation.getRelatedOne("FacilityLocation")?if_exists>
                          <#assign facilityLocationTypeEnum = (facilityLocation.getRelatedOneCache("TypeEnumeration"))?if_exists>
                          <option value="${productFacilityLocation.locationSeqId}"><#if facilityLocation?exists>${facilityLocation.areaId?if_exists}:${facilityLocation.aisleId?if_exists}:${facilityLocation.sectionId?if_exists}:${facilityLocation.levelId?if_exists}:${facilityLocation.positionId?if_exists}</#if><#if facilityLocationTypeEnum?exists>(${facilityLocationTypeEnum.get("description",locale)})</#if>[${productFacilityLocation.locationSeqId}]</option>
                        </#list>
                        <option value="">${uiLabelMap.ProductNoLocation}</option>
                      </select>
                    <#else>
                      <input type="text" class="inputBox" name="locationSeqId_o_${rowCount}" size="12"/>
                      <span class="tabletext">
                          <a href="javascript:call_fieldlookup2(document.selectAllForm.locationSeqId_o_${rowCount},'LookupFacilityLocation<#if parameters.facilityId?exists>?facilityId=${facilityId}</#if>');">
                              <img src="<@ofbizContentUrl>/images/fieldlookup.gif"</@ofbizContentUrl>" width="15" height="14" border="0" alt="Click here For Field Lookup"/>
                          </a>
                      </span>
                    </#if>
                  </td>
                  
                  <td align="right" nowrap>
                    <div class="tableheadtext">${uiLabelMap.ProductQtyReceived}:</div>
                  </td>
                  <td align="right">                    
                    <input type="text" class="inputBox" name="quantityAccepted_o_${rowCount}" size="6" value="${returnItem.returnQuantity?string.number}">
                  </td>                                                      
                </tr>
                <tr>
                   <td width='10%'>
                      <select name="inventoryItemTypeId_o_${rowCount}" size="1" class="selectBox">  
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
                    <span class="tableheadtext">${uiLabelMap.ProductInitialInventoryItemStatus}:</span>&nbsp;&nbsp;
                    <select name="statusId_o_${rowCount}" size='1' class="selectBox">
                      <option value="INV_RETURNED">${uiLabelMap.ProductReturned}</option>
                      <option value="INV_AVAILABLE">${uiLabelMap.ProductAvailable}</option>
                      <option value="INV_DEFECTIVE" <#if returnItem.returnReasonId?default("") == "RTN_DEFECTIVE_ITEM">Selected</#if>>${uiLabelMap.ProductDefective}</option>  
                    </select>                    
                  </td>
                  <#if serializedInv?has_content>                   
                    <td align="right">
                      <div class="tableheadtext">${uiLabelMap.ProductExistingInventoryItem}:</div>                    
                    </td>
                    <td align="right">
                      <select name="inventoryItemId_o_${rowCount}" class="selectBox">
                        <#list serializedInv as inventoryItem>
                          <option>${inventoryItem.inventoryItemId}</option>
                        </#list>
                      </select>
                    </td>
                  <#else>
                    <td colspan="2">&nbsp;</td>
                  </#if>
                  <td align="right" nowrap>
                    <div class="tableheadtext">${uiLabelMap.ProductPerUnitPrice}:</div>
                  </td>
                  <td align="right">
                    <input type='text' name='unitCost_o_${rowCount}' size='6' value='${unitCost?default(0)?string("##0.00")}' class="inputBox">
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
            <hr class="sepbar">
          </td>
        </tr>
        <#if rowCount == 0>
          <tr>
            <td colspan="2">
              <div class="tabletext">${uiLabelMap.ProductNoItemsReturn} #${returnHeader.returnId} ${uiLabelMap.ProductToReceive}.</div>
            </td>
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
  <form name="selectAllForm" method="post" action="<@ofbizUrl>ReceiveReturn</@ofbizUrl>" style='margin: 0;'>
    <input type="hidden" name="facilityId" value="${requestParameters.facilityId?if_exists}">
    <input type="hidden" name="initialSelected" value="Y">
    <table border='0' cellpadding='2' cellspacing='0'>
      <tr><td colspan="4"><div class="head3">${uiLabelMap.ProductReceiveReturn}</div></td></tr>
      <tr>        
        <td width="15%" align='right'><div class="tabletext">${uiLabelMap.ProductReturnNumber}</div></td>
        <td>&nbsp;</td>
        <td width="90%">
          <input type="text" class="inputBox" name="returnId" size="20" maxlength="20" value="${requestParameters.returnId?if_exists}">          
        </td> 
        <td><div class='tabletext'>&nbsp;</div></td>
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
