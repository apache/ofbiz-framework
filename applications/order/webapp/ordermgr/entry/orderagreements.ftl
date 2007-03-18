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
  <div class="screenlet-header">
      <div class="boxtop">
          <div class="boxhead-right" align="right">
              <input type="submit" class="smallSubmit" value="${uiLabelMap.CommonContinue}">
          </div>
          <div class="boxhead-left">
              &nbsp;${uiLabelMap.OrderOrderEntryCurrencyAgreementShipDates}
          </div>
          <div class="boxhead-fill">&nbsp;</div>
      </div>
  </div>
  <div class="screenlet-body">
    <table>
      <tr><td colspan="4">&nbsp;</td></tr>

      <#if agreements?exists>
      <input type='hidden' name='hasAgreements' value='Y'/>
      <tr>
        <td>&nbsp;</td>
        <td align='right' valign='top' nowrap>
          <div class='tableheadtext'>
            ${uiLabelMap.OrderSelectAgreement}
          </div>
        </td>
        <td>&nbsp;</td>
        <td valign='middle'>
          <div class='tabletext' valign='top'>
            <select name="agreementId" class="inputBox">
            <option value="">${uiLabelMap.CommonNone}</option>
            <#list agreements as agreement>
            <option value='${agreement.agreementId}' >${agreement.agreementId} - ${agreement.description?if_exists}</option>
            </#list>
            </select>
          </div>
        </td>
      </tr>

      <#else><input type='hidden' name='hasAgreements' value='N'/>
      </#if>

      <tr>
        <td>&nbsp;</td>
        <td align='right' valign='middle' class='tableheadtext' nowrap>
           ${uiLabelMap.OrderOrderName}
        </td>
        <td>&nbsp;</td>
        <td align='left'>
          <input type='text' class="inputBox" size='60' maxlength='100' name='orderName'/>
        </td>
      </tr>
      
      <#if cart.getOrderType() != "PURCHASE_ORDER">
      <tr>
        <td>&nbsp;</td>
        <td align='right' valign='middle' class='tableheadtext' nowrap>
          ${uiLabelMap.OrderPONumber}
        </td>
        <td>&nbsp;</td>
        <td align='left'>
          <input type="text" class='inputBox' name="correspondingPoId" size="15">
        </td>
      </tr>                                                           
      </#if>

      <tr>
        <td>&nbsp;</td>
        <td align='right' valign='middle' nowrap>
          <div class='tableheadtext'>
            <#if agreements?exists>${uiLabelMap.OrderSelectCurrencyOr}
            <#else>${uiLabelMap.OrderSelectCurrency}
            </#if>
          </div>
        </td>
        <td>&nbsp;</td>
        <td valign='middle'>
          <div class='tabletext' valign='top'>
            <select class="selectBox" name="currencyUomId">
              <option value=""></option>
              <#list currencies as currency>
              <option value="${currency.uomId}" <#if (defaultCurrencyUomId?has_content) && (currency.uomId == defaultCurrencyUomId)>selected</#if>>${currency.uomId}</option>
              </#list>
            </select>
          </div>
        </td>
      </tr>

      <tr>
        <td>&nbsp;</td>
        <td align='right' valign='top' nowrap>
          <div class='tableheadtext'>
            ${uiLabelMap.OrderShipAfterDateDefault}
          </div>
        </td>
        <td>&nbsp;</td>
        <td><input type="text" name="shipAfterDate" size="20" maxlength="30" class="inputBox"/>
          <a href="javascript:call_cal(document.agreementForm.shipAfterDate,'');">
            <img src='/images/cal.gif' width='16' height='16' border='0' alt='Calendar'/>
          </a>
        </td>
      </tr>

      <tr>
        <td>&nbsp;</td>
        <td align='right' valign='top' nowrap>
          <div class='tableheadtext'>
            ${uiLabelMap.OrderShipBeforeDateDefault}
          </div>
        </td>
        <td>&nbsp;</td>
        <td><input type="text" name="shipBeforeDate" size="20" maxlength="30" class="inputBox"/>
          <a href="javascript:call_cal(document.agreementForm.shipBeforeDate,'');">
            <img src='/images/cal.gif' width='16' height='16' border='0' alt='Calendar'/>
          </a>
        </td>
      </tr>
    </table>
  </div>
</div>
</form>
