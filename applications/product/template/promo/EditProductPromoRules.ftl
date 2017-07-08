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
<#if productPromoId?? && productPromo??>
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
  <#assign ruleClass = "2">
  <#list productPromoRules as productPromoRule>
    <#assign productPromoConds = productPromoRule.getRelated("ProductPromoCond", null, null, false)>
    <#assign productPromoActions = productPromoRule.getRelated("ProductPromoAction", null, null, false)>
      <#if productPromoRule_index != 0>
        <tr><td colspan="3"><hr /></td></tr>
      </#if>
      <tr valign="middle" class="row-level-one<#if "1" == ruleClass> alternate-row</#if>">
        <td class="label"><b> ${uiLabelMap.ProductRule} ${(productPromoRule.productPromoRuleId)!}</b></td>
        <td>
          <form method="post" action="<@ofbizUrl>updateProductPromoRule</@ofbizUrl>">
            <input type="hidden" name="productPromoId" value="${(productPromoRule.productPromoId)!}" />
            <input type="hidden" name="productPromoRuleId" value="${(productPromoRule.productPromoRuleId)!}" />
            <input type="text" size="30" name="ruleName" value="${(productPromoRule.ruleName)!}" />
            <input type="submit" value="${uiLabelMap.CommonUpdate}" />
          </form>
        </td>
        <td align="center">&nbsp;
    <#if (productPromoConds.size() == 0 && productPromoActions.size() == 0)>
          <form name="deleteProductPromoRule_${productPromoRule_index}" method="post" action="<@ofbizUrl>deleteProductPromoRule</@ofbizUrl>">
            <input type="hidden" name="productPromoId" value="${(productPromoRule.productPromoId)!}" />
            <input type="hidden" name="productPromoRuleId" value="${(productPromoRule.productPromoRuleId)!}" />
            <a href="javascript:document.deleteProductPromoRule_${productPromoRule_index}.submit()" class="buttontext">${uiLabelMap.CommonDelete}</a>
          </form>
    </#if>
        </td>
      </tr>
      <tr><td><hr /></td><td colspan="2"></td></tr>
      <tr valign="top" class="row-level-one<#if "1" == ruleClass> alternate-row</#if>">
        <td align="right" class="label">${uiLabelMap.ProductConditionsForRule} ${(productPromoRule.productPromoRuleId)!} :</td>
        <td colspan="2">
          <table cellspacing="0" class="basic-table">
    <#assign maxCondSeqId = 1>
    <#assign condClass = "2">
    <#list productPromoConds as productPromoCond>
            <tr class="row-level-two<#if "1" == condClass> alternate-row</#if>">
              <!-- if cur seq id is a number and is greater than max, set new max for input box prefill below -->
      <#if (productPromoCond.productPromoCondSeqId)??>
        <#assign curCondSeqId = Static["java.lang.Integer"].valueOf(productPromoCond.getString("productPromoCondSeqId"))>
        <#if (curCondSeqId >= maxCondSeqId)>
          <#assign maxCondSeqId = curCondSeqId + 1>
        </#if>
      </#if>
              <td></td>
              <td>
                <b>${uiLabelMap.ProductCondition} ${(productPromoCond.productPromoCondSeqId)!}</b>
                <form method="post" action="<@ofbizUrl>updateProductPromoCond</@ofbizUrl>">
                  <input type="hidden" name="productPromoId" value="${(productPromoCond.productPromoId)!}"/>
                  <input type="hidden" name="productPromoRuleId" value="${(productPromoCond.productPromoRuleId)!}"/>
                  <input type="hidden" name="productPromoCondSeqId" value="${(productPromoCond.productPromoCondSeqId)!}"/>
                  <select name="inputParamEnumId" size="1">
      <#if (productPromoCond.inputParamEnumId)??>
        <#assign inputParamEnum = productPromoCond.getRelatedOne("InputParamEnumeration", true)>
                    <option value="${productPromoCond.inputParamEnumId}"><#if inputParamEnum??>${(inputParamEnum.get("description",locale))!}<#else>[${(productPromoCond.inputParamEnumId)!}]</#if></option>
                    <option value="${(productPromoCond.inputParamEnumId)!}">&nbsp;</option>
      <#else>
                    <option value="">&nbsp;</option>
      </#if>
      <#list inputParamEnums as inputParamEnum>
                    <option value="${(inputParamEnum.enumId)!}">${(inputParamEnum.get("description",locale))!}</option>
      </#list>
                  </select>
                  <select name="operatorEnumId" size="1">
      <#if (productPromoCond.operatorEnumId)??>
        <#assign operatorEnum = productPromoCond.getRelatedOne("OperatorEnumeration", true)>
                    <option value="${(productPromoCond.operatorEnumId)!}"><#if operatorEnum??>${(operatorEnum.get("description",locale))!}<#else>[${(productPromoCond.operatorEnumId)!}]</#if></option>
                    <option value="${(productPromoCond.operatorEnumId)!}">&nbsp;</option>
      <#else>
                    <option value="">&nbsp;</option>
      </#if>
      <#list condOperEnums as condOperEnum>
                    <option value="${(condOperEnum.enumId)!}">${(condOperEnum.get("description",locale))!}</option>
      </#list>
                  </select>
                  <label>${uiLabelMap.ProductConditionValue}:</label>
                  <input type="text" size="25" name="condValue" value="${(productPromoCond.condValue)!}" />
      <#assign otherValue = productPromoCond.otherValue!>
                  <label>${uiLabelMap.CommonOther}:</label><input type="text" size="10" name="otherValue" <#if otherValue?has_content && !otherValue.contains("@")> value="${(productPromoCond.otherValue)!}"</#if> />

      <#-- Reseting the values of carrierParty and carrierShippingMethod assigned in previous iteration of productPromoConds -->
      <#assign carrierShippingMethod = ""/>
      <#assign carrierParty = ""/>
      <#if otherValue?has_content && otherValue.contains("@")>
        <#assign carrierShippingMethod = productPromoCond.otherValue!>
      </#if>
      <#if carrierShippingMethod?has_content>
        <#assign carrierParty = carrierShippingMethod.substring(0, carrierShippingMethod.indexOf("@"))>
        <#assign shippingMethodTypeId = carrierShippingMethod.substring(carrierShippingMethod.indexOf("@")+1)>
        <#assign description = (delegator.findOne("ShipmentMethodType", {"shipmentMethodTypeId":shippingMethodTypeId}, false)).description>
      <#else>
        <#assign description = "">
      </#if>
                  <label>${uiLabelMap.OrderSelectShippingMethod}:</label>
                  <select name = "carrierShipmentMethod">
                    <#if carrierShippingMethod?has_content>
                      <option value = "${carrierShippingMethod!}">${carrierParty!}&nbsp;${description}</option>
                    </#if>
                    <option value = "">&nbsp;</option>
      <#list carrierShipmentMethods as carrierShipmentMethod>
        <#assign shipmentMethodType = carrierShipmentMethod.getRelatedOne("ShipmentMethodType", true)>
                    <option value = "${carrierShipmentMethod.partyId!}@${carrierShipmentMethod.shipmentMethodTypeId!}">${carrierShipmentMethod.partyId!}&nbsp;${shipmentMethodType.get("description")!}</option>
      </#list>
                  </select>
                  <input type="submit" value="${uiLabelMap.CommonUpdate}" />
                </form>
                <form name="deleteProductPromoCondition_${productPromoRule_index}_${productPromoCond_index}" method="post" action="<@ofbizUrl>deleteProductPromoCond</@ofbizUrl>">
                  <input type="hidden" name="productPromoId" value="${(productPromoCond.productPromoId)!}" />
                  <input type="hidden" name="productPromoRuleId" value="${(productPromoCond.productPromoRuleId)!}" />
                  <input type="hidden" name="productPromoCondSeqId" value="${(productPromoCond.productPromoCondSeqId)!}" />
                  <a href="javascript:document.deleteProductPromoCondition_${productPromoRule_index}_${productPromoCond_index}.submit()" class="buttontext">${uiLabelMap.CommonDelete}</a>
                </form>
      <#-- ======================= Categories ======================== -->
                <div class="label">${uiLabelMap.ProductConditionsCategoriesForCondition} ${(productPromoCond.productPromoCondSeqId)!}:</div>
      <#assign condProductPromoCategories = productPromoCond.getRelated("ProductPromoCategory", null, null, false)>
      <#if condProductPromoCategories?has_content>
      <#list condProductPromoCategories as condProductPromoCategory>
        <#assign condProductCategory = condProductPromoCategory.getRelatedOne("ProductCategory", true)>
        <#assign condApplEnumeration = condProductPromoCategory.getRelatedOne("ApplEnumeration", true)>
                <div>
                  ${(condProductCategory.get("description",locale))!} [${condProductPromoCategory.productCategoryId}]
                  - ${(condApplEnumeration.get("description",locale))?default(condProductPromoCategory.productPromoApplEnumId)}
                  - ${uiLabelMap.ProductSubCats}? ${condProductPromoCategory.includeSubCategories?default("N")}
                  - ${uiLabelMap.CommonAnd} ${uiLabelMap.CommonGroup}: ${condProductPromoCategory.andGroupId}
                  <form name="deleteProductPromoCategoryCondition_${productPromoRule_index}_${condProductPromoCategory_index}_${productPromoCond_index}" method="post" action="<@ofbizUrl>deleteProductPromoCategory</@ofbizUrl>">
                    <input type="hidden" name="productPromoId" value="${(condProductPromoCategory.productPromoId)!}" />
                    <input type="hidden" name="productPromoRuleId" value="${(condProductPromoCategory.productPromoRuleId)!}" />
                    <input type="hidden" name="productPromoActionSeqId" value="${(condProductPromoCategory.productPromoActionSeqId)!}" />
                    <input type="hidden" name="productPromoCondSeqId" value="${(condProductPromoCategory.productPromoCondSeqId)!}" />
                    <input type="hidden" name="productCategoryId" value="${(condProductPromoCategory.productCategoryId)!}" />
                    <input type="hidden" name="andGroupId" value="${(condProductPromoCategory.andGroupId)!}" />
                    <a href="javascript:document.deleteProductPromoCategoryCondition_${productPromoRule_index}_${condProductPromoCategory_index}_${productPromoCond_index}.submit()" class="buttontext">${uiLabelMap.CommonDelete}</a>
                  </form>
                </div>
      </#list>
      <#else>
        <div>${uiLabelMap.ProductNoConditionCategories}</div>
      </#if>
                <div>
                  <form method="post" action="<@ofbizUrl>createProductPromoCategory</@ofbizUrl>" name="createProductPromoCategoryConditions">
                    <input type="hidden" name="productPromoId" value="${productPromoId}" />
                    <input type="hidden" name="productPromoRuleId" value="${productPromoCond.productPromoRuleId}" />
                    <input type="hidden" name="productPromoActionSeqId" value="_NA_" />
                    <input type="hidden" name="productPromoCondSeqId" value="${productPromoCond.productPromoCondSeqId}" />
                    <@htmlTemplate.lookupField formName="createProductPromoCategoryConditions" name="productCategoryId" id="productCategoryId_cond" fieldFormName="LookupProductCategory"/>
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
                    <input type="submit" value="${uiLabelMap.ProductAddConditionCategory}" />
                  </form>
                </div>
      <#-- ======================= Products ======================== -->
                <div class="label">${uiLabelMap.ProductConditionsProductsForCondition} ${(productPromoCond.productPromoCondSeqId)!}:</div>
      <#assign condProductPromoProducts = productPromoCond.getRelated("ProductPromoProduct", null, null, false)>
      <#if condProductPromoProducts?has_content>
      <#list condProductPromoProducts as condProductPromoProduct>
        <#assign condProduct = condProductPromoProduct.getRelatedOne("Product", true)!>
        <#assign condApplEnumeration = condProductPromoProduct.getRelatedOne("ApplEnumeration", true)>
                <div>
                  ${(condProduct.internalName)!} [${condProductPromoProduct.productId}]
                  - ${(condApplEnumeration.get("description",locale))?default(condProductPromoProduct.productPromoApplEnumId)}
                  <form name="deleteProductPromoProductCondition_${productPromoRule_index}_${productPromoCond_index}_${condProductPromoProduct_index}" method="post" action="<@ofbizUrl>deleteProductPromoProduct</@ofbizUrl>">
                    <input type="hidden" name="productPromoId" value="${(condProductPromoProduct.productPromoId)!}" />
                    <input type="hidden" name="productPromoRuleId" value="${(condProductPromoProduct.productPromoRuleId)!}" />
                    <input type="hidden" name="productPromoActionSeqId" value="${(condProductPromoProduct.productPromoActionSeqId)!}" />
                    <input type="hidden" name="productPromoCondSeqId" value="${(condProductPromoProduct.productPromoCondSeqId)!}" />
                    <input type="hidden" name="productId" value="${(condProductPromoProduct.productId)!}" />
                    <a href="javascript:document.deleteProductPromoProductCondition_${productPromoRule_index}_${productPromoCond_index}_${condProductPromoProduct_index}.submit()" class="buttontext">${uiLabelMap.CommonDelete}</a>
                  </form>
                </div>
      </#list>
      <#else>
        <div>${uiLabelMap.ProductNoConditionProducts}</div>
      </#if>
                <div>
                  <form method="post" action="<@ofbizUrl>createProductPromoProduct</@ofbizUrl>" name="createProductPromoProductConditions">
                    <input type="hidden" name="productPromoId" value="${productPromoId}" />
                    <input type="hidden" name="productPromoRuleId" value="${productPromoCond.productPromoRuleId}" />
                    <input type="hidden" name="productPromoActionSeqId" value="_NA_" />
                    <input type="hidden" name="productPromoCondSeqId" value="${productPromoCond.productPromoCondSeqId}" />
                    <@htmlTemplate.lookupField formName="createProductPromoProductConditions" name="productId" id="productId" fieldFormName="LookupProduct"/>
                    <select name="productPromoApplEnumId">
      <#list productPromoApplEnums as productPromoApplEnum>
                      <option value="${productPromoApplEnum.enumId}">${productPromoApplEnum.get("description",locale)}</option>
      </#list>
                    </select>
                    <input type="submit" value="${uiLabelMap.ProductAddConditionProducts}" />
                  </form>
                </div>
              </td>
              <td></td>
            </tr>
      <#-- toggle the row color -->
      <#if "1" == condClass>
        <#assign condClass = "1">
      <#else>
        <#assign condClass = "2">
      </#if>
    </#list>
            <tr>
              <td colspan="3">
                <form method="post" action="<@ofbizUrl>createProductPromoCond</@ofbizUrl>">
                  <input type="hidden" name="productPromoId" value="${(productPromoRule.productPromoId)!}" />
                  <input type="hidden" name="productPromoRuleId" value="${(productPromoRule.productPromoRuleId)!}" />
                  <span class="label"><b>${uiLabelMap.CommonNew}</b>&nbsp;</span>
                  <select name="inputParamEnumId" size="1">
    <#list inputParamEnums as inputParamEnum>
                    <option value="${(inputParamEnum.enumId)!}">${(inputParamEnum.get("description",locale))!}</option>
    </#list>
                  </select>
                  <select name="operatorEnumId" size="1">
    <#list condOperEnums as condOperEnum>
                    <option value="${(condOperEnum.enumId)!}">${(condOperEnum.get("description",locale))!}</option>
    </#list>
                  </select>
                  <label>${uiLabelMap.ProductConditionValue}:</label>
                  <input type="text" size="25" name="condValue" />
                  ${uiLabelMap.CommonOther}:<input type="text" size="10" name="otherValue" />
                  <label>${uiLabelMap.OrderSelectShippingMethod}:</label>
                  <select name = "carrierShipmentMethod">
                    <option value = "">--${uiLabelMap.OrderSelectShippingMethod}--</option>
    <#list carrierShipmentMethods as carrierShipmentMethod>
      <#assign shipmentMethodType = carrierShipmentMethod.getRelatedOne("ShipmentMethodType", true)>
                    <option value = "${carrierShipmentMethod.partyId!}@${carrierShipmentMethod.shipmentMethodTypeId!}">${carrierShipmentMethod.partyId!}&nbsp;${shipmentMethodType.get("description")!}</option>
    </#list>
                  </select>
                  <input type="submit" value="${uiLabelMap.ProductCreateCondition}" />
                </form>
              </td>
            </tr>
          </table>
        </td>
      </tr>
      <tr><td><hr /></td><td colspan="2"></td></tr>
      <tr valign="top" class="row-level-one<#if "1" == ruleClass> alternate-row</#if>">
        <td align="right" class="label">${uiLabelMap.ProductActionForRule} ${(productPromoRule.productPromoRuleId)!} :</td>
        <td colspan="2">
          <table cellspacing="0" class="basic-table">
    <#assign actionClass = "2">
    <#list productPromoActions as productPromoAction>
            <tr class="row-level-two<#if "1" == actionClass> alternate-row</#if>">
              <td></td>
              <td>
                <div>
                  <b> ${uiLabelMap.ProductAction} ${(productPromoAction.productPromoActionSeqId)!}</b>
                  <form method="post" action="<@ofbizUrl>updateProductPromoAction</@ofbizUrl>" name="updateProductPromoAction">
                    <input type="hidden" name="productPromoId" value="${(productPromoAction.productPromoId)!}" />
                    <input type="hidden" name="productPromoRuleId" value="${(productPromoAction.productPromoRuleId)!}" />
                    <input type="hidden" name="productPromoActionSeqId" value="${(productPromoAction.productPromoActionSeqId)!}" />
                    <select name="productPromoActionEnumId" size="1">
      <#if (productPromoAction.productPromoActionEnumId)??>
        <#assign productPromoActionCurEnum = productPromoAction.getRelatedOne("ActionEnumeration", true)>
                      <option value="${(productPromoAction.productPromoActionEnumId)!}"><#if productPromoActionCurEnum??>${(productPromoActionCurEnum.get("description",locale))!}<#else>[${(productPromoAction.productPromoActionEnumId)!}]</#if></option>
                      <option value="${(productPromoAction.productPromoActionEnumId)!}">&nbsp;</option>
      <#else>
                      <option value="">&nbsp;</option>
      </#if>
      <#list productPromoActionEnums as productPromoActionEnum>
                      <option value="${(productPromoActionEnum.enumId)!}">${(productPromoActionEnum.get("description",locale))!}</option>
      </#list>
                    </select>
                    <input type="hidden" name="orderAdjustmentTypeId" value="${(productPromoAction.orderAdjustmentTypeId)!}" />
                    ${uiLabelMap.ProductQuantity}:&nbsp;<input type="text" size="5" name="quantity" value="${(productPromoAction.quantity)!}" />
                    ${uiLabelMap.ProductAmount}:&nbsp;<input type="text" size="5" name="amount" value="${(productPromoAction.amount)!}" />
                    <br/>
                    ${uiLabelMap.ProductItemId}:&nbsp;<@htmlTemplate.lookupField value="${(productPromoAction.productId)!}" formName="updateProductPromoAction" name="productId" id="productId" fieldFormName="LookupProduct"/>
                    <br/>
                    ${uiLabelMap.PartyParty}:&nbsp;<@htmlTemplate.lookupField value="${(productPromoAction.partyId)!}" formName="updateProductPromoAction" name="partyId" id="partyId" fieldFormName="LookupUserLoginAndPartyDetails"/>
                    <br/>
                    ${uiLabelMap.ProductServiceName}:&nbsp;<input type="text" size="20" name="serviceName" value="${(productPromoAction.serviceName)!}" />
                    ${uiLabelMap.UseCartQuantity}:&nbsp;
                    <select name="useCartQuantity">
      <#if (productPromoAction.useCartQuantity)??>
        <#assign productPromoActionCurEnum = productPromoAction.getRelatedOne("ActionEnumeration", true)>
                      <option value="${(productPromoAction.useCartQuantity)!}"><#if ("Y" == productPromoAction.useCartQuantity)>${uiLabelMap.CommonY}<#else>${uiLabelMap.CommonN}</#if></option>
                      <option value="${(productPromoAction.useCartQuantity)!}">&nbsp;</option>
      <#else>
                      <option value="">&nbsp;</option>
      </#if>
                      <option value="N">${uiLabelMap.CommonN}</option>
                      <option value="Y">${uiLabelMap.CommonY}</option>
                    </select>
                    <input type="submit" value="${uiLabelMap.CommonUpdate}" />
                  </form>
                  <form name="deleteProductPromoAction_${productPromoRule_index}_${productPromoAction_index}" method="post" action="<@ofbizUrl>deleteProductPromoAction</@ofbizUrl>">
                    <input type="hidden" name="productPromoId" value="${(productPromoAction.productPromoId)!}" />
                    <input type="hidden" name="productPromoRuleId" value="${(productPromoAction.productPromoRuleId)!}" />
                    <input type="hidden" name="productPromoActionSeqId" value="${(productPromoAction.productPromoActionSeqId)!}" />
                    <a href="javascript:document.deleteProductPromoAction_${productPromoRule_index}_${productPromoAction_index}.submit()" class="buttontext">${uiLabelMap.CommonDelete}</a>
                  </form>
                </div>
      <#-- ======================= Categories ======================== -->
                <div class="label">${uiLabelMap.ProductActionsCategoriesForAction} ${(productPromoAction.productPromoActionSeqId)!}:</div>
      <#assign actionProductPromoCategories = productPromoAction.getRelated("ProductPromoCategory", null, null, false)>
      <#if actionProductPromoCategories?has_content>
      <#list actionProductPromoCategories as actionProductPromoCategory>
        <#assign actionProductCategory = actionProductPromoCategory.getRelatedOne("ProductCategory", true)>
        <#assign actionApplEnumeration = actionProductPromoCategory.getRelatedOne("ApplEnumeration", true)>
                <div>
                  ${(actionProductCategory.description)!} [${actionProductPromoCategory.productCategoryId}]
                  - ${(actionApplEnumeration.get("description",locale))?default(actionProductPromoCategory.productPromoApplEnumId)}
                  - ${uiLabelMap.ProductSubCats}? ${actionProductPromoCategory.includeSubCategories?default("N")}
                  - ${uiLabelMap.CommonAnd} ${uiLabelMap.CommonGroup}: ${actionProductPromoCategory.andGroupId}
                  <form name="deleteProductPromoCategoryAction_${productPromoRule_index}_${productPromoAction_index}_${actionProductPromoCategory_index}" action="<@ofbizUrl>deleteProductPromoCategory</@ofbizUrl>" method="post">
                    <input type="hidden" name="productPromoId" value="${(actionProductPromoCategory.productPromoId)!}" />
                    <input type="hidden" name="productPromoRuleId" value="${(actionProductPromoCategory.productPromoRuleId)!}" />
                    <input type="hidden" name="productPromoCondSeqId" value="${(actionProductPromoCategory.productPromoCondSeqId)!}" />
                    <input type="hidden" name="productPromoActionSeqId" value="${(actionProductPromoCategory.productPromoActionSeqId)!}" />
                    <input type="hidden" name="productCategoryId" value="${(actionProductPromoCategory.productCategoryId)!}" />
                    <input type="hidden" name="andGroupId" value="${(actionProductPromoCategory.andGroupId)!}" />
                    <a href="javascript:document.deleteProductPromoCategoryAction_${productPromoRule_index}_${productPromoAction_index}_${actionProductPromoCategory_index}.submit()" class="buttontext">${uiLabelMap.CommonDelete}</a>
                  </form>
                </div>
      </#list>
      <#else>
        <div>${uiLabelMap.ProductNoActionCategories}</div>
      </#if>
      <br />
                <div>
                  <form method="post" action="<@ofbizUrl>createProductPromoCategory</@ofbizUrl>" name="createProductPromoCategoryActions">
                    <input type="hidden" name="productPromoId" value="${productPromoId}" />
                    <input type="hidden" name="productPromoRuleId" value="${productPromoAction.productPromoRuleId}" />
                    <input type="hidden" name="productPromoActionSeqId" value="${productPromoAction.productPromoActionSeqId}" />
                    <input type="hidden" name="productPromoCondSeqId" value="_NA_" />
                    <@htmlTemplate.lookupField formName="createProductPromoCategoryActions" name="productCategoryId" id="productCategoryId_act" fieldFormName="LookupProductCategory"/>
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
                    <input type="submit" value="${uiLabelMap.ProductAddActionCategory}" />
                  </form>
                </div>
      <#-- ======================= Products ======================== -->
                 <div class="label">${uiLabelMap.ProductActionsProductsForAction} ${(productPromoAction.productPromoActionSeqId)!}:</div>
      <#assign actionProductPromoProducts = productPromoAction.getRelated("ProductPromoProduct", null, null, false)>
      <#if actionProductPromoProducts?has_content>
      <#list actionProductPromoProducts as actionProductPromoProduct>
        <#assign actionProduct = actionProductPromoProduct.getRelatedOne("Product", true)!>
        <#assign actionApplEnumeration = actionProductPromoProduct.getRelatedOne("ApplEnumeration", true)>
                <div>
                  ${(actionProduct.internalName)!} [${actionProductPromoProduct.productId}]
                  - ${(actionApplEnumeration.get("description",locale))?default(actionProductPromoProduct.productPromoApplEnumId)}
                  <form name="deleteProductPromoProductAction_${productPromoRule_index}_${productPromoAction_index}_${actionProductPromoProduct_index}" method="post" action="<@ofbizUrl>deleteProductPromoProduct</@ofbizUrl>"> 
                    <input type="hidden" name="productPromoId" value="${(actionProductPromoProduct.productPromoId)!}" />
                    <input type="hidden" name="productPromoRuleId" value="${(actionProductPromoProduct.productPromoRuleId)!}" />
                    <input type="hidden" name="productPromoCondSeqId" value="${(actionProductPromoProduct.productPromoCondSeqId)!}" />
                    <input type="hidden" name="productPromoActionSeqId" value="${(actionProductPromoProduct.productPromoActionSeqId)!}" />
                    <input type="hidden" name="productId" value="${(actionProductPromoProduct.productId)!}" />
                    <a href="javascript:document.deleteProductPromoProductAction_${productPromoRule_index}_${productPromoAction_index}_${actionProductPromoProduct_index}.submit()" class="buttontext">${uiLabelMap.CommonDelete}</a>
                  </form>
                </div>
      </#list>
      <#else>
        <div>${uiLabelMap.ProductNoActionProducts}</div>
      </#if>
                <div>
                  <form method="post" action="<@ofbizUrl>createProductPromoProduct</@ofbizUrl>" name="createProductPromoProductActions">
                    <input type="hidden" name="productPromoId" value="${productPromoId}" />
                    <input type="hidden" name="productPromoRuleId" value="${productPromoAction.productPromoRuleId}" />
                    <input type="hidden" name="productPromoActionSeqId" value="${productPromoAction.productPromoActionSeqId}" />
                    <input type="hidden" name="productPromoCondSeqId" value="_NA_" />
                    <@htmlTemplate.lookupField formName="createProductPromoProductActions" name="productId" id="productId" fieldFormName="LookupProduct"/>
                    <select name="productPromoApplEnumId">
      <#list productPromoApplEnums as productPromoApplEnum>
                      <option value="${productPromoApplEnum.enumId}">${productPromoApplEnum.get("description",locale)}</option>
      </#list>
                    </select>
                    <input type="submit" value="${uiLabelMap.ProductAddActionProducts}" />
                  </form>
                </div>
              </td>
              <td></td>
            </tr>
      <#-- toggle the row color -->
      <#if "2" == actionClass>
        <#assign actionClass = "1">
      <#else>
        <#assign actionClass = "2">
      </#if>
    </#list>
            <tr>
              <td colspan="3">
                <div>
                  <form method="post" action="<@ofbizUrl>createProductPromoAction</@ofbizUrl>" name="createProductPromoAction">
                    <input type="hidden" name="productPromoId" value="${(productPromoRule.productPromoId)!}" />
                    <input type="hidden" name="productPromoRuleId" value="${(productPromoRule.productPromoRuleId)!}" />
                    <span class="label"><b>${uiLabelMap.CommonNew}:</b>&nbsp;</span>
                    <select name="productPromoActionEnumId" size="1">
    <#list productPromoActionEnums as productPromoActionEnum>
                      <option value="${(productPromoActionEnum.enumId)!}">${(productPromoActionEnum.get("description",locale))!}</option>
    </#list>
                    </select>
                    <input type="hidden" name="orderAdjustmentTypeId" value="PROMOTION_ADJUSTMENT" />
                    ${uiLabelMap.ProductQuantity}:&nbsp;<input type="text" size="5" name="quantity" />
                    ${uiLabelMap.ProductAmount}:&nbsp;<input type="text" size="5" name="amount" />
                    <br/>
                    ${uiLabelMap.ProductItemId}:<@htmlTemplate.lookupField formName="createProductPromoAction" name="productId" id="productId" fieldFormName="LookupProduct"/>
                    <br/>
                    ${uiLabelMap.PartyParty}:&nbsp;<@htmlTemplate.lookupField formName="createProductPromoAction" name="partyId" id="partyId" fieldFormName="LookupUserLoginAndPartyDetails"/>
                    <br/>
                    ${uiLabelMap.ProductServiceName}:&nbsp;<input type="text" size="20" name="serviceName" />
                    ${uiLabelMap.UseCartQuantity}:&nbsp;
                    <select name="useCartQuantity">
                      <option value="N">${uiLabelMap.CommonN}</option>
                      <option value="Y">${uiLabelMap.CommonY}</option>
                    </select>
                    <input type="submit" value="${uiLabelMap.ProductCreateAction}" />
                  </form>
                </div>
              </td>
            </tr>
          </table>
        </td>
      </tr>
    <#-- toggle the row color -->
    <#if "2" == ruleClass>
      <#assign ruleClass = "1">
    <#else>
      <#assign ruleClass = "2">
    </#if>
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
      <input type="hidden" name="productPromoId" value="${productPromoId!}" />
      <span class="label">${uiLabelMap.ProductName}</span><input type="text" size="30" name="ruleName" />
      <input type="submit" value="${uiLabelMap.CommonAdd}" />
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
    <#assign promoProductCategory = promoProductPromoCategory.getRelatedOne("ProductCategory", true)>
    <#assign promoApplEnumeration = promoProductPromoCategory.getRelatedOne("ApplEnumeration", true)>
    <div>
      ${(promoProductCategory.description)!} [${promoProductPromoCategory.productCategoryId}]
      - ${(promoApplEnumeration.get("description",locale))?default(promoProductPromoCategory.productPromoApplEnumId)}
      - ${uiLabelMap.ProductSubCats}? ${promoProductPromoCategory.includeSubCategories?default("N")}
      - ${uiLabelMap.CommonAnd} ${uiLabelMap.CommonGroup}: ${promoProductPromoCategory.andGroupId}
      <form name="deleteProductPromoCategoryAction_${promoProductPromoCategory_index}" method="post" action="<@ofbizUrl>deleteProductPromoCategory</@ofbizUrl>">
        <input type="hidden" name="productPromoId" value="${(promoProductPromoCategory.productPromoId)!}" />
        <input type="hidden" name="productPromoRuleId" value="${(promoProductPromoCategory.productPromoRuleId)!}" />
        <input type="hidden" name="productPromoActionSeqId" value="${(promoProductPromoCategory.productPromoActionSeqId)!}" />
        <input type="hidden" name="productPromoCondSeqId" value="${(promoProductPromoCategory.productPromoCondSeqId)!}" />
        <input type="hidden" name="productCategoryId" value="${(promoProductPromoCategory.productCategoryId)!}" />
        <input type="hidden" name="andGroupId" value="${(promoProductPromoCategory.andGroupId)!}" />
        <a href="javascript:document.deleteProductPromoCategoryAction_${promoProductPromoCategory_index}.submit()" class="buttontext">${uiLabelMap.CommonDelete}</a>
      </form>
    </div>
  </#list>
    <div>
      <form method="post" action="<@ofbizUrl>createProductPromoCategory</@ofbizUrl>" name="createProductPromoCategoryPromotions">
        <input type="hidden" name="productPromoId" value="${productPromoId}" />
        <input type="hidden" name="productPromoRuleId" value="_NA_" />
        <input type="hidden" name="productPromoActionSeqId" value="_NA_" />
        <input type="hidden" name="productPromoCondSeqId" value="_NA_" />
        <@htmlTemplate.lookupField formName="createProductPromoCategoryPromotions" name="productCategoryId" id="productCategoryId_03" fieldFormName="LookupProductCategory"/>
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
        <input type="submit" value="${uiLabelMap.CommonAdd}" />
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
    <#assign promoProduct = promoProductPromoProduct.getRelatedOne("Product", true)!>
    <#assign promoApplEnumeration = promoProductPromoProduct.getRelatedOne("ApplEnumeration", true)>
    <div>
      ${(promoProduct.internalName)!} [${promoProductPromoProduct.productId}]
      - ${(promoApplEnumeration.get("description",locale))?default(promoProductPromoProduct.productPromoApplEnumId)}
      <form name="deleteProductPromoProductAction_${promoProductPromoProduct_index}" action="<@ofbizUrl>deleteProductPromoProduct</@ofbizUrl>" method="post">
        <input type="hidden" name="productPromoId" value="${(promoProductPromoProduct.productPromoId)!}" />
        <input type="hidden" name="productPromoRuleId" value="${(promoProductPromoProduct.productPromoRuleId)!}" />
        <input type="hidden" name="productPromoActionSeqId" value="${(promoProductPromoProduct.productPromoActionSeqId)!}" />
        <input type="hidden" name="productPromoCondSeqId" value="${(promoProductPromoProduct.productPromoCondSeqId)!}" />
        <input type="hidden" name="productId" value="${(promoProductPromoProduct.productId)!}" />
        <a href="javascript:document.deleteProductPromoProductAction_${promoProductPromoProduct_index}.submit()" class="buttontext">${uiLabelMap.CommonDelete}</a>
      </form>
    </div>
  </#list>
    <div>
      <form method="post" name="createpromoproductform" action="<@ofbizUrl>createProductPromoProduct</@ofbizUrl>">
        <input type="hidden" name="productPromoId" value="${productPromoId}" />
        <input type="hidden" name="productPromoRuleId" value="_NA_" />
        <input type="hidden" name="productPromoActionSeqId" value="_NA_" />
        <input type="hidden" name="productPromoCondSeqId" value="_NA_" />
        <span class="label">${uiLabelMap.ProductProductId}</span>*<@htmlTemplate.lookupField formName="createpromoproductform" name="productId" id="productId" fieldFormName="LookupProduct"/>
        <select name="productPromoApplEnumId">
  <#list productPromoApplEnums as productPromoApplEnum>
          <option value="${productPromoApplEnum.enumId}">${productPromoApplEnum.get("description",locale)}</option>
  </#list>
        </select>
        <input type="submit" value="${uiLabelMap.CommonAdd}" />
      </form>
    </div>
  </div>
</div>
</#if>
