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
<#if productPromoId?exists && productPromo?exists>
    <div class="screenlet">
        <div class="screenlet-title-bar">
            <h3>${uiLabelMap.PageTitleEditProductPromoRules}</h3>
        </div>
        <#-- ======================= Rules ======================== -->
        <div class="screenlet-body">
            <table cellspacing="0" class="basic-table">
                <tr class="header-row">
                    <td width="10%"><b>${uiLabelMap.ProductRuleId}</b></td>
                    <td width="80%"><b>${uiLabelMap.ProductRuleName}</b></td>
                    <td width="10%"><b>&nbsp;</b></td>
                </tr>
                <#assign rowClass = "2">
                <#list productPromoRules as productPromoRule>
                <#assign productPromoConds = productPromoRule.getRelated("ProductPromoCond")>
                <#assign productPromoActions = productPromoRule.getRelated("ProductPromoAction")>
                <tr valign="middle"<#if rowClass == "1"> class="alternate-row"</#if>>
                    <td class="label"><b>${(productPromoRule.productPromoRuleId)?if_exists}</b></td>
                    <td>
                        <form method="post" action="<@ofbizUrl>updateProductPromoRule</@ofbizUrl>">
                            <input type="hidden" name="productPromoId" value="${(productPromoRule.productPromoId)?if_exists}">
                            <input type="hidden" name="productPromoRuleId" value="${(productPromoRule.productPromoRuleId)?if_exists}">
                            <input type="text" size="30" name="ruleName" value="${(productPromoRule.ruleName)?if_exists}">
                            <input type="submit" value="${uiLabelMap.CommonUpdate}">
                        </form>
                    </td>
                    <td align="center">&nbsp;
                        <#if (productPromoConds.size() == 0 && productPromoActions.size() == 0)>
                            <a href="<@ofbizUrl>deleteProductPromoRule?productPromoId=${(productPromoRule.productPromoId)?if_exists}&productPromoRuleId=${(productPromoRule.productPromoRuleId)?if_exists}</@ofbizUrl>" class="buttontext">
                            ${uiLabelMap.CommonDelete}</a>
                        </#if>
                    </td>
                </tr>
                <tr valign="top">
                    <td align="right" class="label">${uiLabelMap.ProductConditions}</td>
                    <td colspan="2">
                        <table cellspacing="0" class="basic-table">
                        <#assign maxCondSeqId = 1>
                        <#list productPromoConds as productPromoCond>
                            <tr>
                                <!-- if cur seq id is a number and is greater than max, set new max for input box prefill below -->
                                <#if (productPromoCond.productPromoCondSeqId)?exists>
                                    <#assign curCondSeqId = Static["java.lang.Integer"].valueOf(productPromoCond.getString("productPromoCondSeqId"))>
                                    <#if (curCondSeqId >= maxCondSeqId)>
                                        <#assign maxCondSeqId = curCondSeqId + 1>
                                    </#if>
                                </#if>
                                <td class="label"><b>${(productPromoCond.productPromoCondSeqId)?if_exists}</b></td>
                                <td>
                                    <form method="post" action="<@ofbizUrl>updateProductPromoCond</@ofbizUrl>">
                                        <input type="hidden" name="productPromoId" value="${(productPromoCond.productPromoId)?if_exists}"/>
                                        <input type="hidden" name="productPromoRuleId" value="${(productPromoCond.productPromoRuleId)?if_exists}"/>
                                        <input type="hidden" name="productPromoCondSeqId" value="${(productPromoCond.productPromoCondSeqId)?if_exists}"/>
                                        <select name="inputParamEnumId" size="1">
                                            <#if (productPromoCond.inputParamEnumId)?exists>
                                                <#assign inputParamEnum = productPromoCond.getRelatedOneCache("InputParamEnumeration")>
                                                <option value="${productPromoCond.inputParamEnumId}"><#if inputParamEnum?exists>${(inputParamEnum.get("description",locale))?if_exists}<#else>[${(productPromoCond.inputParamEnumId)?if_exists}]</#if></option>
                                                <option value="${(productPromoCond.inputParamEnumId)?if_exists}">&nbsp;</option>
                                            <#else>
                                                <option value="">&nbsp;</option>
                                            </#if>
                                            <#list inputParamEnums as inputParamEnum>
                                                <option value="${(inputParamEnum.enumId)?if_exists}">${(inputParamEnum.get("description",locale))?if_exists}</option>
                                            </#list>
                                        </select>
                                        <select name="operatorEnumId" size="1">
                                            <#if (productPromoCond.operatorEnumId)?exists>
                                                <#assign operatorEnum = productPromoCond.getRelatedOneCache("OperatorEnumeration")>
                                                <option value="${(productPromoCond.operatorEnumId)?if_exists}"><#if operatorEnum?exists>${(operatorEnum.get("description",locale))?if_exists}<#else>[${(productPromoCond.operatorEnumId)?if_exists}]</#if></option>
                                                <option value="${(productPromoCond.operatorEnumId)?if_exists}">&nbsp;</option>
                                            <#else>
                                                <option value="">&nbsp;</option>
                                            </#if>
                                            <#list condOperEnums as condOperEnum>
                                            <option value="${(condOperEnum.enumId)?if_exists}">${(condOperEnum.get("description",locale))?if_exists}</option>
                                            </#list>
                                        </select>
                                        <input type="text" size="25" name="condValue" value="${(productPromoCond.condValue)?if_exists}">
                                        ${uiLabelMap.CommonOther}:<input type="text" size="10" name="otherValue" value="${(productPromoCond.otherValue)?if_exists}">
                                        <input type="submit" value="${uiLabelMap.CommonUpdate}">
                                    </form>
                                    <#-- ======================= Categories ======================== -->
                                    <div class="label">${uiLabelMap.ProductConditions} ${uiLabelMap.ProductCategories}:</div>
                                    <#assign condProductPromoCategories = productPromoCond.getRelated("ProductPromoCategory")>
                                    <#list condProductPromoCategories as condProductPromoCategory>
                                        <#assign condProductCategory = condProductPromoCategory.getRelatedOneCache("ProductCategory")>
                                        <#assign condApplEnumeration = condProductPromoCategory.getRelatedOneCache("ApplEnumeration")>
                                        <div>
                                            ${(condProductCategory.get("description",locale))?if_exists} [${condProductPromoCategory.productCategoryId}]
                                            - ${(condApplEnumeration.get("description",locale))?default(condProductPromoCategory.productPromoApplEnumId)}
                                            - ${uiLabelMap.ProductSubCats}? ${condProductPromoCategory.includeSubCategories?default("N")}
                                            - ${uiLabelMap.CommonAnd} ${uiLabelMap.CommonGroup}: ${condProductPromoCategory.andGroupId}
                                            <a href="<@ofbizUrl>deleteProductPromoCategory?productPromoId=${(condProductPromoCategory.productPromoId)?if_exists}&productPromoRuleId=${(condProductPromoCategory.productPromoRuleId)?if_exists}&productPromoActionSeqId=${(condProductPromoCategory.productPromoActionSeqId)?if_exists}&productPromoCondSeqId=${(condProductPromoCategory.productPromoCondSeqId)?if_exists}&productCategoryId=${(condProductPromoCategory.productCategoryId)?if_exists}&andGroupId=${(condProductPromoCategory.andGroupId)?if_exists}</@ofbizUrl>" class="buttontext">
                                            ${uiLabelMap.CommonDelete}</a>
                                        </div>
                                    </#list>
                                    <div>
                                        <form method="post" action="<@ofbizUrl>createProductPromoCategory</@ofbizUrl>" name="createProductPromoCategoryConditions">
                                            <input type="hidden" name="productPromoId" value="${productPromoId}">
                                            <input type="hidden" name="productPromoRuleId" value="${productPromoCond.productPromoRuleId}">
                                            <input type="hidden" name="productPromoActionSeqId" value="_NA_">
                                            <input type="hidden" name="productPromoCondSeqId" value="${productPromoCond.productPromoCondSeqId}">
                                            <input type="text" name="productCategoryId" size="20" maxlength="20"/>
                                            <a href="javascript:call_fieldlookup2(document.createProductPromoCategoryConditions.productCategoryId,'LookupProductCategory');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt="${uiLabelMap.CommonClickHereForFieldLookup}"/></a>
                                            <select name="productPromoApplEnumId">
                                                <#list productPromoApplEnums as productPromoApplEnum>
                                                    <option value="${productPromoApplEnum.enumId}">${productPromoApplEnum.get("description",locale)}</option>
                                                </#list>
                                            </select>
                                            <select name="includeSubCategories">
                                                <option value="N">${uiLabelMap.CommonN}</option>
                                                <option value="Y">${uiLabelMap.CommonY}</option>
                                            </select>
                                            ${uiLabelMap.CommonAnd} ${uiLabelMap.CommonGroup}: <input type="text" size="10" maxlength="20" name="andGroupId" value="_NA_"/>*
                                            <input type="submit" value="${uiLabelMap.CommonAdd}">
                                        </form>
                                    </div>
                                    <#-- ======================= Products ======================== -->
                                    <div class="label">${uiLabelMap.ProductConditions} ${uiLabelMap.ProductProducts}:</div>
                                    <#assign condProductPromoProducts = productPromoCond.getRelated("ProductPromoProduct")>
                                    <#list condProductPromoProducts as condProductPromoProduct>
                                        <#assign condProduct = condProductPromoProduct.getRelatedOneCache("Product")?if_exists>
                                        <#assign condApplEnumeration = condProductPromoProduct.getRelatedOneCache("ApplEnumeration")>
                                        <div>
                                            ${(condProduct.internalName)?if_exists} [${condProductPromoProduct.productId}]
                                            - ${(condApplEnumeration.get("description",locale))?default(condProductPromoProduct.productPromoApplEnumId)}
                                            <a href="<@ofbizUrl>deleteProductPromoProduct?productPromoId=${(condProductPromoProduct.productPromoId)?if_exists}&productPromoRuleId=${(condProductPromoProduct.productPromoRuleId)?if_exists}&productPromoActionSeqId=${(condProductPromoProduct.productPromoActionSeqId)?if_exists}&productPromoCondSeqId=${(condProductPromoProduct.productPromoCondSeqId)?if_exists}&productId=${(condProductPromoProduct.productId)?if_exists}</@ofbizUrl>" class="buttontext">
                                            ${uiLabelMap.CommonDelete}</a>
                                        </div>
                                    </#list>
                                    <div>
                                        <form method="post" action="<@ofbizUrl>createProductPromoProduct</@ofbizUrl>">
                                            <input type="hidden" name="productPromoId" value="${productPromoId}">
                                            <input type="hidden" name="productPromoRuleId" value="${productPromoCond.productPromoRuleId}">
                                            <input type="hidden" name="productPromoActionSeqId" value="_NA_">
                                            <input type="hidden" name="productPromoCondSeqId" value="${productPromoCond.productPromoCondSeqId}">
                                            ${uiLabelMap.ProductProductId}: <input type="text" size="20" maxlength="20" name="productId" value=""/>
                                            <select name="productPromoApplEnumId">
                                                <#list productPromoApplEnums as productPromoApplEnum>
                                                    <option value="${productPromoApplEnum.enumId}">${productPromoApplEnum.get("description",locale)}</option>
                                                </#list>
                                            </select>
                                            <input type="submit" value="${uiLabelMap.CommonAdd}">
                                        </form>
                                    </div>
                                </td>
                                <td align="center">
                                    <a href="<@ofbizUrl>deleteProductPromoCond?productPromoId=${(productPromoCond.productPromoId)?if_exists}&productPromoRuleId=${(productPromoCond.productPromoRuleId)?if_exists}&productPromoCondSeqId=${(productPromoCond.productPromoCondSeqId)?if_exists}</@ofbizUrl>" class="buttontext">
                                    ${uiLabelMap.CommonDelete}</a>
                                </td>
                            </tr>
                        </#list>
                            <tr>
                                <td colspan="3">
                                    <form method="post" action="<@ofbizUrl>createProductPromoCond</@ofbizUrl>">
                                        <input type="hidden" name="productPromoId" value="${(productPromoRule.productPromoId)?if_exists}">
                                        <input type="hidden" name="productPromoRuleId" value="${(productPromoRule.productPromoRuleId)?if_exists}">
                                        <span class="label"><b>${uiLabelMap.CommonNew}</b>&nbsp;</span>
                                        <select name="inputParamEnumId" size="1">
                                            <#list inputParamEnums as inputParamEnum>
                                                <option value="${(inputParamEnum.enumId)?if_exists}">${(inputParamEnum.get("description",locale))?if_exists}</option>
                                            </#list>
                                        </select>
                                        <select name="operatorEnumId" size="1">
                                            <#list condOperEnums as condOperEnum>
                                            <option value="${(condOperEnum.enumId)?if_exists}">${(condOperEnum.get("description",locale))?if_exists}</option>
                                            </#list>
                                        </select>
                                        <input type="text" size="25" name="condValue">
                                        ${uiLabelMap.CommonOther}:<input type="text" size="10" name="otherValue">
                                        <input type="submit" value="${uiLabelMap.CommonCreate}">
                                    </form>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
                <tr valign="top">
                    <td align="right" class="label">${uiLabelMap.ProductActions} :</td>
                    <td colspan="2">
                        <table cellspacing="0" class="basic-table">
                        <#list productPromoActions as productPromoAction>
                            <tr>
                                <td class="label"><b>${(productPromoAction.productPromoActionSeqId)?if_exists}</b></td>
                                <td>
                                    <div>
                                    <form method="post" action="<@ofbizUrl>updateProductPromoAction</@ofbizUrl>">
                                        <input type="hidden" name="productPromoId" value="${(productPromoAction.productPromoId)?if_exists}">
                                        <input type="hidden" name="productPromoRuleId" value="${(productPromoAction.productPromoRuleId)?if_exists}">
                                        <input type="hidden" name="productPromoActionSeqId" value="${(productPromoAction.productPromoActionSeqId)?if_exists}">
                                        <select name="productPromoActionEnumId" size="1">
                                            <#if (productPromoAction.productPromoActionEnumId)?exists>
                                                <#assign productPromoActionCurEnum = productPromoAction.getRelatedOneCache("ActionEnumeration")>
                                                <option value="${(productPromoAction.productPromoActionEnumId)?if_exists}"><#if productPromoActionCurEnum?exists>${(productPromoActionCurEnum.get("description",locale))?if_exists}<#else>[${(productPromoAction.productPromoActionEnumId)?if_exists}]</#if></option>
                                                <option value="${(productPromoAction.productPromoActionEnumId)?if_exists}">&nbsp;</option>
                                            <#else>
                                                <option value="">&nbsp;</option>
                                            </#if>
                                            <#list productPromoActionEnums as productPromoActionEnum>
                                                <option value="${(productPromoActionEnum.enumId)?if_exists}">${(productPromoActionEnum.get("description",locale))?if_exists}</option>
                                            </#list>
                                        </select>
                                        <input type="hidden" name="orderAdjustmentTypeId" value="${(productPromoAction.orderAdjustmentTypeId)?if_exists}">
                                        ${uiLabelMap.ProductQuantity}:&nbsp;<input type="text" size="5" name="quantity" value="${(productPromoAction.quantity)?if_exists}">
                                        ${uiLabelMap.ProductAmount}:&nbsp;<input type="text" size="5" name="amount" value="${(productPromoAction.amount)?if_exists}">
                                        ${uiLabelMap.ProductItemId}:&nbsp;<input type="text" size="15" name="productId" value="${(productPromoAction.productId)?if_exists}">
                                        ${uiLabelMap.PartyParty}:&nbsp;<input type="text" size="10" name="partyId" value="${(productPromoAction.partyId)?if_exists}">
                                        <input type="submit" value="${uiLabelMap.CommonUpdate}">
                                    </form>
                                    </div>
                                    <#-- ======================= Categories ======================== -->
                                    <div class="label">${uiLabelMap.ProductActions} ${uiLabelMap.ProductCategories}:</div>
                                    <#assign actionProductPromoCategories = productPromoAction.getRelated("ProductPromoCategory")>
                                    <#list actionProductPromoCategories as actionProductPromoCategory>
                                        <#assign actionProductCategory = actionProductPromoCategory.getRelatedOneCache("ProductCategory")>
                                        <#assign actionApplEnumeration = actionProductPromoCategory.getRelatedOneCache("ApplEnumeration")>
                                        <div>
                                            ${(actionProductCategory.description)?if_exists} [${actionProductPromoCategory.productCategoryId}]
                                            - ${(actionApplEnumeration.get("description",locale))?default(actionProductPromoCategory.productPromoApplEnumId)}
                                            - ${uiLabelMap.ProductSubCats}? ${actionProductPromoCategory.includeSubCategories?default("N")}
                                            - ${uiLabelMap.CommonAnd} ${uiLabelMap.CommonGroup}: ${actionProductPromoCategory.andGroupId}
                                            <a href="<@ofbizUrl>deleteProductPromoCategory?productPromoId=${(actionProductPromoCategory.productPromoId)?if_exists}&productPromoRuleId=${(actionProductPromoCategory.productPromoRuleId)?if_exists}&productPromoCondSeqId=${(actionProductPromoCategory.productPromoCondSeqId)?if_exists}&productPromoActionSeqId=${(actionProductPromoCategory.productPromoActionSeqId)?if_exists}&productCategoryId=${(actionProductPromoCategory.productCategoryId)?if_exists}&andGroupId=${(actionProductPromoCategory.andGroupId)?if_exists}</@ofbizUrl>" class="buttontext">
                                            ${uiLabelMap.CommonDelete}</a>
                                        </div>
                                    </#list>
                                    <div>
                                        <form method="post" action="<@ofbizUrl>createProductPromoCategory</@ofbizUrl>" name="createProductPromoCategoryActions">
                                            <input type="hidden" name="productPromoId" value="${productPromoId}">
                                            <input type="hidden" name="productPromoRuleId" value="${productPromoAction.productPromoRuleId}">
                                            <input type="hidden" name="productPromoActionSeqId" value="${productPromoAction.productPromoActionSeqId}">
                                            <input type="hidden" name="productPromoCondSeqId" value="_NA_">
                                            <input type="text" name="productCategoryId" size="20" maxlength="20"/>
                                            <a href="javascript:call_fieldlookup2(document.createProductPromoCategoryActions.productCategoryId,'LookupProductCategory');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt="${uiLabelMap.CommonClickHereForFieldLookup}"/></a>
                                            <select name="productPromoApplEnumId">
                                                <#list productPromoApplEnums as productPromoApplEnum>
                                                    <option value="${productPromoApplEnum.enumId}">${productPromoApplEnum.get("description",locale)}</option>
                                                </#list>
                                            </select>
                                            <select name="includeSubCategories">
                                                <option value="N">${uiLabelMap.CommonN}</option>
                                                <option value="Y">${uiLabelMap.CommonY}</option>
                                            </select>
                                            ${uiLabelMap.CommonAnd} ${uiLabelMap.CommonGroup}: <input type="text" size="10" maxlength="20" name="andGroupId" value="_NA_"/>*
                                            <input type="submit" value="${uiLabelMap.CommonAdd}">
                                        </form>
                                    </div>
                                    <#-- ======================= Products ======================== -->
                                    <div class="label">${uiLabelMap.ProductActions} ${uiLabelMap.ProductProducts}:</div>
                                    <#assign actionProductPromoProducts = productPromoAction.getRelated("ProductPromoProduct")>
                                    <#list actionProductPromoProducts as actionProductPromoProduct>
                                        <#assign actionProduct = actionProductPromoProduct.getRelatedOneCache("Product")?if_exists>
                                        <#assign actionApplEnumeration = actionProductPromoProduct.getRelatedOneCache("ApplEnumeration")>
                                        <div>
                                            ${(actionProduct.internalName)?if_exists} [${actionProductPromoProduct.productId}]
                                            - ${(actionApplEnumeration.get("description",locale))?default(actionProductPromoProduct.productPromoApplEnumId)}
                                            <a href="<@ofbizUrl>deleteProductPromoProduct?productPromoId=${(actionProductPromoProduct.productPromoId)?if_exists}&productPromoRuleId=${(actionProductPromoProduct.productPromoRuleId)?if_exists}&productPromoCondSeqId=${(actionProductPromoProduct.productPromoCondSeqId)?if_exists}&productPromoActionSeqId=${(actionProductPromoProduct.productPromoActionSeqId)?if_exists}&productId=${(actionProductPromoProduct.productId)?if_exists}</@ofbizUrl>" class="buttontext">
                                            ${uiLabelMap.CommonDelete}</a>
                                        </div>
                                    </#list>
                                    <div>
                                        <form method="post" action="<@ofbizUrl>createProductPromoProduct</@ofbizUrl>">
                                            <input type="hidden" name="productPromoId" value="${productPromoId}">
                                            <input type="hidden" name="productPromoRuleId" value="${productPromoAction.productPromoRuleId}">
                                            <input type="hidden" name="productPromoActionSeqId" value="${productPromoAction.productPromoActionSeqId}">
                                            <input type="hidden" name="productPromoCondSeqId" value="_NA_">
                                            ${uiLabelMap.ProductProductId}: <input type="text" size="20" maxlength="20" name="productId" value=""/>
                                            <select name="productPromoApplEnumId">
                                                <#list productPromoApplEnums as productPromoApplEnum>
                                                    <option value="${productPromoApplEnum.enumId}">${productPromoApplEnum.get("description",locale)}</option>
                                                </#list>
                                            </select>
                                            <input type="submit" value="${uiLabelMap.CommonAdd}">
                                        </form>
                                    </div>
                                </td>
                                <td align="center">
                                    <a href="<@ofbizUrl>deleteProductPromoAction?productPromoId=${(productPromoAction.productPromoId)?if_exists}&productPromoRuleId=${(productPromoAction.productPromoRuleId)?if_exists}&productPromoActionSeqId=${(productPromoAction.productPromoActionSeqId)?if_exists}</@ofbizUrl>" class="buttontext">
                                    ${uiLabelMap.CommonDelete}</a>
                                </td>
                            </tr>
                        </#list>
                            <tr>
                                <td colspan="3">
                                    <div>
                                    <form method="post" action="<@ofbizUrl>createProductPromoAction</@ofbizUrl>">
                                        <input type="hidden" name="productPromoId" value="${(productPromoRule.productPromoId)?if_exists}">
                                        <input type="hidden" name="productPromoRuleId" value="${(productPromoRule.productPromoRuleId)?if_exists}">
                                        <span class="label"><b>${uiLabelMap.CommonNew}:</b>&nbsp;</span>
                                        <select name="productPromoActionEnumId" size="1">
                                            <#list productPromoActionEnums as productPromoActionEnum>
                                            <option value="${(productPromoActionEnum.enumId)?if_exists}">${(productPromoActionEnum.get("description",locale))?if_exists}</option>
                                            </#list>
                                        </select>
                                        <input type="hidden" name="orderAdjustmentTypeId" value="PROMOTION_ADJUSTMENT">
                                        ${uiLabelMap.ProductQuantity}:&nbsp;<input type="text" size="5" name="quantity">
                                        ${uiLabelMap.ProductAmount}:&nbsp;<input type="text" size="5" name="amount">
                                        ${uiLabelMap.ProductItemId}:&nbsp;<input type="text" size="15" name="productId">
                                        ${uiLabelMap.PartyParty}:&nbsp;<input type="text" size="10" name="partyId">
                                        <input type="submit" value="${uiLabelMap.CommonCreate}">
                                    </form>
                                    </div>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
                </#list>
            </table>
        </div>
        <#-- This was removed in r697962, should have been only commented out as maybe in future will be used again (free shipping promo)
        <div class="tooltip"><b>${uiLabelMap.ProductNoteOnItemId} :</b> ${uiLabelMap.ProductItemIdGiftPurchaseFreeShipping}</div>
        <div class="tooltip"><b>${uiLabelMap.ProductNoteOnPartyId} :</b> ${uiLabelMap.ProductPartyFreeShipping}</div>
        -->
    </div>
    <div class="screenlet">
        <div class="screenlet-title-bar">
            <h3>${uiLabelMap.ProductAddPromoRule}</h3>
        </div>
        <div class="screenlet-body">
            <form method="post" action="<@ofbizUrl>createProductPromoRule</@ofbizUrl>">
                <input type="hidden" name="productPromoId" value="${productPromoId?if_exists}">
                <span class="label">${uiLabelMap.ProductName}</span><input type="text" size="30" name="ruleName">
                <input type="submit" value="${uiLabelMap.CommonAdd}">
            </form>
        </div>
    </div>
    <div class="screenlet">
        <div class="screenlet-title-bar">
            <h3>${uiLabelMap.ProductPromotion} ${uiLabelMap.ProductCategories}</h3>
        </div>
        <#-- ======================= Categories ======================== -->
        <div class="screenlet-body">
            <#list promoProductPromoCategories as promoProductPromoCategory>
            <#assign promoProductCategory = promoProductPromoCategory.getRelatedOneCache("ProductCategory")>
            <#assign promoApplEnumeration = promoProductPromoCategory.getRelatedOneCache("ApplEnumeration")>
            <div>
                ${(promoProductCategory.description)?if_exists} [${promoProductPromoCategory.productCategoryId}]
                - ${(promoApplEnumeration.get("description",locale))?default(promoProductPromoCategory.productPromoApplEnumId)}
                - ${uiLabelMap.ProductSubCats}? ${promoProductPromoCategory.includeSubCategories?default("N")}
                - ${uiLabelMap.CommonAnd} ${uiLabelMap.CommonGroup}: ${promoProductPromoCategory.andGroupId}
                <a href="<@ofbizUrl>deleteProductPromoCategory?productPromoId=${(promoProductPromoCategory.productPromoId)?if_exists}&productPromoRuleId=${(promoProductPromoCategory.productPromoRuleId)?if_exists}&productPromoActionSeqId=${(promoProductPromoCategory.productPromoActionSeqId)?if_exists}&productPromoCondSeqId=${(promoProductPromoCategory.productPromoCondSeqId)?if_exists}&productCategoryId=${(promoProductPromoCategory.productCategoryId)?if_exists}&andGroupId=${(promoProductPromoCategory.andGroupId)?if_exists}</@ofbizUrl>" class="buttontext">
                ${uiLabelMap.CommonDelete}</a>
            </div>
            </#list>
            <div>
                <form method="post" action="<@ofbizUrl>createProductPromoCategory</@ofbizUrl>" name="createProductPromoCategoryPromotions">
                    <input type="hidden" name="productPromoId" value="${productPromoId}">
                    <input type="hidden" name="productPromoRuleId" value="_NA_">
                    <input type="hidden" name="productPromoActionSeqId" value="_NA_">
                    <input type="hidden" name="productPromoCondSeqId" value="_NA_">
                    <input type="text" name="productCategoryId" size="20" maxlength="20"/>
                    <a href="javascript:call_fieldlookup2(document.createProductPromoCategoryPromotions.productCategoryId,'LookupProductCategory');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt="${uiLabelMap.CommonClickHereForFieldLookup}"/></a>
                    <select name="productPromoApplEnumId">
                        <#list productPromoApplEnums as productPromoApplEnum>
                            <option value="${productPromoApplEnum.enumId}">${productPromoApplEnum.get("description",locale)}</option>
                        </#list>
                    </select>
                    <select name="includeSubCategories">
                        <option value="N">${uiLabelMap.CommonN}</option>
                        <option value="Y">${uiLabelMap.CommonY}</option>
                    </select>
                    ${uiLabelMap.CommonAnd} ${uiLabelMap.CommonGroup}: <input type="text" size="10" maxlength="20" name="andGroupId" value="_NA_"/>*
                    <input type="submit" value="${uiLabelMap.CommonAdd}">
                </form>
            </div>
        </div>
    </div>
    <div class="screenlet">
        <div class="screenlet-title-bar">
            <h3>${uiLabelMap.ProductPromotionProducts}</h3>
        </div>
        <#-- ======================= Products ======================== -->
        <div class="screenlet-body">
            <#list promoProductPromoProducts as promoProductPromoProduct>
                <#assign promoProduct = promoProductPromoProduct.getRelatedOneCache("Product")?if_exists>
                <#assign promoApplEnumeration = promoProductPromoProduct.getRelatedOneCache("ApplEnumeration")>
                <div>
                    ${(promoProduct.internalName)?if_exists} [${promoProductPromoProduct.productId}]
                    - ${(promoApplEnumeration.get("description",locale))?default(promoProductPromoProduct.productPromoApplEnumId)}
                    <a href="<@ofbizUrl>deleteProductPromoProduct?productPromoId=${(promoProductPromoProduct.productPromoId)?if_exists}&productPromoRuleId=${(promoProductPromoProduct.productPromoRuleId)?if_exists}&productPromoActionSeqId=${(promoProductPromoProduct.productPromoActionSeqId)?if_exists}&productPromoCondSeqId=${(promoProductPromoProduct.productPromoCondSeqId)?if_exists}&productId=${(promoProductPromoProduct.productId)?if_exists}</@ofbizUrl>" class="buttontext">
                    ${uiLabelMap.CommonDelete}</a>
                </div>
            </#list>
            <div>
                <form method="post" name="createpromoproductform" action="<@ofbizUrl>createProductPromoProduct</@ofbizUrl>">
                    <input type="hidden" name="productPromoId" value="${productPromoId}">
                    <input type="hidden" name="productPromoRuleId" value="_NA_">
                    <input type="hidden" name="productPromoActionSeqId" value="_NA_">
                    <input type="hidden" name="productPromoCondSeqId" value="_NA_">
                    <span class="label">${uiLabelMap.ProductProductId}</span><input type="text" size="20" maxlength="20" name="productId" value=""/>*<a href="javascript:call_fieldlookup2(document.createpromoproductform.productId,'LookupProduct');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt="${uiLabelMap.CommonClickHereForFieldLookup}"/></a>
                    <select name="productPromoApplEnumId">
                        <#list productPromoApplEnums as productPromoApplEnum>
                            <option value="${productPromoApplEnum.enumId}">${productPromoApplEnum.get("description",locale)}</option>
                        </#list>
                    </select>
                    <input type="submit" value="${uiLabelMap.CommonAdd}">
                </form>
            </div>
        </div>
    </div>
</#if>