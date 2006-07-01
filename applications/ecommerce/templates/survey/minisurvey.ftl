<#--
 *  Copyright (c) 2003-2005 The Open For Business Project - www.ofbiz.org
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

<table width="100%" border="0" cellpadding="2" cellspacing="0">
  <#list surveyQuestionAndAppls as surveyQuestionAndAppl>
    <#-- get an answer from the answerMap -->
    <#if surveyAnswers?has_content>
      <#assign answer = surveyAnswers.get(surveyQuestionAndAppl.surveyQuestionId)?if_exists>
    </#if>

    <tr>
      <#-- standard question options -->
      <td align='left'>
        <div class="tabletext">${surveyQuestionAndAppl.question?if_exists}</div>
        <#if surveyQuestionAndAppl.hint?has_content>
          <div class="tabletext">${surveyQuestionAndAppl.hint}</div>
        </#if>
      </td>
    </tr>

      <tr>
        <td align="center">
          <#if surveyQuestionAndAppl.surveyQuestionTypeId == "BOOLEAN">
            <#assign selectedOption = (answer.booleanResponse)?default("Y")>
            <select class="selectBox" name="answers_${surveyQuestionAndAppl.surveyQuestionId}">
              <#if surveyQuestionAndAppl.requiredField?default("N") != "Y">
                <option value=""></option>
              </#if>
              <option <#if "Y" == selectedOption>SELECTED</#if>>Y</option>
              <option <#if "N" == selectedOption>SELECTED</#if>>N</option>
            </select>
          <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "TEXTAREA">
            <textarea class="textAreaBox" cols="40" rows="5" name="answers_${surveyQuestionAndAppl.surveyQuestionId}">${(answer.textResponse)?if_exists}</textarea>
          <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "TEXT_SHORT">
            <input type="text" size="15" class="textBox" name="answers_${surveyQuestionAndAppl.surveyQuestionId}" value="${(answer.textResponse)?if_exists}"/>
          <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "TEXT_LONG">
            <input type="text" size="35" class="textBox" name="answers_${surveyQuestionAndAppl.surveyQuestionId}" value="${(answer.textResponse)?if_exists}"/>
          <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "EMAIL">
            <input type="text" size="30" class="textBox" name="answers_${surveyQuestionAndAppl.surveyQuestionId}" value="${(answer.textResponse)?if_exists}"/>
          <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "URL">
            <input type="text" size="40" class="textBox" name="answers_${surveyQuestionAndAppl.surveyQuestionId}" value="${(answer.textResponse)?if_exists}"/>
          <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "DATE">
            <input type="text" size="12" class="textBox" name="answers_${surveyQuestionAndAppl.surveyQuestionId}" value="${(answer.textResponse)?if_exists}"/>
          <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "CREDIT_CARD">
            <input type="text" size="20" class="textBox" name="answers_${surveyQuestionAndAppl.surveyQuestionId}" value="${(answer.textResponse)?if_exists}"/>
          <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "GIFT_CARD">
            <input type="text" size="20" class="textBox" name="answers_${surveyQuestionAndAppl.surveyQuestionId}" value="${(answer.textResponse)?if_exists}"/>
          <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "NUMBER_CURRENCY">
            <input type="text" size="6" class="textBox" name="answers_${surveyQuestionAndAppl.surveyQuestionId}" value="${(answer.currencyResponse)?if_exists}"/>
          <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "NUMBER_FLOAT">
            <input type="text" size="6" class="textBox" name="answers_${surveyQuestionAndAppl.surveyQuestionId}" value="${(answer.floatResponse)?if_exists}"/>
          <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "NUMBER_LONG">
            <input type="text" size="6" class="textBox" name="answers_${surveyQuestionAndAppl.surveyQuestionId}" value="${(answer.numericResponse?string("#"))?if_exists}"/>
          <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "PASSWORD">
            <input type="password" size="30" class="textBox" name="answers_${surveyQuestionAndAppl.surveyQuestionId}" value="${(answer.textResponse)?if_exists}"/>
          <#elseif surveyQuestionAndAppl.surveyQuestionTypeId == "OPTION">
            <#assign options = surveyQuestionAndAppl.getRelated("SurveyQuestionOption", sequenceSort)?if_exists>
            <#assign selectedOption = (answer.surveyOptionSeqId)?default("_NA_")>
            <select class="selectBox" name="answers_${surveyQuestionAndAppl.surveyQuestionId}">
              <#if surveyQuestionAndAppl.requiredField?default("N") != "Y">
                <option value=""></option>
              </#if>
              <#if options?has_content>
                <#list options as option>
                  <option value="${option.surveyOptionSeqId}" <#if option.surveyOptionSeqId == selectedOption>SELECTED</#if>>${option.description?if_exists}</option>
                </#list>
              <#else>
                <option value="">Nothing to choose</option>
              </#if>
            </select>
          <#else>
            <div class="tabletext">Unsupported question type : ${surveyQuestionAndAppl.surveyQuestionTypeId}</div>
          </#if>
          <#if surveyQuestionAndAppl.requiredField?default("N") == "Y">
            <span class="tabletext">*</span>
          <#else>
            <span class="tabletext">[optional]</span>
          </#if>
        </td>

    </tr>
  </#list>
  <tr>
    <td align="center"><input type="submit" value="<#if survey.submitCaption?has_content>${survey.submitCaption}<#else>Submit</#if>" class="smallSubmit"/></td>
  </tr>
</table>
