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

<div class="screenlet">
    <div class="screenlet-header">
        <div class="simple-right-small">
            <#if isOpen>
                <a href="<@ofbizUrl>main?CategoryProductsState=close</@ofbizUrl>" class="lightbuttontext">&nbsp;_&nbsp;</a>
            <#else>
                <a href="<@ofbizUrl>main?CategoryProductsState=open</@ofbizUrl>" class="lightbuttontext">&nbsp;[]&nbsp;</a>
            </#if>
        </div>
        <div class="boxhead">${uiLabelMap.ProductCategoryProducts}</div>
    </div>
<#if isOpen>
    <div class="screenlet-body">
        <#if productCategory?exists>
          <#if productCategoryMembers?has_content>
              <#list productCategoryMembers as productCategoryMember>
                <#assign product = productCategoryMember.getRelatedOneCache("Product")>
                  <div>
                    <a href='<@ofbizUrl>EditProduct?productId=${product.productId}</@ofbizUrl>' class='buttontext'>
                      ${product.internalName?default("${uiLabelMap.CommonNo} ${uiLabelMap.ProductInternalName}")}
                    </a>
                    <div class='tabletext'>
                      <b>${product.productId}</b>
                    </div>
                  </div>
              </#list>
              <#if (listSize > viewSize)>
                  <div>
                    <div class='tabletext'>NOTE: Only showing the first ${viewSize} of ${listSize} products. To view the rest, use the Products tab for this category.</div>
                  </div>
              </#if>
          <#else>
            <div class='tabletext'>${uiLabelMap.ProductNoProductsInCategory}.</div>
          </#if>
        <#else>
            <div class='tabletext'>${uiLabelMap.ProductNoCategorySpecified}.</div>
        </#if>
    </div>
</#if>
</div>
