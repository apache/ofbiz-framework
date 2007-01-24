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

<#if hasPermission>
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxlink">
            <#if orderId?exists>
                <a href="<@ofbizUrl>orderview?orderId=${orderId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.OrderViewOrder}]</a>
            </#if>
        </div>
        <div class="boxhead">${uiLabelMap.OrderScheduleDelivery}</div>
    </div>
    <div class="screenlet-body">
        <#if orderId?has_content>
          ${updatePODeliveryInfoWrapper.renderFormString(context)}
        <#else>
          <div class="tabletext">${uiLabelMap.OrderNoPurchaseSpecified}</div>
        </#if>
    </div>
</div>
<#else>
 <h3>${uiLabelMap.OrderViewPermissionError}</h3>
</#if>
