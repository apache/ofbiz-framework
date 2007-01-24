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

  <table border="1" cellpadding='2' cellspacing='0'>
    <tr>
      <td><div class="tableheadtext">${uiLabelMap.CommonId}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.CommonType}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.SurveryCategory}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.CommonDescription}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.SurveyQuestion}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.CommonPage}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.SurveyMultiResp}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.SurveyMultiRespColumn}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.CommonRequired}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.CommonSequenceNum}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.SurveyWithQuestion}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.SurveyWithOption}</div></td>
      <td><div class="tableheadtext">&nbsp;</div></td>
      <td><div class="tableheadtext">&nbsp;</div></td>
      <td><div class="tableheadtext">&nbsp;</div></td>
    </tr>

    <#list surveyQuestionAndApplList as surveyQuestionAndAppl>
      <#assign questionType = surveyQuestionAndAppl.getRelatedOneCache("SurveyQuestionType")/>
      <#assign questionCat = surveyQuestionAndAppl.getRelatedOneCache("SurveyQuestionCategory")?if_exists/>
      <#assign currentSurveyPage = surveyQuestionAndAppl.getRelatedOneCache("SurveyPage")?if_exists/>
      <#assign currentSurveyMultiResp = surveyQuestionAndAppl.getRelatedOneCache("SurveyMultiResp")?if_exists/>
      <#if currentSurveyMultiResp?has_content>
        <#assign currentSurveyMultiRespColumns = currentSurveyMultiResp.getRelated("SurveyMultiRespColumn")/>
      <#else/>
        <#assign currentSurveyMultiRespColumns = []/>
      </#if>
      <form method="post" action="<@ofbizUrl>updateSurveyQuestionAppl</@ofbizUrl>">
        <input type="hidden" name="surveyId" value="${surveyQuestionAndAppl.surveyId}">
        <input type="hidden" name="surveyQuestionId" value="${surveyQuestionAndAppl.surveyQuestionId}">
        <input type="hidden" name="fromDate" value="${surveyQuestionAndAppl.fromDate}">
        <tr valign="middle">
          <td><div class="tabletext">${surveyQuestionAndAppl.surveyQuestionId}</div></td>
          <td><div class="tabletext">${questionType.get("description",locale)}</div></td>
          <td><div class="tabletext">${(questionCat.description)?if_exists}</div></td>
          <td><div class="tabletext">${surveyQuestionAndAppl.description?if_exists}</div></td>
          <td><input type="text" name="question" size="30" class="inputBox" value="${surveyQuestionAndAppl.question?if_exists?html}">
          <td>
            <select class="selectBox" name="surveyPageId">
              <#if surveyQuestionAndAppl.surveyPageSeqId?has_content>
                <option value="${surveyQuestionAndAppl.surveyPageSeqId}">${(currentSurveyPage.pageName)?if_exists} [${surveyQuestionAndAppl.surveyPageSeqId}]</option>
                <option value="${surveyQuestionAndAppl.surveyPageSeqId}">----</option>
              </#if>
              <option value=""></option>
              <#list surveyPageList as surveyPage>
                <option value="${surveyPage.surveyPageSeqId}">${surveyPage.pageName?if_exists} [${surveyPage.surveyPageSeqId}]</option>
              </#list>
            </select>
          </td>
          <td>
            <select class="selectBox" name="surveyMultiRespId">
              <#if surveyQuestionAndAppl.surveyMultiRespId?has_content>
                <option value="${surveyQuestionAndAppl.surveyMultiRespId}">${(currentSurveyMultiResp.multiRespTitle)?if_exists} [${surveyQuestionAndAppl.surveyMultiRespId}]</option>
                <option value="${surveyQuestionAndAppl.surveyMultiRespId}">----</option>
              </#if>
              <option value=""></option>
              <#list surveyMultiRespList as surveyMultiResp>
                <option value="${surveyMultiResp.surveyMultiRespId}">${surveyMultiResp.multiRespTitle} [${surveyMultiResp.surveyMultiRespId}]</option>
              </#list>
            </select>
          </td>
          <#if currentSurveyMultiRespColumns?has_content>
          <td>
            <select class="selectBox" name="surveyMultiRespColId">
              <#if surveyQuestionAndAppl.surveyMultiRespColId?has_content>
                <#assign currentSurveyMultiRespColumn = surveyQuestionAndAppl.getRelatedOne("SurveyMultiRespColumn")/>
                <option value="${currentSurveyMultiRespColumn.surveyMultiRespColId}">${(currentSurveyMultiRespColumn.columnTitle)?if_exists} [${currentSurveyMultiRespColumn.surveyMultiRespColId}]</option>
                <option value="${currentSurveyMultiRespColumn.surveyMultiRespColId}">----</option>
              </#if>
              <option value=""></option>
              <#list currentSurveyMultiRespColumns as currentSurveyMultiRespColumn>
                <option value="${currentSurveyMultiRespColumn.surveyMultiRespColId}">${currentSurveyMultiRespColumn.columnTitle} [${currentSurveyMultiRespColumn.surveyMultiRespColId}]</option>
              </#list>
            </select>
          </td>
          <#else/>
            <td><input type="text" name="surveyMultiRespColId" size="4" class="inputBox" value="${surveyQuestionAndAppl.surveyMultiRespColId?if_exists}"/></td>
          </#if>
          <td>
            <select class="selectBox" name="requiredField">
              <option>${surveyQuestionAndAppl.requiredField?default("N")}</option>
              <option value="${surveyQuestionAndAppl.requiredField?default("N")}">----</option>
              <option>Y</option><option>N</option>
            </select>
          </td>
          <td><input type="text" name="sequenceNum" size="5" class="inputBox" value="${surveyQuestionAndAppl.sequenceNum?if_exists}"/></td>
          <td><input type="text" name="withSurveyQuestionId" size="5" class="inputBox" value="${surveyQuestionAndAppl.withSurveyQuestionId?if_exists}"/></td>
          <td><input type="text" name="withSurveyOptionSeqId" size="5" class="inputBox" value="${surveyQuestionAndAppl.withSurveyOptionSeqId?if_exists}"/></td>
          <td><input type="submit" value="${uiLabelMap.CommonUpdate}" class="smallSubmit"/></td>
          <td><a href="<@ofbizUrl>EditSurveyQuestions?surveyId=${requestParameters.surveyId}&surveyQuestionId=${surveyQuestionAndAppl.surveyQuestionId}#edit</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}&nbsp;${uiLabelMap.SurveyQuestion}</a></td>
          <td><a href="<@ofbizUrl>removeSurveyQuestionAppl?surveyId=${surveyQuestionAndAppl.surveyId}&surveyQuestionId=${surveyQuestionAndAppl.surveyQuestionId}&fromDate=${surveyQuestionAndAppl.fromDate}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonRemove}</a></td>
        </tr>
      </form>
    </#list>
  </table>
  <br/>
  <#-- apply question from category -->
  <#if surveyQuestionCategory?has_content>
    <hr class="sepbar">
    <a name="appl">
    <div class="head1">${uiLabelMap.SurveyApplyQuestionFromCategory} - <span class="head2">${surveyQuestionCategory.description?if_exists} [${surveyQuestionCategory.surveyQuestionCategoryId}]</div>
    <br/><br/>
    <table border="1" cellpadding='2' cellspacing='0'>
      <tr>
        <td><div class="tableheadtext">${uiLabelMap.CommonId}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.CommonDescription}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.CommonType}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.SurveyQuestion}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.CommonPage}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.SurveyMultiResp}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.SurveyMultiRespColumn}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.CommonRequired}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.CommonSequenceNum}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.SurveyWithQuestion}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.SurveyWithOption}</div></td>
        <td><div class="tableheadtext">&nbsp;</div></td>
      </tr>

      <#list categoryQuestions as question>
        <#assign questionType = question.getRelatedOne("SurveyQuestionType")>
        <form method="post" action="<@ofbizUrl>createSurveyQuestionAppl</@ofbizUrl>">
          <input type="hidden" name="surveyId" value="${requestParameters.surveyId}">
          <input type="hidden" name="surveyQuestionId" value="${question.surveyQuestionId}">
          <input type="hidden" name="surveyQuestionCategoryId" value="${requestParameters.surveyQuestionCategoryId}">
          <tr valign="middle">
            <td><a href="<@ofbizUrl>EditSurveyQuestions?surveyId=${requestParameters.surveyId}&surveyQuestionId=${question.surveyQuestionId}&surveyQuestionCategoryId=${requestParameters.surveyQuestionCategoryId}#edit</@ofbizUrl>" class="buttontext">${question.surveyQuestionId}</a></td>
            <td><div class="tabletext">${question.description?if_exists}</div></td>
            <td><div class="tabletext">${questionType.get("description",locale)}</div></td>
            <td><div class="tabletext">${question.question?if_exists}</div></td>
          <td>
            <select class="selectBox" name="surveyPageId">
              <option value=""></option>
              <#list surveyPageList as surveyPage>
                <option value="${surveyPage.surveyPageSeqId}">${surveyPage.pageName} [${surveyPage.surveyPageSeqId}]</option>
              </#list>
            </select>
          </td>
          <td>
            <select class="selectBox" name="surveyMultiRespId">
              <option value=""></option>
              <#list surveyMultiRespList as surveyMultiResp>
                <option value="${surveyMultiResp.surveyMultiRespId}">${surveyMultiResp.multiRespTitle} [${surveyMultiResp.surveyMultiRespId}]</option>
              </#list>
            </select>
          </td>
            <td><input type="text" name="surveyMultiRespColId" size="4" class="inputBox"/></td>
            <td>
              <select name="requiredField" class="selectBox">
                <option>N</option>
                <option>Y</option>
              </select>
            </td>
            <td><input type="text" name="sequenceNum" size="5" class="inputBox"/></td>
            <td><input type="text" name="withSurveyQuestionId" size="5" class="inputBox"/></td>
            <td><input type="text" name="withSurveyOptionSeqId" size="5" class="inputBox"/></td>
            <td><input type="submit" value="${uiLabelMap.CommonApply}" class="smallSubmit"/></td>
          </tr>
        </form>
      </#list>
    </table>
    <br/>
  </#if>

  <hr class="sepbar">
  <div class="head2">${uiLabelMap.SurveyApplyQuestionFromCategory}</div>
  <br/>
  <form method="post" action="<@ofbizUrl>EditSurveyQuestions</@ofbizUrl>">
    <input type="hidden" name="surveyId" value="${requestParameters.surveyId}"/>
    <select name="surveyQuestionCategoryId" class="selectBox">
      <#list questionCategories as category>
        <option value="${category.surveyQuestionCategoryId}">${category.description?default("??")} [${category.surveyQuestionCategoryId}]</option>
      </#list>
    </select>
    &nbsp;
    <input type="submit" value="${uiLabelMap.CommonApply}" class="smallSubmit"/>
  </form>
  <br/>

  <hr class="sepbar">
  <a name="edit">
  <#-- new question / category -->
  <#if requestParameters.newCategory?default("N") == "Y">
    <div class="head2">${uiLabelMap.SurveyCreateQuestionCategory}</div>
    <a href="<@ofbizUrl>EditSurveyQuestions?surveyId=${requestParameters.surveyId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonNew} ${uiLabelMap.SurveyQuestion}</a>
    <br/><br/>
    ${createSurveyQuestionCategoryWrapper.renderFormString(context)}
  <#else>
    <#if surveyQuestionId?has_content>
      <div class="head2">${uiLabelMap.CommonEdit} ${uiLabelMap.SurveyQuestion}:</div>
      <a href="<@ofbizUrl>EditSurveyQuestions?surveyId=${requestParameters.surveyId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonNew} ${uiLabelMap.SurveyQuestion}</a>
    <#else>
      <div class="head2">${uiLabelMap.SurveyCreateQuestion}</div>
    </#if>
    <a href="<@ofbizUrl>EditSurveyQuestions?surveyId=${requestParameters.surveyId}&newCategory=Y</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonNew} ${uiLabelMap.SurveyQuestion} ${uiLabelMap.SurveryCategory}</a>
    <br/><br/>
    ${createSurveyQuestionWrapper.renderFormString(context)}
  </#if>

  <#if (surveyQuestion?has_content && surveyQuestion.surveyQuestionTypeId?default("") == "OPTION")>
    <br/>
    <hr class="sepbar">
    <br/>
    <div class="head1">${uiLabelMap.SurveyOptions} - <span class="head2">${uiLabelMap.CommonId}: ${surveyQuestion.surveyQuestionId?if_exists}</div>
    <br/><br/>
    <table border="1" cellpadding='2' cellspacing='0'>
      <tr>
        <td><div class="tableheadtext">${uiLabelMap.CommonDescription}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.CommonSequenceNum}</div></td>
        <td><div class="tableheadtext">&nbsp;</div></td>
        <td><div class="tableheadtext">&nbsp;</div></td>
      </tr>

      <#list questionOptions as option>
        <tr valign="middle">
          <td><div class="tabletext">${option.description?if_exists}</div></td>
          <td><div class="tabletext">${option.sequenceNum?if_exists}</div></td>
          <td><a href="<@ofbizUrl>EditSurveyQuestions?surveyId=${requestParameters.surveyId}&surveyQuestionId=${option.surveyQuestionId}&surveyOptionSeqId=${option.surveyOptionSeqId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonEdit}]</a>
          <td><a href="<@ofbizUrl>removeSurveyQuestionAppl?surveyId=${requestParameters.surveyId}&surveyQuestionId=${option.surveyQuestionId}&surveyOptionSeqId=${option.surveyOptionSeqId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonRemove}]</a>
        </tr>
      </#list>
    </table>
    <br/>
    <#if !surveyQuestionOption?has_content>
      <div class="head2">${uiLabelMap.SurveyCreateQuestionOption}:</div>
    <#else>
      <div class="head2">${uiLabelMap.SurveyEditQuestionOption}:</div>
      <a href="<@ofbizUrl>EditSurveyQuestions?surveyId=${requestParameters.surveyId}&surveyQuestionId=${surveyQuestionOption.surveyQuestionId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonNew} ${uiLabelMap.SurveyOption}]</a>
    </#if>
    ${createSurveyOptionWrapper.renderFormString()}
  </#if>
