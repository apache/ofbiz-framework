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
<#if facilityId?has_content>
  <h1>${uiLabelMap.ProductEditFacility} ${facility.facilityName?if_exists} [${facilityId?if_exists}]</h1>
  <#--div class="button-bar">
    <a href="<@ofbizUrl>EditFacility</@ofbizUrl>" name="EditFacilityForm" class="buttontext">${uiLabelMap.ProductNewFacility}</a>
      <a href="/workeffort/control/calendar?facilityId=${facilityId}&externalLoginKey=${requestAttributes.externalLoginKey?if_exists}" class="buttontext">${uiLabelMap.CommonViewCalendar}</a>
  </div-->
<#else>
  <h1>${uiLabelMap.ProductNewFacility}</h1>
</#if>

<#if facility?exists && facilityId?has_content>
  <form action="<@ofbizUrl>UpdateFacility</@ofbizUrl>" name="EditFacilityForm" method="post">
  <input type="hidden" name="facilityId" value="${facilityId?if_exists}">
  <table class="basic-table" cellspacing='0'>
  <tr>
    <td class="label">${uiLabelMap.ProductFacilityId}</td>
    <td>
      ${facilityId?if_exists} <span class="tooltip">${uiLabelMap.ProductNotModificationRecrationFacility}</span>
    </td>
  </tr>
<#else>
  <form action="<@ofbizUrl>CreateFacility</@ofbizUrl>" name="EditFacilityForm" method="post" style='margin: 0;'>
  <#if facilityId?exists>
    <h3>${uiLabelMap.ProductCouldNotFindFacilityWithId} "${facilityId?if_exists}".</h3>
  </#if>
  <table class="basic-table" cellspacing='0'>
    <tr>
        <td class="label">${uiLabelMap.ProductFacilityId}</td>
        <td><input type="text" class="required" name="facilityId" value="${partyId?default("")}" size="30" maxlength="60"><span class="tooltip">${uiLabelMap.CommonRequired}</span></td>
    </tr>
</#if>
    <input type="hidden" name="partyId" value="${partyId?default("")}"/>
    <input type="hidden" name="facilityTypeId" value="${facilityType.facilityTypeId?default("WAREHOUSE")}"/>
    <input type="hidden" name="ownerPartyId" value="${facility.ownerPartyId?default(partyId)?default("")}"/>
    <input type="hidden" name="defaultInventoryItemTypeId" value="${facility.defaultInventoryItemTypeId?default("NON_SERIAL_INV_ITEM")}"/>
    <input type="hidden" name="defaultWeightUomId" value="${facility.defaultWeightUomId?default("WT_lb")}"/>
    <input type="hidden" name="squareFootage" value="${facility.squareFootage?if_exists}"/>
<#if partyPostalAddress?has_content>
    <input type="hidden" name="toName" value="${partyPostalAddress.toName?if_exists}"/>
    <input type="hidden" name="attnName" value="${partyPostalAddress.attnName?if_exists}"/>
    <input type="hidden" name="address1" value="${partyPostalAddress.address1?if_exists}"/>
    <input type="hidden" name="address2" value="${partyPostalAddress.address2?if_exists}"/>
    <input type="hidden" name="city" value="${partyPostalAddress.city?if_exists}"/>
    <input type="hidden" name="countryGeoId" value="${partyPostalAddress.countryGeoId?if_exists}"/>
    <input type="hidden" name="postalCode" value="${partyPostalAddress.postalCode?if_exists}"/>
    <input type="hidden" name="stateProvinceGeoId" value="${partyPostalAddress.stateProvinceGeoId?if_exists}"/>
</#if>
  <tr>
    <td class="label">${uiLabelMap.ProductName}</td>
    <td><input type="text" name="facilityName" value="${facility.facilityName?if_exists}" size="30" maxlength="60"></td>
  </tr>
  <tr>
    <td class="label">${uiLabelMap.SetupFacilityDescription}</td>
    <td ><input type="text" name="description" value="${facility.description?if_exists}" size="60" maxlength="250"></td>
  </tr>
  <tr>
    <td class="label">${uiLabelMap.ProductDefaultDaysToShip}</td>
    <td><input type="text" name="defaultDaysToShip" value="${facility.defaultDaysToShip?if_exists}" size="10" maxlength="20"></td>
  </tr>
  <tr>
    <td>&nbsp;</td>
    <#if facilityId?has_content>
      <td><input type="submit" name="Update" value="${uiLabelMap.CommonUpdate}"></td>
    <#else>
      <td><input type="submit" name="Update" value="${uiLabelMap.CommonSave}"></td>
    </#if>
  </tr>
</table>
</form>

<#else>
  <h3>${uiLabelMap.ProductFacilityViewPermissionError}</h3>
</#if>