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

<#--
Generates PDF of multiple checks in two styles: one check per page, multiple checks per page
Note that this must be customized to fit specific check layouts. The layout here is copied
by hand from a real template using a ruler.
-->
<#escape x as x?xml>

<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
  <fo:layout-master-set>
    <#-- define the margins of the check layout here -->
    <fo:simple-page-master master-name="checks" page-height="27.9cm" page-width="21.6cm">
      <fo:region-body/>
    </fo:simple-page-master>
  </fo:layout-master-set>

  <fo:page-sequence master-reference="checks">
    <fo:flow flow-name="xsl-region-body">
      <fo:block>
        <fo:table>
          <fo:table-column column-number="1"/>
          <fo:table-column column-number="2"/>
          <fo:table-body>
            <fo:table-row>
              <fo:table-cell>
                <fo:block>
                  <fo:table table-layout="fixed" width="100%" margin-top="50px" margin-left="8px">
                    <fo:table-column column-width="2in"/>
                    <fo:table-column column-width="2in"/>
                    <fo:table-body>
                      <fo:table-row>
                        <fo:table-cell><fo:block font-weight="bold" font-size="10px" text-align="right">${uiLabelMap.FormFieldTitle_invoiceId}:</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block>${parameters.invoiceId!}</fo:block></fo:table-cell>
                      </fo:table-row>
                      <fo:table-row>
                        <fo:table-cell><fo:block font-weight="bold" font-size="10px" text-align="right" padding-top="2px">${uiLabelMap.CommonDescription}:</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block font-weight="bold" font-size="10px" text-align="right" padding-top="2px">${invoiceDescription!}</fo:block></fo:table-cell>
                      </fo:table-row>
                      <fo:table-row>
                        <fo:table-cell><fo:block font-weight="bold" font-size="10px" text-align="right" padding-top="2px">${uiLabelMap.CommonStatus}:</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block>${statusDescription!}</fo:block></fo:table-cell>
                      </fo:table-row>
                      <fo:table-row>
                        <fo:table-cell><fo:block font-weight="bold" font-size="10px" text-align="right" padding-top="2px">${uiLabelMap.AccountingReferenceNumber}:</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block>${referenceNumber!}</fo:block></fo:table-cell>
                      </fo:table-row>
                    </fo:table-body>
                  </fo:table>
                </fo:block>
              </fo:table-cell>
              <fo:table-cell>
                <fo:block>
                  <fo:table table-layout="fixed" width="100%" margin-top="50px" margin-left="8px">
                    <fo:table-column column-width="2in"/>
                    <fo:table-column column-width="2in"/>
                    <fo:table-body>
                      <fo:table-row>
                        <fo:table-cell><fo:block font-weight="bold" font-size="10px" text-align="right">${uiLabelMap.CommonUsername}:</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block>${userLoginName!}</fo:block></fo:table-cell>
                      </fo:table-row>
                      <fo:table-row>
                        <fo:table-cell><fo:block font-weight="bold" font-size="10px" text-align="right" padding-top="2px">${uiLabelMap.CommonDate}:</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block>${invoiceDate?string("dd MMMMM yyyy")}</fo:block></fo:table-cell>
                      </fo:table-row>
                      <fo:table-row>
                        <fo:table-cell><fo:block font-weight="bold" font-size="10px" text-align="right" padding-top="2px">${uiLabelMap.CommonPartyId}:</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block>${partyId!}</fo:block></fo:table-cell>
                      </fo:table-row>
                      <fo:table-row>
                        <fo:table-cell><fo:block font-weight="bold" font-size="10px" text-align="right" padding-top="2px">${uiLabelMap.AccountingPartyName}:</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block>${partyName!}</fo:block></fo:table-cell>
                      </fo:table-row>
                    </fo:table-body>
                  </fo:table>
                </fo:block>
              </fo:table-cell>
            </fo:table-row>
          </fo:table-body>
        </fo:table>
        <fo:table table-layout="fixed" margin-left="5pt" margin-top="100px" margin-right="5pt" width="100%">
          <fo:table-column column-width="3cm" border-style="solid" border-width="solid" border-color="black"/>
          <fo:table-column column-width="6cm" border-style="solid" border-width="solid" border-color="black"/>
          <fo:table-column column-width="3cm" border-style="solid" border-width="solid" border-color="black"/>
          <fo:table-column column-width="3cm" border-style="solid" border-width="solid" border-color="black"/>
          <fo:table-column column-width="3cm" border-style="solid" border-width="solid" border-color="black"/>
          <fo:table-column column-width="3cm" border-style="solid" border-width="solid" border-color="black"/>
          <fo:table-header background-color="#BFBFBF" border-style="solid" border-width="solid" border-color="black">
            <fo:table-row>
              <fo:table-cell>
                <fo:block font-weight="bold" text-align="center" padding-top="2mm">${uiLabelMap.FormFieldTitle_accountCode}</fo:block>
              </fo:table-cell>
              <fo:table-cell>
                <fo:block font-weight="bold" text-align="center" padding-top="2mm">${uiLabelMap.FormFieldTitle_accountName}</fo:block>
              </fo:table-cell>
              <fo:table-cell>
                <fo:block font-weight="bold" text-align="center" padding-top="2mm">${uiLabelMap.AccountingOriginalCurrency}</fo:block>
              </fo:table-cell>
              <fo:table-cell>
                <fo:block font-weight="bold" text-align="center" padding-top="2mm">${uiLabelMap.AccountingExchangeRate}</fo:block>
              </fo:table-cell>
              <fo:table-cell>
                <fo:block font-weight="bold" text-align="center" padding-top="2mm">${uiLabelMap.AccountingDebitFlag}</fo:block>
              </fo:table-cell >
              <fo:table-cell>
                <fo:block font-weight="bold" text-align="center" padding-top="2mm">${uiLabelMap.AccountingCreditFlag}</fo:block>
              </fo:table-cell>
            </fo:table-row>
          </fo:table-header>
          <fo:table-body>
            <#assign debitTotal = 0/>
            <#assign creditTotal = 0/>
            <#list invoiceAcctgTransAndEntries as invoiceAcctgTransAndEntry>
              <fo:table-row border-style="solid" border-width="solid" border-color="black">
                <fo:table-cell><fo:block padding-top="3px">${invoiceAcctgTransAndEntry.accountCode!}</fo:block></fo:table-cell>
                <fo:table-cell><fo:block padding-top="3px">${invoiceAcctgTransAndEntry.accountName!}</fo:block></fo:table-cell>
                <fo:table-cell><fo:block padding-top="3px">${invoiceAcctgTransAndEntry.origCurrencyUomId!}</fo:block></fo:table-cell>
                <fo:table-cell><fo:block padding-top="3px">${invoiceAcctgTransAndEntry.origAmount!}/${invoiceAcctgTransAndEntry.amount!}  ${invoiceAcctgTransAndEntry.origCurrencyUomId!}/${invoiceAcctgTransAndEntry.currencyUomId!}</fo:block></fo:table-cell>
                <fo:table-cell text-align="center">
                  <fo:block>
                    <#if "D"==invoiceAcctgTransAndEntry.debitCreditFlag>
                      <#assign debitTotal = debitTotal +invoiceAcctgTransAndEntry.amount>
                      ${invoiceAcctgTransAndEntry.amount!}
                    </#if>
                  </fo:block>
                </fo:table-cell>
                <fo:table-cell text-align="center">
                  <fo:block>
                    <#if "C"==invoiceAcctgTransAndEntry.debitCreditFlag>
                      <#assign creditTotal = creditTotal +invoiceAcctgTransAndEntry.amount>
                      ${invoiceAcctgTransAndEntry.amount!}
                    </#if>
                  </fo:block>
                </fo:table-cell>
              </fo:table-row>
            </#list>
            <fo:table-row border-style="solid" border-width="solid" border-color="black">
                <fo:table-cell number-columns-spanned="4">
                  <fo:block font-weight="bold" padding-top="3px">
                    <#if debitTotal == creditTotal>
                      <#assign baseCurrencyUomId = (delegator.findOne("PartyAcctgPreference", {"partyId" : partyId}, true))!>
                      <#if baseCurrencyUomId?has_content && "THB" == baseCurrencyUomId.baseCurrencyUomId>
                        <#assign locale = Static["org.apache.ofbiz.base.util.UtilMisc"].parseLocale("th")!/>
                      <#else>
                        <#assign locale = Static["org.apache.ofbiz.base.util.UtilMisc"].parseLocale("en_us")!/>
                      </#if>
                      <#assign amount = Static["org.apache.ofbiz.base.util.UtilNumber"].formatRuleBasedAmount(debitTotal, locale).toUpperCase()>
                      ${uiLabelMap.AccountingTotalCapital} : ${amount!}
                    <#else>
                      ${uiLabelMap.AccountingDebitNotEqualCredit}
                    </#if>
                  </fo:block>
                </fo:table-cell>
                <fo:table-cell text-align="center">
                  <fo:block>${debitTotal!}</fo:block>
                </fo:table-cell>
                <fo:table-cell text-align="center"><fo:block>${creditTotal!}</fo:block></fo:table-cell>
              </fo:table-row>
          </fo:table-body>
        </fo:table>
        <fo:table>
          <fo:table-column column-number="1"/>
          <fo:table-column column-number="2"/>
          <fo:table-column column-number="3"/>
          <fo:table-body>
            <fo:table-row>
              <fo:table-cell>
                <fo:block>
                  <fo:table table-layout="fixed" width="100%" margin-top="50px" margin-left="8px">
                    <fo:table-column column-width="1in"/>
                    <fo:table-column column-width="1in"/>
                    <fo:table-column column-width="1in"/>
                    <fo:table-body>
                      <fo:table-row>
                        <fo:table-cell><fo:block font-size="10px" margin-left="5px">${uiLabelMap.AccountingPreparedBy}</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block ><fo:leader leader-pattern="dots" leader-length="3cm"/></fo:block></fo:table-cell>
                      </fo:table-row>
                    </fo:table-body>
                  </fo:table>
                </fo:block>
              </fo:table-cell>
              <fo:table-cell>
                <fo:block>
                  <fo:table table-layout="fixed" width="100%" margin-top="50px" margin-left="8px">
                    <fo:table-column column-width="1in"/>
                    <fo:table-column column-width="1in"/>
                    <fo:table-body>
                      <fo:table-row>
                        <fo:table-cell><fo:block font-size="10px" text-align="right">${uiLabelMap.AccountingApprovedBy}</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block ><fo:leader leader-pattern="dots" leader-length="3cm"/></fo:block></fo:table-cell>
                      </fo:table-row>
                    </fo:table-body>
                  </fo:table>
                </fo:block>
              </fo:table-cell>
              <fo:table-cell>
                <fo:block>
                  <fo:table table-layout="fixed" width="100%" margin-top="50px" margin-left="8px">
                    <fo:table-column column-width="1in"/>
                    <fo:table-column column-width="1in"/>
                    <fo:table-body>
                      <fo:table-row>
                        <fo:table-cell><fo:block font-size="10px" text-align="right">${uiLabelMap.AccountingReceivedBy}</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block ><fo:leader leader-pattern="dots" leader-length="3cm"/></fo:block></fo:table-cell>
                      </fo:table-row>
                    </fo:table-body>
                  </fo:table>
                </fo:block>
              </fo:table-cell>
            </fo:table-row>
            <fo:table-row>
              <fo:table-cell>
                <fo:block>
                  <fo:table table-layout="fixed" width="100%" margin-top="50px" margin-left="8px">
                    <fo:table-column column-width="1in"/>
                    <fo:table-column column-width="1in"/>
                    <fo:table-column column-width="1in"/>
                    <fo:table-body>
                      <fo:table-row>
                        <fo:table-cell><fo:block font-size="10px" text-align="right">${uiLabelMap.CommonDate}</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block ><fo:leader leader-pattern="dots" leader-length="3cm"/></fo:block></fo:table-cell>
                      </fo:table-row>
                    </fo:table-body>
                  </fo:table>
                </fo:block>
              </fo:table-cell>
              <fo:table-cell>
                <fo:block>
                  <fo:table table-layout="fixed" width="100%" margin-top="50px" margin-left="8px">
                    <fo:table-column column-width="1in"/>
                    <fo:table-column column-width="1in"/>
                    <fo:table-body>
                      <fo:table-row>
                        <fo:table-cell><fo:block font-size="10px" text-align="right">${uiLabelMap.CommonDate}</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block ><fo:leader leader-pattern="dots" leader-length="3cm"/></fo:block></fo:table-cell>
                      </fo:table-row>
                    </fo:table-body>
                  </fo:table>
                </fo:block>
              </fo:table-cell>
              <fo:table-cell>
                <fo:block>
                  <fo:table table-layout="fixed" width="100%" margin-top="50px" margin-left="8px">
                    <fo:table-column column-width="1in"/>
                    <fo:table-column column-width="1in"/>
                    <fo:table-body>
                      <fo:table-row>
                        <fo:table-cell><fo:block font-size="10px" text-align="right">${uiLabelMap.CommonDate}</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block ><fo:leader leader-pattern="dots" leader-length="3cm"/></fo:block></fo:table-cell>
                      </fo:table-row>
                    </fo:table-body>
                  </fo:table>
                </fo:block>
              </fo:table-cell>
            </fo:table-row>
          </fo:table-body>
        </fo:table>
      </fo:block>
    </fo:flow>
  </fo:page-sequence>
</fo:root>
</#escape>