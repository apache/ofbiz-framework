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
<#if productExportList?has_content>
  <#list productExportList as productExportMap><#assign productCategoryCount=0/><#assign productFeatureCount=0/>
    ${productExportMap.productId}    <#if productExportMap.productFeatureCustom?has_content>${productExportMap.productFeatureCustom.description!}</#if>    <#list     productExportMap.productCategories as productCategoryAndMember><#if productCategoryAndMember.categoryName?has_content><#if productCategoryCount &gt; 0>,</#if>${productCategoryAndMember.categoryName}<#assign productCategoryCount=productCategoryCount + 1/></#if></#list>    <#list     productExportMap.productFeatures as productFeatureAndAppl><#if productFeatureAndAppl.description?has_content><#if productFeatureCount &gt; 0>,</#if>${productFeatureAndAppl.description}<#assign productFeatureCount=productFeatureCount + 1/></#if></#list>
  </#list>
<#else>
    ${uiLabelMap.ProductErrorNothingToExport}
</#if>  