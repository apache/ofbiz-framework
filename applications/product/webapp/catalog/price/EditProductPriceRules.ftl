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
<div class="head1">${uiLabelMap.ProductGlobalPriceRule}</div>
<a href="<@ofbizUrl>FindProductPriceRules</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductFindRule}]</a>

<br/>

<table border="1" width="100%" cellpadding="2" cellspacing="0">
  <tr>
    <td width="10%"><div class="tabletext"><b>${uiLabelMap.ProductRuleId}</b></div></td>
    <td width="80%"><div class="tabletext"><b>${uiLabelMap.ProductRuleNameFromDateThruDate}</b></div></td>
    <td width="10%"><div class="tabletext"><b>&nbsp;</b></div></td>
  </tr>

<#if productPriceRule?exists>
  <#assign productPriceConds = productPriceRule.getRelated("ProductPriceCond")>
  <#assign productPriceActions = productPriceRule.getRelated("ProductPriceAction")>
  <tr valign="middle">
    <td><div class="tabletext"><b>${productPriceRule.productPriceRuleId}</b></div></td>
    <td align="left">
        <FORM method="post" action="<@ofbizUrl>updateProductPriceRule</@ofbizUrl>" name="updateProductPriceRule">
            <input type="hidden" name="productPriceRuleId" value="${productPriceRule.productPriceRuleId}">
            <input type="text" size="15" name="ruleName" value="${productPriceRule.ruleName}" class="inputBox">
            <input type="text" size="15" name="description" value="${productPriceRule.description?if_exists}" class="inputBox">
            <input type="text" size="22" name="fromDate" value="${productPriceRule.fromDate?if_exists}" class="inputBox"><a href="javascript:call_cal(document.updateProductPriceRule.fromDate, null);"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Calendar'></a>
            <input type="text" size="22" name="thruDate" value="${productPriceRule.thruDate?if_exists}" class="inputBox"><a href="javascript:call_cal(document.updateProductPriceRule.thruDate, null);"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Calendar'></a>
            &nbsp;&nbsp;
            <#assign saleRule = productPriceRule.isSale?exists && productPriceRule.isSale == "Y">
            <span class="tabletext"><b>${uiLabelMap.ProductNotifySale}:</b>&nbsp;
            <input type="radio" class="radioButton" name="isSale" value="Y" <#if saleRule>CHECKED</#if>>${uiLabelMap.CommonYes}&nbsp;
            <input type="radio" name="isSale" value="N" <#if !saleRule>CHECKED</#if>>${uiLabelMap.CommonNo}</span>
            &nbsp;&nbsp;
            <INPUT type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;">
        </FORM>
    </td>
    <td align="center">&nbsp;
      <#if !productPriceConds?has_content && !productPriceActions?has_content>
          <a href="<@ofbizUrl>deleteProductPriceRule?productPriceRuleId=${productPriceRule.productPriceRuleId}</@ofbizUrl>" class="buttontext">
          [${uiLabelMap.CommonDelete}]</a>
      </#if>
    </td>
  </tr>
  <tr valign="top">
    <td align="right"><div class="tabletext">${uiLabelMap.ProductConditions}:</div></td>
    <td align="left" colspan="2">
        <table border="1" width="100%" cellpadding="2" cellspacing="0">
          <tr>
            <td width="5%"><div class="tabletext"><b>${uiLabelMap.ProductSeqId}</b></div></td>
            <td width="85%"><div class="tabletext"><b>${uiLabelMap.ProductInputOperatorValue}</b></div></td>
            <td width="10%"><div class="tabletext"><b>&nbsp;</b></div></td>
          </tr>
          <#assign maxCondSeqId = 1>
          <#list productPriceConds as productPriceCond>
              <tr>
                <#-- if cur seq id is a number and is greater than max, set new max for input box prefill below -->
                <#assign curCondSeqId = productPriceCond.productPriceCondSeqId?number>
                <#if (curCondSeqId >= maxCondSeqId)><#assign maxCondSeqId = curCondSeqId + 1></#if>
                <td><div class="tabletext"><b>${productPriceCond.productPriceCondSeqId}</b></div></td>
                <td align="left">
                    <FORM method="post" action="<@ofbizUrl>updateProductPriceCond</@ofbizUrl>">
                        <input type="hidden" name="productPriceRuleId" value="${productPriceCond.productPriceRuleId}"/>
                        <input type="hidden" name="productPriceCondSeqId" value="${productPriceCond.productPriceCondSeqId}"/>
                        <select name="inputParamEnumId" size="1" class="selectBox">
                            <#if productPriceCond.inputParamEnumId?has_content>
                              <#assign inputParamEnum = productPriceCond.getRelatedOneCache("InputParamEnumeration")?if_exists>
                              <option value="${productPriceCond.inputParamEnumId}"><#if inputParamEnum?exists>${inputParamEnum.get("description",locale)}<#else>[${productPriceCond.inputParamEnumId}]</#if></option>
                              <option value="${productPriceCond.inputParamEnumId}">&nbsp;</option>
                            <#else>
                              <option value="">&nbsp;</option>
                            </#if>
                            <#list inputParamEnums as inputParamEnum>
                              <option value="${inputParamEnum.enumId}">${inputParamEnum.get("description",locale)}<#--[${inputParamEnum.enumId}]--></option>
                            </#list>
                        </select>
                        <select name="operatorEnumId" size="1" class="selectBox">
                            <#if productPriceCond.operatorEnumId?has_content>
                              <#assign operatorEnum = productPriceCond.getRelatedOneCache("OperatorEnumeration")?if_exists>
                              <option value="${productPriceCond.operatorEnumId}"><#if operatorEnum?exists>${operatorEnum.get("description",locale)}<#else>[${productPriceCond.operatorEnumId}]</#if></option>
                              <option value="${productPriceCond.operatorEnumId}">&nbsp;</option>
                            <#else>
                              <option value="">&nbsp;</option>
                            </#if>
                            <#list condOperEnums as condOperEnum>
                              <option value="${condOperEnum.enumId}">${condOperEnum.get("description",locale)}<#--[${condOperEnum.enumId}]--></option>
                            </#list>
                        </select>
                        <input type="text" size="20" name="condValue" value="${productPriceCond.condValue?if_exists}" class="inputBox">
                        <INPUT type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;">
                    </FORM>
                </td>
                <td align="center">
                  <a href="<@ofbizUrl>deleteProductPriceCond?productPriceRuleId=${productPriceCond.productPriceRuleId}&productPriceCondSeqId=${productPriceCond.productPriceCondSeqId}</@ofbizUrl>" class="buttontext">
                  [${uiLabelMap.CommonDelete}]</a>
                </td>
              </tr>
          </#list>
          <tr>
            <td align="left" colspan="3">
                <FORM method="post" action="<@ofbizUrl>createProductPriceCond</@ofbizUrl>">
                    <input type="hidden" name="productPriceRuleId" value="${productPriceRule.productPriceRuleId}">
                    <span class="tabletext"><b>${uiLabelMap.CommonNew}:</b>&nbsp;</span>
                    <select name="inputParamEnumId" size="1" class="selectBox">
                        <#list inputParamEnums as inputParamEnum>
                          <option value="${inputParamEnum.enumId}">${inputParamEnum.get("description",locale)}<#--[${inputParamEnum.enumId}]--></option>
                        </#list>
                    </select>
                    <select name="operatorEnumId" size="1" class="selectBox">
                        <#list condOperEnums as condOperEnum>
                          <option value="${condOperEnum.enumId}">${condOperEnum.get("description",locale)}<#--[${condOperEnum.enumId}]--></option>
                        </#list>
                    </select>
                    <input type="text" size="20" name="condValue" class="inputBox">
                    <INPUT type="submit" value="${uiLabelMap.CommonCreate}" style="font-size: x-small;">
                </FORM>
            </td>
          </tr>
        </table>
    </td>
  </tr>
  <tr valign="top">
    <td align="right"><div class="tabletext">${uiLabelMap.ProductActions}:</div></td>
    <td align="left" colspan="2">
        <table border="1" width="100%" cellpadding="2" cellspacing="0">
          <tr>
            <td width="5%"><div class="tabletext"><b>${uiLabelMap.ProductSeqId}</b></div></td>
            <td width="85%"><div class="tabletext"><b>${uiLabelMap.ProductActionTypeAmount}</b></div></td>
            <td width="10%"><div class="tabletext"><b>&nbsp;</b></div></td>
          </tr>
          <#assign maxActionSeqId = 1>
          <#list productPriceActions as productPriceAction>
              <tr>
                <#-- if cur seq id is a number and is greater than max, set new max for input box prefill below -->
                <#assign curActionSeqId = productPriceAction.productPriceActionSeqId?number>
                <#if (curActionSeqId >= maxActionSeqId)><#assign maxActionSeqId = curActionSeqId + 1></#if>
                <td><div class="tabletext"><b>${productPriceAction.productPriceActionSeqId}</b></div></td>
                <td align="left">
                    <FORM method="post" action="<@ofbizUrl>updateProductPriceAction</@ofbizUrl>">
                        <input type="hidden" name="productPriceRuleId" value="${productPriceAction.productPriceRuleId}">
                        <input type="hidden" name="productPriceActionSeqId" value="${productPriceAction.productPriceActionSeqId}">
                        <select name="productPriceActionTypeId" size="1" class="selectBox">
                            <#if productPriceAction.productPriceActionTypeId?has_content>
                              <#assign productPriceActionType = productPriceAction.getRelatedOneCache("ProductPriceActionType")>
                              <option value="${productPriceAction.productPriceActionTypeId}"><#if productPriceActionType?exists>${productPriceActionType.get("description",locale)}<#else>[${productPriceAction.productPriceActionTypeId}]</#if></option>
                              <option value="${productPriceAction.productPriceActionTypeId}">&nbsp;</option>
                            <#else>
                              <option value="">&nbsp;</option>
                            </#if>
                            <#list productPriceActionTypes as productPriceActionType>
                              <option value="${productPriceActionType.productPriceActionTypeId}">${productPriceActionType.get("description",locale)}<#--[${productPriceActionType.productPriceActionTypeId}]--></option>
                            </#list>
                        </select>
                        <input type="text" size="8" name="amount" value="${productPriceAction.amount?if_exists}" class="inputBox">
                        <INPUT type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;">
                    </FORM>
                </td>
                <td align="center">
                  <a href="<@ofbizUrl>deleteProductPriceAction?productPriceRuleId=${productPriceAction.productPriceRuleId}&productPriceActionSeqId=${productPriceAction.productPriceActionSeqId}</@ofbizUrl>" class="buttontext">
                  [${uiLabelMap.CommonDelete}]</a>
                </td>
              </tr>
          </#list>
          <tr>
            <td align="left" colspan="3">
                <FORM method="post" action="<@ofbizUrl>createProductPriceAction</@ofbizUrl>">
                    <input type="hidden" name="productPriceRuleId" value="${productPriceRule.productPriceRuleId}">
                    <span class="tabletext"><b>${uiLabelMap.CommonNew}:</b>&nbsp;</span>
                    <select name="productPriceActionTypeId" size="1" class="selectBox">
                        <#list productPriceActionTypes as productPriceActionType>
                          <option value="${productPriceActionType.productPriceActionTypeId}">${productPriceActionType.get("description",locale)}<#--[${productPriceActionType.productPriceActionTypeId}]--></option>
                        </#list>
                    </select>
                    <input type="text" size="8" name="amount" class="inputBox">
                    <INPUT type="submit" value="${uiLabelMap.CommonCreate}" style="font-size: x-small;">
                </FORM>
            </td>
          </tr>
        </table>
    </td>
  </tr>
</#if>
</table>
