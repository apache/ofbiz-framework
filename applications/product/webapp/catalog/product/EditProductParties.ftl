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

<#if productId?exists && product?exists>
  <table cellspacing="0" class="basic-table">
    <tr class="header-row">
      <td>${uiLabelMap.PartyPartyId}</td>
      <td>${uiLabelMap.PartyRole}</td>
      <td>${uiLabelMap.CommonFromDateTime}</td>
      <td>${uiLabelMap.CommonThruDateTime}</td>
      <td>&nbsp;</td>
    </tr>
    <#assign line = 0>
    <#assign rowClass = "2">
    <#list productRoles as productRole>
      <#assign line = line + 1>
      <#assign curRoleType = productRole.getRelatedOneCache("RoleType")>
      <tr valign="middle"<#if rowClass == "1"> class="alternate-row"</#if>>
        <td><a href="/partymgr/control/viewprofile?partyId=${(productRole.partyId)?if_exists}" target="_blank" class="buttontext">${(productRole.partyId)?if_exists}</a></td>
        <td>${(curRoleType.get("description",locale))?if_exists}</td>
        <#assign hasntStarted = false>
        <#if (productRole.getTimestamp("fromDate"))?exists && Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().before(productRole.getTimestamp("fromDate"))> <#assign hasntStarted = true></#if>
        <td<#if hasntStarted> class="alert"</#if>>${(productRole.fromDate)?if_exists}</td>
        <td align="center">
          <form method="post" action="<@ofbizUrl>updatePartyToProduct</@ofbizUrl>" name="lineForm${line}">
            <#assign hasExpired = false>
            <#if (productRole.getTimestamp("thruDate"))?exists && (Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().after(productRole.getTimestamp("thruDate")))> <#assign hasExpired = true></#if>
            <input type="hidden" name="productId" value="${(productRole.productId)?if_exists}">
            <input type="hidden" name="partyId" value="${(productRole.partyId)?if_exists}">
            <input type="hidden" name="roleTypeId" value="${(productRole.roleTypeId)?if_exists}">
            <input type="hidden" name="fromDate" value="${(productRole.getTimestamp("fromDate"))?if_exists}">
            <input type="text" size="25" name="thruDate" value="${(productRole. getTimestamp("thruDate"))?if_exists}"<#if hasExpired> class="alert"</#if>>
            <a href="javascript:call_cal(document.lineForm${line}.thruDate, '${(productRole.getTimestamp("thruDate"))?default(nowTimestamp?string)}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
            <input type="submit" value="${uiLabelMap.CommonUpdate}">
          </form>
        </td>
        <td align="center">
          <form action="<@ofbizUrl>removePartyFromProduct</@ofbizUrl>" method="post">
             <input type="hidden" name="partyId" value="${(productRole.partyId)?if_exists}">
             <input type="hidden" name="productId" value="${(productRole.productId)?if_exists}">
             <input type="hidden" name="roleTypeId" value="${(productRole.roleTypeId)?if_exists}">
             <input type="hidden" name="fromDate" value="${productRole.getString("fromDate")}">
             <input type="submit" value="${uiLabelMap.CommonDelete}">
          </form>
        </td>
      </tr>
      <#-- toggle the row color -->
      <#if rowClass == "2">
          <#assign rowClass = "1">
      <#else>
          <#assign rowClass = "2">
      </#if>
    </#list>
  </table>
  <br />
  <h2>${uiLabelMap.ProductAssociatePartyToProduct}:</h2>
  <br />
  <form method="post" action="<@ofbizUrl>addPartyToProduct</@ofbizUrl>" name="addNewForm">
    <input type="hidden" name="productId" value="${productId}">
    <input type="text" size="20" maxlength="20" name="partyId" value="">
    <#-- TODO: Add PartyId lookup screen
    <a href="javascript:call_fieldlookup2(document.addNewForm.partyId,'LookupCustomerName');">
      <img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'/>
    </a> -->
    <select name="roleTypeId" size="1">
    <#list roleTypes as roleType>
        <option value="${(roleType.roleTypeId)?if_exists}" <#if roleType.roleTypeId.equals("_NA_")> ${uiLabelMap.ProductSelected}</#if>>${(roleType.get("description",locale))?if_exists}</option>
    </#list>
    </select>
    <input type="text" size="25" name="fromDate">
    <a href="javascript:call_cal(document.addNewForm.fromDate, '${nowTimestamp?string}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
    <input type="submit" value="${uiLabelMap.CommonAdd}">
  </form>
</#if>
