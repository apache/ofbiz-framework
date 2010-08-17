<script type="text/javascript">
Event.observe(window, 'load', function() {
    if ($('${dependentForm}')) {
        Event.observe($('${dependentForm}_${mainId}'), 'change', function() {
            getDependentDropdownValues('${requestName}', '${mainId}', '${dependentForm}_${mainId}', '${dependentForm}_${dependentId}', '${responseName}', '${dependentId}', '${descName}', '', '');
        });
        getDependentDropdownValues('${requestName}', '${mainId}', '${dependentForm}_${mainId}', '${dependentForm}_${dependentId}', '${responseName}', '${dependentId}', '${descName}', '${selectedDependentOption}', '');
    }
})
</script>