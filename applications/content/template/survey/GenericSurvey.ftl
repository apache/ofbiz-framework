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

<#macro renderSurveyQuestionText surveyQuestionAndAppl>
  <div>${surveyQuestionAndAppl.question!}</div>
  <#if surveyQuestionAndAppl.hint?has_content>
    <div>${surveyQuestionAndAppl.hint}</div>
  </#if>
</#macro>

<#macro renderSurveyQuestionRequired surveyQuestionAndAppl>
  <#if surveyQuestionAndAppl.requiredField?default("N") == "Y">
    <span>*[required]</span>
  <#else/>
    <span>[optional]</span>
  </#if>
</#macro>

<#macro renderSurveyQuestionInput surveyQuestionAndAppl questionFieldName>
  <#if surveyQuestionAndAppl.surveyQuestionTypeId == "BOOLEAN">
    <#assign selectedOption = (answer.booleanResponse)?default("Y")>
    <select name="${questionFieldName}">
      <#if surveyQuestionAndAppl.requiredField?default("N") != "Y">
        <option value=""></option>
      </#if>
      <option <#if "Y" == selectedOption>selected="selected"</#if>>Y</option>
      <option <#if "N" == selectedOption>selected="selected"</#if>>N</option>
    </select>
  <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "TEXTAREA"/>
    <textarea cols="40" rows="5" name="${questionFieldName}">${(answer.textResponse)!}</textarea>
  <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "TEXT_SHORT"/>
    <input type="text" size="15" class="inputBox" name="${questionFieldName}" value="${(answer.textResponse)?default(defValue!)}" />
  <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "TEXT_LONG"/>
    <input type="text" size="35" class="inputBox" name="${questionFieldName}" value="${(answer.textResponse)?default(defValue!)}" />
  <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "EMAIL"/>
    <input type="text" size="30" class="inputBox" name="${questionFieldName}" value="${(answer.textResponse)?default(defValue!)}" />
  <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "URL"/>
    <input type="text" size="40" class="inputBox" name="${questionFieldName}" value="${(answer.textResponse)?default(defValue!)}" />
  <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "DATE"/>
    <input type="text" size="12" class="inputBox" name="${questionFieldName}" value="${(answer.textResponse)?default(defValue!)}" />
  <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "CREDIT_CARD"/>
    <input type="text" size="20" class="inputBox" name="${questionFieldName}" value="${(answer.textResponse)?default(defValue!)}" />
  <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "GIFT_CARD"/>
    <input type="text" size="20" class="inputBox" name="${questionFieldName}" value="${(answer.textResponse)?default(defValue!)}" />
  <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "NUMBER_CURRENCY"/>
    <input type="text" size="6" class="inputBox" name="${questionFieldName}" value="${(answer.currencyResponse)?default(defValue!)}" />
  <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "NUMBER_FLOAT"/>
    <input type="text" size="6" class="inputBox" name="${questionFieldName}" value="${(answer.floatResponse)?default(defValue!)}" />
  <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "NUMBER_LONG"/>
    <input type="text" size="6" class="inputBox" name="${questionFieldName}" value="${(answer.numericResponse?default(defValue)?string("#"))!}" />
  <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "PASSWORD"/>
    <input type="password" size="30" class="textBox" name="${questionFieldName}" value="${(answer.textResponse)?default(defValue!)}" />
  <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "CONTENT"/>
     <#if (answer.contentId)?has_content>
      <#assign content = answer.getRelatedOne("Content", false)>
      <a href="/content/control/img?imgId=${content.dataResourceId}" class="buttontext">${answer.contentId}</a>&nbsp;-&nbsp;${content.contentName!}&nbsp;&nbsp;&nbsp;
    </#if>
    <input type="file" size="15" name="${questionFieldName}" class="inputBox"/>
  <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "OPTION"/>
    <#assign options = surveyQuestionAndAppl.getRelated("SurveyQuestionOption", null, sequenceSort, false)!/>
    <#assign selectedOption = (answer.surveyOptionSeqId)?default("_NA_")/>
    <select name="${questionFieldName}">
      <#if surveyQuestionAndAppl.requiredField?default("N") != "Y">
        <option value=""></option>
      </#if>
      <#if options?has_content>
        <#list options as option>
          <option value="${option.surveyOptionSeqId}" <#if option.surveyOptionSeqId == selectedOption>selected="selected"</#if>>${option.description!}</option>
        </#list>
      <#else>
        <option value="">Nothing to choose</option>
      </#if>
    </select>
  <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "ENUMERATION"/>
    <select name="${questionFieldName}">
    <#assign formatString = surveyQuestionAndAppl.get("formatString")!/>
    <#assign enums = surveyQuestionAndAppl.getRelated("Enumeration", null, null, false)/>
    <#list enums as enum>
        <#assign selected = ''/>
        <#if (((answer.textResponse)?has_content && answer.textResponse == enum.enumId) || (defValue == enum.enumId))>
            <#assign selected = 'selected'/>
        </#if>
        <#if (formatString?has_content)>
            <#assign description = Static["org.ofbiz.base.util.string.FlexibleStringExpander"].expandString(formatString, enum)/>
        <#else>
            <#assign description = enum.getString("description")/>
        </#if>
        <option value='${enum.enumId}' ${selected}>${description}</option>
    </#list>
    </select>
  <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "GEO"/>
    <select name="${questionFieldName}">
    <#assign formatString = surveyQuestionAndAppl.get("formatString")!/>
    <#assign parentGeoId = surveyQuestionAndAppl.get("geoId")!/>
    <#assign geos = Static["org.ofbiz.common.geo.GeoWorker"].expandGeoGroup(parentGeoId, delegator)>
    <#list geos as geo>
          <#assign selected = ''/>
        <#if (((answer.textResponse)?has_content && answer.textResponse == geo.geoId) || (defValue == geo.geoId))>
          <#assign selected = 'selected'/>
        </#if>
        <#if (formatString?has_content)>
            <#assign description = Static["org.ofbiz.base.util.string.FlexibleStringExpander"].expandString(formatString, geo)/>
        <#else>
            <#assign description = geo.getString("geoName")/>
        </#if>
        <option value='${geo.geoId}' ${selected}>${description}</option>
    </#list>
    </select>
  <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "STATE_PROVINCE"/>
    <select name="${questionFieldName}">
    <#assign states = Static["org.ofbiz.common.CommonWorkers"].getStateList(delegator)>
    <#list states as state>
        <option value='${state.geoId}'>${state.geoName?default(state.geoId)}</option>
    </#list>
    </select>
  <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "COUNTRY"/>
    <select name="${questionFieldName}">
      ${screens.render("component://common/widget/CommonScreens.xml#countries")}
    </select>
  <#else/>
    <div>Unsupported question type : ${surveyQuestionAndAppl.surveyQuestionTypeId}</div>
  </#if>
</#macro>

<#if additionalFields?has_content>
  <#list additionalFields.keySet() as field>
    <input type="hidden" name="${field}" value="${additionalFields.get(field)}"/>
  </#list>
</#if>

<#-- update response -->
<#if surveyResponseId?has_content>
  <input type="hidden" name="surveyResponseId" value="${surveyResponseId}"/>
</#if>

<#-- party ID -->
<#if partyId?has_content>
  <input type="hidden" name="partyId" value="${partyId}"/>
</#if>

<#-- survey ID -->
<input type="hidden" name="surveyId" value="${survey.surveyId}"/>

<h1>${survey.description!}</h1>
<br />

<#if survey.comments?has_content>
<div>${survey.comments}</div>
<br />
</#if>

<table width="100%" border="0" cellpadding="2" cellspacing="0">
  <#assign lastSurveyMultiRespId = ""/>
  <#assign haveOpenMultiRespHeader = false/>

  <#list surveyQuestionAndAppls as surveyQuestionAndAppl>
   <#if !alreadyShownSqaaPkWithColId.contains(surveyQuestionAndAppl.getPrimaryKey())>
    <#-- Get and setup MultiResp info for this question -->
    <#assign openMultiRespHeader = false/>
    <#assign closeMultiRespHeader = false/>
    <#assign surveyMultiResp = surveyQuestionAndAppl.getRelatedOne("SurveyMultiResp", true)!/>
    <#if surveyMultiResp?has_content>
      <#assign surveyMultiRespColumnList = surveyMultiResp.getRelated("SurveyMultiRespColumn", null, Static["org.ofbiz.base.util.UtilMisc"].toList("sequenceNum"), true)/>

      <#if lastSurveyMultiRespId == "">
        <#assign openMultiRespHeader = true/>
      <#elseif lastSurveyMultiRespId != surveyMultiResp.surveyMultiRespId>
        <#assign openMultiRespHeader = true/>
        <#assign closeMultiRespHeader = true/>
      </#if>
      <#assign lastSurveyMultiRespId = surveyMultiResp.surveyMultiRespId/>
    <#else/>
      <#if lastSurveyMultiRespId?has_content><#assign closeMultiRespHeader = true/></#if>
      <#assign lastSurveyMultiRespId = ""/>
    </#if>

    <#-- this is before the rest because it will be done if the current row is not a MultiResp (or is different MultiResp) but the last row was... -->
    <#if closeMultiRespHeader>
      <#assign haveOpenMultiRespHeader = false/>
          </table>
        </td>
      </tr>
    </#if>

    <#-- -->
    <#if openMultiRespHeader>
      <#assign haveOpenMultiRespHeader = true/>
      <tr width="100%">
        <td colspan="5" width="100%">
          <table width="100%" border="1" cellpadding="1" cellspacing="0">
            <tr>
              <td>
                <div class="tableheadtext">${surveyMultiResp.multiRespTitle?default("&nbsp;")}</div>
              </td>
              <#list surveyMultiRespColumnList as surveyMultiRespColumn>
                <td align="center">
                  <div class="tableheadtext">${surveyMultiRespColumn.columnTitle?default("&nbsp;")}</div>
                </td>
              </#list>
              <td><div class="tableheadtext">Required?</div></td><#-- placeholder for required/optional column -->
            </tr>
    </#if>

  <#if surveyMultiResp?has_content>
    <#assign sqaaWithColIdList = (sqaaWithColIdListByMultiRespId[surveyMultiResp.surveyMultiRespId])!/>
    <tr>
      <td>
        <@renderSurveyQuestionText surveyQuestionAndAppl=surveyQuestionAndAppl/>
      </td>
      <#list surveyMultiRespColumnList as surveyMultiRespColumn>
        <td align="center">
          <#--
            if there is a surveyMultiRespColId on the surveyQuestionAndAppl use the corresponding surveyQuestionId;
            these should be in the same order as the surveyQuestionAndAppls List, so just see if it matches the first in the list
          -->
          <#if sqaaWithColIdList?has_content><#assign nextSqaaWithColId = sqaaWithColIdList?first/><#else/><#assign nextSqaaWithColId = []></#if>
          <#if surveyQuestionAndAppl.surveyMultiRespColId?has_content &&
              nextSqaaWithColId?has_content &&
              nextSqaaWithColId.surveyMultiRespColId = surveyMultiRespColumn.surveyMultiRespColId>
            <#assign dummySqaaWithColId = Static["org.ofbiz.base.util.UtilMisc"].removeFirst(sqaaWithColIdList)/>
            <#assign changed = alreadyShownSqaaPkWithColId.add(nextSqaaWithColId.getPrimaryKey())/>
            <#assign questionFieldName = "answers_" + nextSqaaWithColId.surveyQuestionId + "_" + surveyMultiRespColumn.surveyMultiRespColId/>
          <#else/>
            <#assign questionFieldName = "answers_" + surveyQuestionAndAppl.surveyQuestionId + "_" + surveyMultiRespColumn.surveyMultiRespColId/>
          </#if>
          <@renderSurveyQuestionInput surveyQuestionAndAppl=surveyQuestionAndAppl questionFieldName=questionFieldName/>
        </td>
      </#list>
      <td>
        <@renderSurveyQuestionRequired surveyQuestionAndAppl=surveyQuestionAndAppl/>
      </td>
    </tr>
  <#else/>
    <#-- special formatting for select boxes -->
    <#assign align = "left"/>
    <#if surveyQuestionAndAppl?? && surveyQuestionAndAppl.surveyQuestionTypeId?has_content>
        <#if (surveyQuestionAndAppl.surveyQuestionTypeId == "BOOLEAN" || surveyQuestionAndAppl.surveyQuestionTypeId == "CONTENT" || surveyQuestionAndAppl.surveyQuestionTypeId == "OPTION")>
              <#assign align = "right"/>
        </#if>
    </#if>

    <#-- get an answer from the answerMap -->
    <#if surveyAnswers?has_content>
      <#assign answer = surveyAnswers.get(surveyQuestionAndAppl.surveyQuestionId)!/>
    </#if>

    <#-- get the default value from value map -->
    <#if defaultValues?has_content>
      <#assign defValue = defaultValues.get(surveyQuestionAndAppl.surveyQuestionId)!/>
    </#if>

    <tr>
    <#if surveyQuestionAndAppl?? && surveyQuestionAndAppl.surveyQuestionTypeId?has_content>
      <#-- seperator options -->
      <#if surveyQuestionAndAppl.surveyQuestionTypeId == "SEPERATOR_TEXT">
        <td colspan="5"><div>${surveyQuestionAndAppl.question!}</div></td>
      <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "SEPERATOR_LINE"/>
        <td colspan="5"><hr /></td>
      <#else/>
        <#-- standard question options -->
        <td align="right">
          <@renderSurveyQuestionText surveyQuestionAndAppl=surveyQuestionAndAppl/>
        </td>
        <td width="1">&nbsp;</td>
        <td align="${align}">
          <#assign questionFieldName = "answers_" + surveyQuestionAndAppl.surveyQuestionId/>
          <@renderSurveyQuestionInput surveyQuestionAndAppl=surveyQuestionAndAppl questionFieldName=questionFieldName/>
        </td>
        <td>
          <@renderSurveyQuestionRequired surveyQuestionAndAppl=surveyQuestionAndAppl/>
        </td>
        <td width="20%">&nbsp;</td>
      </#if>
    </#if>
    </tr>
  </#if>
   </#if>
  </#list>
  <#-- one last check for a multi-resp table left open before moving on, will happen if last question was in a multi-resp -->
    <#if haveOpenMultiRespHeader>
          </table>
        </td>
      </tr>
    </#if>
  <tr>
    <td>&nbsp;</td>
    <td>&nbsp;</td>
    <td colspan="2"><input type="submit" class="smallSubmit" value="<#if survey.submitCaption?has_content>${survey.submitCaption}<#else/>Submit</#if>"/></td>
  </tr>
</table>
