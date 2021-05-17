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
<script language="JavaScript" type="text/javascript">
if (typeof(checkAmtReq2) == 'undefined') {

    var variantReqAmounts = {};
    function checkAmtReq2(sku) {
        return variantReqAmounts[sku];
    }

    var variantPrices = {};
    function getVariantPrice2(sku) {
        return variantPrices[sku];
    }
    
    function variantUomSelection (select) {
        var $select = $(select);
        var form = $select.closest('form');
        var variantId = $select.val();
        $("[name='product_id']", form).val(variantId);
        if (!variantId) {
            variantId = $("[name='product_id_bak']", form).val() || '';
        }

        $('.product_id_display', form).text(variantId);
        var price = getVariantPrice2(variantId);
        $('.variant_price_display', form).text(price || '');
//        var price_div = $('.variant-price', form);
//        if (price) {
//            price_div.css('display', 'inline-block');
//        }
//        else {
//            price_div.hide();
//        }
    }

          jQuery(document).ready(function(jQuery) {
          jQuery('.popup_link').each(
            function(index) {
              var $this = jQuery(this);
              var popup = $('.popup', $this.closest('.productsummary'));
              $this.attr('title', popup.remove().html());
              $this.tooltip({
                content: function(){
                  return this.getAttribute("title");
                },
                tooltipClass: "popup",
                track: true
              });
            });
          }); 
}
</script>
