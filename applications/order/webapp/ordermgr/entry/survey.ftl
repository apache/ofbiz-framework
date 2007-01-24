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
<#if requestAttributes.surveyWrapper?exists && requestAttributes.surveyAction?exists>
  <form method="post" enctype="multipart/form-data" action="<@ofbizUrl>${requestAttributes.surveyAction}</@ofbizUrl>" style="margin: 0;">
    ${requestAttributes.surveyWrapper.renderSurvey().toString()}
  </form>
<#else>
  <div class="head1">${uiLabelMap.EcommerceNothingToDoHere}</div>
</#if>
