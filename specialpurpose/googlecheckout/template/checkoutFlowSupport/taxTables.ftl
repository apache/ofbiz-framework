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

<tax-tables merchant-calculated=${flowSupport.taxTables.merchantCalculated?default("false")}>
    <default-tax-table>
        <tax-rules>
            <#list flowSupport.taxTables.default.rules as rule>
            <default-tax-rule>
                <#if rule.shippingTaxed??>
                <shipping-taxed>${rule.shippingTaxed}</shipping-taxed>
                </#if>
                <rate>${rule.rate}</rate>
                <tax-area>
                    <#if rule.areaType="states">
                    <#if rule.allowedAreas??>
                    <allowed-areas>
                        <us-state-area>
                        <#list rule.allowedAreas as allowed>
                            <state>${allowed}</state>
                        </#list>
                        </us-state-area>
                    </allowed-areas>
                    </#if>
                    <#if rule.excludedAreas??>
                    <excluded-areas>
                        <us-state-area>
                        <#list rule.excludedAreas as excluded>
                            <state>${excluded}</state>
                        </#list>
                        </us-state-area>
                    </excluded-areas>
                    </#if>
                    <#elseif rule.areaType="zips">
                    <#if rule.allowedAreas??>
                    <allowed-areas>
                        <us-zip-area>
                        <#list rule.allowedAreas as allowed>
                            <zip-pattern>${allowed}</zip-pattern>
                        </#list>
                        </us-zip-area>
                    </allowed-areas>
                    </#if>
                    <#if rule.excludedAreas??>
                    <excluded-areas>
                        <us-zip-area>
                        <#list rule.excludedAreas as excluded>
                            <zip-pattern>${excluded}</zip-pattern>
                        </#list>
                        </us-zip-area>
                    </excluded-areas>
                    </#if>
                    <#elseif rule.areaType="country">
                    <us-country-area country-area="${rule.country}"/>
                    </#if>
                <tax-area>
            </default-tax-rule>
            </#list>
        </tax-rules>
    </default-tax-table>
    <#if flowSupport.taxTables.alternateTaxTables??>
    <alternate-tax-tables>
        <#list flowSupport.taxTables.alternateTaxTables as altTaxTable>
        <alternate-tax-table name="${altTaxTable.name}" standalone="${altTaxTable.standalone}">
            <alternate-tax-rules>
                <#list altTaxTable.rules as altRule>
                <alternate-tax-rule>
                    <rate>${altRule.rate}</rate>
                    <tax-area>
                        <#if altRule.areaType="states">
                        <#if altRule.allowedAreas??>
                        <allowed-areas>
                            <us-state-area>
                                <#list altRule.allowedAreas as altAllowed>
                                <state>${altAllowed}</state>
                                </#list>
                            </us-state-area>
                        </allowed-areas>
                        </#if>
                        <#if altRule.excludedAreas??>
                        <excluded-areas>
                            <us-state-area>
                                <#list altRule.excludedAreas as altExcluded>
                                <state>${altExcluded}</state>
                                </#list>
                            </us-state-area>
                        </excluded-areas>
                        </#if>
                        <#elseif rule.areaType="zips">
                        <#if altRule.allowedAreas??>
                        <allowed-areas>
                            <us-zip-area>
                                <#list altRule.allowedAreas as altAllowed>
                                <zip-pattern>${altAllowed}</zip-pattern>
                                </#list>
                            </us-zip-area>
                        </allowed-areas>
                        </#if>
                        <#if altRule.excludedAreas??>
                        <excluded-areas>
                            <us-zip-area>
                                <#list altRule.excludedAreas as altExcluded>
                                <zip-pattern>${altExcluded}</zip-pattern>
                                </#list>
                            </us-zip-area>
                        </excluded-areas>
                        </#if>
                        <#elseif altRule.areaType="country">
                        <us-country-area country-area="${altRule.country}"/>
                        </#if>
                    </tax-area>
                </alternate-tax-rule>
                </#list>
            </alternate-tax-rules>
        </alternate-tax-table>
        </#list>
    </alternate-tax-tables>
    </#if>
</tax-tables>
