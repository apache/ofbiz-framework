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

<#if "PROD_ORDER_HEADER" == workEffort.workEffortTypeId>
  <a href="<@ofbizUrl controlPath="/manufacturing/control">ShowProductionRun?productionRunId=${workEffort.workEffortId}</@ofbizUrl>" class="event">
    ${workEffort.workEffortId}
  </a>
  &nbsp;${workEffort.workEffortName?default("Undefined")}
  <#if workOrderItemFulfillments?has_content>
    <#list workOrderItemFulfillments as workOrderItemFulfillment>
      <br/>${uiLabelMap.OrderOrderId}: <a href="<@ofbizUrl controlPath="/ordermgr/control">orderview?orderId=${workOrderItemFulfillment.orderId}</@ofbizUrl>" class="event">${workOrderItemFulfillment.orderId} / ${workOrderItemFulfillment.orderItemSeqId}</a>
      <#assign orderItemAndShipGroupAssocs = EntityQuery.use(delegator).from("OrderHeaderItemAndShipGroup").where("orderId", workOrderItemFulfillment.orderId!, "orderItemSeqId", workOrderItemFulfillment.orderItemSeqId!).queryList()!/>
      <#list orderItemAndShipGroupAssocs as orderItemAndShipGroupAssoc>
        <#if orderItemAndShipGroupAssoc.shipByDate?has_content>
          ${uiLabelMap.OrderShipBeforeDate}: ${orderItemAndShipGroupAssoc.shipByDate}
        </#if>
      </#list>
    </#list>
  </#if>
<#elseif "PROD_ORDER_TASK" == workEffort.workEffortTypeId>
  <a href="<@ofbizUrl controlPath="/manufacturing/control">ShowProductionRun?productionRunId=${workEffort.workEffortParentId}</@ofbizUrl>" class="event">
    ${workEffort.workEffortParentId} / ${workEffort.workEffortId}
  </a>
  &nbsp;${workEffort.workEffortName?default("Undefined")}<#if workEffort.reservPersons??>&nbsp;Persons:${workEffort.reservPersons}</#if>
  <#if parentWorkOrderItemFulfillments?has_content>
    <#list parentWorkOrderItemFulfillments as parentWorkOrderItemFulfillment>
      <br/>${uiLabelMap.OrderOrderId}: <a href="<@ofbizUrl controlPath="/ordermgr/control">orderview?orderId=${parentWorkOrderItemFulfillment.orderId}</@ofbizUrl>" class="event">${parentWorkOrderItemFulfillment.orderId} / ${parentWorkOrderItemFulfillment.orderItemSeqId}</a>
      <#assign orderItemAndShipGroupAssocs = EntityQuery.use(delegator).from("OrderHeaderItemAndShipGroup").where("orderId", parentWorkOrderItemFulfillment.orderId!, "orderItemSeqId", parentWorkOrderItemFulfillment.orderItemSeqId!).queryList()!/>
      <#list orderItemAndShipGroupAssocs as orderItemAndShipGroupAssoc>
        <#if orderItemAndShipGroupAssoc.shipByDate?has_content>
          ${uiLabelMap.OrderShipBeforeDate}: ${orderItemAndShipGroupAssoc.shipByDate}
        </#if>
      </#list>
    </#list>
  </#if>
<#else>
  <#-- Allow containing screens to specify the URL for editing an event -->
  <#if !editCalEventUrl??>
    <#assign editCalEventUrl = parameters._LAST_VIEW_NAME_>
  </#if>
  <a href="<@ofbizUrl>${editCalEventUrl}?form=edit&amp;parentTypeId=${parentTypeId!}&amp;period=${periodType!}&amp;start=${parameters.start!}&amp;workEffortId=${workEffort.workEffortId}${addlParam!}${urlParam!}</@ofbizUrl>" class="event">
    ${workEffort.workEffortId}
  </a>
  &nbsp;${workEffort.workEffortName?default("")}
</#if>
