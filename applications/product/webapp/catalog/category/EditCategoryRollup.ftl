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
  <form method="post" action="<@ofbizUrl>updateProductCategoryToCategory</@ofbizUrl>" name="updateProductCategoryForm">
  <input type="hidden" name="showProductCategoryId" value="${productCategoryId}">
  <#list currentProductCategoryRollups as productCategoryRollup>
    <#assign suffix = "_o_" + productCategoryRollup_index>
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
            <a href="javascript:call_cal(document.updateProductCategoryForm.thruDate${suffix}, null);"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
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
      <input type="hidden" value="${currentProductCategoryRollups.size()}" name="_rowCount">
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
    <input type="text" class="inputBox" name="parentProductCategoryId" size="20" maxlength="20" value="${requestParameters.SEARCH_CATEGORY_ID?if_exists}"/>
    <a href="javascript:call_fieldlookup2(document.addParentForm.parentProductCategoryId,'LookupProductCategory');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'/></a>
  <input type="text" size="25" name="fromDate" class="inputBox">
  <a href="javascript:call_cal(document.addParentForm.fromDate, null);"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
  <input type="submit" value="${uiLabelMap.CommonAdd}">
</form>
<br/>
<hr/>
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
            <a href="javascript:call_cal(document.updateProductCategoryToCategoryChild.thruDate${suffix}, null);"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
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
    <input type="text" class="inputBox" name="productCategoryId" size="20" maxlength="20" value="${requestParameters.SEARCH_CATEGORY_ID?if_exists}"/>
    <a href="javascript:call_fieldlookup2(document.addChildForm.productCategoryId,'LookupProductCategory');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'/></a>
  <input type="text" size="25" name="fromDate" class="inputBox">
  <a href="javascript:call_cal(document.addChildForm.fromDate, null);"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
  <input type="submit" value="${uiLabelMap.CommonAdd}">
</form>
</#if>
