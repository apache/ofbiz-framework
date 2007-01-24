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
<#if shipment?exists>
    <div class="head1">${uiLabelMap.ProductShipmentPlan}</div>
    ${findOrderItemsForm.renderFormString(context)}
    <br/>
    <#if addToShipmentPlanRows?has_content>
        ${addToShipmentPlanForm.renderFormString(context)}
<script language="javascript">
    function submitRows(rowCount) {
        var rowCountElement = document.createElement("input");
        rowCountElement.setAttribute("name", "_rowCount");
        rowCountElement.setAttribute("type", "hidden");
        rowCountElement.setAttribute("value", rowCount);
        document.forms.addToShipmentPlan.appendChild(rowCountElement);

        var shipmentIdElement = document.createElement("input");
        shipmentIdElement.setAttribute("name", "shipmentId");
        shipmentIdElement.setAttribute("type", "hidden");
        shipmentIdElement.setAttribute("value", ${shipmentId});
        document.forms.addToShipmentPlan.appendChild(shipmentIdElement);

        document.forms.addToShipmentPlan.submit();
    }
</script>
<form><input type="submit" class="smallSubmit" onClick="submitRows('${rowCount?if_exists}');return false;" name="submitButton" value="${uiLabelMap.CommonAdd}"/></form>
    <hr class="sepbar"/>
    <br/>
    </#if>
    ${listShipmentPlanForm.renderFormString(context)}
    <div class="head2">${uiLabelMap.ProductShipmentTotalWeight}: ${totWeight}</div>
    <div class="head2">${uiLabelMap.ProductShipmentTotalVolume}: ${totVolume}</div>
    ${shipmentPlanToOrderItemsForm.renderFormString(context)}
<#else>
  <h3>${uiLabelMap.ProductShipmentNotFoundId} : [${shipmentId?if_exists}]</h3>
</#if>
