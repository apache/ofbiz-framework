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

<#assign shoppingCart = sessionAttributes.shoppingCart?if_exists>
<#assign currencyUomId = shoppingCart.getCurrency()>
<#assign partyId = shoppingCart.getPartyId()>
<#assign partyMap = Static["org.ofbiz.party.party.PartyWorker"].getPartyOtherValues(request, partyId, "party", "person", "partyGroup")>
<#assign agreementId = shoppingCart.getAgreementId()?if_exists>
<#assign quoteId = shoppingCart.getQuoteId()?if_exists>

<#if shoppingCart?has_content>
    <#assign shoppingCartSize = shoppingCart.size()>
<#else>
    <#assign shoppingCartSize = 0>
</#if>

<table border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
  <tr>
    <td width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxtop'>
        <tr>
          <td valign="middle" align="center">
            <div class='boxhead'><b>${uiLabelMap.OrderOrderHeaderInfo}</b></div>
          </td>
        </tr>
      </table>
    </td>
  </tr>
  <tr>
    <td width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
        <tr>
          <td>
            <table width="100%" border="0" cellpadding="2" cellspacing="0">
              <tr valign="top">
                <td class="tabletext"><b>${uiLabelMap.OrderOrderName}</b>:</td> 
                <td class="tabletext">
                  <form method="post" action="setOrderName" name="setCartOrderNameForm">
                    <input type="text" name="orderName" class="inputBox" size="15" maxlength="200" value='${shoppingCart.getOrderName()?default("")}'/>
                    <input type="submit" value="${uiLabelMap.CommonSet}" class="smallSubmit"/>
                  </form>
                </td>
              </tr>
                <tr>
                  <td><div class="tabletext"><b>${uiLabelMap.Party}</b>:</div></td>
                  <td>
                    <div class="tabletext">
                      <a href="${customerDetailLink}${partyId}&${externalKeyParam?if_exists}" target="partymgr" class="buttontext">${partyId}</a>
                      <#if partyMap.person?exists>
                        ${partyMap.person.firstName?if_exists}&nbsp;${partyMap.person.lastName?if_exists}
                      </#if>
                      <#if partyMap.partyGroup?exists>
                        ${partyMap.partyGroup.groupName?if_exists}
                      </#if>
                    </div>
                  </td>
                </tr>
                <#if shoppingCart.getOrderType() != "PURCHASE_ORDER">
                <tr valign="top">
                  <td class="tabletext"><b>${uiLabelMap.OrderPONumber}</b>:</td> 
                  <td class="tabletext">
                    <form method="post" action="setPoNumber" name="setCartPoNumberForm">
                      <input type="text" name="correspondingPoId" class="inputBox" size="15" value='${shoppingCart.getPoNumber()?default("")}'/>
                      <input type="submit" value="${uiLabelMap.CommonSet}" class="smallSubmit"/>
                    </form>
                  </td>
                </tr>
                </#if>
                <tr>
                  <td valign="bottom"><div class="tabletext"><b>${uiLabelMap.CommonCurrency}</b>:</div></td>
                  <td valign="bottom"><div class="tabletext">${currencyUomId}</div></td>
                </tr>
                <#if agreementId?has_content>
                <tr>
                  <td valign="bottom"><div class="tabletext"><b>${uiLabelMap.AccountingAgreement}</b>:</div></td>
                  <td valign="bottom"><div class="tabletext">${agreementId}</div></td>
                </tr>
                </#if>
                <#if quoteId?has_content>
                <tr>
                  <td valign="bottom"><div class="tabletext"><b>${uiLabelMap.OrderOrderQuote}</b>:</div></td>
                  <td valign="bottom"><div class="tabletext">${quoteId}</div></td>
                </tr>
                </#if>
                <tr>
                  <td colspan="2" align="right">
                    <div class="tabletext"><b>${uiLabelMap.CommonTotal}: <@ofbizCurrency amount=shoppingCart.getGrandTotal() isoCode=currencyUomId/></b></div>
                  </td>
                </tr>
            </table>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>
<br/>
