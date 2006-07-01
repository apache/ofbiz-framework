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
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@version    $Rev$
 *@since      2.1
-->

<#if (requestAttributes.uiLabelMap)?exists><#assign uiLabelMap = requestAttributes.uiLabelMap></#if>
<#if (requestAttributes.security)?exists><#assign security = requestAttributes.security></#if>
<#if (requestAttributes.userLogin)?exists><#assign userLogin = requestAttributes.userLogin></#if>
<#if (requestAttributes.checkLoginUrl)?exists><#assign checkLoginUrl = requestAttributes.checkLoginUrl></#if>

<#assign unselectedLeftClassName = "headerButtonLeft">
<#assign unselectedRightClassName = "headerButtonRight">
<#assign selectedLeftClassMap = {page.headerItem?default("void") : "headerButtonLeftSelected"}>
<#assign selectedRightClassMap = {page.headerItem?default("void") : "headerButtonRightSelected"}>

<div class="apptitle">&nbsp;${uiLabelMap.OrderOrderManagerApplication}&nbsp;</div>
<div class="row">
  <#-- Just goes to Find Orders for now <div class="col"><a href="<@ofbizUrl>main</@ofbizUrl>" class="${selectedLeftClassMap.main?default(unselectedLeftClassName)}">${uiLabelMap.CommonMain}</a></div> -->
  <#if (security.hasEntityPermission("ORDERMGR", "_VIEW", session) || security.hasEntityPermission("ORDERMGR", "_PURCHASE_VIEW", session))>
  <div class="col"><a href="<@ofbizUrl>FindRequest</@ofbizUrl>" class="${selectedLeftClassMap.request?default(unselectedLeftClassName)}">${uiLabelMap.OrderRequests}</a></div>
  <div class="col"><a href="<@ofbizUrl>FindQuote</@ofbizUrl>" class="${selectedLeftClassMap.quote?default(unselectedLeftClassName)}">${uiLabelMap.OrderOrderQuotes}</a></div>
  </#if>
  <#if security.hasEntityPermission("ORDERMGR", "_VIEW", session)>
  <div class="col"><a href="<@ofbizUrl>orderlist</@ofbizUrl>" class="${selectedLeftClassMap.orderlist?default(unselectedLeftClassName)}">${uiLabelMap.OrderOrderList}</a></div>  
  </#if>
  <#if security.hasEntityPermission("ORDERMGR", "_VIEW", session)>
  <div class="col"><a href="<@ofbizUrl>findorders</@ofbizUrl>" class="${selectedLeftClassMap.findorders?default(unselectedLeftClassName)}">${uiLabelMap.OrderFindOrder}</a></div>  
  </#if>
  <#if (security.hasEntityPermission("ORDERMGR", "_CREATE", session) || security.hasEntityPermission("ORDERMGR", "_PURCHASE_CREATE", session))>
  <div class="col"><a href="<@ofbizUrl>orderentry</@ofbizUrl>" class="${selectedLeftClassMap.orderentry?default(unselectedLeftClassName)}">${uiLabelMap.OrderOrderEntry}</a></div>  
  </#if>
  <#if security.hasEntityPermission("ORDERMGR", "_RETURN", session)>
  <div class="col"><a href="<@ofbizUrl>findreturn</@ofbizUrl>" class="${selectedLeftClassMap.return?default(unselectedLeftClassName)}">${uiLabelMap.OrderOrderReturns}</a></div>
  </#if>
  <#if security.hasRolePermission("ORDERMGR", "_VIEW", "", "", session) || security.hasRolePermission("ORDERMGR_ROLE", "_VIEW", "", "", session)>
  <div class="col"><a href="<@ofbizUrl>FindRequirements</@ofbizUrl>" class="${selectedLeftClassMap.requirement?default(unselectedLeftClassName)}">${uiLabelMap.OrderRequirements}</a></div>
  <div class="col"><a href="<@ofbizUrl>tasklist</@ofbizUrl>" class="${selectedLeftClassMap.tasklist?default(unselectedLeftClassName)}">${uiLabelMap.OrderOrderTasks}</a></div>
  </#if>

  <#if userLogin?has_content>
    <div class="col-right"><a href="<@ofbizUrl>logout</@ofbizUrl>" class="${selectedRightClassMap.logout?default(unselectedRightClassName)}">${uiLabelMap.CommonLogout}</a></div>
  <#else>
    <div class="col-right"><a href='<@ofbizUrl>${checkLoginUrl?if_exists}</@ofbizUrl>' class='${selectedRightClassMap.login?default(unselectedRightClassName)}'>${uiLabelMap.CommonLogin}</a></div>
  </#if>  
  <div class="col-right"><a href='<@ofbizUrl>OrderPurchaseReportOptions</@ofbizUrl>' class="${selectedRightClassMap.reports?default(unselectedRightClassName)}">${uiLabelMap.CommonReports}</a></div>
  <div class="col-right"><a href='<@ofbizUrl>orderstats</@ofbizUrl>' class="${selectedRightClassMap.stats?default(unselectedRightClassName)}">${uiLabelMap.CommonStats}</a></div>
  <div class="col-fill">&nbsp;</div>
</div>
