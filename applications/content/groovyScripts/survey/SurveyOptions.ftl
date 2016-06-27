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
              <a href="javascript:document.getElementById('deleteSurveyQuestionOption_${option_index}').submit();"" class="buttontext">${uiLabelMap.CommonRemove}</a>
            </form>
          </td>
        </tr>
        <#assign alt_row = !alt_row>
      </#list>
    </table>
  </div>
</div>