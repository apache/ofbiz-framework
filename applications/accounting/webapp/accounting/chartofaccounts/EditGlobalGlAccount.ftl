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

<#if glAccountId?has_content>
  <div class='tabContainer'>
  <a href="<@ofbizUrl>EditGlobalGlAccount?glAccountId=${glAccountId}</@ofbizUrl>" class="tabButtonSelected">GL Account</a>
  <a href="<@ofbizUrl>EditGlobalGlAccountOrganizations?glAccountId=${glAccountId}</@ofbizUrl>" class="tabButton">Organizations</a>
  <a href="<@ofbizUrl>EditGlobalGlAccountRoles?glAccountId=${glAccountId}</@ofbizUrl>" class="tabButton">Roles</a>
  </div>
</#if>
<div class="head1">GL Account <span class='head2'><#if (glAccount.accountName)?has_content>"${glAccount.accountName}"</#if> [${uiLabelMap.CommonId}:${glAccountId?if_exists}]</span></div>
<a href="<@ofbizUrl>EditGlobalGlAccount</@ofbizUrl>" class="buttontext">[New Global GL Account]</a>

${editGlAccountWrapper.renderFormString()}

<#else>
  <h3>${uiLabelMap.AccountingViewPermissionError}</h3>
</#if>
