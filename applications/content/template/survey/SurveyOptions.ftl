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

<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.ContentSurveyOptions} - ${uiLabelMap.CommonId} ${surveyQuestion.surveyQuestionId!}</li>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
    <table class="basic-table hover-bar" cellspacing="0">
      <tr class="header-row">
        <td>${uiLabelMap.CommonDescription}</td>
        <td>${uiLabelMap.CommonSequenceNum}</td>
        <td>&nbsp;</td>
        <td>&nbsp;</td>
      </tr>
      <#assign alt_row = false>
      <#list questionOptions as option>
        <tr<#if alt_row> class="alternate-row"</#if>>
          <td>${option.description!}</td>
          <td>${option.sequenceNum!}</td>
          <td><a href="<@ofbizUrl>EditSurveyQuestions?surveyId=${requestParameters.surveyId}&amp;surveyQuestionId=${option.surveyQuestionId}&amp;surveyOptionSeqId=${option.surveyOptionSeqId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a></td>
          <td>
            <form id="deleteSurveyQuestionOption_${option_index}" action="<@ofbizUrl>deleteSurveyQuestionOption</@ofbizUrl>" method="post">
              <input type="hidden" name="surveyId" value="${requestParameters.surveyId}" />
              <input type="hidden" name="surveyQuestionId" value="${option.surveyQuestionId}" />
              <input type="hidden" name="surveyOptionSeqId" value="${option.surveyOptionSeqId}" />
              <input type="submit" value="${uiLabelMap.CommonRemove}" />
            </form>
          </td>
        </tr>
        <#assign alt_row = !alt_row>
      </#list>
    </table>
  </div>
</div>