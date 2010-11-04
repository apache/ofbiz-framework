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

<#if productCategory?has_content>
  <table border='0'  cellpadding='3' cellspacing='0'>
    <tr>
      <td>
        <h2>${productCategory.categoryName?if_exists}</h2>
      </td>
      <td align="right">
        <form name="choosequickaddform" method="post" action="<@ofbizUrl>quickadd</@ofbizUrl>" style='margin: 0;'>
          <select name='category_id'>
            <option value='${productCategory.productCategoryId}'>${productCategory.categoryName?if_exists}</option>
            <option value='${productCategory.productCategoryId}'>--</option>
            <#list quickAddCats as quickAddCatalogId>
              <#assign loopCategory = delegator.findByPrimaryKeyCache("ProductCategory", Static["org.ofbiz.base.util.UtilMisc"].toMap("productCategoryId", quickAddCatalogId))>
              <#if loopCategory?has_content>
                <option value='${quickAddCatalogId}'>${loopCategory.categoryName?if_exists}</option>
              </#if>
            </#list>
          </select>
          <div><a href="javascript:document.choosequickaddform.submit()" class="buttontext">${uiLabelMap.ProductChooseQuickAddCategory}</a></div>
        </form>
      </td>
    </tr>
    <#if productCategory.categoryImageUrl?exists || productCategory.longDescription?exists>
      <tr><td colspan='2'><hr class='sepbar'/></td></tr>
      <tr>
        <td valign="top" width="0" colspan='2'>
          <div>
            <#if productCategory.categoryImageUrl?exists>
              <img src="<@ofbizContentUrl>${productCategory.categoryImageUrl}</@ofbizContentUrl>" vspace="5" hspace="5" border="1" height='100' alt="" />
            </#if>
            ${productCategory.longDescription?if_exists}
          </div>
        </td>
      </tr>
    </#if>
  </table>
</#if>

<#if productCategoryMembers?exists && 0 < productCategoryMembers?size>
  <br />
  <center>
  <form method="post" action="<@ofbizUrl>addtocartbulk</@ofbizUrl>" name="bulkaddform" style='margin: 0;'>
    <input type='hidden' name='category_id' value='${categoryId}' />
    <div class="quickaddall">
      <a href="javascript:document.bulkaddform.submit()" class="buttontext">${uiLabelMap.OrderAddAllToCart}</a>
    </div>
    <div class="quickaddtable">
      <#list productCategoryMembers as productCategoryMember>
        <#assign product = productCategoryMember.getRelatedOneCache("Product")>
        <p>
            ${setRequestAttribute("optProductId", productCategoryMember.productId)}
            ${screens.render(quickaddsummaryScreen)}
        </p>
      </#list>
    </div>
    <div class="quickaddall">
      <a href="javascript:document.bulkaddform.submit()" class="buttontext">${uiLabelMap.OrderAddAllToCart}</a>
    </div>
  </form>
  </center>
<#else>
  <table border="0" cellpadding="2">
    <tr><td colspan="2"><hr class='sepbar'/></td></tr>
    <tr>
      <td>
        <div class='tabletext'>${uiLabelMap.ProductNoProductsInThisCategory}.</div>
      </td>
    </tr>
  </table>
</#if>

