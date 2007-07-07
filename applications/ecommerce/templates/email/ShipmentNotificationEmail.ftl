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


<table cellpadding="0" cellspacing="0" width="80%" border="0" class="boxoutside">
   <tr>
      <td colspan="3" class="tableheadtext">
         This email is to inform you that your shipment has been completed.
      </td>
   </tr>   
   <tr><td colspan="3">&nbsp;</td></tr>  
   <#if orderItemShipGroups?has_content>
   <tr>
      <td colspan="3">
         <h3>${uiLabelMap.OrderShippingInformation}</h3>
      </td>
   </tr>   
   <#assign groupIdx = 0>
   <#list orderItemShipGroups as shipGroup>
      <#-- tracking number -->
      <#if trackingNumber?has_content || orderShipmentInfoSummaryList?has_content>
      <tr>
         <td align="right" valign="top" width="15%" nowrap>
            <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderTrackingNumber}</b></div>
         </td>
         <td width="5">&nbsp;</td>
         <td align="left" valign="top" width="80%">
            <#-- TODO: add links to UPS/FEDEX/etc based on carrier partyId  -->
            <#if shipGroup.trackingNumber?has_content>
               <div class="tabletext">${shipGroup.trackingNumber}</div>
            </#if>
            <#if orderShipmentInfoSummaryList?has_content>
               <#list orderShipmentInfoSummaryList as orderShipmentInfoSummary>
                  <div class="tabletext">
                     <#if (orderShipmentInfoSummaryList?size > 1)>${orderShipmentInfoSummary.shipmentPackageSeqId}: </#if>
                        Code: ${orderShipmentInfoSummary.trackingCode?default("[Not Yet Known]")}
                     <#if orderShipmentInfoSummary.boxNumber?has_content>${uiLabelMap.OrderBoxNubmer}${orderShipmentInfoSummary.boxNumber}</#if> 
                     <#if orderShipmentInfoSummary.carrierPartyId?has_content>(${uiLabelMap.ProductCarrier}: ${orderShipmentInfoSummary.carrierPartyId})</#if>
                  </div>
               </#list>
            </#if>
         </td>
      </tr>
      </#if>
      <#assign groupIdx = groupIdx + 1>
   </#list>
   </#if>    
</table>