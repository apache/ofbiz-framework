/*
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
*/

jQuery(document).ready(isValidElement);

function isValidElement(element){
    jQuery('#quickAnonProcessCustomer').validate(); 
 }

jQuery(document).ready(function() {
    jQuery('#useShippingPostalAddressForBilling').click(changeText2);
});
function changeText2(){
    if(document.getElementById('useShippingPostalAddressForBilling').checked) {
        document.getElementById('billToName').value = document.getElementById('shipToName').value;
        document.getElementById('billToAttnName').value = document.getElementById('shipToAttnName').value;
        document.getElementById('billToAddress1').value = document.getElementById('shipToAddress1').value;
        document.getElementById('billToAddress2').value = document.getElementById('shipToAddress2').value;
        document.getElementById('billToCity').value = document.getElementById('shipToCity').value;
        document.getElementById('billToStateProvinceGeoId').value = document.getElementById('shipToStateProvinceGeoId').value;
        document.getElementById('billToPostalCode').value = document.getElementById('shipToPostalCode').value;
        document.getElementById('billToCountryGeoId').value = document.getElementById('shipToCountryGeoId').value;
        document.getElementById('billToName').disabled = true;
        document.getElementById('billToAttnName').disabled = true;
        document.getElementById('billToAddress1').disabled = true;
        document.getElementById('billToAddress2').disabled = true;
        document.getElementById('billToCity').disabled = true;
        document.getElementById('billToStateProvinceGeoId').disabled = true;
        document.getElementById('billToPostalCode').disabled = true;
        document.getElementById('billToCountryGeoId').disabled = true;
    } else {
        document.getElementById('billToName').disabled = false;
        document.getElementById('billToAttnName').disabled = false;
        document.getElementById('billToAddress1').disabled = false;
        document.getElementById('billToAddress2').disabled = false;
        document.getElementById('billToCity').disabled = false;
        document.getElementById('billToStateProvinceGeoId').disabled = false;
        document.getElementById('billToPostalCode').disabled = false;
        document.getElementById('billToCountryGeoId').disabled = false;
    }
}