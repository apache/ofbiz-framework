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

<#if facility?? && facilityId?has_content>
  <form action="<@ofbizUrl>UpdateFacility</@ofbizUrl>" name="EditFacilityForm" method="post" class="basic-form">
  <input type="hidden" name="facilityId" value="${facilityId!}" />
  <table class="basic-table" cellspacing='0'>
  <tr>
    <td class="label">${uiLabelMap.ProductFacilityId}</td>
    <td>
      ${facilityId!} <span class="tooltip">${uiLabelMap.ProductNotModificationRecrationFacility}</span>
    </td>
  </tr>
<#else>
  <form action="<@ofbizUrl>CreateFacility</@ofbizUrl>" name="EditFacilityForm" method="post" class="basic-form">
  <#if facilityId??>
    <h3>${uiLabelMap.ProductCouldNotFindFacilityWithId} "${facilityId!}".</h3>
  </#if>
  <table class="basic-table" cellspacing='0'>
</#if>
  <tr>
    <td class="label">${uiLabelMap.ProductFacilityTypeId}</td>
    <td>
      <select name="facilityTypeId">
        <option selected="selected" value='${facilityType.facilityTypeId!}'>${facilityType.get("description",locale)!}</option>
        <option value='${facilityType.facilityTypeId!}'>----</option>
        <#list facilityTypes as nextFacilityType>
          <option value='${nextFacilityType.facilityTypeId!}'>${nextFacilityType.get("description",locale)!}</option>
        </#list>
      </select>
      <span class="tooltip">${uiLabelMap.CommonRequired}</span>
    </td>
  </tr>
  <tr>
    <td class="label">${uiLabelMap.FormFieldTitle_parentFacilityId}</td>
    <td>
      <@htmlTemplate.lookupField value="${facility.parentFacilityId!}" formName="EditFacilityForm" name="parentFacilityId" id="parentFacilityId" fieldFormName="LookupFacility"/>
    </td>
  </tr>
  <tr>
    <td class="label">${uiLabelMap.ProductFacilityOwner}</td>
    <td>
      <@htmlTemplate.lookupField value="${facility.ownerPartyId!}" formName="EditFacilityForm" name="ownerPartyId" id="ownerPartyId" fieldFormName="LookupPartyName"/>
      <span class="tooltip">${uiLabelMap.CommonRequired}</span>
    </td>
  </tr>
  <tr>
    <td class="label">${uiLabelMap.ProductFacilityDefaultWeightUnit}</td>
    <td>
      <select name="defaultWeightUomId">
          <option value=''>${uiLabelMap.CommonNone}</option>
          <#list weightUomList as uom>
            <option value='${uom.uomId}'
               <#if (facility.defaultWeightUomId?has_content) && (uom.uomId == facility.defaultWeightUomId)>
               selected="selected"
               </#if>
             >${uom.get("description",locale)?default(uom.uomId)}</option>
          </#list>
      </select>
    </td>
  </tr>
  <tr>
    <td class="label">${uiLabelMap.ProductFacilityDefaultInventoryItemType}</td>
    <td>
      <select name="defaultInventoryItemTypeId">
          <#list inventoryItemTypes as nextInventoryItemType>
            <option value='${nextInventoryItemType.inventoryItemTypeId}'
               <#if (facility.defaultInventoryItemTypeId?has_content) && (nextInventoryItemType.inventoryItemTypeId == facility.defaultInventoryItemTypeId)>
               selected="selected"
               </#if>
             >${nextInventoryItemType.get("description",locale)?default(nextInventoryItemType.inventoryItemTypeId)}</option>
          </#list>
      </select>
    </td>
  </tr>
  <tr>
    <td class="label">${uiLabelMap.ProductName}</td>
    <td>
      <input type="text" name="facilityName" value="${facility.facilityName!}" size="30" maxlength="60" />
      <span class="tooltip">${uiLabelMap.CommonRequired}</span>
    </td>
  </tr>
  <tr>
    <td class="label">${uiLabelMap.ProductFacilitySize}</td>
    <td><input type="text" name="facilitySize" value="${facility.facilitySize!}" size="10" maxlength="20" /></td>
  </tr>
  <tr>
   <td class="label">${uiLabelMap.ProductFacilityDefaultAreaUnit}</td>
    <td>
      <select name="facilitySizeUomId">
          <option value=''>${uiLabelMap.CommonNone}</option>
          <#list areaUomList as uom>
            <option value='${uom.uomId}'
               <#if (facility.facilitySizeUomId?has_content) && (uom.uomId == facility.facilitySizeUomId)>
               selected="selected"
               </#if>
             >${uom.get("description",locale)?default(uom.uomId)}</option>
          </#list>
      </select>
    </td>
  </tr>  
  <tr>
    <td class="label">${uiLabelMap.ProductProductDescription}</td>
    <td ><input type="text" name="description" value="${facility.description!}" size="60" maxlength="250" /></td>
  </tr>
  <tr>
    <td class="label">${uiLabelMap.ProductDefaultDaysToShip}</td>
    <td><input type="text" name="defaultDaysToShip" value="${facility.defaultDaysToShip!}" size="10" maxlength="20" /></td>
  </tr>
  <tr>
    <td>&nbsp;</td>
    <#if facilityId?has_content>
      <td><input type="submit" name="Update" value="${uiLabelMap.CommonUpdate}" /></td>
    <#else>
      <td><input type="submit" name="Update" value="${uiLabelMap.CommonSave}" /></td>
    </#if>
  </tr>
</table>
</form>
