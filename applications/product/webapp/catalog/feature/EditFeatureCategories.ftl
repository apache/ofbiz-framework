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
 *@since      2.2
-->

<div class="head1">${uiLabelMap.ProductProductFeatureCategories}</div>

<br/>

<form method="post" action="<@ofbizUrl>EditFeature</@ofbizUrl>" style="margin: 0;">
  <div class="head2">${uiLabelMap.ProductEditFeatureId} :</div>
  <input type="text" class="inputBox" size="12" name="productFeatureId" value=""/>
  <input type="submit" value="${uiLabelMap.CommonEdit}"/>
</form>

<br/>

<table border="1" cellpadding="2" cellspacing="0">
  <tr>
    <td><div class="tabletext"><b>${uiLabelMap.CommonId}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.CommonDescription}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.ProductParentCategory}</b></div></td>
    <td><div class="tabletext">&nbsp;</div></td>
    <td><div class="tabletext">&nbsp;</div></td>
  </tr>


<#list productFeatureCategories as productFeatureCategory>
  <tr valign="middle">
    <form method="post" action="<@ofbizUrl>UpdateFeatureCategory</@ofbizUrl>">
    <input type="hidden" name="productFeatureCategoryId" value="${productFeatureCategory.productFeatureCategoryId}">
    <td><a href="<@ofbizUrl>EditFeatureCategoryFeatures?productFeatureCategoryId=${productFeatureCategory.productFeatureCategoryId}</@ofbizUrl>" class="buttontext">${productFeatureCategory.productFeatureCategoryId}</a></td>
    <td><input type="text" class="inputBox" size="30" name="description" value="${productFeatureCategory.description?if_exists}"></td>
    <td>
      <select name="parentCategoryId" size="1" class="selectBox">
        ${productFeatureCategory}
        <#assign curProdFeatCat = productFeatureCategory.getRelatedOne("ParentProductFeatureCategory")?if_exists>
        <#if curProdFeatCat?has_content>
          <option value="${curProdFeatCat.productFeatureCategoryId}">${curProdFeatCat.description?if_exists}</option>
        </#if>
        <option value="">&nbsp;</option>
          <#list productFeatureCategories as dropDownProductFeatureCategory>
            <option value="${dropDownProductFeatureCategory.productFeatureCategoryId}">${dropDownProductFeatureCategory.description?if_exists}</option>
          </#list>
      </select>
    </td>
    <td><INPUT type="submit" value="${uiLabelMap.CommonUpdate}"></td>
    <td><a href="<@ofbizUrl>EditFeatureCategoryFeatures?productFeatureCategoryId=${productFeatureCategory.productFeatureCategoryId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonEdit}]</a></td>
    </form>
  </tr>
</#list>
</table>
<br/>

