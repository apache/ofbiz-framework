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

<#if security.hasRolePermission("ORDERMGR", "_VIEW", "", "", session)>

  <div class="screenlet">
    <div class="screenlet-title-bar">
      <h3>${uiLabelMap.OrderOrderStatisticsPage}</h3>
    </div>
    <#--<div class='head3'>${uiLabelMap.OrderOrderStatisticsPage}</div>-->
    <table class="basic-table" cellspacing='0'>
      <tr>
        <th>&nbsp;</th>
        <th>&nbsp;</th>
        <th>${uiLabelMap.CommonToday}</th>
        <th>${uiLabelMap.OrderWTD}</th>
        <th>${uiLabelMap.OrderMTD}</th>
        <th>${uiLabelMap.OrderYTD}</th>      
      </tr>
      <tr><td colspan="6"><hr/></td></tr>
      <tr>
        <td colspan="6"><b>${uiLabelMap.OrderOrdersTotals}</b></td>
      </tr>
      <tr>
        <td>&nbsp;</td>
        <td>${uiLabelMap.OrderGrossDollarAmountsIncludesAdjustmentsAndPendingOrders}</td>
        <td>${dayItemTotal}</td>
        <td>${weekItemTotal}</td>
        <td>${monthItemTotal}</td>
        <td>${yearItemTotal}</td>
      </tr>
      <tr>
        <td>&nbsp;</td>
        <td>${uiLabelMap.OrderPaidDollarAmountsIncludesAdjustments}</td>
        <td>${dayItemTotalPaid}</td>
        <td>${weekItemTotalPaid}</td>
        <td>${monthItemTotalPaid}</td>
        <td>${yearItemTotalPaid}</td>
      </tr>
      <tr>
        <td>&nbsp;</td>
        <td>${uiLabelMap.OrderPendingPaymentDollarAmountsIncludesAdjustments}</td>
        <td>${dayItemTotalPending}</td>
        <td>${weekItemTotalPending}</td>
        <td>${monthItemTotalPending}</td>
        <td>${yearItemTotalPending}</td>
      </tr>
      <tr><td colspan="6"><hr/></td></tr>
      <tr>
        <td colspan="6"><b>${uiLabelMap.OrderOrdersItemCounts}</b></td>
      </tr>
      <tr>
        <td>&nbsp;</td>
        <td>${uiLabelMap.OrderGrossItemsSoldIncludesPromotionsAndPendingOrders}</td>
        <td>${dayItemCount?string.number}</td>
        <td>${weekItemCount?string.number}</td>
        <td>${monthItemCount?string.number}</td>
        <td>${yearItemCount?string.number}</td>
      </tr>
      <tr>
        <td>&nbsp;</td>
        <td>${uiLabelMap.OrderPaidItemsSoldIncludesPromotions}</td>
        <td>${dayItemCountPaid?string.number}</td>
        <td>${weekItemCountPaid?string.number}</td>
        <td>${monthItemCountPaid?string.number}</td>
        <td>${yearItemCountPaid?string.number}</td>
      </tr>      
      <tr>
        <td>&nbsp;</td>
        <td>${uiLabelMap.OrderPendingPaymentItemsSoldIncludesPromotions}</td>
        <td>${dayItemCountPending?string.number}</td>
        <td>${weekItemCountPending?string.number}</td>
        <td>${monthItemCountPending?string.number}</td>
        <td>${yearItemCountPending?string.number}</td>
      </tr>      
      <tr><td colspan="6"><hr/></td></tr>
      <tr>
        <td colspan="6"><b>${uiLabelMap.OrderOrdersPending}</b></td>
      </tr>
      <tr>
        <td>&nbsp;</td>
        <td>${uiLabelMap.OrderWaitingPayment}</td>
        <td>${waitingPayment?default(0)?string.number}</td>
        <td>--</td>
        <td>--</td>
        <td>--</td>
      </tr>
      <tr>
        <td>&nbsp;</td>
        <td>${uiLabelMap.OrderWaitingApproval}</td>
        <td>${waitingApproval?default(0)?string.number}</td>
        <td>--</td>
        <td>--</td>
        <td>--</td>
      </tr> 
      <tr>
        <td>&nbsp;</td>
        <td>${uiLabelMap.OrderWaitingCompletion}</td>
        <td>${waitingComplete?default(0)?string.number}</td>
        <td>--</td>
        <td>--</td>
        <td>--</td>
      </tr>             
      <tr><td colspan="6"><hr/></td></tr>
      <tr>
        <td colspan="6"><b>${uiLabelMap.OrderStatusChanges}</b></td>
      </tr>
      <tr>
        <td>&nbsp;</td>
        <td>${uiLabelMap.OrderOrdered}</td>
        <td>${dayOrder?size?default(0)?string.number}</td>
        <td>${weekOrder?size?default(0)?string.number}</td>
        <td>${monthOrder?size?default(0)?string.number}</td>
        <td>${yearOrder?size?default(0)?string.number}</td>
      </tr>
      <tr>
        <td>&nbsp;</td>
        <td>${uiLabelMap.OrderApproved}</td>
        <td>${dayApprove?size?default(0)?string.number}</td>
        <td>${weekApprove?size?default(0)?string.number}</td>
        <td>${monthApprove?size?default(0)?string.number}</td>
        <td>${yearApprove?size?default(0)?string.number}</td>
      </tr>      
      <tr>
        <td>&nbsp;</td>
        <td>${uiLabelMap.OrderCompleted}</td>
        <td>${dayComplete?size?default(0)?string.number}</td>
        <td>${weekComplete?size?default(0)?string.number}</td>
        <td>${monthComplete?size?default(0)?string.number}</td>
        <td>${yearComplete?size?default(0)?string.number}</td>
      </tr>      
      <tr>
        <td>&nbsp;</td>
        <td>${uiLabelMap.OrderCancelled}</td>
        <td>${dayCancelled?size?default(0)?string.number}</td>
        <td>${weekCancelled?size?default(0)?string.number}</td>
        <td>${monthCancelled?size?default(0)?string.number}</td>
        <td>${yearCancelled?size?default(0)?string.number}</td>
      </tr>  
      <tr>
        <td>&nbsp;</td>
        <td>${uiLabelMap.OrderRejected}</td>
        <td>${dayRejected?size?default(0)?string.number}</td>
        <td>${weekRejected?size?default(0)?string.number}</td>
        <td>${monthRejected?size?default(0)?string.number}</td>
        <td>${yearRejected?size?default(0)?string.number}</td>
      </tr>                         
    </table>
  </div>
<#else>
  <h3>${uiLabelMap.OrderViewPermissionError}</h3>
</#if>
