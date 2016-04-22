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

<#assign selected = headerItem?default("void")>
<div id="app-navigation">
  <ul>
    <li>
      <h2>${uiLabelMap.ExampleApplication}</h2>
      <ul>
      <#if userLogin?has_content>
        <li <#if selected = "${uiLabelMap.ExampleExample}"> class="selected"</#if>><a href="<@ofbizUrl>FindExample?portalPageId=${uiLabelMap.ExampleExample}</@ofbizUrl>">${uiLabelMap.ExampleExample}</a></li>
        <li <#if selected = "${uiLabelMap.ExampleFeature}"> class="selected"</#if>><a href="<@ofbizUrl>FindExampleFeature?portalPageId=${uiLabelMap.ExampleFeature}</@ofbizUrl>">${uiLabelMap.ExampleFeature}</a></li>
        <li <#if selected = "${uiLabelMap.ExampleFormWidgetExamples}"> class="selected"</#if>><a href="<@ofbizUrl>FormWidgetExamples?portalPageId=${uiLabelMap.ExampleFormWidgetExamples}</@ofbizUrl>">${uiLabelMap.ExampleFormWidgetExamples}</a></li>
        <li <#if selected = "${uiLabelMap.ExampleAjaxExamples}"> class="selected"</#if>><a href="<@ofbizUrl>authview/findExampleAjax?portalPageId=${uiLabelMap.ExampleAjaxExamples}</@ofbizUrl>">${uiLabelMap.ExampleAjaxExamples}</a></li>
        <#if portalPages?has_content>
            <#list portalPages as page>
              <#if page.portalPageName?has_content>
                <li<#if selected = "${page.portalPageId}"> class="selected"</#if>><a href="<@ofbizUrl>showPortalPage?portalPageId=${page.portalPageId}</@ofbizUrl>"><#if page.portalPageName??>${page.portalPageName}<#else>?</#if></a></li>
              </#if>
            </#list>
        </#if>
        <li class="opposed"><a href="<@ofbizUrl>ManagePortalPages?parentPortalPageId=EXAMPLE</@ofbizUrl>">${uiLabelMap.CommonDashboard}</a></li>
      </#if>
      </ul>
    </li>
  </ul>
  <br class="clear" />
</div>
