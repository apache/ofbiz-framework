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
        <h3>${uiLabelMap.ProductGlobalPriceRules}</h3>
    </div>
    <div class="screenlet-body">
        <#if activeOnly>
            <a href="<@ofbizUrl>FindProductPriceRules?activeOnly=false</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductActiveAndInactive}</a>
        <#else>
            <a href="<@ofbizUrl>FindProductPriceRules</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductActiveOnly}</a>
        </#if>
        <br/>
        <#if productPriceRules?has_content>
          <table cellspacing="0" class="basic-table">
            <tr class="header-row">
              <td><b>${uiLabelMap.ProductPriceRuleNameId}</b></td>
              <td><b>${uiLabelMap.ProductSaleRule}?</b></td>
              <td><b>${uiLabelMap.CommonFromDate}</b></td>
              <td><b>${uiLabelMap.CommonThruDate}</b></td>
              <td>&nbsp;</td>
            </tr>
            <#list productPriceRules as rule>
            <tr>
              <td>&nbsp;<a href="<@ofbizUrl>EditProductPriceRules?productPriceRuleId=${rule.productPriceRuleId}</@ofbizUrl>" class="buttontext">${rule.ruleName?if_exists} [${rule.productPriceRuleId}]</a></td>
              <td>&nbsp;${rule.isSale?if_exists}</td>
              <td>
                <#assign hasntStarted = false>
                <#if rule.fromDate?exists && Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().before(rule.getTimestamp("fromDate"))><#assign hasntStarted = true></#if>
                <div <#if hasntStarted> style="color: red;"</#if>>
                    &nbsp;${rule.fromDate?if_exists}
                </div>
              </td>
              <td>
                <#assign hasExpired = false>
                <#if rule.thruDate?exists && Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().after(rule.getTimestamp("thruDate"))><#assign hasExpired = true></#if>
                <div <#if hasExpired> style="color: red;"</#if>>
                    &nbsp;${rule.thruDate?if_exists}
                </div>
              </td>
              <td align="center">
                <a href="<@ofbizUrl>EditProductPriceRules?productPriceRuleId=${rule.productPriceRuleId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a>
              </td>
            </tr>
            </#list>
          </table>
        <#else>
            <h3>${uiLabelMap.ProductNoPriceRulesFound}.</h3>
        </#if>
    </div>
</div>
<div class="screenlet">
    <div class="screenlet-title-bar">
        <h3>${uiLabelMap.ProductAddPriceRule}</h3>
    </div>
    <div class="screenlet-body">    
        <form method="post" action="<@ofbizUrl>createProductPriceRule</@ofbizUrl>">
          <span class="label">${uiLabelMap.ProductName}</span><input type="text" size="30" name="ruleName"/>
          <input type="submit" class="smallSubmit" value="${uiLabelMap.CommonAdd}"/>
        </form>
    </div>
</div>