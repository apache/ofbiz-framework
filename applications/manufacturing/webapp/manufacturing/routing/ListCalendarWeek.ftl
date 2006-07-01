<#--

Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->

<div class="tabContainer">
    <a href="<@ofbizUrl>FindCalendar</@ofbizUrl>" class="tabButton">${uiLabelMap.ManufacturingCalendar}</a>
    <a href="<@ofbizUrl>ListCalendarWeek</@ofbizUrl>" class="tabButtonSelected">${uiLabelMap.ManufacturingCalendarWeek}</a>
</div>
<div class="head1">${uiLabelMap.ManufacturingListCalendarWeek}&nbsp; </div>
<div>
    <a href="<@ofbizUrl>EditCalendarWeek</@ofbizUrl>" class="buttontext">${uiLabelMap.ManufacturingNewCalendarWeek}</a>
</div>
<br/>
<#if allCalendarWeek?has_content>
    ${listCalendarWeekWrapper.renderFormString(context)} 
</#if>
<br/>