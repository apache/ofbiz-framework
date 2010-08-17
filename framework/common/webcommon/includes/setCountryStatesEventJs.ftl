<script type="text/javascript">
Event.observe(window, 'load', function() {
    if ($('${dependentForm}')) {
        Event.observe($('${dependentForm}_countryGeoId'), 'change', function() {
            getDependentDropdownValues('getAssociatedStateList', 'countryGeoId', '${dependentForm}_countryGeoId', '${dependentForm}_stateProvinceGeoId', 'stateList', 'geoId', 'geoName', '', '');
        });
        getDependentDropdownValues('getAssociatedStateList', 'countryGeoId', '${dependentForm}_countryGeoId', '${dependentForm}_stateProvinceGeoId', 'stateList', 'geoId', 'geoName', '${selectedStateProvinceGeoId}', '');
    }
})
</script>
