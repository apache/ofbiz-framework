<#--
 *  Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
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
 *@author     David E. Jones
 *@author     Brad Steiner
 *@author     thierry.grauss@etu.univ-tours.fr (migration to uiLabelMap)
 *@version    $Rev$
 *@since      2.2
-->

    <div class="head1">${uiLabelMap.ProductInventoryTransfersFor} <span class="head2"><#if facility?exists>${(facility.facilityName)?if_exists}</#if> [${uiLabelMap.CommonId}:${facilityId?if_exists}]</span></div>
    <a href="<@ofbizUrl>EditFacility</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductNewFacility}]</a>
    <#if activeOnly>
        <a href="<@ofbizUrl>FindFacilityTransfers?facilityId=${facilityId}&activeOnly=false</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductActiveAndInactive}]</a>
    <#else>
        <a href="<@ofbizUrl>FindFacilityTransfers?facilityId=${facilityId}&activeOnly=true</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductActiveOnly}]</a>
    </#if>
    <a href="<@ofbizUrl>TransferInventoryItem?facilityId=${facilityId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductInventoryTransfer}]</a>
    
    <br/>
    <#if (toTransfers.size() > 0)>
        <br/>
        <div class="head1">${uiLabelMap.CommonTo}:<span class="head2">&nbsp;<#if facility?exists>${(facility.facilityName)?if_exists}</#if> [${uiLabelMap.CommonId}:${facilityId?if_exists}]</span></div>
        <table border="1" cellpadding="2" cellspacing="0" width="100%">
            <tr>
            <td><div class="tabletext"><b>${uiLabelMap.ProductTransferId}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductItem}</b></div></td>      
            <td><div class="tabletext"><b>${uiLabelMap.CommonFrom}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.CommonSendDate}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.CommonStatus}</b></div></td>
            <td>&nbsp;</td>
            </tr>
        
            <#list toTransfers as transfer>
            <tr>
            <td><div class="tabletext"><a href="<@ofbizUrl>TransferInventoryItem?inventoryTransferId=${(transfer.inventoryTransferId)?if_exists}</@ofbizUrl>" class="buttontext">&nbsp;${(transfer.inventoryTransferId)?if_exists}</a></div></td>
            <td><div class="tabletext"><a href="<@ofbizUrl>EditInventoryItem?inventoryItemId=${(transfer.inventoryItemId)?if_exists}</@ofbizUrl>" class="buttontext">&nbsp;${(transfer.inventoryItemId)?if_exists}</a></div></td>      
            <td>
                <#assign fac = delegator.findByPrimaryKey("Facility", Static["org.ofbiz.base.util.UtilMisc"].toMap("facilityId", transfer.getString("facilityId")))>
                <div class="tabletext"><a href="<@ofbizUrl>EditFacility?facilityId=${(transfer.facilityId)?if_exists}</@ofbizUrl>" class="buttontext">&nbsp;<#if fac?exists>${(fac.facilityName)?if_exists}</#if>&nbsp;[${(transfer.facilityId)?if_exists}]</a></div>
            </td>
            <td><div class="tabletext">&nbsp;${(transfer.sendDate)?if_exists}</div></td>
            <td>
                <#if (transfer.statusId)?exists>
                    <#assign transferStatus = delegator.findByPrimaryKey("StatusItem", Static["org.ofbiz.base.util.UtilMisc"].toMap("statusId", transfer.statusId))>
                    <div class="tabletext">&nbsp;${(transferStatus.get("description",locale))?if_exists}</div>
                </#if>
            </td>
            <td align="center"><div class="tabletext"><a href="<@ofbizUrl>TransferInventoryItem?inventoryTransferId=${(transfer.inventoryTransferId)?if_exists}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonEdit}]</a></div></td>
            </tr>
            </#list>
        </table>
    </#if>
    
    <#if (fromTransfers.size() > 0)>
        <br/>
        <div class="head1">${uiLabelMap.CommonFrom}:<span class="head2">&nbsp;<#if facility?exists>${(facility.facilityName)?if_exists}</#if> [${uiLabelMap.CommonId}:${facilityId?if_exists}]</span></div>
        <table border="1" cellpadding="2" cellspacing="0" width="100%">
            <tr>
            <td><div class="tabletext"><b>${uiLabelMap.ProductTransferId}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductItem}</b></div></td>      
            <td><div class="tabletext"><b>${uiLabelMap.CommonTo}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.CommonSendDate}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.CommonStatus}</b></div></td>
            <td>&nbsp;</td>
            </tr>
        
            <#list fromTransfers as transfer>
            <tr>
            <td><div class="tabletext"><a href="<@ofbizUrl>TransferInventoryItem?inventoryTransferId=${(transfer.inventoryTransferId)?if_exists}</@ofbizUrl>" class="buttontext">&nbsp;${(transfer.inventoryTransferId)?if_exists}</a></div></td>
            <td><div class="tabletext"><a href="<@ofbizUrl>EditInventoryItem?inventoryItemId=${(transfer.inventoryItemId)?if_exists}</@ofbizUrl>" class="buttontext">&nbsp;${(transfer.inventoryItemId)?if_exists}</a></div></td>      
            <td>
                <#assign fac = delegator.findByPrimaryKey("Facility", Static["org.ofbiz.base.util.UtilMisc"].toMap("facilityId", transfer.getString("facilityIdTo")))>
                <div class="tabletext"><a href="<@ofbizUrl>EditFacility?facilityId=${(transfer.facilityIdTo)?if_exists}</@ofbizUrl>" class="buttontext">&nbsp;<#if fac?exists>${(fac.facilityName)?if_exists}</#if>&nbsp;[${(transfer.facilityIdTo)?if_exists}]</a></div>
            </td>
            <td><div class="tabletext">&nbsp;${(transfer.sendDate)?if_exists}</div></td>
            <td>
                <#if (transfer.statusId)?exists>
                    <#assign transferStatus = delegator.findByPrimaryKey("StatusItem", Static["org.ofbiz.base.util.UtilMisc"].toMap("statusId", transfer.statusId))>
                    <div class="tabletext">&nbsp;${(transferStatus.get("description",locale))?if_exists}</div>
                </#if>
            </td>
            <td align="center"><div class="tabletext"><a href="<@ofbizUrl>TransferInventoryItem?inventoryTransferId=${(transfer.inventoryTransferId)?if_exists}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonEdit}]</a></div></td>
            </tr>
            </#list>
        </table>
    </#if>
