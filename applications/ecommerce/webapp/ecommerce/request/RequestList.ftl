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
        <div class="boxhead">${uiLabelMap.RequestHistory}</div>
    </div>
    <div class="screenlet-body">
        <table width="100%" cellpadding="1" cellspacing="0" border="0">
            <tr>
                <td width="10%">
                    <div class="tabletext"><b><nobr>${uiLabelMap.OrderRequest} ${uiLabelMap.OrderNbr}</nobr></b></div>
                </td>
                <td width="10">&nbsp;</td>
                <td width="10%">
                    <div class="tabletext"><b><nobr>${uiLabelMap.CommonType}</nobr></b></div>
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
                    <div class="tabletext"><b>${uiLabelMap.OrderRequestDate}</b></div>
                    <div class="tabletext"><b>${uiLabelMap.OrderRequestCreatedDate}</b></div>
                    <div class="tabletext"><b>${uiLabelMap.OrderRequestLastModifiedDate}</b></div>
                </td>
                <td width="10">&nbsp;</td>
                <td width="10">&nbsp;</td>
            </tr>
            <#list requestList as custRequest>
                <#assign status = custRequest.getRelatedOneCache("StatusItem")>
                <#assign type = custRequest.getRelatedOneCache("CustRequestType")>
                <tr><td colspan="14"><hr class="sepbar"/></td></tr>
                <tr>
                    <td>
                        <div class="tabletext">${custRequest.custRequestId}</div>
                    </td>
                    <td width="10">&nbsp;</td>
                    <td>
                        <div class="tabletext">${type.get("description",locale)?if_exists}</div>
                    </td>
                    <td width="10">&nbsp;</td>
                    <td>
                        <div class="tabletext">${custRequest.custRequestName?if_exists}</div>
                    </td>
                    <td width="10">&nbsp;</td>
                    <td>
                        <div class="tabletext">${custRequest.description?if_exists}</div>
                    </td>
                    <td width="10">&nbsp;</td>
                    <td>
                        <div class="tabletext">${status.get("description",locale)}</div>
                    </td>
                    <td width="10">&nbsp;</td>
                    <td>
                        <div class="tabletext"><nobr>${custRequest.custRequestDate?if_exists}</nobr></div>
                        <div class="tabletext"><nobr>${custRequest.createdDate?if_exists}</nobr></div>
                        <div class="tabletext"><nobr>${custRequest.lastModifiedDate?if_exists}</nobr></div>
                    </td>
                    <td width="10">&nbsp;</td>
                    <td align="right">
                        <a href="<@ofbizUrl>/ViewRequest?custRequestId=${custRequest.custRequestId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonView}</a>
                    </td>
                    <td width="10">&nbsp;</td>
                </tr>
            </#list>
            <#if !requestList?has_content>
                <tr><td colspan="9"><div class="head3">${uiLabelMap.OrderNoRequestFound}</div></td></tr>
            </#if>
        </table>
    </div>
</div>
