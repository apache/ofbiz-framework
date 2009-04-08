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
<div class="screenlet">
    <div class="screenlet-title-bar">
        <ul>
            <li class="h3">${uiLabelMap.ProductShipmentPlan}</li>
        </ul>
        <br class="clear"/>
    </div>
    <div class="screenlet-body">
        ${findOrderItemsForm.renderFormString(context)}
    </div>
</div>
    <#if addToShipmentPlanRows?has_content>
    <div class="screenlet">
        <div class="screenlet-title-bar">
            <ul>
                <li class="h3">${uiLabelMap.ProductShipmentPlanAdd}</li>
            </ul>
            <br class="clear"/>
        </div>
        <div class="screenlet-body">
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
            <form>
                <input type="submit" onClick="submitRows('${rowCount?if_exists}');return false;" name="submitButton" value="${uiLabelMap.CommonAdd}"/>
            </form>
        </div>
    </div>
    </#if>
    <div class="screenlet">
        <div class="screenlet-title-bar">
            <ul>
                <li class="h3">${uiLabelMap.ProductShipmentPlanList}</li>
            </ul>
            <br class="clear"/>
        </div>
        <div class="screenlet-body">
            ${listShipmentPlanForm.renderFormString(context)}
            <br/>
            <div>
                <div align="right"><h2>${uiLabelMap.ProductShipmentTotalWeight}: ${totWeight} ${uiLabelMap.ProductShipmentTotalVolume}: ${totVolume}</h2></div>
                <div>${shipmentPlanToOrderItemsForm.renderFormString(context)}</div>
            </div>
        </div>
    </div>
<#else>
<div class="screenlet">
    <div class="screenlet-title-bar">
        <ul>
            <li class="h3">${uiLabelMap.ProductShipmentNotFoundId} : [${shipmentId?if_exists}]</li>
        </ul>
        <br class="clear"/>
    </div>
</div>
</#if>
