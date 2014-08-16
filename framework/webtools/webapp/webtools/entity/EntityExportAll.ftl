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

<div class="page-title"><span>${uiLabelMap.WebtoolsExportFromDataSource}</span></div>
<p>${uiLabelMap.WebtoolsXMLExportInfo}</p>
<#if results?has_content>
    <hr />
    <h2>${uiLabelMap.WebtoolsResults}:</h2>
    <#list results as result>
        <p>${result}</p>
    </#list>
</#if>
<hr />
<form method="post" action="<@ofbizUrl>entityExportAll</@ofbizUrl>">
    ${uiLabelMap.WebtoolsOutputDirectory}: <input type="text" size="60" name="outpath" value="${outpath!}" /><br />
    ${uiLabelMap.CommonFromDate}: <@htmlTemplate.renderDateTimeField name="fromDate" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="" size="25" maxlength="30" id="fromDate" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/><br/>
    ${uiLabelMap.WebtoolsTimeoutSeconds}: <input type="text" size="6" value="${txTimeout?default('7200')}" name="txTimeout"/><br />
    <br />
    <input type="submit" value="${uiLabelMap.WebtoolsExport}" />
</form>
