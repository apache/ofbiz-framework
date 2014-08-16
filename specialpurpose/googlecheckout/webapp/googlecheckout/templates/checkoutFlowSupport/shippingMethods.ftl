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

<#--TODO: merchant-calculated-shipping cannot be mixed with flat-shipping or pickup, change accordingly -->

<#if flowSupport.shippingMethods??>
<shipping-methods>
<#list flowSupport.shippingMethods as shippingMethod>
    <#if shippingMethod.type="flatRate">
    <flat-rate-shipping name="${shippingMethod.name}">
        <price currency="USD">${shippingMethod.price}</price>
        <#if shippingMethod.restrictions??>
        <#list shippingMethod.restrictions as restriction>
        <shipping-restrictions>
            <#if restriction.allowed??>
            <allowed-areas>
                <#list resitriction.allowed as allow>
                <#if allow.type="state">
                <us-state-area>
                    <#list allow.areas as area>
                    <state>${area}</state>
                    </#list>
                </us-state-area>
                <#elseif allow.type="zip">
                <us-zip-area>
                    <#list allow.areas as area>
                    <zip-pattern>${area}</zip-pattern>
                    </#list>
                </us-zip-area>
                <#elseif allow.type="country">
                <us-country-area country-area="<#list allow.area as area>${area}</#list>"/>
                </#if>
                </#list>
            </allowed-areas>
            </#if>
            <#if restriction.excluded??>
            <excluded-areas>
                <#list resitriction.excluded as exclude>
                <#if exclude.type="state">
                <us-state-area>
                    <#list exclude.areas as area>
                    <state>${area}</state>
                    </#list>
                </us-state-area>
                <#elseif exclude.type="zip">
                <us-zip-area>
                    <#list exclude.areas as area>
                    <zip-pattern>${area}</zip-pattern>
                    </#list>
                </us-zip-area>
                <#elseif exclude.type="country">
                <us-country-area country-area="<#list exclude.area as area>${area}</#list>"/>
                </#if>
                </#list>
            </excludedd-areas>
            </#if>
        </shipping-restrictions>
        </#list>
        </#if>
    </flat-rate-shipping>
    <#elseif shippingMethod.type="merchantCalculated">
    <merchant-calculated-shipping name="${shippingMethod.name}">
        <price currency="USD">${shippingMethod.price}</price>
        <#if shippingMethod.restrictions??>
        <#list shippingMethod.restrictions as restriction>
        <shipping-restrictions>
            <#if restriction.allowed??>
            <allowed-areas>
                <#list resitriction.allowed as allow>
                <#if allow.type="state">
                <us-state-area>
                    <#list allow.areas as area>
                    <state>${area}</state>
                    </#list>
                </us-state-area>
                <#elseif allow.type="zip">
                <us-zip-area>
                    <#list allow.areas as area>
                    <zip-pattern>${area}</zip-pattern>
                    </#list>
                </us-zip-area>
                <#elseif allow.type="country">
                <us-country-area country-area="<#list allow.area as area>${area}</#list>"/>
                </#if>
                </#list>
            </allowed-areas>
            </#if>
            <#if restriction.excluded??>
            <excluded-areas>
                <#list resitriction.excluded as exclude>
                <#if exclude.type="state">
                <us-state-area>
                    <#list exclude.areas as area>
                    <state>${area}</state>
                    </#list>
                </us-state-area>
                <#elseif exclude.type="zip">
                <us-zip-area>
                    <#list exclude.areas as area>
                    <zip-pattern>${area}</zip-pattern>
                    </#list>
                </us-zip-area>
                <#elseif exclude.type="country">
                <us-country-area country-area="<#list exclude.area as area>${area}</#list>"/>
                </#if>
                </#list>
            </excludedd-areas>
            </#if>
        </shipping-restrictions>
        </#list>
        </#if>

    </merchant-calculated-shipping>
    <#elseif shippingMethod.type="pickup">
    <pickup name="${shippingMethod.name}">
        <price currency="USD">${shippingMethod.price}</price>
    </pickup>
    </#if>
</#list>
</shipping-methods>
</#if>
