<#--
$Id: $

Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->
<#assign unselectedClassName = "tabButton">
<#assign selectedClassMap = {page.tabButtonItem?default("void") : "tabButtonSelected"}>

<#if agreement?has_content>
    <div class="tabContainer">
        <a href="<@ofbizUrl>EditAgreement?agreementId=${agreement.agreementId}</@ofbizUrl>" class="${selectedClassMap.EditAgreement?default(unselectedClassName)}">${uiLabelMap.AccountingAgreement}</a>
        <a href="<@ofbizUrl>EditAgreementTerms?agreementId=${agreement.agreementId}</@ofbizUrl>" class="${selectedClassMap.EditAgreementTerms?default(unselectedClassName)}">${uiLabelMap.AccountingAgreementTerms}</a>
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

