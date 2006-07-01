<#--
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
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
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@version    $Rev$
 *@since      2.1
-->

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">${uiLabelMap.QuoteHistory}</div>
    </div>
    <div class="screenlet-body">
        <table width="100%" cellpadding="1" cellspacing="0" border="0">
            <tr>
                <td width="10%">
                    <div class="tabletext"><b><nobr>${uiLabelMap.OrderQuote} ${uiLabelMap.OrderNbr}</nobr></b></div>
                </td>
                <td width="10">&nbsp;</td>
                <td width="20%">
                    <div class="tabletext"><b>${uiLabelMap.CommonName}</b></div>
                </td>
                <td width="10">&nbsp;</td>
                <td width="40%">
                    <div class="tabletext"><b>${uiLabelMap.CommonDescription}</b></div>
                </td>
                <td width="10">&nbsp;</td>
                <td width="10%">
                    <div class="tabletext"><b>${uiLabelMap.CommonStatus}</b></div>
                </td>
                <td width="10">&nbsp;</td>
                <td width="20%">
                    <div class="tabletext"><b>${uiLabelMap.OrderOrderQuoteIssueDate}</b></div>
                    <div class="tabletext"><b>${uiLabelMap.CommonValidFromDate}</b></div>
                    <div class="tabletext"><b>${uiLabelMap.CommonValidThruDate}</b></div>
                </td>
                <td width="10">&nbsp;</td>
                <td width="10">&nbsp;</td>
            </tr>
            <#list quoteList as quote>
                <#assign status = quote.getRelatedOneCache("StatusItem")>                               
                <tr><td colspan="12"><hr class="sepbar"/></td></tr>
                <tr>
                    <td>
                        <div class="tabletext">${quote.quoteId}</div>
                    </td>
                    <td width="10">&nbsp;</td>
                    <td>
                        <div class="tabletext">${quote.quoteName?if_exists}</div>
                    </td>
                    <td width="10">&nbsp;</td>
                    <td>
                        <div class="tabletext">${quote.description?if_exists}</div>
                    </td>
                    <td width="10">&nbsp;</td>
                    <td>
                        <div class="tabletext">${status.get("description",locale)}</div>
                    </td>
                    <td width="10">&nbsp;</td>
                    <td>
                        <div class="tabletext"><nobr>${quote.issueDate?if_exists}</nobr></div>
                        <div class="tabletext"><nobr>${quote.validFromDate?if_exists}</nobr></div>
                        <div class="tabletext"><nobr>${quote.validThruDate?if_exists}</nobr></div>
                    </td>
                    <td width="10">&nbsp;</td>
                    <td align="right">
                        <a href="<@ofbizUrl>ViewQuote?quoteId=${quote.quoteId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonView}</a>
                    </td>
                    <td width="10">&nbsp;</td>
                </tr>
            </#list>
            <#if !quoteList?has_content>
                <tr><td colspan="9"><div class="head3">${uiLabelMap.OrderNoQuoteFound}</div></td></tr>
            </#if>
        </table>
    </div>
</div>
