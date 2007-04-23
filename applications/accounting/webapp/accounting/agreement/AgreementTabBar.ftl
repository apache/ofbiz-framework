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
<#assign selected = page.tabButtonItem?default("void")>

<#if agreement?has_content>
  <div class="button-bar button-style-1">
    <ul>
      <li<#if selected == "EditAgreement"> class="selected"</#if>><a href="<@ofbizUrl>EditAgreement?agreementId=${agreement.agreementId}</@ofbizUrl>">${uiLabelMap.AccountingAgreement}</a></li>
      <li<#if selected == "EditAgreementTerms"> class="selected"</#if>><a href="<@ofbizUrl>EditAgreementTerms?agreementId=${agreement.agreementId}</@ofbizUrl>">${uiLabelMap.AccountingAgreementTerms}</a></li>
      <li<#if selected == "ListAgreementItems"> class="selected"</#if>><a href="<@ofbizUrl>ListAgreementItems?agreementId=${agreement.agreementId}</@ofbizUrl>">${uiLabelMap.AccountingAgreementItems}</a></li>
    </ul>
    <br class="clear"/>
  </div>
</#if>
<#if agreementItem?has_content>
  <div class="button-bar button-style-1">
    <ul>
      <li<#if selected == "EditAgreementItem"> class="selected"</#if>><a href="<@ofbizUrl>EditAgreementItem?agreementId=${agreementItem.agreementId}&agreementItemSeqId=${agreementItem.agreementItemSeqId}</@ofbizUrl>">${uiLabelMap.AccountingAgreementItem}</a></li>
      <li<#if selected == "ListAgreementPromoAppls"> class="selected"</#if>><a href="<@ofbizUrl>ListAgreementPromoAppls?agreementId=${agreementItem.agreementId}&agreementItemSeqId=${agreementItem.agreementItemSeqId}</@ofbizUrl>">${uiLabelMap.AccountingAgreementPromoAppls}</a></li>
      <li<#if selected == "ListAgreementItemTerms"> class="selected"</#if>><a href="<@ofbizUrl>ListAgreementItemTerms?agreementId=${agreementItem.agreementId}&agreementItemSeqId=${agreementItem.agreementItemSeqId}</@ofbizUrl>">${uiLabelMap.AccountingAgreementItemTerms}</a></li>
      <li<#if selected == "ListAgreementItemProducts"> class="selected"</#if>><a href="<@ofbizUrl>ListAgreementItemProducts?agreementId=${agreementItem.agreementId}&agreementItemSeqId=${agreementItem.agreementItemSeqId}</@ofbizUrl>">${uiLabelMap.ProductProducts}</a></li>
      <li<#if selected == "ListAgreementItemParties"> class="selected"</#if>><a href="<@ofbizUrl>ListAgreementItemParties?agreementId=${agreementItem.agreementId}&agreementItemSeqId=${agreementItem.agreementItemSeqId}</@ofbizUrl>">${uiLabelMap.Party}</a></li>
      <li<#if selected == "ListAgreementGeographicalApplic"> class="selected"</#if>><a href="<@ofbizUrl>ListAgreementGeographicalApplic?agreementId=${agreementItem.agreementId}&agreementItemSeqId=${agreementItem.agreementItemSeqId}</@ofbizUrl>">${uiLabelMap.CommonGeo}</a></li>
    </ul>
    <br class="clear"/>
  </div>
</#if>

