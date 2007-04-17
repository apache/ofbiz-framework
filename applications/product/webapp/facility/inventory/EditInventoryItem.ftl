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

<div class="head1">${uiLabelMap.ProductEditInventoryItemWithId} [${inventoryItemId?if_exists}]</div>
<a href="<@ofbizUrl>EditInventoryItem<#if facilityId?exists>?facilityId=${facilityId}</#if></@ofbizUrl>" class="buttontext">${uiLabelMap.ProductNewInventoryItem}</a>
<#if inventoryItemId?exists>
    <a href="<@ofbizUrl>TransferInventoryItem?inventoryItemId=${inventoryItemId}<#if facilityId?exists>&facilityId=${facilityId}</#if></@ofbizUrl>" class="buttontext">${uiLabelMap.ProductTransferItem}</a>
    <a href="<@ofbizUrl>ViewInventoryItemDetail?inventoryItemId=${inventoryItemId}<#if facilityId?exists>&facilityId=${facilityId}</#if></@ofbizUrl>" class="buttontext">${uiLabelMap.ProductInventoryDetails}</a>
</#if>

<#if inventoryItem?exists>
  <form action="<@ofbizUrl>UpdateInventoryItem</@ofbizUrl>" method="post" style="margin: 0;" name="inventoryItemForm">
  <table border="0" cellpadding="2" cellspacing="0">
  <input type="hidden" name="inventoryItemId" value="${inventoryItemId}">
  <tr>
    <td align="right"><div class="tabletext">${uiLabelMap.ProductInventoryItemId}</div></td>
    <td>&nbsp;</td>
    <td>
      <b>${inventoryItemId}</b> ${uiLabelMap.ProductNotModificationRecrationInventoryItem}
    </td>
  </tr>
<#else>
  <#if inventoryItemId?exists>
    <form action="<@ofbizUrl>CreateInventoryItem</@ofbizUrl>" method="post" style="margin: 0;" name="inventoryItemForm">
    <table border="0" cellpadding="2" cellspacing="0">
    <h3>${uiLabelMap.ProductNotFindInventoryItemWithId} "${inventoryItemId}".</h3>
  <#else>
    <form action="<@ofbizUrl>CreateInventoryItem</@ofbizUrl>" method="post" style="margin: 0;" name="inventoryItemForm">
    <table border="0" cellpadding="2" cellspacing="0">
  </#if>
</#if>
      <tr>
        <td align="right"><div class="tabletext">${uiLabelMap.ProductInventoryItemTypeId}</div></td>
        <td>&nbsp;</td>
        <td>
          <select name="inventoryItemTypeId" size="1" class="selectBox">
            <#if inventoryItemType?exists>
                <option selected value="${inventoryItemType.inventoryItemTypeId}">${inventoryItemType.get("description",locale)}</option>
                <option value="${inventoryItemType.inventoryItemTypeId}">----</option>
            </#if>
            <#list inventoryItemTypes as nextInventoryItemType>
              <option value="${nextInventoryItemType.inventoryItemTypeId}">${nextInventoryItemType.get("description",locale)}</option>
            </#list>
          </select>
        </td>
      </tr>
      <tr>
        <td align="right"><div class="tabletext">${uiLabelMap.ProductProductId}</div></td>
        <td>&nbsp;</td>
        <td>
            <input type="text" name="productId" value="${inventoryItemData.productId?if_exists}" size="20" maxlength="20" class="inputBox">
            <#if (inventoryItem.productId)?has_content>
                <a href="/catalog/control/EditProduct?productId=${inventoryItem.productId}&externalLoginKey=${externalLoginKey?if_exists}" class="buttontext">${uiLabelMap.CommonEdit}&nbsp;${uiLabelMap.ProductProduct}&nbsp;${inventoryItem.productId}</a>
            <#else>
                <a href="javascript:call_fieldlookup2(document.inventoryItemForm.productId,'LookupProduct');"><img src="<@ofbizContentUrl>/images/fieldlookup.gif"</@ofbizContentUrl>" width="16" height="16" border="0" alt="Lookup"/></a>
            </#if>
        </td>
      </tr>
      <tr>
        <td align="right"><div class="tabletext">${uiLabelMap.PartyPartyId}</div></td>
        <td>&nbsp;</td>
        <td>
            <input type="text" name="partyId" value="${inventoryItemData.partyId?if_exists}" size="20" maxlength="20" class="inputBox">
            <a href="javascript:call_fieldlookup2(document.inventoryItemForm.partyId, 'LookupPartyName');"><img src="<@ofbizContentUrl>/images/fieldlookup.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Lookup"/></a>            
        </td>
      </tr>
      <tr>
        <td align="right"><div class="tabletext">${uiLabelMap.ProductFacilityOwner}</div></td>
        <td>&nbsp;</td>
        <td>
            <input type="text" name="ownerPartyId" value="${inventoryItemData.ownerPartyId?if_exists}" size="20" maxlength="20" class="inputBox">
            <a href="javascript:call_fieldlookup2(document.inventoryItemForm.ownerPartyId, 'LookupPartyName');"><img src="<@ofbizContentUrl>/images/fieldlookup.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Lookup"/></a>            
        </td>
      </tr>
      <#if "SERIALIZED_INV_ITEM" == (inventoryItem.inventoryItemTypeId)?if_exists>
          <tr>
            <td align="right"><div class="tabletext">${uiLabelMap.ProductStatus}</div></td>
            <td>&nbsp;</td>
            <td>
              <select name="statusId" class="selectBox">
                  <#if (inventoryItem.statusId)?has_content>
                      <option value="${inventoryItem.statusId}">${(curStatusItem.get("description",locale))?default("[" + inventoryItem.statusId + "]")}</option>
                      <option value="${inventoryItem.statusId}">----</option>
                  </#if>
                  <#if !tryEntity && requestParameters.statusId?has_content>
                      <#assign selectedStatusId = requestParameters.statusId>
                  </#if>
                  <#list statusItems as statusItem>
                      <option value="${statusItem.statusId}"<#if selectedStatusId?if_exists == statusItem.statusId>${uiLabelMap.ProductSelected}</#if>>${statusItem.get("description",locale)}</option>
                  </#list>
              </select>
            </td>
          </tr>
      </#if>
      <tr>
        <td align="right"><div class="tabletext">${uiLabelMap.ProductDateReceived}</div></td>
        <td>&nbsp;</td>
        <td>
            <input type="text" size="25" name="datetimeReceived" value="${(inventoryItemData.datetimeReceived.toString())?if_exists}" class="inputBox">
            <a href="javascript:call_cal(document.inventoryItemForm.datetimeReceived, '${(inventoryItemData.datetimeReceived.toString())?default(nowTimestampString)}');"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Calendar'></a>
        </td>
      </tr>
      <tr>
        <td align="right"><div class="tabletext">${uiLabelMap.ProductExpireDate}</div></td>
        <td>&nbsp;</td>
        <td>
            <input type="text" size="25" name="expireDate" value="${(inventoryItemData.expireDate.toString())?if_exists}" class="inputBox">
            <a href="javascript:call_cal(document.inventoryItemForm.expireDate, '${(inventoryItemData.expireDate.toString())?default(nowTimestampString)}');"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Calendar'></a>
        </td>
      </tr>
      <tr>
        <td align="right"><div class="tabletext">${uiLabelMap.ProductFacilityContainer}</div></td>
        <td>&nbsp;</td>
        <td>
            <span class="tabletext">${uiLabelMap.ProductSelectFacility} : </span>
            <select name="facilityId" class="selectBox">
              <#if inventoryItem?exists>
                  <option value="${inventoryItem.facilityId}">${(facility.facilityName)?if_exists} [${inventoryItem.facilityId}]</option>
                  <option value="${inventoryItem.facilityId}">----</option>
              </#if>
              <#if !tryEntity && requestParameters.facilityId?has_content>
                  <#assign selectedFacilityId = requestParameters.facilityId>
              </#if>
              <#list facilities as nextFacility>
                <option value="${nextFacility.facilityId}"<#if selectedFacilityId?if_exists == nextFacility.facilityId> ${uiLabelMap.ProductSelected}</#if>>${nextFacility.facilityName?if_exists} [${nextFacility.facilityId}]</option>
              </#list>
            </select>
            <#if (inventoryItem.facilityId)?has_content>
                <a href="<@ofbizUrl>EditFacility?facilityId=${inventoryItem.facilityId}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductEditFacility} [${inventoryItem.facilityId}]</a>
            </#if>
            <br/>
            <span class="tabletext">${uiLabelMap.ProductOrEnterContainerId} :</span>
            <input type="text" name="containerId" value="${inventoryItemData.containerId?if_exists}" size="20" maxlength="20" class="inputBox"/>
         </td>
       </tr>
      <tr>
        <td align="right"><div class="tabletext">${uiLabelMap.ProductFacilityLocation}</div></td>
        <td>&nbsp;</td>
        <td>
          <#if facilityLocation?exists>
            <div class="tabletext">
              <b>${uiLabelMap.ProductArea} :</b>&nbsp;${facilityLocation.areaId?if_exists}
              <b>${uiLabelMap.ProductAisle} :</b>&nbsp;${facilityLocation.aisleId?if_exists}
              <b>${uiLabelMap.ProductSection} :</b>&nbsp;${facilityLocation.sectionId?if_exists}
              <b>${uiLabelMap.ProductLevel} :</b>&nbsp;${facilityLocation.levelId?if_exists}
              <b>${uiLabelMap.ProductPosition}:</b>&nbsp;${facilityLocation.positionId?if_exists}
            </div>
          </#if>
          <#if inventoryItem?exists>
            <input type="text" size="20" maxsize="20" name="locationSeqId" value="${inventoryItem.locationSeqId?if_exists}" class="inputBox"/>
            <span class="tabletext">
                <a href="javascript:call_fieldlookup2(document.inventoryItemForm.locationSeqId,'LookupFacilityLocation<#if (facilityId?exists)>?facilityId=${facilityId}</#if>');">
                    <img src="<@ofbizContentUrl>/images/fieldlookup.gif"</@ofbizContentUrl>" width="15" height="14" border="0" alt="Click here For Field Lookup"/>
                </a>
            </span>
            &nbsp;<a href="<@ofbizUrl>FindFacilityLocation?facilityId=${facilityId?if_exists}&inventoryItemId=${inventoryItemId}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductFindLocation}</a>
          <#else>
            <input type="text" size="20" maxsize="20" name="locationSeqId" value="${locationSeqId?if_exists}" class="inputBox">
            <span class="tabletext">
                <a href="javascript:call_fieldlookup2(document.inventoryItemForm.locationSeqId,'LookupFacilityLocation<#if (facilityId?exists)>?facilityId=${facilityId}</#if>');">
                    <img src="<@ofbizContentUrl>/images/fieldlookup.gif"</@ofbizContentUrl>" width="15" height="14" border="0" alt="Click here For Field Lookup"/>
                </a>
            </span>
          </#if>
        </td>
      </tr>
      <tr>
        <td align="right"><div class="tabletext">${uiLabelMap.ProductLotId}</div></td>
        <td>&nbsp;</td>
        <td><input type="text" name="lotId" value="${inventoryItemData.lotId?if_exists}" size="20" maxlength="20" class="inputBox"></td>
      </tr>
      <tr>
        <td align="right"><div class="tabletext">${uiLabelMap.ProductUomId}</div></td>
        <td>&nbsp;</td>
        <td><input type="text" name="uomId" value="${inventoryItemData.uomId?if_exists}" size="20" maxlength="20" class="inputBox"></td>
      </tr>
      <tr>
        <td align="right"><div class="tabletext">${uiLabelMap.ProductBinNumber}</div></td>
        <td>&nbsp;</td>
        <td><input type="text" name="binNumber" value="${inventoryItemData.binNumber?if_exists}" size="20" maxlength="20" class="inputBox"></td>
      </tr>
      <tr>
        <td align="right"><div class="tabletext">${uiLabelMap.ProductPerUnitPrice}</div></td>
        <td>&nbsp;</td>
        <td><input type="text" name="unitCost" value="${inventoryItemData.unitCost?default(0)}" size="20" maxlength="20" class="inputBox"></td>
      </tr>
      <tr>
        <td align="right"><div class="tabletext">${uiLabelMap.ProductComments}</div></td>
        <td>&nbsp;</td>
        <td><input type="text" name="comments" value="${inventoryItemData.comments?if_exists}" size="60" maxlength="250" class="inputBox"></td>
      </tr>
      <tr><td colspan="3">&nbsp;</td></tr>
      <tr>
        <td align="right"><div class="tabletext">${uiLabelMap.ProductSoftIdentifier}</div></td>
        <td>&nbsp;</td>
        <td><input type="text" name="comments" value="${inventoryItemData.softIdentifier?if_exists}" size="30" maxlength="250" class="inputBox"></td>
      </tr>
      <tr>
        <td align="right"><div class="tabletext">${uiLabelMap.ProductActivationNumber}</div></td>
        <td>&nbsp;</td>
        <td><input type="text" name="comments" value="${inventoryItemData.activationNumber?if_exists}" size="30" maxlength="250" class="inputBox"></td>
      </tr>
    <#if "NON_SERIAL_INV_ITEM" == (inventoryItem.inventoryItemTypeId)?if_exists>
      <tr>
        <td align="right"><div class="tabletext">${uiLabelMap.ProductAvailablePromiseQuantityHand}</div></td>
        <td>&nbsp;</td>
        <td>
            <div class="tabletext">${inventoryItemData.availableToPromiseTotal?if_exists} / ${inventoryItemData.quantityOnHandTotal?if_exists}</div>
            <div class="tabletext">${uiLabelMap.ProductPhysicalInventoryVariance}</div>
        </td>
      </tr>
    <#elseif "SERIALIZED_INV_ITEM" == (inventoryItem.inventoryItemTypeId)?if_exists>
      <tr>
        <td align="right"><div class="tabletext">${uiLabelMap.ProductSerialNumber}</div></td>
        <td>&nbsp;</td>
        <td><input type="text" name="serialNumber" value="${inventoryItemData.serialNumber?if_exists}" size="30" maxlength="60" class="inputBox"></td>
      </tr>
    <#elseif inventoryItem?exists>
      <tr>
        <td align="right"><div class="tabletext">${uiLabelMap.ProductSerialAtpQoh}</div></td>
        <td>&nbsp;</td>
        <td><div class="tabletext" style="color: red;">${uiLabelMap.ProductErrorType} [${inventoryItem.inventoryItemTypeId?if_exists}] ${uiLabelMap.ProductUnknownSpecifyType}.</div></td>
      </tr>
    </#if>
  <tr>
    <td colspan="2">&nbsp;</td>
    <td colspan="5"><input type="submit" value="${uiLabelMap.CommonUpdate}" class="smallSubmit"></td>
  </tr>
</table>
</form>
