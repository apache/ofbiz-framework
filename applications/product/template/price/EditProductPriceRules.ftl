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
        <h3>${uiLabelMap.ProductGlobalPriceRule}</h3>
    </div>
    <div class="screenlet-body">
        <a href="<@ofbizUrl>FindProductPriceRules</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductFindRule}</a>
        <table cellspacing="0" class="basic-table">
          <tr class="header-row">
            <td width="10%"><b>${uiLabelMap.ProductRuleId}</b></td>
            <td width="80%"><b>${uiLabelMap.ProductRuleNameFromDateThruDate}</b></td>
            <td width="10%"><b>&nbsp;</b></td>
          </tr>
        <#if productPriceRule??>
          <#assign productPriceConds = productPriceRule.getRelated("ProductPriceCond", null, null, false)>
          <#assign productPriceActions = productPriceRule.getRelated("ProductPriceAction", null, null, false)>
          <tr valign="middle">
            <td class="label"><b>${productPriceRule.productPriceRuleId}</b></td>
            <td>
                <form method="post" action="<@ofbizUrl>updateProductPriceRule</@ofbizUrl>" name="updateProductPriceRule">
                    <input type="hidden" name="productPriceRuleId" value="${productPriceRule.productPriceRuleId}" />
                    <input type="text" size="15" name="ruleName" value="${productPriceRule.ruleName}" />
                    <input type="text" size="15" name="description" value="${productPriceRule.description!}" />
                    <@htmlTemplate.renderDateTimeField name="fromDate" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="${productPriceRule.fromDate!}" size="25" maxlength="30" id="fromDate1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                    <@htmlTemplate.renderDateTimeField name="thruDate" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="${productPriceRule.thruDate!}" size="25" maxlength="30" id="thruDate1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                    &nbsp;&nbsp;
                    <#assign saleRule = productPriceRule.isSale?? && "Y" == productPriceRule.isSale>
                    <div>
                    <span class="label"><b>${uiLabelMap.ProductNotifySale}</b></span>&nbsp;
                    <label><input type="radio" name="isSale" value="Y" <#if saleRule>checked="checked"</#if> />${uiLabelMap.CommonYes}</label>&nbsp;
                    <label><input type="radio" name="isSale" value="N" <#if !saleRule>checked="checked"</#if> />${uiLabelMap.CommonNo}</label>
                    &nbsp;&nbsp;
                    <input type="submit" value="${uiLabelMap.CommonUpdate}" />
                    </div>
                </form>
            </td>
            <td align="center">&nbsp;
              <#if !productPriceConds?has_content && !productPriceActions?has_content>
                  <form method="post" action="<@ofbizUrl>deleteProductPriceRule</@ofbizUrl>" name="deleteProductPriceRule">
                      <input type="hidden" name="productPriceRuleId" value="${productPriceRule.productPriceRuleId}" />
                      <input type="submit" value="${uiLabelMap.CommonDelete}" />
                  </form>
              </#if>
            </td>
          </tr>
          <tr valign="top">
            <td align="right" class="label">${uiLabelMap.ProductConditions}</td>
            <td colspan="2">
                <table cellspacing="0" class="basic-table">
                  <tr class="header-row">
                    <td width="5%"><b>${uiLabelMap.ProductSeqId}</b></td>
                    <td width="85%"><b>${uiLabelMap.ProductInputOperatorValue}</b></td>
                    <td width="10%"><b>&nbsp;</b></td>
                  </tr>
                  <#assign maxCondSeqId = 1>
                  <#assign rowClass = "2">
                  <#list productPriceConds as productPriceCond>
                      <tr valign="middle"<#if "1" == rowClass> class="alternate-row"</#if>>
                        <#-- if cur seq id is a number and is greater than max, set new max for input box prefill below -->
                        <#assign curCondSeqId = productPriceCond.productPriceCondSeqId?number>
                        <#if (curCondSeqId >= maxCondSeqId)><#assign maxCondSeqId = curCondSeqId + 1></#if>
                        <td><b>${productPriceCond.productPriceCondSeqId}</b></td>
                        <td>
                            <form method="post" action="<@ofbizUrl>updateProductPriceCond</@ofbizUrl>">
                                <input type="hidden" name="productPriceRuleId" value="${productPriceCond.productPriceRuleId}"/>
                                <input type="hidden" name="productPriceCondSeqId" value="${productPriceCond.productPriceCondSeqId}"/>
                                <select name="inputParamEnumId" size="1">
                                    <#if productPriceCond.inputParamEnumId?has_content>
                                      <#assign inputParamEnum = productPriceCond.getRelatedOne("InputParamEnumeration", true)!>
                                      <option value="${productPriceCond.inputParamEnumId}"><#if inputParamEnum??>${inputParamEnum.get("description",locale)}<#else>[${productPriceCond.inputParamEnumId}]</#if></option>
                                      <option value="${productPriceCond.inputParamEnumId}">&nbsp;</option>
                                    <#else>
                                      <option value="">&nbsp;</option>
                                    </#if>
                                    <#list inputParamEnums as inputParamEnum>
                                      <option value="${inputParamEnum.enumId}">${inputParamEnum.get("description",locale)}<#--[${inputParamEnum.enumId}]--></option>
                                    </#list>
                                </select>
                                <select name="operatorEnumId" size="1">
                                    <#if productPriceCond.operatorEnumId?has_content>
                                      <#assign operatorEnum = productPriceCond.getRelatedOne("OperatorEnumeration", true)!>
                                      <option value="${productPriceCond.operatorEnumId}"><#if operatorEnum??>${operatorEnum.get("description",locale)}<#else>[${productPriceCond.operatorEnumId}]</#if></option>
                                      <option value="${productPriceCond.operatorEnumId}">&nbsp;</option>
                                    <#else>
                                      <option value="">&nbsp;</option>
                                    </#if>
                                    <#list condOperEnums as condOperEnum>
                                      <option value="${condOperEnum.enumId}">${condOperEnum.get("description",locale)}<#--[${condOperEnum.enumId}]--></option>
                                    </#list>
                                </select>
                                <input type="text" size="20" name="condValue" value="${productPriceCond.condValue!}" />
                                <input type="submit" value="${uiLabelMap.CommonUpdate}" />
                            </form>
                        </td>
                        <td align="center">
                         <form name="deleteProductPriceCond_${productPriceCond_index}" method= "post" action= "<@ofbizUrl>deleteProductPriceCond</@ofbizUrl>">
                           <input type="hidden" name="productPriceRuleId" value="${productPriceCond.productPriceRuleId}" />
                           <input type="hidden" name="productPriceCondSeqId" value="${productPriceCond.productPriceCondSeqId}" />
                           <input type="submit" value="${uiLabelMap.CommonDelete}"/>
                         </form>
                        </td>
                      </tr>
                      <#-- toggle the row color -->
                      <#if "2" == rowClass>
                        <#assign rowClass = "1">
                      <#else>
                        <#assign rowClass = "2">
                      </#if>
                  </#list>
                  <tr>
                    <td colspan="3">
                        <form method="post" action="<@ofbizUrl>createProductPriceCond</@ofbizUrl>">
                            <input type="hidden" name="productPriceRuleId" value="${productPriceRule.productPriceRuleId}" />
                            <span class="label"><b>${uiLabelMap.CommonNew}</b>&nbsp;</span>
                            <select name="inputParamEnumId" size="1">
                                <#list inputParamEnums as inputParamEnum>
                                  <option value="${inputParamEnum.enumId}">${inputParamEnum.get("description",locale)}<#--[${inputParamEnum.enumId}]--></option>
                                </#list>
                            </select>
                            <select name="operatorEnumId" size="1">
                                <#list condOperEnums as condOperEnum>
                                  <option value="${condOperEnum.enumId}">${condOperEnum.get("description",locale)}<#--[${condOperEnum.enumId}]--></option>
                                </#list>
                            </select>
                            <input type="text" size="20" name="condValue" />
                            <input type="submit" value="${uiLabelMap.CommonCreate}" />
                        </form>
                    </td>
                  </tr>
                </table>
            </td>
          </tr>
          <tr valign="top">
            <td align="right" class="label">${uiLabelMap.ProductActions}</td>
            <td colspan="2">
                <table cellspacing="0" class="basic-table">
                  <tr class="header-row">
                    <td width="5%"><b>${uiLabelMap.ProductSeqId}</b></td>
                    <td width="85%"><b>${uiLabelMap.ProductActionTypeAmount}</b></td>
                    <td width="10%"><b>&nbsp;</b></td>
                  </tr>
                  <#assign rowClass = "2">
                  <#list productPriceActions as productPriceAction>
                      <tr valign="middle"<#if "1" == rowClass> class="alternate-row"</#if>>
                        <td class="label"><b>${productPriceAction.productPriceActionSeqId}</b></td>
                        <td>
                            <form method="post" action="<@ofbizUrl>updateProductPriceAction</@ofbizUrl>">
                                <input type="hidden" name="productPriceRuleId" value="${productPriceAction.productPriceRuleId}" />
                                <input type="hidden" name="productPriceActionSeqId" value="${productPriceAction.productPriceActionSeqId}" />
                                <select name="productPriceActionTypeId" size="1">
                                    <#if productPriceAction.productPriceActionTypeId?has_content>
                                      <#assign productPriceActionType = productPriceAction.getRelatedOne("ProductPriceActionType", true)>
                                      <option value="${productPriceAction.productPriceActionTypeId}"><#if productPriceActionType??>${productPriceActionType.get("description",locale)}<#else>[${productPriceAction.productPriceActionTypeId}]</#if></option>
                                      <option value="${productPriceAction.productPriceActionTypeId}">&nbsp;</option>
                                    <#else>
                                      <option value="">&nbsp;</option>
                                    </#if>
                                    <#list productPriceActionTypes as productPriceActionType>
                                      <option value="${productPriceActionType.productPriceActionTypeId}">${productPriceActionType.get("description",locale)}<#--[${productPriceActionType.productPriceActionTypeId}]--></option>
                                    </#list>
                                </select>
                                <input type="text" size="8" name="amount" value="${productPriceAction.amount!}" />
                                <input type="submit" value="${uiLabelMap.CommonUpdate}" />
                            </form>
                        </td>
                        <td align="center">
                          <form name="deleteProductPriceAction_${productPriceAction_index}" method="post" action="<@ofbizUrl>deleteProductPriceAction</@ofbizUrl>">
                            <input type="hidden" name="productPriceRuleId" value="${productPriceAction.productPriceRuleId}" />
                            <input type="hidden" name="productPriceActionSeqId" value="${productPriceAction.productPriceActionSeqId}" />
                            <input type="submit" value="${uiLabelMap.CommonDelete}" />
                          </form>
                        </td>
                      </tr>
                      <#-- toggle the row color -->
                      <#if "2" == rowClass>
                        <#assign rowClass = "1">
                      <#else>
                        <#assign rowClass = "2">
                      </#if>
                  </#list>
                  <tr>
                    <td colspan="3">
                        <form method="post" action="<@ofbizUrl>createProductPriceAction</@ofbizUrl>">
                            <input type="hidden" name="productPriceRuleId" value="${productPriceRule.productPriceRuleId}" />
                            <span class="label"><b>${uiLabelMap.CommonNew}</b>&nbsp;</span>
                            <select name="productPriceActionTypeId" size="1">
                                <#list productPriceActionTypes as productPriceActionType>
                                  <option value="${productPriceActionType.productPriceActionTypeId}">${productPriceActionType.get("description",locale)}<#--[${productPriceActionType.productPriceActionTypeId}]--></option>
                                </#list>
                            </select>
                            <input type="text" size="8" name="amount" />
                            <input type="submit" value="${uiLabelMap.CommonCreate}" />
                        </form>
                    </td>
                  </tr>
                </table>
            </td>
          </tr>
        </#if>
        </table>
    </div>
</div>
