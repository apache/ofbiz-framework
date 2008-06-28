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
      <div class="boxhead">${uiLabelMap.OrderCheckout}</div>
    </div>
    <div class="screenlet-body" style="text-align: center;">
      <#if shoppingCart?has_content && shoppingCart.size() gt 0>
        <div id="checkoutPanel" class="form-container" align="center" style="border: 1px solid #333333; height: auto;">
          <div id="cartPanel" class="screenlet">
            <div class="screenlet-header"><div class="boxhead" align="left">Step 1: ${uiLabelMap.PageTitleShoppingCart}</div></div>
            <div id="cartSummaryPanel" style="display: none;">
              <div align="left" style="width: auto; padding: 10px 40px 30px 40px;"><a href="javascript:void(0);" id="openCartPanel"><h3>Click here to edit</h3></a></div>
              <div id="cartSummary" style="display: none;">
                Shopping cart summary.
              </div>
            </div>
            <div id="editCartPanel">
              <form name="cartForm" id="cartForm" method="post" action="<@ofbizUrl></@ofbizUrl>">
                Shopping cart information.
              </form>
              <div><h3><span class="editStep"><a href="javascript:void(0);" id="editShippingAndPersonalDetail"><h3>Continue for step 2</h3></a></span></h3></div>              
            </div>
          </div>

          <div id="shippingPanel" class="screenlet">
            <div class="screenlet-header"><div class="boxhead" align="left">Step 2: Shipping</div></div>
            <div id="shippingSummaryPanel">
              <div align="left" style="width: auto; padding: 10px 40px 30px 40px;"><a href="javascript:void(0);" id="openShippingPanel"><h3>Click here to edit</h3></a></div>
              <div id="shippingSummary" style="display: none;">
                Shipping Summary.
              </div>
            </div>
            <div id="editShippingPanel" style="display: none;">
             <form name="shippingForm" id="shippingForm" action="<@ofbizUrl></@ofbizUrl>" method="post">
               Shipping Detail.
             </form>
              <div><h3><span class="editStep"><a href="javascript:void(0);" id="editShipmentOptions"><h3>Continue for step 3</h3></a></span></h3></div>              
            </div>
          </div>

          <div id="shipmentOptionPanel" class="screenlet">
            <div class="screenlet-header"><div class="boxhead" align="left">Step 3: Shipment Options</div></div>
            <div id="shipmentOptionSummaryPanel">
              <div align="left" style="width: auto; padding: 10px 40px 30px 40px;"><a href="javascript:void(0);" id="openShipmentOptionPanel"><h3>Click here to edit</h3></a></div>
              <div id="shipmentOptionSummary" style="display: none;">
               Shipping Methods summary.
              </div>
            </div>
            <div id="editShipmentOptionPanel" style="display: none;">
              <form name="shipmentOptionForm" id="shipmentOptionForm" action="<@ofbizUrl></@ofbizUrl>" method="post">
                Shipping Methods.
              </form>
              <div><h3><span class="editStep"><a href="javascript:void(0);" id="editBillingAndPayment"><h3>Continue for step 4</h3></a></span></h3></div>
            </div>
          </div>

          <div id="billingPanel" class="screenlet">
            <div class="screenlet-header"><div class="boxhead" align="left">Step 4: Billing</div></div>
            <div id="billingSummaryPanel">
              <div align="left" style="width: auto; padding: 10px 40px 30px 40px;"><a href="javascript:void(0);" id="openBillingPanel"><h3>Click here to edit</h3></a></div>
              <div id="billingSummary" style="display: none;">
                Billing and Payment summary.
              </div>
            </div>
            <div id="editBillingPanel" style="display: none;">
              <form name="billingForm" id="billingForm" class="theform" action="<@ofbizUrl></@ofbizUrl>" method="post">
                Billing and Payment Detail.
              </form>
              <div><h3><span class="editStep"><a href="javascript:void(0);" id=""><h3>Continue for step 5</h3></a></span></h3></div>
            </div>
          </div>

          <div id="" class="screenlet">
            <div class="screenlet-header"><div class="boxhead" align="left">Step 5: Submit Order</div></div>
            <div id="orderSubmitPanel" style="display: none;">
              <form name="orderSubmitForm" id="orderSubmitForm" action="<@ofbizUrl></@ofbizUrl>" method="post">
              </form>
              <div><h3><span class="editStep"><a href="javascript:void(0);" id=""><h3>Submit Order.</h3></a></span></h3></div>  
            </div>
          </div>
        </div>
      </#if>

      <div id="emptyCartCheckoutPanel" align="center" <#if shoppingCart?has_content && shoppingCart.size() gt 0> style="display: none; border: 1px solid #333333; height: auto;"</#if>>
        <div>${uiLabelMap.OrderCheckout}</div>
        <div>
          <div><span style="display: none"><a href="javascript:void(0);"><img src="<@ofbizContentUrl></@ofbizContentUrl>"></a></span></div>
          <div>STEP 1: Confirm Totals</div><br>
          <div>You currently have no items in your cart. Click <a href="<@ofbizUrl>main</@ofbizUrl>">here</a> to view our products.</div>
        </div>
        <div>
          <div><span style="display: none"><a href="javascript:void(0);"><img src="<@ofbizContentUrl></@ofbizContentUrl>"></a></span></div>
          <div>STEP 2: Shipping</div>
        </div>
        <div>
          <div><span style="display: none"><a href="javascript:void(0);"><img src="<@ofbizContentUrl></@ofbizContentUrl>"></a></span></div>
          <div>STEP 3: Billing</div>          
        </div>
      </div>
    </div>
  </div>