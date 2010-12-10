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
      <td>${uiLabelMap.CommonSequenceNum}</td>
      <td>${uiLabelMap.CommonComments}</td>
      <td>${uiLabelMap.CommonFromDateTime}</td>
      <td>${uiLabelMap.CommonThruDateTime}</td>
      <td>&nbsp;</td>
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
        <form method="post" action="<@ofbizUrl>updatePartyToProduct</@ofbizUrl>" name="lineForm${line}">
          <td><input type="text" size="5" name="sequenceNum" value="${(productRole.sequenceNum)?if_exists}" /></td>
          <td><input type='text' size='30' name="comments" value="${(productRole.comments)?if_exists}" /></td>
          <#assign hasntStarted = false>
          <#if (productRole.getTimestamp("fromDate"))?exists && Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().before(productRole.getTimestamp("fromDate"))> <#assign hasntStarted = true></#if>
          <td<#if hasntStarted> class="alert"</#if>>${(productRole.fromDate)?if_exists}</td>
          <td align="center">
            <#assign hasExpired = false>
            <#if (productRole.getTimestamp("thruDate"))?exists && (Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().after(productRole.getTimestamp("thruDate")))> <#assign hasExpired = true></#if>
            <input type="hidden" name="productId" value="${(productRole.productId)?if_exists}" />
            <input type="hidden" name="partyId" value="${(productRole.partyId)?if_exists}" />
            <input type="hidden" name="roleTypeId" value="${(productRole.roleTypeId)?if_exists}" />
            <input type="hidden" name="fromDate" value="${(productRole.getTimestamp("fromDate"))?if_exists}" />
            <#if hasExpired><#assign class="alert"></#if>
            <@htmlTemplate.renderDateTimeField name="thruDate" event="" action="" className="${class!''}" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="${(productRole.getTimestamp('thruDate'))?if_exists}" size="25" maxlength="30" id="thruDate1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
          </td>
          <td><input type="submit" value="${uiLabelMap.CommonUpdate}" /></td>
        </form>

        <td align="center">
          <form action="<@ofbizUrl>removePartyFromProduct</@ofbizUrl>" method="post">
             <input type="hidden" name="partyId" value="${(productRole.partyId)?if_exists}" />
             <input type="hidden" name="productId" value="${(productRole.productId)?if_exists}" />
             <input type="hidden" name="roleTypeId" value="${(productRole.roleTypeId)?if_exists}" />
             <input type="hidden" name="fromDate" value="${(productRole.fromDate)?if_exists}" />
             <input type="submit" value="${uiLabelMap.CommonDelete}" />
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
</#if>
