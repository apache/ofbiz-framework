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
<#include "component://webtools/template/tempexpr/TempExprMacros.ftl"/>
<h1>${title}</h1>
<#if temporalExpression?has_content>
  <#-- Edit existing expression -->
  <#if !"INTERSECTION.UNION.DIFFERENCE.SUBSTITUTION"?contains(temporalExpression.tempExprTypeId)>
    <form name="updateExpression" method="post" action="<@ofbizUrl>updateTemporalExpression</@ofbizUrl>">
      <input type="hidden" name="tempExprId" value="${temporalExpression.tempExprId}"/>
      <input type="hidden" name="tempExprTypeId" value="${temporalExpression.tempExprTypeId}"/>
  </#if>
  <table class="basic-table" cellspacing="0">
    <tr>
      <td class="label">${uiLabelMap.TemporalExpressionId}</td>
      <td>${temporalExpression.tempExprId}</td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.TemporalExpressionType}</td>
      <td>${uiLabelMap.get("TemporalExpression_" + temporalExpression.tempExprTypeId)}</td>
    </tr>
  <#if !"INTERSECTION.UNION.DIFFERENCE.SUBSTITUTION"?contains(temporalExpression.tempExprTypeId)>
    <tr>
      <td class="label">${uiLabelMap.CommonDescription}</td>
      <td><input type="text" name="description" value="${temporalExpression.description!}" maxlength="60" size="20"/></td>
    </tr>
  <#else>
    <tr>
      <td class="label">${uiLabelMap.CommonDescription}</td>
      <td>${temporalExpression.get("description",locale)!}</td>
    </tr>
  </#if>
    <#if temporalExpression.tempExprTypeId == "DATE_RANGE">
      <@DateRange formName="updateExpression" fromDate=temporalExpression.date1 toDate=temporalExpression.date2/>
    <#elseif temporalExpression.tempExprTypeId == "DAY_IN_MONTH">
      <@DayInMonth occurrence=temporalExpression.integer2 day=temporalExpression.integer1/>
    <#elseif temporalExpression.tempExprTypeId == "DAY_OF_MONTH_RANGE">
      <@DayOfMonthRange fromDay=temporalExpression.integer1 toDay=temporalExpression.integer2/>
    <#elseif temporalExpression.tempExprTypeId == "DAY_OF_WEEK_RANGE">
      <@DayOfWeekRange fromDay=temporalExpression.integer1 toDay=temporalExpression.integer2/>
    <#elseif temporalExpression.tempExprTypeId == "FREQUENCY">
      <@Frequency formName="updateExpression" fromDate=temporalExpression.date1 freqType=temporalExpression.integer1 freqValue=temporalExpression.integer2/>
    <#elseif temporalExpression.tempExprTypeId == "DAY_OF_WEEK_RANGE">
      <@DayOfWeekRange fromDay=temporalExpression.integer1 toDay=temporalExpression.integer2/>
    <#elseif temporalExpression.tempExprTypeId == "HOUR_RANGE">
      <@HourOfDayRange fromHour=temporalExpression.integer1 toHour=temporalExpression.integer2/>
    <#elseif temporalExpression.tempExprTypeId == "MINUTE_RANGE">
      <@MinuteRange fromMinute=temporalExpression.integer1 toMinute=temporalExpression.integer2/>
    <#elseif temporalExpression.tempExprTypeId == "MONTH_RANGE">
      <@MonthRange fromMonth=temporalExpression.integer1 toMonth=temporalExpression.integer2/>
    <#elseif temporalExpression.tempExprTypeId == "TIME_OF_DAY_RANGE">
      <@TimeOfDayRange fromTime=temporalExpression.string1 toTime=temporalExpression.string2 freqType=temporalExpression.integer1 freqValue=temporalExpression.integer2/>
    <#elseif "INTERSECTION.UNION.DIFFERENCE.SUBSTITUTION"?contains(temporalExpression.tempExprTypeId)>
      <#assign candidateIdList = Static["org.ofbiz.service.calendar.ExpressionUiHelper"].getCandidateIncludeIds(delegator, temporalExpression.tempExprId)/>
      <#if "INTERSECTION.UNION"?contains(temporalExpression.tempExprTypeId)>
        <tr>
          <td class="label">${uiLabelMap.TemporalExpressionInclude}</td>
          <td><@CreateExprAssocForm formName="includeExpression"/></td>
        </tr>
      <#else>
        <#assign hasInclude = false hasExclude = false hasSubstitution = false/>
        <#if childExpressionList?has_content>
          <#list childExpressionList as childExpression>
            <#if childExpression.exprAssocType == "INCLUDE">
              <#assign hasInclude = true/>
            <#elseif childExpression.exprAssocType == "EXCLUDE">
              <#assign hasExclude = true/>
            <#elseif childExpression.exprAssocType == "SUBSTITUTION">
              <#assign hasSubstitution = true/>
            </#if>
          </#list>
        </#if>
        <#if !hasInclude>
          <tr>
            <td class="label">${uiLabelMap.TemporalExpressionInclude}</td>
            <td><@CreateExprAssocForm formName="includeExpression" exprAssocType="INCLUDE"/></td>
          </tr>
        </#if>
        <#if !hasExclude>
          <tr>
            <td class="label">${uiLabelMap.TemporalExpressionExclude}</td>
            <td><@CreateExprAssocForm formName="excludeExpression" exprAssocType="EXCLUDE"/></td>
          </tr>
        </#if>
        <#if !hasSubstitution && temporalExpression.tempExprTypeId == "SUBSTITUTION">
          <tr>
            <td class="label">${uiLabelMap.TemporalExpression_SUBSTITUTION}</td>
            <td><@CreateExprAssocForm formName="substitutionExpression" exprAssocType="SUBSTITUTION"/></td>
          </tr>
        </#if>
      </#if>
    </#if>
    <#if !"INTERSECTION.UNION.DIFFERENCE.SUBSTITUTION"?contains(temporalExpression.tempExprTypeId)>
        <tr>
          <td>&nbsp;</td>
          <td><input type="submit" name="submitBtn" value="${uiLabelMap.CommonSave}"/></td>
        </tr>
      </table>
      </form>
    <#else>
      </table>    
    </#if>
<#else>
  <#-- Create new expression -->
  <@CreateForm "DATE_RANGE" CreateDateRange/>
  <hr />
  <@CreateForm "DAY_IN_MONTH" DayInMonth/>
  <hr />
  <@CreateForm "DAY_OF_MONTH_RANGE" DayOfMonthRange/>
  <hr />
  <@CreateForm "DAY_OF_WEEK_RANGE" DayOfWeekRange/>
  <hr />
  <@CreateForm "FREQUENCY" CreateFrequency/>
  <hr />
  <@CreateForm "HOUR_RANGE" HourOfDayRange/>
  <hr />
  <@CreateForm "MINUTE_RANGE" MinuteRange/>
  <hr />
  <@CreateForm "MONTH_RANGE" MonthRange/>
  <hr />
  <@CreateForm "TIME_OF_DAY_RANGE" TimeOfDayRange/>
  <hr />
  <@CreateForm "INTERSECTION"/>
  <hr />
  <@CreateForm "UNION"/>
  <hr />
  <@CreateForm "DIFFERENCE"/>
  <hr />
  <@CreateForm "SUBSTITUTION"/>
</#if>

<#macro CreateForm expressionTypeId="" formContents=NullMacro>
  <form name="${expressionTypeId}" method="post" action="<@ofbizUrl>createTemporalExpression</@ofbizUrl>">
    <input type="hidden" name="tempExprTypeId" value="${expressionTypeId}"/>
    <table class="basic-table" cellspacing="0">
      <#assign mapExpression = "TemporalExpression_" + expressionTypeId/>
      <#assign headingText = uiLabelMap[mapExpression]/>
      <tr><td colspan="2" class="h2">${headingText}</td></tr>
      <tr>
        <td class="label">${uiLabelMap.TemporalExpressionId}</td>
        <td><input name="tempExprId" type="text" maxlength="20" size="20"/><span class="tooltip">${uiLabelMap.CommonAutoAssignedId}</span></td>
      </tr>
      <tr>
        <td class="label">${uiLabelMap.CommonDescription}</td>
        <td><input name="description" type="text" maxlength="60" size="20"/></td>
      </tr>
      <@formContents/>
      <tr>
        <td>&nbsp;</td>
        <td><input type="submit" name="submitBtn" value="${uiLabelMap.CommonSave}"/></td>
      </tr>
    </table>
  </form>
</#macro>

<#macro CreateDateRange>
  <@DateRange formName="DATE_RANGE"/>
</#macro>

<#macro CreateFrequency>
  <@Frequency formName="FREQUENCY"/>
</#macro>

<#macro CreateExprAssocForm formName="" exprAssocType="">
  <form name="${formName}" method="post" action="<@ofbizUrl>createTemporalExpressionAssoc</@ofbizUrl>">
    <input type="hidden" name="tempExprId" value="${temporalExpression.tempExprId}"/>
    <input type="hidden" name="fromTempExprId" value="${temporalExpression.tempExprId}"/>
    <input type="hidden" name="exprAssocType" value="${exprAssocType}"/>
    <select name="toTempExprId">
      <#list candidateIdList as candidate>
        <option value="${candidate}">${candidate}</option>
      </#list>
    </select>
    <input type="submit" name="submitBtn" value="${uiLabelMap.CommonSave}"/>
  </form>
</#macro>
