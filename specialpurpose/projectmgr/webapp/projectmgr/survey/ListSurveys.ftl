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
    <ul><li class="h3">${uiLabelMap.CommonList} ${uiLabelMap.EcommerceSurveys}</li></ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
    <#if workEffortSurveyAppls?has_content>
      <form method="post" action="<@ofbizUrl>updateWorkEffortSurveyAppl</@ofbizUrl>" name="editWorkEffortSurveyAppl">
      <table class="basic-table hover-bar" cellspacing="0">
        <tr class="header-row">
          <td>${uiLabelMap.ContentSurveySurveyId}</td>
          <td>${uiLabelMap.CommonFromDateTime}</td>
          <td>${uiLabelMap.CommonThruDateTime}</td>
          <td>&nbsp;</td>
          <td>&nbsp;</td>
          <td>&nbsp;</td>
        </tr>
        <#list workEffortSurveyAppls as workEffortSurveyAppl>
          <#if workEffortSurveyAppl?has_content>
            <#assign productStoreSurveyAppls = workEffortSurveyAppl.getRelated("ProductStoreSurveyAppl")>
            <#list productStoreSurveyAppls as productStoreSurveyAppl>
              <#if productStoreSurveyAppl?has_content>
                <#assign survey = productStoreSurveyAppl.getRelatedOne("Survey")>
                <tr>
                  <td><a href="/content/control/EditSurvey?surveyId=${workEffortSurveyAppl.surveyId?if_exists}" class="buttontext">${workEffortSurveyAppl.surveyId?if_exists} - ${survey.surveyName?if_exists}</a></td>
                  <td>${workEffortSurveyAppl.fromDate?if_exists}</td>
                  <td>
                    <input type="text" size="20" name="thruDate" value="${(workEffortSurveyAppl.thruDate)?if_exists}" <#if isReadable?exists> readonly="readonly"</#if>>
                    <a href="javascript:call_cal(document.editWorkEffortSurveyAppl.thruDate, '${nowTimeStampString}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
                    </td>
                  <td><a href="<@ofbizUrl>testWorkEffortSurvey?productStoreSurveyId=${productStoreSurveyAppl.productStoreSurveyId?if_exists}&workEffortId=${workEffortSurveyAppl.workEffortId?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.EcommerceTakeSurvey}</a></td>
                  <#if !isReadable?exists>
                    <input type="hidden" name="surveyId" value="${workEffortSurveyAppl.surveyId?if_exists}"/>
                    <input type="hidden" name="workEffortId" value="${workEffortSurveyAppl.workEffortId?if_exists}"/>
                    <input type="hidden" name="fromDate" value="${workEffortSurveyAppl.fromDate?if_exists}"/>
                    <td><input type="submit" name="submitBtn" value='${uiLabelMap.CommonUpdate}'> </td> 
                    <td><a href="<@ofbizUrl>deleteWorkEffortSurveyAppl?surveyId=&workEffortId=${workEffortSurveyAppl.workEffortId?if_exists}&fromDate=${workEffortSurveyAppl.fromDate?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonDelete}</a></td>
                  </#if>
                </tr>
              </#if>
            </#list>
          </#if>
        </#list>
      </table>
      </form>
    </#if>
  </div>
</div>
