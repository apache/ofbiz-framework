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


<form method="post" name="agreementForm" action="<@ofbizUrl>setOrderCurrencyAgreementShipDates</@ofbizUrl>">
<div class="screenlet">
  <div class="screenlet-title-bar">
      <ul>
        <li class="h3">${uiLabelMap.OrderOrderEntryCurrencyAgreementShipDates}</li>
        <li><a href="javascript:document.agreementForm.submit()">${uiLabelMap.CommonContinue}</a></li>
      </ul>
      <br class="clear" />
  </div>
  <div class="screenlet-body">
    <table>

      <#if agreements??>
      <tr><td colspan="4">&nbsp;<input type='hidden' name='hasAgreements' value='Y'/></td></tr>
      
      <tr>
        <td>&nbsp;</td>
        <td align='right' valign='top' nowrap="nowrap">
          <div class='tableheadtext'>
            ${uiLabelMap.OrderSelectAgreement}
          </div>
        </td>
        <td>&nbsp;</td>
        <td valign='middle'>
          <div class='tabletext' valign='top'>
            <select name="agreementId">
            <option value="">${uiLabelMap.CommonNone}</option>
            <#list agreements as agreement>
            <option value='${agreement.agreementId}' >${agreement.agreementId} - ${agreement.description!}</option>
            </#list>
            </select>
          </div>
        </td>
      </tr>
      <#else>
      <tr><td colspan="4">&nbsp;<input type='hidden' name='hasAgreements' value='N'/></td></tr>
      </#if>
      <#if agreementRoles??>
        <tr>
          <td>&nbsp;</td>
          <td align='right' valign='top' nowrap="nowrap">
            <div class='tableheadtext'>
              ${uiLabelMap.OrderSelectAgreementRoles}
            </div>
          </td>
          <td>&nbsp;</td>
          <td valign='middle'>
            <div class='tabletext'>
              <select name="agreementId">
              <option value="">${uiLabelMap.CommonNone}</option>
              <#list agreementRoles as agreementRole>
                  <option value='${agreementRole.agreementId!}' >${agreementRole.agreementId!} - ${agreementRole.roleTypeId!}</option>
              </#list>
              </select>
            </div>
          </td>
        </tr>
      </#if>

      <#if "PURCHASE_ORDER" == cart.getOrderType()>
        <tr>
          <td>&nbsp;</td>
          <td align='right' valign='middle' class='tableheadtext'>
            ${uiLabelMap.OrderOrderId}
          </td>
          <td>&nbsp;</td>
          <td align='left'>
            <input type='text' size='15' maxlength='100' name='orderId' value=""/>
          </td>
        </tr>
      </#if>

      <tr>
        <td>&nbsp;</td>
        <td align='right' valign='middle' class='tableheadtext' nowrap="nowrap">
           ${uiLabelMap.OrderOrderName}
        </td>
        <td>&nbsp;</td>
        <td align='left'>
          <input type='text' size='60' maxlength='100' name='orderName'/>
        </td>
      </tr>

      <#if cart.getOrderType() != "PURCHASE_ORDER">
      <tr>
        <td>&nbsp;</td>
        <td align='right' valign='middle' class='tableheadtext' nowrap="nowrap">
          ${uiLabelMap.OrderPONumber}
        </td>
        <td>&nbsp;</td>
        <td align='left'>
          <input type="text" class='inputBox' name="correspondingPoId" size="15" />
        </td>
      </tr>
      </#if>

      <tr>
        <td>&nbsp;</td>
        <td align='right' valign='middle' nowrap="nowrap">
          <div class='tableheadtext'>
            <#if agreements??>${uiLabelMap.OrderSelectCurrencyOr}
            <#else>${uiLabelMap.OrderSelectCurrency}
            </#if>
          </div>
        </td>
        <td>&nbsp;</td>
        <td valign='middle'>
          <div class='tabletext'>
            <select name="currencyUomId">
              <option value=""></option>
              <#list currencies as currency>
              <option value="${currency.uomId}" <#if currencyUomId?default('') == currency.uomId>selected="selected"</#if> >${currency.uomId}</option>
              </#list>
            </select>
          </div>
        </td>
      </tr>

      <tr>
        <td>&nbsp;</td>
        <td align="right">
          ${uiLabelMap.ProductChooseCatalog}
        </td>
        <td>&nbsp;</td>
        <td>
           <#if catalogCol?has_content>
           <select name='CURRENT_CATALOG_ID'>
            <#list catalogCol! as catalogId>
              <#assign thisCatalogName = Static["org.ofbiz.product.catalog.CatalogWorker"].getCatalogName(request, catalogId)>
              <option value="${catalogId}" <#if currentCatalogId?default('') == catalogId>selected="selected"</#if> >${thisCatalogName}</option>
            </#list>
          </select>
          <#else>
             <input type="hidden" name='CURRENT_CATALOG_ID' value=""/> 
          </#if>
        </td>
      </tr>

      <tr>
        <td>&nbsp;</td>
        <td align="right">
          ${uiLabelMap.WorkEffortWorkEffortId}
        </td>
        <td>&nbsp;</td>
        <td>
          <@htmlTemplate.lookupField formName="agreementForm" name="workEffortId" id="workEffortId" fieldFormName="LookupWorkEffort"/>
        </td>
      </tr>

      <tr>
        <td>&nbsp;</td>
        <td align='right' valign='top' nowrap="nowrap">
          <div class='tableheadtext'>
            ${uiLabelMap.OrderShipAfterDateDefault}
          </div>
        </td>
        <td>&nbsp;</td>
        <td>
            <@htmlTemplate.renderDateTimeField name="shipAfterDate" event="" action="" value="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" size="25" maxlength="30" id="shipAfterDate1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
        </td>
      </tr>

      <tr>
        <td>&nbsp;</td>
        <td align='right' valign='top' nowrap="nowrap">
          <div class='tableheadtext'>
            ${uiLabelMap.OrderShipBeforeDateDefault}
          </div>
        </td>
        <td>&nbsp;</td>
        <td>
            <@htmlTemplate.renderDateTimeField name="shipBeforeDate" event="" action="" value="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" size="25" maxlength="30" id="shipBeforeDate1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
        </td>
      </tr>

      <#if cart.getOrderType() == "PURCHASE_ORDER">
        <tr>
          <td>&nbsp;</td>
          <td align='right' valign='top'>
            <div class='tableheadtext'>
              ${uiLabelMap.FormFieldTitle_cancelBackOrderDate}
            </div>
          </td>
          <td>&nbsp;</td>
          <td>
              <@htmlTemplate.renderDateTimeField name="cancelBackOrderDate" event="" action="" value="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" size="25" maxlength="30" id="cancelBackOrderDate1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
          </td>
        </tr>
      </#if>

    </table>
  </div>
</div>
</form>
