<#--
 *  Copyright (c) 2003-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a 
 *  copy of this software and associated documentation files (the "Software"), 
 *  to deal in the Software without restriction, including without limitation 
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 *  and/or sell copies of the Software, and to permit persons to whom the 
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included 
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT 
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     David E. Jones (jonesde@ofbiz.org)
 *@author     Catherine Heintz (catherine.heintz@nereide.biz)
 *@version    $Rev$
 *@since      2.1
-->
<#if (requestAttributes.uiLabelMap)?exists><#assign uiLabelMap = requestAttributes.uiLabelMap></#if>

<div class="screenlet">
    <div class="screenlet-header">
        <div class="simple-right-small">
            <#if isOpen>
                <a href='<@ofbizUrl>main?BrowseCatalogsState=close</@ofbizUrl>' class='lightbuttontext'>&nbsp;_&nbsp;</a>
            <#else>
                <a href='<@ofbizUrl>main?BrowseCatalogsState=open</@ofbizUrl>' class='lightbuttontext'>&nbsp;[]&nbsp;</a>
            </#if>
        </div>
        <div class="boxhead">${uiLabelMap.ProductBrowseCatalogs}</div>
    </div>
<#if isOpen>
    <div class="screenlet-body">
        <div><a href='<@ofbizUrl>FindProdCatalog</@ofbizUrl>' class='buttontext'>${uiLabelMap.ProductCatalogDetailList}</a></div>
        <div style='margin-left: 10px;'>
          <#assign sortList = Static["org.ofbiz.base.util.UtilMisc"].toList("prodCatalogCategoryTypeId", "sequenceNum", "productCategoryId")>
          <#list prodCatalogs as prodCatalog>
          <#if curProdCatalogId?exists && curProdCatalogId == prodCatalog.prodCatalogId>
            <#assign prodCatalogCategories = prodCatalog.getRelatedOrderByCache("ProdCatalogCategory", sortList)>
            <div class='browsecategorytext'>-&nbsp;<a href="<@ofbizUrl>EditProdCatalog?prodCatalogId=${prodCatalog.prodCatalogId}</@ofbizUrl>" class='browsecategorybutton'>${prodCatalog.catalogName?if_exists}</a></div>
              <div style='margin-left: 10px;'>
                <#list prodCatalogCategories as prodCatalogCategory>
                  <#assign productCategory = prodCatalogCategory.getRelatedOneCache("ProductCategory")>
                  <div class='browsecategorytext'>-&nbsp;<a href='<@ofbizUrl>EditCategory?CATALOG_TOP_CATEGORY=${prodCatalogCategory.productCategoryId}&amp;productCategoryId=${prodCatalogCategory.productCategoryId}</@ofbizUrl>' class="browsecategorybutton">${(productCategory.description)?if_exists}</a></div>
                </#list>
              </div>
          <#else>
            <div class='browsecategorytext'>-&nbsp;<a href="<@ofbizUrl>EditProdCatalog?prodCatalogId=${prodCatalog.prodCatalogId}</@ofbizUrl>" class='browsecategorybutton'>${prodCatalog.catalogName?if_exists}</a></div>
          </#if>
          </#list>
        </div>
    </div>
</#if>
</div>

