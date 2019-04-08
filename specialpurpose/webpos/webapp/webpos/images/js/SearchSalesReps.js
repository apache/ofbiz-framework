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
function removeSalesRep(salesRepId) {
    pleaseWait('Y');
    var param = 'partyId=' + salesRepId;
    jQuery.ajax({url: 'RemoveSalesRep',
        data: param,
        type: 'post',
        async: false,
        success: function(data) {
            getResultOfRemoveSalesRep(data);
        },
        error: function(data) {
            getResultOfRemoveSalesRep(data);
        }
    });
    pleaseWait('N');
}

function getResultOfRemoveSalesRep(data) {
    var serverError = getServerError(data);
    if (serverError != "") {
        jQuery('#errors').fadeIn('slow', function() {
            jQuery('#errors').html(serverError);
        });
    } else {
        clearErrorMessages();
        updateSearchSalesReps();
        productToSearchFocus();
    }
}

function addSalesRep(salesRepId) {
    pleaseWait('Y');
    var param = 'partyId=' + salesRepId;
    jQuery.ajax({url: 'AddSalesRep',
        data: param,
        type: 'post',
        async: false,
        success: function(data) {
            getResultOfAddSalesRep(data);
        },
        error: function(data) {
            getResultOfAddSalesRep(data);
        }
    });
    pleaseWait('N');
}

function getResultOfAddSalesRep(data) {
    var serverError = getServerError(data);
    if (serverError != "") {
        jQuery('#errors').fadeIn('slow', function() {
            jQuery('#errors').html(serverError);
        });
    } else {
        clearErrorMessages();
        updateSearchSalesReps();
        productToSearchFocus();
    }
}

function updateSearchSalesReps() {
    jQuery('#SearchSalesReps').load('SearchSalesRepsList', {cartLineIndex:null});
}