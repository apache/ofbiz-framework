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
<h1>${title}</h1>
      <#if illegalInventoryItem?exists>
            <div class="errorMessage">${illegalInventoryItem}</div>
      </#if>
        <div class="button-bar">
          <a href="<@ofbizUrl>EditFacility</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductNewFacility}</a>
          <a href="<@ofbizUrl>PickMoveStockSimple?facilityId=${facilityId?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPrint}</a>
        </div>
       <#if !(inventoryItem?exists)>
            <form method="post" action="<@ofbizUrl>TransferInventoryItem</@ofbizUrl>">
            <input type="hidden" name="facilityId" value="${facilityId}">
            <table cellspacing="0" class="basic-table">
            <tr>
                <td class="label">${uiLabelMap.ProductInventoryItemId}</td>
                <td>
                  <input type="text" name="inventoryItemId" size="20" maxlength="20">
                  <input type="submit" value="${uiLabelMap.ProductGetItem}">
                </td>
            </tr>
            </table>
            </form>
        <#else>
           <#if !(inventoryTransfer?exists)>
                <form method="post" action="<@ofbizUrl>CreateInventoryTransfer</@ofbizUrl>" name="transferform" style="margin: 0;">
            <#else>
                <form method="post" action="<@ofbizUrl>UpdateInventoryTransfer</@ofbizUrl>" name="transferform" style="margin: 0;">
                <input type="hidden" name="inventoryTransferId" value="${inventoryTransferId?if_exists}">
            </#if>

            <script language="JavaScript" type="text/javascript">
                function setNow(field) { eval('document.transferform.' + field + '.value="${nowTimestamp}"'); }
            </script>

            <table cellspacing="0" class="basic-table">
            <input type="hidden" name="inventoryItemId" value="${inventoryItemId?if_exists}">
            <input type="hidden" name="facilityId" value="${facilityId?if_exists}">
            <input type="hidden" name="locationSeqId" value="${(inventoryItem.locationSeqId)?if_exists}">
            <tr>
                <td width="14%">&nbsp;</td>
                <td width="6%"align="right"><span class="label">${uiLabelMap.ProductInventoryItemId}</span></td>
                <td width="6%">&nbsp;</td>
                <td width="74%">${inventoryItemId}</td>
            </tr>
            <tr>
                <td width="14%">&nbsp;</td>
                <td width="6%" align="right" nowrap class="label">${uiLabelMap.ProductInventoryItemTypeId}</td>
                <td width="6%">&nbsp;</td>
                <td width="74%">
                <#if inventoryItemType?exists>
                    ${(inventoryItemType.get("description",locale))?if_exists}
                </#if>
                </td>
            </tr>
            <tr>
                <td width="14%">&nbsp;</td>
                <td width="6%" align="right" nowrap><span class="label">${uiLabelMap.ProductProductId}</span></td>
                <td width="6%">&nbsp;</td>
                <td width="74%">
                    <#if inventoryItem?exists && (inventoryItem.productId)?exists>
                        <a href="/catalog/control/EditProduct?productId=${(inventoryItem.productId)?if_exists}" class="buttontext">${(inventoryItem.productId)?if_exists}</a>
                    </#if>
                </td>
            </tr>
            <tr>
                <td width="14%">&nbsp;</td>
                <td width="6%" align="right" nowrap><span class="label">${uiLabelMap.CommonStatus}</span></td>
                <td width="6%">&nbsp;</td>
                <td width="74%">${(inventoryStatus.get("description",locale))?default("--")}</td>
            </tr>

            <tr>
                <td width="14%">&nbsp;</td>
                <td width="6%" align="right" nowrap><span class="label">${uiLabelMap.ProductComments}</span></td>
                <td width="6%">&nbsp;</td>
                <td width="74%">${(inventoryItem.comments)?default("--")}</td>
            </tr>

            <tr>
                <td width="14%">&nbsp;</td>
                <td width="6%" align="right" nowrap><span class="label">${uiLabelMap.ProductSerialAtpQoh}</span></td>
                <td width="6%">&nbsp;</td>
                <#if inventoryItem?exists && inventoryItem.inventoryItemTypeId.equals("NON_SERIAL_INV_ITEM")>
                    <td width="74%">
                        ${(inventoryItem.availableToPromiseTotal)?if_exists}&nbsp;
                        /&nbsp;${(inventoryItem.quantityOnHandTotal)?if_exists}
                    </td>
                <#elseif inventoryItem?exists && inventoryItem.inventoryItemTypeId.equals("SERIALIZED_INV_ITEM")>
                    <td width="74%">${(inventoryItem.serialNumber)?if_exists}</td>
                <#elseif inventoryItem?exists>
                    <td class="alert" width="74%">${uiLabelMap.ProductErrorType} ${(inventoryItem.inventoryItemTypeId)?if_exists} ${uiLabelMap.ProductUnknownSpecifyType}.</td>
                </#if>
            </tr>
        <tr>
            <td width="14%">&nbsp;</td>
            <td colspan="3"><hr /></td>
        </tr>
        <tr>
            <td width="14%">&nbsp;</td>
            <td width="6%" align="right" nowrap><span class="label">${uiLabelMap.ProductTransferStatus}</span></td>
            <td width="6%">&nbsp;</td>
            <td width="74%">
            <select name="statusId">
                <#if (inventoryTransfer.statusId)?exists>
                    <#assign curStatusItem = inventoryTransfer.getRelatedOneCache("StatusItem")>
                    <option value="${(inventoryTransfer.statusId)?if_exists}">${(curStatusItem.get("description",locale))?if_exists}</option>
                </#if>
                <#list statusItems as statusItem>
                <option value="${(statusItem.statusId)?if_exists}">${(statusItem.get("description",locale))?if_exists}</option>
                </#list>
            </select>
            </td>
        </tr>
        <tr>
           <td width="14%">&nbsp;</td>
            <td width="6%" align="right" nowrap><span class="label">${uiLabelMap.ProductTransferSendDate}</span></td>
            <td width="6%">&nbsp;</td>
            <td width="74%">
            <input type="text" name="sendDate" value="${(inventoryTransfer.sendDate)?if_exists}" size="22">
            <a href="#" onclick="setNow('sendDate')" class="buttontext">${uiLabelMap.CommonNow}</a>
            </td>
        </tr>
        <#if !(inventoryTransfer?exists)>
            <tr>
                <td width="14%">&nbsp;</td>
                <td width="6%" align="right" nowrap><span class="label">${uiLabelMap.ProductToFacilityContainer}</span></td>
                <td width="6%">&nbsp;</td>
                <td width="74%">
                    <div>
                        <select name="facilityIdTo">
                            <#list facilities as nextFacility>
                            <option value="${(nextFacility.facilityId)?if_exists}">${(nextFacility.facilityName)?if_exists} [${(nextFacility.facilityId)?if_exists}]</option>
                            </#list>
                        </select>
                        <span class="tooltip">${uiLabelMap.ProductSelectFacility}</span>
                        <br />
                        <input type="text" name="containerIdTo" value="${(inventoryTransfer.containerIdTo)?if_exists}" size="20" maxlength="20">
                        <span class="tooltip">${uiLabelMap.ProductOrEnterContainerId}</span>
                    </div>
                </td>
            </tr>
            <tr>
                <td width="14%">&nbsp;</td>
                <td width="6%" align="right" nowrap><span class="label">${uiLabelMap.ProductToLocation}</span></td>
                <td width="6%">&nbsp;</td>
                <td width="74%">
                    <input type="text" size="20" name="locationSeqIdTo" value="${(inventoryTransfer.locationSeqIdTo)?if_exists}" maxlength="20">
                    <a href="javascript:call_fieldlookup2(document.transferform.locationSeqIdTo,'LookupFacilityLocation');">
                        <img src="<@ofbizContentUrl>/images/fieldlookup.gif</@ofbizContentUrl>" width="15" height="14" border="0" alt="${uiLabelMap.CommonClickHereForFieldLookup}"/>
                    </a>
                </td>
            </tr>
            <tr>
                <td width="14%">&nbsp;</td>
                <td width="6%" align="right" nowrap><span class="label">${uiLabelMap.ProductComments}</span></td>
                <td width="6%">&nbsp;</td>
                <td width="74%">
                <input type="text" name="comments" size="60" maxlength="250">
                </td>
            </tr>
            <tr>
                <td width="14%">&nbsp;</td>
                <td width="6%" align="right" nowrap><span class="label">${uiLabelMap.ProductQuantityToTransfer}</span></td>
                <td width="6%">&nbsp;</td>
                <td width="74%">
                <#if inventoryItem?exists && inventoryItem.inventoryItemTypeId.equals("NON_SERIAL_INV_ITEM")>
                    <input type="text" size="5" name="xferQty" value="${(inventoryItem.availableToPromiseTotal)?if_exists}">
                <#elseif inventoryItem?exists && inventoryItem.inventoryItemTypeId.equals("SERIALIZED_INV_ITEM")>
                    <input type="hidden" name="xferQty" value="1">
                    1
                <#elseif inventoryItem?exists>
                    <span class="alert">${uiLabelMap.ProductErrorType} ${(inventoryItem.inventoryItemTypeId)?if_exists} ${uiLabelMap.ProductUnknownSpecifyType}.</span>
                </#if>
                </td>
            </tr>
        <#else>
            <tr>
                <td width="14%">&nbsp;</td>
                <td width="6%" align="right" nowrap><span class="label">${uiLabelMap.ProductTransferReceiveDate}</span></td>
                <td width="6%">&nbsp;</td>
                <td width="74%">
                <input type="text" name="receiveDate" value="${(inventoryTransfer.receiveDate)?if_exists}" size="22">
                <a href="#" onclick="setNow('receiveDate')" class="buttontext">${uiLabelMap.CommonNow}</a>
                </td>
            </tr>
            <tr>
                <td width="14%">&nbsp;</td>
                <td width="6%" align="right" nowrap><span class="label">${uiLabelMap.ProductToFacilityContainer}</span></td>
                <td width="6%">&nbsp;</td>
                <#assign fac = delegator.findByPrimaryKey("Facility", Static["org.ofbiz.base.util.UtilMisc"].toMap("facilityId", inventoryTransfer.facilityIdTo))>
                <td width="74%">${(fac.facilityName)?default("&nbsp;")}</td>
            </tr>
            <tr>
                <td width="14%">&nbsp;</td>
                <td width="6%" align="right" nowrap><span class="label">${uiLabelMap.ProductToLocation}</span></td>
                <td width="6%">&nbsp;</td>
                <td width="74%">
                <input type="text" size="20" name="locationSeqIdTo" value="${(inventoryTransfer.locationSeqIdTo)?if_exists}" maxlength="20">
                    <a href="javascript:call_fieldlookup2(document.transferform.locationSeqIdTo,'LookupFacilityLocation?facilityId=${inventoryTransfer.facilityIdTo}');">
                        <img src="<@ofbizContentUrl>/images/fieldlookup.gif</@ofbizContentUrl>" width="15" height="14" border="0" alt="${uiLabelMap.CommonClickHereForFieldLookup}"/>
                    </a>
                </td>
            </tr>
            <tr>
                <td width="14%">&nbsp;</td>
                <td width="6%" align="right" nowrap><span class="label">${uiLabelMap.ProductComments}</span></td>
                <td width="6%">&nbsp;</td>
                <td width="74%">
                <input type="text" name="comments" value="${(inventoryTransfer.comments)?if_exists}" size="60" maxlength="250">
                </td>
            </tr>
        </#if>
        <tr>
            <td colspan="2">&nbsp;</td>
            <#if !(inventoryTransfer?exists)>
                <td colspan="1"><input type="submit" value="${uiLabelMap.ProductTransfer}"></td>
            <#else>
                <td colspan="1"><input type="submit" value="${uiLabelMap.CommonUpdate}"></td>
            </#if>
        </tr>
        </table>
        </form>
        </#if>
