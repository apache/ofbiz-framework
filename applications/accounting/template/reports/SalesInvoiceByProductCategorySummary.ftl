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

<#macro resultSummary resultMap>
    <#if resultMap?has_content>
        ${resultMap.quantityTotal?default(0)}:${resultMap.amountTotal?default(0)}:<#if (resultMap.quantityTotal?? && resultMap.quantityTotal > 0)>${resultMap.amountTotal/resultMap.quantityTotal}<#else>0</#if>
    <#else>
        0:0:0
    </#if>
</#macro>

<ul>
    <li>Month: ${month}/${year}</li>
    <li>Root Category: ${(Static["org.apache.ofbiz.product.category.CategoryContentWrapper"].getProductCategoryContentAsText(rootProductCategory, "CATEGORY_NAME", locale, dispatcher, "html"))!} [${rootProductCategoryId}]</li>
    <li>Organization: ${(organizationPartyName.groupName)!} [${organizationPartyId?default("No Organization Specified")}]</li>
    <li>Currency: ${(currencyUom.description)!} [${currencyUomId?default("No Currency Specified")}]</li>
</ul>
<div>NOTE: each set of numbers is: &lt;quantity&gt;:&lt;total amount&gt;:&lt;average amount&gt;</div>
<table class="basic-table" cellspacing="0">
    <#-- Create the header row -->
    <tr class="header-row">
        <td>Day</td>
        <td>[No Product]</td>
    <#list productList as product>
        <td>${product.internalName?default((Static["org.apache.ofbiz.product.product.ProductContentWrapper"].getProductContentAsText(product, "PRODUCT_NAME", locale, dispatcher, "html"))!)}<br />P:[${product.productId}]</td>
    </#list>
    <#list productCategoryList as productCategory>
        <td>${(Static["org.apache.ofbiz.product.category.CategoryContentWrapper"].getProductCategoryContentAsText(productCategory, "CATEGORY_NAME", locale, dispatcher, "html"))!}<br />C:[${productCategory.productCategoryId}]</td>
    </#list>
    </tr>
    <#-- Days of the month -->
    <#list productNullResultByDayList as productNullResult>
        <#assign productResultMap = productResultMapByDayList.get(productNullResult_index)/>
        <#assign categoryResultMap = categoryResultMapByDayList.get(productNullResult_index)/>

        <#-- now do the null product, then iterate through the products, then categories -->
        <tr>
            <td class="label">${(productNullResult_index + 1)}</td>
            <td><@resultSummary resultMap=productNullResult/></td>
        <#list productList as product>
            <#assign productResult = productResultMap[product.productId]!/>
            <td><@resultSummary resultMap=productResult/></td>
        </#list>
        <#list productCategoryList as productCategory>
            <#assign categoryResult = categoryResultMap[productCategory.productCategoryId]!/>
            <td><@resultSummary resultMap=categoryResult/></td>
        </#list>
        </tr>
    </#list>

    <#-- Totals for the month -->
    <tr>
        <td class="label">Month Total</td>
        <td><@resultSummary resultMap=monthProductNullResult/></td>
    <#list productList as product>
        <#assign productResult = monthProductResultMap[product.productId]!/>
        <td><@resultSummary resultMap=productResult/></td>
    </#list>
    <#list productCategoryList as productCategory>
        <#assign categoryResult = monthCategoryResultMap[productCategory.productCategoryId]!/>
        <td><@resultSummary resultMap=categoryResult/></td>
    </#list>
    </tr>
</table>
