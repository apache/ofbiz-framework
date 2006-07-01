<#--
 *  Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@version    $Rev$
 *@since      3.1
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
            <div class="tabletext"><nobr>
              <#if "Y" == selectedOption><b>==>&nbsp;<font color="red"></#if>${uiLabelMap.CommonY}<#if "Y" == selectedOption></font></b></#if>&nbsp;[${results._yes_total?default(0)?string("#")} / ${results._yes_percent?default(0)?string("#")}%]
            </nobr></div>
            <div class="tabletext"><nobr>
              <#if "N" == selectedOption><b>==>&nbsp;<font color="red"></#if>N<#if "N" == selectedOption></font></b></#if>&nbsp;[${results._no_total?default(0)?string("#")} / ${results._no_percent?default(0)?string("#")}%]
            </nobr></div>
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
                  <div class="tabletext"><nobr>
                    <#if option.surveyOptionSeqId == selectedOption><b>==>&nbsp;<font color="red"></#if>
                    ${option.description?if_exists}
                    <#if option.surveyOptionSeqId == selectedOption></font></b></#if>
                    &nbsp;[${optionResults._total?default(0)?string("#")} / ${optionResults._percent?default(0?string("#"))}%]
                  </nobr></div>
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