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

<#assign productFeaturesByTypeMap = Static["org.ofbiz.product.feature.ParametricSearch"].makeCategoryFeatureLists(productCategoryId, delegator)>

<#if productCategoryId?has_content>
    <a href="<@ofbizUrl>EditCategory?productCategoryId=${productCategoryId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductBackToEditCategory}]</a>
</#if>

<form name="createProductInCategoryCheckExistingForm" method="post" action="<@ofbizUrl>createProductInCategoryCheckExisting</@ofbizUrl>" style="margin: 0;">
    <input type="hidden" name="productCategoryId" value="${productCategoryId}">
    <table cellpadding="1" cellspacing="0" border="1">
        <#list productFeaturesByTypeMap.keySet() as productFeatureTypeId>
            <#assign findPftMap = Static["org.ofbiz.base.util.UtilMisc"].toMap("productFeatureTypeId", productFeatureTypeId)>
            <#assign productFeatureType = delegator.findByPrimaryKeyCache("ProductFeatureType", findPftMap)>
            <#assign productFeatures = productFeaturesByTypeMap[productFeatureTypeId]>
            <tr>
                <td width="15%">
                    <div class="tabletext">${productFeatureType.description}:</div>
                </td>
                <td>
                    <div class="tabletext">
                        <select class="selectBox" name="pft_${productFeatureTypeId}">
                            <option value="">- ${uiLabelMap.CommonNone} -</option>
                            <#list productFeatures as productFeature>
                                <option value="${productFeature.productFeatureId}">${productFeature.description}</option>
                            </#list>
                        </select>
                        <input type="checkbox" name="pftsel_${productFeatureTypeId}"/>${uiLabelMap.ProductSelectable}
                    </div>
                </td>
            </tr>
        </#list>
        <tr>
            <td width="15%"><div class="tabletext">${uiLabelMap.ProductInternalName}:</div></td>
            <td><input type="text" name="internalName" size="30" maxlength="60" class="inputBox"/></td>
        </tr>
        <tr>
            <td width="15%"><div class="tabletext">${uiLabelMap.ProductProductName}:</div></td>
            <td><input type="text" name="productName" size="30" maxlength="60" class="inputBox"/></td>
        </tr>
        <tr>
            <td width="15%"><div class="tabletext">${uiLabelMap.ProductShortDescription}:</div></td>
            <td><input type="text" name="description" size="60" maxlength="250" class="inputBox"/></td>
        </tr>
        <tr>
            <td width="15%"><div class="tabletext">${uiLabelMap.ProductDefaultPrice}:</div></td>
            <td><input type="text" name="defaultPrice" size="8" class="inputBox"/></td>
        </tr>
        <tr>
            <td width="15%"><div class="tabletext">${uiLabelMap.ProductAverageCost}:</div></td>
            <td><input type="text" name="averageCost" size="8" class="inputBox"/></td>
        </tr>
        <tr>
            <td colspan="3">
                <a href="javascript:document.createProductInCategoryCheckExistingForm.submit()" class="buttontext">${uiLabelMap.ProductCheckExisting}</a>
            </td>
        </tr>
    </table>
</form>
