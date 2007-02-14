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
      <td align="left">
        <div class="head2">${productCategory.description?if_exists}</div>
      </td>
      <td align="right">
        <form name="choosequickaddform" method="post" action="<@ofbizUrl>quickadd</@ofbizUrl>" style='margin: 0;'>
          <select name='category_id' class='selectBox'>
            <option value='${productCategory.productCategoryId}'>${productCategory.description?if_exists}</option>
            <option value='${productCategory.productCategoryId}'>--</option>
            <#list quickAddCats as quickAddCatalogId>
              <#assign loopCategory = delegator.findByPrimaryKeyCache("ProductCategory", Static["org.ofbiz.base.util.UtilMisc"].toMap("productCategoryId", quickAddCatalogId))>
              <#if loopCategory?has_content>
                <option value='${quickAddCatalogId}'>${loopCategory.description?if_exists}</option>
              </#if>
            </#list>
          </select>
          <div><a href="javascript:document.choosequickaddform.submit()" class="buttontext">${uiLabelMap.ProductChooseQuickAddCategory}</a></div>
        </form>
      </td>
    </tr>
    <#if productCategory.categoryImageUrl?exists || productCategory.longDescription?exists>  
      <tr><td colspan='2'><hr class='sepbar'></td></tr>
      <tr>
        <td align="left" valign="top" width="0" colspan='2'>
          <div class="tabletext">
            <#if productCategory.categoryImageUrl?exists>
              <img src="<@ofbizContentUrl>${productCategory.categoryImageUrl}</@ofbizContentUrl>" vspace="5" hspace="5" border="1" height='100' align="left">
            </#if>
            ${productCategory.longDescription?if_exists}
          </div>
        </td>
      </tr>
    </#if>
  </table>
</#if>

<#if productCategoryMembers?exists && 0 < productCategoryMembers?size>
  <br/>
  <center>
  <form method="post" action="<@ofbizUrl>addtocartbulk</@ofbizUrl>" name="bulkaddform" style='margin: 0;'>
    <input type='hidden' name='category_id' value='${categoryId}'>
    <div class="tabletext" align="right">
      <a href="javascript:document.bulkaddform.submit()" class="buttontext"><span style="white-space: nowrap;">${uiLabelMap.EcommerceAddAlltoCart}</span></a>
    </div>     
    <table border='1' cellpadding='2' cellspacing='0'>      
      <#list productCategoryMembers as productCategoryMember>
        <#assign product = productCategoryMember.getRelatedOneCache("Product")>
        <tr>
            ${setRequestAttribute("optProductId", productCategoryMember.productId)} 
            ${screens.render(quickaddsummaryScreen)}
        </tr>        
      </#list> 
    </table>
    <div class="tabletext" align="right">
      <a href="javascript:document.bulkaddform.submit()" class="buttontext"><span style="white-space: nowrap;">${uiLabelMap.EcommerceAddAlltoCart}</span></a>
    </div>      
  </form>
  </center>
<#else>
  <table border="0" cellpadding="2">
    <tr><td colspan="2"><hr class='sepbar'></td></tr>
    <tr>
      <td>
        <div class='tabletext'>${uiLabelMap.ProductNoProductsInThisCategory}.</div>
      </td>
    </tr>
  </table>
</#if>
