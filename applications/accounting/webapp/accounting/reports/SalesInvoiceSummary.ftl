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
        ${resultMap.quantityTotal?default(0)}:${resultMap.amountTotal?default(0)}:<#if (resultMap.quantityTotal?exists && resultMap.quantityTotal > 0)>${resultMap.amountTotal/resultMap.quantityTotal}<#else/>0</#if>
    <#else/>
        0:0:0
    </#if>
</#macro>

<div>Sales Invoice Summary Report for:</div>
<ul>
    <li>Month: ${month}/${year}</li>
    <li>Root Category: ${rootProductCategory.categoryName?if_exists} [${rootProductCategoryId}]</li>
    <li>Organization: ${(organizationPartyName.groupName)?if_exists} [${organizationPartyId?default("No Organization Specified")}]</li>
    <li>Currency: ${(currencyUom.description)?if_exists} [${currencyUomId?default("No Currency Specified")}]</li>
</il> 
<table>
    <#-- Create the header row -->
	<tr>
	    <th>Day</th>
	    <th>[No Product]</th>
    <#list productList as product>
	    <th>${product.productName?if_exists} [${product.productId}]</th>
    </#list>
    <#list productCategoryList as productCategory>
	    <th>${productCategory.categoryName?if_exists} [${productCategory.productCategoryId}]</th>
    </#list>
    </tr>
    <#-- Days of the month -->
    <#list productNullResultByDayList as productNullResult>
        <#assign productResultMap = productResultMapByDayList.get(productNullResult_index)/>
        <#assign categoryResultMap = categoryResultMapByDayList.get(productNullResult_index)/>
    
    	<#-- now do the null product, then iterate through the products, then categories -->
    	<tr>
    	    <td>${(productNullResult_index + 1)}</td>
    	    <td><@resultSummary resultMap=productNullResult/></td>
        <#list productList as product>
            <#assign productResult = productResultMap[product.productId]?if_exists/>
    	    <td><@resultSummary resultMap=productResult/></td>
        </#list>
        <#list productCategoryList as productCategory>
            <#assign categoryResult = categoryResultMap[productCategory.productCategoryId]?if_exists/>
    	    <td><@resultSummary resultMap=categoryResult/></td>
        </#list>
        </tr>
    </#list>
    <#-- Totals for the month -->
</table>
