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
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@author     Jean-Luc.Malet@nereide.biz (migration to uiLabelMap)
 *@version    $Rev$
 *@since      2.2
-->

<#assign shoppingCartOrderType = "">
<#assign shoppingCartProductStore = "NA">
<#assign shoppingCartChannelType = "">
<#if shoppingCart?exists>
  <#assign shoppingCartOrderType = shoppingCart.getOrderType()>
  <#assign shoppingCartProductStore = shoppingCart.getProductStoreId()?default("NA")>
  <#assign shoppingCartChannelType = shoppingCart.getChannelType()?default("")>
<#else>
<#-- allow the order type to be set in parameter, so only the appropriate section (Sales or Purchase Order) shows up -->
  <#if parameters.orderTypeId?has_content>
    <#assign shoppingCartOrderType = parameters.orderTypeId>
  </#if>
</#if>
<!-- Sales Order Entry -->
<#if security.hasEntityPermission("ORDERMGR", "_CREATE", session)>
<#if shoppingCartOrderType != "PURCHASE_ORDER">
<table width="100%" border="0" align="center" cellspacing='0' cellpadding='0' class='boxoutside'>
  <tr>
    <td>
      <table width="100%" border='0' cellspacing='0' cellpadding='0' class='boxtop'>
        <tr>
          <td valign="middle" align="left">
            <div class="boxhead">
              ${uiLabelMap.OrderSalesOrder}<#if shoppingCart?exists>&nbsp;${uiLabelMap.OrderInProgress}</#if>
            </div>
          </td>
          <td valign="middle" align="right">
            <a href="/partymgr/control/findparty?externalLoginKey=${externalLoginKey}" class="submenutext">${uiLabelMap.PartyFindParty}</a>
            <a href="javascript:document.salesentryform.submit();" class="submenutextright">${uiLabelMap.CommonContinue}</a>
          </td>
        </tr>
      </table>
    </td>
  </tr>
  <tr>
    <td>
      <form method="post" name="salesentryform" action="<@ofbizUrl>initorderentry</@ofbizUrl>">
      <input type='hidden' name='finalizeMode' value='type'/>
      <input type='hidden' name='orderMode' value='SALES_ORDER'/>
      <table width="100%" border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
        <tr>
          <td >&nbsp;</td>
          <td align='right' valign='middle' nowrap><div class='tableheadtext'>${uiLabelMap.ProductProductStore}</div></td>
          <td >&nbsp;</td>
          <td valign='middle'>
            <div class='tabletext' valign='top'>
              <select class="selectBox" name="productStoreId"<#if sessionAttributes.orderMode?exists> disabled</#if>>
                <#assign currentStore = shoppingCartProductStore>
                <#list productStores as productStore>
                  <option value="${productStore.productStoreId}"<#if productStore.productStoreId == currentStore> selected</#if>>${productStore.storeName?if_exists}</option>
                </#list>
              </select>
              <#if sessionAttributes.orderMode?exists>${uiLabelMap.OrderCannotBeChanged}</#if>
            </div>
          </td>
        </tr>
        <tr><td colspan="4">&nbsp;</td></tr>
        <tr>
          <td>&nbsp;</td>
          <td align='right' valign='middle' nowrap><div class='tableheadtext'>${uiLabelMap.OrderSalesChannel}</div></td>
          <td>&nbsp;</td>
          <td valign='middle'>
            <div class='tabletext' valign='top'>
              <select class="selectBox" name="salesChannelEnumId">
                <#assign currentChannel = shoppingCartChannelType>
                <option value="">${uiLabelMap.OrderNoChannel}</option>
                <#list salesChannels as salesChannel>
                  <option value="${salesChannel.enumId}" <#if (salesChannel.enumId == currentChannel)>selected</#if>>${salesChannel.get("description",locale)}</option>
                </#list>
              </select>
            </div>
          </td>
        </tr>
        <tr><td colspan="4">&nbsp;</td></tr>
        <#if partyId?exists>
          <#assign thisPartyId = partyId>
        <#else>
          <#assign thisPartyId = requestParameters.partyId?if_exists>
        </#if>
        <tr>
          <td>&nbsp;</td>
          <td align='right' valign='middle' nowrap><div class='tableheadtext'>${uiLabelMap.PartyUserLoginId}</div></td>
          <td>&nbsp;</td>
          <td valign='middle'>
            <div class='tabletext' valign='top'>
              <input type='text' class='inputBox' name='userLoginId' value='${requestParameters.userLoginId?if_exists}'/>
            </div>
          </td>
        </tr>
        <tr>
          <td>&nbsp;</td>
          <td align='right' valign='middle' nowrap><div class='tableheadtext'>${uiLabelMap.PartyPartyId}</div></td>
          <td>&nbsp;</td>
          <td valign='middle'>
            <div class='tabletext' valign='top'>
              <input type='text' class='inputBox' name='partyId' value='${thisPartyId?if_exists}'>
              <a href="javascript:call_fieldlookup2(document.salesentryform.partyId,'LookupPartyName');">
                <img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'/>
              </a>
            </div>
          </td>
        </tr>
      </table>
      </form>
    </td>
  </tr>
</table>
</#if>
</#if>
<br/>
<!-- Purchase Order Entry -->
<#if security.hasEntityPermission("ORDERMGR", "_PURCHASE_CREATE", session)>
  <#if shoppingCartOrderType != "SALES_ORDER">
<table width="100%" border="0" align="center" cellspacing='0' cellpadding='0' class='boxoutside'>
  <tr>
    <td>
      <table width="100%" border='0' cellspacing='0' cellpadding='0' class='boxtop'>
        <tr>
          <td valign="middle" align="left">
            <div class="boxhead">
              ${uiLabelMap.OrderPurchaseOrder}<#if shoppingCart?exists>&nbsp;${uiLabelMap.OrderInProgress}</#if>
            </div>
          </td>
          <td valign="middle" align="right">
            <a href="/partymgr/control/findparty?externalLoginKey=${externalLoginKey}" class="submenutext">${uiLabelMap.PartyFindParty}</a>
            <a href="javascript:document.poentryform.submit();" class="submenutextright">${uiLabelMap.CommonContinue}</a>
          </td>
        </tr>
      </table>
    </td>
  </tr>
  <tr>
    <td>
      <form method="post" name="poentryform" action="<@ofbizUrl>initorderentry</@ofbizUrl>">
      <input type='hidden' name='finalizeMode' value='type'/>
      <input type='hidden' name='orderMode' value='PURCHASE_ORDER'/>
      <table width="100%" border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
        <#if partyId?exists>
          <#assign thisPartyId = partyId>
        <#else>
          <#assign thisPartyId = requestParameters.partyId?if_exists>
        </#if>
        <tr>
          <td>&nbsp;</td>
          <td align='right' valign='middle' nowrap><div class='tableheadtext'>${uiLabelMap.OrderOrderEntryInternalOrganziation}</div></td>
          <td>&nbsp;</td>
          <td valign='middle'>
            <div class='tabletext' valign='top'>
              <select class="selectBox" name="billToCustomerPartyId"<#if sessionAttributes.orderMode?default("") == "SALES_ORDER"> disabled</#if>>
                <#list organizations as organization>
                  <#assign organizationName = Static["org.ofbiz.party.party.PartyHelper"].getPartyName(organization, true)/>
                    <#if (organizationName.length() != 0)>
                      <option value="${organization.partyId}">${organization.partyId} - ${organizationName}</option>
                    </#if>
                </#list>
              </select>
            </div>
          </td>
        </tr>
        <tr>
          <td>&nbsp;</td>
          <td align='right' valign='middle' nowrap><div class='tableheadtext'>${uiLabelMap.PartySupplier}</div></td>
          <td>&nbsp;</td>
          <td valign='middle'>
            <div class='tabletext' valign='top'>
              <select class="selectBox" name="supplierPartyId"<#if sessionAttributes.orderMode?default("") == "SALES_ORDER"> disabled</#if>>
                <option value="">${uiLabelMap.PartyNoSupplier}</option>
                <#list suppliers as supplier>
                  <option value="${supplier.partyId}"<#if supplier.partyId == thisPartyId> selected</#if>>${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(supplier, true)}</option>
                </#list>
              </select>
            </div>
          </td>
        </tr>
        <tr>
          <td>&nbsp;</td>
          <td align='right' valign='middle' nowrap><div class='tableheadtext'>${uiLabelMap.PartyUserLoginId}</div></td>
          <td>&nbsp;</td>
          <td valign='middle'>
            <div class='tabletext' valign='top'>
              <input type='text' class='inputBox' name='userLoginId' value='${requestParameters.userLoginId?if_exists}'/>
            </div>
          </td>
        </tr>
      </table>
      </form>
    </td>
  </tr>
</table>
  </#if>
</#if>
