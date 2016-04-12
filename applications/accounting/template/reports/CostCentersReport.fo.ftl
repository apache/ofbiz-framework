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
    <#if glAcctBalancesByCostCenter?has_content && glAccountCategories?has_content>
        <fo:table table-layout="fixed" border="1pt solid" border-width=".1mm" width="19cm">
            <fo:table-header>
                <fo:table-cell border="1pt solid" border-width=".1mm">
                    <fo:block text-align="center">${uiLabelMap.FormFieldTitle_glAccountId}</fo:block>
                </fo:table-cell>
                <fo:table-cell border="1pt solid" border-width=".1mm">
                    <fo:block text-align="center">${uiLabelMap.FormFieldTitle_accountCode}</fo:block>
                </fo:table-cell>
                <fo:table-cell border="1pt solid" border-width=".1mm">
                    <fo:block text-align="center">${uiLabelMap.FormFieldTitle_accountName}</fo:block>
                </fo:table-cell>
                <fo:table-cell border="1pt solid" border-width=".1mm">
                    <fo:block text-align="center">${uiLabelMap.FormFieldTitle_postedBalance} - (${currencyUomId!})</fo:block>
                </fo:table-cell>
                <#list glAccountCategories as glAccountCategory>
                    <fo:table-cell border="1pt solid" border-width=".1mm">
                        <fo:block text-align="center">${glAccountCategory.description!} - (${currencyUomId!})</fo:block>
                    </fo:table-cell>
                </#list>
            </fo:table-header>
            <fo:table-body>
                <#list glAcctBalancesByCostCenter as glAcctBalanceByCostCenter>
                    <#if glAcctgOrgAndCostCenter?has_content>
                        <fo:table-row>
                            <fo:table-cell border="1pt solid" border-width=".1mm">
                                <fo:block text-align="center">${glAcctBalanceByCostCenter.glAccountId!}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell border="1pt solid" border-width=".1mm">
                                <fo:block text-align="center">${glAcctBalanceByCostCenter.accountCode!}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell border="1pt solid" border-width=".1mm">
                                <fo:block text-align="center">${glAcctBalanceByCostCenter.accountName!}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell border="1pt solid" border-width=".1mm">
                                <fo:block text-align="center">${glAcctBalanceByCostCenter.balance!!}</fo:block>
                            </fo:table-cell>
                            <#list glAccountCategories as glAccountCategory>
                                <fo:table-cell border="1pt solid" border-width=".1mm">
                                    <fo:block text-align="center">${(glAcctBalanceByCostCenter[glAccountCategory.glAccountCategoryId!]!)}</fo:block>
                                </fo:table-cell>
                            </#list>
                        </fo:table-row>
                    </#if>
                </#list>
            </fo:table-body>
        </fo:table>
    <#else>
        <fo:block text-align="center">${uiLabelMap.CommonNoRecordFound}</fo:block>
    </#if>
</#escape>
