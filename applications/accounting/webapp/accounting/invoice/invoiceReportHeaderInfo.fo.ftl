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
<#escape x as x?xml>
<fo:table table-layout="fixed" width="100%">
<fo:table-column column-width="1.5in"/>
<fo:table-column column-width="2.5in"/>
<fo:table-body>
<fo:table-row>
  <fo:table-cell>
     <fo:block number-columns-spanned="2" font-weight="bold">${invoice.getRelatedOne("InvoiceType", false).get("description",locale)}</fo:block>
  </fo:table-cell>
</fo:table-row>

<fo:table-row>
  <fo:table-cell><fo:block>${uiLabelMap.AccountingInvoiceDateAbbr}:</fo:block></fo:table-cell>
  <fo:table-cell><fo:block>${invoiceDate!}</fo:block></fo:table-cell>
</fo:table-row>

<fo:table-row>
  <fo:table-cell><fo:block>${uiLabelMap.AccountingCustNr}:</fo:block></fo:table-cell>
  <fo:table-cell><fo:block><#if billToParty?has_content>${billToParty.partyId}</#if></fo:block></fo:table-cell>
</fo:table-row>

<fo:table-row>
  <fo:table-cell><fo:block>${uiLabelMap.AccountingInvNr}:</fo:block></fo:table-cell>
  <fo:table-cell><fo:block><#if invoice?has_content>${invoice.invoiceId}</#if></fo:block></fo:table-cell>
</fo:table-row>
<#if invoice?has_content && invoice.description?has_content>
  <fo:table-row>
    <fo:table-cell><fo:block>${uiLabelMap.AccountingDescr}:</fo:block></fo:table-cell>
    <fo:table-cell><fo:block>${invoice.description}</fo:block></fo:table-cell>
  </fo:table-row>
</#if>

<!--fo:table-row>
  <fo:table-cell><fo:block>${uiLabelMap.CommonStatus}</fo:block></fo:table-cell>
  <fo:table-cell><fo:block font-weight="bold">${invoiceStatus.get("description",locale)}</fo:block></fo:table-cell>
</fo:table-row-->
</fo:table-body>
</fo:table>
</#escape>
