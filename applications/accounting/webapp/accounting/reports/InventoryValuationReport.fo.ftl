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
<#escape x as x?xml>
  <fo:block font-size="14pt" font-weight="bold" text-align="center">${uiLabelMap.AccountingInventoryValuation}</fo:block>
  <fo:block font-size="10pt" text-align="left"  font-weight="bold">
    <#if parameters.organizationPartyId?has_content>${uiLabelMap.Party} : ${parameters.organizationPartyId?if_exists}</#if>
  </fo:block>
  <fo:block font-size="10pt" text-align="left" font-weight="bold">
    <#if parameters.facilityId?has_content>
      <#assign facility = (delegator.findOne("Facility", {"facilityId" : parameters.facilityId}, false))?if_exists>
      <#if facility.facilityName?has_content>${uiLabelMap.Facility} : ${facility.facilityName?if_exists}</#if>
    </#if>
  </fo:block>
  <fo:block font-size="10pt" text-align="left" font-weight="bold">
    <#if parameters.productCategoryId?has_content>
      <#assign productCategory = (delegator.findOne("ProductCategory", {"productCategoryId" : parameters.productCategoryId}, false))?if_exists>
      <#if productCategory.categoryName?has_content>${uiLabelMap.ProductProductCategory} : ${productCategory.categoryName?if_exists}</#if>
    </#if>
  </fo:block>
  <fo:block font-size="10pt" text-align="left" font-weight="bold">
    <#if parameters.fromDate?has_content>
      ${uiLabelMap.CommonFromDate} : ${parameters.fromDate?if_exists}
    </#if>
  </fo:block>
  <fo:block font-size="10pt" text-align="left" font-weight="bold">
    <#if parameters.thruDate?has_content>
      ${uiLabelMap.CommonThruDate} : ${parameters.thruDate?if_exists}
    </#if>
  </fo:block>
  <#if inventoryValuationList?has_content>
    <fo:block><fo:leader/></fo:block>
    <fo:block space-after.optimum="10pt" font-size="10pt">
      <fo:table>
        <fo:table-column column-width="130pt"/>
        <fo:table-column column-width="130pt"/>
        <fo:table-column column-width="130pt"/>
        <fo:table-column column-width="130pt"/>
        <fo:table-header>
          <fo:table-row font-weight="bold">
            <fo:table-cell padding="2pt" background-color="#D4D0C8" border="1pt solid" border-width=".1mm">
              <fo:block text-align="center">${uiLabelMap.ProductProduct}</fo:block>
            </fo:table-cell>
            <fo:table-cell padding="2pt" background-color="#D4D0C8" border="1pt solid" border-width=".1mm">
              <fo:block text-align="center">${uiLabelMap.AccountingTotalQuantityOnHand}</fo:block>
            </fo:table-cell>
            <fo:table-cell padding="2pt" background-color="#D4D0C8" border="1pt solid" border-width=".1mm">
              <fo:block text-align="center">${uiLabelMap.FormFieldTitle_unitCost}</fo:block>
            </fo:table-cell>
            <fo:table-cell padding="2pt" background-color="#D4D0C8" border="1pt solid" border-width=".1mm">
              <fo:block text-align="center">${uiLabelMap.CommonTotalValue}</fo:block>
            </fo:table-cell>
          </fo:table-row>
        </fo:table-header>
        <fo:table-body>
          <#list inventoryValuationList as inventoryValuation>
            <#assign currencyUomId = inventoryValuation.currencyUomId?if_exists>
            <fo:table-row>
              <fo:table-cell padding="2pt" border="1pt solid" border-width=".1mm">
                <fo:block text-align="center">${inventoryValuation.productId?if_exists}</fo:block>
              </fo:table-cell>
              <fo:table-cell padding="2pt" border="1pt solid" border-width=".1mm">
                <fo:block text-align="center">${inventoryValuation.totalQuantityOnHand?if_exists}</fo:block>
              </fo:table-cell>
              <fo:table-cell padding="2pt" border="1pt solid" border-width=".1mm">
                <fo:block text-align="center">
                  <#if currencyUomId != null>
                    <@ofbizCurrency amount = inventoryValuation.productAverageCost?if_exists isoCode = currencyUomId/>
                  </#if>
                </fo:block>
              </fo:table-cell>
              <fo:table-cell padding="2pt" border="1pt solid" border-width=".1mm">
                <fo:block text-align="center">
                  <#if currencyUomId != null>
                    <@ofbizCurrency amount = inventoryValuation.totalInventoryCost?if_exists isoCode = currencyUomId/>
                  </#if>
                </fo:block>
              </fo:table-cell>
            </fo:table-row>
          </#list>
        </fo:table-body>
      </fo:table>
    </fo:block>
  <#else>
    <fo:table-row>
      <fo:table-cell number-columns-spanned="2"/>
      <fo:table-cell padding="2pt">
        <fo:block>${uiLabelMap.AccountingNoRecordFound}</fo:block>
      </fo:table-cell>
      <fo:table-cell number-columns-spanned="2"/>
    </fo:table-row>
  </#if>
</#escape>
