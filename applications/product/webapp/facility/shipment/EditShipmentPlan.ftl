<#--
 *  Copyright (c) 2003-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a 
 *  copy of this software and associated documentation files (the "Software"), 
 *  to deal in the Software without restriction, including without limitation 
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 *  and/or sell copies of the Software, and to permit persons to whom the 
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included 
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT 
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     Jacopo Cappellato (tiz@sastau.it)
 *
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
