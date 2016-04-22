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
<div class="screenlet">
    <div class="screenlet-title-bar">
        <h3>${uiLabelMap.ProductOverrideSimpleFields}</h3>
    </div>
    <div class="screenlet-body">
        <form action="<@ofbizUrl>updateCategoryContent</@ofbizUrl>" method="post" style="margin: 0;" name="categoryForm">
            <table cellspacing="0" class="basic-table">
                <tr>
                    <td width="26%" align="right" class="label"><input type="hidden" name="productCategoryId" value="${productCategoryId!}" />${uiLabelMap.ProductProductCategoryType}</td>
                    <td>&nbsp;</td>
                    <td width="74%">
                        <select name="productCategoryTypeId" size="1">
                        <option value="">&nbsp;</option>
                        <#list productCategoryTypes as productCategoryTypeData>
                            <option <#if productCategory?has_content><#if productCategory.productCategoryTypeId==productCategoryTypeData.productCategoryTypeId> selected="selected"</#if></#if> value="${productCategoryTypeData.productCategoryTypeId}">${productCategoryTypeData.get("description",locale)}</option>
                        </#list>
                        </select>
                    </td>
                </tr>
                <tr>
                    <td width="26%" align="right" class="label">${uiLabelMap.ProductName}</td>
                    <td>&nbsp;</td>
                    <td width="74%"><input type="text" value="${(productCategory.categoryName)!}" name="categoryName" size="60" maxlength="60"/></td>
                </tr>
                <tr>
                    <td width="26%" align="right" class="label">${uiLabelMap.ProductCategoryDescription}</td>
                    <td>&nbsp;</td>
                    <td width="74%" colspan="4" valign="top">
                        <textarea name="description" cols="60" rows="2">${(productCategory.description)!}</textarea>
                    </td>
                </tr>
                <tr>
                    <td width="26%" align="right" valign="top" class="label">${uiLabelMap.ProductLongDescription}</td>
                    <td>&nbsp;</td>
                    <td width="74%" colspan="4" valign="top">
                        <textarea name="longDescription" cols="60" rows="7">${(productCategory.longDescription)!}</textarea>
                    </td>
                </tr>
                <tr>
                    <td width="26%" align="right" class="label">${uiLabelMap.ProductDetailScreen}</td>
                    <td>&nbsp;</td>
                    <td width="74%">
                        <input type="text" <#if productCategory?has_content>value="${productCategory.detailScreen!}"</#if> name="detailScreen" size="60" maxlength="250" />
                        <br />
                        <span class="tooltip">${uiLabelMap.ProductDefaultsTo} &quot;categorydetail&quot;, ${uiLabelMap.ProductDetailScreenMessage}: &quot;component://ecommerce/widget/CatalogScreens.xml#categorydetail&quot;</span>
                    </td>
                </tr>
                <tr>
                    <td colspan="2">&nbsp;</td>
                    <td><input type="submit" name="Update" value="${uiLabelMap.CommonUpdate}" /></td>
                    <td colspan="3">&nbsp;</td>
                </tr>
            </table>
        </form>
    </div>
</div>