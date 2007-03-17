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

<h1>
    ${uiLabelMap.ManufacturingEditCalendarExceptionWeekFor}&nbsp; 
    <#if (techDataCalendar.description)?has_content>"${(techDataCalendar.get("description",locale))}"</#if> 
    [${uiLabelMap.CommonId}:${techDataCalendar.calendarId?if_exists}]
</h1>
<br/>
<#if techDataCalendar?has_content>
    ${listCalendarExceptionWeekWrapper.renderFormString(context)}
    <br/>
    <hr/>
    <#if calendarExceptionWeek?has_content>
        ${updateCalendarExceptionWeekWrapper.renderFormString(context)}
        <br/>
        <hr/>
    </#if>
    ${addCalendarExceptionWeekWrapper.renderFormString(context)}
</#if>
<br/>