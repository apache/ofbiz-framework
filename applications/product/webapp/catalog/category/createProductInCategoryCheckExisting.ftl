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
<div class="screenlet">
    <div class="screenlet-title-bar">
        <h3>
            <b>${uiLabelMap.ProductCheckingForExistingProductInCategory} <#if (productCategory.description)?has_content>"${productCategory.description}"</#if> [${uiLabelMap.CommonId}:${productCategoryId!}]</b>
            <#if productFeatureAndTypeDatas?has_content>
            ${uiLabelMap.CommonWhere}
                <#list productFeatureAndTypeDatas as productFeatureAndTypeData>
                    <#assign productFeatureType = productFeatureAndTypeData.productFeatureType>
                    <#assign productFeature = productFeatureAndTypeData.productFeature>
                    ${productFeatureType.description} = ${productFeature.description}
                    <#if productFeatureAndTypeData_has_next>,${uiLabelMap.CommonAnd} </#if>
                </#list>
            </#if>
        </h3>
    </div>
    <div class="screenlet-body">
        <#if products?has_content>
        <table cellspacing="0" class="basic-table">
            <tr>
                <td>${uiLabelMap.ProductInternalName}</td>
                <td>${uiLabelMap.ProductProductName}</td>
                <td width="10%">&nbsp;</td>
            </tr>
            <#list products as product>
            <tr>
                <td>${product.internalName?default("-no internal name-")} [${product.productId}]</td>
                <td>${product.productName?default("-no name-")} [${product.productId}]</td>
                <td width="10%"><a href="<@ofbizUrl>EditProduct?productId=${product.productId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductThisIsIt}]</a></td>
            </tr>
        </#list>
        </table>
        <#else>
            <h3>&nbsp;${uiLabelMap.ProductNoExistingProductsFound}.</h3>
        </#if>
    </div>
    <br />
    <div class="screenlet-body">
        <form name="createProductInCategoryForm" method="post" action="<@ofbizUrl>createProductInCategory</@ofbizUrl>" style="margin: 0;">
            <input type="hidden" name="productCategoryId" value="${productCategoryId}" />
            <table cellspacing="0" class="basic-table">
                <#list productFeatureAndTypeDatas! as productFeatureAndTypeData>
                <#assign productFeatureType = productFeatureAndTypeData.productFeatureType>
                <#assign productFeature = productFeatureAndTypeData.productFeature>
                <#assign productFeatureTypeId = productFeatureType.productFeatureTypeId>
                <input type="hidden" name="pft_${productFeatureType.productFeatureTypeId}" value="${productFeature.productFeatureId}"/>
                <tr>
                    <td width="15%">${productFeatureType.description}</td>
                    <td>
                        <div>
                            ${productFeature.description}
                            <#if requestParameters["pftsel_" + productFeatureTypeId]??>
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
                    <td width="15%">${uiLabelMap.ProductInternalName}:</td>
                    <td>
                        <input type="hidden" name="internalName" value="${requestParameters.internalName!}"/>
                        <div>&nbsp;${requestParameters.internalName?default("&nbsp;")}</div>
                    </td>
                </tr>
                <tr>
                    <td width="15%">${uiLabelMap.ProductProductName}:</td>
                    <td>
                        <input type="hidden" name="productName" value="${requestParameters.productName!}"/>
                        <div>&nbsp;${requestParameters.productName?default("&nbsp;")}</div>
                    </td>
                </tr>
                <tr>
                    <td width="15%">${uiLabelMap.ProductShortDescription}:</td>
                    <td>
                        <input type="hidden" name="description" value="${requestParameters.description!}"/>
                        <div>&nbsp;${requestParameters.description?default("&nbsp;")}</div>
                    </td>
                </tr>
                <tr>
                    <td width="15%">${uiLabelMap.ProductDefaultPrice}:</td>
                    <td>
                        <input type="hidden" name="defaultPrice" value="${requestParameters.defaultPrice!}"/>
                        <input type="hidden" name="currencyUomId" value="${requestParameters.currencyUomId!}"/>
                        <div>&nbsp;${requestParameters.defaultPrice?default("&nbsp;")}&nbsp;${requestParameters.currencyUomId?default("&nbsp;")}</div>
                    </td>
                </tr>
                <tr>
                    <td width="15%">${uiLabelMap.ProductAverageCost}:</td>
                    <td>
                        <input type="hidden" name="averageCost" value="${requestParameters.averageCost!}"/>
                        <div>&nbsp;${requestParameters.averageCost?default("&nbsp;")}</div>
                    </td>
                </tr>
                <tr>
                    <td colspan="3">
                        <div>
                            ${uiLabelMap.ProductNewProductId}: <input type="text" name="productId" value=""/>
                            <input type="submit" value="${uiLabelMap.ProductCreateNewProduct}" class="smallSubmit"/>
                        </div>
                    </td>
                </tr>
            </table>
        </form>
    </div>
</div>