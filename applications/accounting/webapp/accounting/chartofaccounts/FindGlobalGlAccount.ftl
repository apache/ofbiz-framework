<#--
 *  Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a 
 *  copy of this software and associated documentation files (the "Software"), 
 *  to deal in the Software without restriction, including without limitation 
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 *  and/or sell copies of the Software, and to permit persons to whom the 
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included 
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT 
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     David E. Jones (jonesde@ofbiz.org)
 *@version    $Rev$
 *@since      3.0
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
