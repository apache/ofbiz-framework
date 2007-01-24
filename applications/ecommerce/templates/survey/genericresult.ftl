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

<div class="head1">${survey.description?if_exists}</div>
<br/>

<table width="100%" border="0" cellpadding="2" cellspacing="0">
  <#list surveyQuestionAndAppls as surveyQuestionAndAppl>

    <#-- special formatting for select boxes -->
    <#assign align = "left">
    <#if (surveyQuestionAndAppl.surveyQuestionTypeId == "BOOLEAN" || surveyQuestionAndAppl.surveyQuestionTypeId == "OPTION")>
      <#assign align = "right">
    </#if>

    <#-- get an answer from the answerMap -->
    <#if surveyAnswers?has_content>
      <#assign answer = surveyAnswers.get(surveyQuestionAndAppl.surveyQuestionId)?if_exists>
    </#if>

    <#-- get the question results -->
    <#if surveyResults?has_content>
      <#assign results = surveyResults.get(surveyQuestionAndAppl.surveyQuestionId)?if_exists>
    </#if>

    <tr>

      <#-- seperator options -->
      <#if surveyQuestionAndAppl.surveyQuestionTypeId == "SEPERATOR_TEXT">
        <td colspan="5"><div class="tabletext">${surveyQuestionAndAppl.question?if_exists}</div></td>
      <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "SEPERATOR_LINE">
        <td colspan="5"><hr class="sepbar"/></td>
      <#else>

        <#-- standard question options -->
        <td align='right' nowrap>
          <#assign answerString = "answers">
          <#if (results._total?default(0) == 1)>
             <#assign answerString = "answer">
          </#if>
          <div class="tabletext">${surveyQuestionAndAppl.question?if_exists} (${results._total?default(0)?string.number} ${answerString})</div>
          <#if surveyQuestionAndAppl.hint?has_content>
            <div class="tabletext">${surveyQuestionAndAppl.hint}</div>
          </#if>
        </td>
        <td width='1'>&nbsp;</td>

        <td align="${align}">
          <#if surveyQuestionAndAppl.surveyQuestionTypeId == "BOOLEAN">
            <#assign selectedOption = (answer.booleanResponse)?default("Y")>
            <div class="tabletext"><span style="white-space: nowrap;">
              <#if "Y" == selectedOption><b>==>&nbsp;<font color="red"></#if>${uiLabelMap.CommonY}<#if "Y" == selectedOption></font></b></#if>&nbsp;[${results._yes_total?default(0)?string("#")} / ${results._yes_percent?default(0)?string("#")}%]
            </span></div>
            <div class="tabletext"><span style="white-space: nowrap;">
              <#if "N" == selectedOption><b>==>&nbsp;<font color="red"></#if>N<#if "N" == selectedOption></font></b></#if>&nbsp;[${results._no_total?default(0)?string("#")} / ${results._no_percent?default(0)?string("#")}%]
            </span></div>
          <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "TEXTAREA">
            <div class="tabletext">${(answer.textResponse)?if_exists}</div>
          <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "TEXT_SHORT">
            <div class="tabletext">${(answer.textResponse)?if_exists}</div>
          <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "TEXT_LONG">
            <div class="tabletext">${(answer.textResponse)?if_exists}</div>
          <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "EMAIL">
            <div class="tabletext">${(answer.textResponse)?if_exists}</div>
          <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "URL">
            <div class="tabletext">${(answer.textResponse)?if_exists}</div>
          <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "DATE">
            <div class="tabletext">${(answer.textResponse)?if_exists}</div>
          <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "CREDIT_CARD">
            <div class="tabletext">${(answer.textResponse)?if_exists}</div>
          <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "GIFT_CARD">
            <div class="tabletext">${(answer.textResponse)?if_exists}</div>
          <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "NUMBER_CURRENCY">
            <div class="tabletext">${answer.currencyResponse?number?default(0)}</div>
          <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "NUMBER_FLOAT">
            <div class="tabletext">${answer.floatResponse?number?default(0)?string("#")}</div>
          <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "NUMBER_LONG">
            <div class="tabletext">${answer.numericResponse?number?default(0)?string("#")}&nbsp;[${uiLabelMap.CommonTally}: ${results._tally?default(0)?string("#")} / ${uiLabelMap.CommonAverage}: ${results._average?default(0)?string("#")}]</div>
          <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "PASSWORD">
            <div class="tabletext">[${uiLabelMap.CommonNotShown}]</div>
          <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "CONTENT">
            <#if answer.contentId?has_content>
              <#assign content = answer.getRelatedOne("Content")>
              <a href="/content/control/img?imgId=${content.dataResourceId}" class="buttontext">${answer.contentId}</a>&nbsp;-&nbsp;${content.contentName?if_exists}                         
            </#if>
          <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "OPTION">
            <#assign options = surveyQuestionAndAppl.getRelated("SurveyQuestionOption", sequenceSort)?if_exists>
            <#assign selectedOption = (answer.surveyOptionSeqId)?default("_NA_")>
            <#if options?has_content>
              <#list options as option>
                <#assign optionResults = results.get(option.surveyOptionSeqId)?if_exists>
                  <div class="tabletext"><span style="white-space: nowrap;">
                    <#if option.surveyOptionSeqId == selectedOption><b>==>&nbsp;<font color="red"></#if>
                    ${option.description?if_exists}
                    <#if option.surveyOptionSeqId == selectedOption></font></b></#if>
                    &nbsp;[${optionResults._total?default(0)?string("#")} / ${optionResults._percent?default(0?string("#"))}%]
                  </span></div>
              </#list>
            </#if>
          <#else>
            <div class="tabletext">${uiLabelMap.EcommerceUnsupportedQuestionType}: ${surveyQuestionAndAppl.surveyQuestionTypeId}</div>
          </#if>
        </td>
        <td width="90%">&nbsp;</td>
      </#if>
    </tr>
  </#list>
</table>
