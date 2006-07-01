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
-->

<form method="post" action="<@ofbizUrl>/CreateFeatureCategory</@ofbizUrl>" style="margin: 0;">
  <div class="head2">${uiLabelMap.ProductCreateAProductFeatureCategory}:</div>
  <br/>
  <table>
    <tr>
      <td><div class="tabletext">${uiLabelMap.CommonDescription}:</div></td>
      <td><input type="text" class="inputBox" size="30" name="description" value=""></td>
    </tr>
    <tr>
      <td><div class="tabletext">${uiLabelMap.ProductParentCategory}:</div></td>
      <td><select name="parentCategoryId" size="1" class="selectbox">
        <option value="">&nbsp;</option>
        <#list productFeatureCategories as productFeatureCategory>
          <option value="${productFeatureCategory.productFeatureCategoryId}">${productFeatureCategory.description?if_exists} [${productFeatureCategory.productFeatureCategoryId}]</option>
        </#list>
      </select></td>
    </tr>
    <tr>
      <td colspan="2"><input type="submit" value="${uiLabelMap.CommonCreate}"></td>
    </tr>
  </table>
</form>
<br/>
