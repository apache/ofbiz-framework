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
        <div class="h3">${uiLabelMap.OrderOrderQuoteId}&nbsp;${quote.quoteId}&nbsp;${uiLabelMap.CommonInformation}</div>
    </div>
    <div class="screenlet-body">
        <table cellspacing="0" class="basic-table">
            <#-- quote header information -->
            <tr>
                <td align="right" valign="top" width="15%" class="label">
                    &nbsp;${uiLabelMap.CommonType}
                </td>
                <td width="5%">&nbsp;</td>
                <td valign="top" width="80%">
                    ${(quoteType.get("description",locale))?default(quote.quoteTypeId!)}
                </td>
            </tr>
            <tr><td colspan="3"><hr /></td></tr>

            <#-- quote Channel information -->
            <tr>
                <td align="right" valign="top" width="15%" class="label">
                    &nbsp;${uiLabelMap.OrderSalesChannel}
                </td>
                <td width="5%">&nbsp;</td>
                <td valign="top" width="80%">
                    ${(salesChannel.get("description",locale))?default(quote.salesChannelEnumId!)}
                </td>
            </tr>
            <tr><td colspan="3"><hr /></td></tr>

            <#-- quote status information -->
            <tr>
                <td align="right" valign="top" width="15%" class="label">
                    &nbsp;${uiLabelMap.CommonStatus}
                </td>
                <td width="5%">&nbsp;</td>
                <td valign="top" width="80%">
                    ${(statusItem.get("description", locale))?default(quote.statusId!)}
                </td>
            </tr>
            <#-- party -->
            <tr><td colspan="3"><hr /></td></tr>
            <tr>
                <td align="right" valign="top" width="15%" class="label">
                    &nbsp;${uiLabelMap.PartyParty}
                </td>
                <td width="5%">&nbsp;</td>
                <td valign="top" width="80%">
                    <#-- Party name, as a convenience. -->
                    <#if toParty?has_content>
                        <#if toParty.groupName?has_content>
                            ${toParty.groupName}
                        <#elseif toParty.firstName?has_content && toParty.lastName?has_content>
                            ${toParty.firstName} ${toParty.lastName}
                        </#if>
                    </#if>
                    <#-- Link to the party in Party Manager -->
                    <#if quote.partyId?has_content><a href="<@ofbizUrl controlPath="/partymgr/control">viewprofile?partyId=${quote.partyId}</@ofbizUrl>" class="buttontext" target="_blank">(${quote.partyId})</a></#if>
                </td>
            </tr>
            <#-- quote name -->
            <tr><td colspan="3"><hr /></td></tr>
            <tr>
                <td align="right" valign="top" width="15%" class="label">
                    &nbsp;${uiLabelMap.OrderOrderQuoteName}
                </td>
                <td width="5%">&nbsp;</td>
                <td valign="top" width="80%">
                    ${quote.quoteName!}
                </td>
            </tr>
            <#-- quote description -->
            <tr><td colspan="3"><hr /></td></tr>
            <tr>
                <td align="right" valign="top" width="15%" class="label">
                    &nbsp;${uiLabelMap.CommonDescription}
                </td>
                <td width="5%">&nbsp;</td>
                <td valign="top" width="80%">
                    ${quote.description!}
                </td>
            </tr>
            <#-- quote currency -->
            <tr><td colspan="3"><hr /></td></tr>
            <tr>
                <td align="right" valign="top" width="15%" class="label">
                    &nbsp;${uiLabelMap.CommonCurrency}
                </td>
                <td width="5%">&nbsp;</td>
                <td valign="top" width="80%">
                    <#if currency??>${currency.get("description",locale)?default(quote.currencyUomId!)}</#if>
                </td>
            </tr>
            <#-- quote currency -->
            <tr><td colspan="3"><hr /></td></tr>
            <tr>
                <td align="right" valign="top" width="15%" class="label">
                    &nbsp;${uiLabelMap.ProductProductStore}
                </td>
                <td width="5%">&nbsp;</td>
                <td valign="top" width="80%">
                    <#if store??>${store.storeName?default(quote.productStoreId!)}</#if>
                </td>
            </tr>
        </table>
    </div>
</div>
