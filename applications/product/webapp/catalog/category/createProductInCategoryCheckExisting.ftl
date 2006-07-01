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
    <a href="<@ofbizUrl>EditCategory?productCategoryId=${productCategoryId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductBackToEditCategory}]</a>
</#if>

<div class="head1">
    ${uiLabelMap.ProductCheckingForExistingProductInCategory} <#if (productCategory.description)?has_content>"${productCategory.description}"</#if> [${uiLabelMap.CommonId}:${productCategoryId?if_exists}]

    <#if productFeatureAndTypeDatas?has_content>
       ${uiLabelMap.CommonWhere }
        <#list productFeatureAndTypeDatas as productFeatureAndTypeData>
            <#assign productFeatureType = productFeatureAndTypeData.productFeatureType>
            <#assign productFeature = productFeatureAndTypeData.productFeature>
            ${productFeatureType.description} = ${productFeature.description}
            <#if productFeatureAndTypeData_has_next>,${uiLabelMap.CommonAnd} </#if>
        </#list>
    </#if>
</div>

<#if products?has_content>
    <table cellpadding="1" cellspacing="0" border="1">
        <tr>
            <td><div class="tableheadtext">${uiLabelMap.ProductInternalName}</div></td>
            <td><div class="tableheadtext">${uiLabelMap.ProductProductName}</div></td>
            <td width="10%">&nbsp;</td>
        </tr>
    <#list products as product>
        <tr>
            <td><div class="tabletext">${product.internalName?default("-no internal name-")} [${product.productId}]</div></td>
            <td><div class="tabletext">${product.productName?default("-no name-")} [${product.productId}]</div></td>
            <td width="10%"><a href="<@ofbizUrl>EditProduct?productId=${product.productId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductThisIsIt}]</a></td>
        </tr>
    </#list>
    </table>
<#else>
    <div class="head3">&nbsp;${uiLabelMap.ProductNoExistingProductsFound}.</div>
</#if>

<br/>

<form name="createProductInCategoryForm" method="post" action="<@ofbizUrl>createProductInCategory</@ofbizUrl>" style="margin: 0;">
    <input type="hidden" name="productCategoryId" value="${productCategoryId}">
    <table cellpadding="1" cellspacing="0" border="1">
        <#list productFeatureAndTypeDatas?if_exists as productFeatureAndTypeData>
            <#assign productFeatureType = productFeatureAndTypeData.productFeatureType>
            <#assign productFeature = productFeatureAndTypeData.productFeature>
            <#assign productFeatureTypeId = productFeatureType.productFeatureTypeId>
            <input type="hidden" name="pft_${productFeatureType.productFeatureTypeId}" value="${productFeature.productFeatureId}"/>
            <tr>
                <td width="15%">
                    <div class="tabletext">${productFeatureType.description}</div>
                </td>
                <td>
                    <div class="tabletext">
                        ${productFeature.description}
                        <#if requestParameters["pftsel_" + productFeatureTypeId]?exists>
                            <input type="hidden" name="pftsel_${productFeatureTypeId}" value="Y"/>
                            [${uiLabelMap.ProductSelectable}]
                        <#else>
                            <input type="hidden" name="pftsel_${productFeatureTypeId}" value="N"/>
                            [${uiLabelMap.ProductStandard}]
                        </#if>
                    </div>
                </td>
            </tr>
        </#list>
        <tr>
            <td width="15%"><div class="tabletext">${uiLabelMap.ProductInternalName}:</div></td>
            <td>
                <input type="hidden" name="internalName" value="${requestParameters.internalName?if_exists}"/>
                <div class="tabletext">&nbsp;${requestParameters.internalName?default("&nbsp;")}</div>
            </td>
        </tr>
        <tr>
            <td width="15%"><div class="tabletext">${uiLabelMap.ProductProductName}:</div></td>
            <td>
                <input type="hidden" name="productName" value="${requestParameters.productName?if_exists}"/>
                <div class="tabletext">&nbsp;${requestParameters.productName?default("&nbsp;")}</div>
            </td>
        </tr>
        <tr>
            <td width="15%"><div class="tabletext">${uiLabelMap.ProductShortDescription}:</div></td>
            <td>
                <input type="hidden" name="description" value="${requestParameters.description?if_exists}"/>
                <div class="tabletext">&nbsp;${requestParameters.description?default("&nbsp;")}</div>
            </td>
        </tr>
        <tr>
            <td width="15%"><div class="tabletext">${uiLabelMap.ProductDefaultPrice}:</div></td>
            <td>
                <input type="hidden" name="defaultPrice" value="${requestParameters.defaultPrice?if_exists}"/>
                <div class="tabletext">&nbsp;${requestParameters.defaultPrice?default("&nbsp;")}</div>
            </td>
        </tr>
        <tr>
            <td width="15%"><div class="tabletext">${uiLabelMap.ProductAverageCost}:</div></td>
            <td>
                <input type="hidden" name="averageCost" value="${requestParameters.averageCost?if_exists}"/>
                <div class="tabletext">&nbsp;${requestParameters.averageCost?default("&nbsp;")}</div>
            </td>
        </tr>
        <tr>
            <td colspan="3">
                <div class="tabletext">
                    ${uiLabelMap.ProductNewProductId}: <input type="text" name="productId" value="" class="inputBox"/>
                    <input type="submit" value="${uiLabelMap.ProductCreateNewProduct}" class="smallSubmit"/>
                </div>
            </td>
        </tr>
    </table>
</form>
