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

<#if productCategoryId?exists && productCategory?exists>    
    <table border="1" cellpadding="2" cellspacing="0">
    <tr>
    <td><div class="tabletext"><b>${uiLabelMap.PartyPartyId}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.PartyRole}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.CommonFromDateTime}</b></div></td>
    <td align="center"><div class="tabletext"><b>${uiLabelMap.CommonThruDateTime}</b></div></td>
    <td><div class="tabletext"><b>&nbsp;</b></div></td>
    </tr>
    <#assign line = 0>
    <#list productCategoryRoles as productCategoryRole>
    <#assign line = line + 1>
    <#assign curRoleType = productCategoryRole.getRelatedOneCache("RoleType")>
    <tr valign="middle">
    <td><a href="/partymgr/control/viewprofile?party_id=${(productCategoryRole.partyId)?if_exists}" target="_blank" class="buttontext">[${(productCategoryRole.partyId)?if_exists}]</a></td>
    <td><div class="tabletext">${(curRoleType.get("description",locale))?if_exists}</div></td>
    <#assign hasntStarted = false>
    <#if (productCategoryRole.getTimestamp("fromDate"))?exists && Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().before(productCategoryRole.getTimestamp("fromDate"))> <#assign hasntStarted = true></#if>
    <td><div class="tabletext"<#if hasntStarted> style="color: red;"</#if>>${(productCategoryRole.fromDate)?if_exists}</div></td>
    <td align="center">
        <FORM method="post" action="<@ofbizUrl>updatePartyToCategory</@ofbizUrl>" name="lineForm${line}">
            <#assign hasExpired = false>
            <#if (productCategoryRole.getTimestamp("thruDate"))?exists && (Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().after(productCategoryRole.getTimestamp("thruDate")))> <#assign hasExpired = true></#if>
            <input type="hidden" name="productCategoryId" value="${(productCategoryRole.productCategoryId)?if_exists}">
            <input type="hidden" name="partyId" value="${(productCategoryRole.partyId)?if_exists}">
            <input type="hidden" name="roleTypeId" value="${(productCategoryRole.roleTypeId)?if_exists}">
            <input type="hidden" name="fromDate" value="${(productCategoryRole.getTimestamp("fromDate"))?if_exists}">
            <input type="text" size="25" name="thruDate" value="${(productCategoryRole. getTimestamp("thruDate"))?if_exists}" class="inputBox" <#if hasExpired> style="color: red;"</#if>>
            <a href="javascript:call_cal(document.lineForm${line}.thruDate, '${(productCategoryRole.getTimestamp("thruDate"))?default(nowTimestamp?string)}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
            <INPUT type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;">
        </FORM>
    </td>
    <td align="center">
        <a href="<@ofbizUrl>removePartyFromCategory?productCategoryId=${(productCategoryRole.productCategoryId)?if_exists}&partyId=${(productCategoryRole.partyId)?if_exists}&roleTypeId=${(productCategoryRole.roleTypeId)?if_exists}&fromDate=${Static["org.ofbiz.base.util.UtilFormatOut"].encodeQueryValue(productCategoryRole.getTimestamp("fromDate").toString())}</@ofbizUrl>" class="buttontext">
        [${uiLabelMap.CommonDelete}]</a>
    </td>
    </tr>
    </#list>
    </table>
    <br/>
    <form method="post" action="<@ofbizUrl>addPartyToCategory</@ofbizUrl>" style="margin: 0;" name="addNewForm">
    <input type="hidden" name="productCategoryId" value="${productCategoryId}">
    
    <div class="head2">${uiLabelMap.ProductAssociatePartyToCategory}:</div>
    <br/>
    <input type="text" class="inputBox" size="20" maxlength="20" name="partyId" value="">
    <select name="roleTypeId" size="1" class="selectBox">
    <#list roleTypes as roleType>
        <option value="${(roleType.roleTypeId)?if_exists}" <#if roleType.roleTypeId.equals("_NA_")> ${uiLabelMap.ProductSelected}</#if>>${(roleType.get("description",locale))?if_exists}</option>
    </#list>
    </select>
    <input type="text" size="25" name="fromDate" class="inputBox">
    <a href="javascript:call_cal(document.addNewForm.fromDate, '${nowTimestamp?string}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
    <input type="submit" value="${uiLabelMap.CommonAdd}">
    </form>
</#if>
