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

        <fo:table table-layout="fixed" border-spacing="3pt">
            <fo:table-column column-width="3.75in"/>
            <fo:table-column column-width="3.75in"/>
            <fo:table-body>
                <fo:table-row>
                    <fo:table-cell>
                        <fo:block>
                            <fo:block font-weight="bold">${uiLabelMap.OrderAddress}: </fo:block>
                            <#if quote.partyId??>
                                <#assign quotePartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", quote.partyId, "compareDate", quote.issueDate, "userLogin", userLogin))/>
                                <fo:block>${quotePartyNameResult.fullName?default("[${uiLabelMap.OrderPartyNameNotFound}]")}</fo:block>
                            <#else>
                                <fo:block>[${uiLabelMap.OrderPartyNameNotFound}]</fo:block>
                            </#if>
                        </fo:block>
                    </fo:table-cell>
                </fo:table-row>
                <fo:table-row>
                    <fo:table-cell>
                        <fo:block>
                            <#if toPostalAddress??>
                                ${setContextField("postalAddress", toPostalAddress)}
                                ${screens.render("component://party/widget/partymgr/PartyScreens.xml#postalAddressPdfFormatter")}
                            </#if>
                        </fo:block>
                    </fo:table-cell>
                </fo:table-row>
            </fo:table-body>
        </fo:table>


        <fo:table table-layout="fixed" border-spacing="3pt" space-before="0.5in" space-after="0.5in">
            <fo:table-column column-width="1.5in"/>
            <fo:table-column column-width="3.75in"/>
            <fo:table-body>
                <fo:table-row>
                    <fo:table-cell><fo:block>${uiLabelMap.OrderOrderQuoteName}:</fo:block></fo:table-cell>
                    <fo:table-cell><fo:block>${quote.quoteName!}</fo:block></fo:table-cell>
                </fo:table-row>
                <fo:table-row>
                    <fo:table-cell><fo:block>${uiLabelMap.CommonDescription}:</fo:block></fo:table-cell>
                    <fo:table-cell><fo:block>${quote.description!}</fo:block></fo:table-cell>
                </fo:table-row>
                <fo:table-row>
                    <fo:table-cell><fo:block>${uiLabelMap.CommonCurrency}:</fo:block></fo:table-cell>
                    <fo:table-cell><fo:block><#if currency??>${currency.get("description",locale)?default(quote.currencyUomId!)}</#if></fo:block></fo:table-cell>
                </fo:table-row>
                <fo:table-row>
                    <fo:table-cell><fo:block>${uiLabelMap.CommonValidFromDate}:</fo:block></fo:table-cell>
                    <fo:table-cell><fo:block>${(quote.validFromDate.toString())!}</fo:block></fo:table-cell>
                </fo:table-row>
                <fo:table-row>
                    <fo:table-cell><fo:block>${uiLabelMap.CommonValidThruDate}:</fo:block></fo:table-cell>
                    <fo:table-cell><fo:block>${(quote.validThruDate.toString())!}</fo:block></fo:table-cell>
                </fo:table-row>
                <fo:table-row>
                    <#assign quoteLevelTerms = Static["org.ofbiz.entity.util.EntityUtil"].filterByAnd(quoteTerms, {"quoteItemSeqId": "_NA_"})!>
                    <#if quoteLevelTerms?has_content && quoteLevelTerms.size() gt 0>
                        <fo:table-cell><fo:block>${uiLabelMap.CommonQuoteTerms}:</fo:block></fo:table-cell>
                        <fo:table-cell>
                            <#list quoteLevelTerms as quoteLevelTerm>
                                <fo:block>
                                    ${quoteLevelTerm.getRelatedOne("TermType", false).get("description",locale)} ${quoteLevelTerm.termValue?default("")} ${quoteLevelTerm.termDays?default("")} ${quoteLevelTerm.textValue?default("")}
                                </fo:block>
                            </#list>
                        </fo:table-cell>
                    </#if>
                </fo:table-row>
            </fo:table-body>
        </fo:table>
</#escape>
