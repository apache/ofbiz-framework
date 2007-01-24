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
  <form method="post" enctype="multipart/form-data" action="<@ofbizUrl>updateSurveyResponse</@ofbizUrl>" name="EditSurveyResponseForm" style="margin: 0;">
    <div class="tabletext">${uiLabelMap.PartyPartyId}: <input type="text" size="15" name="partyId" value="${userLogin.partyId}" class="inputBox"/><a href="javascript:call_fieldlookup2(document.EditSurveyResponseForm.partyId, 'LookupPartyName');"><img src="<@ofbizContentUrl>/images/fieldlookup.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Lookup"/></a></div>
    <#-- pass through the dataResourceId so the response can be associated with it -->
    <input type="hidden" name="dataResourceId" value="${parameters.dataResourceId?if_exists}"/>
    <input type="hidden" name="rootContentId" value="${parameters.rootContentId?if_exists}"/>
    ${surveyString}
  </form>
<#else>
  <div class="head1">Problem rendering the survey.</div>
</#if>
