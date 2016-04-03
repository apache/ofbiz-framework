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
    jQuery('#startingDrawerAmount').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            openTerminalConfirm();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#openTerminalConfirm').bind('click', function(event) {
        openTerminalConfirm();
        return false;
    });
    
    jQuery('#openTerminalCancel').bind('click', function(event) {
        productToSearchFocus();
        return false;
    });
});

function openTerminalConfirm() {
    pleaseWait('Y');
    var param = 'startingDrawerAmount=' + jQuery('#startingDrawerAmount').val();
    jQuery.ajax({url: 'OpenTerminal',
        data: param,
        type: 'post',
        async: false,
        success: function(data) {
            getResultOfOpenTerminal(data);
        },
        error: function(data) {
            getResultOfOpenTerminal(data);
        }
    });
    pleaseWait('N');
}

function openTerminal(cleanErrors) {
    if (cleanErrors == undefined) {
        cleanErrors = "Y";
    }
    if (cleanErrors == "Y") {
        hideOverlayDiv();
        jQuery('#openTerminalFormServerError').html("");
        jQuery('#startingDrawerAmount').val("");
    }
    jQuery('#openTerminal').show();
    jQuery('#startingDrawerAmount').focus();
}

function getResultOfOpenTerminal(data) {
    var serverError = getServerError(data);
    if (serverError != "") {
        jQuery('#openTerminalFormServerError').html(serverError);
        openTerminal('N');
    } else {
        clearErrorMessages();
        updateCart();
        productToSearchFocus();
    }
}