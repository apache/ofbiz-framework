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

<#-- Render the survey -->
<#if surveyString?has_content>
  <form method="post" enctype="multipart/form-data" action="<@ofbizUrl>updateSurveyResponse</@ofbizUrl>" 
        name="EditSurveyResponseForm" style="margin: 0;">
    <div>${uiLabelMap.PartyPartyId}: <@htmlTemplate.lookupField value="${surveyPartyId!(userLogin.partyId!)}" 
        formName="EditSurveyResponseForm" name="partyId" id="partyId" fieldFormName="LookupPartyName"/></div>
    <#-- pass through the dataResourceId so the response can be associated with it -->
    <input type="hidden" name="dataResourceId" value="${parameters.dataResourceId!}"/>
    <input type="hidden" name="rootContentId" value="${parameters.rootContentId!}"/>
    ${StringUtil.wrapString(surveyString)}
  </form>
<#else>
  <h1>Problem rendering the survey.</h1>
</#if>
