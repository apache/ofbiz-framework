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
<#include "component://common/template/includes/commonMacros.ftl"/>
-->

<#assign
  dayValueList = Static["org.ofbiz.service.calendar.ExpressionUiHelper"].getDayValueList(locale)
  monthValueList = Static["org.ofbiz.service.calendar.ExpressionUiHelper"].getMonthValueList(locale)
/>

<#macro NullMacro></#macro>

<#macro DateField formName="" fieldName="" fieldValue="" fieldClass="">
  <#if javaScriptEnabled>
    <@htmlTemplate.renderDateTimeField name="${fieldName}" event="${event!}" action="${action!}" className="${fieldClass!''}" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="${fieldValue!''}" size="25" maxlength="30" id="${fieldName}1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
  <#else>
      <input type="text" name="${fieldName}"<#if fieldValue?has_content> value="${fieldValue}"</#if><#if fieldClass?has_content> class="${fieldClass}"</#if> maxlength="25" size="25"/>
  </#if>
  <span class="tooltip">${uiLabelMap.CommonFormatDateTime}</span>
  <#if fieldClass == "required">
    <span class="tooltip">${uiLabelMap.CommonRequired}</span>
  </#if>
</#macro>

<#macro MonthField fieldName="" fieldValue=-1 fieldClass="">
  <select name="${fieldName}"<#if fieldClass?has_content> class="${fieldClass}"</#if>>
    <#list monthValueList as monthValue>
      <option value="${monthValue.value}"<#if monthValue.value == fieldValue> selected="selected"</#if>>${monthValue.description}</option>
    </#list>
  </select>
  <#if fieldClass == "required">
    <span class="tooltip">${uiLabelMap.CommonRequired}</span>
  </#if>
</#macro>

<#macro HourOfDayField fieldName="" fieldValue=-1 fieldClass="">
  <select name="${fieldName}"<#if fieldClass?has_content> class="${fieldClass}"</#if>>
    <#list 0..23 as i>
      <option value="${i}"<#if i == fieldValue> selected="selected"</#if>>${i}</option>
    </#list>
  </select>
  <#if fieldClass == "required">
    <span class="tooltip">${uiLabelMap.CommonRequired}</span>
  </#if>
</#macro>

<#macro MinuteField fieldName="" fieldValue=-1 fieldClass="">
  <select name="${fieldName}"<#if fieldClass?has_content> class="${fieldClass}"</#if>>
    <#list 0..59 as i>
      <option value="${i}"<#if i == fieldValue> selected="selected"</#if>>${i}</option>
    </#list>
  </select>
  <#if fieldClass == "required">
    <span class="tooltip">${uiLabelMap.CommonRequired}</span>
  </#if>
</#macro>

<#macro DayOfWeekField fieldName="" fieldValue=-1 fieldClass="">
  <select name="${fieldName}"<#if fieldClass?has_content> class="${fieldClass}"</#if>>
    <#list dayValueList as dayValue>
      <option value="${dayValue.value}"<#if dayValue.value == fieldValue> selected="selected"</#if>>${dayValue.description}</option>
    </#list>
  </select>
  <#if fieldClass == "required">
    <span class="tooltip">${uiLabelMap.CommonRequired}</span>
  </#if>
</#macro>

<#macro DayOfMonthField fieldName="" fieldValue=-1 fieldClass="">
  <select name="${fieldName}"<#if fieldClass?has_content> class="${fieldClass}"</#if>>
    <#list 1..31 as i>
      <option value="${i}"<#if i == fieldValue> selected="selected"</#if>>${i}</option>
    </#list>
  </select>
  <#if fieldClass == "required">
    <span class="tooltip">${uiLabelMap.CommonRequired}</span>
  </#if>
</#macro>

<#macro fieldErrors fieldName>
  <#if errorMessageList?has_content>
    <#assign fieldMessages = Static["org.ofbiz.base.util.MessageString"].getMessagesForField(fieldName, true, errorMessageList)>
    <ul>
      <#list fieldMessages as errorMsg>
        <li class="errorMessage">${errorMsg}</li>
      </#list>
    </ul>
  </#if>
</#macro>

<#macro fieldErrorsMulti fieldName1 fieldName2 fieldName3 fieldName4>
  <#if errorMessageList?has_content>
    <#assign fieldMessages = Static["org.ofbiz.base.util.MessageString"].getMessagesForField(fieldName1, fieldName2, fieldName3, fieldName4, true, errorMessageList)>
    <ul>
      <#list fieldMessages as errorMsg>
        <li class="errorMessage">${errorMsg}</li>
      </#list>
    </ul>
  </#if>
</#macro>
