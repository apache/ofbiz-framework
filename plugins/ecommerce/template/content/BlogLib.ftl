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

<#macro renderAncestryPath trail siteId startIndex=0 endIndexOffset=0 buttonTitle="Back to" searchOn="" >
  <#local indent = "">
  <#local csv = "">
  <#local counter = 0>
  <#local len = trail?size>
  <table border="0" cellspacing="4">
    <#list trail as content>
      <#if counter < (len - endIndexOffset) && startIndex <= counter >
        <#if 0 < counter >
          <#local csv = csv + ","/>
        </#if>
        <#local csv = csv + content.contentId/>
        <#if counter < len && startIndex <= counter >
        <tr>
          <td>
            ${indent}
            <#if content.contentTypeId == "WEB_SITE_PUB_PT" >
              <a class="tabButton"
                  href="<@ofbizUrl>showcontenttree?contentId=${content.contentId!}&nodeTrailCsv=${csv}</@ofbizUrl>">
                ${uiLabelMap.CommonBackTo}
              </a>
              &nbsp;${content.contentName!}
            <#else>
              <a class="tabButton"
                  href="<@ofbizUrl>showcontenttree?contentId=${siteId!}&nodeTrailCsv=${csv}</@ofbizUrl>">
                ${uiLabelMap.CommonBackTo}
              </a>
              &nbsp;${content.contentName!}
            </#if>
            <#local indent = indent + "&nbsp;&nbsp;&nbsp;&nbsp;">
            [${content.contentId!}]
            <#if searchOn?has_content && searchOn?lower_case == "true">
              &nbsp;
              <a class="tabButton"
                  href="<@ofbizUrl>searchContent?siteId=${siteId!}&nodeTrailCsv=${csv}</@ofbizUrl>">
                ${uiLabelMap.CommonSearch}
              </a>
            </#if>
            </#if>
          </td>
        </tr>
      </#if>
      <#local counter = counter + 1>
      <#if 20 < counter > <#break/></#if>
    </#list>
  </table>
</#macro>
