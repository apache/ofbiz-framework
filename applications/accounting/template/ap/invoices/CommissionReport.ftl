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

<#if commissionReportList?has_content>
  <div>
    <a href="<@ofbizUrl>CommissionReport.pdf?isSearch=Y&amp;productId=${parameters.productId!}&amp;partyId=${parameters.partyId!}&amp;fromDate=${parameters.fromDate!}&amp;thruDate=${parameters.thruDate!}</@ofbizUrl>" target="_BLANK" class="buttontext">${uiLabelMap.AccountingInvoicePDF}</a>
  </div>
  <table class="basic-table hover-bar" cellspacing="0">
    <#-- Header Begins -->
    <tr class="header-row-2">
      <th>${uiLabelMap.AccountingLicensedProduct}</th>
      <th>${uiLabelMap.AccountingQuantity}</th>
      <th>${uiLabelMap.AccountingNumberOfOrders} / ${uiLabelMap.AccountingSalesInvoices}</th>
      <th>${uiLabelMap.AccountingCommissionAmount}</th>
      <th>${uiLabelMap.AccountingNetSale}</th>
      <th>${uiLabelMap.AccountingSalesAgents} / ${uiLabelMap.AccountingTermAmount}</th>
    </tr>
    <#-- Header Ends-->
    <#assign alt_row = false>
    <#list commissionReportList as commissionReport>
      <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
        <td><a href="<@ofbizUrl controlPath="/catalog/control">EditProduct?productId=${commissionReport.productId!}</@ofbizUrl>">${commissionReport.productName!}</a></td>
        <td>${commissionReport.quantity!}</td>
        <td>
          ${commissionReport.numberOfOrders!} /
          <#if commissionReport.salesInvoiceIds?has_content>
            <#list commissionReport.salesInvoiceIds as salesInvoiceId>
              [<a href="<@ofbizUrl controlPath="/ap/control">invoiceOverview?invoiceId=${salesInvoiceId!}</@ofbizUrl>">${salesInvoiceId!}</a>]
            </#list>
          </#if>
        </td>
        <td><@ofbizCurrency amount = commissionReport.commissionAmount!/></td>
        <td><@ofbizCurrency amount = commissionReport.netSale!/></td>
        <td>
          <#if commissionReport.salesAgentAndTermAmtMap?has_content>
            <#list commissionReport.salesAgentAndTermAmtMap.values() as partyIdAndTermAmountMap>
              <#assign partyName = (delegator.findOne("PartyNameView", {"partyId" : partyIdAndTermAmountMap.partyId}, true))!>
              <h6>[${(partyName.firstName)!} ${(partyName.lastName)!} ${(partyName.groupName)!}(<a href="<@ofbizUrl controlPath="/partymgr/control">viewprofile?partyId=${partyIdAndTermAmountMap.partyId!}</@ofbizUrl>">${partyIdAndTermAmountMap.partyId!}</a>)]
                / <@ofbizCurrency amount = (partyIdAndTermAmountMap.termAmount)!/>
              </h6>
            </#list>
          </#if>
        </td>
      </tr>
      <#-- toggle the row color -->
      <#assign alt_row = !alt_row>
    </#list>
  </table>
  <div class="screenlet">
    <ul>
      <li class="label"></li>
      <li class="label"><h3>${uiLabelMap.CommonSummary} :</h3></li>
      <li class="label"></li>
      <li class="label">${uiLabelMap.ManufacturingTotalQuantity} : ${totalQuantity!}</li>
      <li class="label">${uiLabelMap.AccountingTotalCommissionAmount} : <@ofbizCurrency amount = totalCommissionAmount!/></li>
      <li class="label">${uiLabelMap.AccountingTotalNetSales} : <@ofbizCurrency amount = totalNetSales!/></li>
      <li class="label">${uiLabelMap.AccountingTotalNumberOfOrders} : ${totalNumberOfOrders!}</li>
    </ul>
  </div>
<#else>
  <h3>${uiLabelMap.CommonNoRecordFound}</h3>
</#if>
