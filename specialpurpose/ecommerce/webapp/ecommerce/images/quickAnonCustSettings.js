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

document.observe('dom:loaded', isValidElement);

function isValidElement(element){
    var validator = new Validation('quickAnonProcessCustomer',  {immediate : true});
 }

document.observe('dom:loaded', function() {
    Event.observe('useShippingPostalAddressForBilling', 'click', changeText2);
});
function changeText2(){
    if($('useShippingPostalAddressForBilling').checked) {
        $('billToName').value = $F('shipToName');
        $('billToAttnName').value = $F('shipToAttnName');
        $('billToAddress1').value = $F('shipToAddress1');
        $('billToAddress2').value = $F('shipToAddress2');
        $('billToCity').value = $F('shipToCity');
        $('billToStateProvinceGeoId').value = $F('shipToStateProvinceGeoId');
        $('billToPostalCode').value = $F('shipToPostalCode');
        $('billToCountryGeoId').value = $F('shipToCountryGeoId');
        $('billToName').disabled = true;
        $('billToAttnName').disabled = true;
        $('billToAddress1').disabled = true;
        $('billToAddress2').disabled = true;
        $('billToCity').disabled = true;
        $('billToStateProvinceGeoId').disabled = true;
        $('billToPostalCode').disabled = true;
        $('billToCountryGeoId').disabled = true;
    } else {
        $('billToName').disabled = false;
        $('billToAttnName').disabled = false;
        $('billToAddress1').disabled = false;
        $('billToAddress2').disabled = false;
        $('billToCity').disabled = false;
        $('billToStateProvinceGeoId').disabled = false;
        $('billToPostalCode').disabled = false;
        $('billToCountryGeoId').disabled = false;
    }
}