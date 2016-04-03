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
    jQuery('#endingDrawerCashAmount').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            closeTerminalConfirm();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#endingDrawerCheckAmount').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            closeTerminalConfirm();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#endingDrawerCcAmount').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            closeTerminalConfirm();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#endingDrawerGcAmount').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            closeTerminalConfirm();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#endingDrawerOtherAmount').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            closeTerminalConfirm();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#closeTerminalConfirm').bind('click', function(event) {
         closeTerminalConfirm();
         return false;
    });
    
    jQuery('#closeTerminalCancel').bind('click', function(event) {
         productToSearchFocus();
         return false;
    });
});

function closeTerminalConfirm() {
    pleaseWait('Y');
    var param = 'endingDrawerCashAmount=' + jQuery('#endingDrawerCashAmount').val() + '&endingDrawerCheckAmount=' + jQuery('#endingDrawerCheckAmount').val() + 
                '&endingDrawerCcAmount=' + jQuery('#endingDrawerCcAmount').val() + '&endingDrawerGcAmount=' + jQuery('#endingDrawerGcAmount').val() + 
                '&endingDrawerOtherAmount=' + jQuery('#endingDrawerOtherAmount').val();
    jQuery.ajax({url: 'CloseTerminal',
        data: param,
        type: 'post',
        async: false,
        success: function(data) {
            getResultOfCloseTerminal(data);
        },
        error: function(data) {
            getResultOfCloseTerminal(data);
        }
    });
    pleaseWait('N');
}

function closeTerminal(cleanErrors) {
    if (cleanErrors == undefined) {
        cleanErrors = "Y";
    }
    if (cleanErrors == "Y") {
        hideOverlayDiv();
        jQuery('#closeTerminalFormServerError').html("");
        jQuery('#endingDrawerCashAmount').val("");
        jQuery('#endingDrawerCheckAmount').val("");
        jQuery('#endingDrawerCcAmount').val("");
        jQuery('#endingDrawerGcAmount').val("");
        jQuery('#endingDrawerOtherAmount').val("");
    }
    jQuery('#closeTerminal').show();
    jQuery('#endingDrawerCashAmount').focus();
}

function getResultOfCloseTerminal(data) {
    var serverError = getServerError(data);
    if (serverError != "") {
        jQuery('#closeTerminalFormServerError').html(serverError);
        closeTerminal('N');
    } else {
        clearErrorMessages();
        updateCart();
        productToSearchFocus();
    }
}