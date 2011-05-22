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
jQuery(document).ready(function() { 
    jQuery('#searchByPartyIdValue').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            partySearchAgain();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#searchByPartyLastName').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            partySearchAgain();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#searchByPartyFirstName').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            partySearchAgain();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#searchPartiesResultsSearch').bind('click', function(event) {
        partySearchAgain();
        return false;
    });
    
    jQuery('#searchPartiesResultsCancel').bind('click', function(event) {
        jQuery('#searchPartiesResults').hide();
        productToSearchFocus();
        return false;
    });
});

function buildPartiesResults(parties, fromPartySearch) {
    var alt_row = false;
    var idx = 1;
    var tableList = "";
    jQuery.each(parties, function(i, party) {
        if (alt_row) {
            classType = "class=\"pos-cart-even\"";
        } else {
            classType = "class=\"pos-cart-odd\"";
        }
        lastName = checkNull(party.lastName);
        firstName = checkNull(party.firstName);
        address1 = checkNull(party.address1);
        city = checkNull(party.city);
        postalCode = checkNull(party.postalCode);
        stateProvinceGeoId = checkNull(party.stateProvinceGeoId);
        countryGeoId = checkNull(party.countryGeoId);
        contactMechPurposeTypeId = checkNull(party.contactMechPurposeTypeId);
        billingShipping = "B";
        if (contactMechPurposeTypeId == 'SHIPPING_LOCATION') {
            billingShipping = "S";
        }
        editAddr = "editAddress" + i;
        selectedPartyId = "selectedPartyId" + i;
        selectedContactMechId = "selectedContactMechId" + i;
        selectedContactMechPurposeTypeId = "selectedContactMechPurposeTypeId" + i;
        tableList = tableList + "<tr " + classType + ">";
        tableList = tableList + "<td><input type=\"hidden\" id=\"" + selectedPartyId + "\" name=\"" + selectedPartyId + "\" value=\"" + party.partyId + "\"/>";
        tableList = tableList + "<input type=\"hidden\" id=\"" + selectedContactMechId + "\" name=\"" + selectedContactMechId + "\" value=\"" + party.contactMechId + "\"/>";
        tableList = tableList + "<input type=\"hidden\" id=\"" + selectedContactMechPurposeTypeId + "\" name=\"" + selectedContactMechPurposeTypeId + "\" value=\"" + party.contactMechPurposeTypeId + "\"/>";
        tableList = tableList + "<input type=\"checkbox\" class=\"editAddress\" id=\"" + editAddr + "\"/></td>";
        tableList = tableList + "<td><a href=\"javascript:setParty(\'" + party.partyId + "\', \'" + party.contactMechId + "\', \'" + party.contactMechPurposeTypeId +"\');\">" + party.partyId + "</a></td>";
        tableList = tableList + "<td><a href=\"javascript:setParty(\'" + party.partyId + "\', \'" + party.contactMechId + "\', \'" + party.contactMechPurposeTypeId +"\');\">" + lastName + "</a></td>";
        tableList = tableList + "<td><a href=\"javascript:setParty(\'" + party.partyId + "\', \'" + party.contactMechId + "\', \'" + party.contactMechPurposeTypeId +"\');\">" + firstName + "</a></td>";
        tableList = tableList + "<td>" + address1 + "</td>";
        tableList = tableList + "<td>" + city + "</td>";
        tableList = tableList + "<td>" + postalCode + "</td>";
        tableList = tableList + "<td>" + stateProvinceGeoId + "</td>";
        tableList = tableList + "<td>" + countryGeoId + "</td>";
        tableList = tableList + "<td>" + billingShipping + "</td>";
        tableList = tableList + "</tr>";
        alt_row = !alt_row;
        idx++;
    });
    jQuery('#searchPartiesResultsList').html(tableList);
    editAddressClick();
    jQuery('#searchPartiesResults').show();
    if (fromPartySearch == 'Y') {
        if (jQuery('#searchPartyBy').val() == 'lastName') {
            jQuery('#searchByPartyLastName').val(jQuery('#partyToSearch').val());
            jQuery('#searchByPartyFirstName').val("");
            jQuery('#searchByPartyIdValue').val("");
            jQuery('#searchByPartyLastName').focus();
        } else if (jQuery('#searchPartyBy').val() == 'firstName') {
            jQuery('#searchByPartyLastName').val("");
            jQuery('#searchByPartyFirstName').val(jQuery('#partyToSearch').val());
            jQuery('#searchByPartyIdValue').val("");
            jQuery('#searchByPartyFirstName').focus();
        } else if (jQuery('#searchPartyBy').val() == 'idValue') {
            jQuery('#searchByPartyLastName').val("");
            jQuery('#searchByPartyFirstName').val("");
            jQuery('#searchByPartyIdValue').val(jQuery('#partyToSearch').val());
            jQuery('#searchByPartyIdValue').focus();
        }
        
        if (jQuery('#billingLocation').val() == 'Y') {
            jQuery('#billingLoc').val('Y');
            jQuery('#billingLoc').attr('checked', true);
        } else {
            jQuery('#billingLoc').val('N');
            jQuery('#billingLoc').attr('checked', false);
        }
        
        if (jQuery('#shippingLocation').val() == 'Y') {
            jQuery('#shippingLoc').val('Y');
            jQuery('#shippingLoc').attr('checked', true);
        } else {
            jQuery('#shippingLoc').val('N');
            jQuery('#shippingLoc').attr('checked', false);
        }
    }
}

function editAddressClick() {
    jQuery('input.editAddress').each(function(idx) {
        var id = jQuery(this).attr('id');
        if (id != '') {
            id = '#' + id;
            jQuery(id).bind('click', function(event) {
                editAddress(jQuery('#selectedPartyId' + idx).val(), jQuery('#selectedContactMechId' + idx).val(), jQuery('#selectedContactMechPurposeTypeId' + idx).val());
                return false;
            });
        }
    });
}

function partySearchAgain() {
    pleaseWait('Y');
    var param = 'partyIdentificationTypeId=' + jQuery('#partyIdentificationTypeId').val() +
                '&searchByPartyLastName=' + jQuery('#searchByPartyLastName').val() +
                '&searchByPartyFirstName=' + jQuery('#searchByPartyFirstName').val() +
                '&searchByPartyIdValue=' + jQuery('#searchByPartyIdValue').val();
    
    if (jQuery('#billingLoc').attr('checked') == true) {
        param = param + '&billingLocation=Y';
    } else {
        param = param + '&billingLocation=N';
    }
    
    if (jQuery('#shippingLoc').attr('checked') == true) {
        param = param + '&shippingLocation=Y';
    } else {
        param = param + '&shippingLocation=N';
    }
    
    jQuery.ajax({url: 'FindParties',
        data: param,
        type: 'post',
        async: false,
        success: function(data) {
            var parties = data.partiesList;
            buildPartiesResults(parties, 'N');
        },
        error: function(data) {
            alert("Error during party searching again");
        }
    });
    pleaseWait('N');
}