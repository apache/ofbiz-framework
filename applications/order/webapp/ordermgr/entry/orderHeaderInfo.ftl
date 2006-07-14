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
 *@author     Jacopo Cappellato (tiz@sastau.it)
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
