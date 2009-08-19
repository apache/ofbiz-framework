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
<#if glAcctgAndAmountPercentageList?has_content && glAccountCategories?has_content>

  <form name="costCenters" id="costCenters" method="post">
    <table class="basic-table hover-bar" cellspacing="0">
      <tr class="header-row">
        <th>${uiLabelMap.FormFieldTitle_glAccountId}</th>
        <th>${uiLabelMap.FormFieldTitle_accountCode}</th>
        <th>${uiLabelMap.FormFieldTitle_accountName}</th>
        <#list glAccountCategories as glAccountCategory>
          <th>${glAccountCategory.description!}</th>
        </#list>
      </tr>

      <#list glAcctgAndAmountPercentageList as glAcctgAndAmountPercentage>
        <tr>
          <td>${glAcctgAndAmountPercentage.glAccountId}</td>
          <td>${glAcctgAndAmountPercentage.accountCode!}</td>
          <td>${glAcctgAndAmountPercentage.accountName!}</td>
          <#list glAccountCategories as glAccountCategory>
            <td>
              <input type="hidden" id="glAccountId_${glAcctgAndAmountPercentage.glAccountId}" name="glAccountId_${glAcctgAndAmountPercentage.glAccountId!}" value="${glAcctgAndAmountPercentage.glAccountId!}"/>
              <input type="hidden" id="glAccountCategoryId_${glAccountCategory.glAccountCategoryId!}_${glAcctgAndAmountPercentage.glAccountId!}" name="glAccountCategoryId_${glAccountCategory.glAccountCategoryId!}_${glAcctgAndAmountPercentage.glAccountId!}" value="${(glAccountCategory.glAccountCategoryId!)}"/>
              <#assign id = "amountPercentage_" + glAcctgAndAmountPercentage.glAccountId + "_" + glAccountCategory.glAccountCategoryId/>
              <#if (glAcctgAndAmountPercentage[glAccountCategory.glAccountCategoryId!])??>
                <input type="text" id="${id}" name="${id}" value="${(glAcctgAndAmountPercentage[glAccountCategory.glAccountCategoryId!])!}" onchange="javascript:changeAmountPercentage(id);"/>
              <#else>
                <input type="text" id="${id}" name="${id}" value="" onchange="javascript:changeAmountPercentage(id);"/>
              </#if>
            </td>
          </#list>
          <td>
            <span id="notValidTotal_${glAcctgAndAmountPercentage.glAccountId}" style="display:none">${uiLabelMap.FormFieldTitle_notValidTotal}</span>
            <span id="validTotal_${glAcctgAndAmountPercentage.glAccountId}" style="display:none">${uiLabelMap.FormFieldTitle_validTotal}</span>
          </td>
        </tr>
      </#list>
    </table>
  </form>
<#else>
  <label>${uiLabelMap.AccountingNoRecordFound}</label>
</#if>
