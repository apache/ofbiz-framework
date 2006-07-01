<#--
 *  Copyright (c) 2002-2005 The Open For Business Project - www.ofbiz.org
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
 */
-->
<#if (requestAttributes.uiLabelMap)?exists><#assign uiLabelMap = requestAttributes.uiLabelMap></#if>

<#assign unselectedClassName = "tabButton">
<#assign selectedClassMap = {page.tabButtonItem?default("void") : "tabButtonSelected"}>

<#if security.hasEntityPermission("MARKETING", "_VIEW", session)>
<#if segmentGroup?has_content>
<#-- Main Heading -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td align="right">
      <div class="tabContainer">
        <a href="<@ofbizUrl>EditContactList?contactListId=${contactListId}</@ofbizUrl>" class="${selectedClassMap.EditContactList?default(unselectedClassName)}">${uiLabelMap.ContactList}</a>
        <a href="<@ofbizUrl>ListContactListParty?contactListId=${contactListId}</@ofbizUrl>" class="${selectedClassMap.ContactListParty?default(unselectedClassName)}">${uiLabelMap.ContactListParty}</a>
        <a href="<@ofbizUrl>ListContactListCommEvent?contactListId=${contactListId}</@ofbizUrl>" class="${selectedClassMap.ContactListCommEvent?default(unselectedClassName)}">${uiLabelMap.ContactListCommEvent}</a>
      </div>
    </td>
  </tr>
 </table>

<#else>
  <div class="head2">${uiLabelMap.SegmentGroupNoSegmentGroupFoundWithId}: ${segmentGroupIdId?if_exists}</div>
</#if>
<#else>
  <div class="head2">${uiLabelMap.MarketingViewPermissionError}</div>
</#if>
