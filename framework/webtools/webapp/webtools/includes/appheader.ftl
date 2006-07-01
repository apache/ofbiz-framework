<#--
 *  Copyright (c) 2003-2005 The Open For Business Project - www.ofbiz.org
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
 *@author     Catherine.Heintz@nereide.biz (migration to UiLabel)
 *@version    $Rev$
 *@since      2.1
-->
<#if (requestAttributes.uiLabelMap)?exists><#assign uiLabelMap = requestAttributes.uiLabelMap></#if>
<#if (requestAttributes.security)?exists><#assign security = requestAttributes.security></#if>
<#if (requestAttributes.userLogin)?exists><#assign userLogin = requestAttributes.userLogin></#if>
<#if (requestAttributes.checkLoginUrl)?exists><#assign checkLoginUrl = requestAttributes.checkLoginUrl></#if>

<#assign unselectedLeftClassName = "headerButtonLeft">
<#assign unselectedRightClassName = "headerButtonRight">
<#assign selectedLeftClassMap = {(page.headerItem)?default("void") : "headerButtonLeftSelected"}>
<#assign selectedRightClassMap = {(page.headerItem)?default("void") : "headerButtonRightSelected"}>

<div class="apptitle">${uiLabelMap.FrameworkWebTools}</div>
<div class="row">
  <div class="col"><a href="<@ofbizUrl>main</@ofbizUrl>" class="headerButtonLeft">${uiLabelMap.CommonMain}</a></div>
  <#--
  <div class="col"><a href="<@ofbizUrl>FindUtilCache</@ofbizUrl>" class="headerButtonLeft">Cache</a></div>  
  <div class="col"><a href="<@ofbizUrl>entitymaint</@ofbizUrl>" class="headerButtonLeft">Entity</a></div>  
  <div class="col"><a href="<@ofbizUrl>availableServices</@ofbizUrl>" class="headerButtonLeft">Service</a></div>  
  <div class="col"><a href="<@ofbizUrl>workflowMonitor</@ofbizUrl>" class="headerButtonLeft">Workflow</a></div>  
  <div class="col"><a href="<@ofbizUrl>viewdatafile</@ofbizUrl>" class="headerButtonLeft">Data</a></div>  
  <div class="col"><a href="<@ofbizUrl>EditCustomTimePeriod</@ofbizUrl>" class="headerButtonLeft">Misc</a></div>  
  <div class="col"><a href="<@ofbizUrl>StatsSinceStart</@ofbizUrl>" class="headerButtonLeft">Statistics</a></div>  
  -->
  <#if userLogin?has_content>
    <div class="col-right"><a href="<@ofbizUrl>logout</@ofbizUrl>" class="headerButtonRight">${uiLabelMap.CommonLogout}</a></div>
  <#else>
    <div class="col-right"><a href='<@ofbizUrl>${checkLoginUrl?if_exists}</@ofbizUrl>' class='headerButtonRight'>${uiLabelMap.CommonLogin}</a></div>
  </#if>
  <div class="col-fill">&nbsp;</div>
</div>
