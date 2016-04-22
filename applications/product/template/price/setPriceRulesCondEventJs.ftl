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
<script type="text/javascript">
jQuery(document).ready( function() {
<#if 0 < productPriceConds.size()>
  <#list 0..productPriceConds.size()-1 as i>
    if (document.getElementById('EditProductPriceRulesCond_o_${i}')) {
      jQuery('#EditProductPriceRulesCond_condValueInput_o_${i}').hide();
      jQuery('#EditProductPriceRulesCond_inputParamEnumId_o_${i}').change( function() {
        getDependentDropdownValues('getAssociatedPriceRulesConds', 'inputParamEnumId', 'EditProductPriceRulesCond_inputParamEnumId_o_${i}', 'EditProductPriceRulesCond_condValue_o_${i}', 'productPriceRulesCondValues', 'condValue_o_${i}', 'description', '${productPriceConds[i].condValue}', '', '', '', '', 'EditProductPriceRulesCond_condValueInput_o_${i}');
    });
    getDependentDropdownValues('getAssociatedPriceRulesConds', 'inputParamEnumId', 'EditProductPriceRulesCond_inputParamEnumId_o_${i}', 'EditProductPriceRulesCond_condValue_o_${i}', 'productPriceRulesCondValues', 'condValue_o_${i}', 'description', '${productPriceConds[i].condValue}', '', '', '', '', 'EditProductPriceRulesCond_condValueInput_o_${i}');
    }
  </#list>
</#if>
  if (document.getElementById('AddProductPriceRulesCond_o_0')) {
    jQuery('#AddProductPriceRulesCond_condValueInput_o_0').hide();
    jQuery('#AddProductPriceRulesCond_inputParamEnumId_o_0').change( function() {
      getDependentDropdownValues('getAssociatedPriceRulesConds', 'inputParamEnumId', 'AddProductPriceRulesCond_inputParamEnumId_o_0', 'AddProductPriceRulesCond_condValue_o_0', 'productPriceRulesCondValues', 'condValue_o_0', 'description', '', '', '', '', '', 'AddProductPriceRulesCond_condValueInput_o_0');
    });
    getDependentDropdownValues('getAssociatedPriceRulesConds', 'inputParamEnumId', 'AddProductPriceRulesCond_inputParamEnumId_o_0', 'AddProductPriceRulesCond_condValue_o_0', 'productPriceRulesCondValues', 'condValue_o_0', 'description', '', '', '', '', '', 'AddProductPriceRulesCond_condValueInput_o_0');
  }
})
</script>
