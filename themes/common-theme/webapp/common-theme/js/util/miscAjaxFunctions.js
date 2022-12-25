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
 
function getDependentDropdownValues(request, paramKey, paramField, targetField, responseName, keyName, descName, selected, callback, allowEmpty, hide, hideTitle, inputField){
// To dynamically populate a dependent drop-down on change on its parent drop-down, doesn't require any fixed naming convention 
// request      = request calling the service which retrieve the info from the DB, ex: getAssociatedStateList
// paramKey     = parameter value used in the called service 
// paramField   = parent drop-down field Id (mainId)
// targetField  = dependent drop-down field Id (dependentId)
// responseName = result returned by the service (using a standard json response, ie chaining json request)
// keyName      = keyName of the dependent drop-down  
// descName     = name of the dependent drop-down description
// selected     = optional name of a selected option
// callback     = optional javascript function called at end
// allowEmpty   = optional boolean argument, allow selection of an empty value for the dependentId
// hide         = optional boolean argument, if true the dependent drop-down field (targetField) will be hidden when no options are available else only disabled. False by default.
// hideTitle    = optional boolean argument (hide must be set to true), if true the title of the dependent drop-down field (targetField) will be hidden when no options are available else only disabled. False by default.
// inputField   = optional name of an input field    
//             this is to handle a specific case where an input field is needed instead of a drop-down when no values are returned by the request
//             this will be maybe extended later to use an auto-completed drop-down or a lookup, instead of straight drop-down currently, when there are too much values to populate
//             this is e.g. currently used in the Product Price Rules screen
    target = '#' + targetField;
    input = '#' + inputField;
    targetTitle = target + '_title'
    optionList = '';

    var paramData = new Array();
    // if there are multiple paramKeys (because of multiple dependencies) then create 
    // an array with multiple data information
    if (paramKey.indexOf(",") > -1) {
    	    var paramKeyArr = paramKey.split(",");
    	    var paramFieldArr = paramField.split(",");

    	    // Both arrays should be the same length
    	    for (var i=0; i<paramKeyArr.length; i++) {
    	        paramData.push({name: paramKeyArr[i], value: jQuery('#' + paramFieldArr[i]).val()});
    	    }
    } else {
    	paramData = [{
            name: paramKey,
            value: jQuery('#' + paramField).val()
        }] // get requested value from parent drop-down field
    }

    jQuery.ajax({
        url: request,
        data: paramData, // get requested value from parent drop-down field
        async: false,
        type: 'POST',
        success: function(result){
            list = result[responseName];
            // Create and show dependent select options
            if (list) {
                if(allowEmpty) {
                    // Allow null selection in dependent and set it as default if no selection exists.
                    if (selected == undefined || selected == "_none_") {
                      optionList += "<option selected='selected' value=''></option>";
                    } else {
                      optionList += "<option value=''></option>";
                    }
                }
                jQuery.each(list, function(key, value){
                    if (typeof value == 'string') {
                        values = value.split(': ');
                        if (values[1].indexOf(selected) >= 0 && selected.length > 0 && selected == values[1]) {
                            optionList += "<option selected='selected' value = '" + values[1] + "' >" + values[0] + "</option>";
                        } else {
                            optionList += "<option value = '" + values[1] + "' >" + values[0] + "</option>";
                        }
                    } else {
                        if (value[keyName] == selected) {
                            optionList += "<option selected='selected' value = '" + value[keyName] + "' >" + value[descName] + "</option>";
                        } else {
                            optionList += "<option value = '" + value[keyName] + "' >" + value[descName] + "</option>";
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
// arguments must be either a request only (1st argument) or a request followed by {name;value} pair/s parameters 
function getServiceResult(){
    var request = arguments[0];
    var params =  new Array();
    var data;
    jQuery.ajax({
        type: 'POST',
        url: request,
        data: prepareAjaxData(arguments),
        async: false,
        cache: false,
        success: function(result){
            data = result;
        }
    });
    return data;
}

function prepareAjaxData(params) {
  var data = new Array();
  if (params.length > 1) {
    for (var i = 1; i < params.length; i++) {
      data.push({
        name: params[i],
        value: params[i + 1]
    });
      i++;
    }
  }
    return data;
}

//*** checkUomConversion returns true if an UomConversion exists
function checkUomConversion(request, params){
    data = getServiceResult(request, params);
    return data['exist'];
}

/* initTimeZone is used to intialise the path to timezones files

The timezone region that loads on initialization is North America (the Olson 'northamerica' file). 
To change that to another reqion, set timezoneJS.timezone.defaultZoneFile to your desired region, like so:
  timezoneJS.timezone.zoneFileBasePath = '/tz';
  timezoneJS.timezone.defaultZoneFile = 'asia';
  timezoneJS.timezone.init();

If you want to preload multiple regions, set it to an array, like this:

  timezoneJS.timezone.zoneFileBasePath = '/tz';
  timezoneJS.timezone.defaultZoneFile = ['asia', 'backward', 'northamerica', 'southamerica'];
  timezoneJS.timezone.init();

By default the timezoneJS.Date timezone code lazy-loads the timezone data files, pulling them down and parsing them only as needed. 

For example, if you go with the out-of-the-box setup, you'll have all the North American timezones pre-loaded -- 
but if you were to add a date with a timezone of 'Asia/Seoul,' it would grab the 'asia' Olson file and parse it 
before calculating the timezone offset for that date.

You can change this behavior by changing the value of timezoneJS.timezone.loadingScheme. The three possible values are:

  timezoneJS.timezone.loadingSchemes.PRELOAD_ALL -- this will preload all the timezone data files for all reqions up front. This setting would only make sense if you know your users will be using timezones from all around the world, and you prefer taking the up-front load time to the small on-the-fly lag from lazy loading.
  timezoneJS.timezone.loadingSchemes.LAZY_LOAD -- the default. Loads some amount of data up front, then lazy-loads any other needed timezone data as needed.
  timezoneJS.timezone.loadingSchemes.MANUAL_LOAD -- Preloads no data, and does no lazy loading. Use this setting if you're loading pre-parsed JSON timezone data.

  More at https://github.com/mde/timezone-js

*/
function initTimeZone() {
  timezoneJS.timezone.zoneFileBasePath = '/common/js/plugins/date/timezones/min';
  timezoneJS.timezone.loadingSchemes.PRELOAD_ALL;
  timezoneJS.timezone.init();
}
