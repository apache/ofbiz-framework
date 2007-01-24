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
