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
// *** getDependentDropdownValues allows to dynamically populate a dependent drop-down on change on its parent drop-down, doesn't require any fixed naming convention 
// request      = request calling the service which retrieve the info from the DB, ex: getAssociatedStateList
// paramKey     = parameter value used in the called service 
// paramField   = parent drop-down field Id (mainId)
// targetField  = dependent drop-down field Id (dependentId)
// responseName = result returned by the service (using a standard json response, ie chaining json request)
// keyName      = keyName of the dependent drop-down  
// descName     = name of the dependent drop-down description
// selected     = optional name of a selected option
// callback     = optional javascript function called at end
// hide         = optional boolean argument, if true the dependent drop-down field (targetField) will be hidden when no options are available else only disabled. False by default.
// hideTitle    = optional boolean argument (hide must be set to true), if true the title of the dependent drop-down field (targetField) will be hidden when no options are available else only disabled. False by default.
// inputField   = optional name of an input field    
// 				  this is to handle a specific case where an input field is needed instead of a drop-down when no values are returned by the request
// 				  this will be maybe extended later to use an auto-completed drop-down or a lookup, instead of straight drop-down currently, when there are too much values to populate
// 				  this is e.g. currently used in the Product Price Rules screen
function getDependentDropdownValues(request, paramKey, paramField, targetField, responseName, keyName, descName, selected, callback, hide, hideTitle, inputField){
    target = '#' + targetField;
    input = '#' + inputField;
    targetTitle = target + '_title'
    optionList = '';
    jQuery.ajax({
        url: request,
        data: [{
            name: paramKey,
            value: jQuery('#' + paramField).val()
        }], // get requested value from parent drop-down field
        async: false,
        type: 'POST',
        success: function(result){
            list = result[responseName];
            // Create and show dependent select options            
            if (list) {
                jQuery.each(list, function(key, value){
                    if (typeof value == 'string') {
                        values = value.split(': ');
                        if (values[1].indexOf(selected) >= 0 && selected.length > 0) {
                            optionList += "<option selected='selected' value = " + values[1] + " >" + values[0] + "</option>";
                        } else {
                            optionList += "<option value = " + values[1] + " >" + values[0] + "</option>";
                        }
                    } else {
                        if (value[keyName] == selected) {
                            optionList += "<option selected='selected' value = " + value[keyName] + " >" + value[descName] + "</option>";
                        } else {
                            optionList += "<option value = " + value[keyName] + " >" + value[descName] + "</option>";
                        }
                    }
                })
            };
            // Hide/show the dependent drop-down if hide=true else simply disable/enable
            if ((!list) || (list.length < 1) || ((list.length == 1) && jQuery.inArray("_NA_", list) != -1)) {
                jQuery(target).attr('disabled', 'disabled');
                if (hide) {
                    if (jQuery(target).is(':visible')) {
                        jQuery(target).fadeOut(2500);
                        if (hideTitle) jQuery(targetTitle).fadeOut(2500);
                    } else {
                        jQuery(target).fadeIn();
                        if (hideTitle) jQuery(targetTitle).fadeIn();
                        jQuery(target).fadeOut(2500);
                        if (hideTitle) jQuery(targetTitle).fadeOut(2500);
                    }
                }
            } else {
                jQuery(target).removeAttr('disabled');
                if (hide) {
                    if (!jQuery(target).is(':visible')) {
                        jQuery(target).fadeIn();
                        if (hideTitle) jQuery(targetTitle).fadeIn();
                    }
                }
            }
        },
        complete: function(){
            // this is to handle a specific case where an input field is needed instead of a drop-down when no values are returned by the request (else if allow-empty="true" is used autoComplete handle the case)
            // this could be extended later to use an auto-completed drop-down or a lookup, instead of drop-down currently, when there are too much values to populate
            // Another option is to use an input field with Id instead of a drop-down, see setPriceRulesCondEventJs.ftl and top of getAssociatedPriceRulesConds service
            if (!list && inputField) {
                jQuery(target).hide();
                jQuery(input).show();
            } else if (inputField) {
                jQuery(input).hide();
                jQuery(target).show();
            }
            jQuery(target).html(optionList).click().change(); // .change() needed when using also asmselect on same field, .click() specifically for IE8
            if (callback != null) eval(callback);
        }
    });
}

//*** calls any service already mounted as an event
function getServiceResult(request, params){
    var data;
    jQuery.ajax({
        type: 'POST',
        url: request,
        data: params,
        async: false,
        cache: false,
        success: function(result){
            data = result;
        }
    });
    return data;
}

//*** checkUomConversion returns true if an UomConversion exists
function checkUomConversion(request, params){
    data = getServiceResult(request, params);
    return data['exist'];
}

