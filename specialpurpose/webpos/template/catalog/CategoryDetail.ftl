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
<#if productCategoryMembers??>
<table>
  <#assign numButton = 1/>
  <#assign cell = 0/>
  <tr>    
  <#list productCategoryMembers as productCategoryMember>
  <#assign product = productCategoryMember.getRelatedOne("Product", false)!>
  <#if product?? && product?has_content>
    <#assign smallImageUrl = Static["org.apache.ofbiz.product.product.ProductContentWrapper"].getProductContentAsText(product, "SMALL_IMAGE_URL", locale, dispatcher, "url")! />
    <#if !smallImageUrl?string?has_content>
      <#assign smallImageUrl = "/images/defaultImage.jpg">
    </#if>
    <#assign productName = Static["org.apache.ofbiz.product.product.ProductContentWrapper"].getProductContentAsText(product, "PRODUCT_NAME", locale, dispatcher, "html")! />
    <#if !productName?string?has_content>
      <#assign productName = Static["org.apache.ofbiz.product.product.ProductContentWrapper"].getProductContentAsText(product, "DESCRIPTION", locale, dispatcher, "html")! />
    </#if>
    <#assign addItemLink = "javascript:addItem('" + product.productId + "', '1', 'Y');">
    <td>
      <ol id="posButton">
        <#assign button = "button" + numButton>
        <li id="${button}" class="notSelectedButton">
          <#if smallImageUrl?string?has_content>
            <a href="${addItemLink}">
              <img src="<@ofbizContentUrl>${smallImageUrl}</@ofbizContentUrl>" align="center" class="cssImgSmall"/>
            </a>
          </#if>
        </li>
      </ol>
      <div class="linkButton">
        <a href="${addItemLink}">
          <b>${productName}</b>
        </a>
      </div>
    <td>
  </#if>
  <#assign numButton = numButton + 1/>
  <#assign cell = cell + 1/>
  <#if cell == 3>
  </tr>
  <#assign cell = 0/>
  </#if>
  </#list>
  <#if cell != 3>
  </tr>
  </#if>
</table>
<script language="JavaScript" type="text/javascript">
  showSelectedButton();
</script>
</#if>
