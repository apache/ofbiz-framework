<#--
 *  Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a 
 *  copy of this software and associated documentation files (the "Software"), 
 *  to deal in the Software without restriction, including without limitation 
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 *  and/or sell copies of the Software, and to permit persons to whom the 
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included 
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT 
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     Jacopo Cappellato (tiz@sastau.it)
 *@version    $Rev$
-->

<#if quote?exists>
<#if note?exists><p class="tabletext">${note}</p></#if>
<table width="100%" border="0" cellpadding="0" cellspacing="0">
    <tr>
        <#-- left side -->
        <td width="50%" valign="top" align="left">

            <div class="screenlet">
                <div class="screenlet-header">
                    <div class="boxhead">${uiLabelMap.OrderOrderQuoteId}&nbsp;${quote.quoteId}&nbsp;${uiLabelMap.CommonInformation}</div>
                </div>
                <div class="screenlet-body">
                    <table width="100%" border="0" cellpadding="1">
                        <#-- quote header information -->
                        <tr>
                            <td align="right" valign="top" width="15%">
                                <div class="tabletext">&nbsp;<b>${uiLabelMap.CommonType}</b></div>
                            </td>
                            <td width="5">&nbsp;</td>
                            <td align="left" valign="top" width="80%">
                                <div class="tabletext">${(quoteType.get("description",locale))?default(quote.quoteTypeId?if_exists)}</div>
                            </td>
                        </tr>
                        <tr><td colspan="7"><hr class="sepbar"/></td></tr>
                        <#-- quote status information -->
                        <tr>
                            <td align="right" valign="top" width="15%">
                                <div class="tabletext">&nbsp;<b>${uiLabelMap.CommonStatus}</b></div>
                            </td>
                            <td width="5">&nbsp;</td>
                            <td align="left" valign="top" width="80%">
                                <div class="tabletext">${(statusItem.get("description", locale))?default(quote.statusId?if_exists)}</div>
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
                                <div class="tabletext">${quote.partyId?if_exists}</div>
                            </td>
                        </tr>
                        <#-- quote name -->
                        <tr><td colspan="7"><hr class="sepbar"/></td></tr>
                        <tr>
                            <td align="right" valign="top" width="15%">
                                <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderOrderQuoteName}</b></div>
                            </td>
                            <td width="5">&nbsp;</td>
                            <td align="left" valign="top" width="80%">
                                <div class="tabletext">${quote.quoteName?if_exists}</div>
                            </td>
                        </tr>
                        <#-- quote description -->
                        <tr><td colspan="7"><hr class="sepbar"/></td></tr>
                        <tr>
                            <td align="right" valign="top" width="15%">
                                <div class="tabletext">&nbsp;<b>${uiLabelMap.CommonDescription}</b></div>
                            </td>
                            <td width="5">&nbsp;</td>
                            <td align="left" valign="top" width="80%">
                                <div class="tabletext">${quote.description?if_exists}</div>
                            </td>
                        </tr>
                        <#-- quote currency -->
                        <tr><td colspan="7"><hr class="sepbar"/></td></tr>
                        <tr>
                            <td align="right" valign="top" width="15%">
                                <div class="tabletext">&nbsp;<b>${uiLabelMap.CommonCurrency}</b></div>
                            </td>
                            <td width="5">&nbsp;</td>
                            <td align="left" valign="top" width="80%">
                                <div class="tabletext"><#if currency?exists>${currency.get("description",locale)?default(quote.currencyUomId?if_exists)}</#if></div>
                            </td>
                        </tr>
                        <#-- quote currency -->
                        <tr><td colspan="7"><hr class="sepbar"/></td></tr>
                        <tr>
                            <td align="right" valign="top" width="15%">
                                <div class="tabletext">&nbsp;<b>${uiLabelMap.ProductProductStore}</b></div>
                            </td>
                            <td width="5">&nbsp;</td>
                            <td align="left" valign="top" width="80%">
                                <div class="tabletext"><#if store?exists>${store.storeName?default(quote.productStoreId?if_exists)}</#if></div>
                            </td>
                        </tr>
                        
                    </table>
                </div>
            </div>
        </td>

        <td bgcolor="white" width="1">&nbsp;&nbsp;</td>
        <#-- right side -->

        <td width="50%" valign="top" align="left">
            <div class="screenlet">
                <div class="screenlet-header">
                    <div class="boxhead">&nbsp;${uiLabelMap.CommonDate}</div>
                </div>
                <div class="screenlet-body">
                    <table width="100%" border="0" cellpadding="1">
                        <tr>
                            <td align="right" valign="top" width="15%">
                                <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderOrderQuoteIssueDate}</b></div>
                            </td>
                            <td width="5">&nbsp;</td>
                            <td align="left" valign="top" width="80%">
                                <div class="tabletext">${(quote.issueDate.toString())?if_exists}</div>
                            </td>
                        </tr>
                        <tr><td colspan="7"><hr class="sepbar"/></td></tr>
                        <tr>
                            <td align="right" valign="top" width="15%">
                                <div class="tabletext">&nbsp;<b>${uiLabelMap.CommonValidFromDate}</b></div>
                            </td>
                            <td width="5">&nbsp;</td>
                            <td align="left" valign="top" width="80%">
                                <div class="tabletext">${(quote.validFromDate.toString())?if_exists}</div>
                            </td>
                        </tr>
                        <tr><td colspan="7"><hr class="sepbar"/></td></tr>
                        <tr>
                            <td align="right" valign="top" width="15%">
                                <div class="tabletext">&nbsp;<b>${uiLabelMap.CommonValidThruDate}</b></div>
                            </td>
                            <td width="5">&nbsp;</td>
                            <td align="left" valign="top" width="80%">
                                <div class="tabletext">${(quote.validThruDate.toString())?if_exists}</div>
                            </td>
                        </tr>
                    </table>
                </div>
            </div>
            <div class="screenlet">
                <div class="screenlet-header">
                    <div class="boxhead">&nbsp;${uiLabelMap.OrderOrderQuoteRoles}</div>
                </div>
                <div class="screenlet-body">
                    <table width="100%" border="0" cellpadding="1">
                        <#list quoteRoles as quoteRole>
                            <#assign roleType = quoteRole.getRelatedOne("RoleType")>
                            <#assign party = quoteRole.getRelatedOne("Party")>
                            <#assign rolePartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", quoteRole.partyId, "compareDate", quote.issueDate, "userLogin", userLogin))/>
                            <tr>
                                <td align="right" valign="top" width="15%">
                                    <div class="tabletext">&nbsp;<b>${roleType.get("description",locale)?if_exists}</b></div>
                                </td>
                                <td width="5">&nbsp;</td>
                                <td align="left" valign="top" width="80%">
                                    <div class="tabletext">${rolePartyNameResult.fullName?default("Name Not Found")}</div>
                                </td>
                            </tr>
                            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
                        </#list>
                    </table>
                </div>
            </div>
        </td>
    </tr>
</table>
</#if>
