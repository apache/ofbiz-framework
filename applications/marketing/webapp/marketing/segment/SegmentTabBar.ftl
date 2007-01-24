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
