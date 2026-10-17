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

<#-- TODO: Convert hard-coded text to UI label properties -->

<#if security.hasEntityPermission("PAYPROC", "_VIEW", session)>
  <div class='button-bar button-style-1'>
    <a href="<@ofbizUrl>paysetup</@ofbizUrl>" class='selected'>Payment&nbsp;Setup</a>
  </div>
</#if>

<#if security.hasEntityPermission("PAYPROC", "_VIEW", session)>
<table border="0" width='100%' cellpadding='0' cellspacing=0 class='boxoutside'>
  <tr>
    <td width='100%'>
      <table width='100%' border='0' cellpadding='0' cellspacing='0' class='boxtop'>
        <tr>
          <td>
            <div class='boxhead'>&nbsp;Payment Processor Setup</div>
          </td>
        </tr>
      </table>
    </td>
  </tr>
  <tr>
    <td width='100%'>
      <table width='100%' border='0' cellpadding='0' cellspacing='0' class='boxbottom'>
        <tr>
          <td>
            <table width="100%" cellpadding="2" cellspacing="2" border="0">
              <tr class="header-row">
                <td nowrap="nowrap"><div>WebSite</div></td>
                <td nowrap="nowrap"><div>PayMethod Type</div></td>
                <td nowrap="nowrap"><div>Auth Service</div></td>
                <td nowrap="nowrap"><div>Re-Auth Service</td>
                <td nowrap="nowrap"><div>Capture Service</div></td>
                <td nowrap="nowrap"><div>Refund Service</td>
                <td nowrap="nowrap"><div>Payment Config</div></td>
                <td nowrap="nowrap"><div>&nbsp;</div></td>
              </tr>
              <#if paymentSetups?has_content>
                <#list paymentSetups as paymentSetting>
                  <#if rowStyle?? && "alternate-row" == rowStyle>
                    <#assign rowStyle = "alternate-rowSelected">
                  <#else>
                    <#assign rowStyle = "alternate-row">
                  </#if>
                  <tr class="${rowStyle}">
                    <td><div>${paymentSetting.siteName!}</div></td>
                    <td><div>${paymentSetting.description!}</div></td>
                    <td><div>${paymentSetting.paymentAuthService!}</div></td>
                    <td><div>${paymentSetting.paymentReAuthService!}</div></td>
                    <td><div>${paymentSetting.paymentCaptureService!}</div></td>
                    <td><div>${paymentSetting.paymentRefundService!}</div></td>
                    <td><div>${paymentSetting.paymentConfiguration!}</div></td>
                    <td nowrap="nowrap">
                      <div>&nbsp;
                        <#if security.hasEntityPermission("PAYPROC", "_UPDATE", session)>
                        <a href="<@ofbizUrl>paysetup?webSiteId=${paymentSetting.webSiteId!}&amp;paymentMethodTypeId=${paymentSetting.paymentMethodTypeId!}</@ofbizUrl>" class="buttontext">Edit</a>&nbsp;
                        </#if>
                        <#if security.hasEntityPermission("PAYPROC", "_DELETE", session)>
                        <a href="<@ofbizUrl>removeWebSitePaymentSetting?webSiteId=${paymentSetting.webSiteId!}&amp;paymentMethodTypeId=${paymentSetting.paymentMethodTypeId!}</@ofbizUrl>" class="buttontext">Remove</a>&nbsp;
                        </#if>
                      </div>
                    </td>
                  </tr>
                </#list>
              <#else>
                <tr>
                  <td colspan="8"><div>No settings found.</div></td>
                </tr>
              </#if>
            </table>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>

<#if security.hasEntityPermission("PAYPROC", "_CREATE", session)>
<br />
<table border="0" width='100%' cellpadding='0' cellspacing=0 class='boxoutside'>
  <tr>
    <td width='100%'>
      <table width='100%' border='0' cellpadding='0' cellspacing='0' class='boxtop'>
        <tr>
          <td width='90%'>
            <#if webSitePayment?has_content>
              <div class='boxhead'>&nbsp;Update&nbsp;Setting</div>
            <#else>
              <div class='boxhead'>&nbsp;Add&nbsp;New&nbsp;Setting</div>
            </#if>
          </td>
          <#if webSitePayment?has_content>
            <td align='right' width='10%'><a href="<@ofbizUrl>paysetup</@ofbizUrl>" class="lightbuttontext">Add New</a></td>
          <#else>
            <td align='right' width='10%'></td>
          </#if>
        </tr>
      </table>
    </td>
  </tr>
  <tr>
    <td width='100%'>
      <table width='100%' border='0' cellpadding='0' cellspacing='0' class='boxbottom'>
        <tr>
          <td>
            <#if webSitePayment?has_content>
              <form method="post" action="<@ofbizUrl>updateWebSitePaymentSetting</@ofbizUrl>">
            <#else>
              <form method="post" action="<@ofbizUrl>createWebSitePaymentSetting</@ofbizUrl>">
            </#if>
            <table border='0' cellpadding='2' cellspacing='0'>
              <tr>
                <td width="26%" align="right"><div>WebSite</div></td>
                <td>&nbsp;</td>
                <td width="74%">
                  <#if webSitePayment?has_content>
                    <input type='hidden' name='webSiteId' value='${webSitePayment.webSiteId}' />
                    <div>
                      <b>${webSitePayment.siteName}</b> (This cannot be changed without re-creating the setting.)
                    </div>
                  <#else>
                    <select name="webSiteId">
                      <#list webSites as nextWebSite>
                        <option value='${nextWebSite.webSiteId}'>${nextWebSite.siteName}</option>
                      </#list>
                    </select>
                  </#if>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right"><div>Payment Method Type</div></td>
                <td>&nbsp;</td>
                <td width="74%">
                  <#if webSitePayment?has_content>
                    <input type='hidden' name='paymentMethodTypeId' value='${webSitePayment.paymentMethodTypeId}' />
                    <div>
                      <b>${webSitePayment.description}</b> (This cannot be changed without re-creating the setting.)
                    </div>
                  <#else>
                    <select name="paymentMethodTypeId">
                      <#list paymentMethodTypes as nextPayType>
                        <option value='${nextPayType.paymentMethodTypeId}'>${nextPayType.description}</option>
                      </#list>
                    </select>
                  </#if>
                </td>
              </tr>

              <tr>
                <td width="26%" align="right"><div>Processor Auth Service</div></td>
                <td>&nbsp;</td>
                <td width="74%"><input type="text" name="paymentAuthService" value="${payInfo.paymentAuthService!}" size="30" maxlength="60" /></td>
              </tr>
              <tr>
                <td width="26%" align="right"><div>Processor Re-Auth Service</div></td>
                <td>&nbsp;</td>
                <td width="74%"><input type="text" name="paymentReAuthService" value="${payInfo.paymentReAuthService!}" size="30" maxlength="60" /></td>
              </tr>
              <tr>
                <td width="26%" align="right"><div>Processor Capture Service</div></td>
                <td>&nbsp;</td>
                <td width="74%"><input type="text" name="paymentCaptureService" value="${payInfo.paymentCaptureService!}" size="30" maxlength="60" /></td>
              </tr>
              <tr>
                <td width="26%" align="right"><div>Processor Refund Service</div></td>
                <td>&nbsp;</td>
                <td width="74%"><input type="text" name="paymentRefundService" value="${payInfo.paymentRefundService!}" size="30" maxlength="60" /></td>
              </tr>
              <tr>
                <td width="26%" align="right"><div>Processor Properties URL</div></td>
                <td>&nbsp;</td>
                <td width="74%"><input type="text" name="paymentConfiguration" value="${payInfo.paymentConfiguration!}" size="30" maxlength="60" /></td>
              </tr>
              <tr>
                <td colspan='2'>&nbsp;</td>
                <td colspan='1'><input type="submit" value="${uiLabelMap.CommonUpdate}" /></td>
              </tr>
            </table>
            </form>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>
</#if>

<#else>
  <br />
  <h3>You do not have permission to view this page. ("PAYSETUP_VIEW" or "PAYSETUP_ADMIN" needed)</h3>
</#if>
