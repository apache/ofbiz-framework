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
 *@author     Jacopo Cappellato (jacopo.cappellato@sastau.it)
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
