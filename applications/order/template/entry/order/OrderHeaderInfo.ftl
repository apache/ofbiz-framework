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
        <table width="100%" border="0" cellpadding="1">
        <#-- order name -->
        <#if (orderName?has_content)>
            <tr>
                <td align="right" valign="top" width="15%">
                    <span>&nbsp;<b>${uiLabelMap.OrderOrderName}</b> </span>
                </td>
                <td width="5">&nbsp;</td>
                <td valign="top" width="80%">
                    ${orderName}
                </td>
            </tr>
            <tr><td colspan="7"><hr /></td></tr>
        </#if>
        <#-- order for party -->
        <#if (orderForParty??)>
            <tr>
                <td align="right" valign="top" width="15%">
                    <span>&nbsp;<b>${uiLabelMap.OrderOrderFor}</b> </span>
                </td>
                <td width="5">&nbsp;</td>
                <td valign="top" width="80%">
                    ${Static["org.apache.ofbiz.party.party.PartyHelper"].getPartyName(orderForParty, false)} [${orderForParty.partyId}]
                </td>
            </tr>
            <tr><td colspan="7"><hr /></td></tr>
        </#if>
        <#if (cart.getPoNumber()?has_content)>
            <tr>
                <td align="right" valign="top" width="15%">
                    <span>&nbsp;<b>${uiLabelMap.OrderPONumber}</b> </span>
                </td>
                <td width="5">&nbsp;</td>
                <td valign="top" width="80%">
                    ${cart.getPoNumber()}
                </td>
            </tr>
            <tr><td colspan="7"><hr /></td></tr>
        </#if>
        <#if orderTerms?has_content>
            <tr>
                <td align="right" valign="top" width="15%">
                    <div>&nbsp;<b>${uiLabelMap.OrderOrderTerms}</b></div>
                </td>
                <td width="5">&nbsp;</td>
                <td valign="top" width="80%">
                    <table>
                        <tr>
                            <td width="35%"><div><b>${uiLabelMap.OrderOrderTermType}</b></div></td>
                            <td width="10%"><div><b>${uiLabelMap.OrderOrderTermValue}</b></div></td>
                            <td width="10%"><div><b>${uiLabelMap.OrderOrderTermDays}</b></div></td>
                            <td width="10%"><div><b>${uiLabelMap.OrderOrderTextValue}</b></div></td>
                            <td width="35%"><div><b>${uiLabelMap.CommonDescription}</b></div></td>
                        </tr>
                        <tr><td colspan="4"><hr /></td></tr>
                        <#assign index=0/>
                        <#list orderTerms as orderTerm>
                        <tr>
                            <td width="35%"><div>${orderTerm.getRelatedOne("TermType", false).get("description",locale)}</div></td>
                            <td width="10%"><div>${orderTerm.termValue?default("")}</div></td>
                            <td width="10%"><div>${orderTerm.termDays?default("")}</div></td>
                            <td width="10%"><div>${orderTerm.textValue?default("")}</div></td>
                            <td width="35%"><div>${orderTerm.description?default("")}</div></td>
                        </tr>
                            <#if orderTerms.size()&lt;index>
                        <tr><td colspan="5"><hr /></td></tr>
                            </#if>
                            <#assign index=index+1/>
                        </#list>
                    </table>
                </td>
            </tr>
            <tr><td colspan="7"><hr /></td></tr>
        </#if>
        <#-- tracking number -->
        <#if trackingNumber?has_content>
            <tr>
                <td align="right" valign="top" width="15%">
                    <div>&nbsp;<b>${uiLabelMap.OrderTrackingNumber}</b></div>
                </td>
                <td width="5">&nbsp;</td>
                <td valign="top" width="80%">
                    <#-- TODO: add links to UPS/FEDEX/etc based on carrier partyId  -->
                    <div>${trackingNumber}</div>
                </td>
            </tr>
            <tr><td colspan="7"><hr /></td></tr>
        </#if>
        <#-- splitting preference -->
            <tr>
                <td align="right" valign="top" width="15%">
                    <div>&nbsp;<b>${uiLabelMap.OrderSplittingPreference}</b></div>
                </td>
                <td width="5">&nbsp;</td>
                <td valign="top" width="80%">
                    <div>
                        <#if "N" == maySplit?default("N")>${uiLabelMap.FacilityWaitEntireOrderReady}</#if>
                        <#if "Y" == maySplit?default("Y")>${uiLabelMap.FacilityShipAvailable}</#if>
                    </div>
                </td>
            </tr>
        <#-- shipping instructions -->
        <#if shippingInstructions?has_content>
            <tr><td colspan="7"><hr /></td></tr>
            <tr>
                <td align="right" valign="top" width="15%">
                    <div>&nbsp;<b>${uiLabelMap.OrderSpecialInstructions}</b></div>
                </td>
                <td width="5">&nbsp;</td>
                <td valign="top" width="80%">
                    <div>${shippingInstructions}</div>
                </td>
            </tr>
        </#if>
            <tr><td colspan="7"><hr /></td></tr>
        <#if orderType != "PURCHASE_ORDER" && (productStore.showCheckoutGiftOptions)! != "N">
        <#-- gift settings -->
            <tr>
                <td align="right" valign="top" width="15%">
                    <div>&nbsp;<b>${uiLabelMap.OrderGift}</b></div>
                </td>
                <td width="5">&nbsp;</td>
                <td valign="top" width="80%">
                    <div>
                        <#if "N" == isGift?default("N")>${uiLabelMap.OrderThisOrderNotGift}</#if>
                        <#if "Y" == isGift?default("N")>${uiLabelMap.OrderThisOrderGift}</#if>
                    </div>
                </td>
            </tr>
            <tr><td colspan="7"><hr /></td></tr>
            <#if giftMessage?has_content>
            <tr>
                <td align="right" valign="top" width="15%">
                    <div>&nbsp;<b>${uiLabelMap.OrderGiftMessage}</b></div>
                </td>
                <td width="5">&nbsp;</td>
                <td valign="top" width="80%">
                    <div>${giftMessage}</div>
                </td>
            </tr>
            <tr><td colspan="7"><hr /></td></tr>
            </#if>
        </#if>
        <#if shipAfterDate?has_content>
            <tr>
                <td align="right" valign="top" width="15%">
                    <div>&nbsp;<b>${uiLabelMap.OrderShipAfterDate}</b></div>
                </td>
                <td width="5">&nbsp;</td>
                <td valign="top" width="80%">
                    <div>${shipAfterDate}</div>
                </td>
            </tr>
        </#if>
        <#if shipBeforeDate?has_content>
            <tr>
                <td align="right" valign="top" width="15%">
                    <div>&nbsp;<b>${uiLabelMap.OrderShipBeforeDate}</b></div>
                </td>
                <td width="5">&nbsp;</td>
                <td valign="top" width="80%">
                  <div>${shipBeforeDate}</div>
                </td>
            </tr>
        </#if>
        </table>
    </div>
</div>
