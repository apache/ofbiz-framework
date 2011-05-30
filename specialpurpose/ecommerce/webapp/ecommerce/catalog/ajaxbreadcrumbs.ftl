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
  <ul>
    <li>
      <a href="<@ofbizUrl>main</@ofbizUrl>" class="linktext">${uiLabelMap.CommonMain}</a>
    </li>
    <#-- Show the category branch -->
    <#if productCategoryTrail?exists>
        <#list productCategoryTrail as trail>
        <li>
          <a href="<@ofbizCatalogUrl currentCategoryId=trail.productCategoryId previousCategoryId=trail.parentCategory/>" class="linktext">
            <#if trail.title?exists>
              ${trail.title}
            <#else>
              ${trail.productCategoryId}
            </#if>
          </a>
        </li>
        </#list>
    </#if>
    <li>
      <a href="<@ofbizCatalogUrl currentCategoryId=currentCategoryId previousCategoryId=parameters.parentCategoryStr/>" class="linktext">
        <#if currentCategoryName?exists>
          ${currentCategoryName}
        <#elseif currentCategoryDescription?exists>
          ${currentCategoryDescription}
        <#else>
          ${currentCategoryId}
        </#if>
      </a>
    </li>
    <#-- Show the product, if there is one -->
    <#if productContentWrapper?exists>
    <li>${productContentWrapper.get("PRODUCT_NAME")?if_exists}</li>
    </#if>
  </ul>
</div>
<br />

