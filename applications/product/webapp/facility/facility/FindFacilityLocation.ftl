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
    
    <div class="head1">${uiLabelMap.ProductFindLocationsFor} <span class="head2"><#if facility?exists>${(facility.facilityName)?if_exists}</#if> [${uiLabelMap.CommonId}:${facilityId?if_exists}]</span></div>
    <a href="<@ofbizUrl>EditFacility</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductNewFacility}]</a>
    <a href="<@ofbizUrl>EditFacilityLocation?facilityId=${facilityId?if_exists}</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductNewFacilityLocation}]</a>
        
    <form action="<@ofbizUrl>FindFacilityLocation</@ofbizUrl>" method="GET" style="margin: 0;" name="findFacilityLocation">
        <table border="0" cellpadding="2" cellspacing="0">
        <#if !(facilityId?exists)>
            <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.ProductFacility}</div></td>
                <td>&nbsp;</td>
                <td width="74%"><input type="text" class="inputBox" value="" size="19" maxlength="20"></td>
            </tr>
        <#else>
            <input type="hidden" name="facilityId" value="${facilityId}">
        </#if>
        <tr>
            <td width="26%" align="right"><div class="tabletext">${uiLabelMap.ProductLocationSeqId}</div></td>
            <td>&nbsp;</td>
            <td width="74%">
                <input type="text" class="inputBox" name="locationSeqId" value="" size="19" maxlength="20">
                <span class="tabletext">
                    <a href="javascript:call_fieldlookup2(document.findFacilityLocation.locationSeqId,'LookupFacilityLocation<#if (facilityId?exists)>?facilityId=${facilityId}</#if>');">
                        <img src="<@ofbizContentUrl>/images/fieldlookup.gif"</@ofbizContentUrl>" width="15" height="14" border="0" alt="Click here For Field Lookup"/>
                    </a>
                </span>
            </td>
        </tr>
        <tr>
        <tr>
            <td width="26%" align="right"><div class="tabletext">${uiLabelMap.CommonArea}</div></td>
            <td>&nbsp;</td>
            <td width="74%"><input type="text" class="inputBox" name="areaId" value="" size="19" maxlength="20"></td>
        </tr>
        <tr>
            <td width="26%" align="right"><div class="tabletext">${uiLabelMap.ProductAisle}</div></td>
            <td>&nbsp;</td>
            <td width="74%"><input type="text" class="inputBox" name="aisleId" value="" size="19" maxlength="20"></td>
        </tr>
        <tr>
            <td width="26%" align="right"><div class="tabletext">${uiLabelMap.ProductSection}</div></td>
            <td>&nbsp;</td>
            <td width="74%"><input type="text" class="inputBox" name="sectionId" value="" size="19" maxlength="20"></td>
        </tr>
        <tr>
            <td width="26%" align="right"><div class="tabletext">${uiLabelMap.ProductLevel}</div></td>
            <td>&nbsp;</td>
            <td width="74%"><input type="text" class="inputBox" name="levelId" value="" size="19" maxlength="20"></td>
        </tr>
        <tr>
            <td width="26%" align="right"><div class="tabletext">${uiLabelMap.ProductPosition}</div></td>
            <td>&nbsp;</td>
            <td width="74%"><input type="text" class="inputBox" name="positionId" value="" size="19" maxlength="20"></td>
        </tr>             
        <tr>
            <td colspan="2">&nbsp;</td>
            <td colspan="1" align="left"><input type="submit" name="look_up" value="${uiLabelMap.CommonFind}"></td>
        </tr>
        </table>
    </form>
    
    <#if foundLocations?exists>
        <br/>
        <span class="head1">${uiLabelMap.CommonFound}:&nbsp;</span><span class="head2"><b>${foundLocations.size()}</b>&nbsp;${uiLabelMap.ProductLocationsFor}&nbsp;<#if facility?exists>${(facility.facilityName)?if_exists}</#if> [ID:${facilityId?if_exists}]</span>
        <table border="1" cellpadding="2" cellspacing="0">
        <tr>
            <td><div class="tabletext"><b>${uiLabelMap.ProductFacility}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductLocationSeqId}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductType}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.CommonArea}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductAisle}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductSection}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductLevel}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductPosition}</b></div></td>
            <td>&nbsp;</td>
            <td>&nbsp;</td>
            <#if itemId?exists>
                <td>&nbsp;</td>
            </#if>
        </tr>
        <#list foundLocations as location>
        <#assign locationTypeEnum = location.getRelatedOneCache("TypeEnumeration")?if_exists>
        <tr>
            <td><div class="tabletext"><a href="<@ofbizUrl>EditFacility?facilityId=${(location.facilityId)?if_exists}</@ofbizUrl>" class="buttontext">&nbsp;${(location.facilityId)?if_exists}</a></div></td>
            <td><div class="tabletext">&nbsp;<a href="<@ofbizUrl>EditFacilityLocation?facilityId=${facilityId}&locationSeqId=${(location.locationSeqId)?if_exists}</@ofbizUrl>" class="buttontext">${(location.locationSeqId)?if_exists}</a></div></td>
            <td><div class="tabletext">&nbsp;${(locationTypeEnum.get("description",locale))?default(location.locationTypeEnumId?if_exists)}</div></td>
            <td><div class="tabletext">&nbsp;${(location.areaId)?if_exists}</div></td>
            <td><div class="tabletext">&nbsp;${(location.aisleId)?if_exists}</div></td>
            <td><div class="tabletext">&nbsp;${(location.sectionId)?if_exists}</div></td>
            <td><div class="tabletext">&nbsp;${(location.levelId)?if_exists}</div></td>
            <td><div class="tabletext">&nbsp;${(location.positionId)?if_exists}</div></td>       
            <td>
            <a href="<@ofbizUrl>EditInventoryItem?facilityId=${(location.facilityId)?if_exists}&locationSeqId=${(location.locationSeqId)?if_exists}</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductNewInventoryItem}]</a>
            </td>
            <#if itemId?exists>
                <td>
                <a href="<@ofbizUrl>UpdateInventoryItem?inventoryItemId=${itemId}&facilityId=${facilityId}&locationSeqId=${(location.locationSeqId)?if_exists}</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductSetItem} ${itemId}]</a>
                </td>
            </#if>   
            <td>          
            <a href="<@ofbizUrl>EditFacilityLocation?facilityId=${(location.facilityId)?if_exists}&locationSeqId=${(location.locationSeqId)?if_exists}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonEdit}]</a>
            </td>     
        </tr>
        </#list>
        </table>
    </#if>
