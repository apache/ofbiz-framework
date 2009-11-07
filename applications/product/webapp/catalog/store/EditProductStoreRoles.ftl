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
<#if !requestParameters.showAll?exists>
    <a href="<@ofbizUrl>EditProductStoreRoles?productStoreId=${productStoreId}&showAll=Y</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductShowAll}</a>
<#else>
    <a href="<@ofbizUrl>EditProductStoreRoles?productStoreId=${productStoreId}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductShowActive}</a>
</#if>
<div class="screenlet">
    <div class="screenlet-title-bar">
        <h3>${uiLabelMap.PageTitleEditProductStoreRoles}</h3>
    </div>
    <div class="screenlet-body">
        <table cellspacing="0" class="basic-table">
            <tr class="header-row">
              <td><b>${uiLabelMap.PartyParty}</b></td>
              <td><b>${uiLabelMap.PartyRole}</b></td>
              <td><b>${uiLabelMap.CommonFromDate}</b></td>
              <td><b>${uiLabelMap.CommonThruDate}</b></td>
              <td><b>&nbsp;</b></td>
            </tr>
            <#if productStoreRoles?has_content>
              <#assign rowClass = "2">
              <#list productStoreRoles as role>
              <#assign roleType = role.getRelatedOne("RoleType")>
                <tr valign="middle"<#if rowClass == "1"> class="alternate-row"</#if>>
                  <td><a href="/partymgr/control/viewprofile?partyId=${role.partyId}&externalLoginKey=${requestAttributes.externalLoginKey}" class="buttontext">${role.partyId}</a></td>
                  <td>${roleType.get("description",locale)}</td>
                  <td>${role.fromDate?string}</td>
                  <td>${role.thruDate?default("${uiLabelMap.CommonNA}")?string?if_exists}</td>
                  <#if role.thruDate?exists>
                    <td>&nbsp;</td>
                  <#else>
                    <td align="center">
                      <a href="javascript:document.storeRemoveRole_${role_index}.submit();" class="buttontext">${uiLabelMap.CommonDelete}</a>
                      <form name="storeRemoveRole_${role_index}" method="post" action="<@ofbizUrl>storeRemoveRole</@ofbizUrl>">
                          <input type="hidden" name="productStoreId" value="${productStoreId}"/>
                          <input type="hidden" name="partyId" value="${role.partyId}"/>
                          <input type="hidden" name="roleTypeId" value="${role.roleTypeId}"/>
                          <input type="hidden" name="fromDate" value="${role.fromDate}"/>
                      </form>                      
                    </td>
                  </#if>
                </tr>
                <#-- toggle the row color -->
                <#if rowClass == "2">
                    <#assign rowClass = "1">
                <#else>
                    <#assign rowClass = "2">
                </#if>
              </#list>
            </#if>
        </table>
    </div>
</div>
<div class="screenlet">
    <div class="screenlet-title-bar">
        <h3>${uiLabelMap.ProductCreateProductStoreRole}</h3>
    </div>
    <div class="screenlet-body">
        <form name="addProductStoreRole" action="<@ofbizUrl>storeCreateRole</@ofbizUrl>" method="post">
            <input type="hidden" name="productStoreId" value="${productStoreId}">
            <table cellspacing="0" class="basic-table">
              <tr>
                <td class="label">${uiLabelMap.PartyRoleType}</td>
                <td>
                  <select name="roleTypeId">
                    <#list roleTypes as roleType>
                      <option value="${roleType.roleTypeId}">${roleType.get("description",locale)}</option>
                    </#list>
                  </select>
                </td>
              </tr>
              <tr>
                <td class="label">${uiLabelMap.PartyParty}</td>
                <td>
                  <input type="text" name="partyId" size="20">
                  <a href="javascript:call_fieldlookup2(document.addProductStoreRole.partyId,'LookupPartyName');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt="${uiLabelMap.CommonClickHereForFieldLookup}"/></a>
                </td>
              </tr>
              <tr>
                <td class="label">${uiLabelMap.CommonFromDate}</td>
                <td>
                  <input type="text" name="fromDate" size="25">
                  <a href="javascript:call_cal(document.addProductStoreRole.fromDate, null);"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Calendar'></a>
                </td>
              </tr>
              <tr>
                <td>&nbsp;</td>
                <td><input type="submit" class="smallSubmit" value="${uiLabelMap.CommonAdd}"></td>
              </tr>
            </table>
        </form>
    </div>
</div>