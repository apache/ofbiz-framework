Event.observe(window, 'load', isValidElement);

function isValidElement(element){
    var validator = new Validation('quickAnonProcessCustomer',  {immediate : true});
 }

Event.observe(window, 'load', function() {
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