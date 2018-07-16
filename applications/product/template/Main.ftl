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


<#if !sessionAttributes.userLogin??>
  <div class='label'> ${uiLabelMap.ProductGeneralMessage}.</div>
</#if>
<br />
<#if security.hasEntityPermission( "CATALOG", "_VIEW", session)>
  <div class="form-container">
    <form class="basic-form" method="post" action="<@ofbizUrl>EditProdCatalog</@ofbizUrl>" style="margin: 0;" name="EditProdCatalogForm">
      <table class="basic-table form-table">
        <tr>
          <td class="label"><label>${uiLabelMap.ProductEditCatalogWithCatalogId}:</label></td>
          <td>
            <input type="text" size="20" maxlength="20" name="prodCatalogId" value="" />
            <input type="submit" value=" ${uiLabelMap.ProductEditCatalog}" class="smallSubmit" />
          </td>
        </tr>
        <tr>
          <td class="label"><label>${uiLabelMap.CommonOr}:</label></td>
          <td><a href="<@ofbizUrl>EditProdCatalog</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductCreateNewCatalog}</a></td>
        </tr>
      </table>
    </form>
    <hr>
  </div>
  <div class="form-container">
  <form class="basic-form" method="post" action="<@ofbizUrl>EditCategory</@ofbizUrl>" style="margin: 0;" name="EditCategoryForm">
    <table class="basic-table">
      <tr>
        <td class="label"><label>${uiLabelMap.ProductEditCategoryWithCategoryId}:</label></td>
        <td>
          <@htmlTemplate.lookupField name="productCategoryId" id="productCategoryId" formName="EditCategoryForm" fieldFormName="LookupProductCategory" />
          <input type="submit" value="${uiLabelMap.ProductEditCategory}" class="smallSubmit" />
        </td>
      </tr>
      <tr>
        <td class="label"><label>${uiLabelMap.CommonOr}:</label></td>
        <td><a href="<@ofbizUrl>EditCategory</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductCreateNewCategory}</a></td>
      </tr>
    </table>
  </form>
  <hr>
  </div>
  <div class="form-container">
  <form class="basic-form" method="post" action="<@ofbizUrl>EditProduct</@ofbizUrl>" style="margin: 0;" name="EditProductForm">
    <table class="basic-table form-table">
      <tr>
        <td class="label"><label>${uiLabelMap.ProductEditProductWithProductId}:</label></td>
        <td>
          <@htmlTemplate.lookupField name="productId" id="productId" formName="EditProductForm" fieldFormName="LookupProduct" />
          <input type="submit" value=" ${uiLabelMap.ProductEditProduct}" class="smallSubmit" />
        </td>
      </tr>
    </table>
  </form>
  <hr>
  </div>
  <div class="form-container">
    <form class="basic-form" method="post" action="<@ofbizUrl>FindProductById</@ofbizUrl>" style="margin: 0;">
      <table class="basic-table form-table">
        <tr>
          <td class="label"><label>${uiLabelMap.ProductFindProductWithIdValue}:</label>
          </td>
          <td>
            <input type="text"  name="idValue" value="" />
            <input type="submit" value="${uiLabelMap.ProductFindProduct}" class="smallSubmit" />
          </td>
        </tr>
        <tr>
          <td class="label"><label>${uiLabelMap.CommonOr}:</label>
          </td>
          <td> <a href="<@ofbizUrl>EditProduct</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductCreateNewProduct}</a>
          <a href="<@ofbizUrl>CreateVirtualWithVariantsForm</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductQuickCreateVirtualFromVariants}</a></td>
        </tr>
        <tr>
          <td class="label"/>
          <td><a href="<@ofbizUrl>UpdateAllKeywords</@ofbizUrl>" class="buttontext"> ${uiLabelMap.ProductAutoCreateKeywordsForAllProducts}</a>
          </td>
        </tr>
        <tr>
            <td class="label"/>
            <td>
          <a href="<@ofbizUrl>FastLoadCache</@ofbizUrl>" class="buttontext"> ${uiLabelMap.ProductFastLoadCatalogIntoCache}</a></td>
        </tr>
      </table>
    </form>
  </div>
</#if>
