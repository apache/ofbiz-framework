<script type="text/javascript">
jQuery(document).ready( function() {

<#if 0 < productPriceConds.size()>
  <#list 0..productPriceConds.size()-1 as i>
    if (document.getElementById('EditProductPriceRulesCond_o_${i}')) {
      jQuery('#EditProductPriceRulesCond_inputParamEnumId_o_${i}').change( function() {
    <#if 'PRIP_QUANTITY' != productPriceConds[i].inputParamEnumId && 'PRIP_LIST_PRICE' != productPriceConds[i].inputParamEnumId>
        getDependentDropdownValues('getAssociatedPriceRulesConds', 'inputParamEnumId', 'EditProductPriceRulesCond_inputParamEnumId_o_${i}', 'EditProductPriceRulesCond_condValue_o_${i}', 'productPriceRulesCondValues', 'condValue_o_${i}', 'description');
    </#if>
      });
    <#if 'PRIP_QUANTITY' != productPriceConds[i].inputParamEnumId && 'PRIP_LIST_PRICE' != productPriceConds[i].inputParamEnumId>
      getDependentDropdownValues('getAssociatedPriceRulesConds', 'inputParamEnumId', 'EditProductPriceRulesCond_inputParamEnumId_o_${i}', 'EditProductPriceRulesCond_condValue_o_${i}', 'productPriceRulesCondValues', 'condValue_o_${i}', 'description', '${productPriceConds[i].condValue}');
    </#if>
    }
  </#list>
</#if>
  if (document.getElementById('AddProductPriceRulesCond_o_0')) {
    jQuery('#AddProductPriceRulesCond_condValueInput_o_0').hide();
    jQuery('#AddProductPriceRulesCond_inputParamEnumId_o_0').change( function() {
      getDependentDropdownValues('getAssociatedPriceRulesConds', 'inputParamEnumId', 'AddProductPriceRulesCond_inputParamEnumId_o_0', 'AddProductPriceRulesCond_condValue_o_0', 'productPriceRulesCondValues', 'condValue_o_0', 'description', '', '', '', '', 'AddProductPriceRulesCond_condValueInput_o_0');
    });
    getDependentDropdownValues('getAssociatedPriceRulesConds', 'inputParamEnumId', 'AddProductPriceRulesCond_inputParamEnumId_o_0', 'AddProductPriceRulesCond_condValue_o_0', 'productPriceRulesCondValues', 'condValue_o_0', 'description', '', '', '', '', 'AddProductPriceRulesCond_condValueInput_o_0');
  }
})
</script>
