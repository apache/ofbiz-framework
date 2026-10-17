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

  <table class="basic-table hover-bar" cellspacing="0">
    <#assign questions = surveyWrapper.getSurveyQuestionAndAppls()>
    <#assign surveyResults = surveyWrapper.getResults(questions)>

    <#if questions?has_content>
      <#list questions as question>
        <#assign results = surveyResults.get(question.surveyQuestionId)!>

        <tr>
          <#-- seperator options -->
          <#if "SEPERATOR_TEXT" == question.surveyQuestionTypeId>
            <td colspan="5" class="label">${question.question!}</td>
          <#elseif "SEPERATOR_LINE" == question.surveyQuestionTypeId>
            <td colspan="5"><hr/></td>
          <#else>

            <#-- standard questions -->
            <td align='right' nowrap="nowrap" class="label">
              <#assign answerString = "${uiLabelMap.ContentAnswers}">
              <#if (results._total?default(0) == 1)>
                <#assign answerString = "${uiLabelMap.ContentAnswer}">
              </#if>
              <div>${question.question!} (${results._total?default(0)?string.number} ${answerString})</div>
              <#if question.hint?has_content>
                <div>${question.hint}</div>
              </#if>
            </td>
            <td width='1'>&nbsp;</td>

            <#-- answers -->
            <td>
              <#if "BOOLEAN" == question.surveyQuestionTypeId>
                <#assign selectedOption = (answer.booleanResponse)?default("Y")>
                <div>
                  <span style="white-space: nowrap;">${uiLabelMap.CommonY}&nbsp;[${results._yes_total?default(0)?string("#")} / ${results._yes_percent?default(0)?string("#")}%]</span>
                </div>
                <div>
                  <span style="white-space: nowrap;">${uiLabelMap.CommonN}&nbsp;[${results._no_total?default(0)?string("#")} / ${results._no_percent?default(0)?string("#")}%]</span>
                </div>
              <#elseif "OPTION" == question.surveyQuestionTypeId>
                <#assign options = question.getRelated("SurveyQuestionOption", null, sequenceSort, false)!>
                <#if options?has_content>
                  <#list options as option>
                    <#assign optionResults = results.get(option.surveyOptionSeqId)!>
                    <div>
                      <span style="white-space: nowrap;">
                        ${option.description!}
                        &nbsp;[${optionResults._total?default(0)?string("#")} / ${optionResults._percent?default(0?string("#"))}%]
                      </span>
                    </div>
                  </#list>
                </#if>
              <#else>
                <#assign answers = surveyWrapper.getQuestionResponses(question, 0, 0)>
                <#if answers?has_content>
                  <#list answers as answer>
                    <#if "TEXTAREA" == question.surveyQuestionTypeId>
                      <div>${(answer.textResponse)!}</div>
                    <#elseif "TEXT_SHORT" == question.surveyQuestionTypeId>
                      <div>${(answer.textResponse)!}</div>
                    <#elseif "TEXT_LONG" == question.surveyQuestionTypeId>
                      <div>${(answer.textResponse)!}</div>
                    <#elseif "EMAIL" == question.surveyQuestionTypeId>
                      <div>${(answer.textResponse)!}</div>
                    <#elseif "URL" == question.surveyQuestionTypeId>
                      <div>${(answer.textResponse)!}</div>
                    <#elseif "DATE" == question.surveyQuestionTypeId>
                      <div>${(answer.textResponse)!}</div>
                    <#elseif "CREDIT_CARD" == question.surveyQuestionTypeId>
                      <div>${(answer.textResponse)!}</div>
                    <#elseif "GIFT_CARD" == question.surveyQuestionTypeId>
                      <div>${(answer.textResponse)!}</div>
                    <#elseif "NUMBER_CURRENCY" == question.surveyQuestionTypeId>
                      <div>${answer.currencyResponse?default(0)}</div>
                    <#elseif "NUMBER_FLOAT" == question.surveyQuestionTypeId>
                      <div>${answer.floatResponse?default(0)?string("#")}</div>
                    <#elseif "NUMBER_LONG" == question.surveyQuestionTypeId>
                      <div>${answer.numericResponse?default(0)?string("#")}&nbsp;${uiLabelMap.CommonTally}: ${results._tally?default(0)?string("#")} / ${uiLabelMap.CommonAverage}: ${results._average?default(0)?string("#")}</div>
                    <#elseif "PASSWORD" == question.surveyQuestionTypeId>
                      <div>${uiLabelMap.CommonNotShown}</div>
                    <#elseif "CONTENT" == question.surveyQuestionTypeId>
                       <#if answer.contentId?has_content>
                         <#assign content = answer.getRelatedOne("Content", false)>
                         <a href="<@ofbizUrl>img?imgId=${content.dataResourceId}</@ofbizUrl>" class="buttontext">${answer.contentId}</a>&nbsp;-&nbsp;${content.contentName!}
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
        <td class="label">${uiLabelMap.ContentSurveyNoQuestions}</div></td>
      </tr>
    </#if>
  </table>
