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

<div class='tabContainer'>
  <#if security.hasEntityPermission("PAYPROC", "_VIEW", session)>
  <a href="<@ofbizUrl>paysetup</@ofbizUrl>" class='tabButtonSelected'>Payment&nbsp;Setup</a>
  </#if>
</div>

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
              <tr class="viewOneTR1">
                <td nowrap><div class="tableheadtext">WebSite</div></td>
                <td nowrap><div class="tableheadtext">PayMethod Type</div></td>
                <td nowrap><div class="tableheadtext">Auth Service</div></td>
                <td nowrap><div class="tableheadtext">Re-Auth Service</td>
                <td nowrap><div class="tableheadtext">Capture Service</div></td>
                <td nowrap><div class="tableheadtext">Refund Service</td>
                <td nowrap><div class="tableheadtext">Payment Config</div></td>               
                <td nowrap><div class="tableheadtext">&nbsp;</div></td>
              </tr>             
              <#if paymentSetups?has_content>
                <#list paymentSetups as paymentSetting>
                  <#if rowStyle?exists && rowStyle == "viewManyTR1">
                    <#assign rowStyle = "viewManyTR2">
                  <#else>
                    <#assign rowStyle = "viewManyTR1">
                  </#if>
                  <tr class="${rowStyle}">
                    <td><div class="tabletext">${paymentSetting.siteName?if_exists}</div></td>
                    <td><div class="tabletext">${paymentSetting.description?if_exists}</div></td>
                    <td><div class="tabletext">${paymentSetting.paymentAuthService?if_exists}</div></td>
                    <td><div class="tabletext">${paymentSetting.paymentReAuthService?if_exists}</div></td>
                    <td><div class="tabletext">${paymentSetting.paymentCaptureService?if_exists}</div></td>
                    <td><div class="tabletext">${paymentSetting.paymentRefundService?if_exists}</div></td>
                    <td><div class="tabletext">${paymentSetting.paymentConfiguration?if_exists}</div></td>                
                    <td nowrap>
                      <div class="tabletext">&nbsp;
                        <#if security.hasEntityPermission("PAYPROC", "_UPDATE", session)>
                        <a href="<@ofbizUrl>paysetup?webSiteId=${paymentSetting.webSiteId?if_exists}&paymentMethodTypeId=${paymentSetting.paymentMethodTypeId?if_exists}</@ofbizUrl>" class="buttontext">Edit</a>&nbsp;
                        </#if>
                        <#if security.hasEntityPermission("PAYPROC", "_DELETE", session)>
                        <a href="<@ofbizUrl>removeWebSitePaymentSetting?webSiteId=${paymentSetting.webSiteId?if_exists}&paymentMethodTypeId=${paymentSetting.paymentMethodTypeId?if_exists}</@ofbizUrl>" class="buttontext">Remove</a>&nbsp;
                        </#if>
                      </div>
                    </td>
                  </tr>
                </#list>
              <#else>
                <tr>
                  <td colspan="8"><div class="tabletext">No settings found.</div></td>
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
<br/>
<table border="0" width='100%' cellpadding='0' cellspacing=0 class='boxoutside'>
  <tr>
    <td width='100%'>
      <table width='100%' border='0' cellpadding='0' cellspacing='0' class='boxtop'>
        <tr>
          <td align="left" width='90%'>
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
                <td width="26%" align="right"><div class="tabletext">WebSite</div></td>
                <td>&nbsp;</td>
                <td width="74%">
                  <#if webSitePayment?has_content>
                    <input type='hidden' name='webSiteId' value='${webSitePayment.webSiteId}'>
                    <div class="tabletext">
                      <b>${webSitePayment.siteName}</b> (This cannot be changed without re-creating the setting.)
                    </div>
                  <#else>                    
                    <select name="webSiteId" class="selectBox">
                      <#list webSites as nextWebSite>                                         
                        <option value='${nextWebSite.webSiteId}'>${nextWebSite.siteName}</option>
                      </#list>
                    </select>
                  </#if>                  
                </td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">Payment Method Type</div></td>
                <td>&nbsp;</td>
                <td width="74%">
                  <#if webSitePayment?has_content>
                    <input type='hidden' name='paymentMethodTypeId' value='${webSitePayment.paymentMethodTypeId}'>
                    <div class="tabletext">
                      <b>${webSitePayment.description}</b> (This cannot be changed without re-creating the setting.)
                    </div>
                  <#else>
                    <select name="paymentMethodTypeId" class="selectBox">
                      <#list paymentMethodTypes as nextPayType>
                        <option value='${nextPayType.paymentMethodTypeId}'>${nextPayType.description}</option>
                      </#list>
                    </select>
                  </#if>
                </td>
              </tr>
              
              <tr>
                <td width="26%" align="right"><div class="tabletext">Processor Auth Service</div></td>
                <td>&nbsp;</td>
                <td width="74%"><input type="text" class="inputBox" name="paymentAuthService" value="${payInfo.paymentAuthService?if_exists}" size="30" maxlength="60"></td>
              </tr>  
              <tr>
                <td width="26%" align="right"><div class="tabletext">Processor Re-Auth Service</div></td>
                <td>&nbsp;</td>
                <td width="74%"><input type="text" class="inputBox" name="paymentReAuthService" value="${payInfo.paymentReAuthService?if_exists}" size="30" maxlength="60"></td>
              </tr>                      
              <tr>
                <td width="26%" align="right"><div class="tabletext">Processor Capture Service</div></td>
                <td>&nbsp;</td>
                <td width="74%"><input type="text" class="inputBox" name="paymentCaptureService" value="${payInfo.paymentCaptureService?if_exists}" size="30" maxlength="60"></td>
              </tr> 
              <tr>
                <td width="26%" align="right"><div class="tabletext">Processor Refund Service</div></td>
                <td>&nbsp;</td>
                <td width="74%"><input type="text" class="inputBox" name="paymentRefundService" value="${payInfo.paymentRefundService?if_exists}" size="30" maxlength="60"></td>
              </tr>                                            
              <tr>
                <td width="26%" align="right"><div class="tabletext">Processor Properties URL</div></td>
                <td>&nbsp;</td>
                <td width="74%"><input type="text" class="inputBox" name="paymentConfiguration" value="${payInfo.paymentConfiguration?if_exists}" size="30" maxlength="60"></td>
              </tr>  
              <tr>
                <td colspan='2'>&nbsp;</td>
                <td colspan='1' align="left"><input type="submit" value="Update"></td>
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
  <br/>
  <h3>You do not have permission to view this page. ("PAYSETUP_VIEW" or "PAYSETUP_ADMIN" needed)</h3>
</#if>
