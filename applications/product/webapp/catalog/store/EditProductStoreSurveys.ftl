<#--

Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->
<table border="1" cellpadding="2" cellspacing="0" width="100%">
    <tr>
      <td><span class="tableheadtext">${uiLabelMap.CommonType}</span></td>
      <td><span class="tableheadtext">${uiLabelMap.CommonName}</span></td>
      <td><span class="tableheadtext">${uiLabelMap.CommonSurveys}</span></td>
      <td><span class="tableheadtext">${uiLabelMap.ProductProduct}</span></td>
      <td><span class="tableheadtext">${uiLabelMap.ProductCategory}</span></td>
      <td><span class="tableheadtext">${uiLabelMap.CommonFromDate}</span></td>
      <td><span class="tableheadtext">${uiLabelMap.CommonSequenceNum}</span></td>
      <td>&nbsp;</td>
    </tr>

    <#list productStoreSurveys as storeSurvey>
      <#assign surveyType = storeSurvey.getRelatedOne("SurveyApplType")>
      <#assign survey = storeSurvey.getRelatedOne("Survey")>
      <tr>
        <td><span class="tabletext">${surveyType.get("description",locale)}</span></td>
        <td><span class="tabletext">${storeSurvey.groupName?if_exists}</span></td>
        <td><a href="/content/control/EditSurvey?surveyId=${storeSurvey.surveyId}" class="buttontext">${survey.description?default("[" + survey.surveyId + "]")}</a>
        <td><span class="tabletext">${storeSurvey.productId?default("${uiLabelMap.CommonNA}")}</span></td>
        <td><span class="tabletext">${storeSurvey.productCategoryId?default("${uiLabelMap.CommonNA}")}</span></td>
        <td><span class="tabletext">${storeSurvey.fromDate?string}</span></td>
        <td><span class="tabletext">${storeSurvey.sequenceNum?if_exists}</span></td>
        <td><a href="<@ofbizUrl>deleteProductStoreSurveyAppl?productStoreId=${productStoreId}&productStoreSurveyId=${storeSurvey.productStoreSurveyId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonDelete}]</a>
    </#list>
</table>
<br/>

<div class="head2">${uiLabelMap.PageTitleEditProductStoreSurveys}:</div>
<form name="addSurvey" action="<@ofbizUrl>createProductStoreSurveyAppl</@ofbizUrl>" method="post">
    <input type="hidden" name="productStoreId" value="${productStoreId}">
    <table cellspacing="2" cellpadding="2">
      <tr>
        <td><span class="tableheadtext">${uiLabelMap.CommonType}</span></td>
        <td>
          <select class="selectBox" name="surveyApplTypeId">
            <#list surveyApplTypes as type>
              <option value="${type.surveyApplTypeId}">${type.get("description",locale)}</option>
            </#list>
          </select>
        </td>
      </tr>
      <tr>
        <td><span class="tableheadtext">${uiLabelMap.CommonGroup} ${uiLabelMap.CommonName}</span></td>
        <td>
          <input type="text" class="inputBox" size="20" name="groupName">          
        </td>
      </tr>
      <tr>
        <td><span class="tableheadtext">${uiLabelMap.CommonSurveys}</span></td>
        <td>
          <select class="selectBox" name="surveyId">
            <#list surveys as survey>
              <option value="${survey.surveyId}">${survey.description?default("[" + survey.surveyId + "]")}</option>
            </#list>
          </select>
        </td>
      </tr>
      <tr>
        <td><span class="tableheadtext">${uiLabelMap.ProductProductId}</span></td>
        <td>
          <input type="text" class="inputBox" size="20" name="productId">
        </td>
      </tr>
      <tr>
        <td><span class="tableheadtext">${uiLabelMap.ProductCategoryId}</span></td>
        <td>
          <select class="selectBox" name="productCategoryId">
            <option></option>
            <#list productCategories as category>
              <option value="${category.productCategoryId}">${category.description?default("[${uiLabelMap.ProductNoDescription}]")}</option>
            </#list>
          </select>
        </td>
      </tr>
      <tr>
        <td><span class="tableheadtext">${uiLabelMap.CommonFromDate}</span></td>
        <td>
          <input type="text" class="inputBox" size="25" name="fromDate">
          <a href="javascript:call_cal(document.addSurvey.fromDate, '${nowTimestampString}');"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"></a>
        </td>
      </tr>
      <tr>
        <td><span class="tableheadtext">${uiLabelMap.CommonThruDate}</span></td>
        <td>
          <input type="text" class="inputBox" size="25" name="thruDate">
          <a href="javascript:call_cal(document.addSurvey.thruDate, '${nowTimestampString}');"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"></a>
        </td>
      </tr>
      <tr>
        <td><span class="tableheadtext">${uiLabelMap.ProductStoreSurveyTemplatePath}</span></td>
        <td>
          <input type="text" class="inputBox" size="30" name="surveyTemplate">
        </td>
      </tr>
      <tr>
        <td><span class="tableheadtext">${uiLabelMap.ProductStoreSurveyResultTemplatePath}</span></td>
        <td>
          <input type="text" class="inputBox" size="30" name="resultTemplate">
        </td>
      </tr>
      <tr>
        <td><span class="tableheadtext">${uiLabelMap.CommonSequenceNum}</span></td>
        <td>
          <input type="text" class="inputBox" size="5" name="sequenceNum">
        </td>
      </tr>
      <tr>
        <td>&nbsp;</td>
        <td><input type="submit" class="smallSubmit" value="${uiLabelMap.CommonAdd}"></td>
      </tr>
    </table>
</form>
