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

<#if productCategory??>
  <#if productCategoryMembers?has_content>
      <#list productCategoryMembers as productCategoryMember>
        <#assign product = productCategoryMember.getRelatedOne("Product", true)>
          <div>
            <a href='<@ofbizUrl>EditProduct?productId=${product.productId}</@ofbizUrl>' class='buttontext'>
              <#if product.internalName?has_content>
                ${product.internalName}
              <#else>
                ${product.productName?default("${uiLabelMap.CommonNo} ${uiLabelMap.ProductInternalName} / ${uiLabelMap.ProductProductName}")}
              </#if>    
            </a>
            <div>
              <b>${product.productId}</b>
            </div>
          </div>
      </#list>
      <#if (listSize > viewSize)>
          <div>
            <div>NOTE: Only showing the first ${viewSize} of ${listSize} products. To view the rest, use the Products tab for this category.</div>
          </div>
      </#if>
  <#else>
    <div>${uiLabelMap.ProductNoProductsInCategory}.</div>
  </#if>
<#else>
    <div>${uiLabelMap.ProductNoCategorySpecified}.</div>
</#if>
