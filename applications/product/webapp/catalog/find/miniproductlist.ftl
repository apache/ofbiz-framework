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
 *@author     Catherine.Heintz@nereide.biz (migration to UiLabel)
 *@version    $Rev$
 *@since      2.1
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
