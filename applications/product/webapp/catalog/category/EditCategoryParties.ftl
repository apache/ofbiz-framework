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
    <div class="screenlet">
        <div class="screenlet-title-bar">
            <h3>${uiLabelMap.PageTitleEditCategoryParties}</h3>
        </div>
        <div class="screenlet-body">
            <table cellspacing="0" class="basic-table">
            <tr class="header-row">
            <td>${uiLabelMap.PartyPartyId}</td>
            <td>${uiLabelMap.PartyRole}</td>
            <td>${uiLabelMap.CommonFromDateTime}</td>
            <td align="center">${uiLabelMap.CommonThruDateTime}</td>
            <td>&nbsp;</td>
            </tr>
            <#assign line = 0>
            <#assign rowClass = "2">
            <#list productCategoryRoles as productCategoryRole>
            <#assign line = line + 1>
            <#assign curRoleType = productCategoryRole.getRelatedOneCache("RoleType")>
            <tr valign="middle"<#if rowClass == "1"> class="alternate-row"</#if>>
            <td><a href="/partymgr/control/viewprofile?party_id=${(productCategoryRole.partyId)?if_exists}" target="_blank" class="buttontext">${(productCategoryRole.partyId)?if_exists}</a></td>
            <td>${(curRoleType.get("description",locale))?if_exists}</td>
            <#assign hasntStarted = false>
            <#if (productCategoryRole.getTimestamp("fromDate"))?exists && Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().before(productCategoryRole.getTimestamp("fromDate"))> <#assign hasntStarted = true></#if>
            <td <#if hasntStarted> style="color: red;"</#if>>${(productCategoryRole.fromDate)?if_exists}</td>
            <td align="center">
                <FORM method="post" action="<@ofbizUrl>updatePartyToCategory</@ofbizUrl>" name="lineForm_update${line}">
                    <#assign hasExpired = false>
                    <#if (productCategoryRole.getTimestamp("thruDate"))?exists && (Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().after(productCategoryRole.getTimestamp("thruDate")))> <#assign hasExpired = true></#if>
                    <input type="hidden" name="productCategoryId" value="${(productCategoryRole.productCategoryId)?if_exists}">
                    <input type="hidden" name="partyId" value="${(productCategoryRole.partyId)?if_exists}">
                    <input type="hidden" name="roleTypeId" value="${(productCategoryRole.roleTypeId)?if_exists}">
                    <input type="hidden" name="fromDate" value="${(productCategoryRole.getTimestamp("fromDate"))?if_exists}">
                    <input type="text" size="25" name="thruDate" value="${(productCategoryRole. getTimestamp("thruDate"))?if_exists}" <#if hasExpired> style="color: red;"</#if>>
                    <a href="javascript:call_cal(document.lineForm_update${line}.thruDate, '${(productCategoryRole.getTimestamp("thruDate"))?default(nowTimestamp?string)}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
                    <INPUT type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;">
                </FORM>
            </td>
            <td align="center">
                <FORM method="post" action="<@ofbizUrl>removePartyFromCategory</@ofbizUrl>" name="lineForm_delete${line}">
                    <#assign hasExpired = false>
                    <input type="hidden" name="productCategoryId" value="${(productCategoryRole.productCategoryId)?if_exists}">
                    <input type="hidden" name="partyId" value="${(productCategoryRole.partyId)?if_exists}">
                    <input type="hidden" name="roleTypeId" value="${(productCategoryRole.roleTypeId)?if_exists}">
                    <input type="hidden" name="fromDate" value="${(productCategoryRole.getTimestamp("fromDate"))?if_exists}">
                    <a href="javascript:document.lineForm_delete${line}.submit()" class="buttontext">${uiLabelMap.CommonDelete}</a>
                </FORM>
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
        </div>
    </div>
    <div class="screenlet">
        <div class="screenlet-title-bar">
            <h3>${uiLabelMap.ProductAssociatePartyToCategory}</h3>
        </div>
        <div class="screenlet-body">
            <table cellspacing="0" class="basic-table">
                <tr>
                    <td>
                        <form method="post" action="<@ofbizUrl>addPartyToCategory</@ofbizUrl>" style="margin: 0;" name="addNewForm">
                            <input type="hidden" name="productCategoryId" value="${productCategoryId}">
                            <input type="text" size="20" maxlength="20" name="partyId" value="">
                            <select name="roleTypeId" size="1">
                            <#list roleTypes as roleType>
                                <option value="${(roleType.roleTypeId)?if_exists}" <#if roleType.roleTypeId.equals("_NA_")> ${uiLabelMap.ProductSelected}</#if>>${(roleType.get("description",locale))?if_exists}</option>
                            </#list>
                            </select>
                            <input type="text" size="25" name="fromDate">
                            <a href="javascript:call_cal(document.addNewForm.fromDate, '${nowTimestamp?string}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
                            <input type="submit" value="${uiLabelMap.CommonAdd}">
                        </form>
                    </td>
                </tr>
            </table>
        </div>
    </div>
</#if>
