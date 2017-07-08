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

<#if productCategoryId?? && productCategory??>
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
            <#assign curRoleType = productCategoryRole.getRelatedOne("RoleType", true)>
            <tr valign="middle"<#if "1" == rowClass> class="alternate-row"</#if>>
            <td><a href="/partymgr/control/viewprofile?party_id=${(productCategoryRole.partyId)!}" target="_blank" class="buttontext">${(productCategoryRole.partyId)!}</a></td>
            <td>${(curRoleType.get("description",locale))!}</td>
            <#assign hasntStarted = false>
            <#if (productCategoryRole.getTimestamp("fromDate"))?? && Static["org.apache.ofbiz.base.util.UtilDateTime"].nowTimestamp().before(productCategoryRole.getTimestamp("fromDate"))> <#assign hasntStarted = true></#if>
            <td <#if hasntStarted> style="color: red;"</#if>>${(productCategoryRole.fromDate)!}</td>
            <td align="center">
                <form method="post" action="<@ofbizUrl>updatePartyToCategory</@ofbizUrl>" name="lineForm_update${line}">
                    <#assign hasExpired = false>
                    <#if (productCategoryRole.getTimestamp("thruDate"))?? && (Static["org.apache.ofbiz.base.util.UtilDateTime"].nowTimestamp().after(productCategoryRole.getTimestamp("thruDate")))> <#assign hasExpired = true></#if>
                    <input type="hidden" name="productCategoryId" value="${(productCategoryRole.productCategoryId)!}" />
                    <input type="hidden" name="partyId" value="${(productCategoryRole.partyId)!}" />
                    <input type="hidden" name="roleTypeId" value="${(productCategoryRole.roleTypeId)!}" />
                    <input type="hidden" name="fromDate" value="${(productCategoryRole.getTimestamp("fromDate"))!}" />
                    <#if hasExpired><#assign class="alert"><#else><#assign class=""></#if>
                    <@htmlTemplate.renderDateTimeField name="thruDate" event="" action="" className="${class!}" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="${(productCategoryRole. getTimestamp('thruDate'))!}" size="25" maxlength="30" id="thruDate_1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                    <input type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;" />
                </form>
            </td>
            <td align="center">
                <form method="post" action="<@ofbizUrl>removePartyFromCategory</@ofbizUrl>" name="lineForm_delete${line}">
                    <#assign hasExpired = false>
                    <input type="hidden" name="productCategoryId" value="${(productCategoryRole.productCategoryId)!}" />
                    <input type="hidden" name="partyId" value="${(productCategoryRole.partyId)!}" />
                    <input type="hidden" name="roleTypeId" value="${(productCategoryRole.roleTypeId)!}" />
                    <input type="hidden" name="fromDate" value="${(productCategoryRole.getTimestamp("fromDate"))!}" />
                    <a href="javascript:document.lineForm_delete${line}.submit()" class="buttontext">${uiLabelMap.CommonDelete}</a>
                </form>
            </td>
            </tr>
            <#-- toggle the row color -->
            <#if "2" == rowClass>
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
                            <input type="hidden" name="productCategoryId" value="${productCategoryId}" />
                            <@htmlTemplate.lookupField value="${parameters.partyId!}"  formName="addNewForm" name="partyId" id="partyId" fieldFormName="LookupPartyName"/>
                            <select name="roleTypeId" size="1">
                            <#list roleTypes as roleType>
                                <option value="${(roleType.roleTypeId)!}" <#if "_NA_" == roleType.roleTypeId> selected="selected"</#if>>${(roleType.get("description",locale))!}</option>
                            </#list>
                            </select>

                            <@htmlTemplate.renderDateTimeField name="fromDate" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="" size="25" maxlength="30" id="fromDate_1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                            <input type="submit" value="${uiLabelMap.CommonAdd}" />
                        </form>
                    </td>
                </tr>
            </table>
        </div>
    </div>
</#if>
