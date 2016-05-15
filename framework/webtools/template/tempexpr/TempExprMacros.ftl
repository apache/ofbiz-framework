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

<#--
To use these macros in your template, insert the following line in
your template file:
<#include "component://common/template/includes/empExprMacros.ftl"/>
-->

<#include "component://common/template/includes/commonMacros.ftl"/>

<#assign
  occurrenceList = Static["org.ofbiz.service.calendar.ExpressionUiHelper"].getOccurrenceList()
  frequencyTypeList = Static["org.ofbiz.service.calendar.ExpressionUiHelper"].getFrequencyValueList(uiLabelMap)
  firstDayOfWeek = Static["org.ofbiz.service.calendar.ExpressionUiHelper"].getFirstDayOfWeek(locale)
  lastDayOfWeek = Static["org.ofbiz.service.calendar.ExpressionUiHelper"].getLastDayOfWeek(locale)
/>

<#macro DateRange formName="" fromDate="" toDate="">
  <tr>
    <td class="label">${uiLabelMap.CommonFrom}</td>
    <td>
      <@DateField formName=formName fieldName="date1" fieldValue=fromDate/>
    </td>
  </tr>
  <tr>
    <td class="label">${uiLabelMap.CommonTo}</td>
    <td>
      <@DateField formName=formName fieldName="date2" fieldValue=toDate/>
    </td>
  </tr>
</#macro>

<#macro DayInMonth occurrence=0 day=firstDayOfWeek>
  <tr>
    <td class="label">${uiLabelMap.TemporalExpressionOccurrence}</td>
    <td>
      <select name="integer2">
        <#list 1..5 as i>
          <option value="${i}"<#if i == occurrence> selected="selected"</#if>>${i}</option>
        </#list>
        <#list -1..-5 as i>
          <option value="${i}"<#if i == occurrence> selected="selected"</#if>>${i}</option>
        </#list>
      </select>
    </td>
  </tr>
  <tr>
    <td class="label">${uiLabelMap.CommonDay}</td>
    <td><@DayOfWeekField fieldName="integer1" fieldValue=day/></td>
  </tr>
</#macro>

<#macro DayOfMonthRange fromDay=1 toDay=31>
  <tr>
    <td class="label">${uiLabelMap.CommonFrom}</td>
    <td><@DayOfMonthField fieldName="integer1" fieldValue=fromDay/></td>
  </tr>
  <tr>
    <td class="label">${uiLabelMap.CommonTo}</td>
    <td><@DayOfMonthField fieldName="integer2" fieldValue=toDay/></td>
  </tr>
</#macro>

<#macro HourOfDayRange fromHour=1 toHour=23>
  <tr>
    <td class="label">${uiLabelMap.CommonFrom}</td>
    <td><@HourOfDayField fieldName="integer1" fieldValue=fromHour/></td>
  </tr>
  <tr>
    <td class="label">${uiLabelMap.CommonTo}</td>
    <td><@HourOfDayField fieldName="integer2" fieldValue=toHour/></td>
  </tr>
</#macro>

<#macro DayOfWeekRange fromDay=firstDayOfWeek toDay=lastDayOfWeek>
  <tr>
    <td class="label">${uiLabelMap.CommonFrom}</td>
    <td><@DayOfWeekField fieldName="integer1" fieldValue=fromDay/></td>
  </tr>
  <tr>
    <td class="label">${uiLabelMap.CommonTo}</td>
    <td><@DayOfWeekField fieldName="integer2" fieldValue=toDay/></td>
  </tr>
</#macro>

<#macro Frequency formName="" fromDate="" freqType=-1 freqValue=0>
  <tr>
    <td class="label">${uiLabelMap.CommonFrom}</td>
    <td>
      <@DateField formName=formName fieldName="date1" fieldValue=fromDate/>
    </td>
  </tr>
  <tr>
    <td class="label">${uiLabelMap.TemporalExpressionFreqType}</td>
    <td>
      <select name="integer1">
        <#list frequencyTypeList as freqTypeItem>
          <option value="${freqTypeItem.value}"<#if freqTypeItem.value == freqType> selected="selected"</#if>>${freqTypeItem.description}</option>
        </#list>
      </select>
    </td>
  </tr>
  <tr>
    <td class="label">${uiLabelMap.TemporalExpressionFreqCount}</td>
    <td>
      <select name="integer2">
        <#list 1..50 as i>
          <option value="${i}"<#if i == freqValue> selected="selected"</#if>>${i}</option>
        </#list>
      </select>
    </td>
  </tr>
</#macro>

<#macro MinuteRange fromMinute=1 toMinute=59>
  <tr>
    <td class="label">${uiLabelMap.CommonFrom}</td>
    <td><@MinuteField fieldName="integer1" fieldValue=fromMinute/></td>
  </tr>
  <tr>
    <td class="label">${uiLabelMap.CommonTo}</td>
    <td><@MinuteField fieldName="integer2" fieldValue=toMinute/></td>
  </tr>
</#macro>

<#macro MonthRange fromMonth=0 toMonth=11>
  <tr>
    <td class="label">${uiLabelMap.CommonFrom}</td>
    <td><@MonthField fieldName="integer1" fieldValue=fromMonth/></td>
  </tr>
  <tr>
    <td class="label">${uiLabelMap.CommonTo}</td>
    <td><@MonthField fieldName="integer2" fieldValue=toMonth/></td>
  </tr>
</#macro>

<#macro TimeOfDayRange fromTime="" toTime="" freqType=11 freqValue=1>
  <tr>
    <td class="label">${uiLabelMap.CommonFrom}</td>
    <td><input type="text" name="string1" value="${fromTime}" maxlength="8" size="8"/><span class="tooltip">${uiLabelMap.TemporalExpressionTimeFormat}</span></td>
  </tr>
  <tr>
    <td class="label">${uiLabelMap.CommonTo}</td>
    <td><input type="text" name="string2" value="${toTime}" maxlength="8" size="8"/><span class="tooltip">${uiLabelMap.TemporalExpressionTimeFormat}</span></td>
  </tr>
  <tr>
    <td class="label">${uiLabelMap.TemporalExpressionFreqType}</td>
    <td>
      <select name="integer1">
        <option value="13"<#if freqType == 13> selected="selected"</#if>>${uiLabelMap.CommonSecond}</option>
        <option value="12"<#if freqType == 12> selected="selected"</#if>>${uiLabelMap.CommonMinute}</option>
        <option value="11"<#if freqType == 11> selected="selected"</#if>>${uiLabelMap.CommonHour}</option>
      </select>
    </td>
  </tr>
  <tr>
    <td class="label">${uiLabelMap.TemporalExpressionFreqCount}</td>
    <td><input type="text" name="integer2" value="${freqValue}" maxlength="8" size="8"/></td>
  </tr>
</#macro>
