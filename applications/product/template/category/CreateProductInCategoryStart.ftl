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

<#assign productFeaturesByTypeMap = Static["org.apache.ofbiz.product.feature.ParametricSearch"].makeCategoryFeatureLists(productCategoryId, delegator)>

<#if productCategoryId?has_content>
    <a href="<@ofbizUrl>EditCategory?productCategoryId=${productCategoryId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductBackToEditCategory}]</a>
</#if>
<div class="screenlet">
    <div class="screenlet-title-bar">
        <h3><b>&nbsp;</b></h3>
    </div>
    <div class="screenlet-body">
        <form name="createProductInCategoryCheckExistingForm" method="post" action="<@ofbizUrl>CreateProductInCategoryCheckExisting</@ofbizUrl>" style="margin: 0;">
            <input type="hidden" name="productCategoryId" value="${productCategoryId}" />
            <table cellspacing="0" class="basic-table">
            <#list productFeaturesByTypeMap.keySet() as productFeatureTypeId>
                <#assign findPftMap = Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("productFeatureTypeId", productFeatureTypeId)>
                <#assign productFeatureType = delegator.findOne("ProductFeatureType", findPftMap, true)>
                <#assign productFeatures = productFeaturesByTypeMap[productFeatureTypeId]>
                <tr>
                    <td width="15%">${productFeatureType.description}:</td>
                    <td>
                        <div>
                            <select name="pft_${productFeatureTypeId}">
                                <option value="">- ${uiLabelMap.CommonNone} -</option>
                                <#list productFeatures as productFeature>
                                    <option value="${productFeature.productFeatureId}">${productFeature.description}</option>
                                </#list>
                            </select>
                            <label>
                              <input type="checkbox" name="pftsel_${productFeatureTypeId}"/>${uiLabelMap.ProductSelectable}
                            </label>
                        </div>
                    </td>
                </tr>
                </#list>
                <tr>
                    <td width="15%">${uiLabelMap.ProductInternalName}:</td>
                    <td><input type="text" name="internalName" size="30" maxlength="60"/></td>
                </tr>
                <tr>
                    <td width="15%">${uiLabelMap.ProductProductName}:</td>
                    <td><input type="text" name="productName" size="30" maxlength="60"/></td>
                </tr>
                <tr>
                    <td width="15%">${uiLabelMap.ProductShortDescription}:</td>
                    <td><input type="text" name="description" size="60" maxlength="250"/></td>
                </tr>
                <tr>
                    <td width="15%">${uiLabelMap.ProductDefaultPrice}:</td>
                    <td><input type="text" name="defaultPrice" size="8"/>
                    <#assign findCurrenciesMap = Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("uomTypeId", "CURRENCY_MEASURE")>
                    <#assign currencies = delegator.findByAnd('Uom', findCurrenciesMap, null, true) />
                    <#if currencies?has_content && (currencies?size > 0)>
                        <select name="currencyUomId">
                            <option value=""></option>
                            <#list currencies as currency>
                                <option value="${currency.uomId}">${currency.get("description",locale)} [${currency.uomId}]</option>
                            </#list>
                        </select>
                    </#if>
                    </td>
                </tr>
                <tr>
                    <td width="15%">${uiLabelMap.ProductAverageCost}:</td>
                    <td><input type="text" name="averageCost" size="8"/></td>
                </tr>
                <tr>
                    <td colspan="3">
                        <a href="javascript:document.createProductInCategoryCheckExistingForm.submit()" class="buttontext">${uiLabelMap.ProductCheckExisting}</a>
                    </td>
                </tr>
            </table>
        </form>
    </div>
</div>
