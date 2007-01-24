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

<div class="screenlet">
    <div class="screenlet-header">
        <#--
        <div class="boxlink">
            <a href="<@ofbizUrl>main</@ofbizUrl>" class="lightbuttontext">[${uiLabelMap.OrderBackHome}]</a>
        </div>
        -->
        <div class="boxhead">${uiLabelMap.OrderHistory}</div>
    </div>
    <div class="screenlet-body">
        <table width="100%" cellpadding="1" cellspacing="0" border="0">
          <tr>
            <td width="30%">
              <div class="tabletext"><b>${uiLabelMap.CommonDate}</b></div>
            </td>
            <td width="10">&nbsp;</td>
            <td width="15%">
              <div class="tabletext"><b><span style="white-space: nowrap;">${uiLabelMap.OrderOrder} ${uiLabelMap.OrderNbr}</span></b></div>
            </td>
            <td width="10">&nbsp;</td>
            <td width="15%">
              <div class="tabletext"><b>${uiLabelMap.CommonAmount}</b></div>
            </td>
            <td width="10">&nbsp;</td>
            <td width="15%">
              <div class="tabletext"><b>${uiLabelMap.CommonStatus}</b></div>
            </td>
            <td width="10">&nbsp;</td>
            <td width="15%"><b></b></td>                
          </tr>
          <#list orderHeaderList as orderHeader>
            <#assign status = orderHeader.getRelatedOneCache("StatusItem")>                               
            <tr><td colspan="9"><hr class="sepbar"/></td></tr>
            <tr>
              <td>
                <div class="tabletext"><span style="white-space: nowrap;">${orderHeader.orderDate.toString()}</span></div>
              </td>
              <td width="10">&nbsp;</td>
              <td>
                <div class="tabletext">${orderHeader.orderId}</div>
              </td>
              <td width="10">&nbsp;</td>
              <td>
                <div class="tabletext"><@ofbizCurrency amount=orderHeader.grandTotal isoCode=orderHeader.currencyUom/></div>
              </td>
              <td width="10">&nbsp;</td>
              <td>
                <div class="tabletext">${status.get("description",locale)}</div>
              </td>
              <td width="10">&nbsp;</td>
              <td align="right">
                <a href="<@ofbizUrl>orderstatus?orderId=${orderHeader.orderId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonView}</a>
              </td>
            </tr>
          </#list>
          <#if !orderHeaderList?has_content>
            <tr><td colspan="9"><div class="head3">${uiLabelMap.OrderNoOrderFound}</div></td></tr>
          </#if>
        </table>
    </div>
</div>

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">${uiLabelMap.EcommerceDownloadsAvailableTitle}</div>
    </div>
    <div class="screenlet-body">
        <table width="100%" cellpadding="1" cellspacing="0" border="0">
          <tr>
            <td width="10%">
              <div class="tabletext"><b><span style="white-space: nowrap;">${uiLabelMap.OrderOrder} ${uiLabelMap.OrderNbr}</span></b></div>
            </td>
            <td width="10">&nbsp;</td>
            <td width="20%">
              <div class="tabletext"><b>${uiLabelMap.ProductProductName}</b></div>
            </td>
            <td width="10">&nbsp;</td>
            <td width="10%">
              <div class="tabletext"><b>${uiLabelMap.CommonName}</b></div>
            </td>
            <td width="10">&nbsp;</td>
            <td width="40%">
              <div class="tabletext"><b>${uiLabelMap.CommonDescription}</b></div>
            </td>
            <td width="10">&nbsp;</td>
            <td width="10%"><b></b></td>                
          </tr>
          <#list downloadOrderRoleAndProductContentInfoList as downloadOrderRoleAndProductContentInfo>
            <tr><td colspan="9"><hr class="sepbar"/></td></tr>
            <tr>
              <td>
                <div class="tabletext">${downloadOrderRoleAndProductContentInfo.orderId}</div>
              </td>
              <td width="10">&nbsp;</td>
              <td>
                <div class="tabletext">${downloadOrderRoleAndProductContentInfo.productName}</div>
              </td>
              <td width="10">&nbsp;</td>
              <td>
                <div class="tabletext">${downloadOrderRoleAndProductContentInfo.contentName?if_exists}</div>
              </td>
              <td width="10">&nbsp;</td>
              <td>
                <div class="tabletext">${downloadOrderRoleAndProductContentInfo.description?if_exists}</div>
              </td>
              <td width="10">&nbsp;</td>
              <td align="right">
                <a href="<@ofbizUrl>downloadDigitalProduct/${downloadOrderRoleAndProductContentInfo.contentName?if_exists}?dataResourceId=${downloadOrderRoleAndProductContentInfo.dataResourceId}</@ofbizUrl>" class="buttontext">Download</a>
              </td>
            </tr>
          </#list>
          <#if !downloadOrderRoleAndProductContentInfoList?has_content>
            <tr><td colspan="9"><div class="head3">${uiLabelMap.EcommerceDownloadNotFound}</div></td></tr>
          </#if>
        </table>
    </div>
</div>
