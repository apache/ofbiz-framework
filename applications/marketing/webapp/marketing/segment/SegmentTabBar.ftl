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
 * @author     Al Byers
 * @author     David E. Jones
-->
<#assign unselectedClassName = "tabButton">
<#assign selectedClassMap = {page.tabButtonItem?default("void") : "tabButtonSelected"}>

<#if security.hasEntityPermission("MARKETING", "_VIEW", session)>
<#if segmentGroup?has_content>
<#-- Main Heading -->
  <div class="tabContainer">
    <a href="<@ofbizUrl>viewSegmentGroup?segmentGroupId=${segmentGroupId}</@ofbizUrl>" class="${selectedClassMap.viewSegmentGroup?default(unselectedClassName)}">${uiLabelMap.SegmentGroupSegmentGroup}</a>
    <a href="<@ofbizUrl>listSegmentGroupClass?segmentGroupId=${segmentGroupId}</@ofbizUrl>" class="${selectedClassMap.listSegmentGroupClassification?default(unselectedClassName)}">${uiLabelMap.SegmentGroupSegmentGroupClassification}</a>
    <a href="<@ofbizUrl>listSegmentGroupGeo?segmentGroupId=${segmentGroupId}</@ofbizUrl>" class="${selectedClassMap.listSegmentGroupGeo?default(unselectedClassName)}">${uiLabelMap.SegmentGroupSegmentGroupGeo}</a>
    <a href="<@ofbizUrl>listSegmentGroupRole?segmentGroupId=${segmentGroupId}</@ofbizUrl>" class="${selectedClassMap.listSegmentGroupRole?default(unselectedClassName)}">${uiLabelMap.SegmentGroupSegmentGroupRole}</a>
  </div>

<#else>
  <div class="head2">${uiLabelMap.SegmentGroupNoSegmentGroupFoundWithId}: ${segmentGroupIdId?if_exists}</div>
</#if>
<#else>
  <div class="head2">${uiLabelMap.MarketingViewPermissionError}</div>
</#if>
