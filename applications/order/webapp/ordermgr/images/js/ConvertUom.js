function getConvertedPrice(element, uomId, uomIdTo, rowCount, orderCurrencyUnitPrice, unitCost) {
    var request = $F('getConvertedPrice');
    new Ajax.Request(request, {
        asynchronous: false,
        onSuccess: function(transport) {
            var data = transport.responseText.evalJSON(true);
            if (data.convertedValue && confirm($('alertMessage').value)) {
                $('unitCost_'+rowCount).value = data.convertedValue;
            } else {
                $('orderCurrencyUnitPrice_'+rowCount).value = orderCurrencyUnitPrice;
                $('unitCost_'+rowCount).value = unitCost;
            }
        }, parameters: { uomId : uomId, uomIdTo : uomIdTo, originalValue : element.value }
    });
}