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
<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.OrderSalesOrder}<#if shoppingCart?exists>&nbsp;${uiLabelMap.OrderInProgress}</#if></li>
      <li><a href="javascript:document.salesentryform.submit();">${uiLabelMap.CommonContinue}</a></li>
      <li><a href="/partymgr/control/findparty?externalLoginKey=${externalLoginKey}">${uiLabelMap.PartyFindParty}</a></li>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
      <form method="post" name="salesentryform" action="<@ofbizUrl>initorderentry</@ofbizUrl>">
      <input type="hidden" name="originOrderId" value="${parameters.originOrderId?if_exists}"/>
      <input type="hidden" name="finalizeMode" value="type"/>
      <input type="hidden" name="orderMode" value="SALES_ORDER"/>
      <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxbottom">
        <tr>
          <td >&nbsp;</td>
          <td width=300 align='right' valign='middle' nowrap><div class='tableheadtext'>${uiLabelMap.ProductProductStore}</div></td>
          <td >&nbsp;</td>
          <td valign='middle'>
            <div class='tabletext' valign='top'>
              <select name="productStoreId"<#if sessionAttributes.orderMode?exists> disabled</#if>>
                <#assign currentStore = shoppingCartProductStore>
                <#if defaultProductStore?has_content>
                   <option value="${defaultProductStore.productStoreId}">${defaultProductStore.storeName?if_exists}</option>
                   <option value="${defaultProductStore.productStoreId}">----</option>
                </#if>
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
              <select name="salesChannelEnumId">
                <#assign currentChannel = shoppingCartChannelType>
                <#if defaultSalesChannel?has_content>
                   <option value="${defaultSalesChannel.enumId}">${defaultSalesChannel.description?if_exists}</option>
                   <option value="${defaultSalesChannel.enumId}"> ---- </option>
                </#if>
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
              <input type="text" name="userLoginId" value="${parameters.userLogin.userLoginId}"/>
              <a href="javascript:call_fieldlookup2(document.salesentryform.userLoginId,'LookupUserLoginAndPartyDetails');">
                <img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt="${uiLabelMap.CommonClickHereForFieldLookup}"/>
              </a>
            </div>
          </td>
        </tr>
        <tr>
          <td>&nbsp;</td>
          <td align='right' valign='middle' nowrap><div class='tableheadtext'>${uiLabelMap.OrderCustomer}</div></td>
          <td>&nbsp;</td>
          <td valign='middle'>
            <div class='tabletext' valign='top'>
              <input type='text' class='inputBox' name='partyId' value='${thisPartyId?if_exists}'/>
              <a href="javascript:call_fieldlookup2(document.salesentryform.partyId,'LookupCustomerName');">
                <img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt="${uiLabelMap.CommonClickHereForFieldLookup}"/>
              </a>
            </div>
          </td>
        </tr>
      </table>
      </form>
  </div>
</div>
</#if>
</#if>
<br />
<!-- Purchase Order Entry -->
<#if security.hasEntityPermission("ORDERMGR", "_PURCHASE_CREATE", session)>
  <#if shoppingCartOrderType != "SALES_ORDER">
  <div class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <li class="h3">${uiLabelMap.OrderPurchaseOrder}<#if shoppingCart?exists>&nbsp;${uiLabelMap.OrderInProgress}</#if></li>
        <li><a href="javascript:document.poentryform.submit();">${uiLabelMap.CommonContinue}</a></li>
        <li><a href="/partymgr/control/findparty?externalLoginKey=${externalLoginKey}">${uiLabelMap.PartyFindParty}</a></li>
      </ul>
      <br class="clear"/>
    </div>
    <div class="screenlet-body">
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
          <td width=300 align='right' valign='middle' nowrap><div class='tableheadtext'>${uiLabelMap.OrderOrderEntryInternalOrganization}</div></td>
          <td>&nbsp;</td>
          <td valign='middle'>
            <div class='tabletext' valign='top'>
              <select name="billToCustomerPartyId"<#if sessionAttributes.orderMode?default("") == "SALES_ORDER"> disabled</#if>>
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
        <tr><td colspan="4">&nbsp;</td></tr>
        <tr>
          <td>&nbsp;</td>
          <td align='right' valign='middle' nowrap><div class='tableheadtext'>${uiLabelMap.PartyUserLoginId}</div></td>
          <td>&nbsp;</td>
          <td valign='middle'>
            <div class='tabletext' valign='top'>
              <input type='text' class='inputBox' name='userLoginId' value='${parameters.userLogin.userLoginId}'/>
              <a href="javascript:call_fieldlookup2(document.poentryform.userLoginId,'LookupUserLoginAndPartyDetails');">
                <img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt="${uiLabelMap.CommonClickHereForFieldLookup}"/>
              </a>
            </div>
          </td>
        </tr>
        <tr>
          <td>&nbsp;</td>
          <td align='right' valign='middle' nowrap><div class='tableheadtext'>${uiLabelMap.PartySupplier}</div></td>
          <td>&nbsp;</td>
          <td valign='middle'>
            <div class='tabletext' valign='top'>
              <select name="supplierPartyId"<#if sessionAttributes.orderMode?default("") == "SALES_ORDER"> disabled</#if>>
                <option value="">${uiLabelMap.OrderSelectSupplier}</option>
                <#list suppliers as supplier>
                  <option value="${supplier.partyId}"<#if supplier.partyId == thisPartyId> selected</#if>>[${supplier.partyId}] - ${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(supplier, true)}</option>
                </#list>
              </select>
            </div>
          </td>
        </tr>
      </table>
      </form>
    </div>
  </div>
  </#if>
</#if>
