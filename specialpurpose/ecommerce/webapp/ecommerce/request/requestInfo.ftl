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
        <div class="h3">${uiLabelMap.OrderRequest}&nbsp;${custRequest.custRequestId}&nbsp;${uiLabelMap.CommonInformation}</div>
    </div>
    <div class="screenlet-body">
        <table cellspacing="0" class="basic-table">
            <#-- request header information -->
            <tr>
                <td align="right" valign="top" width="15%" class="label">
                    &nbsp;${uiLabelMap.CommonType}
                </td>
                <td width="5%">&nbsp;</td>
                <td valign="top" width="80%">
                    ${(custRequestType.get("description",locale))?default(custRequest.custRequestTypeId?if_exists)}
                </td>
            </tr>
            <tr><td colspan="7"><hr/></td></tr>
            <#-- request status information -->
            <tr>
                <td align="right" valign="top" width="15%" class="label">
                    &nbsp;${uiLabelMap.CommonStatus}
                </td>
                <td width="5%">&nbsp;</td>
                <td valign="top" width="80%">
                    ${(statusItem.get("description", locale))?default(custRequest.statusId?if_exists)}
                </td>
            </tr>
            <#-- party -->
            <tr><td colspan="7"><hr/></td></tr>
            <tr>
                 <td align="right" valign="top" width="15%" class="label">
                     &nbsp;${uiLabelMap.PartyPartyId}
                 </td>
                 <td width="5%">&nbsp;</td>
                 <td valign="top" width="80%">
                    ${custRequest.fromPartyId?if_exists}
                 </td>
            </tr>
            <#-- request name -->
            <tr><td colspan="7"><hr/></td></tr>
            <tr>
                <td align="right" valign="top" width="15%" class="label">
                    &nbsp;${uiLabelMap.CommonName}
                </td>
                <td width="5%">&nbsp;</td>
                <td valign="top" width="80%">
                    ${custRequest.custRequestName?if_exists}
                </td>
            </tr>
            <#-- request description -->
            <tr><td colspan="7"><hr/></td></tr>
            <tr>
                <td align="right" valign="top" width="15%" class="label">
                    &nbsp;${uiLabelMap.CommonDescription}
                </td>
                <td width="5%">&nbsp;</td>
                <td valign="top" width="80%">
                    ${custRequest.description?if_exists}
                </td>
            </tr>
            <#-- request currency -->
            <tr><td colspan="7"><hr/></td></tr>
            <tr>
                <td align="right" valign="top" width="15%" class="label">
                    &nbsp;${uiLabelMap.CommonCurrency}
                </td>
                <td width="5%">&nbsp;</td>
                <td valign="top" width="80%">
                    <#if currency?exists>${currency.get("description", locale)?default(custRequest.maximumAmountUomId?if_exists)}</#if>
                </td>
            </tr>
            <#-- request currency -->
            <tr><td colspan="7"><hr/></td></tr>
            <tr>
                <td align="right" valign="top" width="15%" class="label">
                    &nbsp;${uiLabelMap.ProductProductStore}
                </td>
                <td width="5%">&nbsp;</td>
                <td valign="top" width="80%">
                    <#if store?exists>${store.storeName?default(custRequest.productStoreId?if_exists)}</#if>
                </td>
            </tr>
            <#-- request comment -->
            <tr><td colspan="7"><hr/></td></tr>
            <tr>
                <td align="right" valign="top" width="15%" class="label">
                    &nbsp;${uiLabelMap.CommonInternalComment}
                </td>
                <td width="5%">&nbsp;</td>
                <td valign="top" width="80%">
                    ${custRequest.internalComment?if_exists}
                </td>
            </tr>
            <#-- request reason -->
            <tr><td colspan="7"><hr/></td></tr>
            <tr>
                <td align="right" valign="top" width="15%" class="label">
                    &nbsp;${uiLabelMap.CommonReason}
                </td>
                <td width="5%">&nbsp;</td>
                <td valign="top" width="80%">
                    ${custRequest.reason?if_exists}
                </td>
            </tr>
        </table>
    </div>
</div>