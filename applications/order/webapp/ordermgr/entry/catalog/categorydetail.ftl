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
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@version    $Rev$
 *@since      2.1
-->

<#if productCategory?exists>
    <div class="head1">
        <div>${categoryContentWrapper.get("CATEGORY_NAME")?if_exists}</div>
        <div>${categoryContentWrapper.get("DESCRIPTION")?if_exists}</div>
        <#if hasQuantities?exists>
          <form method="post" action="<@ofbizUrl>addCategoryDefaults<#if requestAttributes._CURRENT_VIEW_?exists>/${requestAttributes._CURRENT_VIEW_}</#if></@ofbizUrl>" name="thecategoryform" style='margin: 0;'>
            <input type='hidden' name='add_category_id' value='${productCategory.productCategoryId}'/>
            <#if requestParameters.product_id?exists><input type='hidden' name='product_id' value='${requestParameters.product_id}'/></#if>
            <#if requestParameters.category_id?exists><input type='hidden' name='category_id' value='${requestParameters.category_id}'/></#if>
            <#if requestParameters.VIEW_INDEX?exists><input type='hidden' name='VIEW_INDEX' value='${requestParameters.VIEW_INDEX}'/></#if>
            <#if requestParameters.SEARCH_STRING?exists><input type='hidden' name='SEARCH_STRING' value='${requestParameters.SEARCH_STRING}'/></#if>
            <#if requestParameters.SEARCH_CATEGORY_ID?exists><input type='hidden' name='SEARCH_CATEGORY_ID' value='${requestParameters.SEARCH_CATEGORY_ID}'/></#if>                                     
            <a href="javascript:document.thecategoryform.submit()" class="buttontext"><nobr>${uiLabelMap.ProductAddProductsUsingDefaultQuantities}</nobr></a>
          </form>
        </#if>
        <a href="<@ofbizUrl>advancedsearch?SEARCH_CATEGORY_ID=${productCategory.productCategoryId}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductSearchinCategory}</a>
    </div>
  <#assign longDescription = categoryContentWrapper.get("LONG_DESCRIPTION")?if_exists/>
  <#assign categoryImageUrl = categoryContentWrapper.get("CATEGORY_IMAGE_URL")?if_exists/>
  <#if categoryImageUrl?has_content || longDescription?has_content>
      <div class="tabletext">
        <#if categoryImageUrl?has_content>
          <img src='<@ofbizContentUrl>${categoryImageUrl}</@ofbizContentUrl>' vspace='5' hspace='5' border='1' height='100' align='left'/>
        </#if>
        <#if longDescription?has_content>
          ${longDescription}
        </#if>
      </div>
  </#if>
</#if>

<#if productCategoryMembers?has_content>
    <div class="product-prevnext">
      <#-- Start Page Select Drop-Down -->
      <#assign viewIndexMax = Static["java.lang.Math"].ceil(listSize?double / viewSize?double)>
      <select name="pageSelect" class="selectBox" onchange="window.location=this[this.selectedIndex].value;">
        <option value="#">${uiLabelMap.CommonPage} ${viewIndex?int} ${uiLabelMap.CommonOf} ${viewIndexMax}</option>
        <#list 1..viewIndexMax as curViewNum>
          <option value="<@ofbizUrl>category/~category_id=${productCategoryId}/~VIEW_SIZE=${viewSize}/~VIEW_INDEX=${curViewNum?int}</@ofbizUrl>">${uiLabelMap.CommonGotoPage} ${curViewNum}</option>
        </#list>
      </select>
      <#-- End Page Select Drop-Down -->
      <b>
        <#if (viewIndex?int > 1)>
          <a href="<@ofbizUrl>category/~category_id=${productCategoryId}/~VIEW_SIZE=${viewSize}/~VIEW_INDEX=${viewIndex?int - 1}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPrevious}</a> |
        </#if>
        <#if (listSize?int > 0)>
          <span class="tabletext">${lowIndex} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}</span>
        </#if>
        <#if highIndex?int < listSize?int>
          | <a href="<@ofbizUrl>category/~category_id=${productCategoryId}/~VIEW_SIZE=${viewSize}/~VIEW_INDEX=${viewIndex?int + 1}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonNext}</a>
        </#if>
      </b>
    </div>

    <div class="productsummary-container">
        <#list productCategoryMembers as productCategoryMember>
            ${setRequestAttribute("optProductId", productCategoryMember.productId)}
            ${setRequestAttribute("productCategoryMember", productCategoryMember)}
            ${setRequestAttribute("listIndex", productCategoryMember_index)}
            ${screens.render(productsummaryScreen)}
        </#list>
    </div>

    <div class="product-prevnext">
      <#-- Start Page Select Drop-Down -->
      <#assign viewIndexMax = Static["java.lang.Math"].ceil(listSize?double / viewSize?double)>
      <select name="pageSelect" class="selectBox" onchange="window.location=this[this.selectedIndex].value;">
        <option value="#">Page ${viewIndex?int} ${uiLabelMap.CommonOf} ${viewIndexMax}</option>
        <#list 1..viewIndexMax as curViewNum>
          <option value="<@ofbizUrl>category/~category_id=${productCategoryId}/~VIEW_SIZE=${viewSize}/~VIEW_INDEX=${curViewNum?int}</@ofbizUrl>">${uiLabelMap.CommonGotoPage} ${curViewNum}</option>
        </#list>
      </select>
      <#-- End Page Select Drop-Down -->
      <b>
        <#if (viewIndex?int > 1)>
          <a href="<@ofbizUrl>category?category_id=${productCategoryId}&amp;VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndex?int - 1}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPrevious}</a> |
        </#if>
        <#if (listSize?int > 0)>
          <span class="tabletext">${lowIndex} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}</span>
        </#if>
        <#if highIndex?int < listSize?int>
          | <a href="<@ofbizUrl>category?category_id=${productCategoryId}&amp;VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndex?int + 1}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonNext}</a>
        </#if>
      </b>
    </div>

<#else>
    <div><hr class='sepbar'/></div>
    <div class='tabletext'>${uiLabelMap.ProductNoProductsInThisCategory}</div>
</#if>
