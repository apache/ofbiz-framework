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
 *@author     Olivier Heintz (olivier.heintz@nereide.biz) 
 *@version    $Rev$
 *@since      2.2
-->

  <#assign unselectedClassName = "tabButton">
  <#assign selectedClassMap = {page.tabButtonItem?default("void") : "tabButtonSelected"}>
  <#if groupId?has_content>
    <div class='tabContainer'>        
      <a href="<@ofbizUrl>EditSecurityGroup?groupId=${groupId}</@ofbizUrl>" class="${selectedClassMap.EditSecurityGroup?default(unselectedClassName)}">${uiLabelMap.PartySecurityGroups}</a>
	  <a href="<@ofbizUrl>EditSecurityGroupPermissions?groupId=${groupId}</@ofbizUrl>" class="${selectedClassMap.EditSecurityGroupPermissions?default(unselectedClassName)}">${uiLabelMap.PartyPermissions}</a>
	  <a href="<@ofbizUrl>EditSecurityGroupUserLogins?groupId=${groupId}</@ofbizUrl>" class="${selectedClassMap.EditSecurityGroupUserLogins?default(unselectedClassName)}">${uiLabelMap.PartyUserLogins}</a>  
    </div>
  </#if>
