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
<#include "component://common/webcommon/includes/commonMacros.ftl"/>
<div class="screenlet">
    <div class="screenlet-title-bar">
        <label><b>${uiLabelMap.AccountingMonthlyTrialBalance}</b></label>
    </div>
    <div class="screenlet-body">
        <form name="MonthlyTrialBalanceForm" id="MonthlyTrialBalanceForm" type="text" method="post" action="<@ofbizUrl>MonthlyTrialBalance</@ofbizUrl>">
            <input type="hidden" name="organizationPartyId" value="${organizationPartyId}"/>
            <#assign currentMonth = Static["org.ofbiz.base.util.UtilDateTime"].getMonth(Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp(), timeZone, locale)/>
            <#if (selectedMonth)??><#assign currentMonth = (selectedMonth)!/></#if>
            <table>
                <tr>
                    <td class="label"><label>${uiLabelMap.CommonMonth}</label></td>
                    <td><@MonthField fieldName="selectedMonth" fieldValue=currentMonth/></td>
                </tr>
                <tr>
                    <td class="label"><label>${uiLabelMap.FormFieldTitle_isPosted}</label></td>
                    <td>
                        <select name="posted" id="posted">
                            <#if (parameters.posted)??>
                                <option value="${(parameters.posted)!}">${(parameters.posted)!}</option>
                                <option value="${(parameters.posted)!}">---</option>
                            </#if>
                            <option value="Y">${uiLabelMap.CommonY}</option>
                            <option value="N">${uiLabelMap.CommonN}</option>
                            <option value="All">${uiLabelMap.CommonAll}</option>
                        </select>
                    </td>
                </tr>
                <tr><td class="label"></td><td><input type="submit" value="${uiLabelMap.CommonSubmit}"/></td></tr>
            </table>
        </form>
    </div>
</div>

