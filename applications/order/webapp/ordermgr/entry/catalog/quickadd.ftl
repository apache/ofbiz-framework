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
              <img src="${productCategory.categoryImageUrl}" vspace="5" hspace="5" border="1" height='100' align="left">
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
      <a href="javascript:document.bulkaddform.submit()" class="buttontext"><nobr>${uiLabelMap.EcommerceAddAlltoCart}</nobr></a>
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
      <a href="javascript:document.bulkaddform.submit()" class="buttontext"><nobr>${uiLabelMap.EcommerceAddAlltoCart}</nobr></a>
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
