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
    
<hr class="sepbar"/>

<div class="head2">${uiLabelMap.ProductOverrideSimpleFields}</div>
<form action="<@ofbizUrl>updateCategoryContent</@ofbizUrl>" method="post" style="margin: 0;" name="categoryForm">
<table border="0" cellpadding="2" cellspacing="0">
<input type="hidden" name="productCategoryId" value="${productCategoryId?if_exists}">
    <tr>
    <td width="26%" align="right"><div class="tabletext">${uiLabelMap.ProductProductCategoryType}</div></td>
    <td>&nbsp;</td>
    <td width="74%">
      <select name="productCategoryTypeId" size="1" class="selectBox">
        <option value="">&nbsp;</option>
        <#list productCategoryTypes as productCategoryTypeData>
          <option <#if productCategory?has_content><#if productCategory.productCategoryTypeId==productCategoryTypeData.productCategoryTypeId> selected</#if></#if> value="${productCategoryTypeData.productCategoryTypeId}">${productCategoryTypeData.get("description",locale)}</option>
       </#list>
      </select>
    </td>
  </tr>    
        
  <tr>
    <td width="26%" align="right"><div class="tabletext">${uiLabelMap.ProductName}</div></td>
    <td>&nbsp;</td>
    <td width="74%"><input type="text" value="${(productCategory.categoryName)?if_exists}" name="categoryName" size="60" maxlength="60" class="inputBox"/></td>
  </tr>
  <tr>
    <td width="26%" align="right"><div class="tabletext">${uiLabelMap.ProductDescription}</div></td>
    <td>&nbsp;</td>
    <td width="80%" colspan="4" valign="top">
        <textarea class="textAreaBox" name="description" cols="60" rows="2">${(productCategory.description)?if_exists}</textarea>
    </td>
  </tr>
  <tr>
    <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.ProductLongDescription}</div></td>
    <td>&nbsp;</td>
    <td width="80%" colspan="4" valign="top">
       <textarea class="textAreaBox" name="longDescription" cols="60" rows="7">${(productCategory.longDescription)?if_exists}</textarea>
    </td>
  </tr>
  <tr>
    <td width="26%" align="right"><div class="tabletext">${uiLabelMap.ProductDetailScreen}</div></td>
    <td>&nbsp;</td>
    <td width="74%">
        <input type="text" <#if productCategory?has_content>value="${productCategory.detailScreen?if_exists}"</#if> name="detailScreen" size="60" maxlength="250" class="inputBox">
        <br/><span class="tabletext">${uiLabelMap.ProductDefaultsTo} &quot;categorydetail&quot;, ${uiLabelMap.ProductDetailScreenMessage}: &quot;component://ecommerce/widget/CatalogScreens.xml#categorydetail&quot;</span>
    </td>
  </tr>

<tr>
    <td colspan="2">&nbsp;</td>
    <td><input type="submit" name="Update" value="${uiLabelMap.CommonUpdate}"></td>
    <td colspan="3">&nbsp;</td>
</tr>
</table>
</form>
<hr class="sepbar"/>
