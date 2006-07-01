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
 *@author     Leon Torres (leon@opensourcestrategies.com)
 *@author     Jacopo Cappellato (tiz@sastau.it)
-->

<#assign shoppingCart = sessionAttributes.shoppingCart?if_exists>

<table border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
  <tr>
    <td width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxtop'>
        <tr>
          <td valign="middle" align="center">
            <div class='boxhead'><b>${uiLabelMap.OrderOrderShortcuts}</b></div>
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
            <table width="100%" border="0" cellpadding="0" cellspacing="0">
                <#if shoppingCart.getOrderType() == "PURCHASE_ORDER">
                  <tr>
                    <td>
                      <a href="<@ofbizUrl>RequirementsForSupplier</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderRequirements}</a>
                    </td>
                  </tr>
                </#if>
                <#if shoppingCart.getOrderType() == "SALES_ORDER">
                  <tr>
                    <td>
                      <a href="<@ofbizUrl>FindQuoteForCart</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderOrderQuotes}</a>
                    </td>
                  </tr>
                  <tr>
                    <td>
                      <a href="<@ofbizUrl>createQuoteFromCart?destroyCart=Y</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderCreateQuoteFromCart}</a>
                    </td>
                  </tr>
                  <tr>
                    <td>
                      <a href="<@ofbizUrl>createCustRequestFromCart?destroyCart=Y</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderCreateCustRequestFromCart}</a>
                    </td>
                  </tr>
                </#if>
                <tr>
                  <td>
                    <a href="/partymgr/control/findparty?${externalKeyParam?if_exists}" class="buttontext">${uiLabelMap.PartyFindParty}</a>
                  </td>
                </tr>
                <#if shoppingCart.getOrderType() == "SALES_ORDER">
                  <tr>
                    <td>
                      <a href="<@ofbizUrl>setCustomer</@ofbizUrl>" class="buttontext">${uiLabelMap.PartyCreateNewCustomer}</a>
                    </td>
                  </tr>
                </#if>
                <tr>
                  <td>
                    <a href="<@ofbizUrl>checkinits</@ofbizUrl>" class="buttontext">${uiLabelMap.PartyChangeParty}</a>
                  </td>
                </tr>
                <#if security.hasEntityPermission("CATALOG", "_CREATE", session)>
                  <tr>
                    <td>
                      <a href="/catalog/control/EditProduct?${externalKeyParam?if_exists}" target="catalog" class="buttontext">${uiLabelMap.ProductCreateNewProduct}</a>
                    </td>
                  </tr>
                </#if>
                <tr>
                  <td>
                    <a href="<@ofbizUrl>quickadd</@ofbizUrl>" class="buttontext">${uiLabelMap.EcommerceQuickAdd}</a>
                  </td>
                </tr>
                <#if shoppingLists?exists>
                  <tr>
                    <td>
                      <a href="<@ofbizUrl>viewPartyShoppingLists?partyId=${partyId}</@ofbizUrl>" class="buttontext">${uiLabelMap.PageTitleShoppingList}</a>
                    </td>
                  </tr>
                </#if>
            </table>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>
<br/>
