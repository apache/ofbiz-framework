<#--
Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->

<#assign uiLabelMap = requestAttributes.uiLabelMap>
<#if hasPermission>

<div class="head1">Find Global GL Account</div>
<div><a href="<@ofbizUrl>EditGlobalGlAccount</@ofbizUrl>" class="buttontext">[Create New Global GL Account]</a></div>
<br/>
<table border="1" cellpadding="2" cellspacing="0">
  <tr>
    <td><div class="tabletext"><b>ID</b></div></td>    
    <td><div class="tabletext"><b>Name</b></div></td>
    <td><div class="tabletext"><b>Code</b></div></td>
    <td><div class="tabletext"><b>Parent ID</b></div></td>
    <td><div class="tabletext"><b>Type</b></div></td>
    <td><div class="tabletext"><b>Class</b></div></td>
    <td><div class="tabletext"><b>Resource</b></div></td>
    <td><div class="tabletext">&nbsp;</div></td>
  </tr>
<#list glAccounts as glAccount>
  <#assign glAccountType = glAccount.getRelatedOne("GlAccountType")>
  <#assign glAccountClass = glAccount.getRelatedOne("GlAccountClass")>
  <#assign glResourceType = glAccount.getRelatedOne("GlResourceType")>
  <tr valign="middle">
    <td><div class="tabletext">&nbsp;<a href="<@ofbizUrl>EditGlobalGlAccount?glAccountId=${glAccount.glAccountId}</@ofbizUrl>" class="buttontext">[${glAccount.glAccountId}]</a></div></td>   
    <td><div class="tabletext">&nbsp;${glAccount.accountName?if_exists}</div></td>
    <td><div class="tabletext">&nbsp;${glAccount.accountCode?if_exists}</div></td>
    <td><div class="tabletext">&nbsp;${glAccount.parentGlAccountId?if_exists}</div></td>
    <td><div class="tabletext">&nbsp;${glAccountType.description?default(glAccount.glAccountTypeId)}</div></td>
    <td><div class="tabletext">&nbsp;${glAccountClass.description?default(glAccount.glAccountClassId)}</div></td>
    <td><div class="tabletext">&nbsp;${glResourceType.description?default(glAccount.glResourceTypeId)}</div></td>
    <td>
      <a href="<@ofbizUrl>EditGlobalGlAccount?glAccountId=${glAccount.glAccountId}</@ofbizUrl>" class="buttontext">
      [${uiLabelMap.CommonEdit}]</a>
    </td>
  </tr>
</#list>
</table>
<br/>

<#else>
  <h3>${uiLabelMap.AccountingViewPermissionError}</h3>
</#if>
