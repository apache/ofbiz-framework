<?xml version="1.0" encoding="UTF-8"?>
<!--
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
<#if glAcctgOrgAndCostCenterMapList?has_content && glAccountCategories?has_content>
  <form name="costCentersReportPdfForm" method="post" action="<@ofbizUrl>CostCentersReport.pdf</@ofbizUrl>">
    <input type="hidden" name="organizationPartyId" value="${parameters.organizationPartyId}"/>
    <input type="hidden" name="fromDate" value="${parameters.fromDate}"/>
    <input type="hidden" name="thruDate" value="${parameters.thruDate}"/>
    <input type="hidden" name="timePeriod" value="${parameters.timePeriod}"/>
    <a href="javascript:document.costCentersReportPdfForm.submit();" class="buttontext">${uiLabelMap.AccountingExportAsPdf}</a>
  </form>
  <table class="basic-table hover-bar" cellspacing="0">
    <tr class="header-row">
      <th>${uiLabelMap.FormFieldTitle_glAccountId}</th>
      <th>${uiLabelMap.FormFieldTitle_accountCode}</th>
      <th>${uiLabelMap.FormFieldTitle_accountName}</th>
      <th>${uiLabelMap.FormFieldTitle_postedBalance} - (${currencyUomId})</th>
      <#list glAccountCategories as glAccountCategory>
        <th>${glAccountCategory.description!} - (${currencyUomId})</th>
      </#list>
    </tr>
    <#list glAcctgOrgAndCostCenterMapList as glAcctgOrgAndCostCenterMap>
      <#if glAcctgOrgAndCostCenterMap?has_content>
        <tr>
          <td>${glAcctgOrgAndCostCenterMap.glAccountId?if_exists}</td>
          <td>${glAcctgOrgAndCostCenterMap.accountCode?if_exists}</td>
          <td>${glAcctgOrgAndCostCenterMap.accountName?if_exists}</td>
          <td>${glAcctgOrgAndCostCenterMap.postedBalance?if_exists}</td>
          <#list glAccountCategories as glAccountCategory>
            <td>${(glAcctgOrgAndCostCenterMap[glAccountCategory.glAccountCategoryId?if_exists]?if_exists)}</td>
          </#list>
        </tr>
      </#if>
    </#list>
  </table>
<#else>
  <label>${uiLabelMap.AccountingNoRecordFound}</label>
</#if>
