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

<#if hasPermission>
  <h1>Find Global GL Account</h1>
  <div class="button-bar"><a href="<@ofbizUrl>EditGlobalGlAccount</@ofbizUrl>" class="smallSubmit">Create New Global GL Account</a></div>
  <br/>
  <table class="basic-table dark-grid" cellspacing="0">
    <tr class="header-row">
      <td>ID</td>    
      <td>Name</td>
      <td>Code</td>
      <td>Parent ID</td>
      <td>Type</td>
      <td>Class</td>
      <td>Resource</td>
      <td>&nbsp;</td>
    </tr>
    <#list glAccounts as glAccount>
      <#assign glAccountType = glAccount.getRelatedOne("GlAccountType")>
      <#assign glAccountClass = glAccount.getRelatedOne("GlAccountClass")>
      <#assign glResourceType = glAccount.getRelatedOne("GlResourceType")>
      <tr>
        <td class="button-col"><a href="<@ofbizUrl>EditGlobalGlAccount?glAccountId=${glAccount.glAccountId}</@ofbizUrl>">${glAccount.glAccountId}</a></td>
        <td>${glAccount.accountName?default("&nbsp;")}</td>
        <td>${glAccount.accountCode?default("&nbsp;")}</td>
        <td>${glAccount.parentGlAccountId?default("&nbsp;")}</td>
        <td>${glAccountType.description?default(glAccount.glAccountTypeId)}</td>
        <td>${glAccountClass.description?default(glAccount.glAccountClassId)}</td>
        <td>${glResourceType.description?default(glAccount.glResourceTypeId)}</td>
        <td class="button-col">
          <a href="<@ofbizUrl>EditGlobalGlAccount?glAccountId=${glAccount.glAccountId}</@ofbizUrl>">${uiLabelMap.CommonEdit}</a>
        </td>
      </tr>
    </#list>
  </table>
<#else>
  <h3>${uiLabelMap.AccountingViewPermissionError}</h3>
</#if>
