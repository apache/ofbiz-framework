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

<#--
     The form widget of type multi can have only one submit button, and hence only one operation on the selected labels.
     To support more than one action, we can create another submit button outside the form and use a javascript
     function to change the target of the multi form and submit it.

     In this case, the multi form has two actions:  Print as labels, which generates a PDF of selected labels, and
     mark as accepted, which updates the selected shipment route segments.  The label printing button is handled
     normally in the form widget.  The mark action is handled here using the technique described above.  If more
     actions are required, we can create more submit buttons in this file with their own action-changing submit functions.

     Note that the facilityId in the form action is a trick to pass the facilityId on to the next request.
     Also note that for layout purposes, the submit button in the form widget can be converted and moved here so that all
     the buttons can be arranged as desired.

-->


<script type="application/javascript">
<!--
  function markAsAccepted() {
    document.Labels.action = "<@ofbizUrl>BatchUpdateShipmentRouteSegments?facilityId=${parameters.facilityId}</@ofbizUrl>";
    document.Labels.submit();
  }
//-->
</script>

<input type="submit" class="smallSubmit" value="${uiLabelMap.ProductMarkAsAccepted}" onclick="javascript:markAsAccepted()"/>
