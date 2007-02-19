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

<#if requestAttributes.productCategoryLink?has_content>
<#assign productCategoryLink = requestAttributes.productCategoryLink>
<#if productCategoryLink.detailSubScreen?has_content>
    ${screens.render(productCategoryLink.detailSubScreen)}
<#else>
    <#if productCategoryLink.linkTypeEnumId == "PCLT_SEARCH_PARAM">
      <#assign linkUrl = requestAttributes._REQUEST_HANDLER_.makeLink(request, response, "keywordsearch?" + productCategoryLink.linkInfo)/>
    <#elseif productCategoryLink.linkTypeEnumId == "PCLT_ABS_URL">
      <#assign linkUrl = productCategoryLink.linkInfo?if_exists/>
    </#if>
    <div class="productcategorylink">
      <#if productCategoryLink.imageUrl?has_content>
        <div class="smallimage">
          <a href="${linkUrl}"><img src="<@ofbizContentUrl>${productCategoryLink.imageUrl}</@ofbizContentUrl>" alt="${productCategoryLink.titleText?default("Link Image")}"/></a>
        </div>
      </#if>
      <#if productCategoryLink.titleText?has_content>
        <a href="${linkUrl}" class="linktext">${productCategoryLink.titleText}</a>
      </#if>
      <#if productCategoryLink.detailText?has_content>
        <div class="tabletext">${productCategoryLink.detailText}</div>
      </#if>
    </div>
</#if>
</#if>
