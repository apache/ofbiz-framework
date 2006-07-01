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
 *@author     David E. Jones (jonesde@ofbiz.org)
 *@version    $Rev$
 *@since      2.2
-->

<#assign unselectedClassName = "tabButton">
<#assign selectedClassMap = {page.tabButtonItem?default("void") : "tabButtonSelected"}>

<#if agreement?has_content>
    <div class="tabContainer">
        <a href="<@ofbizUrl>EditAgreement?agreementId=${agreement.agreementId}</@ofbizUrl>" class="${selectedClassMap.EditAgreement?default(unselectedClassName)}">${uiLabelMap.AccountingAgreement}</a>
        <a href="<@ofbizUrl>ListAgreementTerms?agreementId=${agreement.agreementId}</@ofbizUrl>" class="${selectedClassMap.ListAgreementTerms?default(unselectedClassName)}">${uiLabelMap.AccountingAgreementTerms}</a>
        <a href="<@ofbizUrl>ListAgreementItems?agreementId=${agreement.agreementId}</@ofbizUrl>" class="${selectedClassMap.ListAgreementItems?default(unselectedClassName)}">${uiLabelMap.AccountingAgreementItems}</a>
    </div>
</#if>
<#if agreementItem?has_content>
    <div class="tabContainer">
        <a href="<@ofbizUrl>EditAgreementItem?agreementId=${agreementItem.agreementId}&agreementItemSeqId=${agreementItem.agreementItemSeqId}</@ofbizUrl>" class="${selectedClassMap.EditAgreementItem?default(unselectedClassName)}">${uiLabelMap.AccountingAgreementItem}</a>
        <a href="<@ofbizUrl>ListAgreementPromoAppls?agreementId=${agreementItem.agreementId}&agreementItemSeqId=${agreementItem.agreementItemSeqId}</@ofbizUrl>" class="${selectedClassMap.ListAgreementPromoAppls?default(unselectedClassName)}">${uiLabelMap.AccountingAgreementPromoAppls}</a>
        <a href="<@ofbizUrl>ListAgreementItemTerms?agreementId=${agreementItem.agreementId}&agreementItemSeqId=${agreementItem.agreementItemSeqId}</@ofbizUrl>" class="${selectedClassMap.ListAgreementItemTerms?default(unselectedClassName)}">${uiLabelMap.AccountingAgreementItemTerms}</a>
        <a href="<@ofbizUrl>ListAgreementItemProducts?agreementId=${agreementItem.agreementId}&agreementItemSeqId=${agreementItem.agreementItemSeqId}</@ofbizUrl>" class="${selectedClassMap.ListAgreementItemProducts?default(unselectedClassName)}">${uiLabelMap.ProductProducts}</a>
        <a href="<@ofbizUrl>ListAgreementItemParties?agreementId=${agreementItem.agreementId}&agreementItemSeqId=${agreementItem.agreementItemSeqId}</@ofbizUrl>" class="${selectedClassMap.ListAgreementItemParties?default(unselectedClassName)}">${uiLabelMap.Party}</a>
    </div>
</#if>

