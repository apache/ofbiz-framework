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
var uiLabelJsonObjects = null;
jQuery(document).ready(function() {
    jQuery('#editAddressCreateUpdate').bind('click', function(event) {
        pleaseWait('Y');
        var param = 'partyId=' + jQuery('#partyId').val() + '&firstName=' + jQuery('#personFirstName').val() + '&lastName=' + jQuery('#personLastName').val() + 
                    '&contactMechId=' + jQuery('#contactMechId').val() + '&contactMechPurposeTypeId=' + jQuery('#contactMechPurposeTypeId').val() + 
                    '&address1=' + jQuery('#personAddress1').val() + '&address2=' + jQuery('#personAddress2').val() + 
                    '&countryProvinceGeo=' + jQuery('#countryProvinceGeo').val() + '&stateProvinceGeo=' + jQuery('#stateProvinceGeo').val() + 
                    '&city=' + jQuery('#personCity').val() + '&postalCode=' + jQuery('#personPostalCode').val();
        jQuery.ajax({url: 'CreateUpdateAddress',
            data: param,
            type: 'post',
            async: false,
            success: function(data) {
                getResultOfCreateUpdateAddress(data);
            },
            error: function(data) {
                getResultOfCreateUpdateAddress(data);
            }
        });
        productToSearchFocus();
        pleaseWait('N');
        partySearchAgain();
        return false;
    });
    
    jQuery('#editAddressCancel').bind('click', function(event) {
        pleaseWait('Y');
        productToSearchFocus();
        pleaseWait('N');
        jQuery('#searchPartiesResults').show();
        return false;
    });
    
    var labelObject = {
        "CommonUiLabels" : ["CommonCreate", "CommonUpdate"],
    };

    getJSONuiLabels(labelObject, function(result){
    	uiLabelJsonObjects = result.responseJSON;
    });
});

function getResultOfCreateUpdateAddress(data) {
    var serverError = getServerError(data);
    if (serverError != "") {
        jQuery('#editAddressFormServerError').html(serverError);
    } else {
        productToSearchFocus();
    }
}

function editAddress(partyIdIn, contactMechIdIn, contactMechPurposeTypeIdIn) {
    var param = 'partyId=' + partyIdIn + '&contactMechId=' + contactMechIdIn + '&contactMechPurposeTypeId=' + contactMechPurposeTypeIdIn;
    jQuery.ajax({url: 'EditAddress',
        data: param,
        type: 'post',
        async: false,
        success: function(data) {
            getResultOfEditAddress(data);
        }
    });      
    jQuery('#editAddress').show();
    jQuery('#personLastName').focus();
}

function getResultOfEditAddress(data) {
    jQuery('#partyId').val(data.partyId);
    jQuery('#contactMechId').val(data.contactMechId);
    jQuery('#contactMechPurposeTypeId').val(data.contactMechPurposeTypeId);
    jQuery('#personLastName').val(data.lastName);
    jQuery('#personFirstName').val(data.firstName);
    jQuery('#personAddress1').val(data.address1);
    jQuery('#personAddress2').val(data.address2);
    jQuery('#countryProvinceGeo').val(data.countryGeoId);
    jQuery('#stateProvinceGeo').val(data.stateProvinceGeoId);
    jQuery('#personCity').val(data.city);
    jQuery('#personPostalCode').val(data.postalCode);
    if (data.partyId != "") {
        jQuery('#editAddressCreateUpdate').val(uiLabelJsonObjects.CommonUiLabels[1]);
    } else {
        jQuery('#editAddressCreateUpdate').val(uiLabelJsonObjects.CommonUiLabels[0]);
    }    
}