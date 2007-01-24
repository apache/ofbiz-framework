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

  <table width="100%" border="0" cellpadding="2" cellspacing="0">
    <#assign questions = surveyWrapper.getSurveyQuestionAndAppls()>
    <#assign surveyResults = surveyWrapper.getResults(questions)>

    <#if questions?has_content>
      <#list questions as question>
        <#assign results = surveyResults.get(question.surveyQuestionId)?if_exists>

        <tr>
          <#-- seperator options -->
          <#if question.surveyQuestionTypeId == "SEPERATOR_TEXT">
            <td colspan="5"><div class="tabletext">${question.question?if_exists}</div></td>
          <#elseif question.surveyQuestionTypeId == "SEPERATOR_LINE">
            <td colspan="5"><hr class="sepbar"></td>
          <#else>

            <#-- standard questions -->
            <td align='right' nowrap>
              <#assign answerString = "${uiLabelMap.ContentAnswers}">
              <#if (results._total?default(0) == 1)>
                <#assign answerString = "${uiLabelMap.ContentAnswer}">
              </#if>
              <div class="tabletext">${question.question?if_exists} (${results._total?default(0)?string.number} ${answerString})</div>
              <#if question.hint?has_content>
                <div class="tabletext">${question.hint}</div>
              </#if>
            </td>
            <td width='1'>&nbsp;</td>

            <#-- answers -->
            <td>
              <#if question.surveyQuestionTypeId == "BOOLEAN">
                <#assign selectedOption = (answer.booleanResponse)?default("Y")>
                <div class="tabletext">
                  <span style="white-space: nowrap;">${uiLabelMap.CommonY}&nbsp;[${results._yes_total?default(0)?string("#")} / ${results._yes_percent?default(0)?string("#")}%]</span>
                </div>
                <div class="tabletext">
                  <span style="white-space: nowrap;">${uiLabelMap.CommonN}&nbsp;[${results._no_total?default(0)?string("#")} / ${results._no_percent?default(0)?string("#")}%]</span>
                </div>
              <#elseif question.surveyQuestionTypeId == "OPTION">
                <#assign options = question.getRelated("SurveyQuestionOption", sequenceSort)?if_exists>
                <#if options?has_content>
                  <#list options as option>
                    <#assign optionResults = results.get(option.surveyOptionSeqId)?if_exists>
                    <div class="tabletext">
                      <span style="white-space: nowrap;">
                        ${option.description?if_exists}
                        &nbsp;[${optionResults._total?default(0)?string("#")} / ${optionResults._percent?default(0?string("#"))}%]
                      </span>
                    </div>
                  </#list>
                </#if>
              <#else>
                <#assign answers = surveyWrapper.getQuestionResponses(question, 0, 0)>
                <#if answers?has_content>
                  <#list answers as answer>
                    <#if question.surveyQuestionTypeId == "TEXTAREA">
                      <div class="tabletext">${(answer.textResponse)?if_exists}</div>
                    <#elseif question.surveyQuestionTypeId == "TEXT_SHORT">
                      <div class="tabletext">${(answer.textResponse)?if_exists}</div>
                    <#elseif question.surveyQuestionTypeId == "TEXT_LONG">
                      <div class="tabletext">${(answer.textResponse)?if_exists}</div>
                    <#elseif question.surveyQuestionTypeId == "EMAIL">
                      <div class="tabletext">${(answer.textResponse)?if_exists}</div>
                    <#elseif question.surveyQuestionTypeId == "URL">
                      <div class="tabletext">${(answer.textResponse)?if_exists}</div>
                    <#elseif question.surveyQuestionTypeId == "DATE">
                      <div class="tabletext">${(answer.textResponse)?if_exists}</div>
                    <#elseif question.surveyQuestionTypeId == "CREDIT_CARD">
                      <div class="tabletext">${(answer.textResponse)?if_exists}</div>
                    <#elseif question.surveyQuestionTypeId == "GIFT_CARD">
                      <div class="tabletext">${(answer.textResponse)?if_exists}</div>
                    <#elseif question.surveyQuestionTypeId == "NUMBER_CURRENCY">
                      <div class="tabletext">${answer.currencyResponse?default(0)}</div>
                    <#elseif question.surveyQuestionTypeId == "NUMBER_FLOAT">
                      <div class="tabletext">${answer.floatResponse?default(0)?string("#")}</div>
                    <#elseif question.surveyQuestionTypeId == "NUMBER_LONG">
                      <div class="tabletext">${answer.numericResponse?default(0)?string("#")}&nbsp;[${uiLabelMap.CommonTally}: ${results._tally?default(0)?string("#")} / ${uiLabelMap.CommonAverage}: ${results._average?default(0)?string("#")}]</div>
                    <#elseif question.surveyQuestionTypeId == "PASSWORD">
                      <div class="tabletext">[${uiLabelMap.CommonNotShown}]</div>
                    <#elseif question.surveyQuestionTypeId == "CONTENT">
                       <#if answer.contentId?has_content>
                         <#assign content = answer.getRelatedOne("Content")>
                         <a href="<@ofbizUrl>img?imgId=${content.dataResourceId}</@ofbizUrl>" class="buttontext">${answer.contentId}</a>&nbsp;-&nbsp;${content.contentName?if_exists}                         
                       </#if>
                    </#if>
                  </#list>
                </#if>
              </#if>
            </td>

            <td width="90%">&nbsp;</td>
          </#if>
        </tr>
        <tr><td colspan="3">&nbsp;</td></tr>
      </#list>
    <#else>
      <tr>
        <td><div class="tabletext">${uiLabelMap.SurveyNoQuestions}</div></td>
      </tr>
    </#if>
  </table>
