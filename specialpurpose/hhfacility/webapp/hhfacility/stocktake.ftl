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

<#assign facility = parameters.facility>
<#if parameters.idValue?if_exists?has_content>
    <#assign idValue = parameters.idValue?has_content>
</#if>

<#if idValue?has_content && !productList?has_content>
    <span class="tabletext">No Product(s) Found</span>
</#if>

<#if productList?has_content>
    <span class="tabletext">Products Found:</span>
    <table>
        <#list productList as product>
            <tr>
                <td>
                    <div class="tabletext"><b>${product.productId}</b></div>
                </td>
                <td>&nbsp;&nbsp;</td>
                <td>
                    <a href="<@ofbizUrl>productstocktake?facilityId=${facility.facilityId?if_exists}&amp;productId=${product.productId}</@ofbizUrl>" class="buttontext">${(product.internalName)?if_exists}</a>
                </td>
            </tr>
        </#list>
    </table>
</#if>


