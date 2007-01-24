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

<div class="tabContainer">
    <a href="<@ofbizUrl>day<#if eventsParam?has_content>?${eventsParam}</#if></@ofbizUrl>" class="${selectedClassMap.day?default(unselectedClassName)}">${uiLabelMap.WorkEffortDayView}</a>
    <a href="<@ofbizUrl>week<#if eventsParam?has_content>?${eventsParam}</#if></@ofbizUrl>" class="${selectedClassMap.week?default(unselectedClassName)}">${uiLabelMap.WorkEffortWeekView}</a>
    <a href="<@ofbizUrl>month<#if eventsParam?has_content>?${eventsParam}</#if></@ofbizUrl>" class="${selectedClassMap.month?default(unselectedClassName)}">${uiLabelMap.WorkEffortMonthView}</a>
    <a href="<@ofbizUrl>upcoming<#if eventsParam?has_content>?${eventsParam}</#if></@ofbizUrl>" class="${selectedClassMap.upcoming?default(unselectedClassName)}">${uiLabelMap.WorkEffortUpcomingEvents}</a>
</div>

<div>
    <a href="<@ofbizUrl>EditWorkEffort?workEffortTypeId=EVENT&currentStatusId=CAL_TENTATIVE</@ofbizUrl>" class="buttontext">${uiLabelMap.WorkEffortNewEvent}</a>
</div>
<br/>
