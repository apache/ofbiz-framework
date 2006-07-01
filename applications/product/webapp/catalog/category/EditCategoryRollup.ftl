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

<#if productCategoryId?has_content> 
<p class="head2">${uiLabelMap.ProductCategoryRollupParentCategories}</p>

<table border="1" cellpadding="2" cellspacing="0">
  <tr>
    <td><div class="tabletext"><b>${uiLabelMap.ProductParentCategoryId}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.CommonFromDate}</b></div></td>
    <td align="center"><div class="tabletext"><b>${uiLabelMap.ProductThruDateTimeSequence}</b></div></td>
    <td><div class="tabletext"><b>&nbsp;</b></div></td>
  </tr>
<#if currentProductCategoryRollups.size() != 0>
  <#assign lineParent = 0>
  <form method="post" action="<@ofbizUrl>updateProductCategoryToCategory</@ofbizUrl>" name="updateProductCategoryForm">
    <input type="hidden" name="showProductCategoryId" value="${productCategoryId}">
  <#list currentProductCategoryRollups as productCategoryRollup>
    <#assign suffix = "_o_" + lineParent>
    <#assign lineParent = lineParent + 1>
    <#assign curCategory = productCategoryRollup.getRelatedOne("ParentProductCategory")>
    <#assign hasntStarted = false>
    <#if productCategoryRollup.fromDate?exists && nowTimestamp.before(productCategoryRollup.getTimestamp("fromDate"))><#assign hasntStarted = true></#if>
    <#assign hasExpired = false>
    <#if productCategoryRollup.thruDate?exists && nowTimestamp.after(productCategoryRollup.getTimestamp("thruDate"))><#assign hasExpired = true></#if>
    <tr valign="middle">
      <td><#if curCategory?has_content><a href="<@ofbizUrl>EditCategory?productCategoryId=${curCategory.productCategoryId}</@ofbizUrl>" class="buttontext">${curCategory.description?if_exists} [${curCategory.productCategoryId}]</a></#if></td>
      <td><div class="tabletext" <#if hasntStarted>style="color: red;"</#if>>${productCategoryRollup.fromDate}</div></td>
      <td align="center">
            <input type="hidden" name="showProductCategoryId${suffix}" value="${productCategoryRollup.productCategoryId}">
            <input type="hidden" name="productCategoryId${suffix}" value="${productCategoryRollup.productCategoryId}">
            <input type="hidden" name="parentProductCategoryId${suffix}" value="${productCategoryRollup.parentProductCategoryId}">
            <input type="hidden" name="fromDate${suffix}" value="${productCategoryRollup.fromDate}">
            <input type="text" size="25" name="thruDate${suffix}" value="${productCategoryRollup.thruDate?if_exists}" class="inputBox" <#if hasExpired>style="color: red"</#if>>
            <a href="javascript:call_cal(document.updateProductCategoryForm.thruDate${suffix}, null);"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"></a>
            <input type="text" size="5" name="sequenceNum${suffix}" value="${productCategoryRollup.sequenceNum?if_exists}" class="inputBox">
      </td>
      <td>
        <a href="<@ofbizUrl>removeProductCategoryFromCategory?showProductCategoryId=${productCategoryId}&productCategoryId=${productCategoryRollup.productCategoryId}&parentProductCategoryId=${productCategoryRollup.parentProductCategoryId}&fromDate=${productCategoryRollup.fromDate}</@ofbizUrl>" class="buttontext">
        [${uiLabelMap.CommonDelete}]</a>
      </td>
    </tr>
  </#list>
  <tr valign="middle">
    <td colspan="3" align="center">
      <input type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;">
      <input type="hidden" value="${lineParent}" name="_rowCount">
    </td>
  </tr>
  </form>
</#if>
<#if currentProductCategoryRollups.size() == 0>
  <tr valign="middle">
    <td colspan="5"><DIV class="tabletext">${uiLabelMap.ProductNoParentCategoriesFound}.</DIV></td>
  </tr>
</#if>
</table>
<br/>
<form method="post" action="<@ofbizUrl>addProductCategoryToCategory</@ofbizUrl>" style="margin: 0;" name="addParentForm">
  <input type="hidden" name="productCategoryId" value="${productCategoryId}">
  <input type="hidden" name="showProductCategoryId" value="${productCategoryId}">
  <div class="tabletext">${uiLabelMap.CommonAddA} <b>${uiLabelMap.ProductParent}</b> ${uiLabelMap.ProductCategorySelectCategoryAndEnterFromDate}:</div>
    <select name="parentProductCategoryId" class="selectBox">
    <#list productCategoryCol as curCategory>
        <#if productCategoryId != curCategory.productCategoryId>
          <option value="${curCategory.productCategoryId}">${curCategory.description?if_exists} [${curCategory.productCategoryId}]</option>
        </#if>
    </#list>
    </select>
  <input type="text" size="25" name="fromDate" class="inputBox">
  <a href="javascript:call_cal(document.addParentForm.fromDate, null);"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"></a>
  <input type="submit" value="${uiLabelMap.CommonAdd}">
</form>
<br/>
<hr>
<br/>

<p class="head2">${uiLabelMap.ProductCategoryRollupChildCategories}</p>

<table border="1" cellpadding="2" cellspacing="0">
  <tr>
    <td><div class="tabletext"><b>${uiLabelMap.ProductChildCategoryId}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.CommonFromDate}</b></div></td>
    <td align="center"><div class="tabletext"><b>${uiLabelMap.ProductThruDateTimeSequence}</b></div></td>
    <td><div class="tabletext"><b>&nbsp;</b></div></td>
  </tr>
<#if parentProductCategoryRollups.size() != 0>
  <form method="post" action="<@ofbizUrl>updateProductCategoryToCategory</@ofbizUrl>" name="updateProductCategoryToCategoryChild">
  <input type="hidden" name="showProductCategoryId" value="${productCategoryId}">
  <#assign lineChild = 0>
  <#list parentProductCategoryRollups as productCategoryRollup>
    <#assign suffix = "_o_" + lineChild>
    <#assign lineChild = lineChild + 1>
    <#assign curCategory = productCategoryRollup.getRelatedOne("CurrentProductCategory")>
    <#assign hasntStarted = false>
    <#if productCategoryRollup.fromDate?exists && nowTimestamp.before(productCategoryRollup.getTimestamp("fromDate"))><#assign hasntStarted = true></#if>
    <#assign hasExpired = false>
    <#if productCategoryRollup.thruDate?exists && nowTimestamp.after(productCategoryRollup.getTimestamp("thruDate"))><#assign hasExpired = true></#if>
    <tr valign="middle">
      <td><#if curCategory?has_content><a href="<@ofbizUrl>EditCategory?productCategoryId=${curCategory.productCategoryId}</@ofbizUrl>" class="buttontext">${curCategory.description?if_exists} [${curCategory.productCategoryId}]</a></#if>
      <td><div class="tabletext" <#if hasntStarted>style="color: red"</#if>>${productCategoryRollup.fromDate}</div></td>
      <td align="center">
            <input type="hidden" name="productCategoryId${suffix}" value="${productCategoryRollup.productCategoryId}">
            <input type="hidden" name="parentProductCategoryId${suffix}" value="${productCategoryRollup.parentProductCategoryId}">
            <input type="hidden" name="fromDate${suffix}" value="${productCategoryRollup.fromDate}">
            <input type="text" size="25" name="thruDate${suffix}" value="${productCategoryRollup.thruDate?if_exists}" class="inputBox" <#if hasExpired>style="color: red;"</#if>>
            <a href="javascript:call_cal(document.updateProductCategoryToCategoryChild.thruDate${suffix}, null);"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"></a>
            <input type="text" size="5" name="sequenceNum${suffix}" value="${productCategoryRollup.sequenceNum?if_exists}" class="inputBox">
      </td>
      <td>
        <a href="<@ofbizUrl>removeProductCategoryFromCategory?showProductCategoryId=${productCategoryId}&productCategoryId=${productCategoryRollup.productCategoryId}&parentProductCategoryId=${productCategoryRollup.parentProductCategoryId}&fromDate=${productCategoryRollup.fromDate}</@ofbizUrl>" class="buttontext">
        [${uiLabelMap.CommonDelete}]</a>
      </td>
    </tr>
  </#list>
  <tr valign="middle">
    <td colspan="3" align="center">
      <input type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;">
      <input type="hidden" value="${lineChild}" name="_rowCount">
    </td>
  </tr>
</form>
</#if>
<#if parentProductCategoryRollups.size() == 0>
  <tr valign="middle">
    <td colspan="5"><div class="tabletext">${uiLabelMap.ProductNoChildCategoriesFound}.</div></td>
  </tr>
</#if>
</table>
<br/>

<form method="post" action="<@ofbizUrl>addProductCategoryToCategory</@ofbizUrl>" style="margin: 0;" name="addChildForm">
  <input type="hidden" name="showProductCategoryId" value="${productCategoryId}">
  <input type="hidden" name="parentProductCategoryId" value="${productCategoryId}">
  <div class="tabletext">${uiLabelMap.CommonAddA} <b>${uiLabelMap.ProductChild}</b> ${uiLabelMap.ProductCategorySelectCategoryAndEnterFromDate}:</div>
    <select name="productCategoryId" class="selectBox">
    <#list productCategoryCol as curCategory>
        <#if productCategoryId != curCategory.productCategoryId>
          <option value="${curCategory.productCategoryId}">${curCategory.description?if_exists} [${curCategory.productCategoryId}]</option>
        </#if>
    </#list>
    </select>
  <input type="text" size="25" name="fromDate" class="inputBox">
  <a href="javascript:call_cal(document.addChildForm.fromDate, null);"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"></a>
  <input type="submit" value="${uiLabelMap.CommonAdd}">
</form>
</#if>
