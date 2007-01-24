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
    <div class="screenlet-header">
        <div class="boxhead">${uiLabelMap.OrderRequest}&nbsp;${custRequest.custRequestId}&nbsp;${uiLabelMap.CommonInformation}</div>
    </div>
    <div class="screenlet-body">
        <table width="100%" border="0" cellpadding="1">
            <#-- request header information -->
            <tr>
                <td align="right" valign="top" width="15%">
                    <div class="tabletext">&nbsp;<b>${uiLabelMap.CommonType}</b></div>
                </td>
                <td width="5">&nbsp;</td>
                <td align="left" valign="top" width="80%">
                    <div class="tabletext">${(custRequestType.get("description",locale))?default(custRequest.custRequestTypeId?if_exists)}</div>
                </td>
            </tr>
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
            <#-- request status information -->
            <tr>
                <td align="right" valign="top" width="15%">
                    <div class="tabletext">&nbsp;<b>${uiLabelMap.CommonStatus}</b></div>
                </td>
                <td width="5">&nbsp;</td>
                <td align="left" valign="top" width="80%">
                     <div class="tabletext">${(statusItem.get("description", locale))?default(custRequest.statusId?if_exists)}</div>
                </td>
            </tr>
            <#-- party -->
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
            <tr>
                 <td align="right" valign="top" width="15%">
                     <div class="tabletext">&nbsp;<b>${uiLabelMap.PartyPartyId}</b></div>
                 </td>
                 <td width="5">&nbsp;</td>
                 <td align="left" valign="top" width="80%">
                     <div class="tabletext">${custRequest.fromPartyId?if_exists}</div>
                 </td>
            </tr>
            <#-- request name -->
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
            <tr>
                <td align="right" valign="top" width="15%">
                    <div class="tabletext">&nbsp;<b>${uiLabelMap.CommonName}</b></div>
                </td>
                <td width="5">&nbsp;</td>
                <td align="left" valign="top" width="80%">
                    <div class="tabletext">${custRequest.custRequestName?if_exists}</div>
                </td>
            </tr>
            <#-- request description -->
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
            <tr>
                <td align="right" valign="top" width="15%">
                    <div class="tabletext">&nbsp;<b>${uiLabelMap.CommonDescription}</b></div>
                </td>
                <td width="5">&nbsp;</td>
                <td align="left" valign="top" width="80%">
                    <div class="tabletext">${custRequest.description?if_exists}</div>
                </td>
            </tr>
            <#-- request currency -->
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
            <tr>
                <td align="right" valign="top" width="15%">
                    <div class="tabletext">&nbsp;<b>${uiLabelMap.CommonCurrency}</b></div>
                </td>
                <td width="5">&nbsp;</td>
                <td align="left" valign="top" width="80%">
                    <div class="tabletext"><#if currency?exists>${currency.get("description", locale)?default(custRequest.maximumAmountUomId?if_exists)}</#if></div>
                </td>
            </tr>
            <#-- request currency -->
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
            <tr>
                <td align="right" valign="top" width="15%">
                    <div class="tabletext">&nbsp;<b>${uiLabelMap.ProductProductStore}</b></div>
                </td>
                <td width="5">&nbsp;</td>
                <td align="left" valign="top" width="80%">
                    <div class="tabletext"><#if store?exists>${store.storeName?default(custRequest.productStoreId?if_exists)}</#if></div>
                </td>
            </tr>
        </table>
    </div>
</div>
