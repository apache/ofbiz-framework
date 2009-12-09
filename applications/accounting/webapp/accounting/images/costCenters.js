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

Event.observe(window, 'load', function() {
    Event.observe($('costCentersSubmit'), 'click', processCostCenterData);
    // Find all text boxes in form and add a method to list on for on change.
    var categoryShareInputs = $('costCenters').getInputs('text');
    categoryShareInputs.each(function (element) {
        Event.observe(element, 'change', function(){
            var textIdSplit = element.id.split('|');
            var tableRowId = 'row_' + textIdSplit[0];
            var tableRow = $(tableRowId);
            // get all text inputs
            var rowInputs = $(tableRowId).select('input[type="text"]');
            var totalPercentage = 0;
            rowInputs.each(function (inputElement) {
                var inputElementIdSplit = inputElement.id.split("|");
                if (inputElement.value) {
                    totalPercentage = totalPercentage + parseFloat(inputElement.value) 
                }
            });
            if (totalPercentage == 100 || totalPercentage == 0 ) {
                if ( $(tableRowId).hasClassName('alternate-rowWarn')){
                    $(tableRowId).removeClassName('alternate-rowWarn');
                }
                if ($$('tr.alternate-rowWarn').size() == 0) {
                    $('costCentersSubmit').removeClassName('buttontextdisabled')
                    $('costCentersSubmit').disabled = false;
                }

            } else {
                if ( !$(tableRowId).hasClassName('alternate-rowWarn')){
                    $(tableRowId).addClassName('alternate-rowWarn');
                }
                if (!$('costCentersSubmit').hasClassName('buttontextdisabled')) {
                    $('costCentersSubmit').addClassName('buttontextdisabled')
                    $('costCentersSubmit').disabled = true;
                }
            }
        });
    });
});

function processCostCenterData() {
    new Ajax.Request($('costCenters').action, {
        asynchronous: false,
        onSuccess: function(transport) {
            if (transport.responseText == "") {
                Effect.Appear('errorMessage', {duration: 0.0});
                Effect.Fade('errorMessage', {duration: 0.0, delay: 3.0});
            } else {
                Effect.Appear('eventMessage', {duration: 0.0});
                Effect.Fade('eventMessage', {duration: 0.0, delay: 3.0});
            }
        }, parameters: $('costCenters').serialize(), requestHeaders: {Accept: 'application/json'}
    });
}
