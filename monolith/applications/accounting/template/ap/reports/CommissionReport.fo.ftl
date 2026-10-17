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
  <fo:block font-size="14pt" font-weight="bold" text-align="center">${uiLabelMap.AccountingCommissionReport}</fo:block>
  <fo:block space-after="20pt"/>
  <fo:table table-layout="fixed" font-size="10pt">
    <fo:table-column column-width="35mm"/>
    <fo:table-column column-width="20mm"/>
    <fo:table-column column-width="35mm"/>
    <fo:table-column column-width="25mm"/>
    <fo:table-column column-width="20mm"/>
    <fo:table-column column-width="60mm"/>
    <fo:table-header height="14px">
      <fo:table-row>
        <fo:table-cell border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
          <fo:block font-weight="bold">${uiLabelMap.AccountingLicensedProduct}</fo:block>
        </fo:table-cell>
        <fo:table-cell border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
          <fo:block font-weight="bold">${uiLabelMap.AccountingQuantity}</fo:block>
        </fo:table-cell>
        <fo:table-cell border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
          <fo:block font-weight="bold">${uiLabelMap.AccountingNumberOfOrders} / ${uiLabelMap.AccountingSalesInvoices}</fo:block>
        </fo:table-cell>
        <fo:table-cell border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
          <fo:block font-weight="bold">${uiLabelMap.AccountingCommissionAmount}</fo:block>
        </fo:table-cell>
        <fo:table-cell border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
          <fo:block font-weight="bold">${uiLabelMap.AccountingNetSale}</fo:block>
        </fo:table-cell>
        <fo:table-cell  border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
          <fo:block font-weight="bold">${uiLabelMap.AccountingSalesAgents} / ${uiLabelMap.AccountingTermAmount}</fo:block>
        </fo:table-cell>
      </fo:table-row>
    </fo:table-header>
    <fo:table-body font-size="10pt" height="500px">
      <#if commissionReportList?has_content>
        <#list commissionReportList as commissionReport>
          <fo:table-row height="14px">
            <fo:table-cell>
              <fo:block>${commissionReport.productName!}</fo:block>
            </fo:table-cell>
            <fo:table-cell>
              <fo:block>${commissionReport.quantity!}</fo:block>
            </fo:table-cell>
            <fo:table-cell>
              <fo:block>${commissionReport.numberOfOrders!} / ${commissionReport.salesInvoiceIds!}</fo:block>
            </fo:table-cell>
            <fo:table-cell>
              <fo:block><@ofbizCurrency amount = commissionReport.commissionAmount!/></fo:block>
            </fo:table-cell>
            <fo:table-cell>
              <fo:block><@ofbizCurrency amount = commissionReport.netSale!/></fo:block>
            </fo:table-cell>
            <fo:table-cell>
              <#if commissionReport.salesAgentAndTermAmtMap?has_content>
                <#list commissionReport.salesAgentAndTermAmtMap.values() as partyIdAndTermAmountMap>
                  <#assign partyName = (delegator.findOne("PartyNameView", {"partyId" : partyIdAndTermAmountMap.partyId}, true))!>
                  <fo:block>
                    [${(partyName.firstName)!} ${(partyName.lastName)!} ${(partyName.groupName)!} (${partyIdAndTermAmountMap.partyId!})] / <@ofbizCurrency amount = (partyIdAndTermAmountMap.termAmount)!/>
                  </fo:block>
                </#list>
              </#if>
            </fo:table-cell>
          </fo:table-row>
        </#list>
      <#else>
        <fo:table-row height="14px">
          <fo:table-cell number-columns-spanned="5">
            <fo:block space-after="10pt"/>
            <fo:block text-align="center">${uiLabelMap.CommonNoRecordFound}</fo:block>
          </fo:table-cell>
        </fo:table-row>
      </#if>
    </fo:table-body>
 </fo:table>
 <fo:block space-after="50pt"/>
 <#if commissionReportList?has_content && totalQuantity?has_content && totalCommissionAmount?has_content && totalNetSales?has_content && totalNumberOfOrders?has_content>
   <fo:table table-layout="fixed" font-size="14pt">
     <fo:table-body font-size="10pt">
       <fo:table-row>
         <fo:table-cell>
           <fo:block font-size="14pt" font-weight="bold">${uiLabelMap.CommonSummary} :</fo:block>
           <fo:block space-after="10pt"/>
           <fo:block font-weight="bold">${uiLabelMap.ManufacturingTotalQuantity} : ${totalQuantity!}</fo:block>
           <fo:block font-weight="bold">${uiLabelMap.AccountingTotalCommissionAmount} : <@ofbizCurrency amount = totalCommissionAmount!/></fo:block>
           <fo:block font-weight="bold">${uiLabelMap.AccountingTotalNetSales} : <@ofbizCurrency amount = totalNetSales!/></fo:block>
           <fo:block font-weight="bold">${uiLabelMap.AccountingTotalNumberOfOrders} : ${totalNumberOfOrders!}</fo:block>
         </fo:table-cell>
       </fo:table-row>
     </fo:table-body>
   </fo:table>
 </#if>
</#escape>
