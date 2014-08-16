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

<div class="breadcrumbs">
<#assign isDefaultTheme = !layoutSettings.VT_FTR_TMPLT_LOC?contains("multiflex")>        
<#if isDefaultTheme>
  <a href="<@ofbizUrl>main</@ofbizUrl>" class="linktext">${uiLabelMap.CommonMain}</a> &gt;
<#else>
  <ul>
    <li>
      <a href="<@ofbizUrl>main</@ofbizUrl>" class="linktext">${uiLabelMap.CommonMain}</a>
    </li>
</#if>    
    <#-- Show the category branch -->
    <#if productCategoryTrail??>
        <#list productCategoryTrail as trail>
          <#if !isDefaultTheme>                 
            <li>
              <a href="<@ofbizCatalogAltUrl productCategoryId=trail.productCategoryId previousCategoryId=trail.parentCategory!""/>" class="linktext">
                <#if trail.title??>
                  ${trail.title}
                <#else>
                  ${trail.productCategoryId}
                </#if>
              </a>
            </li>
          <#else>
            <a href="<@ofbizCatalogAltUrl productCategoryId=trail.productCategoryId previousCategoryId=trail.parentCategory!""/>" class="linktext">
              <#if trail.title??>
                ${trail.title} >
              <#else>
                ${trail.productCategoryId} >
              </#if>
            </a>
          </#if>
        </#list>
    </#if>
    <#if !isDefaultTheme>                 
      <li>
        <a href="<@ofbizCatalogAltUrl productCategoryId=currentCategoryId previousCategoryId=parameters.parentCategoryStr/>" class="linktext">
          <#if currentCategoryName??>
            ${currentCategoryName}
          <#elseif currentCategoryDescription??>
            ${currentCategoryDescription}
          <#else>
            ${currentCategoryId}
          </#if>
        </a>
      </li>
    <#else>
      <a href="<@ofbizCatalogAltUrl productCategoryId=currentCategoryId previousCategoryId=parameters.parentCategoryStr/>" class="linktext">
        <#if currentCategoryName??>
          ${currentCategoryName}
        <#elseif currentCategoryDescription??>
          ${currentCategoryDescription}
        <#else>
          ${currentCategoryId}
        </#if>
      </a>
    </#if>
  </ul>
</div>
<br />

