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

<div class="head1">${uiLabelMap.ProductGlobalPriceRules}</div>
<#if activeOnly>
    <a href="<@ofbizUrl>FindProductPriceRules?activeOnly=false</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductActiveAndInactive}]</a>
<#else>
    <a href="<@ofbizUrl>FindProductPriceRules</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductActiveOnly}]</a>
</#if>
<br/>
<br/>
<#if productPriceRules?has_content>
  <table border="1" cellpadding="2" cellspacing="0">
    <tr>
      <td><div class="tabletext"><b>${uiLabelMap.ProductPriceRuleNameId}</b></div></td>
      <td><div class="tabletext"><b>${uiLabelMap.ProductSaleRule}?</b></div></td>
      <td><div class="tabletext"><b>${uiLabelMap.CommonFromDate}</b></div></td>
      <td><div class="tabletext"><b>${uiLabelMap.CommonThruDate}</b></div></td>
      <td><div class="tabletext">&nbsp;</div></td>
    </tr>
    <#list productPriceRules as rule>
    <tr>
      <td><div class="tabletext">&nbsp;<a href="<@ofbizUrl>EditProductPriceRules?productPriceRuleId=${rule.productPriceRuleId}</@ofbizUrl>" class="buttontext">${rule.ruleName?if_exists} [${rule.productPriceRuleId}]</a></div></td>
      <td><div class="tabletext">&nbsp;${rule.isSale?if_exists}</div></td>
      <td>
        <#assign hasntStarted = false>
        <#if rule.fromDate?exists && Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().before(rule.getTimestamp("fromDate"))><#assign hasntStarted = true></#if>
        <div class="tabletext"<#if hasntStarted> style="color: red;"</#if>>
            &nbsp;${rule.fromDate?if_exists}
        </div>
      </td>
      <td>
        <#assign hasExpired = false>
        <#if rule.thruDate?exists && Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().after(rule.getTimestamp("thruDate"))><#assign hasExpired = true></#if>
        <div class="tabletext"<#if hasExpired> style="color: red;"</#if>>
            &nbsp;${rule.thruDate?if_exists}
        </div>
      </td>
      <td align="center">
        <a href="<@ofbizUrl>EditProductPriceRules?productPriceRuleId=${rule.productPriceRuleId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonEdit}]</a>
      </td>
    </tr>
    </#list>
  </table>
<#else>
    <h3>${uiLabelMap.ProductNoPriceRulesFound}.</h3>
</#if>

<br/>
<form method="post" action="<@ofbizUrl>createProductPriceRule</@ofbizUrl>" style="margin: 0;">
  <div class="head2">${uiLabelMap.ProductAddPriceRule}:</div>
  <br/>
  ${uiLabelMap.ProductName}: <input type="text" size="30" class="inputBox" name="ruleName"/>
  <input type="submit" class="smallSubmit" value="${uiLabelMap.CommonAdd}"/>
</form>
