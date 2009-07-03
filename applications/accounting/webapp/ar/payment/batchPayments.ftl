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
    <div class="screenlet-body">
        <form name='batchPaymentForm'>
            <table class="basic-table">
                <tr class="header-row">
                    <td>${uiLabelMap.FormFieldTitle_paymentId}</td>
                    <td>${uiLabelMap.Party}</td>
                    <td>${uiLabelMap.CommonAmount}</td>
                    <td>${uiLabelMap.CommonDate}</td>
                </tr>
                <#if paymentList?has_content>
                    <#list paymentList as payment>
                        <tr>
                            <td><a href="<@ofbizUrl>paymentOverview?paymentId=${payment.paymentId}</@ofbizUrl>">${payment.paymentId}</a></td>
                            <td>
                                <#assign partyName = (delegator.findOne("PartyNameView", {"partyId" : payment.partyIdFrom}, false))!>
                                <#if partyName.partyTypeId == "PERSON">
                                    ${(partyName.firstName)!} ${(partyName.lastName)!}
                                <#elseif (partyName.partyTypeId)! == "PARTY_GROUP">
                                    ${(partyName.groupName)!}
                                </#if>
                            </td>
                            <td>${payment.amount?if_exists}</td>
                            <td>${payment.effectiveDate?if_exists}</td>
                        </tr>
                    </#list>
                </#if>
            </table>
        </form>
    </div>
</div>