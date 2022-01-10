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

<#if paymentList?has_content>
    <table class="basic-table">
        <tr class="header-row-2">
            <td>${uiLabelMap.CommonPayment}</td>
            <td align="right">${uiLabelMap.CommonAmount}</td>
            <td>${uiLabelMap.CommonDate}</td>
            <td>${uiLabelMap.CommonType}</td>
            <td>${uiLabelMap.CommonFrom}</td>
            <td>${uiLabelMap.CommonTo}</td>
        </tr>
        <#assign alt_row = false>
        <#list paymentList as payment>
            <tr <#if alt_row> class="alternate-row"</#if>>
                <td><a href="<@ofbizUrl>paymentOverview?paymentId=${payment.paymentId}</@ofbizUrl>">${payment.paymentId}</a></td>
                <td align="right"><@ofbizCurrency amount=payment.amount isoCode=payment.currencyUomId/></td>
                <td>${payment.effectiveDate!}</td>
                <td>${payment.paymentTypeDesc!}</td>
                <td>${(payment.partyFromFirstName)!} ${(payment.partyFromLastName)!} ${(payment.partyFromGroupName)!}</td>
                <td>${(payment.partyToFirstName)!} ${(payment.partyToLastName)!} ${(payment.partyToGroupName)!}</td>
            </tr>
            <#assign alt_row = !alt_row>
        </#list>
    </table>
<#else>
    <span class="label">${uiLabelMap.CommonNoRecordFound}</span>
</#if>