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

<#if requestAttributes._ERROR_MESSAGE_??>
  <br/>
  <div class='errorMessage'>${requestAttributes._ERROR_MESSAGE_}</div><br/>
<#else>
  <#if trailList??>
    <#assign indent = "">
    <#assign csv = "">
    <#assign counter = 1>
    <#assign len = trailList?size>
    <#list trailList as pair>
      <#if 0 < csv?length >
        <#assign csv = csv + ","/>
      </#if>
      <#assign csv = csv + pair[0]!/>
      <#if counter < len>
      ${indent}
      ${pair[0]!} - ${pair[1]!}
      <a class="tabButton" href="<@ofbizUrl>ViewBlog?contentId=${pair[0]!}&nodeTrailCsv=${csv!}"></@ofbizUrl>
        ${uiLabelMap.CommonView}
      </a>
      <br/>
        <#assign indent = indent + "&nbsp;&nbsp;&nbsp;&nbsp;">
      <#else>

      <u>${uiLabelMap.EcommerceAddResponseFor}${pair[0]!} - ${pair[1]!}:</u><br/>
      </#if>
      <#assign counter = counter + 1>
    </#list>

    <#if dataResourceId??>
    <br/>
    <img src="<@ofbizUrl>img?imgId=${dataResourceId}</@ofbizUrl>" alt=""/>
    </#if>
  <br/>
  </#if>
  ${singleWrapper.renderFormString()}
  <br/>
</#if>
