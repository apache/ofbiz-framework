/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

jQuery(document).ready( function() {
    jQuery('#costCentersSubmit').click(processCostCenterData);
    // Find all text boxes in form and add a method to list on for on change.
    var categoryShareInputs = jQuery('#costCenters [type=text]');

    jQuery.each(categoryShareInputs, function (element) {
        jQuery(this).change( function() {
            var textIdSplit = jQuery(this).attr('id').split('|');
            var tableRowId = 'row_' + textIdSplit[0];
            var tableRow = jQuery("#" + tableRowId);
            // get all text inputs
            var rowInputs = jQuery("#" + tableRowId + " [type=text]");
            var totalPercentage = 0;
            jQuery.each( rowInputs, function (inputElement) {
                var inputElementIdSplit = jQuery(this).attr('id').split("|");
                if (jquery(this).val()) {
                    totalPercentage = totalPercentage + parseFloat(jquery(this).val()) 
                }
            });
            if (totalPercentage == 100 || totalPercentage == 0 ) {
                if ( jQuery("#" + tableRowId).hasClass('alternate-rowWarn')){
                     jQuery("#" +tableRowId).removeClass('alternate-rowWarn');
                }
                if (jQuery('tr.alternate-rowWarn').length == 0) {
                    jQuery("#" + 'costCentersSubmit').removeClass('buttontextdisabled')
                    jQuery("#" + 'costCentersSubmit').attr("disabled", false);
                }

            } else {
                if ( !jQuery("#" + tableRowId).hasClass('alternate-rowWarn')){
                    jQuery("#" + tableRowId).addClass('alternate-rowWarn');
                }
                if (!jQuery("#" + 'costCentersSubmit').hasClass('buttontextdisabled')) {
                    jQuery("#" + 'costCentersSubmit').addClass('buttontextdisabled')
                    jQuery("#" + 'costCentersSubmit').attr("disabled", true);;
                }
            }
        });
    });
});

function processCostCenterData() {
    jQuery.ajax({
        url: jQuery("#costCenters").attr('action'),
        data: jQuery("#costCenters").serialize(),
        async: false,
        type: 'POST',
        success: function(data){
            if (data == "") {
                jQuery("#errorMessage").show();
                jQuery("#errorMessage").fadeOut('slow');
            } else {
                jQuery("#eventMessage").show();
                jQuery("#eventMessage").fadeOut('slow');
            }
        }
    });
};
