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
 *@author     Brett G. Palmer (brettgpalmer@gmail.com)
 *@version    $Rev$
 *@since      2.2
-->
<#if (requestAttributes.uiLabelMap)?exists><#assign uiLabelMap = requestAttributes.uiLabelMap></#if>
<#assign unselectedClassName = "tabButton">
<#assign selectedClassMap = {page.tabButtonItem?default("void") : "tabButtonSelected"}>

<div class="tabContainer">
    <a href="<@ofbizUrl>FindGlobalGlAccount</@ofbizUrl>" class="${selectedClassMap.FindGlobalGlAccount?default(unselectedClassName)}">${uiLabelMap.AcctgChartOfAcctsTabMenu}</a>
    <a href="<@ofbizUrl>GlAccountNavigate</@ofbizUrl>" class="${selectedClassMap.GlAccountNavigate?default(unselectedClassName)}">${uiLabelMap.AcctgNavigateAccts}</a>
    <a href="<@ofbizUrl>AssignGlAccount</@ofbizUrl>" class="${selectedClassMap.AssignGlAccount?default(unselectedClassName)}">${uiLabelMap.AcctgAssignGlAccount}</a>
    <a href="<@ofbizUrl>EditGlJournalEntry</@ofbizUrl>" class="${selectedClassMap.EditGlJournalEntry?default(unselectedClassName)}">${uiLabelMap.AcctgEditGlJournalEntry}</a>
    <a href="<@ofbizUrl>ListGlAccountOrganization</@ofbizUrl>" class="${selectedClassMap.ListGlAccountOrganization?default(unselectedClassName)}">${uiLabelMap.AcctgListGlAcctOrg}</a>
</div>
